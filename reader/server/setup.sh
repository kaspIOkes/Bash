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

echo "Setup complete. Run: python3 app.py"
