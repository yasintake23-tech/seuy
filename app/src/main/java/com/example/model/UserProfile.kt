package com.example.model

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val birthDate: String = "", // e.g. "14.05.2000" or "2000-05-14"
    val email: String = "",
    val avatarPreset: String = "heart_rose", // "heart_rose", "bear_cute", "star_gold", "flower_pink", "couple_silhouette", "cat_white"
    val avatarBase64: String? = null,
    val profileImageUrl: String? = avatarBase64,
    val pairingCode: String = "",
    val partnerId: String? = null,
    val partnerName: String? = null,
    val isPaired: Boolean = false,
    val pairedAt: Long? = null, // timestamp in ms
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis()
) {
    val displayPhotoUrl: String?
        get() = profileImageUrl?.takeIf { it.isNotBlank() } ?: avatarBase64?.takeIf { it.isNotBlank() }

    fun toMap(): Map<String, Any?> {
        val effectivePhoto = profileImageUrl ?: avatarBase64
        return mapOf(
            "userId" to userId,
            "displayName" to displayName,
            "birthDate" to birthDate,
            "email" to email,
            "avatarPreset" to avatarPreset,
            "avatarBase64" to effectivePhoto,
            "profileImageUrl" to effectivePhoto,
            "pairingCode" to pairingCode,
            "partnerId" to partnerId,
            "partnerName" to partnerName,
            "isPaired" to isPaired,
            "pairedAt" to pairedAt,
            "notificationsEnabled" to notificationsEnabled,
            "createdAt" to createdAt,
            "lastActive" to lastActive
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            val photo = (map["profileImageUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: (map["avatarBase64"] as? String)?.takeIf { it.isNotBlank() }

            return UserProfile(
                userId = (map["userId"] as? String) ?: "",
                displayName = (map["displayName"] as? String) ?: "",
                birthDate = (map["birthDate"] as? String) ?: "",
                email = (map["email"] as? String) ?: "",
                avatarPreset = (map["avatarPreset"] as? String) ?: "heart_rose",
                avatarBase64 = photo,
                profileImageUrl = photo,
                pairingCode = (map["pairingCode"] as? String) ?: "",
                partnerId = map["partnerId"] as? String,
                partnerName = map["partnerName"] as? String,
                isPaired = (map["isPaired"] as? Boolean) ?: false,
                pairedAt = (map["pairedAt"] as? Number)?.toLong(),
                notificationsEnabled = (map["notificationsEnabled"] as? Boolean) ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastActive = (map["lastActive"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

sealed interface PairingResult {
    data class Success(val partnerName: String) : PairingResult
    data class Error(val message: String) : PairingResult
}
