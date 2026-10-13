package com.louaysadok.redgif.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louaysadok.redgif.data.local.FavoritesRepository
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.data.net.MediaKind
import com.louaysadok.redgif.data.net.Order
import com.louaysadok.redgif.data.net.Page
import com.louaysadok.redgif.data.net.RedGifsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedKind { BROWSE, SEARCH, CREATOR }

/** Describes one feed. The key identifies the ViewModel, so changing any field starts a fresh feed. */
data class FeedSource(
    val kind: FeedKind,
    val order: Order = Order.TRENDING,
    val mediaKind: MediaKind = MediaKind.GIF,
    val query: String = "",
    val creator: String = "",
) {
    val key: String get() = "$kind|$order|$mediaKind|$query|$creator"
}

class FeedViewModel(
    private val repo: RedGifsRepository,
    private val favorites: FavoritesRepository,
    private val source: FeedSource,
) : ViewModel() {

    data class UiState(
        val items: List<RedMedia> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val exhausted: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val favoriteUids: StateFlow<Set<String>> = favorites.observeUids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var nextPage = 1
    private var job: Job? = null

    init {
        loadNext()
    }

    /** Loads the next page. Safe to call repeatedly; it is a no-op while a load is running. */
    fun loadNext() {
        if (job?.isActive == true || _state.value.exhausted) return
        _state.update { it.copy(loading = true, error = null) }
        val requested = nextPage
        job = viewModelScope.launch {
            try {
                val page = fetch(requested)
                nextPage = requested + 1
                _state.update { current ->
                    current.copy(
                        items = (current.items + page.items).distinctBy { it.id },
                        loading = false,
                        exhausted = page.items.isEmpty() || requested >= page.pages,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Network error") }
            }
        }
    }

    fun refresh() {
        job?.cancel()
        nextPage = 1
        _state.value = UiState()
        loadNext()
    }

    fun toggleMediaFavorite(media: RedMedia) {
        viewModelScope.launch { favorites.toggleMedia(media) }
    }

    private suspend fun fetch(page: Int): Page = when (source.kind) {
        FeedKind.BROWSE -> repo.browse(source.order, source.mediaKind, page)
        FeedKind.SEARCH -> repo.search(source.query, source.order, source.mediaKind, page)
        FeedKind.CREATOR -> repo.creator(source.creator, source.order, source.mediaKind, page)
    }
}
