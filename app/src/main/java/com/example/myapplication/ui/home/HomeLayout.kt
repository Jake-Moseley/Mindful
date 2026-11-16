package com.example.myapplication.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel

/** Every widget that can appear on the home screen. */
enum class HomeWidget(val label: String, val icon: ImageVector, val canBeSmall: Boolean = true) {
    INSIGHT("Weekly Story", Icons.Default.AutoAwesome, canBeSmall = false), // too much text for half width
    MOOD("Today's Mood", Icons.Default.Face),
    GOALS("Daily Goals", Icons.Default.CheckCircle),
    STREAK("Goal Streak", Icons.Default.LocalFireDepartment),
    SLEEP("Sleep", Icons.Default.Bedtime),
    WATER("Hydration", Icons.Default.WaterDrop),
    RESOURCES("Professional Help", Icons.Default.Campaign),
    JOURNAL("Journal", Icons.Default.EditNote);

    val canResize: Boolean get() = canBeSmall
}

/** One widget's place on the home screen. Hidden widgets keep their spot so they come back where they were. */
data class WidgetSlot(val widget: HomeWidget, val wide: Boolean, val visible: Boolean = true)

/**
 * Which home widgets show, in what order and at what size. Saved in SharedPreferences.
 * Shared by the home screen and Settings (created once in MindfulApp).
 */
class HomeLayoutViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Every widget in display order, hidden ones included. */
    var slots by mutableStateOf(load())
        private set

    /** True while the home screen is in "jiggle" edit mode. */
    var isEditing by mutableStateOf(false)

    val visibleSlots: List<WidgetSlot> get() = slots.filter { it.visible }
    val hiddenWidgets: List<HomeWidget> get() = slots.filter { !it.visible }.map { it.widget }

    fun remove(widget: HomeWidget) = change(widget) { it.copy(visible = false) }

    /** Shows a hidden widget again, at the bottom of the home screen. */
    fun add(widget: HomeWidget) {
        val slot = slots.firstOrNull { it.widget == widget } ?: return
        save(slots - slot + slot.copy(visible = true))
    }

    fun toggleSize(widget: HomeWidget) {
        if (widget.canResize) change(widget) { it.copy(wide = !it.wide) }
    }

    /** Swaps a widget with the visible widget before it (up) or after it (down). */
    fun move(widget: HomeWidget, up: Boolean) {
        val from = slots.indexOfFirst { it.widget == widget }
        if (from == -1) return
        val range = if (up) (from - 1 downTo 0) else (from + 1 until slots.size)
        val to = range.firstOrNull { slots[it].visible } ?: return
        save(slots.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Moves [widget] into [target]'s spot, shifting the widgets in between (used by drag-to-move). */
    fun moveTo(widget: HomeWidget, target: HomeWidget) {
        val from = slots.indexOfFirst { it.widget == widget }
        val to = slots.indexOfFirst { it.widget == target }
        if (from == -1 || to == -1 || from == to) return
        save(slots.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun reset() = save(DEFAULT)

    private fun change(widget: HomeWidget, transform: (WidgetSlot) -> WidgetSlot) {
        save(slots.map { if (it.widget == widget) transform(it) else it })
    }

    private fun save(newSlots: List<WidgetSlot>) {
        slots = newSlots
        val encoded = newSlots.joinToString(",") { "${it.widget.name}:${if (it.wide) "W" else "S"}:${if (it.visible) 1 else 0}" }
        prefs.edit().putString(KEY_LAYOUT, encoded).apply()
    }

    /** Reads the saved layout. Widgets added in a later app version are appended with their defaults. */
    private fun load(): List<WidgetSlot> {
        val saved = prefs.getString(KEY_LAYOUT, null) ?: return DEFAULT
        val parsed = saved.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            val widget = HomeWidget.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return@mapNotNull null
            WidgetSlot(
                widget = widget,
                wide = parts.getOrNull(1) == "W" || !widget.canBeSmall,
                visible = parts.getOrNull(2) != "0"
            )
        }.distinctBy { it.widget }
        val missing = DEFAULT.filter { d -> parsed.none { it.widget == d.widget } }
        return parsed + missing
    }

    companion object {
        private const val PREFS_NAME = "mindful_home_layout"
        private const val KEY_LAYOUT = "layout"

        /** The original layout: Weekly Story across the top, everything else in two columns. */
        val DEFAULT: List<WidgetSlot> = HomeWidget.entries.map { WidgetSlot(it, wide = !it.canBeSmall) }
    }
}
