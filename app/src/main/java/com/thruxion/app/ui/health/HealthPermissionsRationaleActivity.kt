package com.thruxion.app.ui.health

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that displays the rationale for requesting health permissions.
 * Required by the Health Connect SDK.
 */
class HealthPermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // In a real app, you would show a UI explaining the benefits of Health Connect.
        // For now, we just finish it or show a simple message.
        finish()
    }
}
