# PDF Page Reorder Screen (Kotlin + Jetpack Compose)

A single screen: pick a PDF, **long-press and drag** any page thumbnail, drop it between two
others, and the rest of the grid automatically reflows into the new sequence. Save writes a new
PDF with the pages in that final order.

## Files

| File | Purpose |
|---|---|
| `PdfPageItem.kt` | One page = stable `id` (its **original** index in the source PDF) + its rendered thumbnail |
| `PdfReorderHelper.kt` | PDFBox: render all thumbnails once, and write a new PDF from a given page order |
| `PdfReorderViewModel.kt` | Loads PDF, exposes the live page order as a `SnapshotStateList`, saves the result |
| `GridDragDropState.kt` | The drag-to-reorder engine — no external library, ~70 lines |
| `PdfPageReorderScreen.kt` | The screen itself |

## How the drag/auto-reflow works

1. **`viewModel.pages`** is a `SnapshotStateList<PdfPageItem>` — its current order *is* the
   current page order. Reordering is just `pages.add(to, pages.removeAt(from))`.
2. **`GridDragDropState`** listens to a single `detectDragGesturesAfterLongPress` on the whole
   grid (not per-item):
   - `onDragStart` finds which visible cell the long-press landed on using
     `LazyGridState.layoutInfo.visibleItemsInfo` (each item's on-screen rect).
   - `onDrag` accumulates the raw pointer delta, computes the dragged item's current visual
     center, and checks whether that center has entered a *different* cell's rect. If so, it
     calls `onMove(from, to)` — which is wired straight to `viewModel.movePage` — and re-anchors
     its internal offset to the new slot so the item keeps following the finger with no jump.
3. **Everyone else animates automatically.** Every non-dragged cell has `Modifier.animateItem()`
   (Compose Foundation's built-in reflow animation for lazy layouts), so when the underlying list
   mutates, the other pages smoothly slide into their new positions — that's the "auto adjustable"
   behavior you asked for, and it's free once the list mutates in place.
4. The item actually being dragged gets a manual `graphicsLayer` translation (`draggingItemOffset`)
   instead of `animateItem()`, plus a slight scale-up and shadow so it visually "lifts" while you
   drag it.

## Setup

Same dependencies as the page-splitter screen (`pdfbox-android`, Compose BOM, activity-compose,
lifecycle-viewmodel-compose) — reuse `build.gradle.kts.snippet` from that delivery if you already
added it. `Modifier.animateItem()` requires:

```
implementation("androidx.compose.foundation:foundation:1.7.0") // or newer
```

If you're on an older Compose Foundation version, swap `Modifier.animateItem()` for the older
`Modifier.animateItemPlacement()` (same purpose, deprecated but functional).

**FileProvider**: reuse the same provider entry / `file_paths.xml` as the splitter screen. If this
is a standalone module, just add another `<external-files-path>` entry pointing at
`reorder_output/` to that same `file_paths.xml`:

```xml
<external-files-path name="reorder_output" path="reorder_output/" />
```

**Use the screen**:
```kotlin
setContent {
    MaterialTheme {
        PdfPageReorderScreen()
    }
}
```

## Notes / things to tune for production

- **Auto-scroll while dragging near the top/bottom edge is not implemented.** For PDFs with more
  pages than fit on one screen, add a check in `onDrag`: if the pointer's absolute Y is within
  ~40dp of the grid's top/bottom bounds, call `gridState.scrollBy(...)` in a coroutine each frame
  while the drag continues near that edge. Happy to add this if you need it.
- Long-press-to-start-drag (`detectDragGesturesAfterLongPress`) is the standard pattern so normal
  taps/scrolls aren't accidentally hijacked. If you'd rather drag start immediately on touch-down
  (e.g. via a dedicated drag-handle icon on each cell instead of the whole thumbnail), that's a
  straightforward swap to `detectDragGestures` scoped to just the handle.
- Thumbnails are rendered once at load time; reordering never re-renders anything, it only
  reshuffles the in-memory list — so dragging stays fast even on longer documents.
