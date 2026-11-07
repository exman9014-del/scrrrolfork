package com.example.ui.dashboard

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import com.example.data.db.SearchHistoryItem
import com.example.model.SearchEngine
import com.example.model.SocialPlatform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.ScrollSearchStateBus
import com.example.model.ImageQuality
import com.example.model.OverlayUiVersion
import com.example.model.PremiumSearchProvider
import com.example.model.SearchType
import com.example.service.FloatingOverlayService
import com.example.ui.overlay.AddTagDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgPurple
import com.example.ui.theme.IgYellow
import com.example.ui.theme.LightBackground
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokDarkText
import com.example.ui.theme.TikTokRed
import com.example.util.GoogleSearchHelper
import com.example.util.PremiumProviderResult
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    isOverlayPermissionGranted: Boolean,
    isAccessibilityServiceEnabled: Boolean,
    isStoragePermissionGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onLaunchInstagram: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUsername by ScrollSearchStateBus.currentUsername.collectAsState()
    val isOverlayActive by ScrollSearchStateBus.isOverlayActive.collectAsState()
    val activeVersion by ScrollSearchStateBus.activeVersion.collectAsState()
    val currentEngine by ScrollSearchStateBus.selectedSearchEngine.collectAsState()
    val searchType by ScrollSearchStateBus.selectedSearchType.collectAsState()
    val isSafeSearchDisabled by ScrollSearchStateBus.isSafeSearchDisabled.collectAsState()
    val isSuperFullscreen by ScrollSearchStateBus.isSuperFullscreenEnabled.collectAsState()
    val isDark by ScrollSearchStateBus.isDarkTheme.collectAsState()
    val selectedPlatform by ScrollSearchStateBus.selectedPlatform.collectAsState()
    val isHighQualityFilter by ScrollSearchStateBus.isHighQualityFilterEnabled.collectAsState()
    val tags by ScrollSearchStateBus.tags.collectAsState()
    val selectedTag by ScrollSearchStateBus.selectedTag.collectAsState()
    val detectionCount by ScrollSearchStateBus.detectionCount.collectAsState()
    val isTikTokForeground by ScrollSearchStateBus.isTikTokForeground.collectAsState()
    val imageQuality by ScrollSearchStateBus.imageQuality.collectAsState()
    val isSaveSearchHistoryEnabled by ScrollSearchStateBus.isSaveSearchHistoryEnabled.collectAsState()
    val searchHistory by ScrollSearchStateBus.searchHistory.collectAsState()
    val isPremiumActive by ScrollSearchStateBus.isPremiumActive.collectAsState()
    val currentPlan by ScrollSearchStateBus.currentPlan.collectAsState()

    val surfaceColor = if (isDark) DarkSurface else Color.White
    val bgColor = if (isDark) DarkBackground else LightBackground
    val textColor = if (isDark) Color.White else TikTokDarkText
    val textMuted = if (isDark) Color(0xFFA0A0C0) else Color(0xFF6B6C7B)

    var showAddTagDialog by remember { mutableStateOf(false) }
    var selectedPremiumProvider by remember { mutableStateOf<PremiumSearchProvider?>(null) }
    var showPremiumRequired by remember { mutableStateOf(false) }
    var providerQuery by remember { mutableStateOf("") }
    var reverseImageUrl by remember { mutableStateOf("") }
    var providerResults by remember { mutableStateOf<List<PremiumProviderResult>>(emptyList()) }
    var providerSearchError by remember { mutableStateOf<String?>(null) }
    var providerSearchLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Title & Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        TikTokRed.copy(alpha = if (isDark) 0.22f else 0.12f),
                                        TikTokCyan.copy(alpha = if (isDark) 0.10f else 0.05f),
                                        surfaceColor
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TikTok",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Lens",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TikTokRed
                                    )
                                }
                                Text(
                                    text = "Detector de Vídeos TikTok Live Overlay",
                                    fontSize = 12.sp,
                                    color = TikTokCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Theme toggle button (TikTok Dark / TikTok Light)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.toggleTheme() }
                                        .testTag("theme_toggle_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Alternar Tema",
                                            tint = if (isDark) Color(0xFFFFD600) else Color(0xFF161823),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isDark) "Claro" else "Escuro",
                                            color = textColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Live TikTok Activity Chip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isTikTokForeground) Color(0xFF00C853).copy(alpha = 0.2f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                                    border = if (isTikTokForeground) {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C853))
                                    } else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isTikTokForeground) Color(0xFF00C853) else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isTikTokForeground) "TikTok Ativo" else "Standby",
                                            color = if (isTikTokForeground) Color(0xFF00E676) else textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Plan Mode Indicator Chip (Básico vs Premium)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPremiumActive) Color(0x33FFD700) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                                    border = if (isPremiumActive) {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.togglePlan() }
                                        .testTag("dashboard_plan_indicator_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPremiumActive) "👑 Premium" else "📦 Básico",
                                            color = if (isPremiumActive) Color(0xFFFFD700) else textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = "Criador TikTok",
                                value = "@${currentUsername ?: "nenhum"}",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Vídeos",
                                value = "$detectionCount",
                                modifier = Modifier.weight(0.8f)
                            )
                            StatBox(
                                title = "Plano Ativo",
                                value = if (isPremiumActive) "👑 Premium" else "📦 Básico",
                                modifier = Modifier.weight(1.1f)
                            )
                            StatBox(
                                title = "Scanner",
                                value = "35ms",
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // SEÇÃO DE ESCOLHA DE PLANOS (PLANO BÁSICO VS PLANO PREMIUM)
            // =========================================================================
            item {
                PlanComparisonCard()
            }

            // Master Activation Toggle & Permissions
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overlay Flutuante TikTokLens",
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exibe busca instantânea sobreposta aos vídeos do TikTok",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = isOverlayActive,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (isOverlayPermissionGranted) {
                                            FloatingOverlayService.startService(context)
                                        } else {
                                            onRequestOverlayPermission()
                                        }
                                    } else {
                                        FloatingOverlayService.stopService(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TikTokRed,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = if (isDark) DarkSurfaceElevated else Color(0xFFE4E4EB)
                                ),
                                modifier = Modifier.testTag("overlay_toggle_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Permissions Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PermissionStatusPill(
                                label = "Sobreposição",
                                granted = isOverlayPermissionGranted,
                                onClick = onRequestOverlayPermission,
                                modifier = Modifier.weight(1f)
                            )
                            PermissionStatusPill(
                                label = "Acessibilidade",
                                granted = isAccessibilityServiceEnabled,
                                onClick = onRequestAccessibilityPermission,
                                modifier = Modifier.weight(1f)
                            )
                            PermissionStatusPill(
                                label = "Armazenamento",
                                granted = isStoragePermissionGranted,
                                onClick = onRequestStoragePermission,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // SEÇÃO LANDING PAGE: ESCOLHA A REDE SOCIAL PARA SCROLAR
            // =========================================================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    modifier = Modifier.fillMaxWidth().testTag("landing_page_social_section")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Escolha a Rede Social para Rolar",
                                    color = textColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Detecta posts, vídeos e buscas no TikTok, Instagram, Facebook, Shorts e Bluesky",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Carousel of Social Network Cards
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SocialPlatform.entries) { platform ->
                                val isSelected = selectedPlatform == platform
                                SocialPlatformCard(
                                    platform = platform,
                                    isSelected = isSelected,
                                    isDark = isDark,
                                    onSelect = {
                                        ScrollSearchStateBus.selectSocialPlatform(platform)
                                    },
                                    onLaunch = {
                                        ScrollSearchStateBus.selectSocialPlatform(platform)
                                        if (!isOverlayActive && isOverlayPermissionGranted) {
                                            FloatingOverlayService.startService(context)
                                        }
                                        launchSocialApp(context, platform)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(surfaceColor)
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "Buscas Premium",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "Google, Yandex e TikTok; contas Básicas veem os cards bloqueados.",
                        color = textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(PremiumSearchProvider.entries) { provider ->
                            PremiumProviderCard(
                                provider = provider,
                                isLocked = !isPremiumActive,
                                isDark = isDark,
                                onClick = {
                                    if (!isPremiumActive) {
                                        showPremiumRequired = true
                                    } else {
                                        providerQuery = if (provider.requiresImageUrl) {
                                            ""
                                        } else {
                                            ScrollSearchStateBus.getEffectiveSearchQuery()
                                        }
                                        reverseImageUrl = ""
                                        providerResults = emptyList()
                                        providerSearchError = null
                                        selectedPremiumProvider = provider
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Image quality filters are used by both Premium and F-version searches.
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth().testTag("image_quality_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IgOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Qualidade",
                                    tint = IgOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Qualidade da Imagem",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Escolha o tamanho priorizado nas buscas Premium e nas versões puras",
                                    color = Color(0xFFA0A0C0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Options: Baixa, Média, Alta, Altíssima
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImageQuality.entries.forEach { quality ->
                                val isSelected = imageQuality == quality
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) IgOrange.copy(alpha = 0.20f) else DarkSurfaceElevated,
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, IgOrange)
                                    } else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.setImageQuality(quality) }
                                        .testTag("quality_option_${quality.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) IgOrange else Color.Gray,
                                                    shape = CircleShape
                                                )
                                                .padding(3.dp)
                                                .background(
                                                    if (isSelected) IgOrange else Color.Transparent,
                                                    CircleShape
                                                )
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = quality.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                            Text(
                                                text = quality.subtitle,
                                                color = Color(0xFFA0A0B8),
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = IgOrange,
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Ativa",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search engine and SafeSearch preference.
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mecanismo de Busca",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Escolha o buscador livremente (evita bloqueios/captcha do Google)",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SearchEngine.entries) { engine ->
                                val isSelected = currentEngine == engine
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else DarkSurfaceElevated,
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.setSearchEngine(engine) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = engine.shortName,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = engine.domain,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search Mode Selection (Imagens, Vídeos, Sites, Notícias)
                        Text(
                            text = "Tipo de Conteúdo (Além de Imagens)",
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pesquise vídeos, sites e notícias como ocorre no Google",
                            color = textMuted,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SearchType.entries) { type ->
                                val isSelected = searchType == type
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TikTokRed.copy(alpha = 0.20f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, TikTokRed)
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.setSearchType(type) }
                                        .testTag("dashboard_search_type_${type.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = type.displayName,
                                            color = if (isSelected) TikTokRed else textColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SafeSearch toggle; this cannot override provider-side policies.
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSafeSearchDisabled) Color(0xFF1B5E20).copy(alpha = 0.35f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSafeSearchDisabled) Color(0xFF00E676) else Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { ScrollSearchStateBus.toggleSafeSearch() }
                                .testTag("dashboard_safesearch_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isSafeSearchDisabled) "SafeSearch: DESLIGADO" else "SafeSearch: LIGADO",
                                            color = if (isSafeSearchDisabled) Color(0xFF00E676) else textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSafeSearchDisabled) Color(0xFF00E676) else Color.Gray
                                        ) {
                                            Text(
                                                text = if (isSafeSearchDisabled) "Safe: OFF" else "Safe: ON",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Solicita menos filtragem quando suportado; provedores e fontes ainda podem limitar resultados.",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isSafeSearchDisabled,
                                    onCheckedChange = { ScrollSearchStateBus.setSafeSearchDisabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E676),
                                        checkedTrackColor = Color(0xFF1B5E20)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Super Tela Cheia Toggle (Enabled by default as requested)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSuperFullscreen) TikTokRed.copy(alpha = 0.20f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSuperFullscreen) TikTokRed else Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { ScrollSearchStateBus.toggleSuperFullscreen() }
                                .testTag("dashboard_super_fullscreen_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSuperFullscreen) "Super Tela Cheia: ATIVADA" else "Super Tela Cheia: DESATIVADA",
                                        color = if (isSuperFullscreen) TikTokRed else textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A tela cheia fica em todo celular ao visualizar as fotos, sobreposto sobre outros apps",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isSuperFullscreen,
                                    onCheckedChange = { ScrollSearchStateBus.setSuperFullscreen(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokRed,
                                        checkedTrackColor = TikTokRed.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Size preference filter; it cannot guarantee image resolution.
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isHighQualityFilter) TikTokCyan.copy(alpha = 0.20f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isHighQualityFilter) TikTokCyan else Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { ScrollSearchStateBus.toggleHighQualityFilter() }
                                .testTag("dashboard_high_quality_filter_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isHighQualityFilter) "Priorizar imagens maiores: ATIVADO" else "Priorizar imagens maiores: DESATIVADO",
                                            color = if (isHighQualityFilter) TikTokCyan else textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isHighQualityFilter) TikTokCyan else Color.Gray
                                        ) {
                                            Text(
                                                text = if (isHighQualityFilter) "Tamanho: ON" else "Tamanho: OFF",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Solicita imagens maiores quando o mecanismo aceita o filtro; resolução e acesso dependem da fonte.",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isHighQualityFilter,
                                    onCheckedChange = { ScrollSearchStateBus.setHighQualityFilter(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokCyan,
                                        checkedTrackColor = TikTokCyan.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // AdBlocker Toggle
                            val isAdBlockEnabled by ScrollSearchStateBus.isAdBlockEnabled.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Bloqueador de Anúncios (AdBlock)",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Bloqueia anúncios e rastreadores automaticamente ao abrir sites no overlay",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isAdBlockEnabled,
                                    onCheckedChange = { ScrollSearchStateBus.toggleAdBlock() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokCyan,
                                        checkedTrackColor = TikTokCyan.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("adblock_toggle_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Auto-Download Video on Scroll Toggle
                            val isAutoDownloadVideo by ScrollSearchStateBus.isAutoDownloadVideoEnabled.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Download Automático de Vídeos ao Rolar",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Captura e baixa automaticamente o link do vídeo do @username detectado ao rolar",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isAutoDownloadVideo,
                                    onCheckedChange = { ScrollSearchStateBus.toggleAutoDownloadVideo() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokRed,
                                        checkedTrackColor = TikTokRed.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("auto_download_video_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Picture-in-Picture (PiP) Video Toggle
                            val isPipVideoEnabled by ScrollSearchStateBus.isPipVideoEnabled.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Modo Picture-in-Picture (PiP) ao Assistir Vídeo",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Exibe o vídeo em janela flutuante Picture-in-Picture sobre a tela do celular",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isPipVideoEnabled,
                                    onCheckedChange = { ScrollSearchStateBus.togglePipVideo() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokCyan,
                                        checkedTrackColor = TikTokCyan.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("pip_video_toggle_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Histórico de Pesquisas Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_history_dashboard_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(TikTokCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Histórico",
                                        tint = TikTokCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Histórico de Pesquisas",
                                        color = textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isSaveSearchHistoryEnabled) "Salvar histórico ativado" else "Gravação desativada",
                                        color = if (isSaveSearchHistoryEnabled) TikTokCyan else textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = isSaveSearchHistoryEnabled,
                                    onCheckedChange = { ScrollSearchStateBus.toggleSaveSearchHistory() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TikTokCyan,
                                        checkedTrackColor = TikTokCyan.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("save_history_toggle_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isSaveSearchHistoryEnabled)
                                "Suas pesquisas recentes ficam gravadas com segurança no dispositivo para acesso rápido."
                            else
                                "A opção de salvar histórico está desativada. As novas buscas não serão guardadas.",
                            color = textMuted,
                            fontSize = 12.sp
                        )

                        if (searchHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recentes (${searchHistory.size})",
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { ScrollSearchStateBus.openHistoryDialog() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Ver todos", fontSize = 11.sp, color = TikTokCyan)
                                    }

                                    OutlinedButton(
                                        onClick = { ScrollSearchStateBus.clearSearchHistory() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Limpar", fontSize = 11.sp, color = TikTokRed)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val recentItems = searchHistory.take(5)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                recentItems.forEach { item ->
                                    val dateStr = remember(item.timestamp) {
                                        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(item.timestamp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) DarkSurfaceElevated else Color(0xFFF5F5FA),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                ScrollSearchStateBus.setSearchBarText(item.query)
                                                if (!isOverlayActive && isOverlayPermissionGranted) {
                                                    try {
                                                        val intent = Intent(context, FloatingOverlayService::class.java)
                                                        context.startForegroundService(intent)
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                            .testTag("dashboard_history_item_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = TikTokCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.query,
                                                        color = textColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = item.searchEngine,
                                                            color = TikTokCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "• $dateStr",
                                                            color = textMuted,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { ScrollSearchStateBus.deleteHistoryItem(item) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remover",
                                                    tint = textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Version Selector (F1-F7 Pure Photos & V1-V7 Tools)
            item {
                Text(
                    text = "Versões F — Só Fotos Grandes (Sem Google)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Foco total na imagem limpa: sem header, sem barras, sem Google, tela limpa",
                    color = IgOrange,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayUiVersion.entries.filter { it.isCleanPhotoOnly }.forEach { version ->
                        val isSelected = activeVersion == version
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) IgOrange.copy(alpha = 0.2f) else DarkSurface,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, IgOrange)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    ScrollSearchStateBus.setVersion(version)
                                }
                                .testTag("select_version_${version.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IgOrange else DarkSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = version.code,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${version.code}: ${version.title}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = version.subtitle,
                                        color = Color(0xFFA0A0C0),
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = IgOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Versões V — Com Controles e Google Web",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayUiVersion.entries.filter { !it.isCleanPhotoOnly }.forEach { version ->
                        val isSelected = activeVersion == version
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) IgPink.copy(alpha = 0.15f) else DarkSurface,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, IgPink)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    ScrollSearchStateBus.setVersion(version)
                                }
                                .testTag("select_version_${version.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IgPink else DarkSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = version.code,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${version.code}: ${version.title}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = version.subtitle,
                                        color = Color(0xFFA0A0C0),
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = IgPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keyword Templates & Dynamic Tags
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Templates de Palavras-Chave",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Busca automática: {nickname} + tag",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { showAddTagDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("dashboard_add_tag_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar tag",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nova Tag", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tags) { tag ->
                                val isSelected = tag.equals(selectedTag, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) IgPink.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.dp, IgPink)
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.selectTag(tag) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (tag == "tudo") "Tudo" else "#$tag",
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )

                                        if (tag != "tudo") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover tag",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { ScrollSearchStateBus.removeTag(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TikTok Simulator & Test Area
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Test Studio / Simulador de Vídeos TikTok",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Simule scrolls rápidos entre criadores famosos do TikTok",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = onLaunchInstagram,
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("launch_instagram_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Abrir TikTok",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Abrir TikTok", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val sampleCreators = listOf(
                            "khaby.lame", "charlidamelio", "bellapoarch", "mrbeast",
                            "luvadepedreiro", "cristiano", "addisonre", "anitta"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sampleCreators) { creator ->
                                val isCurrent = creator.equals(currentUsername, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) TikTokRed.copy(alpha = 0.25f) else (if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)),
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, TikTokRed) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            ScrollSearchStateBus.updateDetectedUsername(creator)
                                        }
                                        .testTag("simulate_creator_$creator")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@$creator",
                                            color = if (isCurrent) TikTokRed else textColor,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Clean WebView Reset Button
                        OutlinedButton(
                            onClick = {
                                FloatingOverlayService.resetWebView(context)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clean_reset_webview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar WebView",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recriar WebView Limpo (Estabilidade)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onAddTag = { ScrollSearchStateBus.addCustomTag(it) }
        )
    }

    if (showPremiumRequired) {
        AlertDialog(
            onDismissRequest = { showPremiumRequired = false },
            title = { Text("Recurso Premium") },
            text = { Text("Este card está disponível no plano Premium. Os resultados também dependem da configuração e dos limites da conta SerpApi.") },
            confirmButton = {
                TextButton(onClick = { showPremiumRequired = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    selectedPremiumProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = {
                if (!providerSearchLoading) selectedPremiumProvider = null
            },
            title = { Text(provider.title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(provider.description, color = textMuted, fontSize = 12.sp)
                    Text(
                        "Tamanho preferido da imagem",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.example.model.ImageQuality.entries) { quality ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (imageQuality == quality) IgOrange else DarkSurfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { ScrollSearchStateBus.setImageQuality(quality) }
                            ) {
                                Text(
                                    quality.title,
                                    color = if (imageQuality == quality) Color.White else textColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Switch(
                            checked = isSafeSearchDisabled,
                            onCheckedChange = { ScrollSearchStateBus.setSafeSearchDisabled(it) }
                        )
                        Text(
                            "SafeSearch desligado; o provedor ainda pode filtrar resultados.",
                            color = textMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (!provider.requiresImageUrl) {
                        OutlinedTextField(
                            value = providerQuery,
                            onValueChange = { providerQuery = it },
                            label = { Text("O que deseja buscar?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = reverseImageUrl,
                            onValueChange = { reverseImageUrl = it },
                            label = { Text("URL HTTPS de uma imagem") },
                            supportingText = { Text("A imagem precisa estar hospedada e acessível publicamente.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            providerSearchLoading = true
                            providerSearchError = null
                            providerResults = emptyList()
                            coroutineScope.launch {
                                try {
                                    val result = GoogleSearchHelper.fetchPremiumProviderResults(
                                        provider = provider,
                                        query = providerQuery,
                                        imageUrl = reverseImageUrl
                                    )
                                    result.onSuccess { providerResults = it }
                                        .onFailure { providerSearchError = it.message ?: "Não foi possível concluir a busca." }
                                } finally {
                                    providerSearchLoading = false
                                }
                            }
                        },
                        enabled = !providerSearchLoading &&
                            (if (provider.requiresImageUrl) reverseImageUrl.startsWith("https://", true)
                            else providerQuery.trim().length >= 2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (providerSearchLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (providerSearchLoading) "Buscando..." else "Pesquisar")
                    }
                    providerSearchError?.let {
                        Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                    if (providerResults.isEmpty() && !providerSearchLoading && providerSearchError == null) {
                        Text("Os resultados aparecerão aqui.", color = textMuted, fontSize = 12.sp)
                    }
                    providerResults.take(30).forEach { result ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                                        selectedPremiumProvider = null
                                    } catch (_: ActivityNotFoundException) {
                                        providerSearchError = "Não há um navegador disponível para abrir este resultado."
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                result.previewUrl?.let { preview ->
                                    AsyncImage(
                                        model = preview,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (result.description.isNotBlank()) {
                                        Text(result.description, color = textMuted, fontSize = 11.sp, maxLines = 3)
                                    }
                                    if (result.source.isNotBlank()) {
                                        Text(result.source, color = TikTokCyan, fontSize = 10.sp)
                                    }
                                }
                                if (providerResults.size > 30) {
                                    Text(
                                        text = "Mostrando 30 de ${providerResults.size} resultados.",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedPremiumProvider = null },
                    enabled = !providerSearchLoading
                ) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun PremiumProviderCard(
    provider: PremiumSearchProvider,
    isLocked: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)
        ),
        modifier = Modifier
            .width(176.dp)
            .height(108.dp)
            .clickable(onClick = onClick)
            .testTag("premium_provider_${provider.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.title, color = if (isDark) Color.White else TikTokDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text(provider.description, color = if (isDark) Color.LightGray else Color(0xFF6B6C7B), fontSize = 10.sp, maxLines = 2)
            }
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado no plano Básico",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Disponível no Premium",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PermissionStatusPill(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (granted) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (granted) Color(0xFF00E676) else Color(0xFFFF5252),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: ${if (granted) "OK" else "Ativar"}",
                color = if (granted) Color(0xFF00E676) else Color(0xFFFF5252),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun launchSocialApp(context: Context, platform: SocialPlatform) {
    var launched = false
    for (pkg in platform.packageNames) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                launched = true
                break
            } catch (e: Exception) {
                android.util.Log.e("Dashboard", "Launch error: ${e.message}")
            }
        }
    }
    if (!launched) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.webFallbackUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Web fallback error: ${e.message}")
        }
    }
}

@Composable
fun SocialPlatformCard(
    platform: SocialPlatform,
    isSelected: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) DarkSurfaceElevated else Color.White
    val textPrimary = if (isDark) Color.White else TikTokDarkText
    val textMuted = if (isDark) Color(0xFFA0A0C0) else Color(0xFF6B6C7B)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, platform.primaryColor) else null,
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .testTag("social_platform_card_${platform.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Platform Gradient Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(platform.primaryColor, platform.accentColor)
                        )
                    )
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = platform.displayName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    tint = platform.primaryColor,
                                    modifier = Modifier.size(14.dp).padding(1.dp)
                                )
                            }
                        }
                    }
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.40f),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "Ativo",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = platform.tagline,
                    color = textMuted,
                    fontSize = 11.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Criadores / Amostras:",
                    color = textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Creator quick simulation chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(platform.sampleCreators) { creator ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = platform.primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelect()
                                    ScrollSearchStateBus.updateDetectedUsername(creator)
                                }
                        ) {
                            Text(
                                text = "@$creator",
                                color = platform.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Button: Abrir App e Rastrear
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = platform.primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("launch_platform_${platform.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir ${platform.displayName}",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abrir & Rastrear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
