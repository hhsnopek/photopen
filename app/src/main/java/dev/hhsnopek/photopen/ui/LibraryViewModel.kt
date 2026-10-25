package dev.hhsnopek.photopen.ui

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hhsnopek.photopen.data.MediaAccess
import dev.hhsnopek.photopen.data.MediaItem
import dev.hhsnopek.photopen.data.MediaLibrary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryState {
    data object Loading : LibraryState

    data object NoAccess : LibraryState

    data object Empty : LibraryState

    data class Content(
        val items: ImmutableList<MediaItem>,
    ) : LibraryState
}

class LibraryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val library = MediaLibrary(application.contentResolver)
    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var observer: ContentObserver? = null
    private var refreshJob: Job? = null

    // The chosen folder survives restarts; null means "everything".
    private val preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _selectedFolder = MutableStateFlow(preferences.getString(KEY_FOLDER, null))
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    /**
     * Called whenever the permission snapshot changes (first launch, grant,
     * revoke, or a return from the system photo picker).
     */
    fun onAccessChanged(access: MediaAccess) {
        if (!access.granted) {
            refreshJob?.cancel()
            _state.value = LibraryState.NoAccess
            return
        }
        if (observer == null) {
            observer =
                library.observe {
                    // A camera write or a delete fires this; coalesce bursts.
                    scheduleRefresh()
                }
        }
        scheduleRefresh(immediate = true)
    }

    private fun scheduleRefresh(immediate: Boolean = false) {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                if (!immediate) delay(REFRESH_DEBOUNCE_MS)
                // Keep the grid on screen while a resume-triggered reload runs;
                // blanking it would flash on every return from the picker.
                if (_state.value !is LibraryState.Content) _state.value = LibraryState.Loading
                val items = library.load()
                _state.value =
                    if (items.isEmpty()) {
                        LibraryState.Empty
                    } else {
                        LibraryState.Content(items.toPersistentList())
                    }
            }
    }

    fun selectFolder(key: String?) {
        if (_selectedFolder.value == key) return
        preferences.edit().putString(KEY_FOLDER, key).apply()
        _selectedFolder.value = key
    }

    override fun onCleared() {
        observer?.let { library.stopObserving(it) }
        observer = null
        super.onCleared()
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS = 300L
        const val PREFS = "library"
        const val KEY_FOLDER = "selected_folder"
    }
}
