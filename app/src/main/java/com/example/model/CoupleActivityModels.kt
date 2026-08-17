package com.example.model

data class BucketItem(
    val id: String = "",
    val title: String = "",
    val category: String = "Aktivite", // "Gezilecek", "Aktivite", "Romantik", "Gurme"
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val addedByName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "category" to category,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt,
        "addedByName" to addedByName,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): BucketItem = BucketItem(
            id = (map["id"] as? String) ?: "",
            title = (map["title"] as? String) ?: "",
            category = (map["category"] as? String) ?: "Aktivite",
            isCompleted = (map["isCompleted"] as? Boolean) ?: false,
            completedAt = (map["completedAt"] as? Number)?.toLong(),
            addedByName = (map["addedByName"] as? String) ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

data class SecretLoveNote(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val unlockCondition: String = "Her Zaman Açılabilir",
    val isUnlocked: Boolean = false,
    val authorName: String = "",
    val iconEmoji: String = "💌",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "content" to content,
        "unlockCondition" to unlockCondition,
        "isUnlocked" to isUnlocked,
        "authorName" to authorName,
        "iconEmoji" to iconEmoji,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SecretLoveNote = SecretLoveNote(
            id = (map["id"] as? String) ?: "",
            title = (map["title"] as? String) ?: "",
            content = (map["content"] as? String) ?: "",
            unlockCondition = (map["unlockCondition"] as? String) ?: "Her Zaman Açılabilir",
            isUnlocked = (map["isUnlocked"] as? Boolean) ?: false,
            authorName = (map["authorName"] as? String) ?: "",
            iconEmoji = (map["iconEmoji"] as? String) ?: "💌",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

data class DailyCoupleQuestion(
    val id: String = "",
    val question: String = "",
    val myAnswer: String = "",
    val partnerAnswer: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "question" to question,
        "myAnswer" to myAnswer,
        "partnerAnswer" to partnerAnswer,
        "date" to date,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DailyCoupleQuestion = DailyCoupleQuestion(
            id = (map["id"] as? String) ?: "",
            question = (map["question"] as? String) ?: "",
            myAnswer = (map["myAnswer"] as? String) ?: "",
            partnerAnswer = (map["partnerAnswer"] as? String) ?: "",
            date = (map["date"] as? String) ?: "",
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

data class WheelOption(
    val title: String,
    val emoji: String,
    val colorHex: Long
)
