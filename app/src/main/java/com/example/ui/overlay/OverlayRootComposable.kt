package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.overlay.FastWebViewManager
import com.example.ui.overlay.views.PremiumPhotoGridView
import com.example.ui.overlay.views.PureFloatingControlsPill
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ScrollSearchTheme

@Composable
fun OverlayRootComposable(
    fastWebViewManager: FastWebViewManager,
    onCloseOverlay: () -> Unit,
    onBubbleDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onMenuDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val username by ScrollSearchStateBus.currentUsername.collectAsState()
    val activeVersion by ScrollSearchStateBus.activeVersion.collectAsState()
    val currentEngine by ScrollSearchStateBus.selectedSearchEngine.collectAsState()
    val isSafeSearchDisabled by ScrollSearchStateBus.isSafeSearchDisabled.collectAsState()
    val isBubbleCollapsed by ScrollSearchStateBus.isBubbleCollapsed.collectAsState()
    val tags by ScrollSearchStateBus.tags.collectAsState()
    val selectedTag by ScrollSearchStateBus.selectedTag.collectAsState()
    val searchBarText by ScrollSearchStateBus.searchBarText.collectAsState()
    val heightRatio by ScrollSearchStateBus.heightRatio.collectAsState()
    val isPhotoDetailOpen by ScrollSearchStateBus.isPhotoDetailOpen.collectAsState()
    val fullscreenImages by ScrollSearchStateBus.fullscreenImages.collectAsState()
    val fullscreenFallbackImages by ScrollSearchStateBus.fullscreenFallbackImages.collectAsState()
    val fullscreenInitialIndex by ScrollSearchStateBus.fullscreenInitialIndex.collectAsState()

    val isPremiumActive by ScrollSearchStateBus.isPremiumActive.collectAsState()
    val photos by ScrollSearchStateBus.photos.collectAsState()
    val isLoadingPhotos by ScrollSearchStateBus.isLoadingPhotos.collectAsState()

    val capturedBitmap by ScrollSearchStateBus.currentCapturedBitmap.collectAsState()
    val isHistoryDialogOpen by ScrollSearchStateBus.isHistoryDialogOpen.collectAsState()

    // Synchronize pure mode CSS when switching between V and F versions
    LaunchedEffect(activeVersion) {
        fastWebViewManager.setPureMode(activeVersion.isCleanPhotoOnly)
    }

    ScrollSearchTheme {
        if (isHistoryDialogOpen) {
            SearchHistoryDialog(
                onDismiss = { ScrollSearchStateBus.setHistoryDialogOpen(false) },
                onSelectSearch = { selectedQuery ->
                    ScrollSearchStateBus.setSearchBarText(selectedQuery)
                }
            )
        }

        if (isBubbleCollapsed) {
            // Minimized floating pill docked to edge
            FloatingBubbleView(
                username = username,
                onExpand = {
                    ScrollSearchStateBus.setBubbleCollapsed(false)
                },
                onDrag = onBubbleDrag,
                modifier = modifier
            )
        } else if (isPhotoDetailOpen && (fullscreenImages.isNotEmpty() || capturedBitmap != null)) {
            PhotoDetailDialog(
                images = fullscreenImages,
                fallbackImages = fullscreenFallbackImages,
                initialIndex = fullscreenInitialIndex,
                onDismiss = {
                    ScrollSearchStateBus.closeFullscreenPhoto()
                }
            )
        } else {
            if (activeVersion.isCleanPhotoOnly) {
                // ========================================================
                // F VERSIONS (F1, F2, F5, F6, F7): PURE PHOTOS WEBVIEW
                // Displays Google Images with headers, search bars, chips,
                // and footers hidden via injected CSS, leaving ONLY the
                // clean grid of photos visible directly on screen.
                // ========================================================
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .clip(
                            if (activeVersion == OverlayUiVersion.F2) {
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            } else {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            }
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top drag handle for resizable panels (F5, F6, F7)
                        if (activeVersion == OverlayUiVersion.F5 ||
                            activeVersion == OverlayUiVersion.F6 ||
                            activeVersion == OverlayUiVersion.F7
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(Color.Black.copy(alpha = 0.70f))
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaRatio = -dragAmount / 1200f
                                            ScrollSearchStateBus.adjustHeightRatio(deltaRatio)
                                        }
                                    }
                                    .testTag("top_border_resize_handle_f"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }

                        if (isPremiumActive) {
                            PremiumPhotoGridView(
                                photos = photos,
                                isLoading = isLoadingPhotos,
                                onPhotoClick = { photo ->
                                    val allUrls = photos.map { it.url }
                                    val index = photos.indexOf(photo)
                                    ScrollSearchStateBus.openFullscreenPhoto(
                                        photo.url,
                                        allUrls,
                                        index,
                                        photos.map { it.previewUrl.orEmpty() }
                                    )
                                },
                                onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                                onLoadMore = { ScrollSearchStateBus.loadMoreCustomSearchPhotos() },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Google Images WebView in F mode (Clean pure photos)
                            AndroidWebViewContainer(
                                fastWebViewManager = fastWebViewManager,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Floating minimalist control pill (version selector, tags, close, minimize)
                    PureFloatingControlsPill(
                        currentVersion = activeVersion,
                        currentEngine = currentEngine,
                        username = username,
                        tags = tags,
                        selectedTag = selectedTag,
                        isSafeSearchDisabled = isSafeSearchDisabled,
                        onTagSelected = { ScrollSearchStateBus.selectTag(it) },
                        onAddCustomTag = { ScrollSearchStateBus.addCustomTag(it) },
                        onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                        onEngineSelected = { ScrollSearchStateBus.setSearchEngine(it) },
                        onToggleSafeSearch = { ScrollSearchStateBus.toggleSafeSearch() },
                        onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                        onClose = onCloseOverlay,
                        onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                        onMoveDrag = onMenuDrag,
                        modifier = Modifier
                            .align(if (activeVersion == OverlayUiVersion.F2) Alignment.TopStart else Alignment.TopEnd)
                            .padding(
                                top = if (activeVersion == OverlayUiVersion.F5 ||
                                    activeVersion == OverlayUiVersion.F6 ||
                                    activeVersion == OverlayUiVersion.F7
                                ) 22.dp else 4.dp,
                                start = if (activeVersion == OverlayUiVersion.F2) 4.dp else 0.dp,
                                end = if (activeVersion == OverlayUiVersion.F2) 0.dp else 4.dp
                            )
                    )
                }
            } else {
                val isDark = ScrollSearchStateBus.isDarkTheme.collectAsState().value
                // ========================================================
                // V VERSIONS (V1, V2, V5, V6, V7): FULL CONTROLS + WEBVIEW
                // Displays OverlayHeader, TagsRow, SearchBarComponent, and
                // the full Google Images WebView in all V versions.
                // ========================================================
                Surface(
                    shape = if (activeVersion == OverlayUiVersion.V2) {
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    } else {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    },
                    color = if (isDark) DarkSurface else Color.White,
                    shadowElevation = 16.dp,
                    modifier = modifier
                        .fillMaxSize()
                        .clip(
                            if (activeVersion == OverlayUiVersion.V2) {
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            } else {
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            }
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OverlayHeader(
                            username = username,
                            currentVersion = activeVersion,
                            currentEngine = currentEngine,
                            isSafeSearchDisabled = isSafeSearchDisabled,
                            onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                            onEngineSelected = { ScrollSearchStateBus.setSearchEngine(it) },
                            onToggleSafeSearch = { ScrollSearchStateBus.toggleSafeSearch() },
                            onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                            onClose = onCloseOverlay,
                            onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                            onMoveDrag = onMenuDrag
                        )

                        val isHeaderMenuCollapsed by ScrollSearchStateBus.isHeaderMenuCollapsed.collectAsState()
                        if (!isHeaderMenuCollapsed) {
                            TagsRow(
                                tags = tags,
                                selectedTag = selectedTag,
                                onTagSelected = { ScrollSearchStateBus.selectTag(it) },
                                onAddCustomTag = { ScrollSearchStateBus.addCustomTag(it) }
                            )

                            SearchBarComponent(
                                query = searchBarText,
                                onQueryChange = { ScrollSearchStateBus.setSearchBarText(it) },
                                placeholder = "Pesquisar imagens com @${username ?: "criador"}...",
                                onSearchSubmitted = { ScrollSearchStateBus.setSearchBarText(it) },
                                onFocusChanged = { ScrollSearchStateBus.setSearchFocused(it) }
                            )
                        }

                        if (isPremiumActive) {
                            PremiumPhotoGridView(
                                photos = photos,
                                isLoading = isLoadingPhotos,
                                onPhotoClick = { photo ->
                                    val allUrls = photos.map { it.url }
                                    val index = photos.indexOf(photo)
                                    ScrollSearchStateBus.openFullscreenPhoto(
                                        photo.url,
                                        allUrls,
                                        index,
                                        photos.map { it.previewUrl.orEmpty() }
                                    )
                                },
                                onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                                onLoadMore = { ScrollSearchStateBus.loadMoreCustomSearchPhotos() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            // Google Images WebView in all V versions!
                            AndroidWebViewContainer(
                                fastWebViewManager = fastWebViewManager,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
