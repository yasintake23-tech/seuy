package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BucketItem
import com.example.model.DailyCoupleQuestion
import com.example.model.SecretLoveNote
import com.example.model.WheelOption
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
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GamesScreen(
    bucketList: List<BucketItem>,
    secretNotes: List<SecretLoveNote>,
    dailyQuestions: List<DailyCoupleQuestion>,
    onToggleBucketItem: (String) -> Unit,
    onAddBucketItem: (String, String) -> Unit,
    onUnlockNote: (String) -> Unit,
    onAddSecretNote: (String, String, String) -> Unit,
    onAnswerDailyQuestion: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("🎡 Aşk Çarkı", "📝 Bucket List", "💌 Kilitli Sandık", "❓ Günün Sorusu")

    var showAddBucketDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var viewingNote by remember { mutableStateOf<SecretLoveNote?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Eğlence & Aktiviteler",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "🎮", fontSize = 20.sp)
                    }
                    Text(
                        text = "Birlikte eğlenin, hayaller kurun ve aşkınızı keşfedin",
                        fontSize = 12.sp,
                        color = SlateNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = SoftCoralPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                subTabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SoftCoralPrimary else SlateNavy
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedSubTab) {
                0 -> LoveWheelSection()
                1 -> BucketListSection(
                    items = bucketList,
                    onToggle = onToggleBucketItem,
                    onOpenAddDialog = { showAddBucketDialog = true }
                )
                2 -> SecretNotesSection(
                    notes = secretNotes,
                    onOpenNote = { viewingNote = it },
                    onOpenAddDialog = { showAddNoteDialog = true }
                )
                3 -> DailyQuestionsSection(
                    questions = dailyQuestions,
                    onAnswerQuestion = onAnswerDailyQuestion
                )
            }
        }

        // Add Bucket Item Dialog
        if (showAddBucketDialog) {
            AddBucketItemDialog(
                onDismiss = { showAddBucketDialog = false },
                onAdd = { title, cat ->
                    onAddBucketItem(title, cat)
                    showAddBucketDialog = false
                }
            )
        }

        // Add Secret Note Dialog
        if (showAddNoteDialog) {
            AddSecretNoteDialog(
                onDismiss = { showAddNoteDialog = false },
                onAdd = { title, content, cond ->
                    onAddSecretNote(title, content, cond)
                    showAddNoteDialog = false
                }
            )
        }

        // Viewing Note Dialog
        viewingNote?.let { note ->
            ViewSecretNoteDialog(
                note = note,
                onDismiss = { viewingNote = null },
                onUnlock = {
                    onUnlockNote(note.id)
                    viewingNote = note.copy(isUnlocked = true)
                }
            )
        }
    }
}

// ----------------------------------------------------
// 1. AŞK ÇARKI (LOVE WHEEL)
// ----------------------------------------------------
@Composable
fun LoveWheelSection() {
    val wheelOptions = remember {
        listOf(
            WheelOption("Akşam Yemeği Senden 🍝", "🍝", 0xFFE07A5F),
            WheelOption("10 Saniye Sarılma 🤗", "🤗", 0xFF81B29A),
            WheelOption("Sürpriz Masaj 💆‍♂️", "💆", 0xFFF2CC8F),
            WheelOption("Film Seçimi Sende 🎬", "🎬", 0xFF3D405B),
            WheelOption("Kahve Ismarla ☕", "☕", 0xFFC85A3D),
            WheelOption("Gece Yürüyüşü 🌙", "🌙", 0xFF6C757D)
        )
    }

    val rotationAngle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Romantik Karar Çarkı 🎡",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )
                    Text(
                        text = "Bugün ne yapacağınıza şansınız karar versin!",
                        fontSize = 12.sp,
                        color = SlateNavy
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Wheel Canvas
                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(240.dp)
                                .rotate(rotationAngle.value)
                        ) {
                            val count = wheelOptions.size
                            val sweepAngle = 360f / count
                            val radius = size.minDimension / 2f
                            val center = Offset(radius, radius)

                            wheelOptions.forEachIndexed { index, option ->
                                val startAngle = index * sweepAngle
                                drawArc(
                                    color = Color(option.colorHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    size = Size(radius * 2, radius * 2)
                                )
                            }

                            // Outer stroke
                            drawCircle(
                                color = Color.White,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 6f)
                            )
                        }

                        // Center indicator pin
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WarmCreamSurface)
                                .border(3.dp, SoftCoralPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = SoftCoralPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Top Pointer Arrow
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(SoftCoralDark)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Spin Button
                    Button(
                        onClick = {
                            if (!isSpinning) {
                                isSpinning = true
                                selectedResult = null
                                scope.launch {
                                    val randomSpins = Random.nextInt(5, 10) * 360f
                                    val randomExtra = Random.nextFloat() * 360f
                                    val targetRotation = rotationAngle.value + randomSpins + randomExtra

                                    rotationAngle.animateTo(
                                        targetValue = targetRotation,
                                        animationSpec = tween(
                                            durationMillis = 3500,
                                            easing = FastOutSlowInEasing
                                        )
                                    )

                                    val finalAngle = (targetRotation % 360f + 360f) % 360f
                                    val sweep = 360f / wheelOptions.size
                                    val index = ((270f - finalAngle + 360f) % 360f / sweep).toInt() % wheelOptions.size
                                    selectedResult = wheelOptions[index].title
                                    isSpinning = false
                                }
                            }
                        },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoralPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("spin_wheel_btn")
                    ) {
                        Text(
                            text = if (isSpinning) "Çark Dönüyor... 💖" else "Çarkı Çevir! ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Result card
                    AnimatedVisibility(visible = selectedResult != null) {
                        Column(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftCoralContainer)
                                .border(1.dp, SoftCoralPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎉 Çarkın Kararı:", fontSize = 13.sp, color = SlateNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedResult ?: "",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftCoralDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. ORTAK YAPILACAKLAR (BUCKET LIST)
// ----------------------------------------------------
@Composable
fun BucketListSection(
    items: List<BucketItem>,
    onToggle: (String) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Tümü") }
    val categories = listOf("Tümü", "Gezilecek", "Romantik", "Aktivite", "Gurme")

    val filteredItems = remember(items, selectedCategory) {
        if (selectedCategory == "Tümü") items else items.filter { it.category == selectedCategory }
    }

    val completedCount = remember(items) { items.count { it.isCompleted } }
    val progress = if (items.isNotEmpty()) completedCount.toFloat() / items.size else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ortak Hedeflerimiz 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DeepCharcoal
                        )
                        Text(
                            text = "$completedCount / ${items.size} Tamamlandı",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftCoralPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = SoftCoralPrimary,
                        trackColor = SoftCoralContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Category Filter Row & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SoftCoralPrimary else WarmCreamContainer)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else DeepCharcoal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenAddDialog,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SoftCoralPrimary)
                        .testTag("add_bucket_item_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Yeni Hedef Ekle",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎯", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Henüz ortak hedef eklenmedi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Birlikte gerçekleştirmek istediğiniz hayalleri ekleyin ✨",
                            fontSize = 12.sp,
                            color = SlateNavy,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isCompleted) WarmCreamSurface.copy(alpha = 0.6f) else WarmCreamSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (item.isCompleted) SageGreen.copy(alpha = 0.4f) else BorderSoft,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggle(item.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (item.isCompleted) SageGreen else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (item.isCompleted) TextMuted else DeepCharcoal,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "🏷️ ${item.category}",
                                    fontSize = 11.sp,
                                    color = SlateNavy
                                )
                                if (item.isCompleted) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✨ Başarıldı",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SageGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// ----------------------------------------------------
// 3. KİLİTLİ SÜRPRİZ NOTLAR (SECRET NOTES)
// ----------------------------------------------------
@Composable
fun SecretNotesSection(
    notes: List<SecretLoveNote>,
    onOpenNote: (SecretLoveNote) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aşk Sandığımız 💌",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DeepCharcoal
                )
                Button(
                    onClick = onOpenAddDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftCoralPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Yeni Not Bırak", fontSize = 12.sp)
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💌", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aşk sandığı henüz boş",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "'Yeni Not Bırak' butonuna basarak sevgiline özel bir sürpriz mektup yaz 💖",
                            fontSize = 12.sp,
                            color = SlateNavy,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(notes, key = { it.id }) { note ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                        .clickable { onOpenNote(note) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (note.isUnlocked) SoftCoralContainer else WarmCreamContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = note.iconEmoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Koşul: ${note.unlockCondition}",
                                fontSize = 12.sp,
                                color = SlateNavy
                            )
                        }
                        Icon(
                            imageVector = if (note.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (note.isUnlocked) SoftCoralPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// ----------------------------------------------------
// 4. GÜNÜN AŞK SORUSU
// ----------------------------------------------------
@Composable
fun DailyQuestionsSection(
    questions: List<DailyCoupleQuestion>,
    onAnswerQuestion: (String, String) -> Unit
) {
    // If empty, offer an initial today's question
    val displayQuestions = remember(questions) {
        if (questions.isEmpty()) {
            listOf(
                DailyCoupleQuestion(
                    id = "daily_today",
                    question = "Birlikte geçirdiğimiz en komik veya en tatlı an neydi? ✨",
                    date = "Bugün"
                )
            )
        } else questions
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(displayQuestions, key = { it.id }) { q ->
            var answerInput by remember { mutableStateOf(q.myAnswer) }

            Card(
                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Günün Sorusu • ${q.date}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftCoralPrimary
                        )
                        Text(text = "💭", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = q.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // My Answer Input
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        placeholder = { Text("Senin cevabın...", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DeepCharcoal,
                            unfocusedTextColor = DeepCharcoal,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = SoftCoralPrimary,
                            focusedBorderColor = SoftCoralPrimary,
                            unfocusedBorderColor = BorderSoft,
                            focusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onAnswerQuestion(q.id, answerInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cevabı Kaydet", fontSize = 12.sp)
                        }
                    }

                    // Partner's Answer Card
                    if (q.partnerAnswer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftCoralContainer)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Sevgilinin Cevabı 💖:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCoralDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = q.partnerAnswer,
                                    fontSize = 13.sp,
                                    color = DeepCharcoal
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// ----------------------------------------------------
// DIALOGS
// ----------------------------------------------------
@Composable
fun AddBucketItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Aktivite") }
    val categories = listOf("Gezilecek", "Romantik", "Aktivite", "Gurme")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Birlikte Yapılacak Hedef 🎯", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hedef / Hayal") },
                    placeholder = { Text("Örn: Birlikte Paris'e gitmek") },
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kategori Seç:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SoftCoralPrimary else WarmCreamContainer)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else DeepCharcoal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title.trim(), selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
fun AddSecretNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Özlediğinde") }

    val conditions = listOf("Özlediğinde", "Moral Bozulduğunda", "Yıldönümümüzde", "Her Zaman")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Kilitli Sevgi Notu Bırak 💌", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Not Başlığı") },
                    placeholder = { Text("Örn: Seni çok özlediğim bir an...") },
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mektubun / Notun") },
                    placeholder = { Text("Sevgiline en içten hislerini yaz...") },
                    minLines = 3,
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Açılma Koşulu:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(conditions) { cond ->
                        val isSelected = condition == cond
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SoftCoralPrimary else WarmCreamContainer)
                                .clickable { condition = cond }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cond,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else DeepCharcoal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onAdd(title.trim(), content.trim(), condition) },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Kilitle & Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
fun ViewSecretNoteDialog(
    note: SecretLoveNote,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = note.iconEmoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = note.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (note.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftCoralContainer)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = note.content,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = DeepCharcoal
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SoftCoralPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bu not kilitli!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Açılma Şartı: ${note.unlockCondition}",
                            fontSize = 12.sp,
                            color = SlateNavy
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!note.isUnlocked) {
                Button(
                    onClick = onUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
                ) {
                    Text("Kilidi Aç 💖")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Kapat") }
            }
        },
        dismissButton = {
            if (!note.isUnlocked) {
                TextButton(onClick = onDismiss) { Text("Daha Sonra") }
            }
        }
    )
}
