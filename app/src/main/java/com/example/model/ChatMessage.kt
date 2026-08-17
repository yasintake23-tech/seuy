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
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "senderName" to senderName,
        "text" to (if (isDeleted) "" else text),
        "messageText" to (if (isDeleted) "" else text),
        "timestamp" to timestamp,
        "reactionEmoji" to (if (isDeleted) null else reactionEmoji),
        "isPhoto" to (if (isDeleted) false else isPhoto),
        "photoPreset" to (if (isDeleted) null else photoPreset),
        "imageUrl" to (if (isDeleted) null else imageUrl),
        "isRead" to isRead,
        "replyToText" to replyToText,
        "replyToSenderName" to replyToSenderName,
        "replyToId" to replyToId,
        "isEdited" to isEdited,
        "isDeleted" to isDeleted,
        "deleted" to isDeleted
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ChatMessage {
            val isDel = when (val d = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> d
                is String -> d.equals("true", ignoreCase = true) || d == "1"
                is Number -> d.toInt() == 1
                else -> false
            }

            val rawText = (map["messageText"] as? String) ?: (map["text"] as? String) ?: ""
            val rawImage = map["imageUrl"] as? String

            return ChatMessage(
                id = (map["id"] as? String) ?: "",
                senderId = (map["senderId"] as? String) ?: "",
                receiverId = (map["receiverId"] as? String) ?: "",
                senderName = (map["senderName"] as? String) ?: "",
                text = if (isDel) "" else rawText,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                reactionEmoji = if (isDel) null else (map["reactionEmoji"] as? String),
                isPhoto = if (isDel) false else ((map["isPhoto"] as? Boolean) ?: false),
                photoPreset = if (isDel) null else (map["photoPreset"] as? String),
                imageUrl = if (isDel) null else rawImage,
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
