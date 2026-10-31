package com.zmusic.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmusic.app.data.settings.AppSettings
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onScanMusic: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val allowLongTracks by appSettings.allowLongTracks.collectAsState()
    var rememberPosition by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Section 1: Library
        item {
            SettingsCategoryHeader(title = "LIBRARY")
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Scan Music",
                subtitle = "Scan storage to discover local music files",
                onClick = onScanMusic
            )
            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Music Folders",
                subtitle = "Default storage / Music directory",
                onClick = {}
            )
        }

        // Section 2: Playback
        item {
            SettingsCategoryHeader(title = "PLAYBACK & DURATION FILTER")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timelapse,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Allow Long Mixes & Podcasts (> 5 min)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (allowLongTracks) "Allowed: Long DJ mixes and podcasts included" else "Filtered: Max 5 minutes (clean single songs only)",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = allowLongTracks,
                    onCheckedChange = { appSettings.setAllowLongTracks(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Remember playback position", color = TextPrimary, fontSize = 16.sp)
                    Text(text = "Resume songs where you left off", color = TextSecondary, fontSize = 13.sp)
                }
                Switch(
                    checked = rememberPosition,
                    onCheckedChange = { rememberPosition = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                )
            }
        }

        // Section 3: Share App
        item {
            SettingsCategoryHeader(title = "SHARE WITH FRIENDS")
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Share Z-Music App (APK)",
                subtitle = "Send APK file via WhatsApp, Bluetooth, Nearby Share",
                onClick = { shareAppApk(context) }
            )
        }

        // Section 3: Appearance
        item {
            SettingsCategoryHeader(title = "APPEARANCE")
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Dark Mode (Default Premium Dark)",
                onClick = {}
            )
        }

        // Section 4: Privacy & Data
        item {
            SettingsCategoryHeader(title = "PRIVACY & STORAGE")
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Listening History",
                subtitle = "Permanently remove local history records",
                onClick = onClearHistory
            )
            SettingsItem(
                icon = Icons.Default.Shield,
                title = "Privacy Statement",
                subtitle = "No account required • 100% offline local playback",
                onClick = {}
            )
        }

        // Section 5: About
        item {
            SettingsCategoryHeader(title = "ABOUT")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Z-Music", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                        Text(text = "Version 1.0.0 (Release APK Build)", color = TextSecondary, fontSize = 13.sp)
                        Text(text = "Built with Kotlin & Jetpack Compose", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryPurple,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

private fun shareAppApk(context: android.content.Context) {
    try {
        val appInfo = context.applicationInfo
        val sourceApk = java.io.File(appInfo.sourceDir)
        if (!sourceApk.exists()) {
            android.widget.Toast.makeText(context, "APK file not found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val shareDir = java.io.File(context.cacheDir, "shared_apk").apply { mkdirs() }
        val shareFile = java.io.File(shareDir, "Z-Music.apk")
        sourceApk.copyTo(shareFile, overwrite = true)

        val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            shareFile
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Z-Music App APK")
            putExtra(android.content.Intent.EXTRA_TEXT, "Here is the Z-Music app APK! Enjoy clean online music streaming & synced lyrics.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Share Z-Music APK via")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("SettingsScreen", "Failed to share APK", e)
        android.widget.Toast.makeText(context, "Error sharing APK: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
