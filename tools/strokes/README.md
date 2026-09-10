# Hanzi stroke-data pipeline

Build-time import of per-character stroke-order data. The Android app only
reads the committed JSON assets and never touches the network.

## Source data

Upstream: https://github.com/chanind/hanzi-writer-data (`data/<char>.json`,
derived from Make Me a Hanzi). It is **not** vendored here; obtain a copy and
point the importer at its `data/` directory:

```bash
git clone --depth 1 https://github.com/chanind/hanzi-writer-data /tmp/hanzi-writer-data
python tools/strokes/import_strokes.py --source-data /tmp/hanzi-writer-data/data
```

`$HANZI_WRITER_DATA` is accepted in place of `--source-data`.

## Usage

```bash
python tools/strokes/import_strokes.py --source-data .../data   # missing characters only
python tools/strokes/import_strokes.py --source-data ... --char 狗
python tools/strokes/import_strokes.py --source-data ... --dry-run
```

## Rules

- Extraction finds unique Hanzi (CJK Unified Ideographs + Extension A) across
  all cards, in first-appearance order. Parenthesized variants (人（亻）),
  punctuation, Latin, and digits are handled by the same rule: only Hanzi
  code points are extracted.
- One file per character, named by code point: `U+72D7.json`. Deterministic
  and filesystem-safe.
- Files match exactly the schema `parseStrokeData` accepts:
  `{"character", "strokes", "medians"}` with one non-empty median per stroke.
  Upstream `radStrokes` are dropped.
- Imports are idempotent: byte-identical files are left untouched and
  reported as unchanged. Exit code is 1 when any character is missing
  upstream or fails validation.

## Licensing

The character data is Arphic-licensed, unlike the MIT Hanzi Writer code.
Attribution lives in `THIRD_PARTY_NOTICES.md` at the repository root.
