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
     * Upload an image to Cloudflare R2 or return Base64 fallback if R2 is not configured yet.
     */
    suspend fun uploadImageUri(
        uri: Uri,
        folder: String = "photos",
        maxWidth: Int = 1080,
        maxHeight: Int = 1080
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
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
            val imageBytes = outputStream.toByteArray()

            if (R2Config.isConfigured) {
                val objectKey = "$folder/${UUID.randomUUID()}.jpg"
                uploadToR2(imageBytes, objectKey, "image/jpeg")
            } else {
                // If R2 credentials are not yet entered, use clean base64 data url as seamless local fallback
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                Result.success("data:image/jpeg;base64,$base64")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed", e)
            Result.failure(e)
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
                val publicUrl = if (R2Config.publicDomain.isNotBlank()) {
                    val base = R2Config.publicDomain.trimEnd('/')
                    "$base/$objectKey"
                } else {
                    endpoint
                }
                Result.success(publicUrl)
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
