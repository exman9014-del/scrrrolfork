package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScrollSearchStateBus
import com.example.model.UserPlan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokDarkText
import com.example.ui.theme.TikTokRed
import com.example.util.GoogleSearchHelper

// Gold accent palette for Premium card
private val GoldPrimary = Color(0xFFFFD700)
private val GoldSecondary = Color(0xFFFFA000)
private val GoldBackgroundTint = Color(0x18FFD700)
private val DarkCardBg = Color(0xFF14141E)

@Composable
fun PlanComparisonCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by ScrollSearchStateBus.isDarkTheme.collectAsState()
    val currentPlan by ScrollSearchStateBus.currentPlan.collectAsState()
    val isPremiumActive by ScrollSearchStateBus.isPremiumActive.collectAsState()
    val customSearchApiUrl by ScrollSearchStateBus.customSearchApiUrl.collectAsState()
    val effectiveSearchApiUrl = ScrollSearchStateBus.getEffectiveSearchApiUrl()
    val isApiConfigured = GoogleSearchHelper.isSearchApiConfigured(effectiveSearchApiUrl)
    val isLoadingPhotos by ScrollSearchStateBus.isLoadingPhotos.collectAsState()
    val photos by ScrollSearchStateBus.photos.collectAsState()
    val lastCustomSearchError by ScrollSearchStateBus.lastCustomSearchError.collectAsState()

    var showApiSettings by remember { mutableStateOf(false) }
    var searchApiUrlInput by remember(customSearchApiUrl) {
        mutableStateOf(customSearchApiUrl.ifBlank { GoogleSearchHelper.getSearchApiUrl() })
    }

    val textColor = if (isDark) Color.White else TikTokDarkText
    val textMuted = if (isDark) Color(0xFFA0A0C0) else Color(0xFF6B6C7B)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_comparison_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with TikTokLens branding & title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "TL" Neon logo badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(listOf(TikTokRed, TikTokCyan)),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TL",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TikTok",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = "Lens",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TikTokCyan
                        )
                    }
                    Text(
                        text = "Busque. Encontre. Use.",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Subtitle: "Escolha seu Plano" / "Compare e escolha o plano ideal para você."
            Text(
                text = "Escolha seu Plano",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compare e escolha o ",
                    fontSize = 12.sp,
                    color = textMuted
                )
                Text(
                    text = "plano ideal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TikTokRed
                )
                Text(
                    text = " para você.",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Two Plans Side-by-Side (Plano Básico vs Plano Premium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ==========================================
                // PLANO BÁSICO (FREE)
                // ==========================================
                val isBasicSelected = !isPremiumActive
                val basicBorderColor by animateColorAsState(
                    targetValue = if (isBasicSelected) TikTokCyan else Color.White.copy(alpha = 0.12f),
                    label = "basic_border"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) DarkCardBg else Color(0xFFF7F7FA),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isBasicSelected) 1.5.dp else 1.dp,
                        color = basicBorderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { ScrollSearchStateBus.setPlan(UserPlan.BASIC) }
                        .testTag("basic_plan_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon in rounded box
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF222232) else Color(0xFFE8E8F0))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Plano Básico",
                                tint = if (isDark) Color.White.copy(alpha = 0.85f) else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Plano Básico",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "(Free)",
                            fontSize = 11.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Features List
                        FeatureItem(
                            icon = Icons.Default.Language,
                            text = "Busca via WebView (Google, Bing)",
                            iconTint = Color.Gray,
                            textColor = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Visibility,
                            text = "Interface visual da página de busca",
                            iconTint = Color.Gray,
                            textColor = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Warning,
                            text = "Pode ter captcha ou bloqueios",
                            iconTint = Color(0xFFFF7043),
                            textColor = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Image,
                            text = "Imagens em qualidade média",
                            iconTint = Color.Gray,
                            textColor = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Speed,
                            text = "Mais lento e pesado",
                            iconTint = Color(0xFFFF8A65),
                            textColor = textColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom Pill: "Grátis / para sempre"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBasicSelected) TikTokCyan.copy(alpha = 0.20f) else (if (isDark) Color(0xFF1E1E2C) else Color(0xFFE4E4EE)),
                            border = if (isBasicSelected) androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isBasicSelected) "Plano Ativo" else "Grátis",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBasicSelected) TikTokCyan else textColor
                                )
                                Text(
                                    text = "para sempre",
                                    fontSize = 9.sp,
                                    color = textMuted
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // PLANO PREMIUM (RECOMENDADO)
                // ==========================================
                val isPremiumSelected = isPremiumActive
                val premiumBorderColor by animateColorAsState(
                    targetValue = if (isPremiumSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.45f),
                    label = "premium_border"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) DarkCardBg else Color(0xFF1A1A28),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isPremiumSelected) 2.dp else 1.5.dp,
                        color = premiumBorderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GoldBackgroundTint,
                                    Color.Transparent
                                )
                            )
                        )
                        .clickable { ScrollSearchStateBus.setPlan(UserPlan.PREMIUM) }
                        .testTag("premium_plan_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge: "👑 RECOMENDADO"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x33FFD700),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👑",
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "RECOMENDADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Sparkles Icon ✨
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Premium Sparkle",
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Plano Premium",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Mais rápido, estável e poderoso",
                            fontSize = 9.sp,
                            color = Color(0xFFFFECB3),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = GoldPrimary.copy(alpha = 0.20f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Features List with Gold Icons
                        FeatureItem(
                            icon = Icons.Default.Search,
                            text = "Busca de imagens por API em servidor dedicado",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Image,
                            text = "Filtro para imagens grandes, quando disponível",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Security,
                            text = "Sem captcha ou bloqueios",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.Description,
                            text = "Resultados mais limpos e organizados",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.RocketLaunch,
                            text = "Melhor performance e menos consumo de dados",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem(
                            icon = Icons.Default.WorkspacePremium,
                            text = "Acesso prioritário.",
                            iconTint = GoldPrimary,
                            textColor = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom Pill: "Plano Ativo" or "Ativar Premium"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPremiumSelected) GoldPrimary else Color(0x33FFD700),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isPremiumSelected) "✓ Ativo" else "Experimentar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isPremiumSelected) Color.Black else GoldPrimary
                                )
                                Text(
                                    text = "Google API",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPremiumSelected) Color.Black.copy(alpha = 0.75f) else Color(0xFFFFECB3)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================================
            // ALTERNAR PLANO (SWITCH / TOGGLE ROW)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF161825) else Color(0xFFEFEFF6),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_toggle_container")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Label: Plano Básico
                    Text(
                        text = "Plano Básico",
                        fontSize = 13.sp,
                        fontWeight = if (!isPremiumActive) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (!isPremiumActive) textColor else textMuted,
                        modifier = Modifier.clickable { ScrollSearchStateBus.setPlan(UserPlan.BASIC) }
                    )

                    // Center: ⚡ Alternar plano
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { ScrollSearchStateBus.togglePlan() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Alternar",
                            tint = TikTokCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Alternar plano",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TikTokCyan
                        )
                    }

                    // Right: Plano Premium + Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plano Premium",
                            fontSize = 13.sp,
                            fontWeight = if (isPremiumActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isPremiumActive) GoldPrimary else textMuted,
                            modifier = Modifier.clickable { ScrollSearchStateBus.setPlan(UserPlan.PREMIUM) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isPremiumActive,
                            onCheckedChange = { ScrollSearchStateBus.setPremiumActive(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TikTokCyan,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = if (isDark) DarkSurfaceElevated else Color(0xFFD4D4DF)
                            ),
                            modifier = Modifier.testTag("premium_toggle_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle with shield: "Você pode alterar seu plano a qualquer momento."
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Você pode alterar seu plano a qualquer momento.",
                    fontSize = 11.sp,
                    color = textMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // PRIMARY ACTION BUTTON (GRADIENT)
            // =========================================================================
            Button(
                onClick = {
                    if (isPremiumActive) {
                        Toast.makeText(
                            context,
                            "Plano Premium ativo! Busca de imagens no backend ativada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        ScrollSearchStateBus.setPlan(UserPlan.PREMIUM)
                        Toast.makeText(
                            context,
                            "Plano Premium ativado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isPremiumActive) {
                                listOf(TikTokRed, TikTokCyan)
                            } else {
                                listOf(TikTokRed, Color(0xFFFF5252))
                            }
                        )
                    )
                    .testTag("continue_plan_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPremiumActive) "Continuar com Premium" else "Ativar Plano Premium",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continuar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Security note: "🔒 Pagamento seguro • Cancelamento fácil"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = textMuted.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Pagamento seguro • Cancelamento fácil",
                    fontSize = 11.sp,
                    color = textMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // API Details Expandable Bar (Shows search backend status)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showApiSettings = !showApiSettings }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Config",
                            tint = if (isApiConfigured) Color(0xFF00E676) else GoldSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApiConfigured) "Buscas SerpApi: backend configurado" else "Configurar backend de buscas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                    Text(
                        text = if (showApiSettings) "Ocultar ▲" else "Configurar ▼",
                        fontSize = 11.sp,
                        color = TikTokCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = showApiSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = if (isApiConfigured) "Backend de busca: configurado" else "Backend de busca: não configurado",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = searchApiUrlInput,
                        onValueChange = { searchApiUrlInput = it },
                        label = { Text("URL HTTPS do backend Vercel") },
                        placeholder = { Text("https://seu-projeto.vercel.app") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_api_url_field")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A chave SerpApi fica somente nas variáveis privadas do Vercel, nunca no APK.",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = textMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ScrollSearchStateBus.setCustomSearchApiUrl(searchApiUrlInput)
                                Toast.makeText(context, "URL do backend aplicada nesta sessão.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aplicar Configuração", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                ScrollSearchStateBus.setPremiumActive(true)
                                Toast.makeText(context, "Testando busca via backend...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isLoadingPhotos) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("Testar API", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (lastCustomSearchError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x33FF9800),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Status do backend de busca",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFE0B2),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lastCustomSearchError ?: "",
                                    color = Color(0xFFFFE0B2),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (photos.isNotEmpty() && isPremiumActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ ${photos.size} resultados reais recebidos do backend de busca",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            lineHeight = 14.sp
        )
    }
}
