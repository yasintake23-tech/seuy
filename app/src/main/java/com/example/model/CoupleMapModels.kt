package com.example.model

data class PartnerStatus(
    val userId: String = "",
    val statusType: String = "Evde", // "Evde", "Okulda", "İşte", "Yolda", "Kahve İçiyor", "Seni Düşünüyor"
    val statusEmoji: String = "🏡",
    val statusNote: String = "Evde dinleniyor",
    val batteryPercent: Int = 85,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "statusType" to statusType,
        "statusEmoji" to statusEmoji,
        "statusNote" to statusNote,
        "batteryPercent" to batteryPercent,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PartnerStatus = PartnerStatus(
            userId = (map["userId"] as? String) ?: "",
            statusType = (map["statusType"] as? String) ?: "Evde",
            statusEmoji = (map["statusEmoji"] as? String) ?: "🏡",
            statusNote = (map["statusNote"] as? String) ?: "Evde dinleniyor",
            batteryPercent = (map["batteryPercent"] as? Number)?.toInt() ?: 85,
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

data class MemoryPin(
    val id: String = "",
    val title: String = "",
    val locationName: String = "",
    val category: String = "Kafe", // "Kafe", "Doğa", "Restoran", "Yürüyüş", "Özel"
    val date: String = "",
    val note: String = "",
    val iconEmoji: String = "📍",
    val posX: Float = 0.5f, // Normalized 0..1 position on romantic map canvas
    val posY: Float = 0.5f,
    val addedByName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "locationName" to locationName,
        "category" to category,
        "date" to date,
        "note" to note,
        "iconEmoji" to iconEmoji,
        "posX" to posX,
        "posY" to posY,
        "addedByName" to addedByName,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): MemoryPin = MemoryPin(
            id = (map["id"] as? String) ?: "",
            title = (map["title"] as? String) ?: "",
            locationName = (map["locationName"] as? String) ?: "",
            category = (map["category"] as? String) ?: "Kafe",
            date = (map["date"] as? String) ?: "",
            note = (map["note"] as? String) ?: "",
            iconEmoji = (map["iconEmoji"] as? String) ?: "📍",
            posX = (map["posX"] as? Number)?.toFloat() ?: 0.5f,
            posY = (map["posY"] as? Number)?.toFloat() ?: 0.5f,
            addedByName = (map["addedByName"] as? String) ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
