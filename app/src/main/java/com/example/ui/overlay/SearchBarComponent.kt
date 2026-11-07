package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgPink

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onSearchSubmitted: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isDark = com.example.data.ScrollSearchStateBus.isDarkTheme.collectAsState().value
    val bgColor = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)
    val textStyleColor = if (isDark) Color.White else com.example.ui.theme.TikTokDarkText

    val isPremiumActive = com.example.data.ScrollSearchStateBus.isPremiumActive.collectAsState().value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar imagens",
            tint = if (isPremiumActive) Color(0xFFFFD700) else IgPink,
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    focusManager.clearFocus()
                    onSearchSubmitted(query)
                }
                .testTag("search_icon_button")
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Mode badge inside search bar (👑 Premium vs 📦 Básico)
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isPremiumActive) Color(0x33FFD700) else Color(0x22888888),
            border = if (isPremiumActive) androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD700)) else null,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = if (isPremiumActive) "👑 PREM" else "BÁSICO",
                color = if (isPremiumActive) Color(0xFFFFD700) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = textStyleColor,
                fontSize = 12.sp
            ),
            cursorBrush = SolidColor(if (isPremiumActive) Color(0xFFFFD700) else IgPink),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearchSubmitted(query)
                }
            ),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .testTag("search_text_input"),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF8888A0),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Limpar busca",
                tint = Color.Gray,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onQueryChange("")
                    }
                    .testTag("clear_search_button")
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Dedicated search action button
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isPremiumActive) Color(0xFFFFD700) else IgPink,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    focusManager.clearFocus()
                    onSearchSubmitted(query)
                }
                .testTag("submit_search_action_button")
        ) {
            Text(
                text = "Buscar",
                color = if (isPremiumActive) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.History,
            contentDescription = "Histórico de pesquisa",
            tint = Color(0xFFA0A0C0),
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    com.example.data.ScrollSearchStateBus.setHistoryDialogOpen(true)
                }
                .padding(2.dp)
                .testTag("open_history_from_searchbar_btn")
        )
    }
}
