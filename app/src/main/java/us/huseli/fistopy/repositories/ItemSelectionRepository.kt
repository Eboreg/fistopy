package us.huseli.fistopy.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemSelectionRepository @Inject constructor() {
    private val selectedItemIds = mutableMapOf<String, MutableStateFlow<List<String>>>()

    fun getSelectedItemIds(key: String): StateFlow<List<String>> =
        getMutableSelectedItemIds(key).asStateFlow()

    fun setItemsIsSelected(key: String, itemIds: Iterable<String>, value: Boolean) {
        if (value) getMutableSelectedItemIds(key).value += itemIds
        else getMutableSelectedItemIds(key).value -= itemIds
    }

    fun unselectAllItems(key: String) {
        getMutableSelectedItemIds(key).value = emptyList()
    }

    private fun getMutableSelectedItemIds(key: String): MutableStateFlow<List<String>> =
        selectedItemIds.getOrPut(key) { MutableStateFlow<List<String>>(emptyList()) }
}
