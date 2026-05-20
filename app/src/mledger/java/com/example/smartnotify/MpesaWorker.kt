package com.example.smartnotify

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MpesaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
        
    private val dao = AppDatabase.getDatabase(context).mpesaDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queuedMessages = dao.getQueuedMessages()
        if (queuedMessages.isEmpty()) return@withContext Result.success()

        var allSuccessful = true

        for (message in queuedMessages) {
            try {
                val json = JSONObject()
                json.put("key", message.rawBody)
                
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val url = "https://kingw10-my-mpesa-accountant.hf.space/webhook/mpesa"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "SmartNotify-App")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        dao.update(message.copy(status = "SENT"))
                        Log.d("MpesaWorker", "SENT SUCCESS: ${message.id}")
                    } else {
                        Log.e("MpesaWorker", "SERVER REJECTED: ${response.code}")
                        allSuccessful = false
                    }
                }
                delay(1000)
            } catch (e: Exception) {
                Log.e("MpesaWorker", "NETWORK ERROR: ${e.message}")
                allSuccessful = false
            }
        }

        if (allSuccessful) Result.success() else Result.retry()
    }
}
