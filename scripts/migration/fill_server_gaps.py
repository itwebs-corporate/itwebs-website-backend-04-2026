#!/usr/bin/env python3
"""Fill only empty values in existing server page schemas, preserving publication.

No page/block creation, schema change, collection change, or replacement of content.
SQLite stores before/after values for recovery and audit of this technical operation.
"""
import argparse
import copy
import fcntl
import json
import os
from pathlib import Path
import sqlite3

from migrate_by import Api
from pages_by import build_pages, block, link

RUNTIME = Path(__file__).resolve().parents[2] / '.migration-local'


def empty(value):
    return not value or not any(value.get(k) not in (None, '') for k in ('text', 'href', 'mediaId'))


def fill_values(current, proposed, schema, changes, prefix):
    for key, value in proposed.items():
        if key not in schema or not empty(current.get(key)) or empty(value):
            continue
        if (value.get('href') and schema[key]['type'] != 'LINK') or value.get('mediaId'):
            continue
        filled = copy.deepcopy(value)
        filled['published'] = current.get(key, {}).get('published', False)
        current[key] = filled
        changes.append(prefix + key)


def plan_block(current, proposed):
    data = copy.deepcopy(current['data'])
    changes = []
    fill_values(data['fields'], proposed['data']['fields'], current['schema']['fields'], changes, 'field:')
    if not data['items'] and proposed['data']['items']:
        # Never append to or replace an existing list. Exclude unsupported columns.
        rows = [{k: copy.deepcopy(v) for k, v in row.items() if k in current['schema']['itemFields']}
                for row in proposed['data']['items']]
        if all(row and any(not empty(v) for v in row.values()) for row in rows):
            for row in rows:
                for value in row.values():
                    value['published'] = False  # New content stays unpublished.
            data['items'] = rows
            changes.append('empty-list:' + str(len(rows)))
    # Existing lists and all additional fields remain byte-for-byte equivalent.
    return data, changes


def specifications():
    specs = build_pages()
    extra = {
        'home': [block('brands', '', 'COPY', {'cta': link('Обсудить проект', '/contacts')}),
                 block('staffing', '', 'COPY', {'orderLabel': 'Обсудить участие'})],
        'outstaff': [block('outstaff-page', '', 'COPY', {'badgeMobile': 'IT-аутстаффинг'}),
                    block('outstaff-page-specialists', '', 'COPY', {'allLabel': 'Все специалисты', 'emptyLabel': 'Специалисты пока не опубликованы'}),
                    block('outstaff-page-team', '', 'COPY', {'title': 'Команда проекта', 'eyebrow': 'Специалисты'})],
        'blog-article': [block('blog-article-page', '', 'COPY', {'floatingSecondary': link('Все материалы', '/blog')})],
        'product': [block('product-page', '', 'COPY', {'floatingSecondary': link('Все услуги', '/services')})],
        'contacts': [block('contacts-company', '', 'COPY', {
            **{key + 'Label': value for key, value in {'inn': 'ИНН', 'kpp': 'КПП', 'name': 'Наименование компании',
                'ogrn': 'ОГРН', 'innKpp': 'ИНН / КПП', 'address': 'Юридический адрес'}.items()},
            **{key + 'LabelMobile': value for key, value in {'inn': 'ИНН', 'kpp': 'КПП', 'name': 'Компания',
                'ogrn': 'ОГРН', 'innKpp': 'ИНН / КПП', 'address': 'Адрес'}.items()}})],
        'policy': [block('policy-page', '', 'COPY', {'title': 'Политика в отношении обработки персональных данных', 'badgeMobile': 'Документы'})],
    }
    for slug, additions in extra.items():
        spec = next((p for p in specs if p['slug'] == slug), None)
        if spec is None:
            spec = {'slug': slug, 'blocks': []}
            specs.append(spec)
        for addition in additions:
            existing = next((b for b in spec['blocks'] if b['key'] == addition['key']), None)
            if existing:
                existing['data']['fields'].update(addition['data']['fields'])
            else:
                spec['blocks'].append(addition)
    shared = next(p for p in specs if p['slug'] == 'shared')
    contact = next(b for b in shared['blocks'] if b['key'] == 'contact')
    contact['data']['fields']['error']['text'] = 'Не удалось отправить обращение. Попробуйте ещё раз или свяжитесь с нами по контактам на сайте.'
    return specs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    settings = json.loads((RUNTIME / 'server-env.json').read_text())
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    api = Api('http://127.0.0.1:19998')
    lock = open(RUNTIME / 'server-page-gaps.lock', 'a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    db = None
    if args.apply:
        path = RUNTIME / 'server-page-gaps.sqlite'
        fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
        os.close(fd)
        os.chmod(path, 0o600)
        db = sqlite3.connect(path)
        db.execute('PRAGMA synchronous=FULL')
        db.execute('CREATE TABLE IF NOT EXISTS writes(id INTEGER PRIMARY KEY,url TEXT NOT NULL,before_json TEXT NOT NULL,request_json TEXT NOT NULL,after_json TEXT,status TEXT NOT NULL)')
        db.commit()
        for ident, url, request in db.execute("SELECT id,url,request_json FROM writes WHERE status='pending'").fetchall():
            current = api.request(url)
            desired = json.loads(request)
            if current['data'] != desired['data'] or current['published'] != desired['published']:
                raise ValueError('Pending operation requires review: ' + url)
            db.execute("UPDATE writes SET after_json=?,status='verified' WHERE id=?", (json.dumps(current), ident))
            db.commit()
    count = 0
    for spec in specifications():
        page = api.request('/api/admin/pages/' + spec['slug'])
        blocks = {b['key']: b for b in page['blocks'] if b.get('ownerPage') == spec['slug']}
        for proposed in spec['blocks']:
            current = blocks.get(proposed['key'])
            if current is None:
                print('NO CREATE API:', spec['slug'] + '/' + proposed['key'], flush=True)
                continue
            data, changes = plan_block(current, proposed)
            missing = sorted(set(proposed['data']['fields']) - set(current['schema']['fields']))
            if missing:
                print('SCHEMA MISSING:', spec['slug'] + '/' + proposed['key'], ', '.join(missing), flush=True)
            if not changes:
                continue
            url = '/api/admin/pages/' + spec['slug'] + '/blocks/' + str(current['id'])
            request = {'version': current['version'], 'published': current['published'], 'data': data}
            if args.apply:
                cursor = db.execute("INSERT INTO writes(url,before_json,request_json,status) VALUES(?,?,?,'pending')",
                    (url, json.dumps(current), json.dumps(request)))
                ident = cursor.lastrowid
                db.commit()
                api.request(url, 'PUT', request)
                actual = api.request('/api/admin/pages/' + spec['slug'])
                found = next(b for b in actual['blocks'] if b['id'] == current['id'])
                if found['data'] != data or found['published'] != current['published'] or actual['page'] != page['page']:
                    raise ValueError('Verification mismatch: ' + url)
                db.execute("UPDATE writes SET after_json=?,status='verified' WHERE id=?", (json.dumps(found), ident))
                db.commit()
            print('FILLED' if args.apply else 'WOULD FILL', spec['slug'] + '/' + proposed['key'], ', '.join(changes), flush=True)
            count += len(changes)
    print('Total:', count, 'empty fields/lists.', 'Applied and verified.' if args.apply else 'Read only.')


if __name__ == '__main__':
    main()
