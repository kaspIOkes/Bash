"""Piper wrapper: synthesizes speech and derives per-word timing.

Uses real phoneme/audio alignments straight from the model's duration
predictor when the voice has been patched with
`piper.patch_voice_with_alignment` (setup.sh does this automatically).
Falls back to an estimate proportional to letter count for voices that
don't expose alignments, or if the alignment can't be lined up with the
tokenized words 1:1.
"""
import io
import re
import wave
from typing import Optional

from piper.voice import PiperVoice

WORD_RE = re.compile(r"\S+")
_SEPARATOR_PHONEMES = {" ", "^", "$"}

_voice_cache: dict[str, PiperVoice] = {}


def _load_voice(model_path: str) -> PiperVoice:
    voice = _voice_cache.get(model_path)
    if voice is None:
        voice = PiperVoice.load(model_path)
        _voice_cache[model_path] = voice
    return voice


def _timings_from_letters(text: str, duration: float) -> list[dict]:
    words = WORD_RE.findall(text)
    weights = [len(word) + 1 for word in words]
    total_weight = sum(weights) or 1

    timings = []
    elapsed = 0
    for word, weight in zip(words, weights):
        start = duration * elapsed / total_weight
        elapsed += weight
        end = duration * elapsed / total_weight
        timings.append({"word": word, "start": round(start, 3), "end": round(end, 3)})
    return timings


def _timings_from_alignments(alignments, words, sample_rate: int) -> Optional[list[dict]]:
    """Group phoneme alignments into words, split on spaces/sentence boundaries.

    Punctuation glued to a word (e.g. "Test.") stays in the same group since
    it isn't a separator phoneme; the separator that follows it (space, or
    end-of-sentence) is folded in as that word's trailing pause.
    """
    timings = []
    t = 0.0
    word_start = 0.0
    in_word = False
    word_idx = 0

    for alignment in alignments:
        dur = alignment.num_samples / sample_rate
        if alignment.phoneme in _SEPARATOR_PHONEMES:
            if in_word:
                if word_idx >= len(words):
                    return None
                timings.append({
                    "word": words[word_idx],
                    "start": round(word_start, 3),
                    "end": round(t + dur, 3),
                })
                word_idx += 1
                in_word = False
        elif not in_word:
            word_start = t
            in_word = True
        t += dur

    if word_idx != len(words):
        return None

    return timings


def synthesize(text: str, model_path: str) -> tuple[bytes, list[dict]]:
    """Synthesize speech for text, returning (wav_bytes, word_timings)."""
    voice = _load_voice(model_path)
    words = WORD_RE.findall(text)

    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav_file:
        alignments = voice.synthesize_wav(text, wav_file, include_alignments=True)
        duration = wav_file.getnframes() / wav_file.getframerate()
    wav_bytes = buffer.getvalue()

    timings = None
    if alignments:
        timings = _timings_from_alignments(alignments, words, voice.config.sample_rate)
    if timings is None:
        timings = _timings_from_letters(text, duration)

    return wav_bytes, timings
