package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(Executors.newSingleThreadExecutor())
                .build()
            WorkManager.initialize(context, config)
        } catch (_: IllegalStateException) {
            // Already initialized
        }
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TodoRelatos", appName)
    }

    @Test
    fun `work manager is initialized in context`() {
        val wm = WorkManager.getInstance(context)
        org.junit.Assert.assertNotNull(wm)
    }
}
