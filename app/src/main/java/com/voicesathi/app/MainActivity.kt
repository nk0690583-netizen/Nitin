package com.voicesathi.app

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.ViewGroup
import android.widget.*
import java.io.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var script: EditText
    private lateinit var language: Spinner
    private lateinit var voice: Spinner
    private lateinit var speed: SeekBar
    private lateinit var status: TextView
    private lateinit var generate: Button
    private lateinit var export: Button
    private var ready = false
    private var chunks: List<String> = emptyList()
    private val wavFiles = mutableListOf<File>()
    private var nextIndex = 0
    private var pending = 0
    private var finalWav: File? = null
    private val reqCreate = 42
    private val voiceItems = mutableListOf<Voice>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(17, 24, 39)
        window.navigationBarColor = android.graphics.Color.rgb(17, 24, 39)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Voice Sathi"
            textSize = 26f
            setTextColor(0xFF111827.toInt())
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "Hindi + American English • Long-script voice maker"
            textSize = 14f
            setTextColor(0xFF4B5563.toInt())
            setPadding(0, dp(4), 0, dp(14))
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(label("Your script"))
        script = EditText(this).apply {
            hint = "Paste your full narration script here..."
            minLines = 8
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBg(0xFFFFFFFF.toInt(), dp(10).toFloat())
        }
        content.addView(script, lp(match, 220))
        content.addView(label("Language"))
        language = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Hindi (India)", "American English (US)")
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p: AdapterView<*>?) {}
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    if (ready) refreshVoices()
                }
            }
        }
        content.addView(language, lp(match, 48))
        content.addView(label("Voice available on this phone"))
        voice = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Loading voices...")
            )
        }
        content.addView(voice, lp(match, 48))
        val speedLabel = label("Speaking speed: 1.05x")
        content.addView(speedLabel)
        speed = SeekBar(this).apply {
            max = 10
            progress = 4
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    speedLabel.text = "Speaking speed: ${String.format(Locale.US, "%.2f", 0.65f + progress * 0.1f)}x"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        content.addView(speed, lp(match, 42))
        val speedNote = TextView(this).apply {
            text = "Move left for slower narration; right for faster narration."
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
        }
        content.addView(speedNote)
        generate = Button(this).apply {
            text = "Generate full voice"
            isEnabled = false
            setOnClickListener { startGeneration() }
        }
        content.addView(generate, lp(match, 52))
        export = Button(this).apply {
            text = "Export final WAV"
            isEnabled = false
            setOnClickListener { saveFinal() }
        }
        content.addView(export, lp(match, 52))
        status = TextView(this).apply {
            text = "Starting voice engine..."
            textSize = 14f
            setTextColor(0xFF374151.toInt())
            setPadding(0, dp(12), 0, dp(10))
        }
        content.addView(status)
        val note = TextView(this).apply {
            text = "Works offline with your phone's installed TTS voices. For better quality, install Hindi and English voice data in Android Text-to-speech settings. Output is WAV. Voice availability and quality depend on your phone; this app does not clone the sample speakers."
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(note)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(match, 0, 1f))
        setContentView(root)
    }

    private fun label(s: String) = TextView(this).apply {
        text = s
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(0xFF1F2937.toInt())
        setPadding(0, dp(14), 0, dp(6))
    }

    private fun lp(w: Int, hDp: Int) = LinearLayout.LayoutParams(w, if (hDp > 0) dp(hDp) else hDp).apply {
        bottomMargin = dp(8)
    }

    private val match get() = ViewGroup.LayoutParams.MATCH_PARENT

    private fun roundedBg(color: Int, radius: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(dp(1), 0xFFE5E7EB.toInt())
        }

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) {
            runOnUiThread { status.text = "Voice engine could not start. Check Android TTS settings." }
            return
        }
        ready = true
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    status.text = "A chunk failed. Check the selected voice and installed language data, then try again."
                    generate.isEnabled = true
                    export.isEnabled = false
                    wavFiles.forEach { if (it.exists()) it.delete() }
                    wavFiles.clear()
                    finalWav = null
                }
            }
            override fun onDone(utteranceId: String?) {
                runOnUiThread { onChunkDone() }
            }
        })
        runOnUiThread {
            refreshVoices()
            generate.isEnabled = true
            status.text = "Ready. Paste a script to begin."
        }
    }

    private fun currentLocale() = if (language.selectedItemPosition == 0) Locale("hi", "IN") else Locale.US

    private fun refreshVoices() {
        if (!ready) return
        val loc = currentLocale()
        val defName = tts.defaultVoice?.name
        val all = tts.voices?.filter {
            it.locale.language == loc.language && !it.isNetworkConnectionRequired
        }?.sortedWith(
            compareBy<Voice> { it.name != defName }.thenBy { it.name }
        ) ?: emptyList()
        voiceItems.clear()
        voiceItems.addAll(all)
        val names = if (all.isEmpty()) listOf("System default voice (install language data)")
        else all.map { v -> "${v.name}${if (v.name == defName) " • default" else ""}" }
        voice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        if (all.isEmpty()) {
            status.text = "No offline ${if (loc.language == "hi") "Hindi" else "English"} voice found. Install voice data in Android settings."
        }
    }

    private fun splitText(text: String, maxChars: Int = 2200): List<String> {
        val normalized = text.replace("\r", " ").trim()
        if (normalized.isEmpty()) return emptyList()
        val sentences = normalized.split(Regex("(?<=[.?!।])\\s+|\\n+"))
        val result = mutableListOf<String>()
        var part = StringBuilder()
        for (sentence in sentences) {
            var remaining = sentence.trim()
            while (remaining.length > maxChars) {
                val cut = remaining.lastIndexOf(' ', maxChars).let { if (it < maxChars / 2) maxChars else it }
                if (part.isNotEmpty()) {
                    result.add(part.toString().trim())
                    part = StringBuilder()
                }
                result.add(remaining.substring(0, cut).trim())
                remaining = remaining.substring(cut).trim()
            }
            if (remaining.isNotEmpty()) {
                if (part.isNotEmpty() && part.length + remaining.length + 1 > maxChars) {
                    result.add(part.toString().trim())
                    part = StringBuilder()
                }
                if (part.isNotEmpty()) part.append(' ')
                part.append(remaining)
            }
        }
        if (part.isNotEmpty()) result.add(part.toString().trim())
        return result.filter { it.isNotBlank() }
    }

    private fun startGeneration() {
        if (!ready) return
        chunks = splitText(script.text.toString())
        if (chunks.isEmpty()) {
            status.text = "Please paste a script first."
            return
        }
        val loc = currentLocale()
        val langResult = tts.setLanguage(loc)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            status.text = "This language voice is missing. Install voice data in Android TTS settings."
            return
        }
        if (voiceItems.isNotEmpty()) {
            tts.voice = voiceItems[voice.selectedItemPosition.coerceIn(0, voiceItems.lastIndex)]
        }
        tts.setSpeechRate(0.65f + speed.progress * 0.1f)
        wavFiles.clear()
        nextIndex = 0
        pending = 0
        finalWav = null
        export.isEnabled = false
        generate.isEnabled = false
        status.text = "Preparing ${chunks.size} parts..."
        queueNextChunk()
    }

    private fun queueNextChunk() {
        if (nextIndex >= chunks.size) {
            combineChunks()
            return
        }
        val index = nextIndex++
        val file = File(cacheDir, "voice_part_$index.wav")
        if (file.exists()) file.delete()
        wavFiles.add(file)
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "part_$index")
        pending = index
        val result = tts.synthesizeToFile(chunks[index], params, file, "part_$index")
        if (result == TextToSpeech.ERROR) {
            status.text = "Could not start audio generation for part ${index + 1}. Check the selected TTS voice."
            generate.isEnabled = true
            wavFiles.forEach { if (it.exists()) it.delete() }
            wavFiles.clear()
            finalWav = null
        } else {
            status.text = "Generating part ${index + 1} of ${chunks.size}..."
        }
    }

    private fun onChunkDone() {
        if (!isFinishing && nextIndex < chunks.size) {
            queueNextChunk()
        } else if (nextIndex >= chunks.size && wavFiles.isNotEmpty()) {
            combineChunks()
        }
    }

    private fun combineChunks() {
        try {
            val valid = wavFiles.filter { it.exists() && it.length() > 44L }
            if (valid.isEmpty()) throw IOException("No audio was created")
            val parsed = valid.map { readWav(it) }
            val first = parsed.first()
            if (parsed.any { it.sampleRate != first.sampleRate || it.channels != first.channels || it.bitsPerSample != first.bitsPerSample }) {
                throw IOException("Voice chunks have different audio formats")
            }
            val output = File(cacheDir, "voice_sathi_final.wav")
            val dataBytes = parsed.sumOf { it.data.size.toLong() }
            FileOutputStream(output).use { out ->
                writeWavHeader(out, dataBytes, first.sampleRate, first.channels, first.bitsPerSample)
                parsed.forEach { out.write(it.data) }
            }
            finalWav = output
            runOnUiThread {
                status.text = "Done! ${chunks.size} parts combined into one WAV file (${output.length() / 1024} KB)."
                export.isEnabled = true
                generate.isEnabled = true
            }
        } catch (e: Exception) {
            runOnUiThread {
                status.text = "Could not combine audio: ${e.message ?: "unsupported TTS audio format"}. Try shorter script or another voice."
                generate.isEnabled = true
            }
        }
    }

    private data class WavData(val data: ByteArray, val sampleRate: Int, val channels: Int, val bitsPerSample: Int)

    private fun readWav(file: File): WavData {
        val bytes = file.readBytes()
        if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") {
            throw IOException("The phone TTS engine did not return WAV audio")
        }
        val bb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var rate = 0
        var channels = 0
        var bits = 0
        var dataStart = -1
        var dataLen = 0
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val len = bb.getInt(pos + 4).coerceAtLeast(0)
            val start = pos + 8
            if (start + len > bytes.size) break
            if (id == "fmt " && len >= 16) {
                channels = bb.getShort(start + 2).toInt() and 0xffff
                rate = bb.getInt(start + 4)
                bits = bb.getShort(start + 14).toInt() and 0xffff
            }
            if (id == "data") {
                dataStart = start
                dataLen = len
                break
            }
            pos = start + len + (len % 2)
        }
        if (dataStart < 0 || rate == 0 || channels == 0 || bits == 0) throw IOException("Invalid WAV chunk")
        return WavData(bytes.copyOfRange(dataStart, dataStart + dataLen), rate, channels, bits)
    }

    private fun writeWavHeader(out: OutputStream, dataSize: Long, rate: Int, channels: Int, bits: Int) {
        val bb = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val byteRate = rate * channels * bits / 8
        val block = channels * bits / 8
        bb.put("RIFF".toByteArray())
        bb.putInt((36 + dataSize).toInt())
        bb.put("WAVEfmt ".toByteArray())
        bb.putInt(16)
        bb.putShort(1)
        bb.putShort(channels.toShort())
        bb.putInt(rate)
        bb.putInt(byteRate)
        bb.putShort(block.toShort())
        bb.putShort(bits.toShort())
        bb.put("data".toByteArray())
        bb.putInt(dataSize.toInt())
        out.write(bb.array())
    }

    private fun saveFinal() {
        val file = finalWav ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/wav"
            putExtra(Intent.EXTRA_TITLE, "VoiceSathi_${System.currentTimeMillis()}.wav")
        }
        startActivityForResult(intent, reqCreate)
    }

    @Deprecated("Deprecated in Android API but widely compatible")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqCreate && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val file = finalWav ?: return
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                status.text = "Exported successfully. Find the WAV in the folder you selected."
            } catch (e: Exception) {
                status.text = "Export failed: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
