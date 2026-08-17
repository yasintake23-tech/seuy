package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CoupleMemory
import com.example.model.RelationshipMilestone
import com.example.model.UserProfile
import com.example.ui.components.AvatarImage
import com.example.ui.components.MEMORY_PRESETS
import com.example.ui.components.MemoryPresetImage
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ProfileGalleryScreen(
    currentUser: UserProfile,
    partnerUser: UserProfile?,
    memories: List<CoupleMemory>,
    completedGoalsCount: Int,
    onOpenSettings: () -> Unit,
    onAddMemory: (String, String, String, String, String, String?, Uri?) -> Unit,
    onToggleLikeMemory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Grid, 1: Timeline
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var viewingMemory by remember { mutableStateOf<CoupleMemory?>(null) }

    val partnerName = partnerUser?.displayName ?: currentUser.partnerName ?: "Sevgilin"

    // Calculate days together
    val daysTogether = remember(currentUser.pairedAt) {
        val pairedTime = currentUser.pairedAt ?: System.currentTimeMillis()
        val diff = System.currentTimeMillis() - pairedTime
        (TimeUnit.MILLISECONDS.toDays(diff) + 1).coerceAtLeast(1)
    }

    val pairedDateFormatted = remember(currentUser.pairedAt) {
        val pairedTime = currentUser.pairedAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
        try {
            sdf.format(Date(pairedTime))
        } catch (e: Exception) {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(pairedTime))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Top Bar: Title & Settings Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentUser.displayName} & $partnerName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepCharcoal
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = DeepCharcoal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profile Header Card (Instagram / Couple Showcase Style)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connected Dual Avatars
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarImage(
                                    preset = currentUser.avatarPreset,
                                    base64 = currentUser.avatarBase64,
                                    size = 54.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AvatarImage(
                                    preset = partnerUser?.avatarPreset ?: "flower_pink",
                                    base64 = partnerUser?.avatarBase64,
                                    size = 54.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(SoftCoralPrimary)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stats counters
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$daysTogether",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = DeepCharcoal
                                )
                                Text(text = "Gün", fontSize = 11.sp, color = SlateNavy)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${memories.size}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = DeepCharcoal
                                )
                                Text(text = "Anı", fontSize = 11.sp, color = SlateNavy)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$completedGoalsCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = DeepCharcoal
                                )
                                Text(text = "Hedef", fontSize = 11.sp, color = SlateNavy)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bio / Romantic Quote
                    Text(
                        text = "✨ \"Birlikte yazılan en güzel aşk hikayesi...\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepCharcoal
                    )
                    Text(
                        text = "İki kalp, tek dünya 💖 | Sonsuza kadar el ele.",
                        fontSize = 11.sp,
                        color = SlateNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Row: Grid vs Timeline
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SoftCoralPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Anı Galerisi (${memories.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = SoftCoralPrimary,
                    unselectedContentColor = SlateNavy
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Zaman Tüneli", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = SoftCoralPrimary,
                    unselectedContentColor = SlateNavy
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Contents
            if (selectedTab == 0) {
                if (memories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(SoftCoralContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📸", fontSize = 30.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Henüz kaydedilmiş anı yok",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepCharcoal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sağ alttaki '+' butonuna basarak ilk ortak fotoğrafınızı veya romantik anınızı galeriye ekleyin ✨",
                                fontSize = 12.sp,
                                color = SlateNavy,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(memories, key = { it.id }) { memory ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderSoft, RoundedCornerShape(12.dp))
                                    .clickable { viewingMemory = memory }
                            ) {
                                MemoryPresetImage(
                                    preset = memory.imagePreset,
                                    base64 = memory.imageBase64,
                                    imageUrl = memory.imageUrl
                                )

                                // Heart badge if liked
                                if (memory.isLikedByMe) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = SoftCoralPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(3) }) {
                            Spacer(modifier = Modifier.height(70.dp))
                        }
                    }
                }
            } else {
                // Real Timeline List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Milestone 1: Official Pairing Event
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, SoftCoralPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(SoftCoralContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "💖", fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "İkimiz Çift Alanı Başlangıcı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DeepCharcoal
                                    )
                                    Text(
                                        text = "$pairedDateFormatted • Birlikte $daysTogether. Günümüz",
                                        fontSize = 11.sp,
                                        color = SoftCoralPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentUser.displayName} ve $partnerName kalplerini birleştirdi.",
                                        fontSize = 12.sp,
                                        color = SlateNavy
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Memories Chronological Stream
                    if (memories.isNotEmpty()) {
                        items(memories.sortedByDescending { it.createdAt }, key = { it.id }) { memory ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = WarmCreamSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                                    .clickable { viewingMemory = memory }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, BorderSoft, RoundedCornerShape(10.dp))
                                    ) {
                                        MemoryPresetImage(
                                            preset = memory.imagePreset,
                                            base64 = memory.imageBase64,
                                            imageUrl = memory.imageUrl
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memory.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DeepCharcoal
                                        )
                                        Text(
                                            text = "${memory.date} • 📍 ${memory.location}",
                                            fontSize = 11.sp,
                                            color = SoftCoralPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (memory.caption.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = memory.caption,
                                                fontSize = 12.sp,
                                                color = SlateNavy,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    if (memory.isLikedByMe) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = SoftCoralPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = WarmCreamContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "📸", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Zaman Tünelinizi Anılarla Doldurun",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DeepCharcoal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sağ alttaki '+' butonundan fotoğraf ve anı ekledikçe burada tarih sırasıyla sergilenir ✨",
                                        fontSize = 12.sp,
                                        color = SlateNavy,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }

        // Floating Action Button to Add New Photo Memory
        FloatingActionButton(
            onClick = { showAddMemoryDialog = true },
            containerColor = SoftCoralPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp, end = 20.dp)
                .testTag("add_new_memory_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Yeni Anı Ekle")
        }

        // Add Memory Dialog
        if (showAddMemoryDialog) {
            AddMemoryDialog(
                onDismiss = { showAddMemoryDialog = false },
                onAdd = { title, caption, location, date, preset, base64, imageUri ->
                    onAddMemory(title, caption, location, date, preset, base64, imageUri)
                    showAddMemoryDialog = false
                }
            )
        }

        // Memory Detail Modal
        viewingMemory?.let { memory ->
            MemoryDetailDialog(
                memory = memory,
                onDismiss = { viewingMemory = null },
                onToggleLike = {
                    onToggleLikeMemory(memory.id)
                    viewingMemory = memory.copy(
                        isLikedByMe = !memory.isLikedByMe,
                        likesCount = if (memory.isLikedByMe) memory.likesCount - 1 else memory.likesCount + 1
                    )
                }
            )
        }
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String?, Uri?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("Bugün") }
    var selectedPreset by remember { mutableStateOf("romantic_sunset") }
    var selectedBase64 by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(originalBitmap, 500, 500, true)
                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    selectedBase64 = encoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Anı Fotoğrafı Ekle 📸", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Preview or Gallery Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, BorderSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        MemoryPresetImage(
                            preset = selectedPreset,
                            base64 = selectedBase64
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { photoLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = SoftCoralPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedUri != null) "Galeriden Farklı Fotoğraf Seç" else "Galeriden Fotoğraf Yükle",
                            fontSize = 12.sp,
                            color = SoftCoralDark
                        )
                    }
                }

                item {
                    Text("Veya Romantik Sanat Teması Seç:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateNavy)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MEMORY_PRESETS) { theme ->
                            val isSelected = selectedUri == null && selectedPreset == theme.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SoftCoralPrimary else WarmCreamContainer)
                                    .clickable {
                                        selectedUri = null
                                        selectedBase64 = null
                                        selectedPreset = theme.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${theme.emoji} ${theme.title}",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else DeepCharcoal
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Anı Başlığı") },
                        placeholder = { Text("Örn: Sahilde Gün Batımı") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DeepCharcoal,
                            unfocusedTextColor = DeepCharcoal,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = SoftCoralPrimary,
                            focusedBorderColor = SoftCoralPrimary,
                            unfocusedBorderColor = BorderSoft,
                            focusedLabelColor = SoftCoralPrimary,
                            unfocusedLabelColor = SlateNavy,
                            focusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Romantik Notunuz") },
                        placeholder = { Text("Bu anıyı özel kılan neydi?") },
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
                            unfocusedLabelColor = SlateNavy,
                            focusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Lokasyon") },
                            placeholder = { Text("Örn: İzmir") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DeepCharcoal,
                                unfocusedTextColor = DeepCharcoal,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = SoftCoralPrimary,
                                focusedBorderColor = SoftCoralPrimary,
                                unfocusedBorderColor = BorderSoft,
                                focusedLabelColor = SoftCoralPrimary,
                                unfocusedLabelColor = SlateNavy,
                                focusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f),
                                unfocusedPlaceholderColor = SlateNavy.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title.trim(),
                            caption.trim(),
                            location.trim(),
                            date.trim(),
                            selectedPreset,
                            selectedBase64,
                            selectedUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Anılara Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
fun MemoryDetailDialog(
    memory: CoupleMemory,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                ) {
                    MemoryPresetImage(
                        preset = memory.imagePreset,
                        base64 = memory.imageBase64,
                        imageUrl = memory.imageUrl
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = memory.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = DeepCharcoal
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (memory.location.isNotEmpty()) {
                                Text(text = "📍 ${memory.location} • ", fontSize = 11.sp, color = SlateNavy)
                            }
                            Text(text = "📅 ${memory.date}", fontSize = 11.sp, color = SlateNavy)
                        }
                    }

                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (memory.isLikedByMe) SoftCoralContainer else WarmCreamContainer)
                    ) {
                        Icon(
                            imageVector = if (memory.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (memory.isLikedByMe) SoftCoralPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (memory.caption.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmCreamContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = memory.caption,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = DeepCharcoal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralPrimary)
            ) {
                Text("Kapat")
            }
        }
    )
}
