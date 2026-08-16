package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RoseLight
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoft
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class AvatarPresetItem(
    val id: String,
    val name: String,
    val emoji: String,
    val bgColor1: Color,
    val bgColor2: Color
)

val AVATAR_PRESETS = listOf(
    AvatarPresetItem("heart_rose", "Gül Kalp", "💖", Color(0xFFFFB3C1), Color(0xFFE5587A)),
    AvatarPresetItem("bear_cute", "Tatlı Ayıcık", "🧸", Color(0xFFFFDFBA), Color(0xFFFFB347)),
    AvatarPresetItem("flower_pink", "Bahar Çiçeği", "🌸", Color(0xFFFFC6FF), Color(0xFFFF85A1)),
    AvatarPresetItem("cat_white", "Şirin Kedi", "🐱", Color(0xFFE0C3FC), Color(0xFF8EC5FC)),
    AvatarPresetItem("crown_royal", "Prens / Prenses", "👑", Color(0xFFFDE68A), Color(0xFFF59E0B)),
    AvatarPresetItem("couple_stars", "Aşk Yıldızı", "✨", Color(0xFFFBCFE8), Color(0xFFEC4899)),
    AvatarPresetItem("bunny_sweet", "Tavşancık", "🐰", Color(0xFFFFD6E0), Color(0xFFFF9EAA)),
    AvatarPresetItem("panda_love", "Panda", "🐼", Color(0xFFD8B4E2), Color(0xFFAE7AC5))
)

@Composable
fun AvatarImage(
    preset: String,
    base64: String?,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64) {
        if (!base64.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Profil Fotoğrafı",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, RosePrimary, CircleShape)
        )
    } else {
        val selectedPreset = AVATAR_PRESETS.firstOrNull { it.id == preset } ?: AVATAR_PRESETS.first()
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(selectedPreset.bgColor1, selectedPreset.bgColor2)
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val emojiSize = (size.value * 0.48f).sp
            Text(
                text = selectedPreset.emoji,
                fontSize = emojiSize
            )
        }
    }
}

@Composable
fun AvatarSelector(
    selectedPreset: String,
    selectedBase64: String?,
    onPresetSelected: (String) -> Unit,
    onPhotoSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Resize to a clean 300x300 for optimal fast storage
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    onPhotoSelected(encoded)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = RoseSoft),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarImage(
                    preset = selectedPreset,
                    base64 = selectedBase64,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Profil Görseli",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedBase64 != null) "Özel fotoğraf seçildi" else "Tatlı bir avatar veya fotoğraf seç",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets row
            Text(
                text = "Romantik Avatarlar:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RoseDark
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AVATAR_PRESETS) { item ->
                    val isSelected = selectedBase64 == null && selectedPreset == item.id
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(item.bgColor1, item.bgColor2)))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) RosePrimary else Color.White,
                                shape = CircleShape
                            )
                            .clickable {
                                onPhotoSelected(null)
                                onPresetSelected(item.id)
                            }
                            .testTag("avatar_preset_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.emoji, fontSize = 24.sp)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(RosePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seçildi",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gallery photo picker button
            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_gallery_photo_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = RosePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedBase64 != null) "Galeriden Farklı Fotoğraf Seç" else "Galeriden Fotoğraf Yükle",
                    color = RoseDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
