package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.SoftCoralPrimary

data class MemoryArtTheme(
    val id: String,
    val title: String,
    val emoji: String,
    val gradient: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val MEMORY_PRESETS = listOf(
    MemoryArtTheme(
        id = "romantic_sunset",
        title = "Gün Batımı",
        emoji = "🌅",
        gradient = listOf(Color(0xFFE07A5F), Color(0xFFF2CC8F), Color(0xFFFDECE8)),
        icon = Icons.Default.WbSunny
    ),
    MemoryArtTheme(
        id = "coffee_date",
        title = "Kahve Randevusu",
        emoji = "☕",
        gradient = listOf(Color(0xFF8D6E63), Color(0xFFD7CCC8), Color(0xFFF5EBE6)),
        icon = Icons.Default.Coffee
    ),
    MemoryArtTheme(
        id = "walk_park",
        title = "Doğa Yürüyüşü",
        emoji = "🍃",
        gradient = listOf(Color(0xFF81B29A), Color(0xFFC7E2D6), Color(0xFFF0F7F4)),
        icon = Icons.Default.Park
    ),
    MemoryArtTheme(
        id = "candle_dinner",
        title = "Mum Işığı",
        emoji = "🕯️",
        gradient = listOf(Color(0xFF9E2A2B), Color(0xFFE07A5F), Color(0xFFFDECE8)),
        icon = Icons.Default.Restaurant
    ),
    MemoryArtTheme(
        id = "stargazing",
        title = "Yıldızlar Altında",
        emoji = "✨",
        gradient = listOf(Color(0xFF2B2D42), Color(0xFF3D405B), Color(0xFF8D99AE)),
        icon = Icons.Default.Nightlight
    ),
    MemoryArtTheme(
        id = "beach_trip",
        title = "Sahil Gezisi",
        emoji = "🌊",
        gradient = listOf(Color(0xFF4EA8DE), Color(0xFF90E0EF), Color(0xFFCAF0F8)),
        icon = Icons.Default.Favorite
    )
)

@Composable
fun MemoryPresetImage(
    preset: String,
    base64: String? = null,
    imageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    if (!imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Anı Fotoğrafı",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val bitmap = remember(base64) {
        if (!base64.isNullOrEmpty()) {
            try {
                val clean = if (base64.startsWith("data:")) {
                    base64.substringAfter("base64,")
                } else base64
                val decoded = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Anı Fotoğrafı",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        val theme = MEMORY_PRESETS.firstOrNull { it.id == preset } ?: MEMORY_PRESETS.first()
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.linearGradient(theme.gradient)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = theme.emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = theme.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}
