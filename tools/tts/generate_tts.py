#!/usr/bin/env python3
"""Build-time Mandarin TTS generation via Amazon Polly.

Reads the bundled vocabulary, synthesizes missing pronunciation clips with
Amazon Polly, saves them as local MP3 assets, and records the result in the
vocabulary metadata plus a generation manifest.

The Android app never calls Polly; it only plays the committed MP3s, so the
app stays offline and credential-free. AWS credentials come from the normal
boto3 chain (environment, ~/.aws, IAM role) -- never from this repository.

Idempotency:
  audioPath set + file exists + manifest hash matches -> SKIP
  audioPath set + file exists, but no manifest entry   -> SKIP (unrecorded)
  audioPath set + file missing                         -> WARN + regenerate
  manifest entry exists but inputs changed (stale hash) -> regenerate
  --force regenerates the selected cards unconditionally.

Usage:
  python tools/tts/generate_tts.py                 # all cards missing audio
  python tools/tts/generate_tts.py --section animals
  python tools/tts/generate_tts.py --card dog
  python tools/tts/generate_tts.py --force --card dog
  python tools/tts/generate_tts.py --dry-run        # report only, no writes
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

PROVIDER = "amazon-polly"
DEFAULT_LANGUAGE = "cmn-CN"
DEFAULT_FORMAT = "mp3"

CARD_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]+$")


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vocabulary", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/vocabulary.json")
    parser.add_argument("--audio-dir", type=Path,
                        default=REPO_ROOT / "app/src/main/assets/audio")
    parser.add_argument("--manifest", type=Path,
                        default=REPO_ROOT / "tools/tts/tts-manifest.json")
    parser.add_argument("--section", help="Only generate for this section id.")
    parser.add_argument("--card", help="Only generate for this card id.")
    parser.add_argument("--force", action="store_true",
                        help="Regenerate even valid existing audio.")
    parser.add_argument("--dry-run", action="store_true",
                        help="Report actions without calling Polly or writing files.")
    parser.add_argument("--voice", default=os.environ.get("POLLY_VOICE", "Zhiyu"))
    parser.add_argument("--engine", default=os.environ.get("POLLY_ENGINE", "neural"))
    parser.add_argument("--language", default=os.environ.get("POLLY_LANGUAGE", DEFAULT_LANGUAGE))
    parser.add_argument("--region", default=os.environ.get("POLLY_REGION"))
    return parser.parse_args(argv)


def load_json(path: Path, what: str):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        if what == "manifest":
            return {}
        raise SystemExit(f"error: {what} not found: {path}")
    except json.JSONDecodeError as error:
        raise SystemExit(f"error: invalid {what} {path}: {error}")


def content_hash(hanzi: str, voice: str, engine: str, language: str) -> str:
    """Hash of the synthesis inputs; a mismatch means the audio is stale."""
    return hashlib.sha256(f"{hanzi}|{voice}|{engine}|{language}".encode("utf-8")).hexdigest()


def validate_card(section_id: str, card: dict, seen_ids: set) -> str | None:
    """Return an error message, or None when the card is usable."""
    card_id = card.get("id", "")
    if not isinstance(card_id, str) or not CARD_ID_PATTERN.match(card_id):
        return f"card has an invalid id: {card_id!r}"
    if card_id in seen_ids:
        return f"duplicate card id: {card_id}"
    seen_ids.add(card_id)
    if card.get("sectionId") != section_id:
        return f"card {card_id}: sectionId mismatch"
    hanzi = card.get("hanzi", "")
    if not isinstance(hanzi, str) or not hanzi.strip():
        return f"card {card_id}: missing hanzi"
    audio_path = card.get("audioPath")
    if audio_path is not None and (not isinstance(audio_path, str) or not audio_path.strip()):
        return f"card {card_id}: invalid audioPath"
    return None


def make_polly_client(region: str | None):
    try:
        import boto3
    except ImportError:
        raise SystemExit("error: boto3 is not installed (pip install boto3). "
                         "AWS credentials come from the standard boto3 chain.")
    try:
        return boto3.client("polly", region_name=region) if region else boto3.client("polly")
    except Exception as error:
        raise SystemExit(f"error: cannot create Polly client: {error}")


def synthesize(client, text: str, voice: str, engine: str, language: str) -> bytes:
    response = client.synthesize_speech(
        Text=text,
        TextType="text",
        VoiceId=voice,
        Engine=engine,
        LanguageCode=language,
        OutputFormat="mp3",
    )
    return response["AudioStream"].read()


def looks_like_mp3(data: bytes) -> bool:
    if len(data) == 0:
        return False
    if data[:3] == b"ID3":
        return True
    return len(data) > 1 and data[0] == 0xFF and (data[1] & 0xE0) == 0xE0


def decide(card: dict, manifest: dict, audio_dir: Path, args) -> tuple[str, str, str]:
    """Return (action, filename, audio_path) for a card.

    Actions: skip, skip-unrecorded, generate, regenerate-broken,
    regenerate-stale, force.
    """
    filename = f"{card['id']}.mp3"
    audio_path = f"{audio_dir.name}/{filename}"
    target = audio_dir / filename
    entry = manifest.get(card["id"])

    if args.force:
        return ("force", filename, audio_path)
    if card.get("audioPath"):
        if not target.is_file():
            return ("regenerate-broken", filename, audio_path)
        if entry is None:
            return ("skip-unrecorded", filename, card["audioPath"])
        current = content_hash(card["hanzi"], args.voice, args.engine, args.language)
        if entry.get("contentHash") != current or entry.get("hanzi") != card["hanzi"]:
            return ("regenerate-stale", filename, audio_path)
        return ("skip", filename, card["audioPath"])
    return ("generate", filename, audio_path)


def generate_targets(vocabulary: list, manifest: dict, audio_dir: Path, args):
    """Yield (section_title, card, action, filename, audio_path) in file order."""
    seen_ids: set = set()
    found_section = args.section is None
    found_card = args.card is None
    for section in vocabulary:
        section_id = section.get("id", "")
        if args.section is not None and section_id != args.section:
            continue
        found_section = True
        for card in section.get("cards", []):
            if args.card is not None and card.get("id") != args.card:
                continue
            found_card = True
            error = validate_card(section_id, card, seen_ids)
            if error is not None:
                yield (section.get("title", section_id), card, "invalid", "", "")
                print(f"  ✗ {error}", file=sys.stderr)
                continue
            action, filename, audio_path = decide(card, manifest, audio_dir, args)
            yield (section.get("title", section_id), card, action, filename, audio_path)
    if not found_section:
        raise SystemExit(f"error: unknown section: {args.section}")
    if not found_card:
        raise SystemExit(f"error: unknown card: {args.card}")


def write_json(path: Path, data) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main(argv=None) -> int:
    args = parse_args(argv)
    vocabulary = load_json(args.vocabulary, "vocabulary")
    manifest = load_json(args.manifest, "manifest")

    client = None
    counts = {"generated": 0, "skipped": 0, "stale": 0, "broken": 0, "failed": 0, "invalid": 0}
    unrecorded: list = []
    current_section = None
    vocab_changed = False
    manifest_changed = False

    for title, card, action, filename, audio_path in generate_targets(vocabulary, manifest, args.audio_dir, args):
        if title != current_section:
            current_section = title
            print(f"\n{title}")
        label = card.get("hanzi", card.get("id", "?"))

        if action == "invalid":
            counts["invalid"] += 1
            continue
        if action in ("skip", "skip-unrecorded"):
            counts["skipped"] += 1
            if action == "skip-unrecorded":
                unrecorded.append(card["id"])
            print(f"  ○ {label} → already exists, skipped")
            continue

        if args.dry_run:
            counts["generated"] += 1
            print(f"  · {label} → {filename} (dry run)")
            continue

        if client is None:
            client = make_polly_client(args.region)
        try:
            # The complete hanzi is the synthesis source: pronunciation can
            # depend on word context (e.g. polyphonic characters), so neither
            # pinyin nor English is ever sent to Polly.
            data = synthesize(client, card["hanzi"], args.voice, args.engine, args.language)
            if not looks_like_mp3(data):
                raise ValueError("Polly returned data that does not look like MP3")
            args.audio_dir.mkdir(parents=True, exist_ok=True)
            (args.audio_dir / filename).write_bytes(data)
        except Exception as error:
            counts["failed"] += 1
            print(f"  ✗ {label} → FAILED: {error}", file=sys.stderr)
            continue

        # Metadata is updated only after a verified audio file exists.
        card["audioPath"] = audio_path
        vocab_changed = True
        manifest[card["id"]] = {
            "hanzi": card["hanzi"],
            "file": audio_path,
            "provider": PROVIDER,
            "voice": args.voice,
            "engine": args.engine,
            "language": args.language,
            "format": DEFAULT_FORMAT,
            "contentHash": content_hash(card["hanzi"], args.voice, args.engine, args.language),
        }
        manifest_changed = True
        if action == "regenerate-stale":
            counts["stale"] += 1
            print(f"  ↻ {label} → {filename} (inputs changed, regenerated)")
        elif action == "regenerate-broken":
            counts["broken"] += 1
            print(f"  ⚠ {label} → {filename} (missing file, regenerated)")
        elif action == "force":
            counts["generated"] += 1
            print(f"  ✓ {label} → {filename} (forced)")
        else:
            counts["generated"] += 1
            print(f"  ✓ {label} → {filename}")

    if vocab_changed:
        write_json(args.vocabulary, vocabulary)
    if manifest_changed:
        write_json(args.manifest, manifest)

    print(f"\nGenerated: {counts['generated']}")
    print(f"Skipped:   {counts['skipped']}")
    print(f"Stale:     {counts['stale']}")
    print(f"Broken:    {counts['broken']}")
    print(f"Failed:    {counts['failed']}")
    if unrecorded:
        print(f"Unrecorded (valid audio, no manifest entry): {', '.join(unrecorded)}")
    if counts["invalid"]:
        print(f"Invalid:   {counts['invalid']}", file=sys.stderr)
    return 1 if (counts["failed"] or counts["invalid"]) else 0


if __name__ == "__main__":
    sys.exit(main())
