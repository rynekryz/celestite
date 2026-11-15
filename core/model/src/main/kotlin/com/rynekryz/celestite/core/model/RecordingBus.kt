package com.rynekryz.celestite.core.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingBus {
    private val mutable = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = mutable.asStateFlow()
    var lastSavedName: String? = null
        private set

    fun post(state: RecordingState) {
        if (state == RecordingState.Idle) lastSavedName = null
        mutable.value = state
    }

    fun markSaved(name: String) {
        lastSavedName = name
        mutable.value = RecordingState.Idle
    }
}
