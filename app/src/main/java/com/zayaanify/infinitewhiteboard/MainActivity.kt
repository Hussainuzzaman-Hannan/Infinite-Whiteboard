package com.zayaanify.infinitewhiteboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.WhiteboardViewModel
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.WhiteboardScreen
import com.zayaanify.infinitewhiteboard.ui.theme.InfiniteWhiteboardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfiniteWhiteboardTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val viewModel: WhiteboardViewModel = hiltViewModel()
                    WhiteboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}