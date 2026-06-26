package com.example.chattingapp.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import com.example.chattingapp.BuildConfig

class CloudinaryRepository(
    private val context: Context
) {
    private val client = OkHttpClient()

    companion object {
        private val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        private val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
    }

    suspend fun uploadAvatar(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"

        if (cloudName.isBlank() || cloudName == "ten_cloud_name_cua_ban") {
            throw IllegalStateException("Thiếu CLOUDINARY_CLOUD_NAME trong local.properties")
        }

        if (uploadPreset.isBlank()) {
            throw IllegalStateException("Thiếu CLOUDINARY_UPLOAD_PRESET trong local.properties")
        }
        if (!mimeType.startsWith("image/")) {
            throw IllegalArgumentException("File được chọn không phải ảnh")
        }

        val imageBytes = contentResolver.openInputStream(imageUri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IOException("Không đọc được ảnh đã chọn")

        if (imageBytes.size > MAX_IMAGE_SIZE_BYTES) {
            throw IllegalArgumentException("Ảnh không được vượt quá 5MB")
        }

        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart(
                "file",
                "avatar_${System.currentTimeMillis()}.$extension",
                imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("Upload Cloudinary thất bại: ${response.code} - $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getString("secure_url")
        }
    }
}