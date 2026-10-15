package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aurora AI", appName)
  }

  @Test
  fun `verify RECORD_AUDIO permission is present in manifest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val permissions = packageInfo.requestedPermissions ?: emptyArray()
    org.junit.Assert.assertTrue(
      "RECORD_AUDIO permission must be declared in AndroidManifest.xml",
      permissions.contains(android.Manifest.permission.RECORD_AUDIO)
    )
  }

  @Test
  fun `verify ChatViewModel initializes and can send message`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.ChatViewModel(application)
    org.junit.Assert.assertNotNull(viewModel.currentSessionId.value)
    viewModel.sendMessage("Cześć Aurora")
    org.junit.Assert.assertNotNull(viewModel.messages.value)
  }
}
