#!/usr/bin/env python3
"""Apply user-approved conversion to three imported ready-made offers only.

Full before/after responses are stored in the private technical SQLite journal.
No other fields or publications are changed.
"""
import argparse
from decimal import Decimal, ROUND_HALF_UP, ROUND_CEILING
import fcntl
import json
import os
from pathlib import Path
import sqlite3

from migrate_by import Api
from switch_server_content import catalog_payload
from copy_by import RU_PRICES

RUNTIME = Path(__file__).resolve().parents[2] / '.migration-local'
RATE = Decimal('27.3499')
RATE_SOURCE = 'https://www.cbr.ru/currency_base/daily/?UniDbQuery.Posted=True&UniDbQuery.To=27.10.2026'
OLD_BYN = {32: 1800, 33: 3500, 34: 4000}


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
    source = sqlite3.connect((RUNTIME / 'server-by-state.sqlite').resolve().as_uri() + '?mode=ro', uri=True)
    path = RUNTIME / 'server-offer-prices.sqlite'
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
    os.close(fd)
    os.chmod(path, 0o600)
    db = sqlite3.connect(path)
    db.execute('PRAGMA synchronous=FULL')
    db.execute('CREATE TABLE IF NOT EXISTS offers(id INTEGER PRIMARY KEY,old_id INTEGER NOT NULL,before_json TEXT NOT NULL,after_json TEXT,status TEXT NOT NULL)')
    db.execute('CREATE TABLE IF NOT EXISTS rate(source TEXT,date TEXT,rub_per_byn TEXT)')
    db.execute('CREATE TABLE IF NOT EXISTS rounding_backups(id INTEGER PRIMARY KEY,before_json TEXT NOT NULL,reason TEXT NOT NULL)')
    if not db.execute('SELECT count(*) FROM rate').fetchone()[0]:
        db.execute('INSERT INTO rate VALUES(?,?,?)', (RATE_SOURCE, '2026-10-27', str(RATE)))
    prepared = []
    for old, byn in OLD_BYN.items():
        exact = Decimal(byn) * RATE
        previous_price = ('от {:,} ₽'.format(int(exact.quantize(Decimal('1'), rounding=ROUND_HALF_UP)))).replace(',', ' ')
        amount = (exact / Decimal('10000')).quantize(Decimal('1'), rounding=ROUND_CEILING) * Decimal('10000')
        price = ('от {:,} ₽'.format(int(amount))).replace(',', ' ')
        if price != RU_PRICES[old]:
            raise ValueError('Manifest price differs from conversion')
        ident = source.execute('SELECT id FROM records WHERE key=? AND status=\'created\'', ('service:' + str(old),)).fetchone()[0]
        url = '/api/admin/catalog/services/' + str(ident)
        before = api.request(url)
        if before['entry']['price'] not in (None, '', previous_price, price):
            raise ValueError('Existing price protected: ' + url)
        db.execute("INSERT OR IGNORE INTO offers VALUES(?,?,?,NULL,'prepared')", (ident, old, json.dumps(before)))
        db.execute('INSERT OR IGNORE INTO rounding_backups VALUES(?,?,?)',
            (ident, json.dumps(before), 'User requested commercial prices rounded up to 10000 RUB'))
        desired = dict(catalog_payload(before), price=price, published=True)
        prepared.append((ident, url, before, desired))
    db.commit()  # Backup all three current responses before any server writes.
    for ident, url, before, desired in prepared:
        if catalog_payload(before) != desired:
            db.execute("UPDATE offers SET status='pending' WHERE id=?", (ident,))
            db.commit()
            api.request(url, 'PUT', dict(desired, version=before['entry']['version']))
        after = api.request(url)
        if catalog_payload(after) != desired:
            raise ValueError('Price/publication verification mismatch: ' + url)
        db.execute("UPDATE offers SET after_json=?,status='verified' WHERE id=?", (json.dumps(after), ident))
        db.commit()
        print('UPDATED', after['entry']['title'], desired['price'], 'published=true', flush=True)
    print('Verified 3 prices; all other content preserved.', flush=True)


if __name__ == '__main__':
    main()
