package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.ui.overlay.OverlayRootComposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var composeView: ComposeView? = null
    private var viewTreeOwner: ServiceViewTreeOwner? = null
    private var fastWebViewManager: FastWebViewManager? = null
    private var isAttached = false

    private val displayMetrics get() = context.resources.displayMetrics
    private val screenWidth get() = displayMetrics.widthPixels
    private val screenHeight get() = displayMetrics.heightPixels
    private val density get() = displayMetrics.density

    // Overlay WindowManager layout params
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        gravity = Gravity.BOTTOM or Gravity.START
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = (screenHeight * 0.48f).toInt()
        x = 0
        y = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (isAttached) return

        try {
            fastWebViewManager = FastWebViewManager(context)
            viewTreeOwner = ServiceViewTreeOwner()

            recalculateLayoutParams()

            composeView = ComposeView(context).apply {
                viewTreeOwner?.attachToView(this)
                setContent {
                    OverlayRootComposable(
                        fastWebViewManager = fastWebViewManager!!,
                        onCloseOverlay = {
                            ScrollSearchStateBus.setOverlayActive(false)
                            hideOverlay()
                        },
                        onBubbleDrag = { dx, dy ->
                            onBubbleDragged(dx, dy)
                        },
                        onMenuDrag = { dx, dy ->
                            onMenuDragged(dx, dy)
                        }
                    )
                }
            }

            windowManager.addView(composeView, layoutParams)
            isAttached = true
            Log.d(TAG, "Floating overlay attached with full WebView support across all versions")

            observeStateBus()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying floating overlay: ${e.message}")
        }
    }

    private fun observeStateBus() {
        // Toggle input focus and IME soft keyboard
        ScrollSearchStateBus.setOnRequestFocusToggle { focused ->
            updateFocusableMode(focused)
        }

        // Query change trigger
        ScrollSearchStateBus.setOnQueryChangedListener { query ->
            onSearchQueryTriggered(query)
        }

        val initialQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
        onSearchQueryTriggered(initialQuery)

        // Observe layout, version, position, and super fullscreen changes
        scope.launch {
            combine(
                ScrollSearchStateBus.isBubbleCollapsed,
                ScrollSearchStateBus.activeVersion,
                ScrollSearchStateBus.heightRatio,
                ScrollSearchStateBus.isPhotoDetailOpen,
                ScrollSearchStateBus.isV1WebViewExpanded
            ) { collapsed, version, ratio, isPhotoDetail, isV1Expanded ->
                recalculateLayoutParams(collapsed, version, ratio, isPhotoDetail, isV1Expanded)
            }.collectLatest {
                kotlinx.coroutines.delay(16L)
                applyLayoutParams()
            }
        }

        scope.launch {
            combine(
                ScrollSearchStateBus.overlayOffsetY,
                ScrollSearchStateBus.overlayOffsetX,
                ScrollSearchStateBus.isSuperFullscreenEnabled,
                ScrollSearchStateBus.isModalFullscreen
            ) { _, _, _, _ ->
                recalculateLayoutParams()
            }.collectLatest {
                kotlinx.coroutines.delay(16L)
                applyLayoutParams()
            }
        }

        // Observe searchType changes (Imagens, Vídeos, Sites, Notícias)
        scope.launch {
            ScrollSearchStateBus.selectedSearchType.drop(1).collectLatest { searchType ->
                val query = ScrollSearchStateBus.getEffectiveSearchQuery()
                val version = ScrollSearchStateBus.activeVersion.value
                val quality = ScrollSearchStateBus.imageQuality.value
                fastWebViewManager?.loadSearchQuery(
                    query = query,
                    isPure = version.isCleanPhotoOnly,
                    qualityParam = quality.tbsParam,
                    searchType = searchType
                )
            }
        }

        // Observe activeVersion changes to switch pure mode CSS and update quality
        scope.launch {
            ScrollSearchStateBus.activeVersion.drop(1).collectLatest { version ->
                fastWebViewManager?.setPureMode(version.isCleanPhotoOnly)
                val query = ScrollSearchStateBus.getEffectiveSearchQuery()
                val quality = ScrollSearchStateBus.imageQuality.value
                val searchType = ScrollSearchStateBus.selectedSearchType.value
                fastWebViewManager?.loadSearchQuery(
                    query = query,
                    isPure = version.isCleanPhotoOnly,
                    qualityParam = quality.tbsParam,
                    searchType = searchType
                )
            }
        }

        // Observe image quality changes (applied to F versions)
        scope.launch {
            ScrollSearchStateBus.imageQuality.drop(1).collectLatest { quality ->
                val version = ScrollSearchStateBus.activeVersion.value
                if (version.isCleanPhotoOnly) {
                    val query = ScrollSearchStateBus.getEffectiveSearchQuery()
                    val searchType = ScrollSearchStateBus.selectedSearchType.value
                    fastWebViewManager?.loadSearchQuery(
                        query = query,
                        isPure = true,
                        qualityParam = quality.tbsParam,
                        searchType = searchType
                    )
                }
            }
        }

        // Observe plan changes (when returning to Basic mode, reload search query in WebView)
        scope.launch {
            ScrollSearchStateBus.isPremiumActive.drop(1).collectLatest { isPremium ->
                if (!isPremium) {
                    val query = ScrollSearchStateBus.getEffectiveSearchQuery()
                    val version = ScrollSearchStateBus.activeVersion.value
                    val quality = ScrollSearchStateBus.imageQuality.value
                    val searchType = ScrollSearchStateBus.selectedSearchType.value
                    fastWebViewManager?.loadSearchQuery(
                        query = query,
                        isPure = version.isCleanPhotoOnly,
                        qualityParam = quality.tbsParam,
                        searchType = searchType
                    )
                }
            }
        }
    }

    private fun recalculateLayoutParams(
        collapsed: Boolean = ScrollSearchStateBus.isBubbleCollapsed.value,
        version: OverlayUiVersion = ScrollSearchStateBus.activeVersion.value,
        ratio: Float = ScrollSearchStateBus.heightRatio.value,
        isPhotoDetail: Boolean = ScrollSearchStateBus.isPhotoDetailOpen.value,
        isV1Expanded: Boolean = ScrollSearchStateBus.isV1WebViewExpanded.value,
        offsetY: Int = ScrollSearchStateBus.overlayOffsetY.value,
        offsetX: Int = ScrollSearchStateBus.overlayOffsetX.value,
        isSuperFullscreen: Boolean = ScrollSearchStateBus.isSuperFullscreenEnabled.value
    ) {
        val isFocused = ScrollSearchStateBus.isSearchFocused.value

        val baseFlags = if (isFocused) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
        layoutParams.flags = baseFlags

        if (isPhotoDetail) {
            if (isSuperFullscreen) {
                // Super Tela Cheia: covers the ENTIRE phone screen over all apps edge-to-edge
                layoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            }
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.gravity = Gravity.FILL
            layoutParams.x = 0
            layoutParams.y = 0
            return
        }

        if (collapsed) {
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = ScrollSearchStateBus.bubbleX.value
            layoutParams.y = ScrollSearchStateBus.bubbleY.value
            return
        }

        if (ScrollSearchStateBus.isModalFullscreen.value) {
            layoutParams.flags = baseFlags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 0
            layoutParams.y = 0
            return
        }

        when (version) {
            // V1 and F1: Bottom bar layouts
            OverlayUiVersion.V1,
            OverlayUiVersion.F1 -> {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                val v1Height = if (version == OverlayUiVersion.F1) {
                    (210 * density).toInt()
                } else if (isV1Expanded) {
                    (screenHeight * 0.50f).toInt()
                } else {
                    (225 * density).toInt()
                }
                layoutParams.height = v1Height
                layoutParams.x = offsetX
                layoutParams.y = offsetY.coerceAtLeast(0)
            }

            // V2 and F2: Sidebar layouts on right edge
            OverlayUiVersion.V2,
            OverlayUiVersion.F2 -> {
                layoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                layoutParams.width = (240 * density).toInt().coerceAtMost((screenWidth * 0.65f).toInt())
                layoutParams.height = (screenHeight * 0.78f).toInt()
                layoutParams.x = offsetX
                layoutParams.y = offsetY
            }

            // V5, V6, V7 and F5, F6, F7: Resizable bottom panels
            OverlayUiVersion.V5,
            OverlayUiVersion.V6,
            OverlayUiVersion.V7,
            OverlayUiVersion.F5,
            OverlayUiVersion.F6,
            OverlayUiVersion.F7 -> {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                val panelHeight = (screenHeight * ratio).toInt().coerceIn(
                    (200 * density).toInt(),
                    (screenHeight * 0.88f).toInt()
                )
                layoutParams.height = panelHeight
                layoutParams.x = 0
                layoutParams.y = 0
            }
        }
    }

    private fun applyLayoutParams() {
        if (!isAttached || composeView == null) return
        try {
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply layout params: ${e.message}")
        }
    }

    private fun updateFocusableMode(isFocused: Boolean) {
        if (!isAttached || composeView == null) return
        try {
            recalculateLayoutParams()
            windowManager.updateViewLayout(composeView, layoutParams)
            Log.d(TAG, "Updated focusable mode: isFocused=$isFocused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update focusable mode: ${e.message}")
        }
    }

    private fun onMenuDragged(deltaX: Float, deltaY: Float) {
        if (!isAttached || composeView == null) return
        try {
            val currentVersion = ScrollSearchStateBus.activeVersion.value
            if (currentVersion == OverlayUiVersion.V2 || currentVersion == OverlayUiVersion.F2) {
                // Sidebar: move vertically
                val newY = (layoutParams.y + deltaY.toInt()).coerceIn(-600, 600)
                layoutParams.y = newY
                windowManager.updateViewLayout(composeView, layoutParams)
            } else {
                // Bottom card: dragging vertically changes the card's height smoothly
                val deltaRatio = -deltaY / screenHeight.toFloat()
                ScrollSearchStateBus.adjustHeightRatio(deltaRatio)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dragging menu: ${e.message}")
        }
    }

    private fun onBubbleDragged(deltaX: Float, deltaY: Float) {
        if (!isAttached || composeView == null) return
        try {
            val newX = (layoutParams.x + deltaX.toInt()).coerceIn(0, screenWidth - 100)
            val newY = (layoutParams.y + deltaY.toInt()).coerceIn(60, screenHeight - 100)
            layoutParams.x = newX
            layoutParams.y = newY
            ScrollSearchStateBus.updateBubblePosition(newX, newY)
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble position: ${e.message}")
        }
    }

    private fun onSearchQueryTriggered(query: String) {
        val cleanQuery = query.trim().ifEmpty { "tiktok" }
        val currentVersion = ScrollSearchStateBus.activeVersion.value
        val quality = ScrollSearchStateBus.imageQuality.value

        // Only load into FastWebViewManager if in Basic mode (Premium uses the SerpApi-backed search endpoint)
        if (!ScrollSearchStateBus.isPremiumActive.value) {
            fastWebViewManager?.loadSearchQuery(
                query = cleanQuery,
                isPure = currentVersion.isCleanPhotoOnly,
                qualityParam = quality.tbsParam
            )
        }
    }

    fun cleanResetWebView() {
        fastWebViewManager?.cleanRecreate()
    }

    fun hideOverlay() {
        if (!isAttached) return
        try {
            ScrollSearchStateBus.setOnRequestFocusToggle(null)
            ScrollSearchStateBus.setOnQueryChangedListener(null)

            composeView?.let { view ->
                windowManager.removeView(view)
            }
            viewTreeOwner?.destroy()
            fastWebViewManager?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating overlay: ${e.message}")
        } finally {
            composeView = null
            viewTreeOwner = null
            fastWebViewManager = null
            isAttached = false
            scope.cancel()
        }
    }
}
