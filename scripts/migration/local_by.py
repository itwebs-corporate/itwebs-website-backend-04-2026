#!/usr/bin/env python3
"""Local entry point: python3 scripts/migration/local_by.py serve|preview|apply|verify.

Uses the separately prepared migration database/media containers on ports 25432/29000.
No production destination or credentials are accepted here. Never starts seed runners.
"""
import argparse
import json
import os
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile
import zipfile


def main():
    root = Path(__file__).resolve().parents[2]
    runtime = root / '.migration-local'
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['serve', 'preview', 'apply', 'verify'])
    parser.add_argument('--static-archive', type=Path, default=Path.home() / 'Downloads/itwebs-by-static.tar.gz')
    parser.add_argument('--update-owned', action='store_true')
    parser.add_argument('--skip-verification', action='store_true')
    args = parser.parse_args()
    if args.update_owned and args.command != 'apply':
        parser.error('--update-owned is available only with apply')
    settings = json.loads((runtime / 'local-env.json').read_text())
    if settings.get('JDBC_URL', '').split('/')[2] != 'localhost:25432' or settings.get('S3_ENDPOINT') != 'http://localhost:29000':
        raise ValueError('Local migration ports required')
    os.chdir(root)
    if args.command == 'serve':
        # Use an already built jar. This command neither builds nor runs tests.
        folder = Path(tempfile.mkdtemp(prefix='itwebs-local-runtime-'))
        lib = folder / 'lib'
        classes = folder / 'classes'
        lib.mkdir()
        classes.mkdir()
        with zipfile.ZipFile(root / 'target/site-cms-0.1.0.jar') as archive:
            for name in archive.namelist():
                if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):
                    (lib / Path(name).name).write_bytes(archive.read(name))
        classpath = f'{root / "target/classes"}:{lib}/*'
        subprocess.run(['javac', '--release', '21', '-cp', classpath, '-d', str(classes),
            str(Path(__file__).with_name('LocalMigrationApplication.java'))], check=True)
        os.environ.update(settings)
        os.environ['SERVER_ADDRESS'] = '127.0.0.1'
        os.execvp('java', ['java', '-cp', f'{classes}:{classpath}',
            'ru.itwebs.cms.LocalMigrationApplication', '--logging.level.root=WARN'])
    os.environ['CMS_ADMIN_EMAIL'] = settings['ADMIN_EMAIL']
    os.environ['CMS_ADMIN_PASSWORD'] = settings['ADMIN_PASSWORD']
    state = runtime / 'by-state.sqlite'
    if args.command == 'verify':
        from migrate_by import Migration
        options = argparse.Namespace(base_url='http://127.0.0.1:19999', source_container='itwebs-legacy-audit-20261026',
            source_db='legacy_main', docker='/Applications/Docker.app/Contents/Resources/bin/docker',
            static_archive=args.static_archive, state=state, apply=False, update_owned=False, skip_verification=False)
        Migration(options).verify()
    else:
        sys.argv = ['migrate_by.py', '--static-archive', str(args.static_archive), '--state', str(state)]
        if args.command == 'apply':
            sys.argv.append('--apply')
        if args.update_owned:
            sys.argv.append('--update-owned')
        if args.skip_verification:
            sys.argv.append('--skip-verification')
        runpy.run_path(str(Path(__file__).with_name('migrate_by.py')), run_name='__main__')


if __name__ == '__main__':
    main()
