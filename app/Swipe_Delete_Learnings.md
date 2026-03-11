# Swipe-to-Delete Learnings (CRUD: Delete)

What we built, how it works, and the Kotlin/Android concepts behind every decision.

---

## What This PR Adds

Checkpoint 4 requires a full **CRUD** implementation. The Delete operation was the missing piece. This PR adds swipe-to-delete on the History list — the user swipes a match card left, a red background with a trash icon appears, and the match is removed from Room. An **Undo** Snackbar gives a 5-second window to reverse the deletion.

```
User swipes left on a card
        ↓
SwipeToDeleteCallback.onSwiped()
        ↓
History.kt captures match from adapter list, calls viewModel.deleteMatch()
        ↓
HistoryViewModel.deleteMatch() launches coroutine → matchRepository.deleteMatch()
        ↓
MatchRepository.deleteMatch() calls matchDao.delete() on Dispatchers.IO
        ↓
Room deletes the row from SQLite
        ↓
MatchDao.getMatchesByUser() Flow auto-emits the updated list
        ↓
History.kt collector updates the RecyclerView
```

---

## New Concept: ItemTouchHelper

`ItemTouchHelper` is an Android utility class that detects swipe and drag gestures on a `RecyclerView`. You attach it to a RecyclerView and it handles all the touch event math for you.

You interact with it by subclassing `ItemTouchHelper.SimpleCallback`:

```kotlin
class SwipeToDeleteCallback(
    context: Context,
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)
```

The two constructor parameters tell it what gestures to allow:
- `(dragDirs) = 0` — drag-to-reorder is disabled entirely
- `(swipeDirs) = ItemTouchHelper.LEFT` — only left swipes trigger deletion

`ItemTouchHelper.RIGHT` would enable right swipes too. We deliberately only allow left because it's the universally understood "delete" direction on Android.

---

## The Two Methods You Must Override

### `onMove()` — For Drag-to-Reorder

```kotlin
override fun onMove(...): Boolean = false
```

We don't support reordering, so this always returns `false`. It's required by the interface but has nothing to do.

### `onSwiped()` — The Actual Delete Trigger

```kotlin
override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    onSwiped(viewHolder.adapterPosition)
}
```

Called when the user completes a swipe. We grab the position from the `ViewHolder` and fire the lambda that was passed into our callback. The Fragment gets the position back and can look up the actual `Match` object.

---

## Drawing the Swipe Background

Android doesn't draw anything behind a card as it swipes — that's your job. We override `onChildDraw()` to paint the red background and trash icon:

```kotlin
override fun onChildDraw(
    canvas: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,     // how far the view has been dragged (negative = left swipe)
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
) {
    super.onChildDraw(...)  // MUST call super first — it moves the card view
    drawBackground(canvas, viewHolder.itemView, dX)
    drawIcon(canvas, viewHolder.itemView, dX)
}
```

`dX` is negative for a left swipe. When the user has swiped 100px left, `dX = -100`. We use this to size the red background so it only fills the revealed area.

### Drawing the Background

```kotlin
private fun drawBackground(canvas: Canvas, itemView: View, dX: Float) {
    val swipedFully = dX == 0f
    if (swipedFully) return
    
    background.setBounds(
        itemView.right + dX.toInt(),  // left edge grows as dX gets more negative
        itemView.top,
        itemView.right,               // right edge is always the card's right side
        itemView.bottom
    )
    background.draw(canvas)
}
```

We set the background's bounds to exactly the revealed area and draw it onto the canvas.

### Drawing the Icon

```kotlin
private fun drawIcon(canvas: Canvas, itemView: View, dX: Float) {
    val swipeThreshold = deleteIcon.intrinsicWidth + iconMargin * 2
    if (-dX < swipeThreshold) return   // don't show icon until enough space exists
    ...
    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
    deleteIcon.draw(canvas)
}
```

The icon only appears once the user has swiped far enough that it won't be clipped. The threshold is `iconWidth + margin * 2` — enough room for the icon plus breathing space on both sides.

---

## New Concept: `.toDrawable()` KTX Extension

```kotlin
private val background = ContextCompat.getColor(context, R.color.loss_red).toDrawable()
```

`ContextCompat.getColor()` returns an `Int` (a packed ARGB color value). The KTX extension `.toDrawable()` converts that `Int` directly into a `ColorDrawable` — a drawable that simply fills a rectangle with that color.

This is equivalent to `ColorDrawable(ContextCompat.getColor(context, R.color.loss_red))` but more idiomatic Kotlin. We reuse `loss_red` from `colors.xml` — the same red already used for the "L" badge on match cards. This is Rule #2 (DRY) — one color defined once.

---

## New Concept: PorterDuffColorFilter

The system trash icon drawable is black by default. We tint it white:

```kotlin
private val deleteIcon = requireNotNull(
    ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
) { "Delete icon drawable not found" }.mutate().apply {
    colorFilter = PorterDuffColorFilter(
        ContextCompat.getColor(context, R.color.white),
        PorterDuff.Mode.SRC_IN
    )
}
```

`PorterDuffColorFilter` applies a color on top of a drawable using a compositing mode. `SRC_IN` means: "show the source color (white), but only where the destination (the icon) is non-transparent." This is the standard way to tint a drawable programmatically.

`.mutate()` before `.apply {}` is required — drawables loaded from resources are cached and shared. Calling `.mutate()` gives you a private copy so your color filter doesn't affect the drawable anywhere else it might be used.

---

## New Concept: `requireNotNull()` with a Message

```kotlin
private val deleteIcon = requireNotNull(
    ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
) { "Delete icon drawable not found" }
```

`ContextCompat.getDrawable()` returns a nullable `Drawable?`. Instead of using `!!` (forbidden by kotlin-rules #9), we use `requireNotNull()` with an explanatory message. If it ever returns null, the app throws with a clear message instead of a cryptic NPE.

---

## New Concept: `dimens.xml`

```xml
<!-- res/values/dimens.xml -->
<dimen name="swipe_icon_margin">24dp</dimen>
```

A dimension resource converts `dp` (density-independent pixels) to actual pixels at runtime based on the device's screen density. We access it as:

```kotlin
private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)
```

This returns the correct pixel value for any screen — `24dp` might be 48px on a high-density screen and 36px on a medium-density one. We store it in `dimens.xml` rather than hardcoding `24` (Rule #4 — no magic numbers).

The resource type is always `R.dimen` and the XML tag is always `<dimen>` — these are fixed by Android. Only the filename is flexible (ours is `dimens.xml`).

---

## ViewModel Changes: `deleteMatch()` and `undoDelete()`

### `deleteMatch()`

```kotlin
fun deleteMatch(match: Match) {
    viewModelScope.launch {
        matchRepository.deleteMatch(match)
    }
}
```

The ViewModel launches a coroutine in `viewModelScope` (Rule #7) and delegates to the repository. No return value needed — the `Flow` in `matches` automatically emits the updated list after deletion.

### `undoDelete()`

```kotlin
fun undoDelete(match: Match) {
    viewModelScope.launch {
        matchRepository.addMatch(
            userId = match.userId,
            opponent1Name = match.opponent1Name,
            opponent2Name = match.opponent2Name,
            partnerName = match.partnerName,
            teamScore = match.teamScore,
            opponentScore = match.opponentScore,
            locationName = match.locationName,
            notes = match.notes
        )
    }
}
```

Undo is implemented by re-inserting the deleted match with all its original data. Room assigns a new `matchId` (since the original row is gone), but all the match content is preserved. This is simpler and more reliable than trying to "cancel" a deletion.

---

## Fragment Changes: `setupSwipeToDelete()` and `showUndoSnackbar()`

### `setupSwipeToDelete()`

```kotlin
private fun setupSwipeToDelete() {
    val callback = SwipeToDeleteCallback(requireContext()) { position ->
        val match = matchAdapter.currentList[position]
        viewModel.deleteMatch(match)
        showUndoSnackbar(match)
    }
    ItemTouchHelper(callback).attachToRecyclerView(binding.matchesList)
}
```

We create the callback, pass a lambda that captures the match at the swiped position, then attach it to the RecyclerView. `currentList` is provided by `ListAdapter` and always reflects the latest submitted list.

**Why capture the match before calling `deleteMatch()`?**
After deletion, Room emits a new list without that match. By the time the Snackbar's undo button is tapped, the adapter's `currentList` no longer contains the deleted match. We capture it first so `showUndoSnackbar()` has the full object to pass to `undoDelete()`.

### `showUndoSnackbar()`

```kotlin
private fun showUndoSnackbar(deletedMatch: Match) {
    Snackbar.make(binding.root, R.string.match_deleted, Snackbar.LENGTH_LONG)
        .setAction(R.string.action_undo) { viewModel.undoDelete(deletedMatch) }
        .show()
}
```

`Snackbar` is a Material Design component that slides up from the bottom of the screen. `LENGTH_LONG` gives the user ~5 seconds before it auto-dismisses. `setAction()` adds the "Undo" button. Both string references (`R.string.match_deleted`, `R.string.action_undo`) follow Rule #4 — no hardcoded strings.

---

## Why the Flow Auto-Updates the List

We didn't write any code to refresh the RecyclerView after deletion. It just happens. Here's why:

```
MatchDao.getMatchesByUser() returns Flow<List<Match>>
         ↓
MatchRepository.getMatchesForUser() passes it through unchanged
         ↓
HistoryViewModel.matches is a StateFlow built from that Flow
         ↓
History.kt collects matches and calls matchAdapter.submitList()
```

Room's `Flow` is a **live query** — it observes the database table. Any INSERT or DELETE on the `matches` table triggers a new emission with the updated list. The chain is fully reactive: delete a row → Flow emits → Fragment collects → adapter diffs → RecyclerView updates. Zero manual refresh logic needed.

---

## Kotlin-Rules Compliance

| Rule | How We Applied It |
|---|---|
| **#1 SRP** | `SwipeToDeleteCallback` has one job: handle the swipe gesture and drawing. Deletion logic stays in ViewModel. |
| **#2 DRY** | Reused `R.color.loss_red` for swipe background — same red as the loss badge. No duplicate color defined. |
| **#3 Function Size** | `onChildDraw()` delegates to `drawBackground()` and `drawIcon()` — each under 10 lines. |
| **#4 No Magic Strings/Numbers** | `R.string.match_deleted`, `R.string.action_undo`, `R.dimen.swipe_icon_margin` — no hardcoded values anywhere. |
| **#5 MVVM** | Fragment handles gesture → ViewModel handles delete/undo → Repository handles DB. No layer crosses another. |
| **#6 File Organization** | `SwipeToDeleteCallback` lives in `components/` — it's a reusable component, not page-specific logic. |
| **#7 Idioms** | `.toDrawable()` KTX extension, `requireNotNull()` over `!!`, `viewModelScope.launch`, expression body on `onMove()`. |
| **#8 Naming** | `SwipeToDeleteCallback` (PascalCase), `drawBackground`/`drawIcon` (camelCase), `TAG` (SCREAMING_SNAKE). |
| **#9 Forbidden** | No `!!`, no `runBlocking`, no `lateinit var`. |

---

## Files Changed / Created

```
New files:
├── components/SwipeToDeleteCallback.kt   ← Gesture handler + drawing logic
└── res/values/dimens.xml                 ← swipe_icon_margin = 24dp

Changed files:
├── pages/HistoryViewModel.kt   ← Added deleteMatch() and undoDelete()
├── pages/History.kt            ← Added setupSwipeToDelete() and showUndoSnackbar()
└── res/values/strings.xml      ← Added match_deleted and action_undo
```

---

## What's Next

With swipe-to-delete merged, the remaining Checkpoint 4 CRUD operations are:

- **Update** — Edit Match screen with Safe Args navigation
