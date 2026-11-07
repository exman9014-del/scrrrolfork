package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.SearchHistoryItem
import com.example.data.db.SearchHistoryRepository
import com.example.model.ImageQuality
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.model.SearchEngine
import com.example.model.SearchType
import com.example.model.UserPlan
import com.example.util.GoogleSearchHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object ScrollSearchStateBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var historyRepository: SearchHistoryRepository? = null

    // Option for user to save search history (True by default, can be toggled by user)
    private val _isSaveSearchHistoryEnabled = MutableStateFlow(true)
    val isSaveSearchHistoryEnabled: StateFlow<Boolean> = _isSaveSearchHistoryEnabled.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()

    private val _isHistoryDialogOpen = MutableStateFlow(false)
    val isHistoryDialogOpen: StateFlow<Boolean> = _isHistoryDialogOpen.asStateFlow()

    private val _currentUsername = MutableStateFlow<String?>("khaby.lame")
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _selectedSearchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val selectedSearchEngine: StateFlow<SearchEngine> = _selectedSearchEngine.asStateFlow()

    // Sistema de Planos (Plano Básico vs Plano Premium) - Inicia em Premium por padrão
    private val _currentPlan = MutableStateFlow(UserPlan.PREMIUM)
    val currentPlan: StateFlow<UserPlan> = _currentPlan.asStateFlow()

    private val _isPremiumActive = MutableStateFlow(true)
    val isPremiumActive: StateFlow<Boolean> = _isPremiumActive.asStateFlow()

    private val _customSearchApiUrl = MutableStateFlow("")
    val customSearchApiUrl: StateFlow<String> = _customSearchApiUrl.asStateFlow()

    private val _lastCustomSearchError = MutableStateFlow<String?>(null)
    val lastCustomSearchError: StateFlow<String?> = _lastCustomSearchError.asStateFlow()

    // Search Type: IMAGES, VIDEOS, ALL_WEB (sites), NEWS
    private val _selectedSearchType = MutableStateFlow(SearchType.IMAGES)
    val selectedSearchType: StateFlow<SearchType> = _selectedSearchType.asStateFlow()

    // Sem Censura (SafeSearch OFF) - Enabled by default as requested
    private val _isSafeSearchDisabled = MutableStateFlow(true)
    val isSafeSearchDisabled: StateFlow<Boolean> = _isSafeSearchDisabled.asStateFlow()

    // Super Tela Cheia - Enabled by default as requested
    private val _isSuperFullscreenEnabled = MutableStateFlow(true)
    val isSuperFullscreenEnabled: StateFlow<Boolean> = _isSuperFullscreenEnabled.asStateFlow()

    // Modal Cheio (ocupa 100% da tela)
    private val _isModalFullscreen = MutableStateFlow(false)
    val isModalFullscreen: StateFlow<Boolean> = _isModalFullscreen.asStateFlow()

    // Encolher/Expandir menu do cabeçalho nas versões V
    private val _isHeaderMenuCollapsed = MutableStateFlow(false)
    val isHeaderMenuCollapsed: StateFlow<Boolean> = _isHeaderMenuCollapsed.asStateFlow()

    // Bloqueador de Anúncios (AdBlock) ao entrar em sites
    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    // Download automático de vídeo ao rolar
    private val _isAutoDownloadVideoEnabled = MutableStateFlow(false)
    val isAutoDownloadVideoEnabled: StateFlow<Boolean> = _isAutoDownloadVideoEnabled.asStateFlow()

    // Modo Picture-in-Picture (PiP) ao assistir vídeo
    private val _isPipVideoEnabled = MutableStateFlow(true)
    val isPipVideoEnabled: StateFlow<Boolean> = _isPipVideoEnabled.asStateFlow()

    private val _lastDetectedVideoUrl = MutableStateFlow<String?>(null)
    val lastDetectedVideoUrl: StateFlow<String?> = _lastDetectedVideoUrl.asStateFlow()

    // Unread notification badge count when bubble is minimized (e.g. "3" in red badge)
    private val _unreadNotificationCount = MutableStateFlow(3)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    // Filtro de Alta Qualidade (apenas imagens HD/grandes na busca)
    private val _isHighQualityFilterEnabled = MutableStateFlow(true)
    val isHighQualityFilterEnabled: StateFlow<Boolean> = _isHighQualityFilterEnabled.asStateFlow()

    // Rede Social selecionada na Landing Page (TikTok, Instagram, Facebook, YouTube Shorts, Bluesky)
    private val _selectedPlatform = MutableStateFlow(com.example.model.SocialPlatform.TIKTOK)
    val selectedPlatform: StateFlow<com.example.model.SocialPlatform> = _selectedPlatform.asStateFlow()

    private val _activeDetectedPlatform = MutableStateFlow<com.example.model.SocialPlatform?>(com.example.model.SocialPlatform.TIKTOK)
    val activeDetectedPlatform: StateFlow<com.example.model.SocialPlatform?> = _activeDetectedPlatform.asStateFlow()

    private val _imageQuality = MutableStateFlow(ImageQuality.HIGH)
    val imageQuality: StateFlow<ImageQuality> = _imageQuality.asStateFlow()

    // TikTok Dark (true) or TikTok Light (false)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(true)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isBubbleCollapsed = MutableStateFlow(false)
    val isBubbleCollapsed: StateFlow<Boolean> = _isBubbleCollapsed.asStateFlow()

    private val _activeVersion = MutableStateFlow(OverlayUiVersion.F7)
    val activeVersion: StateFlow<OverlayUiVersion> = _activeVersion.asStateFlow()

    private val _tags = MutableStateFlow(
        listOf("tudo", "viral", "dança", "humor", "tutorial", "outfit", "wallpaper", "lifestyle")
    )
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTag = MutableStateFlow("tudo")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // Holds the exact text shown in the search bar: e.g. "@khaby.lame" or "@khaby.lame viral"
    private val _searchBarText = MutableStateFlow("@khaby.lame")
    val searchBarText: StateFlow<String> = _searchBarText.asStateFlow()

    private val _heightRatio = MutableStateFlow(0.50f)
    val heightRatio: StateFlow<Float> = _heightRatio.asStateFlow()

    // Offset coordinates for dragging the menu bar / overlay around the screen
    private val _overlayOffsetY = MutableStateFlow(0)
    val overlayOffsetY: StateFlow<Int> = _overlayOffsetY.asStateFlow()

    private val _overlayOffsetX = MutableStateFlow(0)
    val overlayOffsetX: StateFlow<Int> = _overlayOffsetX.asStateFlow()

    // Rotation angle for PhotoDetailDialog (0f, 90f, 180f, 270f) - "ver celular virado"
    private val _rotationAngle = MutableStateFlow(0f)
    val rotationAngle: StateFlow<Float> = _rotationAngle.asStateFlow()

    private val _isSearchFocused = MutableStateFlow(false)
    val isSearchFocused: StateFlow<Boolean> = _isSearchFocused.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()

    private val _nextPageStartIndex = MutableStateFlow<Int?>(null)
    val nextPageStartIndex: StateFlow<Int?> = _nextPageStartIndex.asStateFlow()
    private var customSearchJob: Job? = null
    private var customSearchRequestId = 0L

    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    private val _isTikTokForeground = MutableStateFlow(false)
    val isTikTokForeground: StateFlow<Boolean> = _isTikTokForeground.asStateFlow()

    // Backward compatibility for existing references
    val isInstagramForeground: StateFlow<Boolean> = _isTikTokForeground

    private val _isPhotoDetailOpen = MutableStateFlow(false)
    val isPhotoDetailOpen: StateFlow<Boolean> = _isPhotoDetailOpen.asStateFlow()

    private val _currentCapturedBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val currentCapturedBitmap: StateFlow<android.graphics.Bitmap?> = _currentCapturedBitmap.asStateFlow()

    private val _fullscreenImages = MutableStateFlow<List<String>>(emptyList())
    val fullscreenImages: StateFlow<List<String>> = _fullscreenImages.asStateFlow()

    private val _fullscreenFallbackImages = MutableStateFlow<List<String>>(emptyList())
    val fullscreenFallbackImages: StateFlow<List<String>> = _fullscreenFallbackImages.asStateFlow()

    private val _fullscreenInitialIndex = MutableStateFlow(0)
    val fullscreenInitialIndex: StateFlow<Int> = _fullscreenInitialIndex.asStateFlow()

    private val _isV1WebViewExpanded = MutableStateFlow(false)
    val isV1WebViewExpanded: StateFlow<Boolean> = _isV1WebViewExpanded.asStateFlow()

    private val _bubbleX = MutableStateFlow(0)
    val bubbleX: StateFlow<Int> = _bubbleX.asStateFlow()

    private val _bubbleY = MutableStateFlow(400)
    val bubbleY: StateFlow<Int> = _bubbleY.asStateFlow()

    // Listener callbacks for overlay updates
    private var onQueryChangedListener: ((query: String) -> Unit)? = null
    private var onRequestFocusToggle: ((focused: Boolean) -> Unit)? = null

    fun setOnQueryChangedListener(listener: ((query: String) -> Unit)?) {
        onQueryChangedListener = listener
    }

    fun setOnRequestFocusToggle(listener: ((focused: Boolean) -> Unit)?) {
        onRequestFocusToggle = listener
    }

    /**
     * Called when TikTokAccessibilityService detects a new creator.
     * Updates current creator, increments unread search count, and updates search bar text.
     */
    fun updateDetectedUsername(username: String) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return
        if (_currentUsername.value != clean) {
            _currentUsername.value = clean
            _detectionCount.value += 1
            // If bubble is collapsed, increment unread badge count
            if (_isBubbleCollapsed.value) {
                _unreadNotificationCount.value += 1
            } else {
                _unreadNotificationCount.value = 1
            }
        }
        updateDetectedCreatorQuery(clean)
    }

    private fun updateDetectedCreatorQuery(username: String) {
        val tagSuffix = if (_selectedTag.value.isNotEmpty() && _selectedTag.value != "tudo") " ${_selectedTag.value}" else ""
        val platformSuffix = if (_activeDetectedPlatform.value == com.example.model.SocialPlatform.X) " X Twitter" else ""
        val nextQuery = "@$username$platformSuffix$tagSuffix"
        if (_searchBarText.value != nextQuery) {
            _searchBarText.value = nextQuery
            notifyEffectiveQueryChanged()
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        if (_selectedSearchEngine.value != engine) {
            _selectedSearchEngine.value = engine
            notifyEffectiveQueryChanged()
        }
    }

    fun setSearchType(type: SearchType) {
        if (_selectedSearchType.value != type) {
            _selectedSearchType.value = type
            notifyEffectiveQueryChanged()
        }
    }

    fun toggleSafeSearch() {
        _isSafeSearchDisabled.value = !_isSafeSearchDisabled.value
        notifyEffectiveQueryChanged()
    }

    fun setSafeSearchDisabled(disabled: Boolean) {
        if (_isSafeSearchDisabled.value != disabled) {
            _isSafeSearchDisabled.value = disabled
            notifyEffectiveQueryChanged()
        }
    }

    fun toggleSuperFullscreen() {
        _isSuperFullscreenEnabled.value = !_isSuperFullscreenEnabled.value
    }

    fun setSuperFullscreen(enabled: Boolean) {
        _isSuperFullscreenEnabled.value = enabled
    }

    fun toggleModalFullscreen() {
        _isModalFullscreen.value = !_isModalFullscreen.value
    }

    fun setModalFullscreen(enabled: Boolean) {
        _isModalFullscreen.value = enabled
    }

    fun toggleHeaderMenuCollapsed() {
        _isHeaderMenuCollapsed.value = !_isHeaderMenuCollapsed.value
    }

    fun toggleAdBlock() {
        _isAdBlockEnabled.value = !_isAdBlockEnabled.value
    }

    fun toggleAutoDownloadVideo() {
        _isAutoDownloadVideoEnabled.value = !_isAutoDownloadVideoEnabled.value
    }

    fun togglePipVideo() {
        _isPipVideoEnabled.value = !_isPipVideoEnabled.value
    }

    fun updateDetectedVideoUrl(url: String?) {
        _lastDetectedVideoUrl.value = url
    }

    fun toggleHighQualityFilter() {
        _isHighQualityFilterEnabled.value = !_isHighQualityFilterEnabled.value
        notifyEffectiveQueryChanged()
    }

    fun setHighQualityFilter(enabled: Boolean) {
        if (_isHighQualityFilterEnabled.value != enabled) {
            _isHighQualityFilterEnabled.value = enabled
            notifyEffectiveQueryChanged()
        }
    }

    fun selectSocialPlatform(platform: com.example.model.SocialPlatform) {
        _selectedPlatform.value = platform
        _activeDetectedPlatform.value = platform
        val firstSample = platform.sampleCreators.firstOrNull() ?: "criador"
        updateDetectedUsername(firstSample)
    }

    fun setActiveDetectedPlatform(platform: com.example.model.SocialPlatform?) {
        if (_activeDetectedPlatform.value != platform) {
            _activeDetectedPlatform.value = platform
            _currentUsername.value?.let(::updateDetectedCreatorQuery)
        }
    }

    fun setVersion(version: OverlayUiVersion) {
        _activeVersion.value = version
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
    }

    fun setImageQuality(quality: ImageQuality) {
        _imageQuality.value = quality
        notifyEffectiveQueryChanged()
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }

    fun setBubbleCollapsed(collapsed: Boolean) {
        _isBubbleCollapsed.value = collapsed
        if (collapsed) {
            setSearchFocused(false)
        } else {
            // User opened the overlay, reset unread notification badge
            _unreadNotificationCount.value = 0
        }
    }

    fun resetUnreadCount() {
        _unreadNotificationCount.value = 0
    }

    fun moveOverlay(deltaX: Int, deltaY: Int) {
        _overlayOffsetX.value = (_overlayOffsetX.value + deltaX).coerceIn(-400, 400)
        _overlayOffsetY.value = (_overlayOffsetY.value + deltaY).coerceIn(-800, 800)
    }

    fun resetOverlayPosition() {
        _overlayOffsetX.value = 0
        _overlayOffsetY.value = 0
    }

    fun rotateOrientation() {
        val current = _rotationAngle.value
        _rotationAngle.value = (current + 90f) % 360f
    }

    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle % 360f
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
        val current = _searchBarText.value.trim()
        val userMatch = Regex("^@[a-zA-Z0-9._]+").find(current)
        if (userMatch != null) {
            val userPrefix = userMatch.value
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "$userPrefix$tagSuffix"
        } else if (current.isNotEmpty()) {
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "$current$tagSuffix"
        } else {
            val user = _currentUsername.value ?: "tiktok"
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "@$user$tagSuffix"
        }
        notifyEffectiveQueryChanged()
    }

    fun addCustomTag(newTag: String) {
        val clean = newTag.trim().lowercase().removePrefix("#")
        if (clean.isNotEmpty() && !_tags.value.contains(clean)) {
            _tags.value = _tags.value + clean
            selectTag(clean)
        }
    }

    fun removeTag(tag: String) {
        if (tag != "tudo" && _tags.value.contains(tag)) {
            _tags.value = _tags.value - tag
            if (_selectedTag.value == tag) {
                selectTag("tudo")
            }
        }
    }

    fun setSearchBarText(text: String, executeSearch: Boolean = false) {
        _searchBarText.value = text
        if (executeSearch) {
            notifyEffectiveQueryChanged()
        }
    }

    fun submitSearch(query: String = _searchBarText.value) {
        _searchBarText.value = query
        notifyEffectiveQueryChanged()
    }

    fun setSearchFocused(focused: Boolean) {
        _isSearchFocused.value = focused
        onRequestFocusToggle?.invoke(focused)
    }

    fun setHeightRatio(ratio: Float) {
        _heightRatio.value = ratio.coerceIn(0.20f, 0.88f)
    }

    fun adjustHeightRatio(delta: Float) {
        _heightRatio.value = (_heightRatio.value + delta).coerceIn(0.20f, 0.88f)
    }

    fun setTikTokForeground(inForeground: Boolean) {
        _isTikTokForeground.value = inForeground
    }

    // Compatibility method for Instagram references
    fun setInstagramForeground(inForeground: Boolean) {
        _isTikTokForeground.value = inForeground
    }

    fun setPhotoDetailOpen(open: Boolean) {
        _isPhotoDetailOpen.value = open
        if (!open) {
            _rotationAngle.value = 0f
        }
    }

    fun openFullscreenPhoto(
        selectedUrl: String,
        allUrls: List<String> = emptyList(),
        explicitIndex: Int = -1,
        fallbackUrls: List<String> = emptyList()
    ) {
        _currentCapturedBitmap.value = null
        val list = if (allUrls.isNotEmpty()) allUrls else listOf(selectedUrl)
        val index = if (explicitIndex in list.indices) {
            explicitIndex
        } else {
            val idx = list.indexOf(selectedUrl)
            if (idx >= 0) idx else 0
        }
        _fullscreenImages.value = list
        _fullscreenFallbackImages.value = fallbackUrls
        _fullscreenInitialIndex.value = index
        _isPhotoDetailOpen.value = true
    }

    fun openFullscreenWithBitmap(
        bitmap: android.graphics.Bitmap?,
        selectedUrl: String,
        allUrls: List<String> = emptyList(),
        explicitIndex: Int = -1
    ) {
        _currentCapturedBitmap.value = if (bitmap != null && bitmap.width > 10 && bitmap.height > 10) bitmap else null
        val list = if (allUrls.isNotEmpty()) {
            allUrls
        } else if (selectedUrl.isNotBlank()) {
            listOf(selectedUrl)
        } else {
            emptyList()
        }
        val index = if (explicitIndex in list.indices) {
            explicitIndex
        } else if (selectedUrl.isNotBlank()) {
            val idx = list.indexOf(selectedUrl)
            if (idx >= 0) idx else 0
        } else {
            0
        }
        _fullscreenImages.value = list
        _fullscreenFallbackImages.value = emptyList()
        _fullscreenInitialIndex.value = index
        _isPhotoDetailOpen.value = true
    }

    fun closeFullscreenPhoto() {
        _currentCapturedBitmap.value = null
        _isPhotoDetailOpen.value = false
        _fullscreenFallbackImages.value = emptyList()
        _rotationAngle.value = 0f
    }

    fun setV1WebViewExpanded(expanded: Boolean) {
        _isV1WebViewExpanded.value = expanded
    }

    fun updateBubblePosition(x: Int, y: Int) {
        _bubbleX.value = x
        _bubbleY.value = y
    }

    fun updatePhotos(items: List<PhotoItem>, loading: Boolean = false) {
        _photos.value = items
        _isLoadingPhotos.value = loading
    }

    fun setLoadingPhotos(loading: Boolean) {
        _isLoadingPhotos.value = loading
    }

    fun getEffectiveSearchQuery(): String {
        val raw = _searchBarText.value.trim()
        if (raw.isEmpty()) {
            val user = _currentUsername.value ?: "tiktok"
            return "$user tiktok"
        }
        val clean = raw.replace(Regex("@([a-zA-Z0-9._]+)"), "$1").trim()
        return clean.ifEmpty { "tiktok" }
    }

    private var sharedPreferences: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val prefs = context.getSharedPreferences("scrollsearch_prefs", Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val savedPlanName = prefs.getString("user_plan", UserPlan.PREMIUM.name)
            val restoredPlan = try {
                UserPlan.valueOf(savedPlanName ?: UserPlan.PREMIUM.name)
            } catch (_: Exception) {
                UserPlan.PREMIUM
            }
            _currentPlan.value = restoredPlan
            _isPremiumActive.value = restoredPlan.isPremium
        }
        if (historyRepository != null) return
        val db = AppDatabase.getDatabase(context)
        val repo = SearchHistoryRepository(db.searchHistoryDao())
        historyRepository = repo

        scope.launch {
            repo.getRecentHistory(50).collectLatest { list ->
                _searchHistory.value = list
            }
        }
    }

    fun toggleSaveSearchHistory() {
        _isSaveSearchHistoryEnabled.value = !_isSaveSearchHistoryEnabled.value
    }

    fun setSaveSearchHistory(enabled: Boolean) {
        _isSaveSearchHistoryEnabled.value = enabled
    }

    fun setHistoryDialogOpen(open: Boolean) {
        _isHistoryDialogOpen.value = open
    }

    fun openHistoryDialog() {
        _isHistoryDialogOpen.value = true
    }

    fun deleteHistoryItem(id: Long) {
        scope.launch {
            historyRepository?.deleteById(id)
        }
    }

    fun deleteHistoryItem(item: com.example.data.db.SearchHistoryItem) {
        deleteHistoryItem(item.id)
    }

    fun clearSearchHistory() {
        scope.launch {
            historyRepository?.clearAll()
        }
    }

    fun recordSearch(query: String) {
        if (!_isSaveSearchHistoryEnabled.value) return
        val clean = query.trim()
        if (clean.isBlank()) return
        scope.launch {
            historyRepository?.saveSearch(
                query = query,
                cleanQuery = clean,
                engine = _selectedSearchEngine.value.shortName,
                searchType = _selectedSearchType.value.displayName,
                creatorUsername = _currentUsername.value
            )
        }
    }

    fun setPlan(plan: UserPlan) {
        _currentPlan.value = plan
        _isPremiumActive.value = plan.isPremium
        sharedPreferences?.edit()?.putString("user_plan", plan.name)?.apply()
        notifyEffectiveQueryChanged()
    }

    fun togglePlan() {
        val next = if (_currentPlan.value == UserPlan.PREMIUM) UserPlan.BASIC else UserPlan.PREMIUM
        setPlan(next)
    }

    fun setPremiumActive(active: Boolean) {
        val plan = if (active) UserPlan.PREMIUM else UserPlan.BASIC
        setPlan(plan)
    }

    fun setCustomSearchApiUrl(apiUrl: String) {
        _customSearchApiUrl.value = apiUrl.trim()
        if (_isPremiumActive.value) {
            notifyEffectiveQueryChanged()
        }
    }

    fun getEffectiveSearchApiUrl(): String {
        val custom = _customSearchApiUrl.value.trim()
        val raw = custom.ifBlank { GoogleSearchHelper.getSearchApiUrl() }
        return GoogleSearchHelper.sanitizeSearchApiUrl(raw)
    }

    fun notifyEffectiveQueryChanged() {
        val query = getEffectiveSearchQuery()
        recordSearch(query)
        customSearchRequestId += 1
        val requestId = customSearchRequestId
        customSearchJob?.cancel()
        if (_isPremiumActive.value) {
            _photos.value = emptyList()
            _nextPageStartIndex.value = null
            _lastCustomSearchError.value = null
            _isLoadingPhotos.value = true
            customSearchJob = scope.launch {
                try {
                    val page = GoogleSearchHelper.fetchGoogleCustomSearchPhotos(
                        query = query,
                        apiUrl = getEffectiveSearchApiUrl(),
                        start = 1,
                        num = 100
                    )
                    if (requestId == customSearchRequestId) {
                        _photos.value = page.photos
                        _nextPageStartIndex.value = page.nextStartIndex
                        _lastCustomSearchError.value = page.errorMessage
                    }
                } finally {
                    if (requestId == customSearchRequestId) _isLoadingPhotos.value = false
                }
            }
        } else {
            _isLoadingPhotos.value = false
        }
        onQueryChangedListener?.invoke(query)
    }

    fun loadMoreCustomSearchPhotos() {
        val start = _nextPageStartIndex.value ?: return
        if (!_isPremiumActive.value || _isLoadingPhotos.value) return

        val requestId = customSearchRequestId
        val query = getEffectiveSearchQuery()
        _isLoadingPhotos.value = true
        customSearchJob = scope.launch {
            try {
                val page = GoogleSearchHelper.fetchGoogleCustomSearchPhotos(
                    query = query,
                    apiUrl = getEffectiveSearchApiUrl(),
                    start = start,
                    num = 100
                )
                if (requestId == customSearchRequestId) {
                    val existingUrls = _photos.value.mapTo(mutableSetOf()) { it.url }
                    _photos.value = _photos.value + page.photos.filter { existingUrls.add(it.url) }
                    _nextPageStartIndex.value = page.nextStartIndex
                    _lastCustomSearchError.value = page.errorMessage
                }
            } finally {
                if (requestId == customSearchRequestId) _isLoadingPhotos.value = false
            }
        }
    }
}
