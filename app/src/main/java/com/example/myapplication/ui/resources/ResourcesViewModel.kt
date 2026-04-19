package com.example.myapplication.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.ResourcesDAO
import com.example.myapplication.data.model.ResourceEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResourcesViewModel(private val resourceDAO: ResourcesDAO) : ViewModel() {

    val ResourceList: StateFlow<List<ResourceEntry>> = resourceDAO.getAllResources().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun DemoResources() {
        viewModelScope.launch {
            ResourceEntry(name = "", description = "", phoneNum = "")
        }
    }

}

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
