#!/usr/bin/env python3
"""Produce a deterministic, bounded runtime manifest. No secrets or build files included."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REMOTE = ROOT / 'remote'
EXTENSIONS = {'.html', '.css', '.js', '.json', '.png', '.webp', '.jpg', '.woff2'}

def manifest():
    files = []
    for folder in ('ui', 'core'):
        for path in sorted((REMOTE / folder).rglob('*')):
            if not path.is_file():
                continue
            if path.is_symlink() or path.suffix not in EXTENSIONS:
                raise ValueError(f'Unsupported resource: {path}')
            content = path.read_bytes()
            if not 0 < len(content) <= 8 * 1024 * 1024:
                raise ValueError(f'Resource size invalid: {path}')
            files.append({'path': path.relative_to(REMOTE).as_posix(),
                          'sha256': hashlib.sha256(content).hexdigest(), 'bytes': len(content)})
    files.sort(key=lambda entry: entry['path'])
    if not 1 <= len(files) <= 100 or sum(f['bytes'] for f in files) > 32 * 1024 * 1024:
        raise ValueError('Release limits exceeded')
    canonical = ''.join(f"{f['path']}:{f['sha256']}:{f['bytes']}\n" for f in files)
    return {'schemaVersion': 1, 'minShellVersion': 1,
            'release': hashlib.sha256(canonical.encode()).hexdigest(), 'files': files}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    text = json.dumps(manifest(), indent=2) + '\n'
    target = REMOTE / 'manifest.json'
    if args.check:
        if not target.exists() or target.read_text() != text:
            raise SystemExit('Manifest is stale. Run python tools/release.py and commit it with the runtime files.')
        print('Release manifest verified')
    else:
        target.write_text(text)
        print('Wrote remote/manifest.json')
