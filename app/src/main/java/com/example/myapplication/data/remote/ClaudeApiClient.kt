package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.model.ClaudeMessage
import com.example.myapplication.data.remote.model.ClaudeRequest
import com.example.myapplication.data.remote.model.ClaudeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClaudeApiClient(
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.anthropic.com/v1/messages"
    private val mediaType = "application/json".toMediaType()

    suspend fun sendMessage(request: ClaudeRequest): Result<ClaudeResponse> {
        return try {
            val jsonRequest = JSONObject().apply {
                put("model", request.model)
                put("max_tokens", request.maxTokens)
                put("messages", JSONArray().apply {
                    request.messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("anthropic-dangerous-direct-browser-access", "true")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(httpRequest).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val claudeResponse = parseClaudeResponse(body)
                    Result.success(claudeResponse)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("API error: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseClaudeResponse(json: String): ClaudeResponse {
        val jsonObject = JSONObject(json)
        val contentArray = jsonObject.getJSONArray("content")
        val contentBlocks = mutableListOf<com.example.myapplication.data.remote.model.ClaudeContentBlock>()

        for (i in 0 until contentArray.length()) {
            val block = contentArray.getJSONObject(i)
            contentBlocks.add(
                com.example.myapplication.data.remote.model.ClaudeContentBlock(
                    type = block.getString("type"),
                    text = block.optString("text", null)
                )
            )
        }

        return com.example.myapplication.data.remote.model.ClaudeResponse(
            id = jsonObject.getString("id"),
            type = jsonObject.getString("type"),
            role = jsonObject.getString("role"),
            content = contentBlocks,
            model = jsonObject.getString("model"),
            stopReason = jsonObject.optString("stop_reason", null)
        )
    }
}
