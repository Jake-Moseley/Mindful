package com.example.myapplication.ui.resources

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.ResourcesDAO
import com.example.myapplication.data.model.ResourceEntry
import com.example.myapplication.data.repository.NpiResourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResourcesViewModel(private val resourceDAO: ResourcesDAO) : ViewModel() {

    //pulls entire table
    val ResourceList: StateFlow<List<ResourceEntry>> = resourceDAO.getAllResources().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //modifiable query value
    val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery

    //updates search query with new search query
    fun updateSearchQuery(newQuery: String) {
        mutableQuery.value = newQuery
    }

    //declare FilteredResource list like regular ResourceList, except update base query whenever search query changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val FilteredResourceList: StateFlow<List<ResourceEntry>> = mutableQuery.flatMapLatest { query ->
        resourceDAO.getResourceByInput(query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //do this when application is started
    init {
        loadResources()
    }

    private val repository = NpiResourceRepository()
    fun loadResources() {
        viewModelScope.launch {
            //check db count
            val currentCount = resourceDAO.count()
            Log.d("ResourcesDB", "Current DB count: $currentCount")

            //If db has less than 10 entries, reset list and repopulate
            if (currentCount < 10) {
                Log.d("ResourcesDB", "Clearing old entries and fetching live NPI Registry data...")
                resourceDAO.deleteAll()

                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "988 Suicide & Crisis Lifeline",
                        description = "Free and confidential 24/7 support for emotional distress or suicidal crisis. Call or text 988.",
                        phoneNum = "988"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Crisis Text Line",
                        description = "Free 24/7 support with a trained crisis counselor for help with anxiety or depression.",
                        phoneNum = "Text HOME to 741741"
                    )
                )

                //Get live data
                val liveProviders = repository.fetchMentalHealthProviders(limit = 30)
                for (provider in liveProviders) {
                    resourceDAO.insertResource(provider)
                }
            }
        }
    }
}

//function to build context for viewModel
class ResourceViewModelFactory(
    private val resourceDAO: ResourcesDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResourcesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResourcesViewModel(resourceDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
