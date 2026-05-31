package com.aydarov.compilers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aydarov.compilers.theme.CompilersTheme
import com.vk.recompose.highlighter.RecomposeHighlighterConfig
import com.vk.recompose.logger.RecomposeLoggerConfig
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RecomposeHighlighterConfig.isEnabled = true
        RecomposeLoggerConfig.isEnabled = true
        setContent {
            CompilersTheme {
                Surface(color = MaterialTheme.colorScheme.background) {

                    val currentCount by produceState(initialValue = 0) {
                        while (true) {
                            delay(1000L.milliseconds)
                            value++
                        }
                    }

                    MyText(currentCount)
                }
            }
        }

    }

    @Composable
    private fun MyText(currentCount: Int, modifier: Modifier = Modifier) {
        Text("Hello World $currentCount", modifier = modifier)
    }

}