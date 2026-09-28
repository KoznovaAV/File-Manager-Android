package com.example.filemanager

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class FileViewModel : ViewModel() {

    private val repository = FileRepository()

    val currentDir = MutableLiveData<File>()
    val sortType = MutableLiveData(SortType.NAME)
    val searchQuery = MutableLiveData("")

    val files = MediatorLiveData<List<File>>().apply {
        addSource(currentDir) { refresh() }
        addSource(sortType) { refresh() }
        addSource(searchQuery) { refresh() }
    }

    private fun refresh() {
        val dir = currentDir.value ?: return
        files.value = repository.listFiles(dir, sortType.value ?: SortType.NAME, searchQuery.value)
    }

    fun openDir(dir: File) { currentDir.value = dir }
    fun setSortType(type: SortType) { sortType.value = type }
    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun refreshCurrent() = refresh()
}