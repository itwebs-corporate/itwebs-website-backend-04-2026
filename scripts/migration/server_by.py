#!/usr/bin/env python3
"""Inspect or import draft catalog content through the dedicated SSH tunnel.

configure/preflight/preview do not change server content. apply requires explicit
backup confirmation and never writes existing pages or unowned catalog entries.
"""
import argparse
import getpass
import json
import os
from pathlib import Path

from migrate_by import Api, Migration

RUNTIME = Path(__file__).resolve().parents[2] / '.migration-local'
CREDENTIALS = RUNTIME / 'server-env.json'


class ServerMigration(Migration):
    def __init__(self, options):
        super().__init__(options)
        # Explicit semantic mapping, reviewed against the source and server groups.
        # Reuse the server dictionary verbatim; never rename or reorder it.
        slugs = {'group:1': 'web', 'group:2': 'mobile', 'group:3': 'support',
                 'group:4': 'crm', 'group:5': 'automation', 'group:6': '1c'}
        rows = self.api.listing('/api/admin/catalog/services/categories')
        for item in self.manifest['categories']:
            if item['key'].startswith('blog-category:'):
                candidates = self.existing('/api/admin/blog/categories')
                slug = item['body']['slug']
            elif item['key'] in slugs:
                candidates, slug = rows, slugs[item['key']]
            else:
                continue
            matches = [row for row in candidates if row['slug'] == slug]
            if len(matches) > 1:
                raise ValueError('Ambiguous server category: ' + item['key'])
            if matches:
                item['body'] = {k: matches[0][k] for k in ('name', 'slug', 'sortOrder')}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['configure', 'preflight', 'preview', 'apply', 'verify'])
    parser.add_argument('--backup-confirmed', action='store_true')
    args = parser.parse_args()
    if args.command == 'apply' and not args.backup_confirmed:
        parser.error('Для записи на сервер требуется --backup-confirmed после резервного копирования')
    if args.command == 'configure':
        email = input('Логин серверной админки: ').strip()
        password = getpass.getpass('Пароль серверной админки (не отображается): ')
        if not email or not password:
            raise ValueError('Логин и пароль обязательны')
        RUNTIME.mkdir(mode=0o700, exist_ok=True)
        fd = os.open(CREDENTIALS, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'w') as output:
            json.dump({'ADMIN_EMAIL': email, 'ADMIN_PASSWORD': password}, output)
        print('Доступ сохранён локально в закрытом файле, исключённом из Git.')
        return
    settings = json.loads(CREDENTIALS.read_text())
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    if args.command in ('preview', 'apply', 'verify'):
        options = argparse.Namespace(base_url='http://127.0.0.1:19998',
            source_container='itwebs-legacy-audit-20261026', source_db='legacy_main',
            docker='/Applications/Docker.app/Contents/Resources/bin/docker',
            static_archive=Path.home() / 'Downloads/itwebs-by-static.tar.gz',
            state=RUNTIME / 'server-by-state.sqlite', apply=args.command == 'apply',
            update_owned=False, skip_verification=False)
        migration = ServerMigration(options)
        if args.command == 'verify':
            migration.verify()
        else:
            migration.run()
        return
    api = Api('http://127.0.0.1:19998')
    for label, path in [
        ('Страницы', '/api/admin/pages'),
        ('Услуги', '/api/admin/catalog/services'),
        ('Кейсы', '/api/admin/catalog/cases'),
        ('Статьи', '/api/admin/blog/articles'),
        ('Медиа', '/api/admin/media'),
    ]:
        rows = api.listing(path)
        print(f'{label}: {len(rows)}')
        if label != 'Медиа':
            for row in rows:
                card = row.get('entry') or row.get('article') or row.get('page') or row
                print(' ', json.dumps({k: card[k] for k in
                    ('id', 'slug', 'title', 'published', 'status') if k in card}, ensure_ascii=False))
    print('Завершено. Только чтение; серверные данные не изменены.')


if __name__ == '__main__':
    main()
