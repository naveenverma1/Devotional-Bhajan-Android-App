#!/usr/bin/env python3
"""
align_audio.py -- derive per-verse start times for a chalisa's bundled MP3.

Pipeline
    1. faster-whisper (Hindi) transcribes the MP3 with word timestamps.
    2. The transcript words and the verse text from content.json are both
       normalised to a bare Devanagari consonant/vowel stream (no
       punctuation, dandas, nukta, chandrabindu/anusvara differences).
    3. difflib aligns the two character streams; each verse's start time
       is the timestamp of the first transcript word whose characters land
       inside that verse's span. Verses that got no confident match are
       interpolated between their neighbours so the sequence is monotonic.
    4. Result is written as `startMs` on each verse in content.json (or to
       a side JSON with --out for inspection).

Usage
    python scripts/align_audio.py --chalisa hanuman_chalisa
    python scripts/align_audio.py --chalisa sunderkand --model large-v3 --device cuda

The transcript is cached next to the MP3 (`<name>.whisper.json`) so you
can iterate on the alignment without re-running Whisper.
"""

from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "app/src/main/assets/content.json"
RAW = ROOT / "app/src/main/res/raw"
CACHE_DIR = ROOT / "scripts" / ".cache"

MIN_RUN = 3

# Characters that don't carry phonetic weight for alignment purposes.
_STRIP = re.compile(r"[\s\u0964\u0965\u0970\u0971.,;:!?\-–—()\[\]\"'“”‘’|॰0-9०-९]+")


def normalise(text: str) -> str:
    """Collapse Devanagari text to a comparable consonant/vowel stream."""
    t = unicodedata.normalize("NFC", text)
    t = _STRIP.sub("", t)
    t = t.replace("\u093c", "")  # nukta
    t = t.replace("\u0901", "\u0902")  # chandrabindu -> anusvara
    t = t.replace("\u094d", "")  # virama (halant) -- sung text often drops it
    return t


def transcribe(mp3: Path, model_name: str, device: str) -> list[dict]:
    # Never inside res/raw -- AAPT rejects anything that isn't a resource.
    CACHE_DIR.mkdir(exist_ok=True)
    cache = CACHE_DIR / f"{mp3.stem}.{model_name}.whisper.json"
    if cache.exists():
        print(f"-> Using cached transcript {cache}")
        return json.loads(cache.read_text(encoding="utf-8"))

    from faster_whisper import WhisperModel  # imported lazily: heavy

    compute = "float16" if device == "cuda" else "int8"
    print(f"-> Loading whisper '{model_name}' on {device} ({compute}) ...")
    model = WhisperModel(model_name, device=device, compute_type=compute)
    print(f"-> Transcribing {mp3.name} ... (this can take a while)")
    # No VAD: sung / chanted audio over a tanpura bed gets classified as
    # "not speech" and whole minutes vanish. Hallucinations are tolerated
    # by the fuzzy alignment step instead.
    segments, _info = model.transcribe(
        str(mp3),
        language="hi",
        word_timestamps=True,
        vad_filter=False,
        beam_size=5,
        condition_on_previous_text=False,
        no_speech_threshold=0.9,
        compression_ratio_threshold=3.0,
    )
    words: list[dict] = []
    for seg in segments:
        for w in seg.words or []:
            words.append({"word": w.word, "start": w.start, "end": w.end, "p": w.probability})
        print(f"   [{seg.start:8.1f}s] {seg.text.strip()[:70]}", flush=True)
    cache.write_text(json.dumps(words, ensure_ascii=False, indent=0), encoding="utf-8")
    print(f"-> {len(words)} words; cached to {cache}")
    return words


def align(words: list[dict], verses: list[dict]) -> list[float | None]:
    """Return a start time (seconds) per verse, or None when unmatched."""
    # Build the transcript character stream with a char->word index map.
    t_chars: list[str] = []
    t_owner: list[int] = []
    for wi, w in enumerate(words):
        for ch in normalise(w["word"]):
            t_chars.append(ch)
            t_owner.append(wi)

    # Build the verse character stream with a char->verse index map.
    v_chars: list[str] = []
    v_owner: list[int] = []
    for vi, v in enumerate(verses):
        for ch in normalise(" ".join(v["lines"])):
            v_chars.append(ch)
            v_owner.append(vi)

    # Char offset at which each verse begins inside v_chars.
    v_begin: dict[int, int] = {}
    for idx, vi in enumerate(v_owner):
        v_begin.setdefault(vi, idx)

    sm = difflib.SequenceMatcher(None, v_chars, t_chars, autojunk=False)
    # For each verse: earliest transcript word whose chars matched inside it
    # (and how far into the verse that match sits), plus how many of the
    # verse's chars matched at all (confidence).
    first_word: dict[int, int] = {}
    first_offset: dict[int, int] = {}
    matched: dict[int, int] = {}
    for a, b, n in sm.get_matching_blocks():
        # A block may straddle a verse boundary; split it per verse so a
        # stray trailing character never anchors the *next* verse.
        k = 0
        while k < n:
            vi = v_owner[a + k]
            run_start = k
            while k < n and v_owner[a + k] == vi:
                k += 1
            run_len = k - run_start
            matched[vi] = matched.get(vi, 0) + run_len
            # Single stray characters match everywhere in Devanagari; only
            # runs of MIN_RUN chars are trusted to anchor a verse start.
            if run_len < MIN_RUN:
                continue
            wi = t_owner[b + run_start]
            if vi not in first_word or wi < first_word[vi]:
                first_word[vi] = wi
                first_offset[vi] = (a + run_start) - v_begin[vi]

    raw: list[tuple[float, float] | None] = []  # (time of first match, fraction into verse)
    for vi, v in enumerate(verses):
        total = len(normalise(" ".join(v["lines"]))) or 1
        conf = matched.get(vi, 0) / total
        if vi in first_word and conf >= 0.35:
            raw.append((words[first_word[vi]]["start"], first_offset[vi] / total))
        else:
            raw.append(None)

    # Whisper routinely drops a line; when the first matched word sits some
    # way into the verse, back-date the start proportionally using the
    # typical verse duration for this recording.
    anchors = [t for t, _ in (r for r in raw if r is not None)]
    gaps = sorted(b - a for a, b in zip(anchors, anchors[1:]) if b > a)
    typical = gaps[len(gaps) // 2] if gaps else 0.0
    starts: list[float | None] = []
    for r in raw:
        if r is None:
            starts.append(None)
        else:
            t, frac = r
            starts.append(max(0.0, t - frac * typical))
    return starts


def make_monotonic(starts: list[float | None], duration: float) -> list[float]:
    """Drop out-of-order matches, then linearly interpolate the gaps."""
    fixed: list[float | None] = list(starts)
    last = -1.0
    for i, s in enumerate(fixed):
        if s is None:
            continue
        if s <= last:
            fixed[i] = None
        else:
            last = s
    # Interpolate None runs between known anchors (0 at the start, duration at the end).
    n = len(fixed)
    i = 0
    while i < n:
        if fixed[i] is not None:
            i += 1
            continue
        j = i
        while j < n and fixed[j] is None:
            j += 1
        lo = fixed[i - 1] if i > 0 else 0.0
        hi = fixed[j] if j < n else duration
        span = j - i + 1
        for k in range(i, j):
            fixed[k] = lo + (hi - lo) * (k - i + 1) / span
        i = j
    return [float(x) for x in fixed]  # type: ignore[arg-type]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--chalisa", required=True, help="chalisa id in content.json (e.g. hanuman_chalisa)")
    ap.add_argument("--model", default="large-v3", help="faster-whisper model name")
    ap.add_argument("--device", default="cuda", choices=("cuda", "cpu"))
    ap.add_argument("--out", type=Path, default=None, help="write timings JSON here instead of patching content.json")
    ap.add_argument("--dry-run", action="store_true", help="print the table, change nothing")
    args = ap.parse_args()

    content = json.loads(CONTENT.read_text(encoding="utf-8"))
    chalisa = next((c for c in content["chalisas"] if c["id"] == args.chalisa), None)
    if chalisa is None:
        sys.exit(f"No chalisa with id {args.chalisa}")
    if not chalisa.get("audio"):
        sys.exit(f"{args.chalisa} has no audio")
    mp3 = RAW / f"{chalisa['audio']}.mp3"
    if not mp3.exists():
        sys.exit(f"Missing {mp3}")

    words = transcribe(mp3, args.model, args.device)
    duration = max(w["end"] for w in words) if words else 0.0

    verses = [v for s in chalisa["sections"] for v in s["verses"]]
    raw_starts = align(words, verses)
    starts = make_monotonic(raw_starts, duration)

    matched = sum(1 for s in raw_starts if s is not None)
    print(f"\n-> {matched}/{len(verses)} verses matched directly, rest interpolated\n")
    for v, raw, s in zip(verses, raw_starts, starts):
        flag = " " if raw is not None else "~"
        print(f"  {flag} {s:8.1f}s  {v['lines'][0][:48]}")

    if args.dry_run:
        return
    if args.out:
        args.out.write_text(
            json.dumps([{"startMs": int(s * 1000)} for s in starts], ensure_ascii=False, indent=1),
            encoding="utf-8",
        )
        print(f"-> wrote {args.out}")
        return

    for v, s in zip(verses, starts):
        v["startMs"] = int(s * 1000)
    CONTENT.write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"-> patched {CONTENT} ({args.chalisa}: startMs on {len(verses)} verses)")


if __name__ == "__main__":
    main()
