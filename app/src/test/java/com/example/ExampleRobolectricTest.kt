package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("İkimiz", appName)
  }

  @Test
  fun `pairing code generator produces valid 6 character code`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = AuthRepository(context)
    val code = repo.generatePairingCode()
    assertEquals(6, code.length)
    assertTrue(code.all { it.isLetterOrDigit() && (it.isDigit() || it.isUpperCase()) })
  }
}

