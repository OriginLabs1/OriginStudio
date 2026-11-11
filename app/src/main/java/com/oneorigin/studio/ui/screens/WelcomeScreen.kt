package com.oneorigin.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oneorigin.studio.BuildConfig
import com.oneorigin.studio.R
import com.oneorigin.studio.ui.components.BrandMark
import com.oneorigin.studio.ui.components.LeadingTile
import com.oneorigin.studio.ui.theme.OriginStudioIcons
import com.oneorigin.studio.ui.theme.Palette

/**
 * The first thing a new install sees, once the Linux environment is ready.
 *
 * It says three things and then gets out of the way: the workspace is local, every agent
 * lands in one fleet, and the same workspace travels with the companion app. There is no
 * account to create and no tour to dismiss later; one button starts the work.
 */
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(size = 58.dp)

            Spacer(Modifier.height(26.dp))

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Palette.Text,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
            )

            Spacer(Modifier.height(30.dp))

            Point(
                icon = OriginStudioIcons.Folder,
                title = stringResource(R.string.welcome_point_local_title),
                body = stringResource(R.string.welcome_point_local_body),
            )
            Spacer(Modifier.height(18.dp))
            Point(
                icon = OriginStudioIcons.Fleet,
                title = stringResource(R.string.welcome_point_agents_title),
                body = stringResource(R.string.welcome_point_agents_body),
            )
            Spacer(Modifier.height(18.dp))
            Point(
                icon = OriginStudioIcons.Globe,
                title = stringResource(R.string.welcome_point_universal_title),
                body = stringResource(R.string.welcome_point_universal_body),
            )

            Spacer(Modifier.height(34.dp))

            StartButton(onStart)

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.welcome_restore),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextFaint,
            )

            Spacer(Modifier.height(26.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME} · ${stringResource(R.string.app_name)}",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.TextFaint,
            )
        }
    }
}

@Composable
private fun Point(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        LeadingTile(tint = Palette.Accent, icon = icon)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Text,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.welcome_start),
            style = MaterialTheme.typography.titleMedium,
            color = Palette.OnAccent,
        )
    }
}
