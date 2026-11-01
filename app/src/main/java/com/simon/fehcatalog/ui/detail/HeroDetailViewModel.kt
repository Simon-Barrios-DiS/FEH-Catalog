package com.simon.fehcatalog.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simon.fehcatalog.data.HeroRepository
import com.simon.fehcatalog.data.HeroWithCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HeroDetailViewModel(
    private val repository: HeroRepository,
    private val heroId: Long
) : ViewModel() {

    val hero: StateFlow<HeroWithCollection?> = repository.observeHero(heroId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setOwned(owned: Boolean) {
        viewModelScope.launch {
            repository.setOwned(heroId, owned)
        }
    }

    fun setCopies(copies: Int) {
        viewModelScope.launch {
            repository.setCopies(heroId, copies)
        }
    }

    fun setKept(kept: Boolean) {
        viewModelScope.launch {
            repository.setKept(heroId, kept)
        }
    }

    fun setMerges(merges: Int) {
        viewModelScope.launch {
            repository.setMerges(heroId, merges)
        }
    }

    fun setFavorite(favoriteSlot: Int) {
        viewModelScope.launch {
            repository.setFavorite(heroId, favoriteSlot)
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            repository.setNotes(heroId, notes)
        }
    }

    companion object {
        fun factory(repository: HeroRepository, heroId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HeroDetailViewModel(repository, heroId) }
            }
    }
}
