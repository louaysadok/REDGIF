package com.louaysadok.redgif.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.ui.common.CreatorAvatar
import com.louaysadok.redgif.ui.common.formatCount
import com.louaysadok.redgif.ui.common.relativeTime
import com.louaysadok.redgif.ui.player.PlayerLayer

/**
 * One feed card in the Infinity style: creator header, media block with type badge,
 * and a footer with save, share, likes and views.
 */
@Composable
fun MediaCard(
    media: RedMedia,
    activePlayer: ExoPlayer?,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onCreatorClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShare: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreatorAvatar(name = media.creator, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCreatorClick),
            ) {
                Text(
                    text = "@${media.creator}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val time = relativeTime(media.createdAt)
                if (time.isNotEmpty()) {
                    Text(time, style = MaterialTheme.typography.bodySmall, color = secondary)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(media.aspectRatio)
                .background(Color.Black)
                .clickable(onClick = onOpen),
        ) {
            if (activePlayer != null && media.isVideo) {
                PlayerLayer(
                    player = activePlayer,
                    poster = media.posterUrl ?: media.thumbUrl,
                    mediaKey = media.id,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = media.displayUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            MediaTypeBadge(
                isVideo = media.isVideo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )

            if (media.isVideo && activePlayer == null) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(8.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else secondary,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = secondary)
            }
            Spacer(Modifier.weight(1f))
            StatItem(Icons.Filled.ThumbUp, formatCount(media.likes), secondary)
            Spacer(Modifier.width(12.dp))
            StatItem(Icons.Filled.Visibility, formatCount(media.views), secondary)
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun MediaTypeBadge(isVideo: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1565C0),
    ) {
        Text(
            text = if (isVideo) "VIDEO" else "IMAGE",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}
