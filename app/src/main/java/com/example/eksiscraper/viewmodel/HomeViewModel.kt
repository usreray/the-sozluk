package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class HomeCategory(val key: String, val label: String) {
    Gundem("popular", "gündem"),
    Bugun("today", "bugün"),
    Debe("debe", "debe")
}

data class CategoryState(
    val topics: List<Topic> = emptyList(),
    /** First load with nothing on screen yet */
    val isLoading: Boolean = false,
    /** Pull-to-refresh over an existing list */
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val canLoadMore: Boolean = true
)

/** Each tab keeps its own list, so swiping between tabs never shows another tab's topics. */
class HomeViewModel(private val repository: EksiRepository) : ViewModel() {

    private val states = mutableStateMapOf<HomeCategory, CategoryState>()
    private val jobs = mutableMapOf<HomeCategory, Job>()

    val listStates: Map<HomeCategory, LazyListState> =
        HomeCategory.entries.associateWith { LazyListState() }

    fun state(category: HomeCategory): CategoryState = states[category] ?: CategoryState()

    fun ensureLoaded(category: HomeCategory) {
        val s = state(category)
        if (s.page == 0 && !s.isLoading && s.error == null) load(category, page = 1)
    }

    fun refresh(category: HomeCategory) = load(category, page = 1, refreshing = true)

    fun retry(category: HomeCategory) = load(category, page = 1)

    fun loadMore(category: HomeCategory) {
        val s = state(category)
        if (s.page > 0 && s.canLoadMore && !s.isLoading && !s.isRefreshing && !s.isLoadingMore) {
            load(category, s.page + 1)
        }
    }

    private fun load(category: HomeCategory, page: Int, refreshing: Boolean = false) {
        jobs[category]?.cancel()
        update(category) {
            if (page == 1) it.copy(
                isLoading = !refreshing || it.topics.isEmpty(),
                isRefreshing = refreshing && it.topics.isNotEmpty(),
                isLoadingMore = false,
                error = null
            ) else it.copy(isLoadingMore = true)
        }
        jobs[category] = viewModelScope.launch {
            try {
                val result = repository.getPopularTopics(page, category.key)
                update(category) {
                    it.copy(
                        // Lazy list keys are topic URLs, so never keep the same topic twice
                        topics = (if (page == 1) result else it.topics + result).distinctBy { t -> t.url },
                        page = page,
                        // debe is a single list; the others have ~50 topics per page
                        canLoadMore = category != HomeCategory.Debe && result.size >= 40,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update(category) {
                    if (page == 1) it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        // Keep a list that is already on screen; only show the error if empty
                        error = if (it.topics.isEmpty()) e.message ?: "Bir şeyler ters gitti" else null
                    ) else it.copy(isLoadingMore = false, canLoadMore = false)
                }
            }
        }
    }

    private inline fun update(category: HomeCategory, block: (CategoryState) -> CategoryState) {
        states[category] = block(state(category))
    }
}
