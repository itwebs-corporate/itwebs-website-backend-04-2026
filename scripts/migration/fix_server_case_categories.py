#!/usr/bin/env python3
"""Assign factual categories to imported cases; no frontend or editorial changes.

The frontend requires a category to render a case. Back up full responses before
writing, preserve populated categories, and verify content after each update.
"""
import argparse
import fcntl
import json
import os
from pathlib import Path
import sqlite3

from migrate_by import Api
from switch_server_content import catalog_payload

RUNTIME = Path(__file__).resolve().parents[2] / '.migration-local'
# Derived from the actual project descriptions, not filename/title inference.
CATEGORIES = {**{i: ['mobile'] for i in range(1, 9)},
    **{i: ['web'] for i in (9, 10, 11, 12, 14, 15, 16, 18, 19)},
    7: ['mobile', 'ai'], 13: ['web', 'ecom'], 17: ['web', 'ai']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', required=True)
    parser.parse_args()
    settings = json.loads((RUNTIME / 'server-env.json').read_text())
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    api = Api('http://127.0.0.1:19998')
    lock = open(RUNTIME / 'server-content-switch.lock', 'a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    mapping = sqlite3.connect((RUNTIME / 'server-by-state.sqlite').resolve().as_uri() + '?mode=ro', uri=True)
    ids = dict(mapping.execute("SELECT key,id FROM records WHERE key LIKE 'case:%' AND status='created'"))
    categories = {c['slug']: c['id'] for c in api.listing('/api/admin/catalog/cases/categories')}
    if len(ids) != 19 or not {'web', 'mobile', 'ai', 'ecom'}.issubset(categories):
        raise ValueError('Unexpected case/category scope')
    path = RUNTIME / 'server-case-categories.sqlite'
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
    os.close(fd)
    os.chmod(path, 0o600)
    db = sqlite3.connect(path)
    db.execute('PRAGMA synchronous=FULL')
    db.execute('CREATE TABLE IF NOT EXISTS cases(id INTEGER PRIMARY KEY,before_json TEXT NOT NULL,after_json TEXT,status TEXT NOT NULL)')
    prepared = []
    for old, slugs in CATEGORIES.items():
        ident = ids['case:' + str(old)]
        url = '/api/admin/catalog/cases/' + str(ident)
        before = api.request(url)
        desired_ids = sorted(categories[s] for s in slugs)
        existing_ids = sorted(c['id'] for c in before['entry']['categories'])
        if existing_ids:
            if existing_ids != desired_ids:
                raise ValueError('Existing case categories protected: ' + url)
            continue
        db.execute("INSERT OR IGNORE INTO cases VALUES(?,?,NULL,'prepared')", (ident, json.dumps(before)))
        body = catalog_payload(before)
        body['categoryIds'] = desired_ids
        prepared.append((ident, url, before, body))
    db.commit()  # Full technical snapshots before any server writes.
    for ident, url, before, body in prepared:
        db.execute("UPDATE cases SET status='pending' WHERE id=?", (ident,))
        db.commit()
        api.request(url, 'PUT', dict(body, version=before['entry']['version']))
        after = api.request(url)
        if catalog_payload(after) != body:
            raise ValueError('Category-only update mismatch: ' + url)
        db.execute("UPDATE cases SET after_json=?,status='verified' WHERE id=?", (json.dumps(after), ident))
        db.commit()
        print('CATEGORIZED', ident, after['entry']['slug'], flush=True)
    print('Verified: all 19 imported cases have categories. Texts, images, relations and publication preserved.', flush=True)


if __name__ == '__main__':
    main()
