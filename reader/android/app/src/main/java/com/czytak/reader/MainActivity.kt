package com.czytak.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.attachContext(this)
        viewModel.loadVoices()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReaderScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(viewModel: ReaderViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Czytak", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.serverUrl,
            onValueChange = { viewModel.setServerUrl(it) },
            label = { Text("Adres serwera (reader/server)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.sourceText,
            onValueChange = { viewModel.sourceText = it },
            label = { Text("Tekst źródłowy") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.translationText,
            onValueChange = { viewModel.translationText = it },
            label = { Text("Tłumaczenie (jedno zdanie na linię)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        VoiceDropdown(viewModel)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Szybkość: ${"%.2f".format(viewModel.rate)}x")
        Slider(
            value = viewModel.rate,
            onValueChange = { viewModel.setRate(it) },
            valueRange = 0.5f..2f
        )

        Button(onClick = { viewModel.togglePlay() }) {
            Text(if (viewModel.isPlaying) "Pauza" else "Czytaj")
        }

        if (viewModel.status.isNotEmpty()) {
            Text(viewModel.status, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ReadingView(viewModel)
    }
}

@Composable
fun VoiceDropdown(viewModel: ReaderViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(viewModel.selectedVoice.ifEmpty { "Wybierz lektora" })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            viewModel.voices.forEach { voice ->
                DropdownMenuItem(
                    text = { Text(voice) },
                    onClick = {
                        viewModel.selectedVoice = voice
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReadingView(viewModel: ReaderViewModel) {
    var globalIndex = 0
    Column {
        viewModel.sentences.forEachIndexed { sIdx, sentence ->
            val isActive = sIdx == viewModel.currentSentenceIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isActive) Color(0x14808080) else Color.Transparent)
                    .padding(8.dp)
            ) {
                FlowRow {
                    sentence.words.forEach { word ->
                        val wordIdx = globalIndex
                        globalIndex++
                        val isCurrent = wordIdx == viewModel.currentWordIndex
                        Text(
                            text = "$word ",
                            fontSize = 20.sp,
                            modifier = Modifier.background(
                                if (isCurrent) Color(0xFFFFD54F) else Color.Transparent
                            )
                        )
                    }
                }
                if (sentence.translation.isNotEmpty()) {
                    Text(
                        text = sentence.translation,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
