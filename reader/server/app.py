import base64
import os
import tempfile
from pathlib import Path

from flask import Flask, jsonify, request, send_from_directory

from tts import synthesize

BASE_DIR = Path(__file__).resolve().parent
WEB_DIR = BASE_DIR.parent / "web"
VOICES_DIR = BASE_DIR / "voices"

app = Flask(__name__, static_folder=None)


@app.get("/")
def index():
    return send_from_directory(WEB_DIR, "index.html")


@app.get("/<path:filename>")
def static_files(filename):
    return send_from_directory(WEB_DIR, filename)


@app.get("/api/voices")
def voices():
    return jsonify(sorted(p.stem for p in VOICES_DIR.glob("*.onnx")))


@app.post("/api/speak")
def speak():
    data = request.get_json(force=True)
    text = (data.get("text") or "").strip()
    voice = data.get("voice") or ""

    if not text:
        return jsonify({"error": "text is required"}), 400

    model_path = VOICES_DIR / f"{voice}.onnx"
    if not model_path.exists():
        return jsonify({"error": f"unknown voice: {voice}"}), 400

    fd, wav_path = tempfile.mkstemp(suffix=".wav")
    os.close(fd)
    try:
        timings = synthesize(text, str(model_path), wav_path)
        audio_bytes = Path(wav_path).read_bytes()
    finally:
        os.remove(wav_path)

    return jsonify({
        "audio_base64": base64.b64encode(audio_bytes).decode("ascii"),
        "timings": timings,
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
