package com.example.model

data class CoupleMemory(
    val id: String = "",
    val title: String = "",
    val caption: String = "",
    val date: String = "",
    val location: String = "",
    val imagePreset: String = "romantic_sunset",
    val imageBase64: String? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val likesCount: Int = 1,
    val isLikedByMe: Boolean = true,
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "caption" to caption,
        "date" to date,
        "location" to location,
        "imagePreset" to imagePreset,
        "imageBase64" to imageBase64,
        "imageUrl" to imageUrl,
        "isFavorite" to isFavorite,
        "likesCount" to likesCount,
        "isLikedByMe" to isLikedByMe,
        "authorName" to authorName,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): CoupleMemory = CoupleMemory(
            id = (map["id"] as? String) ?: "",
            title = (map["title"] as? String) ?: "",
            caption = (map["caption"] as? String) ?: "",
            date = (map["date"] as? String) ?: "",
            location = (map["location"] as? String) ?: "",
            imagePreset = (map["imagePreset"] as? String) ?: "romantic_sunset",
            imageBase64 = map["imageBase64"] as? String,
            imageUrl = map["imageUrl"] as? String,
            isFavorite = (map["isFavorite"] as? Boolean) ?: false,
            likesCount = (map["likesCount"] as? Number)?.toInt() ?: 1,
            isLikedByMe = (map["isLikedByMe"] as? Boolean) ?: true,
            authorName = (map["authorName"] as? String) ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

data class RelationshipMilestone(
    val id: String,
    val title: String,
    val dateDescription: String,
    val iconEmoji: String,
    val note: String
)
