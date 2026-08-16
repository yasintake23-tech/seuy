package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileDialog
import com.example.ui.screens.PairedHomeScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.UnpairConfirmationDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainApp(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun MainApp(
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = viewModel()
) {
  val context = LocalContext.current
  val currentUser by viewModel.currentUser.collectAsState()
  val partnerUser by viewModel.partnerUser.collectAsState()
  val isSignUpMode by viewModel.isSignUpMode.collectAsState()
  val isAuthLoading by viewModel.isAuthLoading.collectAsState()
  val authError by viewModel.authError.collectAsState()
  val profileCompletionData by viewModel.profileCompletionData.collectAsState()
  val pairingInput by viewModel.pairingCodeInput.collectAsState()
  val isPairingInProgress by viewModel.isPairingInProgress.collectAsState()
  val pairingError by viewModel.pairingError.collectAsState()
  val pairingSuccessMessage by viewModel.pairingSuccessMessage.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
  val showUnpairConfirmDialog by viewModel.showUnpairConfirmDialog.collectAsState()

  Crossfade(
    targetState = currentUser,
    label = "main_screen_crossfade",
    modifier = modifier.fillMaxSize()
  ) { user ->
    when {
      user == null -> {
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
          onGoogleSignIn = {
            viewModel.signInWithGoogle(context)
          },
          onToggleMode = { isSignUp ->
            viewModel.setSignUpMode(isSignUp)
          }
        )
      }
      !user.isPaired -> {
        PairingScreen(
          user = user,
          pairingInput = pairingInput,
          isPairingInProgress = isPairingInProgress,
          pairingError = pairingError,
          pairingSuccessMessage = pairingSuccessMessage,
          onPairingInputChange = { viewModel.setPairingInput(it) },
          onPairClick = { viewModel.pairWithPartner() },
          onSignOutClick = { viewModel.signOut() }
        )
      }
      else -> {
        PairedHomeScreen(
          currentUser = user,
          partnerUser = partnerUser,
          onOpenSettings = { viewModel.openSettings() }
        )
      }
    }
  }

  // Google Sign-In Profile Completion Modal
  profileCompletionData?.let { completionData ->
    CompleteProfileDialog(
      completionData = completionData,
      isLoading = isAuthLoading,
      errorMessage = authError,
      onSaveProfile = { name, birthDate, avatarPreset, avatarBase64 ->
        viewModel.completeGoogleProfile(name, birthDate, avatarPreset, avatarBase64)
      },
      onCancel = {
        viewModel.dismissProfileCompletion()
      }
    )
  }

  // Settings Modal Dialog
  if (showSettingsDialog && currentUser != null) {
    SettingsDialog(
      currentUser = currentUser!!,
      onDismiss = { viewModel.closeSettings() },
      onOpenUnpairConfirm = { viewModel.openUnpairConfirm() },
      onSignOut = { viewModel.signOut() }
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
