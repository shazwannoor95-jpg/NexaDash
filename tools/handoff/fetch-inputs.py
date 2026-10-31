#!/usr/bin/env python3
"""Fetch only selected handoff inputs, with recorded size/SHA256 verification."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import sys
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
BASE = 'https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/'

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()

def relative(value):
    path = PurePosixPath(value)
    if path.is_absolute() or '..' in path.parts or '\\' in value or not path.parts:
        raise ValueError('Unsafe input path')
    return path

def fetch(item, target, url):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_file() and target.stat().st_size == item['size_bytes'] and digest(target) == item['sha256']:
        print('Verified existing:', item['path'])
        return
    temporary = target.with_name(target.name + '.partial')
    request = urllib.request.Request(url, headers={'User-Agent': 'NexaDash-handoff'})
    try:
        total = 0
        h = hashlib.sha256()
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open('wb') as output:
            while True:
                block = response.read(1024 * 1024)
                if not block:
                    break
                total += len(block)
                if total > item['size_bytes']:
                    raise ValueError('Download exceeds recorded size')
                h.update(block)
                output.write(block)
        if total != item['size_bytes'] or h.hexdigest() != item['sha256']:
            raise ValueError('Download size/SHA256 mismatch')
        temporary.replace(target)
        print('Downloaded and verified:', item['path'])
    finally:
        temporary.unlink(missing_ok=True)

def main():
    manifest = json.loads((ROOT / 'docs/handoff-inputs-manifest.json').read_text())
    items = manifest['files'] + manifest['published_companions']
    groups = sorted({item['group'] for item in items})
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--list', action='store_true')
    parser.add_argument('--group', action='append', choices=groups)
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--out', type=Path, default=ROOT / '.handoff-inputs')
    args = parser.parse_args()
    if args.list:
        for group in groups:
            selected = [item for item in items if item['group'] == group]
            print(group, len(selected), 'files,', sum(x['size_bytes'] for x in selected), 'bytes')
        print('Original-input branch published:', manifest['binary_branch_published'])
        return 0
    if not args.all and not args.group:
        parser.error('Choose --list, --group or --all; nothing is downloaded implicitly')
    selected = [item for item in items if args.all or item['group'] in args.group]
    if any('url' not in item for item in selected) and not manifest['binary_branch_published']:
        print('Original inputs are staged locally, not published. Public upload is pending explicit owner confirmation after automatic approval rejection. No downloads attempted.', file=sys.stderr)
        return 2
    for item in selected:
        path = relative(item['path'])
        url = item.get('url') or BASE + manifest['input_commit'] + '/' + urllib.parse.quote(str(path), safe='/')
        if not url.startswith(BASE):
            raise ValueError('Input URL is outside the documented repository')
        target = args.out.resolve().joinpath(*path.parts)
        fetch(item, target, url)
    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError) as error:
        print('Handoff download failed:', error, file=sys.stderr)
        sys.exit(1)
