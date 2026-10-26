#!/usr/bin/env python3
"""Content-only migration through the existing admin API. Default: read-only preview.

Use --source-container/--source-db for a separately restored legacy database.
Credentials come from CMS_ADMIN_EMAIL/CMS_ADMIN_PASSWORD, never from arguments.
State is operational SQLite, not a report. Keep it to resume and attach missing media.
Only loopback destinations are supported in this local migration tool.
"""
import argparse
import base64
import hashlib
import json
import mimetypes
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import tarfile
import urllib.error
import urllib.parse
import urllib.request

from copy_by import build_manifest


def encoded(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()


def digest(value):
    return hashlib.sha256(encoded(value)).hexdigest()


def own_snapshot(response):
    """Do not mistake a referenced entity's changed version for an edit of this entity."""
    value = json.loads(json.dumps(response))
    card = value.get('entry') or value.get('article')
    if card:
        card['categories'] = sorted(item['id'] for item in card.get('categories', []))
        for field in ('cover', 'mobileCover'):
            if field in card:
                card[field] = (card[field] or {}).get('id')
    if 'examples' in value:
        value['examples'] = [item['id'] for item in value['examples']]
    if 'images' in value:
        value['images'] = sorted(item['id'] for item in value['images'])
    if 'linkedService' in value:
        value['linkedService'] = (value['linkedService'] or {}).get('id')
    if value.get('service'):
        value['service'] = {k: v for k, v in value['service'].items()
            if k not in {'faq', 'stack', 'process', 'tasks', 'audience', 'cooperation'}}
        for field in ('stackDescription', 'audienceDescription'):
            if value['service'].get(field) == '':
                value['service'][field] = None  # Existing admin represents an empty textarea this way.
    if 'image' in value:
        value['image'] = (value['image'] or {}).get('id')
    return value


class Api:
    def __init__(self, base):
        parsed = urllib.parse.urlsplit(base)
        if parsed.scheme != 'http' or parsed.hostname not in ('127.0.0.1', 'localhost', '::1') or parsed.path or parsed.query or parsed.fragment or parsed.username:
            raise ValueError('Only an explicit local http://host:port destination is allowed')
        self.base = base.rstrip('/')
        credentials = os.environ['CMS_ADMIN_EMAIL'] + ':' + os.environ['CMS_ADMIN_PASSWORD']
        self.auth = 'Basic ' + base64.b64encode(credentials.encode()).decode()
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, path, method='GET', body=None, content_type='application/json', raw=False):
        data = body if isinstance(body, bytes) else encoded(body) if body is not None else None
        request = urllib.request.Request(self.base + path, data=data, method=method,
            headers={'Authorization': self.auth, 'Content-Type': content_type})
        try:
            with self.opener.open(request, timeout=45) as response:
                data = response.read()
                return data if raw else json.loads(data) if data else None
        except urllib.error.HTTPError as error:
            # Never log the Authorization header or credentials.
            message = error.read().decode(errors='replace')[:700]
            raise RuntimeError(f'{method} {path}: HTTP {error.code}: {message}') from None

    def listing(self, path):
        rows, page = [], 0
        while True:
            result = self.request(path + f'?page={page}&size=100')
            if isinstance(result, list):
                return result
            rows.extend(result['items'])
            page += 1
            if page >= result['totalPages']:
                return rows


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise RuntimeError('Redirect refused: credentials must stay on the selected local API')


class Migration:
    def __init__(self, args):
        self.args, self.api = args, Api(args.base_url)
        self.manifest = build_manifest(read_source(args))
        self.media = {}
        with tarfile.open(args.static_archive) as archive:
            for member in archive.getmembers():
                if member.isfile() and not member.issym() and not member.islnk():
                    if member.name.startswith('/') or '..' in Path(member.name).parts:
                        raise ValueError('Unsafe archive member')
                    if member.size > 15 * 1024 * 1024:
                        raise ValueError('File exceeds CMS upload limit')
                    if member.name in self.media:
                        raise ValueError('Duplicate archive path')
                    self.media[member.name] = archive.extractfile(member).read()
        self.cache, self.ids = {}, {}
        self.db = None
        if args.apply:
            state = Path(args.state)
            state.parent.mkdir(parents=True, exist_ok=True)
            # Exclusive process lock also covers the POST -> journal-commit interval.
            import fcntl
            self.lock = open(str(state) + '.lock', 'a')
            fcntl.flock(self.lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            self.db = sqlite3.connect(state)
            os.chmod(state, 0o600)
            self.db.execute('PRAGMA synchronous=FULL')
            self.db.execute('CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY,value TEXT NOT NULL)')
            self.db.execute('CREATE TABLE IF NOT EXISTS records (key TEXT PRIMARY KEY,path TEXT NOT NULL,id INTEGER,payload_hash TEXT NOT NULL,response TEXT, status TEXT NOT NULL)')
            previous = self.db.execute('SELECT value FROM meta WHERE key=?', ('target',)).fetchone()
            if previous and previous[0] != self.api.base:
                raise ValueError('State belongs to another API destination')
            self.db.execute('INSERT OR IGNORE INTO meta VALUES (?,?)', ('target', self.api.base))
            self.db.commit()
            self.ids.update({key: item_id for key, item_id in self.db.execute('SELECT key,id FROM records WHERE id IS NOT NULL')})
        elif args.state.exists():
            self.db = sqlite3.connect(args.state.resolve().as_uri() + '?mode=ro', uri=True)
            target = self.db.execute('SELECT value FROM meta WHERE key=?', ('target',)).fetchone()
            if not target or target[0] != self.api.base:
                raise ValueError('State belongs to another API destination')
            self.ids.update({key: item_id for key, item_id in self.db.execute('SELECT key,id FROM records WHERE id IS NOT NULL')})

    def existing(self, path):
        if path not in self.cache:
            self.cache[path] = self.api.listing(path)
        return self.cache[path]

    def mapped(self, ref):
        if ref is None:
            return None
        if ref not in self.ids:
            raise ValueError('Unresolved dependency: ' + ref)
        return self.ids[ref]

    def persist(self, key, path, item_id, payload, response, status):
        self.db.execute('INSERT OR REPLACE INTO records VALUES (?,?,?,?,?,?)',
            (key, path, item_id, digest(payload), json.dumps(response, ensure_ascii=False) if response is not None else None, status))
        self.db.commit()

    def create(self, key, path, payload, natural_key='slug', linking=False):
        saved = self.db.execute('SELECT id,payload_hash,response,status FROM records WHERE key=?', (key,)).fetchone() if self.db else None
        if saved and saved[0] is not None:
            current = self.api.request(path + '/' + str(saved[0]))
            if digest(own_snapshot(current)) != digest(own_snapshot(json.loads(saved[2]))):
                raise ValueError(f'{key}: record edited since migration; refusing overwrite')
            if saved[1] != digest(payload):
                if not self.args.apply:
                    print('WOULD UPDATE OWNED', key, '(requires --update-owned for content changes)')
                    self.ids[key] = saved[0]
                    return saved[0]
                link_only = linking and matches_payload(current, dict(payload, caseIds=[r['id'] for r in current.get('examples', [])]))
                if (not self.args.update_owned and not link_only) or saved[3] != 'created':
                    raise ValueError(f'{key}: prepared content changed; --update-owned required for owned records')
                version = (current.get('entry') or current.get('article') or current)['version']
                result = self.api.request(path + '/' + str(saved[0]), 'PUT', dict(payload, version=version))
                self.persist(key, path, saved[0], payload, result, 'created')
                print('UPDATED', key)
            self.ids[key] = saved[0]
            return saved[0]
        matches = [r for r in self.existing(path) if r.get(natural_key) == payload[natural_key]]
        if 'service-components' in path:
            # The API deduplicates by kind + full content. The same question/title
            # can legitimately have different answers; a label is not an identity.
            semantic = {k: v for k, v in payload.items() if k not in {'sortOrder', 'version'}}
            matches = [r for r in matches if matches_payload(r, semantic)]
        if matches:
            row = matches[0]
            if len(matches) != 1:
                raise ValueError(f'{key}: ambiguous existing records')
            # If POST completed before journal commit, verify every requested field.
            if saved and saved[3] == 'pending':
                result = self.api.request(path + '/' + str(row['id']))
                if not matches_payload(result, payload):
                    raise ValueError(f'{key}: unresolved interrupted POST; manual reconciliation needed')
                if self.args.apply:
                    self.persist(key, path, row['id'], payload, result, 'created')
            elif '/categories' in path or 'service-components' in path:
                result = self.api.request(path + '/' + str(row['id']))
                compared = {k: v for k, v in payload.items() if k not in {'sortOrder', 'version'}} if 'service-components' in path else payload
                if not matches_payload(result, compared):
                    raise ValueError(f'{key}: pre-existing dictionary differs; refusing overwrite')
                if self.args.apply:
                    self.persist(key, path, row['id'], payload, result, 'reused')
            else:
                raise ValueError(f'{key}: address is already occupied; existing record is protected')
            self.ids[key] = row['id']
            return row['id']
        if not self.args.apply:
            item_id = -(len(self.ids) + 1)
            self.ids[key] = item_id
            return item_id
        self.persist(key, path, None, payload, None, 'pending')
        result = self.api.request(path, 'POST', payload)
        row = result.get('entry') or result.get('article') or result
        self.persist(key, path, row['id'], payload, result, 'created')
        self.cache.setdefault(path, []).append(row)
        self.ids[key] = row['id']
        print('CREATED', key)
        return row['id']

    def image(self, path, alt):
        if not path:
            return None
        if path not in self.media:
            candidates = [name for name in self.media if Path(name).name == Path(path).name]
            if len(candidates) > 1:
                raise ValueError('Ambiguous image filename: ' + path)
            if not candidates:
                return None
            path = candidates[0]
        data = self.media[path]
        sha = hashlib.sha256(data).hexdigest()
        key = 'media:' + sha
        if key in self.ids:
            return self.ids[key]
        suffix = Path(path).suffix.lower()
        filename = 'legacy-by-' + sha + suffix
        candidates = [r for r in self.existing('/api/admin/media') if r['originalName'] == filename]
        if candidates:
            if len(candidates) != 1 or self.api.request('/api/media/' + str(candidates[0]['id']), raw=True) != data:
                raise ValueError('Media recovery collision: ' + path)
            self.ids[key] = candidates[0]['id']
            return self.ids[key]
        if not self.args.apply:
            self.ids[key] = -(len(self.ids) + 1)
            return self.ids[key]
        boundary = 'itwebs-' + os.urandom(12).hex()
        mime = mimetypes.guess_type(filename)[0] or 'application/octet-stream'
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="altText"\r\n\r\n{alt}\r\n'
            f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
            f'Content-Type: {mime}\r\n\r\n').encode() + data + f'\r\n--{boundary}--\r\n'.encode()
        result = self.api.request('/api/admin/media', 'POST', body, 'multipart/form-data; boundary=' + boundary)
        if not self.args.skip_verification and self.api.request('/api/media/' + str(result['id']), raw=True) != data:
            raise ValueError('Uploaded media differs: ' + path)
        self.cache['/api/admin/media'].append(result)
        self.ids[key] = result['id']
        if self.db:
            self.persist(key, '/api/admin/media', result['id'], {'sha256': sha}, result, 'created')
        return result['id']

    def run(self):
        print('Destination:', self.api.base, 'mode:', 'APPLY DRAFTS' if self.args.apply else 'READ-ONLY PREVIEW')
        for item in self.manifest['categories']:
            self.create(item['key'], item['path'], item['body'])
        for item in self.manifest['components']:
            self.create(item['key'], item['path'], item['body'], natural_key='title')
        # Services precede cases; examples are attached in a later explicit pass if approved.
        service_bodies = []
        for item in self.manifest['entries']:
            body = dict(item['body'])
            body['categoryIds'] = [self.mapped(r) for r in item.get('categories', [])]
            body['coverMediaId'] = self.image(item.get('image'), body['title'])
            body['published'] = False
            if item.get('examples'):
                body['caseIds'] = [self.ids[ref] for ref in item['examples'] if ref in self.ids]
            if body.get('serviceSelections') is not None:
                body['serviceSelections'] = {k: [{'componentId': self.mapped(ref), 'override': None} for ref in refs]
                    for k, refs in item['selections'].items()}
            if body.get('project') is not None and item.get('service'):
                body['project'] = dict(body['project'], serviceId=self.mapped(item['service']))
            self.create(item['key'], item['path'], body, linking=bool(item.get('examples')))
            if item.get('examples'):
                service_bodies.append((item, body))
        for item, body in service_bodies:
            linked = dict(body, caseIds=[self.mapped(ref) for ref in item['examples']])
            self.create(item['key'], item['path'], linked, linking=True)
        for item in self.manifest['articles']:
            body = dict(item['body'], status='DRAFT')
            body['categoryIds'] = [self.mapped(r) for r in item['categories']]
            body['coverMediaId'] = self.image(item.get('image'), body['title'])
            self.create(item['key'], '/api/admin/blog/articles', body)
        print('Prepared:', len(self.manifest['entries']), 'catalog entries,', len(self.manifest['articles']), 'articles,',
            len(self.manifest['components']), 'components; no published content or existing pages changed')
        for note in self.manifest['holds']:
            print('HELD:', note)
        if self.args.apply and not self.args.skip_verification:
            self.verify()

    def verify(self):
        checked = 0
        for key, path, item_id, response, status in self.db.execute('SELECT key,path,id,response,status FROM records WHERE id IS NOT NULL'):
            if key.startswith('media:'):
                data = self.api.request('/api/media/' + str(item_id), raw=True)
                if hashlib.sha256(data).hexdigest() != key.split(':', 1)[1]:
                    raise ValueError('Media checksum mismatch: ' + key)
                continue
            current = self.api.request(path + '/' + str(item_id))
            if digest(own_snapshot(current)) != digest(own_snapshot(json.loads(response))):
                raise ValueError('Verification mismatch: ' + key)
            card = current.get('entry') or current.get('article')
            if card and (card.get('published') or card.get('status') == 'PUBLISHED'):
                raise ValueError('Unexpected publication: ' + key)
            checked += 1
        print('Verified through API:', checked, 'records and all uploaded image checksums')


def matches_payload(response, payload):
    """Compare request fields with the existing read DTO; no fuzzy adoption."""
    card = response.get('entry') or response.get('article') or response
    def subset(actual, desired):
        if isinstance(desired, dict):
            return isinstance(actual, dict) and all(subset(actual.get(k), v) for k, v in desired.items())
        return actual == desired
    for key, value in payload.items():
        if key == 'version':
            continue
        if key == 'readingMinutes' and value is None:
            continue  # The API calculates it from the article text.
        if key == 'coverMediaId':
            actual = (card.get('cover') or {}).get('id')
        elif key == 'mobileCoverMediaId':
            actual = (card.get('mobileCover') or {}).get('id')
        elif key == 'categoryIds':
            actual = sorted(r['id'] for r in card.get('categories', []))
            value = sorted(value)
        elif key == 'caseIds':
            actual = [r['id'] for r in response.get('examples', [])]
        elif key == 'mediaId':
            actual = (response.get('image') or {}).get('id')
        elif key == 'service':
            # Read DTO expands dictionary selections; write DTO deliberately requires empty lists.
            computed = {'faq', 'stack', 'process', 'tasks', 'audience', 'cooperation'}
            actual = {k: v for k, v in response[key].items() if k not in computed}
            value = {k: v for k, v in value.items() if k not in computed}
        elif key in response:
            actual = response[key]
        else:
            actual = card.get(key)
        if not subset(actual, value):
            return False
    return True


def read_source(args):
    # Explicit allowlist; no users, employees, leads, requests, tokens or backup tables.
    tables = ('services', 'groups', 'cases', 'posts', 'stages', 'faqs', 'decisions', 'service_seos')
    result = {}
    for table in tables:
        query = f"SELECT coalesce(json_agg(t ORDER BY id),'[]') FROM public.\"{table}\" t"
        output = subprocess.check_output([args.docker, 'exec', args.source_container, 'psql', '-U', 'postgres',
            '-d', args.source_db, '-At', '-c', query], text=True)
        result[table] = json.loads(output)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:19999')
    parser.add_argument('--source-container', default='itwebs-legacy-audit-20261026')
    parser.add_argument('--source-db', default='legacy_main')
    parser.add_argument('--docker', default='/Applications/Docker.app/Contents/Resources/bin/docker')
    parser.add_argument('--static-archive', type=Path, required=True)
    parser.add_argument('--state', type=Path, required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--update-owned', action='store_true', help='Update only migration-owned, unchanged records')
    parser.add_argument('--skip-verification', action='store_true', help='Fill drafts now; defer the separate QA pass')
    args = parser.parse_args()
    if args.update_owned and not args.apply:
        parser.error('--update-owned requires --apply')
    try:
        Migration(args).run()
    except Exception as error:
        print('STOPPED:', str(error), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
