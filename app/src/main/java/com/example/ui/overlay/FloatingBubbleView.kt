package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScrollSearchStateBus
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NotificationRed
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokRed

@Composable
fun FloatingBubbleView(
    username: String?,
    onExpand: () -> Unit,
    onDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val unreadCount by ScrollSearchStateBus.unreadNotificationCount.collectAsState()
    val isDark by ScrollSearchStateBus.isDarkTheme.collectAsState()

    Box(
        modifier = modifier
            .padding(6.dp)
            .testTag("floating_bubble_container")
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) DarkSurface else Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = if (isDark) TikTokCyan.copy(alpha = 0.5f) else Color(0xFFE4E4EB),
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag?.invoke(dragAmount.x, dragAmount.y)
                        }
                    )
                }
                .clickable {
                    ScrollSearchStateBus.resetUnreadCount()
                    onExpand()
                }
                .testTag("floating_bubble_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // TikTok Gradient Icon Orb (Red + Cyan)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(TikTokCyan, TikTokRed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Expandir TikTokLens",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (!username.isNullOrBlank()) "@$username" else "TikTokLens",
                    color = if (isDark) Color.White else Color(0xFF161823),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Red Notification Badge with number of search results / detections (e.g. "3")
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NotificationRed)
                    .border(1.5.dp, Color.White, CircleShape)
                    .testTag("notification_badge_bubble"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
