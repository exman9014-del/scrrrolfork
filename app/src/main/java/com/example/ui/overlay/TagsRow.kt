package com.example.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScrollSearchStateBus
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgPink
import kotlinx.coroutines.delay

@Composable
fun TagsRow(
    tags: List<String>,
    selectedTag: String,
    onTagSelected: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddingTag by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isAddingTag) {
        if (isAddingTag) {
            ScrollSearchStateBus.setSearchFocused(true)
            delay(100)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Ignore focus request failure
            }
        } else {
            ScrollSearchStateBus.setSearchFocused(false)
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAddingTag) {
            // Inline tag editor with active keyboard focus
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, IgPink),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(30.dp)
                    .testTag("inline_tag_editor")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(IgPink),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newTagInput.isNotBlank()) {
                                    onAddCustomTag(newTagInput)
                                    newTagInput = ""
                                    isAddingTag = false
                                    ScrollSearchStateBus.setSearchFocused(false)
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .width(100.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    ScrollSearchStateBus.setSearchFocused(true)
                                }
                            }
                            .testTag("inline_new_tag_input"),
                        decorationBox = { innerTextField ->
                            if (newTagInput.isEmpty()) {
                                Text("Nome da tag...", color = Color.Gray, fontSize = 11.sp)
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Confirm checkmark button
                    IconButton(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                onAddCustomTag(newTagInput)
                                newTagInput = ""
                                isAddingTag = false
                                ScrollSearchStateBus.setSearchFocused(false)
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(20.dp).testTag("confirm_inline_tag_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmar tag",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Cancel button
                    IconButton(
                        onClick = {
                            newTagInput = ""
                            isAddingTag = false
                            ScrollSearchStateBus.setSearchFocused(false)
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(20.dp).testTag("cancel_inline_tag_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            // "+" Add Tag Button
            Surface(
                shape = CircleShape,
                color = IgPink,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .clickable { isAddingTag = true }
                    .testTag("add_tag_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Tag",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Tag",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of tags
        LazyRow(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tags) { tag ->
                val isDark = ScrollSearchStateBus.isDarkTheme.collectAsState().value
                val isSelected = tag.equals(selectedTag, ignoreCase = true)
                val unselectedBg = if (isDark) DarkSurfaceElevated else Color(0xFFF1F1F4)
                val unselectedText = if (isDark) Color(0xFFC0C0D8) else Color(0xFF4A4A60)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) IgPink.copy(alpha = 0.25f) else unselectedBg,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.dp, IgPink)
                    } else null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTagSelected(tag) }
                        .testTag("tag_item_${tag}")
                ) {
                    Text(
                        text = if (tag == "tudo") "Tudo" else "#$tag",
                        color = if (isSelected) (if (isDark) Color.White else IgPink) else unselectedText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
