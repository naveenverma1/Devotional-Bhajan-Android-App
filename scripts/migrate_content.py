#!/usr/bin/env python3
"""
migrate_content.py — one-time migration from the legacy assets/*.html
files (UTF-8 / UTF-16, mixed) into a single structured
app/src/main/assets/content.json that the v4.0 Compose reader consumes.

Each output `Chalisa` has:
  - id (machine identifier; same as the URL constant in the old code)
  - title (Devanagari display title)
  - audio (filename in res/raw, or null)
  - sections: ordered list of { heading, verses[] }

Each `Verse` has:
  - type: "doha" | "chaupai" | "shlok" | "mantra" | "verse"
  - number: 1-based index within its section, or null
  - lines: list[str] — the original line breaks preserved as authored

Run from the repo root:

    python scripts/migrate_content.py
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Iterable

from lxml import html as lxml_html

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS_DIR = REPO_ROOT / "app" / "src" / "main" / "assets"
OUTPUT = ASSETS_DIR / "content.json"

# Each chalisa: (id, source file, title, optional audio rawName, optional author)
SOURCES = [
    ("sunderkand",
     "n.html",
     "श्री सुन्दर काण्ड",
     "sunder",
     "तुलसीदास"),
    ("hanuman_chalisa",
     "k.html",
     "श्री हनुमान चालीसा",
     "hanumanchalisa",
     "तुलसीदास"),
    ("bajrang_baan",
     "baaan.html",
     "श्री बजरंग बाण",
     None,
     "तुलसीदास"),
    ("khatu_shyam_chalisa",
     "Khatushyam.html",
     "श्री खाटू श्याम चालीसा",
     None,
     None),
    ("hanuman_aarti",
     "aarti.html",
     "हनुमान आरती",
     None,
     None),
]

# Section heading keywords. Order matters — we walk left to right and
# classify each verse by the most recent heading seen.
SECTION_KEYWORDS = {
    "दोहा": "doha",
    "॥दोहा॥": "doha",
    "चौपाई": "chaupai",
    "॥चौपाई॥": "chaupai",
    "श्लोक": "shlok",
    "॥श्लोक॥": "shlok",
    "मंत्र": "mantra",
    "मन्त्र": "mantra",
}

# Some chalisas have an explicit verse number like "॥1॥" or "।।1।।" at
# the end of the second line. We extract it when present.
VERSE_NUMBER_RE = re.compile(r"[॥।]\s*(\d{1,3})\s*[॥।]")


def detect_and_read(path: Path) -> str:
    """Read a HTML file with BOM/encoding auto-detection."""
    raw = path.read_bytes()
    # UTF-16 BOMs
    if raw.startswith(b"\xff\xfe") or raw.startswith(b"\xfe\xff"):
        return raw.decode("utf-16")
    # UTF-8 BOM
    if raw.startswith(b"\xef\xbb\xbf"):
        return raw[3:].decode("utf-8")
    # Try UTF-8 first, fall back to UTF-16 LE which is what n.html uses
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("utf-16-le")


def normalize(text: str) -> str:
    """Collapse runs of whitespace + strip NBSP / zero-width entities."""
    text = text.replace("\xa0", " ").replace("\u200b", "")
    # &nbsp without trailing ; (some files do this)
    text = re.sub(r"&nbsp\s?;?", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def classify_section(text: str) -> tuple[str | None, str | None]:
    """If `text` is a section heading (दोहा / चौपाई / श्लोक), return
    (verse_type, heading_text). Otherwise (None, None).

    Real headings in this corpus are very short (just the section
    keyword, sometimes flanked by `॥`). Anything longer than 25
    Devanagari characters is a verse that happens to mention one of
    the keywords (e.g., chaupai #16 of the Hanuman Chalisa mentions
    "मंत्र" inside the verse).
    """
    stripped = text.strip()
    if not stripped or len(stripped) > 25:
        return None, None
    for needle, vtype in SECTION_KEYWORDS.items():
        if needle in stripped:
            return vtype, stripped
    return None, None


def split_lines(text: str) -> list[str]:
    """Split a verse's text into individual lines. We split on the
    single-danda `।` (end of half-line) and on the double-danda `॥` /
    `।।` (end of verse), but keep the danda on the line that ends it."""
    # Normalise the double-danda alternates to a single character.
    text = text.replace("।।", "॥")
    parts = re.split(r"([।॥])", text)
    lines: list[str] = []
    buf = ""
    for part in parts:
        buf += part
        if part in ("।", "॥"):
            line = buf.strip()
            if line:
                lines.append(line)
            buf = ""
    tail = buf.strip()
    if tail:
        lines.append(tail)
    return lines


def extract_blocks(file_text: str) -> Iterable[str]:
    """Yield the body text in small blocks, where a "block" is the text
    inside a single `<p>` / `<p/>` / `<center>` element OR plain text
    between such elements. We deliberately treat `<p/>` and `<center/>`
    as block separators (the legacy HTML uses them like that) rather
    than as proper containers."""

    # Strip <style>, <head>, <html>, <body>, <meta>, <title> wrappers
    cleaned = re.sub(r"<style[^>]*>.*?</style>", "", file_text, flags=re.DOTALL | re.IGNORECASE)
    cleaned = re.sub(r"<!--.*?-->", "", cleaned, flags=re.DOTALL)
    cleaned = re.sub(r"<meta[^>]*>", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"<title[^>]*>.*?</title>", "", cleaned, flags=re.DOTALL | re.IGNORECASE)
    cleaned = re.sub(r"</?(html|head|body)[^>]*>", "", cleaned, flags=re.IGNORECASE)

    # Now split on <p ...>, <p/>, </p>, <center>, <center/>, </center>
    splitter = re.compile(r"<\s*/?\s*(p|center)\s*/?\s*>", re.IGNORECASE)
    pieces = splitter.split(cleaned)
    # `pieces` alternates [text, tag_name, text, tag_name, ...] but we
    # only care about the text content; the tags themselves told us
    # to break here.
    for i, piece in enumerate(pieces):
        if i % 2 == 0:  # text portion
            # Strip any remaining inline tags (<b>, <i>, <br>, etc.).
            inline_stripped = re.sub(r"<[^>]+>", "", piece)
            inline_stripped = normalize(inline_stripped)
            if inline_stripped:
                yield inline_stripped


def parse_chalisa(html_text: str, audio: str | None, author: str | None) -> dict:
    """Walk the blocks, building sections.

    Two distinct legacy formats live in the assets dir:
      A. Block-based -- each `<p>...</p>` is a complete verse. We may
         find multiple `॥`-terminated verses inside one block.
      B. Line-based  -- each `<p/>` self-closing tag is a single LINE.
         A verse spans multiple `<p/>` markers and ends at `॥`.

    We pick the mode by counting tags: if `</p>` outnumbers `<p/>`
    in the source, we're block-based. Otherwise we're line-based.
    """
    p_close = html_text.count("</p>")
    p_self = html_text.count("<p/>")
    block_based = p_close > p_self

    sections: list[dict] = []
    current: dict | None = None
    counter = 1
    pending = ""  # only used in line-based mode

    def open_section(heading: str, vtype: str) -> None:
        nonlocal current, counter
        sections.append({"heading": heading, "verses": [], "_hint": vtype})
        current = sections[-1]
        counter = 1

    def emit_verse(text: str) -> None:
        nonlocal counter
        if current is None or not text.strip():
            return
        verse_type = current.get("_hint", "verse")
        lines = split_lines(text)
        if not lines:
            return
        # Drop decorative non-content verses (e.g., a string of dots
        # used as a separator in the legacy HTML closing block).
        joined = "".join(lines)
        letters = sum(1 for c in joined if c.isalpha())
        if letters < 3:
            return
        number: int | None = None
        m = VERSE_NUMBER_RE.search(lines[-1])
        if m:
            try:
                number = int(m.group(1))
            except ValueError:
                pass
        current["verses"].append({
            "type": verse_type,
            "number": number if number is not None else (counter if verse_type == "chaupai" else None),
            "lines": lines,
        })
        if verse_type == "chaupai":
            counter += 1

    def emit_block_as_verses(block: str) -> None:
        """Block-based mode: split this `<p>` block on `॥` (or `।।`)
        into one or more verses. Blocks without a terminator emit a
        single verse."""
        text = block.replace("।।", "॥")
        if "॥" in text:
            pieces = text.split("॥")
            for piece in pieces[:-1]:
                emit_verse(piece + "॥")
            tail = pieces[-1].strip()
            if tail:
                emit_verse(tail)
        else:
            emit_verse(text)

    def flush_pending(force: bool = False) -> None:
        """Line-based mode helper: emit complete `॥`-terminated verses
        from the accumulator. Optionally flush the trailing remainder."""
        nonlocal pending
        if not pending or current is None:
            if force:
                pending = ""
            return
        text = pending.replace("।।", "॥")
        pieces = text.split("॥")
        for piece in pieces[:-1]:
            verse_text = piece.strip() + "॥"
            if verse_text != "॥":
                emit_verse(verse_text)
        tail = pieces[-1].strip()
        if force and tail:
            emit_verse(tail)
            pending = ""
        else:
            pending = tail

    for block in extract_blocks(html_text):
        vtype, heading = classify_section(block)
        if vtype is not None:
            if block_based:
                pass  # nothing pending to flush in block-based mode
            else:
                flush_pending(force=True)
            open_section(heading or block, vtype)
            continue

        if current is None:
            # Title / stray text before the first heading.
            sections.append({"heading": "", "verses": [], "_hint": "verse"})
            current = sections[-1]

        if block_based:
            emit_block_as_verses(block)
        else:
            pending = (pending + " " + block).strip() if pending else block
            flush_pending(force=False)

    if not block_based:
        flush_pending(force=True)

    cleaned: list[dict] = []
    for s in sections:
        s.pop("_hint", None)
        if s["verses"]:
            cleaned.append(s)
    return {"sections": cleaned}


def main() -> int:
    if not ASSETS_DIR.exists():
        sys.exit(f"assets dir not found: {ASSETS_DIR}")

    out: dict = {"version": "4.0", "chalisas": []}

    for cid, fname, title, audio, author in SOURCES:
        src = ASSETS_DIR / fname
        if not src.exists():
            print(f"WARN: missing {src}, skipping {cid}", file=sys.stderr)
            continue
        text = detect_and_read(src)
        parsed = parse_chalisa(text, audio, author)
        chalisa = {
            "id": cid,
            "title": title,
            "audio": audio,
            "author": author,
            "sections": parsed["sections"],
        }
        verse_count = sum(len(s["verses"]) for s in chalisa["sections"])
        print(f"  {cid:<22}  {fname:<18}  sections={len(chalisa['sections'])}  verses={verse_count}")
        out["chalisas"].append(chalisa)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nWrote {OUTPUT.relative_to(REPO_ROOT)}  ({OUTPUT.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
