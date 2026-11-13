package com.example.data.repository

import android.util.Log
import com.example.data.model.AiTuningRecommendation
import com.example.data.model.HondaEcuModel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiTuningRepository {

    private val tuningSchema = Schema.obj(
        properties = mapOf(
            "fuelDeltaPct" to Schema.double(description = "Recommended global Fuel VE percentage adjustment (-10.0 to +20.0)"),
            "ignitionDeltaDeg" to Schema.double(description = "Recommended Ignition timing advance offset in degrees (-4.0 to +6.0)"),
            "recommendedLimiterRpm" to Schema.integer(description = "Safe recommended RPM rev limiter"),
            "targetAfr" to Schema.double(description = "Recommended full-load target AFR (12.2 to 14.2)"),
            "summary" to Schema.string(description = "Concise Indonesian engineering explanation for the mechanic"),
            "warnings" to Schema.array(
                items = Schema.string(),
                description = "Safety or mechanical warnings (e.g., octane requirement, valve springs)"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tuningSchema
            }
        )
    }

    suspend fun generateTuningRecommendation(
        ecu: HondaEcuModel,
        engineMods: String,
        fuelOctane: String,
        goal: String
    ): Result<AiTuningRecommendation> {
        return try {
            val prompt = """
                Anda adalah Master Tuner balap dan kalibrator ECU Honda PGM-FI profesional di Indonesia.
                Motor: ${ecu.name} (${ecu.displacementCc}cc, Part Number ${ecu.partNumber}, Limiter Standar ${ecu.stockRevLimiter} RPM).
                Modifikasi Mesin: $engineMods
                Bahan Bakar: $fuelOctane
                Target Penggunaan: $goal

                Berikan rekomendasi penyesuaian Fuel VE (%), Ignition Advance (derajat), Rev Limiter aman, Target AFR beban penuh, ringkasan teknis bahasa Indonesia, dan peringatan keamanan mekanis.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI tidak mengembalikan respons.")

            val json = JSONObject(rawJson)
            val warningsArr = json.optJSONArray("warnings")
            val warningsList = if (warningsArr != null) {
                (0 until warningsArr.length()).map { warningsArr.getString(it) }
            } else {
                emptyList()
            }

            Result.success(
                AiTuningRecommendation(
                    fuelDeltaPct = json.optDouble("fuelDeltaPct", 5.0),
                    ignitionDeltaDeg = json.optDouble("ignitionDeltaDeg", 2.0),
                    recommendedLimiterRpm = json.optInt("recommendedLimiterRpm", ecu.stockRevLimiter + 800),
                    targetAfr = json.optDouble("targetAfr", 12.8),
                    summary = json.optString("summary", "Rekomendasi kalibrasi siap diterapkan."),
                    warnings = warningsList
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI tuning generation timed out", e)
            Result.failure(Exception("Waktu analisis AI habis (60 detik). Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI tuning generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiTuningRepository"
    }
}
