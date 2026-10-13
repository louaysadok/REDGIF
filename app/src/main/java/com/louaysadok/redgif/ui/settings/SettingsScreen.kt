@file:OptIn(ExperimentalMaterial3Api::class)

package com.louaysadok.redgif.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louaysadok.redgif.BuildConfig
import com.louaysadok.redgif.data.media.MediaCache
import com.louaysadok.redgif.data.settings.AppSettings
import com.louaysadok.redgif.data.settings.AutoplayMode
import com.louaysadok.redgif.data.settings.ThemeMode
import com.louaysadok.redgif.data.settings.VideoQuality
import com.louaysadok.redgif.ui.common.EnumDropdown
import com.louaysadok.redgif.ui.common.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onMenu: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer()
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s: AppSettings = settings ?: AppSettings()

    fun change(block: (AppSettings) -> AppSettings) {
        scope.launch { container.settings.update(block) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { inner ->
        LazyColumn(modifier = Modifier.padding(inner)) {
            item { SectionTitle("Appearance") }
            item {
                EnumDropdown(
                    label = "Theme",
                    options = ThemeMode.entries,
                    labelOf = { it.label },
                    selected = s.theme,
                    onSelected = { value -> change { it.copy(theme = value) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item { SectionTitle("Playback") }
            item {
                EnumDropdown(
                    label = "Autoplay previews",
                    options = AutoplayMode.entries,
                    labelOf = { it.label },
                    selected = s.autoplay,
                    onSelected = { value -> change { it.copy(autoplay = value) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                EnumDropdown(
                    label = "Video quality",
                    options = VideoQuality.entries,
                    labelOf = { it.label },
                    selected = s.quality,
                    onSelected = { value -> change { it.copy(quality = value) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Mute by default") },
                    supportingContent = { Text("Previews and the viewer start with sound off") },
                    trailingContent = {
                        Switch(
                            checked = s.muted,
                            onCheckedChange = { checked -> change { it.copy(muted = checked) } },
                        )
                    },
                )
            }

            item { SectionTitle("Storage") }
            item {
                ListItem(
                    headlineContent = { Text("Clear media cache") },
                    supportingContent = { Text("Removes cached videos and thumbnails") },
                    modifier = Modifier.clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) { MediaCache.clear(context) }
                            container.imageLoader.memoryCache?.clear()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            item { SectionTitle("About") }
            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                )
            }
            item { HorizontalDivider() }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp),
    )
}
