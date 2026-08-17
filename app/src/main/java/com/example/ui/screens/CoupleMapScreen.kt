package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MemoryPin
import com.example.model.PartnerStatus
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.theme.BorderSoft
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SoftCoralContainer
import com.example.ui.theme.SoftCoralDark
import com.example.ui.theme.SoftCoralPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmCreamBackground
import com.example.ui.theme.WarmCreamContainer
import com.example.ui.theme.WarmCreamSurface
import com.example.ui.theme.WarmGold

@Composable
fun CoupleMapScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    myStatus: PartnerStatus,
    partnerStatus: PartnerStatus,
    memoryPins: List<MemoryPin>,
    onUpdateMyStatus: (String, String, String) -> Unit,
    onAddMemoryPin: (String, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusPicker by remember { mutableStateOf(false) }
    var showAddPinDialog by remember { mutableStateOf(false) }
    var selectedPin by remember { mutableStateOf<MemoryPin?>(null) }

    val partnerDisplayName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aşk Haritamız",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "📍", fontSize = 20.sp)
                    }
                    Text(
                        text = "Anlık durumlarınız ve birlikte keşfettiğiniz özel yerler",
                        fontSize = 12.sp,
                        color = SlateNavy
                    )
                }
            }
        }

        // Live Statuses Card (Sen & Sevgilin)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Anlık Durum & Konum Takibi 📡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // My Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarmCreamContainer.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStatusPicker = true }
                                .testTag("open_my_status_picker")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AvatarImage(preset = currentUser.avatarPreset, base64 = currentUser.avatarBase64, size = 34.dp)
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Düzenle",
                                        tint = SoftCoralPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Sen: ${myStatus.statusEmoji} ${myStatus.statusType}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepCharcoal
                                )
                                Text(
                                    text = myStatus.statusNote,
                                    fontSize = 11.sp,
                                    color = SlateNavy,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = SageGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = "%${myStatus.batteryPercent} Şarj", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }

                        // Partner Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SoftCoralContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AvatarImage(preset = partnerUser?.avatarPreset ?: "flower_pink", base64 = partnerUser?.avatarBase64, size = 34.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(SageGreen)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$partnerDisplayName: ${partnerStatus.statusEmoji} ${partnerStatus.statusType}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepCharcoal
                                )
                                Text(
                                    text = partnerStatus.statusNote,
                                    fontSize = 11.sp,
                                    color = SlateNavy,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = SageGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = "%${partnerStatus.batteryPercent} Şarj", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Realtime Live Status Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmGold.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = SoftCoralDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (partnerStatus.statusNote.isNotBlank()) {
                                    "$partnerDisplayName şu an: ${partnerStatus.statusEmoji} ${partnerStatus.statusType} • \"${partnerStatus.statusNote}\""
                                } else {
                                    "📡 Canlı durum ve ortak anı lokasyonları anında senkronize edilir ✨"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepCharcoal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Stylized Interactive Couple Canvas Map
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ortak Hatıra Lokasyonları ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DeepCharcoal
                        )
                        IconButton(
                            onClick = { showAddPinDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SoftCoralPrimary)
                                .testTag("add_memory_pin_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Yeni Lokasyon Ekle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Map View Canvas with Pins
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFF7F2EC),
                                        Color(0xFFEFE8DE),
                                        Color(0xFFE5DCD0)
                                    )
                                )
                            )
                            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (memoryPins.isEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🗺️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Haritada henüz lokasyon pini yok",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepCharcoal
                                )
                                Text(
                                    text = "Sağ üstteki '+' butonuna basarak ilk anı noktanızı ekleyin ✨",
                                    fontSize = 11.sp,
                                    color = SlateNavy,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Drawing grid / map paths
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                if (memoryPins.size >= 2) {
                                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                    for (i in 0 until memoryPins.size - 1) {
                                        val p1 = memoryPins[i]
                                        val p2 = memoryPins[i + 1]
                                        drawLine(
                                            color = SoftCoralPrimary.copy(alpha = 0.5f),
                                            start = Offset(p1.posX * w, p1.posY * h),
                                            end = Offset(p2.posX * w, p2.posY * h),
                                            strokeWidth = 3f,
                                            pathEffect = pathEffect
                                        )
                                    }
                                }
                            }

                            // Pins overlay
                            memoryPins.forEach { pin ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(
                                            start = (pin.posX * 280).dp.coerceIn(8.dp, 280.dp),
                                            top = (pin.posY * 140).dp.coerceIn(8.dp, 140.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WarmCreamSurface)
                                        .border(1.5.dp, SoftCoralPrimary, RoundedCornerShape(12.dp))
                                        .clickable { selectedPin = pin }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = pin.iconEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = pin.locationName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepCharcoal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Memory Pins List
        item {
            Text(
                text = "Ziyaret Edilen Anı Noktaları (${memoryPins.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )
        }

        if (memoryPins.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz kayıtlı bir anı mekanı bulunmuyor 📍",
                        fontSize = 12.sp,
                        color = SlateNavy
                    )
                }
            }
        } else {
            items(memoryPins, key = { it.id }) { pin ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSoft, RoundedCornerShape(14.dp))
                        .clickable { selectedPin = pin }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftCoralContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = pin.iconEmoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pin.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepCharcoal
                            )
                            Text(
                                text = "📍 ${pin.locationName} • 📅 ${pin.date}",
                                fontSize = 11.sp,
                                color = SlateNavy
                            )
                            if (pin.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pin.note,
                                    fontSize = 12.sp,
                                    color = DeepCharcoal.copy(alpha = 0.8f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(50.dp)) }
    }

    // Status Picker Dialog
    if (showStatusPicker) {
        StatusPickerDialog(
            currentStatus = myStatus,
            onDismiss = { showStatusPicker = false },
            onSelect = { type, emoji, note ->
                onUpdateMyStatus(type, emoji, note)
                showStatusPicker = false
            }
        )
    }

    // Add Memory Pin Dialog
    if (showAddPinDialog) {
        AddMemoryPinDialog(
            onDismiss = { showAddPinDialog = false },
            onAdd = { title, loc, cat, date, note ->
                onAddMemoryPin(title, loc, cat, date, note)
                showAddPinDialog = false
            }
        )
    }

    // Selected Pin View Dialog
    selectedPin?.let { pin ->
        AlertDialog(
            onDismissRequest = { selectedPin = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pin.iconEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = pin.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "📍 Lokasyon: ${pin.locationName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(text = "📅 Tarih: ${pin.date}", fontSize = 12.sp, color = SlateNavy)
                    Text(text = "🏷️ Kategori: ${pin.category}", fontSize = 12.sp, color = SlateNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarmCreamContainer)
                            .padding(10.dp)
                    ) {
                        Text(text = pin.note, fontSize = 13.sp, color = DeepCharcoal)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedPin = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
                ) {
                    Text("Tamam")
                }
            }
        )
    }
}

@Composable
fun StatusPickerDialog(
    currentStatus: PartnerStatus,
    onDismiss: () -> Unit,
    onSelect: (String, String, String) -> Unit
) {
    val presets = listOf(
        Triple("Evde", "🏡", "Evde dinleniyor"),
        Triple("Okulda", "🎓", "Ders çalışıyor"),
        Triple("İşte", "💼", "İşle meşgul"),
        Triple("Yolda", "🚗", "Yolda seyahat ediyor"),
        Triple("Kahve İçiyor", "☕", "Kahve molası veriyor"),
        Triple("Seni Düşünüyor", "💭", "Seni çok özledi ve düşünüyor"),
        Triple("Spor Yapıyor", "🏃", "Sporda enerji depoluyor")
    )

    var customNote by remember { mutableStateOf(currentStatus.statusNote) }
    var selectedItem by remember { mutableStateOf(presets.firstOrNull { it.first == currentStatus.statusType } ?: presets.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Durumunu Güncelle 📡", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Şu an neredesin / ne yapıyorsun?", fontSize = 12.sp, color = SlateNavy)

                presets.forEach { item ->
                    val isSelected = selectedItem.first == item.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SoftCoralContainer else WarmCreamSurface)
                            .border(1.dp, if (isSelected) SoftCoralPrimary else BorderSoft, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedItem = item
                                customNote = item.third
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.second, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.first,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = DeepCharcoal
                        )
                    }
                }

                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Özel Not") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedLabelColor = SoftCoralPrimary,
                        unfocusedLabelColor = SlateNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(selectedItem.first, selectedItem.second, customNote.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun AddMemoryPinDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Kafe") }
    var date by remember { mutableStateOf("Bugün") }
    var note by remember { mutableStateOf("") }

    val categories = listOf("Kafe ☕", "Doğa 🌊", "Restoran 🍕", "Özel ✨")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Anı Lokasyonu Ekle 📍", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Anı Başlığı") },
                    placeholder = { Text("Örn: İlk Kahvemiz") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedLabelColor = SoftCoralPrimary,
                        unfocusedLabelColor = SlateNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Mekan / Lokasyon Adı") },
                    placeholder = { Text("Örn: Moda Sahil Kafe") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedLabelColor = SoftCoralPrimary,
                        unfocusedLabelColor = SlateNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tarih") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedLabelColor = SoftCoralPrimary,
                        unfocusedLabelColor = SlateNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Romantik Notunuz") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepCharcoal,
                        unfocusedTextColor = DeepCharcoal,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = SoftCoralPrimary,
                        focusedBorderColor = SoftCoralPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedLabelColor = SoftCoralPrimary,
                        unfocusedLabelColor = SlateNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && locationName.isNotBlank()) {
                        onAdd(title.trim(), locationName.trim(), category, date.trim(), note.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Haritaya Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
