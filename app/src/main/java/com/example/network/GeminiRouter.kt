package com.example.network

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

// The target parsed output structure
@JsonClass(generateAdapter = true)
data class ParsedInvoice(
    val date: String? = null,
    val vehicle_number: String? = null,
    val gate_pass_number: String? = null,
    val net_weight: Double? = null,
    val balances: List<Double>? = null,
    val transport_charges: Double? = null,
    val commission_rate: Double? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Single central moshi instance
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun parseLogisticsText(
        apiKey: String,
        inputText: String,
        currentDate: String
    ): ParsedInvoice? {
        val systemInstructionText = """
            You are an expert logistics accountant. Parse the unstructured logistics text pasted or dictated by the user into a structured, single, flat JSON object.
            Ensure the response is extremely valid JSON conforming strictly to the requested schema. Do not output anything outside the JSON object.
            
            Use the following exact JSON keys:
            - "date" (String): The parsed entry date in "YYYY-MM-DD" format. If the date is relative (e.g. "today", "arrived today") or missing from the text, use the current date provided: '$currentDate'.
            - "vehicle_number" (String): The parsed truck, transport, or vehicle license plate number/id. Example: "ABC-123" or "1234". Default to empty string "" if not found.
            - "gate_pass_number" (String): The parsed gate pass number, token number, or slip number. Default to empty string "" if not found.
            - "net_weight" (Double): Decimal total weight in tons/units. Example: 12.5. Default to 0.0 if not found.
            - "balances" (List of Double): An array of individual partial payments, driver balances, collections, or balance values mentioned. Example: [5000.0, 3000.0]. Default to an empty array [] if not found.
            - "transport_charges" (Double): Transport charges, shipping costs, or driver expenses. Example: 1200.0. Default to 0.0 if not found.
            - "commission_rate" (Double): Commissions rate per unit of net weight. Example: 50.0. Default to 0.0 if not found.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = inputText)))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.1
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("GeminiParser", "Raw response from Gemini text: $jsonText")

            if (jsonText != null) {
                val adapter = moshi.adapter(ParsedInvoice::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiParser", "Failed to contact or parse Gemini output", e)
            null
        }
    }

    suspend fun parseLogisticsImage(
        apiKey: String,
        base64Image: String,
        currentDate: String
    ): ParsedInvoice? {
        val systemInstructionText = """
            You are an expert logistics accountant. Parse the uploaded logistics document/receipt into a structured, single, flat JSON object.
            Ensure the response is extremely valid JSON conforming strictly to the requested schema. Do not output anything outside the JSON object.
            
            Use the following exact JSON keys:
            - "date" (String): The parsed entry date in "YYYY-MM-DD" format. If the date is relative or missing from the text, use the current date provided: '$currentDate'.
            - "vehicle_number" (String): The parsed truck, transport, or vehicle license plate number/id. Default to empty string "" if not found.
            - "gate_pass_number" (String): The parsed gate pass number, token number, or slip number. Default to empty string "" if not found.
            - "net_weight" (Double): Decimal total weight in tons/units. Default to 0.0 if not found.
            - "balances" (List of Double): An array of individual partial payments, driver balances, collections, or balance values mentioned. Default to an empty array [] if not found.
            - "transport_charges" (Double): Transport charges, shipping costs, or driver expenses. Default to 0.0 if not found.
            - "commission_rate" (Double): Commissions rate per unit of net weight. Default to 0.0 if not found.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(
                    GeminiPart(
                        inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = base64Image
                        )
                    ),
                    GeminiPart(text = "Extract the fields from this document.")
                ))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.1
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("GeminiParser", "Raw response from Gemini image: $jsonText")

            if (jsonText != null) {
                val adapter = moshi.adapter(ParsedInvoice::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiParser", "Failed to contact or parse Gemini image output", e)
            null
        }
    }
}
