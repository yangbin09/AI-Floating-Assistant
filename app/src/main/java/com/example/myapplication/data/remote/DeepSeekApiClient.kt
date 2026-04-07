package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.model.DeepSeekMessage
import com.example.myapplication.data.remote.model.DeepSeekRequest
import com.example.myapplication.data.remote.model.DeepSeekResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepSeekApiClient(
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.deepseek.com/chat/completions"
    private val mediaType = "application/json".toMediaType()

    suspend fun sendMessage(request: DeepSeekRequest): Result<DeepSeekResponse> {
        return try {
            val jsonRequest = JSONObject().apply {
                put("model", request.model)
                put("max_tokens", request.maxTokens)
                put("temperature", request.temperature)
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
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(httpRequest).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val deepSeekResponse = parseDeepSeekResponse(body)
                    Result.success(deepSeekResponse)
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

    private fun parseDeepSeekResponse(json: String): DeepSeekResponse {
        val jsonObject = JSONObject(json)
        val choicesArray = jsonObject.getJSONArray("choices")
        val choices = mutableListOf<com.example.myapplication.data.remote.model.DeepSeekChoice>()

        for (i in 0 until choicesArray.length()) {
            val choiceObj = choicesArray.getJSONObject(i)
            val messageObj = choiceObj.getJSONObject("message")
            choices.add(
                com.example.myapplication.data.remote.model.DeepSeekChoice(
                    index = choiceObj.getInt("index"),
                    message = com.example.myapplication.data.remote.model.DeepSeekMessage(
                        role = messageObj.getString("role"),
                        content = messageObj.getString("content")
                    ),
                    finishReason = if (choiceObj.has("finish_reason") && !choiceObj.isNull("finish_reason"))
                        choiceObj.getString("finish_reason") else null
                )
            )
        }

        val usage = if (jsonObject.has("usage") && !jsonObject.isNull("usage")) {
            val usageObj = jsonObject.getJSONObject("usage")
            com.example.myapplication.data.remote.model.DeepSeekUsage(
                promptTokens = if (usageObj.has("prompt_tokens") && !usageObj.isNull("prompt_tokens"))
                    usageObj.getInt("prompt_tokens") else null,
                completionTokens = if (usageObj.has("completion_tokens") && !usageObj.isNull("completion_tokens"))
                    usageObj.getInt("completion_tokens") else null,
                totalTokens = if (usageObj.has("total_tokens") && !usageObj.isNull("total_tokens"))
                    usageObj.getInt("total_tokens") else null
            )
        } else null

        return com.example.myapplication.data.remote.model.DeepSeekResponse(
            id = jsonObject.getString("id"),
            `object` = jsonObject.getString("object"),
            created = jsonObject.getLong("created"),
            model = jsonObject.getString("model"),
            choices = choices,
            usage = usage
        )
    }
}
