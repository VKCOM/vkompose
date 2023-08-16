package com.vk.recompose.logger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object RecomposeLoggerConfig {
    var isEnabled by mutableStateOf(false)
}
