package com.example.ui.overlay.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.model.SearchEngine
import com.example.model.SearchType
import com.example.ui.overlay.TagsRow
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.util.ImageDownloader

/**
 * Minimalist floating pill with version switch, search engine switch, tags toggle, refresh, minimize, and close.
 * Allows quick switching between V and F versions and managing engines without polluting the clean photo view.
 */
@Composable
fun PureFloatingControlsPill(
    currentVersion: OverlayUiVersion,
    currentEngine: SearchEngine,
    username: String?,
    tags: List<String>,
    selectedTag: String,
    isSafeSearchDisabled: Boolean = true,
    onTagSelected: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onVersionSelected: (OverlayUiVersion) -> Unit,
    onEngineSelected: (SearchEngine) -> Unit,
    onToggleSafeSearch: () -> Unit = {},
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onMoveDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEngineMenu by remember { mutableStateOf(false) }
    var showSearchTypeMenu by remember { mutableStateOf(false) }
    var showTagsRow by remember { mutableStateOf(false) }

    val isDark by ScrollSearchStateBus.isDarkTheme.collectAsState()
    val searchType by ScrollSearchStateBus.selectedSearchType.collectAsState()
    val isSuperFullscreen by ScrollSearchStateBus.isSuperFullscreenEnabled.collectAsState()

    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color.Black.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag move handle indicator
                if (onMoveDrag != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onMoveDrag.invoke(dragAmount.x, dragAmount.y)
                                    }
                                )
                            }
                            .testTag("f_drag_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.OpenWith,
                            contentDescription = "Mover pela tela",
                            tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // Version pill
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMenu = true }
                            .padding(horizontal = 5.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentVersion.code,
                            color = com.example.ui.theme.TikTokRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (!username.isNullOrBlank()) "@$username" else "",
                            color = if (isDark) Color.White else com.example.ui.theme.TikTokDarkText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        OverlayUiVersion.entries.forEach { version ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = version.code,
                                            color = if (version.isCleanPhotoOnly) com.example.ui.theme.TikTokRed else com.example.ui.theme.TikTokCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = version.title,
                                            color = if (version == currentVersion) Color.White else Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onVersionSelected(version)
                                }
                            )
                        }
                    }
                }

                // Search Type (Imagens / Vídeos / Sites / Notícias)
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSearchTypeMenu = true }
                            .padding(horizontal = 5.dp, vertical = 4.dp)
                            .testTag("f_search_type_button")
                    ) {
                        Text(
                            text = searchType.displayName,
                            color = com.example.ui.theme.TikTokCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = showSearchTypeMenu,
                        onDismissRequest = { showSearchTypeMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        SearchType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.displayName,
                                        color = if (type == searchType) com.example.ui.theme.TikTokRed else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (type == searchType) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    showSearchTypeMenu = false
                                    ScrollSearchStateBus.setSearchType(type)
                                }
                            )
                        }
                    }
                }

                // SafeSearch toggle button right in the floating pill ("Sem Censura")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSafeSearchDisabled) Color(0xFF1B5E20).copy(alpha = 0.8f)
                            else Color.White.copy(alpha = 0.15f)
                        )
                        .clickable { onToggleSafeSearch() }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                        .testTag("f_safesearch_toggle_button")
                ) {
                    Text(
                        text = if (isSafeSearchDisabled) "Safe: OFF" else "Safe: ON",
                        color = if (isSafeSearchDisabled) Color(0xFF00E676) else Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Plan Indicator Badge (Premium vs Basic)
                val isPremiumActive by ScrollSearchStateBus.isPremiumActive.collectAsState()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isPremiumActive) Color(0x33FFD700)
                            else Color.White.copy(alpha = 0.15f)
                        )
                        .border(
                            width = if (isPremiumActive) 1.dp else 0.dp,
                            color = if (isPremiumActive) Color(0xFFFFD700) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { ScrollSearchStateBus.togglePlan() }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                        .testTag("f_plan_badge")
                ) {
                    Text(
                        text = if (isPremiumActive) "👑 PREM" else "BÁSICO",
                        color = if (isPremiumActive) Color(0xFFFFD700) else Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // HD Filter Toggle
                val isHighQualityFilter by ScrollSearchStateBus.isHighQualityFilterEnabled.collectAsState()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isHighQualityFilter) com.example.ui.theme.TikTokCyan.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.15f)
                        )
                        .clickable { ScrollSearchStateBus.toggleHighQualityFilter() }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                        .testTag("f_hd_filter_toggle_button")
                ) {
                    Text(
                        text = if (isHighQualityFilter) "HD: ON" else "HD: OFF",
                        color = if (isHighQualityFilter) com.example.ui.theme.TikTokCyan else Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Super Fullscreen Toggle
                IconButton(
                    onClick = { ScrollSearchStateBus.toggleSuperFullscreen() },
                    modifier = Modifier.size(24.dp).testTag("f_super_fullscreen_btn")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Fullscreen,
                        contentDescription = "Super Tela Cheia",
                        tint = if (isSuperFullscreen) com.example.ui.theme.TikTokRed else Color.LightGray,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Theme Toggle
                IconButton(
                    onClick = { ScrollSearchStateBus.toggleTheme() },
                    modifier = Modifier.size(24.dp).testTag("f_theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDark) androidx.compose.material.icons.Icons.Default.LightMode else androidx.compose.material.icons.Icons.Default.DarkMode,
                        contentDescription = "Alternar Tema",
                        tint = if (isDark) Color(0xFFFFD600) else Color(0xFF161823),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Tags toggle button
                IconButton(
                    onClick = { showTagsRow = !showTagsRow },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = "Tags",
                        tint = if (showTagsRow) com.example.ui.theme.TikTokRed else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // History button in pure floating pill
                IconButton(
                    onClick = { ScrollSearchStateBus.setHistoryDialogOpen(true) },
                    modifier = Modifier.size(24.dp).testTag("f_history_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.History,
                        contentDescription = "Histórico de Pesquisa",
                        tint = com.example.ui.theme.TikTokRed,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minimizar",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Animated tags row for all F versions when toggled
        AnimatedVisibility(visible = showTagsRow) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.90f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                TagsRow(
                    tags = tags,
                    selectedTag = selectedTag,
                    onTagSelected = onTagSelected,
                    onAddCustomTag = onAddCustomTag
                )
            }
        }
    }
}

/**
 * F1: Baseado no V1 (Barra inferior), mas só fotos grandes na horizontal com botão de download.
 */
@Composable
fun F1PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(26.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .width(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f1_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Download button on photo
                        IconButton(
                            onClick = {
                                ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("download_btn_f1_${photo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * F2: Baseado no V2 (Sidebar Lateral), fotos verticais puras com botão de download.
 */
@Composable
fun F2PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(26.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f2_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Download button on photo
                        IconButton(
                            onClick = {
                                ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("download_btn_f2_${photo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * F5: Baseado no V5 (Bottom Sheet), fotos grandes com deslizamento lateral e download.
 */
@Composable
fun F5PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f5_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Download button on photo
                        IconButton(
                            onClick = {
                                ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("download_btn_f5_${photo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * F6: Baseado no V6 (Fotos Vertical), grade vertical de fotos com download.
 */
@Composable
fun F6PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f6_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Download button on photo
                        IconButton(
                            onClick = {
                                ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("download_btn_f6_${photo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * F7: Baseado no V7 (Swipe Horizontal), tela cheia de fotos com contador e download.
 */
@Composable
fun F7PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(photos) { index, photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f7_photo_card_$index")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Discreto contador no canto superior esquerdo
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopStart)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${index + 1}/${photos.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Download button on photo
                        IconButton(
                            onClick = {
                                ImageDownloader.downloadPhoto(context, photo.url, photo.title)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("download_btn_f7_${photo.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
