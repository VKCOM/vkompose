package com.aydarov.compilers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aydarov.compilers.theme.CompilersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompilersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().then(Modifier),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                    }
                }
            }
        }
    }
}