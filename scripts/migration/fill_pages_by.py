#!/usr/bin/env python3
"""Fill draft pages through the existing API; no app launch or QA pass.

The empty legacy local copy has no page-creation API. --scaffold-local creates only
missing empty page/block/collection containers in the explicitly named local copy.
All text, metadata, buttons and catalogue selections are subsequently written by API.
Other destinations and unowned existing content are refused.
"""
import argparse
import fcntl
import json
import os
from pathlib import Path
import sqlite3
import subprocess

from migrate_by import Api, digest
from pages_by import build_pages, text

ROOT = Path(__file__).resolve().parents[2]
RUNTIME = ROOT / '.migration-local'
DOCKER = '/Applications/Docker.app/Contents/Resources/bin/docker'


def sql_string(value):
    return "'" + str(value).replace("'", "''") + "'"


def scaffold(specs):
    statements = ["BEGIN; SELECT pg_advisory_xact_lock(591738208);",
        'CREATE TABLE IF NOT EXISTS migration_by_page_scaffold (kind text NOT NULL, key text NOT NULL, id bigint NOT NULL, PRIMARY KEY(kind,key));']
    for spec in specs:
        slug = sql_string(spec['slug'])
        statements.append(f"""WITH inserted AS (
            INSERT INTO site_pages(version,slug,path,title,published)
            VALUES(0,{slug},{sql_string(spec['path'])},{sql_string(spec['slug'])},false)
            ON CONFLICT DO NOTHING RETURNING id)
            INSERT INTO migration_by_page_scaffold SELECT 'page',{slug},id FROM inserted ON CONFLICT DO NOTHING;""")
        for order, block in enumerate(spec['blocks']):
            key = sql_string(spec['slug'] + '/' + block['key'])
            empty_data = {'fields': {field: text(None) for field in block['schema']['fields']}, 'items': []}
            statements.append(f"""WITH inserted AS (
                INSERT INTO site_blocks(version,page_id,block_key,title,type,sort_order,published,schema,data)
                SELECT 0,id,{sql_string(block['key'])},{sql_string(block['title'])},{sql_string(block['type'])},{order},false,
                    {sql_string(json.dumps(block['schema'], ensure_ascii=False))}::jsonb,
                    {sql_string(json.dumps(empty_data))}::jsonb
                FROM migration_by_page_scaffold WHERE kind='page' AND key={slug}
                ON CONFLICT DO NOTHING RETURNING id)
                INSERT INTO migration_by_page_scaffold SELECT 'block',{key},id FROM inserted ON CONFLICT DO NOTHING;""")
        for collection in spec['collections']:
            key = sql_string(spec['slug'] + '/' + collection['key'])
            statements.append(f"""WITH inserted AS (
                INSERT INTO page_collections(version,page_id,collection_key,kind,all_entries,overrides)
                SELECT 0,id,{sql_string(collection['key'])},{sql_string(collection['kind'])},false,'[]'::jsonb
                FROM migration_by_page_scaffold WHERE kind='page' AND key={slug}
                ON CONFLICT DO NOTHING RETURNING id)
                INSERT INTO migration_by_page_scaffold SELECT 'collection',{key},id FROM inserted ON CONFLICT DO NOTHING;""")
    statements.append("COMMIT; SELECT coalesce(json_agg(t),'[]') FROM migration_by_page_scaffold t;")
    result = subprocess.check_output([DOCKER, 'exec', '-i', 'itwebs-by-migration-db', 'psql', '-U', 'itwebs',
        '-p', '15432', '-d', 'itwebs', '-v', 'ON_ERROR_STOP=1', '-Atq'], input='\n'.join(statements), text=True)
    # Advisory-lock SELECT produces whitespace; composite JSON arrays can span lines.
    return {(r['kind'], r['key']): r['id'] for r in json.loads(result.strip())}


def snapshot(kind, data):
    if kind == 'page':
        return {k: data[k] for k in ['page', 'seoTitle', 'seoDescription', 'canonical']}
    return data


class PageWriter:
    def __init__(self):
        credentials = json.loads((RUNTIME / 'local-env.json').read_text())
        os.environ['CMS_ADMIN_EMAIL'] = credentials['ADMIN_EMAIL']
        os.environ['CMS_ADMIN_PASSWORD'] = credentials['ADMIN_PASSWORD']
        self.api = Api('http://127.0.0.1:19999')
        self.lock = open(RUNTIME / 'pages-state.lock', 'a')
        fcntl.flock(self.lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        self.db = sqlite3.connect(RUNTIME / 'pages-state.sqlite')
        os.chmod(RUNTIME / 'pages-state.sqlite', 0o600)
        self.db.execute('PRAGMA synchronous=FULL')
        self.db.execute('CREATE TABLE IF NOT EXISTS writes(key TEXT PRIMARY KEY,payload_hash TEXT NOT NULL,snapshot TEXT NOT NULL,pending INTEGER NOT NULL)')
        self.db.commit()
        with sqlite3.connect(f'file:{RUNTIME / "by-state.sqlite"}?mode=ro', uri=True) as mapping:
            self.ids = dict(mapping.execute('SELECT key,id FROM records WHERE id IS NOT NULL'))

    def write(self, kind, key, url, current, desired):
        own = snapshot(kind, current)
        saved = self.db.execute('SELECT payload_hash,snapshot,pending FROM writes WHERE key=?', (key,)).fetchone()
        if saved and digest(own) != digest(json.loads(saved[1])):
            actual = current if kind != 'page' else dict(current['page'], **{k: current[k] for k in ['seoTitle', 'seoDescription', 'canonical']})
            if not (saved[2] and saved[0] == digest(desired) and all(actual.get(k) == v for k, v in desired.items())):
                raise ValueError('Page content changed outside this import; refusing overwrite: ' + key)
            self.db.execute('UPDATE writes SET snapshot=?,pending=0 WHERE key=?', (json.dumps(own, ensure_ascii=False), key))
            self.db.commit()
            return
        if saved and saved[0] == digest(desired) and not saved[2]:
            return
        if not saved:
            if kind == 'block' and (current['published'] or current['data']['items'] or any(v.get('text') or v.get('href') or v.get('mediaId') for v in current['data']['fields'].values())):
                raise ValueError('Existing nonempty block is protected: ' + key)
            if kind == 'page' and (current['page']['published'] or current['seoTitle'] or current['seoDescription'] or current['canonical']):
                raise ValueError('Existing page metadata is protected: ' + key)
            if kind == 'collection' and (current['entryIds'] or current['allEntries'] or current['overrides']):
                raise ValueError('Existing selection is protected: ' + key)
        version = current['page']['version'] if kind == 'page' else current['version']
        self.db.execute('INSERT OR REPLACE INTO writes VALUES (?,?,?,1)', (key, digest(desired), json.dumps(own, ensure_ascii=False)))
        self.db.commit()
        response = self.api.request(url, 'PUT', dict(desired, version=version))
        self.db.execute('UPDATE writes SET snapshot=?,pending=0 WHERE key=?', (json.dumps(snapshot(kind, response), ensure_ascii=False), key))
        self.db.commit()
        print('FILLED', key, flush=True)

    def run(self, specs, ownership):
        for spec in specs:
            slug = spec['slug']
            if ('page', slug) not in ownership:
                print('SKIPPED existing/unowned page:', slug)
                continue
            url = '/api/admin/pages/' + slug
            current = self.api.request(url)  # Current versions, not a QA run.
            self.write('page', 'page:' + slug, url, current, {
                **{k: spec[k] for k in ['title', 'seoTitle', 'seoDescription', 'canonical']}, 'published': False})
            for block in spec['blocks']:
                block_key = slug + '/' + block['key']
                if ('block', block_key) not in ownership:
                    print('SKIPPED existing/unowned block:', block_key)
                    continue
                found = next((item for item in current['blocks'] if item['id'] == ownership['block', block_key] and item['ownerPage'] == slug), None)
                if found is None:
                    raise ValueError('Owned block unavailable: ' + block_key)
                if set(block['data']['fields']) - set(found['schema']['fields']):
                    raise ValueError('Content does not fit fixed block schema: ' + block_key)
                fields = {key: block['data']['fields'].get(key, text(None)) for key in found['schema']['fields']}
                items = [{key: row.get(key, text(None)) for key in found['schema']['itemFields']} for row in block['data']['items']]
                self.write('block', 'block:' + block_key, url + '/blocks/' + str(found['id']), found,
                    {'data': {'fields': fields, 'items': items}, 'published': False})
            for collection in spec['collections']:
                collection_key = slug + '/' + collection['key']
                if ('collection', collection_key) not in ownership:
                    print('SKIPPED existing/unowned selection:', collection_key)
                    continue
                found = next(item for item in current['collections'] if item['id'] == ownership['collection', collection_key])
                desired = {'allEntries': collection['allEntries'],
                    'entryIds': [self.ids[ref] for ref in collection['refs']] if collection['refs'] is not None else [], 'overrides': []}
                self.write('collection', 'collection:' + collection_key, url + '/collections/' + str(found['id']), found, desired)
        print('Draft page filling finished. No app launch, publication or verification pass performed.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scaffold-local', action='store_true', required=True)
    parser.parse_args()
    specs = build_pages()
    PageWriter().run(specs, scaffold(specs))


if __name__ == '__main__':
    main()
