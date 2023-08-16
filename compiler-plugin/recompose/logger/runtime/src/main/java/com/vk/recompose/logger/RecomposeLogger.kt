package com.vk.recompose.logger

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NoLiveLiterals
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

@NoLiveLiterals
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun RecomposeLogger(
    name: String,
    arguments: Map<String, Any?>
) {
    val ref = remember { Ref(0) }
    SideEffect { ref.count++ }

    val recomposeLog = StringBuilder()

    for ((argumentName, argumentValue) in arguments) {
        val dataDiff = remember { DataDiffHolder(argumentValue) }
        dataDiff.setNewValue(argumentValue)

        if (dataDiff.isChanged()) {
            val previous = dataDiff.previous
            val current = dataDiff.current
            recomposeLog.append("\n\t $argumentName changed: prev=[value=$previous, hashcode = ${previous.hashCode()}], current=[value=$current, hashcode = ${current.hashCode()}]")
        }
    }

    val isEnabled = RecomposeLoggerConfig.isEnabled
    if (recomposeLog.isNotEmpty() && isEnabled) {
        Log.d("RecomposeLogger", "$name recomposed ${ref.count} times. Reason for now:")
        Log.d("RecomposeLogger", "${recomposeLog}\n")
    }
}

class DataDiffHolder(current: Any?) {
    var current: Any? = current
        private set

    var previous: Any? = null
        private set

    fun isChanged() = current != previous

    fun setNewValue(newCurrent: Any?) {
        previous = current
        current = newCurrent
    }
}

data class Ref(var count: Int = 0)
