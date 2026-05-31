package androidx.compose.compiler.plugins.kotlin.lower.vk

import androidx.compose.compiler.plugins.kotlin.vk.ComposeFqNames.InternalPackage
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.packageFqName

val IrConstructorCall.annotationClass
    get() = type.classOrNull

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrType.isSyntheticComposableFunction() =
    classOrNull?.owner?.let {
        it.name.asString().startsWith("ComposableFunction") &&
                it.packageFqName == InternalPackage
    } ?: false