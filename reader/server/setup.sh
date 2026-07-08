#!/usr/bin/env bash
# Installs Python deps and downloads a German Piper voice into ./voices
set -euo pipefail
cd "$(dirname "$0")"

pip3 install -r requirements.txt

mkdir -p voices
VOICE=de_DE-thorsten-medium
BASE_URL="https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium"

if [ ! -f "voices/${VOICE}.onnx" ]; then
  curl -sL -o "voices/${VOICE}.onnx" "${BASE_URL}/${VOICE}.onnx"
  curl -sL -o "voices/${VOICE}.onnx.json" "${BASE_URL}/${VOICE}.onnx.json"
fi

# Patch the model in-place so synthesis returns real phoneme/audio
# alignments (accurate word timing) instead of a letter-count guess.
# Re-running this on an already-patched model is a harmless no-op.
python3 -m piper.patch_voice_with_alignment "voices/${VOICE}.onnx" 2>&1 || true

echo "Setup complete. Run: python3 app.py"
