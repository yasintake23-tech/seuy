package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GoogleSignInOutcome
import com.example.ui.components.AvatarSelector
import com.example.ui.components.RomanticDatePickerField
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.WineTertiary

@Composable
fun CompleteProfileDialog(
    completionData: GoogleSignInOutcome.NeedsProfileCompletion,
    isLoading: Boolean,
    errorMessage: String?,
    onSaveProfile: (name: String, birthDate: String, avatarPreset: String, avatarBase64: String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(completionData.displayName) }
    var birthDate by remember { mutableStateOf("") }
    var avatarPreset by remember { mutableStateOf("heart_rose") }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profilini Tamamla",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WineTertiary
                    )
                }

                Text(
                    text = "Google hesabın başarıyla bağlandı. Sevgilinle eşleşmek için lütfen profilini tamamla.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                )

                val displayError = errorMessage ?: localError
                if (!displayError.isNullOrEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = displayError,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Avatar Selector
                AvatarSelector(
                    selectedPreset = avatarPreset,
                    selectedBase64 = avatarBase64,
                    onPresetSelected = { avatarPreset = it },
                    onPhotoSelected = { avatarBase64 = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsminiz") },
                    placeholder = { Text("Örn: Aslı, Kerem...") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = RosePrimary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RosePrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("complete_profile_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Birth Date input
                RomanticDatePickerField(
                    birthDate = birthDate,
                    onDateSelected = { birthDate = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            localError = "Lütfen isminizi girin."
                            return@Button
                        }
                        if (birthDate.isBlank()) {
                            localError = "Lütfen doğum tarihinizi seçin."
                            return@Button
                        }
                        localError = null
                        onSaveProfile(name.trim(), birthDate.trim(), avatarPreset, avatarBase64)
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RosePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("complete_profile_save_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kaydet ve Devam Et",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onCancel,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Vazgeç",
                        color = RoseDark,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
