@file:OptIn(ExperimentalMaterial3Api::class)

package com.louaysadok.redgif.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.louaysadok.redgif.data.local.Favorite
import com.louaysadok.redgif.data.local.FavoriteType
import com.louaysadok.redgif.data.local.FavoritesRepository
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.ui.common.CreatorAvatar
import com.louaysadok.redgif.ui.common.EnumDropdown
import com.louaysadok.redgif.ui.common.appContainer
import kotlinx.coroutines.launch

/**
 * Favorites with a Creators / Media dropdown.
 * Media favorites open in the viewer as a playlist. Creator favorites open that creator's feed.
 */
@Composable
fun FavoritesScreen(
    onMenu: () -> Unit,
    onOpenViewer: (List<RedMedia>, Int) -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val container = LocalContext.current.appContainer()
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(FavoriteType.MEDIA) }
    val favoritesFlow = remember(type) { container.favorites.observe(type) }
    val favorites by favoritesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            EnumDropdown(
                label = "Show",
                options = FavoriteType.entries,
                labelOf = { if (it == FavoriteType.MEDIA) "Media" else "Creators" },
                selected = type,
                onSelected = { type = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (type == FavoriteType.MEDIA) {
                            "No favorite media yet. Tap the bookmark on any card to save it."
                        } else {
                            "No favorite creators yet. Use the star at the top of a creator's feed."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (type == FavoriteType.MEDIA) {
                val media = favorites.mapNotNull { it.media }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(media, key = { _, m -> m.id }) { index, m ->
                        FavoriteMediaRow(
                            media = m,
                            onClick = { onOpenViewer(media, index) },
                            onRemove = {
                                scope.launch { container.favorites.remove(FavoritesRepository.mediaUid(m.id)) }
                            },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    items(favorites, key = { it.uid }) { fav: Favorite ->
                        CreatorRow(
                            name = fav.title,
                            onClick = { onOpenCreator(fav.title) },
                            onRemove = { scope.launch { container.favorites.remove(fav.uid) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteMediaRow(
    media: RedMedia,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = media.displayUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(96.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color.Black),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${media.creator}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (media.isVideo) "Video" else "Image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove favorite")
            }
        }
    }
}

@Composable
private fun CreatorRow(
    name: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreatorAvatar(name = name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "@$name",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove favorite")
            }
        }
    }
}
