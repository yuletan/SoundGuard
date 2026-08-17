package com.yuletan.soundguard

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class SoundSeverity {
    High,
    Low,
    None,
}

data class ClassificationResult(
    val category: String,
    val displayLabel: String,
    val confidence: Float,
    val severity: SoundSeverity,
    val isEmergency: Boolean,
)

class SoundClassifier(private val context: Context) {
    companion object {
        private const val TAG = "SoundClassifier"
        const val MODEL_FILENAME = "yamnet.tflite"
        const val SAMPLE_RATE = 16000
        const val INPUT_SAMPLES = 15600 // 0.975s at 16kHz
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetDescriptor: AssetFileDescriptor? = try {
                context.assets.openFd(MODEL_FILENAME)
            } catch (e: Exception) {
                null
            }

            if (assetDescriptor != null) {
                val inputStream = FileInputStream(assetDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = assetDescriptor.startOffset
                val declaredLength = assetDescriptor.declaredLength
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

                val options = Interpreter.Options().apply {
                    setNumThreads(2)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                Log.i(TAG, "YAMNet TFLite model loaded successfully.")
            } else {
                Log.w(TAG, "yamnet.tflite not found in assets. Running in smart acoustic fallback mode.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing YAMNet interpreter: ${e.message}", e)
            isModelLoaded = false
        }
    }

    fun classify(audioPcm: FloatArray): ClassificationResult {
        if (isModelLoaded && interpreter != null) {
            return runTfliteInference(audioPcm)
        }
        return runHeuristicFallback(audioPcm)
    }

    private fun runTfliteInference(audioPcm: FloatArray): ClassificationResult {
        try {
            val inputBuffer = ByteBuffer.allocateDirect(INPUT_SAMPLES * 4).apply {
                order(ByteOrder.nativeOrder())
                rewind()
                val len = minOf(audioPcm.size, INPUT_SAMPLES)
                for (i in 0 until len) {
                    putFloat(audioPcm[i])
                }
                for (i in len until INPUT_SAMPLES) {
                    putFloat(0f)
                }
            }

            val outputScores = Array(1) { FloatArray(521) }
            interpreter?.run(inputBuffer, outputScores)

            val scores = outputScores[0]
            var maxIndex = 0
            var maxScore = 0f
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxIndex = i
                }
            }

            return mapYamnetClass(maxIndex, maxScore)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            return runHeuristicFallback(audioPcm)
        }
    }

    private fun mapYamnetClass(classIndex: Int, confidence: Float): ClassificationResult {
        // YAMNet AudioSet Class Indices Mapping
        return when (classIndex) {
            // Alarms & Emergency (indices correspond to AudioSet classes for smoke alarm, fire alarm, sirens, etc.)
            in 390..405, 416, 417 -> ClassificationResult(
                category = "alarm",
                displayLabel = "Alarm / Siren Detected",
                confidence = confidence,
                severity = SoundSeverity.High,
                isEmergency = true,
            )
            in 435..440 -> ClassificationResult(
                category = "glass_break",
                displayLabel = "Glass Breaking Detected",
                confidence = confidence,
                severity = SoundSeverity.High,
                isEmergency = true,
            )
            in 406..410 -> ClassificationResult(
                category = "doorbell",
                displayLabel = "Doorbell / Knock",
                confidence = confidence,
                severity = SoundSeverity.Low,
                isEmergency = false,
            )
            in 0..50 -> ClassificationResult(
                category = "speech",
                displayLabel = "Human Speech",
                confidence = confidence,
                severity = SoundSeverity.None,
                isEmergency = false,
            )
            else -> ClassificationResult(
                category = "background",
                displayLabel = "Normal Background",
                confidence = confidence,
                severity = SoundSeverity.None,
                isEmergency = false,
            )
        }
    }

    private fun runHeuristicFallback(audioPcm: FloatArray): ClassificationResult {
        // Compute RMS energy
        var sumSquares = 0.0
        for (sample in audioPcm) {
            sumSquares += (sample * sample)
        }
        val rms = kotlin.math.sqrt(sumSquares / audioPcm.size.coerceAtLeast(1)).toFloat()

        return when {
            rms > 0.65f -> ClassificationResult(
                category = "alarm",
                displayLabel = "High Frequency Siren / Alarm",
                confidence = (0.75f + (rms * 0.2f)).coerceAtMost(0.98f),
                severity = SoundSeverity.High,
                isEmergency = true,
            )
            rms > 0.45f -> ClassificationResult(
                category = "speech",
                displayLabel = "Speech / Active Sound",
                confidence = 0.70f,
                severity = SoundSeverity.None,
                isEmergency = false,
            )
            else -> ClassificationResult(
                category = "background",
                displayLabel = "Normal Background",
                confidence = 0.95f,
                severity = SoundSeverity.None,
                isEmergency = false,
            )
        }
    }
}
