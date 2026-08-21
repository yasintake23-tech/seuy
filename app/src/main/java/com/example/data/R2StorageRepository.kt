package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.config.R2Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class R2StorageRepository(private val context: Context) {
    private val TAG = "R2StorageRepo"
    private val client = OkHttpClient()

    /**
     * Compress an image from Uri directly into base64 data string.
     */
    fun compressUriToBase64(
        uri: Uri,
        maxWidth: Int = 600,
        maxHeight: Int = 600,
        quality: Int = 80
    ): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null

            if (originalBitmap == null) return null

            val scaledBitmap = scaleBitmap(originalBitmap, maxWidth, maxHeight)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val imageBytes = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Uri to base64", e)
            null
        }
    }

    /**
     * Upload an image to Cloudflare R2 or return Base64 fallback if R2 is not configured yet.
     */
    suspend fun uploadImageUri(
        uri: Uri,
        folder: String = "photos",
        maxWidth: Int = 800,
        maxHeight: Int = 800
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return@withContext Result.failure(Exception("Görsel okunamadı"))
            }

            // Scale down for mobile efficiency
            val scaledBitmap = scaleBitmap(originalBitmap, maxWidth, maxHeight)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()

            val base64Fallback = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            if (R2Config.isConfigured) {
                val objectKey = "$folder/${UUID.randomUUID()}.jpg"
                val r2Res = uploadToR2(imageBytes, objectKey, "image/jpeg")
                if (r2Res.isSuccess) {
                    r2Res
                } else {
                    Log.w(TAG, "R2 upload failed, falling back to base64: ${r2Res.exceptionOrNull()?.message}")
                    Result.success(base64Fallback)
                }
            } else {
                // If R2 credentials are not yet entered, use clean base64 data url as seamless local fallback
                Result.success(base64Fallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generate standard AWS S3 SigV4 Pre-signed GET URL for Cloudflare R2.
     * This allows any client (Coil, browser) to download the private R2 object without 403 Forbidden.
     */
    fun generatePresignedGetUrl(objectKey: String, expiresInSeconds: Long = 604800L): String {
        return try {
            val region = "auto"
            val service = "s3"
            val host = "${R2Config.accountId}.r2.cloudflarestorage.com"
            val canonicalUri = "/${R2Config.bucketName}/$objectKey"

            val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val now = Date()
            val amzDate = dateFormat.format(now)
            val dateStamp = dateStampFormat.format(now)
            val credentialScope = "$dateStamp/$region/$service/aws4_request"
            val credential = "${R2Config.accessKeyId}/$credentialScope"
            val encodedCredential = java.net.URLEncoder.encode(credential, "UTF-8")

            val canonicalQuery = "X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Credential=$encodedCredential" +
                "&X-Amz-Date=$amzDate" +
                "&X-Amz-Expires=$expiresInSeconds" +
                "&X-Amz-SignedHeaders=host"

            val canonicalHeaders = "host:$host\n"
            val signedHeaders = "host"
            val payloadHash = "UNSIGNED-PAYLOAD"

            val canonicalRequest = "GET\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
            val algorithm = "AWS4-HMAC-SHA256"
            val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            val signingKey = getSignatureKey(R2Config.secretAccessKey, dateStamp, region, service)
            val signature = hmacSha256Hex(signingKey, stringToSign)

            "https://$host$canonicalUri?$canonicalQuery&X-Amz-Signature=$signature"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating presigned GET url", e)
            if (R2Config.publicDomain.isNotBlank()) {
                val base = R2Config.publicDomain.trimEnd('/')
                "$base/$objectKey"
            } else {
                "https://${R2Config.accountId}.r2.cloudflarestorage.com/${R2Config.bucketName}/$objectKey"
            }
        }
    }

    /**
     * Upload byte array directly to Cloudflare R2 bucket using standard AWS S3 SigV4.
     */
    suspend fun uploadToR2(
        data: ByteArray,
        objectKey: String,
        contentType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val region = "auto"
            val service = "s3"
            val host = "${R2Config.accountId}.r2.cloudflarestorage.com"
            val canonicalUri = "/${R2Config.bucketName}/$objectKey"
            val endpoint = "https://$host$canonicalUri"

            val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val now = Date()
            val amzDate = dateFormat.format(now)
            val dateStamp = dateStampFormat.format(now)

            val payloadHash = sha256Hex(data)

            // Canonical Request
            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            // String to Sign
            val algorithm = "AWS4-HMAC-SHA256"
            val credentialScope = "$dateStamp/$region/$service/aws4_request"
            val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            // Calculate Signature
            val signingKey = getSignatureKey(R2Config.secretAccessKey, dateStamp, region, service)
            val signature = hmacSha256Hex(signingKey, stringToSign)

            val authorizationHeader = "$algorithm Credential=${R2Config.accessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val requestBody = data.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .put(requestBody)
                .addHeader("Host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authorizationHeader)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val presignedUrl = generatePresignedGetUrl(objectKey)
                Result.success(presignedUrl)
            } else {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "R2 Upload error HTTP ${response.code}: $errBody")
                Result.failure(Exception("R2 Yükleme başarısız: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "R2 S3 exception", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratioBitmap = width.toFloat() / height.toFloat()
        var finalWidth = maxWidth
        var finalHeight = (maxWidth / ratioBitmap).toInt()

        if (finalHeight > maxHeight) {
            finalHeight = maxHeight
            finalWidth = (maxHeight * ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        val hash = hmacSha256(key, data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }
}
