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

    //pulls entire table
    val ResourceList: StateFlow<List<ResourceEntry>> = resourceDAO.getAllResources().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //do this when application is started
    init {
        CreateDemoResources()
    }

    //If no entries exist, create demo entries
    fun CreateDemoResources() {
        viewModelScope.launch {
            if (resourceDAO.count() == 0) {
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Clark Daniels",
                        description = "Clark is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Jessica Smith",
                        description = "Jessica is a fictional Mental Health Professional. She focuses on helping people who struggle with low self-esteem and anxiety. She has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Jane Tran",
                        description = "Jane is a fictional Mental Health Professional. She focuses on helping people who struggle with low self-esteem and anxiety. She has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "John Power",
                        description = "John is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Steve Ryan",
                        description = "Steve is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Marco Bernadino",
                        description = "Marco is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Brandon Hughes",
                        description = "Brandon is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
                resourceDAO.insertResource(
                    ResourceEntry(
                        name = "Jason Lin",
                        description = "Jason is a fictional Mental Health Professional. He focuses on helping people who struggle with low self-esteem and anxiety. He has been practicing as a licensed psychologist for 5 years now and is based in Lewisville.",
                        phoneNum = "999-999-9999"
                    )
                )
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
