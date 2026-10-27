package com.oneorigin.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.oneorigin.studio.core.AppViewModel
import com.oneorigin.studio.notify.AppPresence
import com.oneorigin.studio.notify.Notifier
import com.oneorigin.studio.ui.theme.Origin StudioTheme

/**
 * The whole app in an Android bubble, floating over whatever the user is doing.
 *
 * It is the same UI as the main window and shares the running sessions with it (agents and
 * shells live in the process, not in a window), so the bubble opens on the exact chat or shell
 * the user left and they can still move around the app from there.
 */
class BubbleActivity : Origin StudioActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Notifier.placeFrom(intent)?.let(AppPresence::request)
        setContent {
            Origin StudioTheme {
                Origin StudioRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Notifier.placeFrom(intent)?.let(AppPresence::request)
    }
}
