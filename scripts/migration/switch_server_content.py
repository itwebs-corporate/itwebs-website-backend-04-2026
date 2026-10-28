#!/usr/bin/env python3
"""Switch publication to imported content, preserving full content and a backup journal.

Publishes imported services/cases/articles, updates relevant page selections, then
archives previously published services/cases. No editorial page/block changes.
"""
import argparse
import concurrent.futures
import copy
import fcntl
import json
import os
from pathlib import Path
import sqlite3
import urllib.error
import urllib.parse
import urllib.request

from migrate_by import Api, NoRedirect, digest, own_snapshot
from pages_by import build_pages
from publish_server_blog import payload as article_payload

ROOT = Path(__file__).resolve().parents[2] / '.migration-local'
BASE = 'http://127.0.0.1:19998'


def catalog_payload(response):
    card = response['entry']
    body = {k: card[k] for k in ('title', 'slug', 'description', 'price', 'published', 'sortOrder')}
    body.update({k: response[k] for k in ('seoTitle', 'seoDescription', 'canonical', 'service', 'project', 'specialist', 'member', 'serviceSelections')})
    body['coverMediaId'] = (card.get('cover') or {}).get('id')
    body['mobileCoverMediaId'] = (card.get('mobileCover') or {}).get('id')
    body['categoryIds'] = sorted(c['id'] for c in card['categories'])
    body['caseIds'] = [c['id'] for c in response['examples']]
    if body['service']:
        body['service'] = copy.deepcopy(body['service'])
        for key in ('faq', 'stack', 'process', 'tasks', 'audience', 'cooperation'):
            body['service'][key] = []  # Actual relations remain in serviceSelections.
    if body['project']:
        body['project'] = {k: v for k, v in body['project'].items() if k != 'serviceSlug'}
    return body


def material(kind, response):
    if kind == 'catalog':
        return catalog_payload(response)
    if kind == 'article':
        return dict(article_payload(response), status=response['article']['status'],
                    publishedAt=response['article']['publishedAt'], readingMinutes=response['article']['readingMinutes'])
    return {k: response[k] for k in ('allEntries', 'entryIds', 'overrides')}


def current(api, kind, url):
    if kind != 'collection':
        return api.request(url)
    page_url, ident = url.rsplit('/collections/', 1)
    return next(c for c in api.request(page_url)['collections'] if c['id'] == int(ident))


def same_result(kind, actual, request):
    expected = {k: v for k, v in request.items() if k != 'version'}
    if kind == 'article' and expected['publishedAt'] is None:
        expected['publishedAt'] = actual['article']['publishedAt']
        if not expected['publishedAt']:
            return False
    return material(kind, actual) == expected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', required=True)
    parser.parse_args()
    settings = json.loads((ROOT / 'server-env.json').read_text())
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    locks = []
    for name in ('server-content-switch.lock', 'server-blog-publication.lock', 'server-page-gaps.lock'):
        handle = open(ROOT / name, 'a')
        fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
        locks.append(handle)
    api = Api(BASE)
    mapping = sqlite3.connect((ROOT / 'server-by-state.sqlite').resolve().as_uri() + '?mode=ro', uri=True)
    if mapping.execute("SELECT value FROM meta WHERE key='target'").fetchone()[0] != BASE:
        raise ValueError('Wrong migration target')
    imported = {key: ident for key, ident in mapping.execute("SELECT key,id FROM records WHERE status='created' AND id IS NOT NULL")}
    service_ids = {i for k, i in imported.items() if k.startswith('service:')}
    case_ids = {i for k, i in imported.items() if k.startswith('case:')}
    article_ids = {i for k, i in imported.items() if k.startswith('post:')}
    if (len(service_ids), len(case_ids), len(article_ids)) != (34, 19, 43):
        raise ValueError('Unexpected imported scope')
    path = ROOT / 'server-content-switch.sqlite'
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
    os.close(fd)
    os.chmod(path, 0o600)
    db = sqlite3.connect(path)
    db.execute('PRAGMA synchronous=FULL')
    db.execute('CREATE TABLE IF NOT EXISTS ops(seq INTEGER PRIMARY KEY,kind TEXT NOT NULL,url TEXT NOT NULL,before_json TEXT NOT NULL,request_json TEXT NOT NULL,after_json TEXT,status TEXT NOT NULL)')
    db.execute('CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)')
    fingerprint = digest(imported)
    previous = db.execute("SELECT value FROM meta WHERE key='mapping'").fetchone()
    if previous and previous[0] != fingerprint:
        raise ValueError('Switch journal belongs to another import')
    if not db.execute('SELECT count(*) FROM ops').fetchone()[0]:
        services = api.listing('/api/admin/catalog/services')
        cases = api.listing('/api/admin/catalog/cases')
        old_services = [c for c in services if c['published'] and c['id'] not in service_ids]
        old_cases = [c for c in cases if c['published'] and c['id'] not in case_ids]
        if (len(old_services), len(old_cases)) != (30, 12):
            raise ValueError('Published old scope changed; inspect before switching')
        operations = []

        def add(kind, url, before, desired):
            if material(kind, before) == desired:
                return
            card = before.get('entry') or before.get('article') or before
            operations.append((kind, url, json.dumps(before), json.dumps(dict(desired, version=card['version']))))

        targets = [('catalog', '/api/admin/catalog/services/' + str(i)) for i in sorted(service_ids)]
        targets += [('catalog', '/api/admin/catalog/cases/' + str(i)) for i in sorted(case_ids)]
        targets += [('article', '/api/admin/blog/articles/' + str(i)) for i in sorted(article_ids)]

        def read_target(item):
            kind, url = item
            return kind, url, Api(BASE).request(url)

        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            for kind, url, before in pool.map(read_target, targets):
                desired = material(kind, before)
                if kind == 'catalog':
                    desired['published'] = True
                else:
                    if not all(desired[k] for k in ('blocks', 'excerpt', 'coverMediaId', 'categoryIds')):
                        raise ValueError('Incomplete article: ' + url)
                    desired.update(status='PUBLISHED', publishedAt=None)
                add(kind, url, before, desired)
        specs = {p['slug']: p for p in build_pages()}
        for page_card in api.listing('/api/admin/pages'):
            slug = page_card['slug']
            page = api.request('/api/admin/pages/' + slug)
            desired_specs = {c['key']: c for c in specs.get(slug, {}).get('collections', [])}
            for collection in page['collections']:
                if collection['kind'] not in ('SERVICES', 'CASES'):
                    continue
                desired = material('collection', collection)
                spec = desired_specs.get(collection['key'])
                if spec:
                    desired = {'allEntries': spec['allEntries'],
                        'entryIds': [imported[k] for k in spec['refs']] if spec['refs'] is not None else [],
                        'overrides': []}
                elif slug == 'product' and collection['kind'] == 'CASES':
                    # Only Learn English is the same real project in both datasets.
                    replacements = {947: imported['case:1']}
                    desired['entryIds'] = [replacements.get(i, i) for i in collection['entryIds']]
                    desired['overrides'] = [dict(o, entryId=replacements.get(o['entryId'], o['entryId'])) for o in collection['overrides']]
                if desired != material('collection', collection):
                    add('collection', '/api/admin/pages/' + slug + '/collections/' + str(collection['id']), collection, desired)
        old_targets = [('catalog', '/api/admin/catalog/cases/' + str(c['id'])) for c in old_cases]
        old_targets += [('catalog', '/api/admin/catalog/services/' + str(c['id'])) for c in old_services]
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            for kind, url, before in pool.map(read_target, old_targets):
                add(kind, url, before, dict(material(kind, before), published=False))
        db.execute("INSERT OR REPLACE INTO meta VALUES('mapping',?)", (fingerprint,))
        for seq, op in enumerate(operations):
            db.execute("INSERT INTO ops VALUES(?,?,?,?,?,NULL,'prepared')", (seq, *op))
        db.commit()  # Full before snapshots, requests and versions before any writes.
        print('Backed up switch:', len(operations), 'operations.', flush=True)
    for seq, kind, url, before_json, request_json, after_json, state in db.execute('SELECT * FROM ops ORDER BY seq').fetchall():
        before, request = json.loads(before_json), json.loads(request_json)
        live = current(api, kind, url)
        if state == 'done':
            if not same_result(kind, live, request):
                raise ValueError('Previously completed operation differs: ' + url)
            continue
        if state == 'pending' and same_result(kind, live, request):
            db.execute("UPDATE ops SET after_json=?,status='done' WHERE seq=?", (json.dumps(live), seq))
            db.commit()
            continue
        if digest(own_snapshot(live)) != digest(own_snapshot(before)):
            raise ValueError('Record edited after preflight: ' + url)
        db.execute("UPDATE ops SET status='pending' WHERE seq=?", (seq,))
        db.commit()
        api.request(url, 'PUT', request)
        after = current(api, kind, url)
        if not same_result(kind, after, request):
            raise ValueError('Write verification mismatch: ' + url)
        db.execute("UPDATE ops SET after_json=?,status='done' WHERE seq=?", (json.dumps(after), seq))
        db.commit()
        print('SWITCHED', url, flush=True)

    def public(route):
        with urllib.request.build_opener(NoRedirect()).open(BASE + route, timeout=45) as response:
            return json.load(response)

    for kind, expected in [('services', service_ids), ('cases', case_ids)]:
        visible = public('/api/catalog/' + kind + '?size=100')['items']
        if {r['id'] for r in visible} != expected:
            raise ValueError('Public catalog mismatch: ' + kind)
    visible = public('/api/blog/articles?size=100')['items']
    if not article_ids.issubset({r['id'] for r in visible}):
        raise ValueError('Public blog mismatch')
    for slug, key, expected in [('home', 'cases', 6), ('services', 'cases', 4), ('cases', 'cases', 19), ('blog-article', 'services', 3)]:
        page = public('/api/pages/' + slug)
        if len(page['collections'][key]) != expected:
            raise ValueError('Public page selection mismatch: ' + slug)
    jobs = []
    for kind, expected in [('services', service_ids), ('cases', case_ids)]:
        for r in public('/api/catalog/' + kind + '?size=100')['items']:
            jobs.append('/api/catalog/' + kind + '/' + urllib.parse.quote(r['slug'], safe=''))
    jobs += ['/api/blog/articles/' + urllib.parse.quote(r['slug'], safe='') for r in visible if r['id'] in article_ids]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(public, jobs))
    print('Verified public API: 34 services, 19 cases, 43 articles and updated page selections. Old 30 services and 12 cases are drafts.', flush=True)
    print('Product: Learn English selection remapped; two unrelated old product cases retained as draft references, not substituted.', flush=True)


if __name__ == '__main__':
    main()
