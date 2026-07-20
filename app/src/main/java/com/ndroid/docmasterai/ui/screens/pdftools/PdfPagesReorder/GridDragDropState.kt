package com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Drives drag-to-reorder for a LazyVerticalGrid without any external library.
 *
 * Usage: attach [onDragStart]/[onDrag]/[onDragInterrupted] to a
 * `detectDragGesturesAfterLongPress` pointerInput on the grid, and check [draggingItemIndex] /
 * [draggingItemOffset] per item to decide whether to apply a manual graphicsLayer translation
 * (dragged item) or let `Modifier.animateItem()` handle the reflow (everything else).
 */
class GridDragDropState internal constructor(
    private val gridState: LazyGridState,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    /** Offset (in px) the dragged item should be visually translated by, beyond its current grid slot. */
    var draggingItemOffset by mutableStateOf(Offset.Zero)
        private set

    // Position of the dragged item's current grid slot, captured fresh after every swap.
    private var currentSlotOffset: Offset = Offset.Zero

    fun onDragStart(pointerPosition: Offset) {
        val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            pointerPosition.x.toInt() in item.offsetXRange && pointerPosition.y.toInt() in item.offsetYRange
        } ?: return

        draggingItemIndex = item.index
        currentSlotOffset = Offset(item.offset.x.toFloat(), item.offset.y.toFloat())
        draggingItemOffset = Offset.Zero
    }

    fun onDrag(dragAmount: Offset) {
        val draggingIndex = draggingItemIndex ?: return
        draggingItemOffset += dragAmount

        val draggingItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingIndex } ?: return
        val absoluteTopLeft = currentSlotOffset + draggingItemOffset
        val center = Offset(
            x = absoluteTopLeft.x + draggingItem.size.width / 2f,
            y = absoluteTopLeft.y + draggingItem.size.height / 2f
        )

        val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != draggingIndex &&
                center.x.toInt() in item.offsetXRange &&
                center.y.toInt() in item.offsetYRange
        } ?: return

        onMove(draggingIndex, targetItem.index)

        // The dragged item now lives at targetItem's index. Its grid *slot* position (a fixed
        // physical spot in the layout) becomes targetItem's old offset. Re-anchor so the item
        // keeps following the finger with no visual jump.
        val newSlotOffset = Offset(targetItem.offset.x.toFloat(), targetItem.offset.y.toFloat())
        draggingItemOffset = absoluteTopLeft - newSlotOffset
        currentSlotOffset = newSlotOffset
        draggingItemIndex = targetItem.index
    }

    fun onDragInterrupted() {
        draggingItemIndex = null
        draggingItemOffset = Offset.Zero
    }

    private val LazyGridItemInfo.offsetXRange: IntRange get() = offset.x..(offset.x + size.width)
    private val LazyGridItemInfo.offsetYRange: IntRange get() = offset.y..(offset.y + size.height)
}

@Composable
fun rememberGridDragDropState(
    gridState: LazyGridState,
    onMove: (from: Int, to: Int) -> Unit
): GridDragDropState {
    return remember(gridState) { GridDragDropState(gridState, onMove) }
}
