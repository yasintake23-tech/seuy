package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.example.model.UserProfile

class ProfileRepository(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val r2StorageRepository: R2StorageRepository
) {
    private val TAG = "ProfileRepository"

    @OptIn(ExperimentalCoilApi::class)
    private fun clearCoilCache(url: String?) {
        try {
            val imageLoader = ImageLoader(context)
            if (!url.isNullOrBlank()) {
                imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(url))
                imageLoader.diskCache?.remove(url)
            } else {
                imageLoader.memoryCache?.clear()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing Coil cache: ${e.message}")
        }
    }

    suspend fun updateProfilePhoto(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            val uploadRes = r2StorageRepository.uploadImageUri(imageUri, "profile_photos")
            val publicUrl = uploadRes.getOrNull()
            if (publicUrl.isNullOrBlank()) {
                return Result.failure(Exception("Görsel yüklenemedi. Lütfen internet bağlantınızı kontrol edin."))
            }

            // Update Firestore users/{uid} with both profileImageUrl and avatarBase64
            val updates = mapOf(
                "profileImageUrl" to publicUrl,
                "avatarBase64" to publicUrl,
                "avatarPreset" to "custom",
                "lastActive" to System.currentTimeMillis()
            )

            authRepository.updateUserProfile(userId, updates)

            // Clear cache for fresh display
            clearCoilCache(publicUrl)

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile photo", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreset(
        userId: String,
        preset: String
    ): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>(
                "avatarPreset" to preset,
                "avatarBase64" to null,
                "profileImageUrl" to null,
                "lastActive" to System.currentTimeMillis()
            )
            authRepository.updateUserProfile(userId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update preset", e)
            Result.failure(e)
        }
    }
}
