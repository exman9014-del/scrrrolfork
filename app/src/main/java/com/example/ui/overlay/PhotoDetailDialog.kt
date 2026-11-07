package com.example.ui.overlay

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.ScrollSearchStateBus
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokRed
import com.example.util.ImageDownloader
import kotlinx.coroutines.launch

@Composable
fun PhotoDetailDialog(
    images: List<String>,
    fallbackImages: List<String> = emptyList(),
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val capturedBitmap by ScrollSearchStateBus.currentCapturedBitmap.collectAsStateWithLifecycle()
    val isSuperFullscreen by ScrollSearchStateBus.isSuperFullscreenEnabled.collectAsStateWithLifecycle()

    val safeImages = if (images.isNotEmpty()) images else listOf("")
    val safeIndex = initialIndex.coerceIn(0, (safeImages.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { safeImages.size }
    )

    // Current page zoom and pan transformation states
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // "Ver celular virado" - 90° rotation state to view landscape/horizontal photos completely!
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var preferOriginal by remember { mutableStateOf(false) }

    // User preference: retain zoom level when dragging to the next image
    var keepZoomOnSwipe by remember { mutableStateOf(true) }

    // Prevent duplicate swipe triggers during ongoing animated page transitions
    var isPagingTransitionActive by remember { mutableStateOf(false) }

    // When opened or safeIndex changes, ensure pager is scrolled to the clicked image index!
    LaunchedEffect(safeIndex, safeImages.size) {
        if (safeIndex in safeImages.indices && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }

    // When page changes, reset offset to center
    LaunchedEffect(pagerState.currentPage) {
        preferOriginal = false
        if (!keepZoomOnSwipe) {
            scale = 1f
        }
        offset = Offset.Zero
        isPagingTransitionActive = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isSuperFullscreen) 1.0f else 0.96f))
            .testTag("photo_detail_dialog")
    ) {
        // HorizontalPager allows swiping/dragging to previous and next images
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val imageUrl = safeImages.getOrNull(pageIndex).orEmpty()
            val isCurrentCaptured = (pageIndex == safeIndex && capturedBitmap != null && capturedBitmap!!.width > 20)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5.5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                val prospectiveX = offset.x + pan.x * scale
                                val prospectiveY = offset.y + pan.y * scale

                                // Edge dragging transition
                                if (prospectiveX < -maxOffsetX - 50f && pan.x < -2f) {
                                    if (pagerState.currentPage < safeImages.size - 1 && !isPagingTransitionActive) {
                                        isPagingTransitionActive = true
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                } else if (prospectiveX > maxOffsetX + 50f && pan.x > 2f) {
                                    if (pagerState.currentPage > 0 && !isPagingTransitionActive) {
                                        isPagingTransitionActive = true
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }

                                val newX = prospectiveX.coerceIn(-maxOffsetX, maxOffsetX)
                                val newY = prospectiveY.coerceIn(-maxOffsetY, maxOffsetY)
                                offset = Offset(newX, newY)
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(pageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Decode Base64 data URI if present
                val decodedBase64Bitmap = remember(imageUrl) {
                    try {
                        if (imageUrl.startsWith("data:image") && imageUrl.contains(",")) {
                            val base64Data = imageUrl.substringAfter(",")
                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }

                val previewUrl = fallbackImages.getOrNull(pageIndex)
                    ?.takeIf { it.isNotBlank() && it != imageUrl }

                val modelToUse: Any = when {
                    decodedBase64Bitmap != null -> decodedBase64Bitmap
                    !preferOriginal && previewUrl != null -> previewUrl
                    imageUrl.isNotBlank() -> imageUrl
                    isCurrentCaptured && capturedBitmap != null -> capturedBitmap!!
                    capturedBitmap != null -> capturedBitmap!!
                    else -> ""
                }

                val imageRequest = remember(modelToUse, context) {
                    if (modelToUse is String && modelToUse.startsWith("http")) {
                        val engineDomain = ScrollSearchStateBus.selectedSearchEngine.value.domain
                        ImageRequest.Builder(context)
                            .data(modelToUse)
                            .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                            .setHeader("Referer", "https://$engineDomain/")
                            .crossfade(true)
                            .build()
                    } else {
                        ImageRequest.Builder(context)
                            .data(modelToUse)
                            .crossfade(true)
                            .build()
                    }
                }
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Visualização da imagem",
                    contentScale = if (rotationAngle % 180f != 0f) ContentScale.Fit else ContentScale.Fit,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TikTokRed, modifier = Modifier.size(36.dp))
                        }
                    },
                    error = {
                        if (capturedBitmap != null && capturedBitmap!!.width > 20) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto capturada",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (previewUrl != null && preferOriginal) {
                            SubcomposeAsyncImage(
                                model = previewUrl,
                                contentDescription = "Prévia da imagem",
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = TikTokRed, modifier = Modifier.size(36.dp))
                                    }
                                },
                                error = {
                                    Text(
                                        text = "A fonte não permitiu carregar a imagem nem a prévia.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(32.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (previewUrl != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "A prévia não carregou",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tente abrir a imagem original da fonte.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { preferOriginal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed)
                                ) {
                                    Text("Tentar original", color = Color.White)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Não foi possível carregar a imagem original",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "A fonte pode bloquear o acesso direto.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            rotationZ = rotationAngle
                        }
                )
            }
        }

        // Left Navigation Arrow (Previous Photo)
        if (pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("prev_photo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Foto anterior",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right Navigation Arrow (Next Photo)
        if (pagerState.currentPage < safeImages.size - 1) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("next_photo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Próxima foto",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                    .testTag("close_photo_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar visualização",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${pagerState.currentPage + 1} de ${safeImages.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (rotationAngle != 0f) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TikTokCyan.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "Virado ${rotationAngle.toInt()}°",
                                color = TikTokCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (scale > 1.05f) {
                        "Zoom: ${(scale * 100).toInt()}% • Arraste p/ o lado no zoom"
                    } else {
                        "Toque no ícone para girar fotos horizontais"
                    },
                    color = if (scale > 1.05f) TikTokCyan else Color.LightGray,
                    fontSize = 10.sp
                )
                if (!fallbackImages.getOrNull(pagerState.currentPage).isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TikTokCyan.copy(alpha = 0.2f),
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { preferOriginal = !preferOriginal }
                    ) {
                        Text(
                            text = if (preferOriginal) "Mostrar prévia" else "Tentar imagem original",
                            color = TikTokCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // "Ver Celular Virado" / Rotate 90° Button (as requested: "icone p ver celular virado isso pq algumas fotos visualiza inteira virado")
            IconButton(
                onClick = {
                    rotationAngle = (rotationAngle + 90f) % 360f
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (rotationAngle != 0f) TikTokCyan.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (rotationAngle != 0f) TikTokCyan else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .testTag("rotate_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "Ver celular virado (Girar foto 90°)",
                    tint = if (rotationAngle != 0f) TikTokCyan else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download Current Photo Button (High Quality)
            val currentPhotoUrl = safeImages.getOrNull(pagerState.currentPage).orEmpty()
            Button(
                onClick = {
                    if (pagerState.currentPage == safeIndex && capturedBitmap != null) {
                        ImageDownloader.saveBitmapToGallery(context, capturedBitmap!!, "TikTokLens_Photo")
                    } else if (currentPhotoUrl.startsWith("data:image")) {
                        ImageDownloader.downloadBase64(context, currentPhotoUrl, "TikTokLens_Photo")
                    } else if (currentPhotoUrl.isNotBlank()) {
                        ImageDownloader.downloadPhoto(context, currentPhotoUrl, "https://www.google.com/", "TikTokLens_Photo")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                shape = CircleShape,
                modifier = Modifier.testTag("download_photo_fullscreen_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Baixar imagem",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Baixar imagem", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Zoom Controls Bar (Zoom In, Zoom Out, Reset, Manter Zoom, Super Fullscreen indicator)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom out
                IconButton(
                    onClick = {
                        scale = (scale - 0.5f).coerceAtLeast(1f)
                        if (scale <= 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    },
                    modifier = Modifier.size(34.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Diminuir zoom",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom percentage indicator
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )

                // Zoom in
                IconButton(
                    onClick = {
                        scale = (scale + 0.5f).coerceAtMost(5.5f)
                    },
                    modifier = Modifier.size(34.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Aumentar zoom",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // "Ver Celular Virado" Quick Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (rotationAngle != 0f) TikTokCyan.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (rotationAngle != 0f) TikTokCyan else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            rotationAngle = (rotationAngle + 90f) % 360f
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("rotate_quick_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Virar",
                            tint = if (rotationAngle != 0f) TikTokCyan else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (rotationAngle == 0f) "Virar 90°" else "${rotationAngle.toInt()}°",
                            color = if (rotationAngle != 0f) TikTokCyan else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // "Super Tela Cheia" Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSuperFullscreen) TikTokRed.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSuperFullscreen) TikTokRed else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            ScrollSearchStateBus.toggleSuperFullscreen()
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("super_fullscreen_toggle_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Super Tela Cheia",
                            tint = if (isSuperFullscreen) TikTokRed else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isSuperFullscreen) "Super Cheia: ON" else "Super Cheia: OFF",
                            color = if (isSuperFullscreen) TikTokRed else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // "Sair do Zoom" / Reset Zoom Button
                AnimatedVisibility(
                    visible = scale > 1.05f || rotationAngle != 0f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotationAngle = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reset_zoom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Resetar",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resetar",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
