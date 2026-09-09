# Mandarin TTS pipeline

Build-time pronunciation generation. Amazon Polly synthesizes the audio on a
development machine; the Android app only plays the committed MP3s and never
touches AWS.

## Prerequisites

- Python 3 (standard library only, except for generation itself)
- `pip install boto3` (only needed for real generation, not `--dry-run`)
- AWS credentials via the normal boto3 chain: `AWS_ACCESS_KEY_ID` /
  `AWS_SECRET_ACCESS_KEY`, `~/.aws/credentials`, or an IAM role. Never commit
  credentials to this repository.

## Usage

```bash
python tools/tts/generate_tts.py                 # all cards missing audio
python tools/tts/generate_tts.py --section animals
python tools/tts/generate_tts.py --card animals-01
python tools/tts/generate_tts.py --force --card animals-01
python tools/tts/generate_tts.py --dry-run        # report only, no writes
```

Configuration (flags override environment):

| Flag / env      | Default  |
| --------------- | -------- |
| `--voice` / `POLLY_VOICE`   | `Zhiyu`  |
| `--engine` / `POLLY_ENGINE` | `neural` |
| `--language` / `POLLY_LANGUAGE` | `cmn-CN` |
| `--region` / `POLLY_REGION` | boto3 default |

Note: the `neural` engine with the `Zhiyu` voice is not available in every
region (`us-east-2` rejects it); `us-east-1` is confirmed working.

## Rules

- The synthesis source is always the complete card `hanzi` (word context
  matters for polyphonic characters). Pinyin and English are never sent.
- Filenames are deterministic card ids: `animals-01.mp3`, never Chinese text.
- `audioPath` and the manifest are updated only after a verified MP3 exists.
- Existing valid audio is never overwritten without `--force`.
- A stale manifest hash (changed hanzi/voice/engine/language) regenerates.
- Exit code is 1 when anything fails or any card is invalid.

## Layout

- `app/src/main/assets/vocabulary.json` — source of truth, updated in place
  (2-space indent, raw UTF-8, trailing newline are preserved).
- `app/src/main/assets/audio/` — generated MP3s, referenced as `audio/<id>.mp3`.
- `tools/tts/tts-manifest.json` — harness-only provenance record
  (provider, voice, engine, language, format, content hash). Not read by the
  app and not shipped as learner content.

Known limitation: parenthesized variant hanzi such as `人（亻）` is sent to
Polly verbatim. Per-card synthesis overrides are deliberately out of scope
until a real mispronunciation is encountered.
