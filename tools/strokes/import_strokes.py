#!/usr/bin/env python3
"""Import per-character stroke data from Hanzi Writer data into app assets.

Scans the vocabulary for unique Hanzi, looks each one up in a local copy of
https://github.com/chanind/hanzi-writer-data, and normalizes the result into
the schema owned by the app (see parseStrokeData and tools/strokes/README.md).

Only characters actually used by the vocabulary are imported, each exactly
once, under deterministic code-point filenames (U+72D7.json). Existing files
with identical content are left untouched.

Stroke data is Arphic-licensed (see THIRD_PARTY_NOTICES.md), unlike the
MIT-licensed Hanzi Writer code.

Usage:
  python tools/strokes/import_strokes.py --source-data /path/to/hanzi-writer-data/data
  python tools/strokes/import_strokes.py --source-data ... --dry-run
  python tools/strokes/import_strokes.py --source-data ... --char 狗
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

# CJK Unified Ideographs + Extension A. Punctuation, Latin, digits, and other
# symbols that appear in vocabulary entries are deliberately ignored.
HANZI_PATTERN = re.compile("[\u4e00-\u9fff\u3400-\u4dbf]")


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vocabulary", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/vocabulary.json")
    parser.add_argument("--strokes-dir", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/strokes")
    parser.add_argument("--source-data", type=Path,
                        default=os.environ.get("HANZI_WRITER_DATA"),
                        help="Path to hanzi-writer-data/data (or $HANZI_WRITER_DATA).")
    parser.add_argument("--char", help="Only import this single character.")
    parser.add_argument("--dry-run", action="store_true",
                        help="Report actions without writing files.")
    return parser.parse_args(argv)


def extract_hanzi(vocabulary: list) -> list:
    """Unique Hanzi across all cards, in first-appearance order."""
    seen: dict = {}
    for section in vocabulary:
        for card in section.get("cards", []):
            for char in HANZI_PATTERN.findall(card.get("hanzi", "")):
                seen.setdefault(char, None)
    return list(seen)


def stroke_filename(character: str) -> str:
    return f"U+{ord(character):04X}.json"


def normalize(character: str, upstream: dict) -> dict:
    """Reduce upstream data to the app-owned schema, validating as we go."""
    strokes = upstream.get("strokes", [])
    medians = upstream.get("medians", [])
    if not isinstance(strokes, list) or not strokes or not all(
            isinstance(s, str) and s.strip() for s in strokes):
        raise ValueError(f"upstream data for {character} has no usable strokes")
    if not isinstance(medians, list) or len(medians) != len(strokes) or not all(
            isinstance(m, list) and m and all(
                isinstance(p, list) and len(p) == 2 and all(
                    isinstance(n, (int, float)) for n in p) for p in m)
            for m in medians):
        raise ValueError(f"upstream data for {character} has invalid medians")
    return {"character": character, "strokes": strokes, "medians": medians}


def main(argv=None) -> int:
    args = parse_args(argv)
    if args.source_data is None:
        raise SystemExit("error: pass --source-data (a hanzi-writer-data/data directory) "
                         "or set $HANZI_WRITER_DATA")
    if not args.source_data.is_dir():
        raise SystemExit(f"error: source data is not a directory: {args.source_data}")
    vocabulary = json.loads(args.vocabulary.read_text(encoding="utf-8"))

    wanted = extract_hanzi(vocabulary)
    if args.char is not None:
        if args.char not in wanted:
            raise SystemExit(f"error: {args.char!r} is not used by the vocabulary")
        wanted = [args.char]

    imported = unchanged = 0
    missing: list = []
    invalid: list = []
    for character in wanted:
        target = args.strokes_dir / stroke_filename(character)
        try:
            upstream = json.loads((args.source_data / f"{character}.json").read_text(encoding="utf-8"))
        except FileNotFoundError:
            missing.append(character)
            print(f"  ! {character} → no upstream data")
            continue
        try:
            payload = (json.dumps(normalize(character, upstream), ensure_ascii=False, indent=2) + "\n").encode("utf-8")
        except ValueError as error:
            invalid.append(character)
            print(f"  ✗ {error}", file=sys.stderr)
            continue
        if args.dry_run:
            print(f"  · {character} → {target.name} (dry run)")
            continue
        if target.is_file() and target.read_bytes() == payload:
            unchanged += 1
            print(f"  ○ {character} → {target.name} (unchanged)")
            continue
        args.strokes_dir.mkdir(parents=True, exist_ok=True)
        target.write_bytes(payload)
        imported += 1
        print(f"  ✓ {character} → {target.name}")

    print(f"\nCharacters: {len(wanted)}")
    print(f"Imported:   {imported}")
    print(f"Unchanged:  {unchanged}")
    print(f"Missing:    {len(missing)}")
    if missing:
        print(f"Missing: {', '.join(missing)}", file=sys.stderr)
    if invalid:
        print(f"Invalid: {', '.join(invalid)}", file=sys.stderr)
    return 1 if (missing or invalid) else 0


if __name__ == "__main__":
    sys.exit(main())
