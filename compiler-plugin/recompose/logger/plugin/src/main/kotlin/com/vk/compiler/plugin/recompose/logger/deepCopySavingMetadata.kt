package com.vk.compiler.plugin.recompose.logger

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun <T : IrElement> T.deepCopySavingMetadata(
    initialParent: IrDeclarationParent? = null,
    symbolRemapper: DeepCopySymbolRemapper = DeepCopySymbolRemapper()
): T {
    acceptVoid(symbolRemapper)
    @Suppress("UNCHECKED_CAST")
    return transform(DeepCopySavingMetadata(symbolRemapper), null).patchDeclarationParents(initialParent) as T
}
private class DeepCopySavingMetadata(symbolRemapper: SymbolRemapper) : DeepCopyIrTreeWithSymbols(symbolRemapper) {
    override fun visitFile(declaration: IrFile): IrFile =
        super.visitFile(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitClass(declaration: IrClass): IrClass =
        super.visitClass(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        super.visitConstructor(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        super.visitSimpleFunction(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitProperty(declaration: IrProperty): IrProperty =
        super.visitProperty(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitField(declaration: IrField): IrField =
        super.visitField(declaration).apply {
            metadata = declaration.metadata
        }
    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        super.visitLocalDelegatedProperty(declaration).apply {
            metadata = declaration.metadata
        }
}