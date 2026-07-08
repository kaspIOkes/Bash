"""Piper wrapper: synthesizes speech and estimates per-word timing from letter counts."""
import re
import subprocess
import wave

WORD_RE = re.compile(r"\S+")


def synthesize(text: str, model_path: str, output_wav: str) -> list[dict]:
    subprocess.run(
        ["python3", "-m", "piper", "-m", model_path, "-f", output_wav],
        input=text.encode("utf-8"),
        check=True,
    )

    with wave.open(output_wav, "rb") as wav_file:
        duration = wav_file.getnframes() / wav_file.getframerate()

    words = [m.group() for m in WORD_RE.finditer(text)]
    # +1 per word approximates the pause/space that follows it, so trailing
    # punctuation-heavy words still get a sliver of time for the pause.
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
