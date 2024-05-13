package com.aydarov.compilers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Suppress("NonSkippableComposable")
@Composable
private fun TopBanner(state: TestState) {
    Text(text = "Hello ${state.name}!")
}
