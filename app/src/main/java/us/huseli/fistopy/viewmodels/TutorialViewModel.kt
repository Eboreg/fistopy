package us.huseli.fistopy.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.Constants.PREF_CURRENT_TUTORIAL_PAGE
import us.huseli.fistopy.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext context: Context,
) : AbstractBaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val pages =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(1, 2, 3, 4) else listOf(1, 2, 3)
    private val _currentPage = MutableStateFlow(
        preferences.getInt(PREF_CURRENT_TUTORIAL_PAGE, pages.first()).takeIf { pages.contains(it) } ?: pages.first()
    )

    val currentPage = _currentPage.asStateFlow()
    val isFirstPage = _currentPage.map { pages.indexOf(it) == 0 }.stateWhileSubscribed(true)
    val isLastPage = _currentPage.map { pages.indexOf(it) == pages.lastIndex }.stateWhileSubscribed(false)

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun gotoNextPage() {
        val page = pages.indexOf(_currentPage.value)
            .takeIf { it > -1 && it < pages.lastIndex }
            ?.let { pages[it + 1] }
            ?: 1

        setPagePreference(page)
    }

    fun gotoPreviousPage() {
        val page = pages.indexOf(_currentPage.value)
            .takeIf { it > 0 }
            ?.let { pages[it - 1] }
            ?: 1

        setPagePreference(page)
    }

    fun setStartDestination(value: String) = repos.settings.setStartDestination(value)

    private fun setPagePreference(value: Int) {
        preferences.edit().putInt(PREF_CURRENT_TUTORIAL_PAGE, value).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_CURRENT_TUTORIAL_PAGE) {
            preferences.getInt(PREF_CURRENT_TUTORIAL_PAGE, pages.first()).takeIf { pages.contains(it) }?.also {
                _currentPage.value = it
            }
        }
    }
}
