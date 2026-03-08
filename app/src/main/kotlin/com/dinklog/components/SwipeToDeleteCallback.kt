package com.dinklog.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dinklog.R

class SwipeToDeleteCallback(
    context: Context,
    private val onSwiped: (position: Int) -> Unit) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.LEFT) {
    private val background = ContextCompat.getColor(context, R.color.loss_red).toDrawable()
    private val deleteIcon = requireNotNull(
        ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
    ) { "Delete icon drawable not found" }.mutate().apply {
        colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }
    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        drawBackground(canvas, viewHolder.itemView, dX)
        drawIcon(canvas, viewHolder.itemView, dX)
    }

    private fun drawBackground(canvas: Canvas, itemView: View, dX: Float) {
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(canvas)
    }

    private fun drawIcon(canvas: Canvas, itemView: View, dX: Float) {
        val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight
        val iconRight = itemView.right - iconMargin
        val iconLeft = iconRight - deleteIcon.intrinsicWidth

        val swipeThreshold = deleteIcon.intrinsicWidth + iconMargin * 2
        if (-dX < swipeThreshold) return

        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteIcon.draw(canvas)
    }
}