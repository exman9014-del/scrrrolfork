package com.example.ui.overlay.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overlay.FastWebViewManager
import com.example.ui.overlay.AndroidWebViewContainer
import com.example.ui.theme.DarkSurface

@Composable
fun V5BottomSheetView(
    fastWebViewManager: FastWebViewManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidWebViewContainer(
                fastWebViewManager = fastWebViewManager,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "Arraste a borda superior para redimensionar o painel",
            color = Color(0xFF707090),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
