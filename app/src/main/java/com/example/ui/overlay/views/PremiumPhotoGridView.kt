package com.example.ui.overlay.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ScrollSearchStateBus
import com.example.model.PhotoItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokRed
import com.example.util.ImageDownloader

private val GoldPrimary = Color(0xFFFFD700)
private val GoldAccent = Color(0xFFFFA000)

@Composable
fun PremiumPhotoGridView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query = ScrollSearchStateBus.getEffectiveSearchQuery()
    val isDark by ScrollSearchStateBus.isDarkTheme.collectAsState()
    val searchApiUrl = ScrollSearchStateBus.getEffectiveSearchApiUrl()
    val imageQuality by ScrollSearchStateBus.imageQuality.collectAsState()
    val hasMoreResults by ScrollSearchStateBus.nextPageStartIndex.collectAsState()
    val lastCustomSearchError by ScrollSearchStateBus.lastCustomSearchError.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkSurface else Color(0xFF101018))
    ) {
        // Premium Indicator Header Bar
        Surface(
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            color = Color(0x33FFD700),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Premium Mode",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "👑 MODO PREMIUM • GOOGLE IMAGES",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = if (photos.isNotEmpty()) {
                                "${photos.size} resultados da API"
                            } else "Busca via SerpApi",
                            color = Color(0xFFFFECB3),
                            fontSize = 9.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar busca",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (lastCustomSearchError != null) {
            Surface(
                color = Color(0x33FF9800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastCustomSearchError!!,
                        color = Color(0xFFFFE0B2),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isLoading && photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Consultando Google Images via SerpApi...",
                        color = GoldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prioridade de tamanho ${imageQuality.title.lowercase()} para \"$query\"",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        } else if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhuma foto retornada no momento",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (searchApiUrl.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure a URL HTTPS do backend Vercel nas configurações Premium.",
                            color = Color(0xFFFFCC80),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Tentar Novamente",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("premium_photos_grid")
            ) {
                items(photos) { photo ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("premium_photo_item_${photo.id}")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = photo.previewUrl ?: photo.url,
                                contentDescription = photo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Search source badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPrimary),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Google",
                                        color = GoldPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Top-Right Actions (Download & Fullscreen)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val allUrls = photos.map { it.url }
                                        val index = photos.indexOf(photo)
                                        ScrollSearchStateBus.openFullscreenPhoto(
                                            photo.url,
                                            allUrls,
                                            index,
                                            photos.map { it.previewUrl.orEmpty() }
                                        )
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.70f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Tela Cheia",
                                        tint = TikTokCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.70f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Baixar",
                                        tint = TikTokRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Bottom title overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column {
                                    Text(
                                        text = photo.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = photo.sourceDomain,
                                        color = GoldPrimary,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                if (hasMoreResults != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(
                            onClick = onLoadMore,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isLoading) "Buscando..." else "Carregar mais resultados",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
