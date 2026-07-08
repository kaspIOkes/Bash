const sourceEl = document.getElementById("source");
const translationEl = document.getElementById("translation");
const voiceEl = document.getElementById("voice");
const rateEl = document.getElementById("rate");
const rateValueEl = document.getElementById("rate-value");
const playEl = document.getElementById("play");
const statusEl = document.getElementById("status");
const readingEl = document.getElementById("reading");
const audioEl = document.getElementById("audio");

let wordMeta = []; // [{word, sentenceIndex}] in reading order
let timings = []; // [{word, start, end}] from server, same order/length as wordMeta
let spans = []; // flat span elements, same order
let sentenceEls = [];
let currentWordIndex = -1;
let currentSentenceIndex = -1;
let lastSynthesizedText = null;

function splitSentences(text) {
  return text
    .split(/(?<=[.!?])\s+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

function tokenizeWords(sentence) {
  return sentence.match(/\S+/g) || [];
}

async function loadVoices() {
  const res = await fetch("/api/voices");
  const voices = await res.json();
  voiceEl.innerHTML = "";
  for (const v of voices) {
    const opt = document.createElement("option");
    opt.value = v;
    opt.textContent = v;
    voiceEl.appendChild(opt);
  }
}

function renderReading(sentences, translations) {
  readingEl.innerHTML = "";
  wordMeta = [];
  spans = [];
  sentenceEls = [];

  sentences.forEach((sentence, sIdx) => {
    const sentenceDiv = document.createElement("div");
    sentenceDiv.className = "sentence";

    const sourceLine = document.createElement("div");
    sourceLine.className = "source-line";

    const words = tokenizeWords(sentence);
    words.forEach((word, i) => {
      const span = document.createElement("span");
      span.textContent = word;
      sourceLine.appendChild(span);
      sourceLine.appendChild(document.createTextNode(" "));
      wordMeta.push({ word, sentenceIndex: sIdx });
      spans.push(span);
    });

    const translationLine = document.createElement("div");
    translationLine.className = "translation-line";
    translationLine.textContent = translations[sIdx] || "";

    sentenceDiv.appendChild(sourceLine);
    sentenceDiv.appendChild(translationLine);
    readingEl.appendChild(sentenceDiv);
    sentenceEls.push(sentenceDiv);
  });
}

function setCurrentWord(idx) {
  if (idx === currentWordIndex) return;
  if (currentWordIndex >= 0 && spans[currentWordIndex]) {
    spans[currentWordIndex].classList.remove("current");
  }
  currentWordIndex = idx;
  if (idx >= 0 && spans[idx]) {
    spans[idx].classList.add("current");
  }

  const sIdx = idx >= 0 ? wordMeta[idx].sentenceIndex : -1;
  if (sIdx !== currentSentenceIndex) {
    if (currentSentenceIndex >= 0 && sentenceEls[currentSentenceIndex]) {
      sentenceEls[currentSentenceIndex].classList.remove("active");
    }
    currentSentenceIndex = sIdx;
    if (sIdx >= 0 && sentenceEls[sIdx]) {
      sentenceEls[sIdx].classList.add("active");
      sentenceEls[sIdx].scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }
}

function findWordIndexForTime(t) {
  for (let i = timings.length - 1; i >= 0; i--) {
    if (t >= timings[i].start) return i;
  }
  return -1;
}

audioEl.addEventListener("timeupdate", () => {
  setCurrentWord(findWordIndexForTime(audioEl.currentTime));
});

audioEl.addEventListener("ended", () => {
  setCurrentWord(-1);
  playEl.textContent = "Czytaj";
});

rateEl.addEventListener("input", () => {
  rateValueEl.textContent = Number(rateEl.value).toFixed(2);
  audioEl.playbackRate = Number(rateEl.value);
});

async function synthesize(fullText, voice) {
  statusEl.textContent = "Generuję mowę...";
  playEl.disabled = true;
  try {
    const res = await fetch("/api/speak", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: fullText, voice }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Błąd serwera");

    timings = data.timings;
    audioEl.src = "data:audio/wav;base64," + data.audio_base64;
    audioEl.playbackRate = Number(rateEl.value);
    lastSynthesizedText = fullText;
    statusEl.textContent = "";
  } catch (err) {
    statusEl.textContent = "Błąd: " + err.message;
    throw err;
  } finally {
    playEl.disabled = false;
  }
}

playEl.addEventListener("click", async () => {
  const sentences = splitSentences(sourceEl.value);
  if (sentences.length === 0) {
    statusEl.textContent = "Wklej najpierw jakiś tekst.";
    return;
  }
  const translations = translationEl.value.split("\n").map((s) => s.trim());
  const fullText = sentences.join(" ");

  if (!audioEl.paused) {
    audioEl.pause();
    playEl.textContent = "Czytaj";
    return;
  }

  if (fullText !== lastSynthesizedText) {
    renderReading(sentences, translations);
    await synthesize(fullText, voiceEl.value);
  }

  audioEl.play();
  playEl.textContent = "Pauza";
});

loadVoices();
