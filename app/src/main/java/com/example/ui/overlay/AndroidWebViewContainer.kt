package com.example.ui.overlay

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.overlay.FastWebViewManager
import com.example.ui.theme.IgPink
import com.example.ui.theme.TikTokRed

@Composable
fun AndroidWebViewContainer(
    fastWebViewManager: FastWebViewManager,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }

    DisposableEffect(fastWebViewManager) {
        fastWebViewManager.onPageStartedListener = {
            isLoading = true
        }
        fastWebViewManager.onPageFinishedListener = {
            isLoading = false
        }
        onDispose {
            fastWebViewManager.onPageStartedListener = null
            fastWebViewManager.onPageFinishedListener = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("webview_container_root")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("android_webview_view"),
            factory = { context ->
                val webView = fastWebViewManager.getOrCreateWebView(context)

                val parent = webView.parent as? ViewGroup
                parent?.removeView(webView)

                val container = FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                container
            },
            update = { _ ->
                // WebView is managed by FastWebViewManager singleton
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = IgPink,
                trackColor = Color.Transparent
            )
        }

        // Turbo Fast-Scroll Floating Controls (Rolagem Rápida e Fluida)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Scroll to top
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { fastWebViewManager.scrollToTopFast() }
                        .testTag("fast_scroll_top_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Rolar ao topo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Fast scroll down (Turbo jump)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TikTokRed.copy(alpha = 0.85f))
                        .clickable { fastWebViewManager.scrollDownFast(amountDp = 500) }
                        .testTag("fast_scroll_down_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Rolagem rápida próxima tela",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
