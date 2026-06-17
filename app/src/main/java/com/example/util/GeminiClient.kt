package com.example.util

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun generateContent(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API_KEY_ERROR"
        }

        try {
            val base64Data = bitmap.toBase64()
            
            val partText = JSONObject().put("text", prompt)
            val partImage = JSONObject().put("inlineData", JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Data)
            )

            val parts = JSONArray().put(partText).put(partImage)
            val content = JSONObject().put("parts", parts)
            val contents = JSONArray().put(content)
            val requestBodyJson = JSONObject().put("contents", contents)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: HTTP ${response.code} ${response.message}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
                
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val replyContent = firstCandidate.optJSONObject("content")
                    val partsArray = replyContent?.optJSONArray("parts")
                    if (partsArray != null && partsArray.length() > 0) {
                        return@withContext partsArray.getJSONObject(0).optString("text")
                    }
                }
                "No text generated."
            }
        } catch (e: Exception) {
            "Exception: ${e.localizedMessage ?: "Unknown Error"}"
        }
    }

    suspend fun generateTextContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API_KEY_ERROR"
        }

        try {
            val partText = JSONObject().put("text", prompt)
            val parts = JSONArray().put(partText)
            val content = JSONObject().put("parts", parts)
            val contents = JSONArray().put(content)
            val requestBodyJson = JSONObject().put("contents", contents)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: HTTP ${response.code} ${response.message}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
                
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val replyContent = firstCandidate.optJSONObject("content")
                    val partsArray = replyContent?.optJSONArray("parts")
                    if (partsArray != null && partsArray.length() > 0) {
                        return@withContext partsArray.getJSONObject(0).optString("text")
                    }
                }
                "No text generated."
            }
        } catch (e: Exception) {
            "Exception: ${e.localizedMessage ?: "Unknown Error"}"
        }
    }
}
