package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AuthDiagnosticState
import com.example.model.AuthStageLog
import com.example.model.StepStatus
import com.example.ui.components.AvatarSelector
import com.example.ui.components.RomanticDatePickerField
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.WineTertiary

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.44f

        // Draw Google G colorful representation
        drawCircle(
            color = Color(0xFF4285F4),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.6f,
            center = Offset(cx, cy)
        )
        // Red slice
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 200f,
            sweepAngle = 100f,
            useCenter = true,
            size = size
        )
        // Yellow slice
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 80f,
            useCenter = true,
            size = size
        )
        // Green slice
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 30f,
            sweepAngle = 90f,
            useCenter = true,
            size = size
        )
        // Blue slice & center bar
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -30f,
            sweepAngle = 60f,
            useCenter = true,
            size = size
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.55f,
            center = Offset(cx, cy)
        )
        // Crossbar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.2f)
        )
    }
}

@Composable
fun AuthDiagnosticCard(
    state: AuthDiagnosticState,
    modifier: Modifier = Modifier
) {
    if (state.stages.isEmpty() && !state.isRunning) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.92f)
            .padding(bottom = 14.dp)
            .border(
                1.5.dp,
                when (state.overallSuccess) {
                    true -> Color(0xFF4CAF50)
                    false -> Color(0xFFE53935)
                    null -> Color(0xFFFFB74D)
                },
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CANLI TEST TANILAMA (DEBUG)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                val badgeText = when {
                    state.isRunning -> "TEST EDİLİYOR..."
                    state.overallSuccess == true -> "BAŞARILI ✓"
                    state.overallSuccess == false -> "HATA ✕"
                    else -> "HAZIR"
                }
                val badgeColor = when {
                    state.isRunning -> Color(0xFFFFA726)
                    state.overallSuccess == true -> Color(0xFF66BB6A)
                    state.overallSuccess == false -> Color(0xFFEF5350)
                    else -> Color(0xFFBDBDBD)
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (state.operation.isNotBlank()) {
                Text(
                    text = "İşlem: ${state.operation}",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color(0xFF37474F),
                thickness = 1.dp
            )

            // Stages list (1 to 6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.stages.sortedBy { it.stageNumber }.forEach { stage ->
                    val statusIcon = when (stage.status) {
                        StepStatus.SUCCESS -> Icons.Default.CheckCircle
                        StepStatus.FAIL -> Icons.Default.Cancel
                        StepStatus.RUNNING -> Icons.Default.Refresh
                        StepStatus.IDLE -> Icons.Default.Info
                    }
                    val statusColor = when (stage.status) {
                        StepStatus.SUCCESS -> Color(0xFF81C784)
                        StepStatus.FAIL -> Color(0xFFE57373)
                        StepStatus.RUNNING -> Color(0xFFFFD54F)
                        StepStatus.IDLE -> Color(0xFF90A4AE)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF26293A), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (stage.stageTitle.startsWith("STAGE")) stage.stageTitle else "STAGE ${stage.stageNumber}: ${stage.stageTitle}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (stage.info.isNotBlank()) {
                            Text(
                                text = stage.info,
                                color = if (stage.status == StepStatus.FAIL) Color(0xFFFF8A80) else Color(0xFFE0E0E0),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }

                        // Detailed Exception breakdown if present
                        if (stage.exceptionClass != null || stage.errorCode != null || stage.exceptionMessage != null) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 22.dp, top = 6.dp)
                                    .fillMaxWidth()
                                    .background(Color(0xFF381E24), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                if (!stage.exceptionClass.isNullOrBlank()) {
                                    Text(
                                        text = "EXCEPTION: ${stage.exceptionClass}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!stage.errorCode.isNullOrBlank()) {
                                    Text(
                                        text = "CODE: ${stage.errorCode}",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                if (!stage.exceptionMessage.isNullOrBlank()) {
                                    Text(
                                        text = "MESSAGE: ${stage.exceptionMessage}",
                                        color = Color(0xFFFFEBEE),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    isSignUp: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    diagnosticState: AuthDiagnosticState = AuthDiagnosticState(),
    onSignUp: (email: String, pass: String, confirmPass: String, name: String, birthDate: String, avatarPreset: String, avatarBase64: String?) -> Unit,
    onSignIn: (email: String, pass: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var avatarPreset by remember { mutableStateOf("heart_rose") }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .imePadding()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.romantic_hero_banner),
                contentDescription = "Romantik İkimiz",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Romantic gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "İkimiz",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WineTertiary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Sadece İkimizin Özel Dünyası",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Auth Tabs
        TabRow(
            selectedTabIndex = if (isSignUp) 0 else 1,
            containerColor = Color.Transparent,
            contentColor = RosePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (isSignUp) 0 else 1]),
                    color = RosePrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = isSignUp,
                onClick = { onToggleMode(true) },
                text = {
                    Text(
                        text = "Hesap Oluştur",
                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_create_account")
            )
            Tab(
                selected = !isSignUp,
                onClick = { onToggleMode(false) },
                text = {
                    Text(
                        text = "Giriş Yap",
                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_login")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Diagnostic Debug Information Box
        AuthDiagnosticCard(state = diagnosticState)

        // Error message banner
        AnimatedVisibility(
            visible = !errorMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Hata",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Form Container Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Google Sign In Button
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFAFAFA),
                        contentColor = Color(0xFF333333)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_button")
                ) {
                    GoogleLogoIcon(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSignUp) "Google ile Kayıt Ol" else "Google ile devam et",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3C4043)
                    )
                }

                // Divider with "veya"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderLight,
                        thickness = 1.dp
                    )
                    Text(
                        text = "veya e-posta ile",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderLight,
                        thickness = 1.dp
                    )
                }

                if (isSignUp) {
                    // Profile Avatar Selector
                    AvatarSelector(
                        selectedPreset = avatarPreset,
                        selectedBase64 = avatarBase64,
                        onPresetSelected = { avatarPreset = it },
                        onPhotoSelected = { avatarBase64 = it }
                    )

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("İsminiz") },
                        placeholder = { Text("Örn: Aslı, Kerem...") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = RosePrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RosePrimary,
                            unfocusedBorderColor = BorderLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_full_name")
                    )

                    // Birth Date Field
                    RomanticDatePickerField(
                        birthDate = birthDate,
                        onDateSelected = { birthDate = it }
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta Adresi") },
                    placeholder = { Text("ornek@gmail.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = RosePrimary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RosePrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Şifre (en az 6 karakter)") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RosePrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster",
                                tint = RosePrimary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            focusManager.clearFocus()
                            if (!isSignUp) onSignIn(email, password)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RosePrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                if (isSignUp) {
                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Şifre Tekrar") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = RosePrimary)
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSignUp(email, password, confirmPassword, name, birthDate, avatarPreset, avatarBase64)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RosePrimary,
                            unfocusedBorderColor = BorderLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_password")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isSignUp) {
                            onSignUp(email, password, confirmPassword, name, birthDate, avatarPreset, avatarBase64)
                        } else {
                            onSignIn(email, password)
                        }
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
                        .testTag("auth_submit_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUp) "Hesabımı Oluştur" else "E-posta ile Giriş Yap",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch Mode Text Button
        TextButton(
            onClick = { onToggleMode(!isSignUp) },
            modifier = Modifier.testTag("toggle_auth_mode_btn")
        ) {
            Text(
                text = if (isSignUp) "Zaten hesabın var mı? Giriş Yap" else "Hesabın yok mu? Kendine Hesap Oluştur",
                color = RoseDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
