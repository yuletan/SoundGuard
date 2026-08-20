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
    Medium,
    Low,
    None,
}

data class CategoryScore(
    val category: String,
    val displayLabel: String,
    val score: Float,
    val severity: SoundSeverity,
    val isEmergency: Boolean,
)

data class ClassificationResult(
    val category: String,
    val displayLabel: String,
    val confidence: Float,
    val severity: SoundSeverity,
    val isEmergency: Boolean,
    val topCandidates: List<CategoryScore> = emptyList(),
    val debugText: String = "",
)

private data class CategoryRule(
    val category: String,
    val displayLabel: String,
    val severity: SoundSeverity,
    val isEmergency: Boolean,
    val indices: Set<Int>,
)

private fun idx(vararg ranges: IntRange): Set<Int> {
    return ranges.flatMap { it.toList() }.toSet()
}

class SoundSmoother(private val windowSize: Int = 5) {
    private val history = ArrayDeque<List<CategoryScore>>()

    fun update(frame: List<CategoryScore>): List<CategoryScore> {
        history.addLast(frame)
        if (history.size > windowSize) {
            history.removeFirst()
        }
        val allCategories = frame.map { it.category }.toSet()
        return allCategories.map { category ->
            val scoresForCategory = history.mapNotNull { f ->
                f.firstOrNull { it.category == category }?.score
            }
            val averageScore = scoresForCategory.average().toFloat()
            val latest = frame.first { it.category == category }
            latest.copy(score = averageScore)
        }.sortedByDescending { it.score }
    }

    fun reset() {
        history.clear()
    }
}

class HysteresisSwitcher {
    private var currentCategory = "background"
    private var emergencyHoldFrames = 0

    fun update(best: CategoryScore, enterThreshold: Float, exitThreshold: Float): String {
        val actualExit = exitThreshold.coerceAtMost(enterThreshold * 0.6f)
        if (best.category == currentCategory) {
            if (best.score < actualExit) {
                currentCategory = "background"
                emergencyHoldFrames = 0
            }
        } else {
            if (best.score >= enterThreshold) {
                currentCategory = best.category
            }
        }
        if (currentCategory != "background" && best.category == currentCategory) {
            emergencyHoldFrames++
        }
        return currentCategory
    }

    fun reset() {
        currentCategory = "background"
        emergencyHoldFrames = 0
    }
}

class EmergencyGate(private val requiredFrames: Int = 2) {
    private var candidateCategory: String? = null
    private var qualifyingFrames = 0

    fun update(category: CategoryScore?, threshold: Float): Boolean {
        if (category == null || !category.isEmergency || category.score < threshold) {
            reset()
            return false
        }

        if (candidateCategory == category.category) {
            qualifyingFrames++
        } else {
            candidateCategory = category.category
            qualifyingFrames = 1
        }
        return qualifyingFrames >= requiredFrames
    }

    fun reset() {
        candidateCategory = null
        qualifyingFrames = 0
    }
}

class SoundClassifier(private val context: Context) {
    companion object {
        private const val TAG = "SoundClassifier"
        const val MODEL_FILENAME = "yamnet.tflite"
        const val SAMPLE_RATE = 16000
        const val INPUT_SAMPLES = 15600

        private val categoryRules = listOf(
            CategoryRule("speech", "Human Speech", SoundSeverity.None, false, idx(0..18)),
            CategoryRule("crying", "Crying Detected", SoundSeverity.Medium, false, idx(19..23)),
            CategoryRule("singing", "Singing", SoundSeverity.None, false, idx(24..32)),
            CategoryRule("human_body", "Cough / Sneeze / Snore", SoundSeverity.Medium, false, idx(33..60)),
            CategoryRule("crowd", "Crowd / Cheering", SoundSeverity.None, false, idx(61..66)),
            CategoryRule("animal", "Animal Sound", SoundSeverity.Low, false, idx(67..131)),
            CategoryRule("music", "Music / TV", SoundSeverity.None, false, idx(132..276)),
            CategoryRule("wind", "Wind", SoundSeverity.None, false, idx(277..279)),
            CategoryRule("thunder", "Thunder", SoundSeverity.Medium, false, idx(280..281)),
            CategoryRule("water", "Water Sound", SoundSeverity.Low, false, idx(282..289)),
            CategoryRule("fire", "Fire / Crackling", SoundSeverity.High, true, idx(290..293)),
            CategoryRule("vehicle", "Vehicle", SoundSeverity.None, false, idx(294..315, 335..347)),
            CategoryRule("emergency_vehicle", "Emergency Vehicle Siren", SoundSeverity.High, true, idx(316..319)),
            CategoryRule("train", "Train", SoundSeverity.None, false, idx(322..328)),
            CategoryRule("aircraft", "Aircraft", SoundSeverity.None, false, idx(329..334)),
            CategoryRule("door", "Door / Doorbell / Knock", SoundSeverity.Medium, false, idx(348..354)),
            CategoryRule("household", "Household Sound", SoundSeverity.None, false, idx(355..381)),
            CategoryRule("alarm_telephone", "Alarm / Telephone", SoundSeverity.High, true, idx(382..389)),
            CategoryRule("siren_smoke", "Siren / Smoke Alarm", SoundSeverity.High, true, idx(390..396)),
            CategoryRule("mechanism", "Mechanical Sound", SoundSeverity.None, false, idx(397..411)),
            CategoryRule("construction", "Tool / Construction", SoundSeverity.None, false, idx(412..419)),
            CategoryRule("explosion_gunshot", "Explosion / Gunshot", SoundSeverity.High, true, idx(420..430)),
            CategoryRule("wood", "Wood Sound", SoundSeverity.None, false, idx(431..434)),
            CategoryRule("glass_break", "Glass Breaking", SoundSeverity.High, true, idx(435..437)),
            CategoryRule("liquid", "Liquid Sound", SoundSeverity.None, false, idx(438..449)),
            CategoryRule("object_impact", "Object Impact", SoundSeverity.None, false, idx(450..493)),
            CategoryRule("background", "Normal Background", SoundSeverity.None, false, idx(494..520)),
        )

        private val recognitionPriority = setOf(
            "crying",
            "human_body",
            "door",
            "fire",
            "emergency_vehicle",
            "alarm_telephone",
            "siren_smoke",
            "explosion_gunshot",
            "glass_break",
            "object_impact",
        )

        fun displayThresholdFor(category: String): Float {
            return when (category) {
                "speech" -> 0.25f
                "crying" -> 0.30f
                "human_body" -> 0.30f
                "door" -> 0.20f
                "music" -> 0.30f
                "animal" -> 0.35f
                "vehicle" -> 0.35f
                "household" -> 0.35f
                "object_impact" -> 0.40f
                "water" -> 0.30f
                "glass_break" -> 0.30f
                "alarm_telephone",
                "siren_smoke",
                "emergency_vehicle",
                "explosion_gunshot",
                "fire" -> 0.50f
                else -> 0.35f
            }
        }

        fun emergencyThresholdFor(category: String): Float {
            return when (category) {
                "alarm_telephone",
                "siren_smoke" -> 0.65f
                "emergency_vehicle" -> 0.70f
                "glass_break" -> 0.45f
                "explosion_gunshot",
                "fire" -> 0.70f
                else -> Float.MAX_VALUE
            }
        }

        fun tvAdjustedThreshold(base: Float, tvLikelihood: Float): Float {
            return if (tvLikelihood > 0.45f) {
                (base + 0.20f).coerceAtMost(0.85f)
            } else {
                base
            }
        }
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    val smoother = SoundSmoother(windowSize = 3)
    val hysteresis = HysteresisSwitcher()
    val emergencyGate = EmergencyGate(requiredFrames = 2)

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
                val inputTensor = interpreter!!.getInputTensor(0)
                val outputTensor = interpreter!!.getOutputTensor(0)
                Log.i(TAG, "YAMNet TFLite model loaded successfully.")
                Log.i(TAG, "Input tensor: name=${inputTensor.name()}, shape=${inputTensor.shape().contentToString()}, dataType=${inputTensor.dataType()}")
                Log.i(TAG, "Output tensor: name=${outputTensor.name()}, shape=${outputTensor.shape().contentToString()}, dataType=${outputTensor.dataType()}")

                if (inputTensor.shape().contentEquals(intArrayOf(1))) {
                    Log.i(TAG, "Resizing input tensor from [1] to [$INPUT_SAMPLES]")
                    interpreter!!.resizeInput(0, intArrayOf(INPUT_SAMPLES))
                    interpreter!!.allocateTensors()
                }
            } else {
                Log.w(TAG, "yamnet.tflite not found in assets. Running heuristic fallback.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing YAMNet interpreter: ${e.message}", e)
            isModelLoaded = false
        }
    }

    fun classify(audioPcm: FloatArray): ClassificationResult {
        return if (isModelLoaded && interpreter != null) {
            runTfliteInference(audioPcm)
        } else {
            runHeuristicFallback(audioPcm)
        }
    }

    fun resetState() {
        smoother.reset()
        hysteresis.reset()
        emergencyGate.reset()
    }

    private fun runTfliteInference(audioPcm: FloatArray): ClassificationResult {
        try {
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputSize = inputTensor.shape().fold(1) { acc, i -> acc * i }

            val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4).apply {
                order(ByteOrder.nativeOrder())
                rewind()
                val len = minOf(audioPcm.size, inputSize)
                for (i in 0 until len) {
                    putFloat(audioPcm[i])
                }
                for (i in len until inputSize) {
                    putFloat(0f)
                }
            }

            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputSize = outputShape.fold(1) { acc, i -> acc * i }
            val outputScores = Array(outputShape[0]) { FloatArray(outputShape.getOrElse(1) { outputSize }) }
            interpreter?.run(inputBuffer, outputScores)

            val scores = outputScores[0]

            val topRaw = scores
                .mapIndexed { index, score -> index to score }
                .sortedByDescending { it.second }
                .take(10)

            val aggregated = aggregateCategoryScores(scores)

            val smoothed = smoother.update(aggregated)

            val strongest = smoothed.firstOrNull() ?: aggregated.firstOrNull()
                ?: return ClassificationResult(
                    category = "background",
                    displayLabel = "Normal Background",
                    confidence = 0.95f,
                    severity = SoundSeverity.None,
                    isEmergency = false,
                )

            val best = smoothed.firstOrNull { candidate ->
                candidate.category in recognitionPriority &&
                    candidate.score >= displayThresholdFor(candidate.category) * 0.7f
            } ?: strongest

            val musicScore = smoothed.firstOrNull { it.category == "music" }?.score ?: 0f
            val tvLikelihood = musicScore

            // Prefer a recognized safety sound over a louder background category.
            // This matters when a real event is quiet or far from the microphone.
            val topEmergency = smoothed.firstOrNull { it.isEmergency && it.score >= 0.30f }

            val displayCategory: CategoryScore
            val stableCategory: String

            if (topEmergency != null && topEmergency.score >= displayThresholdFor(topEmergency.category) * 0.7f) {
                displayCategory = topEmergency
                stableCategory = topEmergency.category
            } else {
                val enterThreshold = displayThresholdFor(best.category)
                val exitThreshold = enterThreshold * 0.6f
                stableCategory = hysteresis.update(best, enterThreshold, exitThreshold)
                displayCategory = smoothed.firstOrNull { it.category == stableCategory } ?: best
            }

            val adjustedThreshold = tvAdjustedThreshold(emergencyThresholdFor(stableCategory), tvLikelihood)

            val shouldAlert = emergencyGate.update(displayCategory, adjustedThreshold)

            val displayLabel = if (displayCategory.score >= displayThresholdFor(displayCategory.category) * 0.7f) {
                displayCategory.displayLabel
            } else if (displayCategory.score >= displayThresholdFor(displayCategory.category)) {
                displayCategory.displayLabel
            } else {
                "Analyzing..."
            }

            val topCandidates = smoothed.filter { it.score > 0.05f }.take(3)
            val debugText = topRaw.joinToString(", ") { (idx, score) ->
                "$idx:${String.format("%.3f", score)}"
            }

            Log.d(TAG, "Aggregated top: ${smoothed.take(5).joinToString { "${it.category}=${String.format("%.3f", it.score)}${if(it.isEmergency) "!" else ""}" }}")
            Log.d(TAG, "Display=${displayCategory.category}, alert=$shouldAlert, tv=$tvLikelihood")

            return ClassificationResult(
                category = displayCategory.category,
                displayLabel = displayLabel,
                confidence = displayCategory.score,
                severity = displayCategory.severity,
                isEmergency = shouldAlert,
                topCandidates = topCandidates,
                debugText = debugText,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            return runHeuristicFallback(audioPcm)
        }
    }

    private fun aggregateCategoryScores(scores: FloatArray): List<CategoryScore> {
        return categoryRules.map { rule ->
            val classScores = rule.indices.mapNotNull { index ->
                if (index in scores.indices) scores[index] else null
            }.sortedDescending()

            val score = when {
                classScores.isEmpty() -> 0f
                rule.isEmergency -> classScores.take(2).sum().coerceAtMost(1f)
                else -> classScores.take(3).sum().coerceAtMost(1f)
            }

            CategoryScore(
                category = rule.category,
                displayLabel = rule.displayLabel,
                score = score,
                severity = rule.severity,
                isEmergency = rule.isEmergency,
            )
        }.sortedByDescending { it.score }
    }

    private fun runHeuristicFallback(audioPcm: FloatArray): ClassificationResult {
        var sumSquares = 0.0
        var peak = 0f
        for (sample in audioPcm) {
            val abs = kotlin.math.abs(sample)
            if (abs > peak) peak = abs
            sumSquares += (sample * sample)
        }
        val rms = kotlin.math.sqrt(sumSquares / audioPcm.size.coerceAtLeast(1)).toFloat()

        Log.d(TAG, "Heuristic fallback: rms=$rms, peak=$peak")

        return when {
            rms > 0.30f -> ClassificationResult(
                category = "loud_sound",
                displayLabel = "Loud Sound",
                confidence = 0.50f,
                severity = SoundSeverity.Low,
                isEmergency = false,
            )
            rms > 0.03f -> ClassificationResult(
                category = "speech",
                displayLabel = "Speech / Active Sound",
                confidence = (0.50f + (rms * 2.0f)).coerceAtMost(0.90f),
                severity = SoundSeverity.None,
                isEmergency = false,
            )
            rms > 0.005f -> ClassificationResult(
                category = "background",
                displayLabel = "Low Level Sound",
                confidence = 0.75f,
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
