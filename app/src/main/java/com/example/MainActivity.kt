package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BottomNavTab
import com.example.ui.components.CoupleBottomNavigationBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CoupleMapScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.PairedHomeScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.ProfileGalleryScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WarmCreamBackground
import com.example.ui.viewmodel.MainViewModel
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var themeMode by remember { mutableStateOf(getSharedPreferences("ikimiz_settings", MODE_PRIVATE).getString("theme_mode", "system") ?: "system") }
      MyApplicationTheme(
        darkTheme = when (themeMode) {
          "dark" -> true
          "light" -> false
          else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
      ) {
        MainApp(intent = intent, onThemeModeChanged = { themeMode = it })
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApp(
  intent: Intent? = null,
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = viewModel(),
  onThemeModeChanged: (String) -> Unit = {}
) {
  val context = LocalContext.current

  val currentUser by viewModel.currentUser.collectAsState()
  val partnerUser by viewModel.partnerUser.collectAsState()
  val isSignUpMode by viewModel.isSignUpMode.collectAsState()
  val isAuthLoading by viewModel.isAuthLoading.collectAsState()
  val authError by viewModel.authError.collectAsState()
  val pairingInput by viewModel.pairingCodeInput.collectAsState()
  val isPairingInProgress by viewModel.isPairingInProgress.collectAsState()
  val pairingError by viewModel.pairingError.collectAsState()
  val pairingSuccessMessage by viewModel.pairingSuccessMessage.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
  val showUnpairConfirmDialog by viewModel.showUnpairConfirmDialog.collectAsState()

  // 5 Tab Data
  val currentTab by viewModel.currentTab.collectAsState()
  val bucketList by viewModel.bucketList.collectAsState()
  val secretNotes by viewModel.secretNotes.collectAsState()
  val dailyQuestions by viewModel.dailyQuestions.collectAsState()
  val myStatus by viewModel.myStatus.collectAsState()
  val partnerStatus by viewModel.partnerStatus.collectAsState()
  val memoryPins by viewModel.memoryPins.collectAsState()
  val chatMessages by viewModel.chatMessages.collectAsState()
  val unreadMessageCount by viewModel.unreadMessageCount.collectAsState()
  val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
  val coupleMemories by viewModel.coupleMemories.collectAsState()
  val doubleTapEmoji by viewModel.doubleTapEmoji.collectAsState()

  // Request Notification permission for Android 13+ (POST_NOTIFICATIONS)
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ -> }

  LaunchedEffect(Unit) {
    // Create channel for Android 8+
    NotificationHelper.createNotificationChannel(context)

    // Request runtime permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (!NotificationHelper.areNotificationsEnabled(context)) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Check if opened from Chat Notification
  LaunchedEffect(intent) {
    val targetTab = intent?.getStringExtra("extra_tab")
    if (targetTab == "chat") {
      viewModel.selectTab(BottomNavTab.CHAT)
    }
  }

  Crossfade(
    targetState = currentUser,
    label = "main_screen_crossfade",
    modifier = modifier.fillMaxSize()
  ) { user ->
    when {
      user == null -> {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          AuthScreen(
            isSignUp = isSignUpMode,
            isLoading = isAuthLoading,
            errorMessage = authError,
            onSignUp = { email, pass, confirmPass, name, birthDate, avatarPreset, avatarBase64 ->
              viewModel.signUp(email, pass, confirmPass, name, birthDate, avatarPreset, avatarBase64)
            },
            onSignIn = { email, pass ->
              viewModel.signIn(email, pass)
            },
            onToggleMode = { isSignUp ->
              viewModel.setSignUpMode(isSignUp)
            },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
      !user.isPaired -> {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PairingScreen(
            user = user,
            pairingInput = pairingInput,
            isPairingInProgress = isPairingInProgress,
            pairingError = pairingError,
            pairingSuccessMessage = pairingSuccessMessage,
            onPairingInputChange = { viewModel.setPairingInput(it) },
            onPairClick = { viewModel.pairWithPartner() },
            onSignOutClick = { viewModel.signOut() },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
      else -> {
        val isImeVisible = WindowInsets.isImeVisible

        // 5-Tab Paired Couple Space
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = WarmCreamBackground,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          bottomBar = {
            if (!isImeVisible) {
              CoupleBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                unreadMessageCount = unreadMessageCount
              )
            }
          }
        ) { innerPadding ->
          Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) { tab ->
            when (tab) {
              BottomNavTab.GAMES -> {
                GamesScreen(
                  bucketList = bucketList,
                  secretNotes = secretNotes,
                  dailyQuestions = dailyQuestions,
                  onToggleBucketItem = { viewModel.toggleBucketItem(it) },
                  onAddBucketItem = { title, cat -> viewModel.addBucketItem(title, cat) },
                  onUnlockNote = { viewModel.unlockSecretNote(it) },
                  onAddSecretNote = { title, content, cond -> viewModel.addSecretNote(title, content, cond) },
                  onAnswerDailyQuestion = { qId, ans -> viewModel.answerDailyQuestion(qId, ans) }
                )
              }
              BottomNavTab.MAP -> {
                CoupleMapScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  myStatus = myStatus,
                  partnerStatus = partnerStatus,
                  memoryPins = memoryPins,
                  onUpdateMyStatus = { type, emoji, note -> viewModel.updateMyStatus(type, emoji, note) },
                  onAddMemoryPin = { title, loc, cat, date, note -> viewModel.addMemoryPin(title, loc, cat, date, note) }
                )
              }
              BottomNavTab.HOME -> {
                PairedHomeScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  bucketList = bucketList,
                  onOpenSettings = { viewModel.openSettings() },
                  onNavigateToTab = { viewModel.selectTab(it) }
                )
              }
              BottomNavTab.CHAT -> {
                ChatScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  messages = chatMessages,
                  doubleTapEmoji = doubleTapEmoji,
                  isPartnerTyping = isPartnerTyping,
                  onTypingChanged = { viewModel.setTyping(it) },
                  onMarkAsRead = { viewModel.markChatAsRead() },
                  onSetDoubleTapEmoji = { viewModel.setDoubleTapEmoji(it) },
                  onSendMessage = { text, imageUri, replyMsg -> viewModel.sendChatMessage(text, imageUri, replyMsg) },
                  onDeleteMessage = { id -> viewModel.deleteChatMessage(id) },
                  onEditMessage = { id, newText -> viewModel.editChatMessage(id, newText) },
                  onReactMessage = { id, emoji -> viewModel.reactToChatMessage(id, emoji) }
                )
              }
              BottomNavTab.PROFILE -> {
                val completedGoals = bucketList.count { it.isCompleted }
                ProfileGalleryScreen(
                  currentUser = user,
                  partnerUser = partnerUser,
                  memories = coupleMemories,
                  completedGoalsCount = completedGoals,
                  onOpenSettings = { viewModel.openSettings() },
                  onChangeProfilePhoto = { viewModel.updateProfilePhoto(it) },
                  onChangeAvatarPreset = { viewModel.updateProfilePreset(it) },
                  onAddMemory = { title, caption, loc, date, preset, base64, imageUri ->
                    viewModel.addCoupleMemory(title, caption, loc, date, preset, base64, imageUri)
                  },
                  onToggleLikeMemory = { viewModel.toggleLikeMemory(it) }
                )
              }
            }
          }
        }
      }
    }
  }

  // Settings Modal Dialog
  if (showSettingsDialog && currentUser != null) {
    SettingsDialog(
      currentUser = currentUser!!,
      doubleTapEmoji = doubleTapEmoji,
      onSetDoubleTapEmoji = { viewModel.setDoubleTapEmoji(it) },
      onChangeProfilePhoto = { viewModel.updateProfilePhoto(it) },
      onChangeAvatarPreset = { viewModel.updateProfilePreset(it) },
      onDismiss = { viewModel.closeSettings() },
      onOpenUnpairConfirm = { viewModel.openUnpairConfirm() },
      onSignOut = { viewModel.signOut() },
      onThemeModeChanged = onThemeModeChanged
    )
  }

  // Unpair Confirmation Dialog
  if (showUnpairConfirmDialog) {
    UnpairConfirmationDialog(
      isUnpairing = isPairingInProgress,
      onConfirm = { viewModel.unpair() },
      onDismiss = { viewModel.closeUnpairConfirm() }
    )
  }
}
