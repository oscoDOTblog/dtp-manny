#!/usr/bin/env python3
"""Validate generated TTS assets against the vocabulary and manifest.

Checks:
  - every audioPath points to a real, non-empty, MP3-looking file
  - no two cards share an audioPath
  - every MP3 in the audio dir belongs to a known card (orphans reported)
  - every manifest entry matches a vocabulary card (hanzi + content hash)
  - cards without audio and stale generations are reported

Exit 0 when nothing is broken or stale; exit 1 otherwise. Missing audio
and orphaned files are warnings, not failures.

Usage:
  python tools/tts/validate_tts.py
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_tts import (  # noqa: E402
    CARD_ID_PATTERN,
    REPO_ROOT,
    content_hash,
    looks_like_mp3,
)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vocabulary", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/vocabulary.json")
    parser.add_argument("--audio-dir", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/audio")
    parser.add_argument("--manifest", type=Path,
                        default=REPO_ROOT / "tools/tts/tts-manifest.json")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    vocabulary = json.loads(args.vocabulary.read_text(encoding="utf-8"))
    manifest = json.loads(args.manifest.read_text(encoding="utf-8")) if args.manifest.is_file() else {}

    cards = [c for s in vocabulary for c in s.get("cards", [])]
    by_id = {c["id"]: c for c in cards if isinstance(c.get("id"), str)}

    missing: list = []
    broken: list = []
    invalid_files: list = []
    stale: list = []
    seen_paths: dict = {}
    duplicates: list = []

    for card in cards:
        card_id = card.get("id", "?")
        audio_path = card.get("audioPath")
        if not audio_path:
            missing.append(card_id)
            continue
        if audio_path in seen_paths:
            duplicates.append(audio_path)
            continue
        seen_paths[audio_path] = card_id
        target = args.vocabulary.parent / audio_path
        if not target.is_file():
            broken.append(f"{card_id} -> {audio_path}")
            continue
        data = target.read_bytes()
        if not looks_like_mp3(data):
            invalid_files.append(f"{card_id} -> {audio_path}")

    orphans = sorted(
        f"audio/{p.name}" for p in args.audio_dir.glob("*.mp3")
        if f"audio/{p.name}" not in seen_paths
    ) if args.audio_dir.is_dir() else []

    manifest_issues: list = []
    for key, entry in manifest.items():
        card = by_id.get(key)
        if card is None:
            manifest_issues.append(f"{key}: no such card")
            continue
        if not isinstance(card.get("id"), str) or not CARD_ID_PATTERN.match(card["id"]):
            continue
        expected = content_hash(
            card.get("hanzi", ""),
            entry.get("voice", ""),
            entry.get("engine", ""),
            entry.get("language", ""),
        )
        if entry.get("hanzi") != card.get("hanzi") or entry.get("contentHash") != expected:
            stale.append(key)

    print("TTS Validation\n")
    print(f"Vocabulary cards:       {len(cards)}")
    print(f"Cards with audio:       {len(cards) - len(missing)}")
    print(f"Missing audio:          {len(missing)}")
    print(f"Broken references:      {len(broken)}")
    print(f"Invalid audio files:    {len(invalid_files)}")
    print(f"Duplicate audio paths:  {len(duplicates)}")
    print(f"Orphaned MP3s:          {len(orphans)}")
    print(f"Stale generations:      {len(stale)}")
    print(f"Manifest issues:        {len(manifest_issues)}")
    for label, items in (("Missing", missing), ("Broken", broken),
                         ("Invalid", invalid_files), ("Duplicate", duplicates),
                         ("Orphaned", orphans), ("Stale", stale),
                         ("Manifest", manifest_issues)):
        if items:
            print(f"\n{label}:")
            for item in items:
                print(f"- {item}")

    failed = broken or invalid_files or duplicates or stale or manifest_issues
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
