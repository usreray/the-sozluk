package com.thesozluk.app.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Taps on the bottom bar item of the tab that is already open, by route. Each tab reacts in
 * its own way: lists go back to the top, the search tab opens its search field.
 */
object TabReselect {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun reselect(route: String) {
        _events.tryEmit(route)
    }
}
