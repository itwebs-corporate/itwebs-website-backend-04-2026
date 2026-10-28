#!/usr/bin/env python3
"""Publish only the 43 imported articles, preserving their editorial content.

Requires --apply. Saves all original article responses before the first write,
journals publication, refuses outside edits, and checks anonymous public access.
"""
import argparse
import concurrent.futures
import fcntl
import json
import os
from pathlib import Path
import sqlite3
import urllib.parse
import urllib.request

from migrate_by import Api, NoRedirect, digest, own_snapshot

RUNTIME = Path(__file__).resolve().parents[2] / '.migration-local'


def payload(response):
    card = response['article']
    return {**{k: card[k] for k in ('title', 'slug', 'excerpt')},
        'blocks': [{k: v for k, v in b.items() if v is not None} for b in response['blocks']],
        'categoryIds': sorted(c['id'] for c in card['categories']),
        'coverMediaId': (card['cover'] or {}).get('id'),
        'readingMinutes': None, **{k: response[k] for k in ('seoTitle', 'seoDescription')}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', required=True)
    parser.parse_args()
    settings = json.loads((RUNTIME / 'server-env.json').read_text())
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    api = Api('http://127.0.0.1:19998')
    lock = open(RUNTIME / 'server-blog-publication.lock', 'a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    source = sqlite3.connect((RUNTIME / 'server-by-state.sqlite').resolve().as_uri() + '?mode=ro', uri=True)
    records = source.execute("SELECT key,path,id,response FROM records WHERE key LIKE 'post:%' AND status='created' ORDER BY id").fetchall()
    if len(records) != 43:
        raise ValueError('Expected exactly 43 imported articles')
    path = RUNTIME / 'server-blog-publication.sqlite'
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
    os.close(fd)
    os.chmod(path, 0o600)
    db = sqlite3.connect(path)
    db.execute('PRAGMA synchronous=FULL')
    db.execute('CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,key TEXT NOT NULL,before_json TEXT NOT NULL,after_json TEXT,status TEXT NOT NULL)')
    db.commit()
    prepared = []
    for key, endpoint, ident, imported in records:
        current = api.request(endpoint + '/' + str(ident))
        saved = db.execute('SELECT before_json,after_json,status FROM articles WHERE id=?', (ident,)).fetchone()
        baseline = json.loads(saved[0] if saved else imported)
        if saved and saved[2] == 'published':
            if digest(own_snapshot(current)) != digest(own_snapshot(json.loads(saved[1]))):
                raise ValueError('Published article edited outside operation: ' + key)
        elif saved and saved[2] == 'pending' and current['article']['status'] == 'PUBLISHED':
            if payload(current) != payload(baseline) or current['article']['version'] != baseline['article']['version'] + 1:
                raise ValueError('Ambiguous interrupted publication: ' + key)
            db.execute("UPDATE articles SET after_json=?,status='published' WHERE id=?", (json.dumps(current), ident))
        elif digest(own_snapshot(current)) != digest(own_snapshot(baseline)):
            raise ValueError('Imported article edited outside migration: ' + key)
        if not saved:
            if current['article']['status'] != 'DRAFT' or current['article']['publishedAt'] is not None:
                raise ValueError('Expected an unscheduled draft: ' + key)
            db.execute("INSERT INTO articles VALUES(?,?,?,NULL,'prepared')", (ident, key, json.dumps(current)))
        body = payload(current)
        if not all(body[k] for k in ('excerpt', 'blocks', 'categoryIds', 'coverMediaId')):
            raise ValueError('Incomplete article: ' + key)
        prepared.append((key, endpoint, ident, current, body))
    db.commit()  # Durable backup of all 43 before the first server PUT.
    print('Backed up and checked 43 articles before publication.', flush=True)
    for key, endpoint, ident, before, body in prepared:
        status = db.execute('SELECT status FROM articles WHERE id=?', (ident,)).fetchone()[0]
        if status == 'published':
            continue
        db.execute("UPDATE articles SET status='pending' WHERE id=?", (ident,))
        db.commit()
        api.request(endpoint + '/' + str(ident), 'PUT', dict(body,
            status='PUBLISHED', publishedAt=None, version=before['article']['version']))
        after = api.request(endpoint + '/' + str(ident))
        if payload(after) != body or after['article']['status'] != 'PUBLISHED' or not after['article']['publishedAt']:
            raise ValueError('Publication readback mismatch: ' + key)
        db.execute("UPDATE articles SET after_json=?,status='published' WHERE id=?", (json.dumps(after), ident))
        db.commit()
        print('PUBLISHED', key, after['article']['slug'], flush=True)

    def public_read(route):
        opener = urllib.request.build_opener(NoRedirect())
        with opener.open(api.base + route, timeout=45) as response:
            return json.load(response)

    visible = public_read('/api/blog/articles?size=100')['items']
    if not {r[2] for r in records}.issubset({a['id'] for a in visible}):
        raise ValueError('Imported articles missing from anonymous public list')

    def verify_article(item):
        key, _, ident, before, body = item
        public = public_read('/api/blog/articles/' + urllib.parse.quote(body['slug'], safe=''))
        if public['article']['id'] != ident or public['article']['status'] != 'PUBLISHED' or payload(public) != body:
            raise ValueError('Public article mismatch: ' + key)

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(verify_article, prepared))
    print('Verified: 43 published articles, unchanged content, anonymous list and article endpoints.', flush=True)


if __name__ == '__main__':
    main()
