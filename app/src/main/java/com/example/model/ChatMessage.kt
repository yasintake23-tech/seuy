package com.example.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reactionEmoji: String? = null,
    val isPhoto: Boolean = false,
    val photoPreset: String? = null,
    val mediaUrl: String? = null,
    val imageUrl: String? = mediaUrl,
    val isRead: Boolean = false,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    val displayMediaUrl: String?
        get() = mediaUrl?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }

    val effectiveMediaUrl: String?
        get() = displayMediaUrl

    fun toMap(): Map<String, Any?> {
        val effectiveMedia = if (isDeleted) null else (mediaUrl ?: imageUrl)
        return mapOf(
            "id" to id,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "senderName" to senderName,
            "text" to (if (isDeleted) "" else text),
            "messageText" to (if (isDeleted) "" else text),
            "timestamp" to timestamp,
            "reactionEmoji" to (if (isDeleted) null else reactionEmoji),
            "isPhoto" to (if (isDeleted) false else (isPhoto || !effectiveMedia.isNullOrBlank())),
            "photoPreset" to (if (isDeleted) null else photoPreset),
            "mediaUrl" to effectiveMedia,
            "imageUrl" to effectiveMedia,
            "isRead" to isRead,
            "replyToText" to replyToText,
            "replyToSenderName" to replyToSenderName,
            "replyToId" to replyToId,
            "isEdited" to isEdited,
            "isDeleted" to isDeleted,
            "deleted" to isDeleted
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ChatMessage {
            val isDel = when (val d = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> d
                is String -> d.equals("true", ignoreCase = true) || d == "1"
                is Number -> d.toInt() == 1
                else -> false
            }

            val rawText = (map["messageText"] as? String) ?: (map["text"] as? String) ?: ""
            val rawMedia = (map["mediaUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: (map["imageUrl"] as? String)?.takeIf { it.isNotBlank() }

            return ChatMessage(
                id = (map["id"] as? String) ?: "",
                senderId = (map["senderId"] as? String) ?: "",
                receiverId = (map["receiverId"] as? String) ?: "",
                senderName = (map["senderName"] as? String) ?: "",
                text = if (isDel) "" else rawText,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                reactionEmoji = if (isDel) null else (map["reactionEmoji"] as? String),
                isPhoto = if (isDel) false else (((map["isPhoto"] as? Boolean) ?: false) || rawMedia != null),
                photoPreset = if (isDel) null else (map["photoPreset"] as? String),
                mediaUrl = if (isDel) null else rawMedia,
                imageUrl = if (isDel) null else rawMedia,
                isRead = (map["isRead"] as? Boolean) ?: false,
                replyToText = map["replyToText"] as? String,
                replyToSenderName = map["replyToSenderName"] as? String,
                replyToId = map["replyToId"] as? String,
                isEdited = (map["isEdited"] as? Boolean) ?: false,
                isDeleted = isDel
            )
        }
    }
}
