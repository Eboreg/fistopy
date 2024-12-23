package us.huseli.fistopy.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.fistopy.Constants.PREF_APP_START_COUNT
import us.huseli.fistopy.Constants.PREF_AUTO_IMPORT_LOCAL_MUSIC
import us.huseli.fistopy.Constants.PREF_LIBRARY_RADIO_NOVELTY
import us.huseli.fistopy.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.fistopy.Constants.PREF_REGION
import us.huseli.fistopy.Constants.PREF_START_DESTINATION
import us.huseli.fistopy.Constants.PREF_UMLAUTIFY
import us.huseli.fistopy.LibraryDestination
import us.huseli.fistopy.R
import us.huseli.fistopy.TutorialDestination
import us.huseli.fistopy.Umlautify
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.ListType
import us.huseli.fistopy.dataclasses.tag.TagPojo
import us.huseli.fistopy.enums.AlbumSortParameter
import us.huseli.fistopy.enums.ArtistSortParameter
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.Region
import us.huseli.fistopy.enums.SortOrder
import us.huseli.fistopy.enums.TrackSortParameter
import us.huseli.retaintheme.extensions.sanitizeFilename
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _albumSearchTerm = MutableStateFlow("")
    private val _albumSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _albumSortParameter = MutableStateFlow(AlbumSortParameter.ARTIST)
    private val _artistSearchTerm = MutableStateFlow("")
    private val _artistSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _artistSortParameter = MutableStateFlow(ArtistSortParameter.NAME)
    private val _autoImportLocalMusic = MutableStateFlow(
        if (preferences.contains(PREF_AUTO_IMPORT_LOCAL_MUSIC))
            preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
        else null
    )
    private val _contentSize = MutableStateFlow(Size.Zero)
    private val _libraryAlbumTagFilter = MutableStateFlow<ImmutableList<TagPojo>>(persistentListOf())
    private val _libraryAvailabilityFilter = MutableStateFlow(AvailabilityFilter.ALL)
    private val _libraryDisplayType = MutableStateFlow(DisplayType.LIST)
    private val _libraryListType = MutableStateFlow(ListType.ALBUMS)
    private val _libraryRadioNovelty = MutableStateFlow(preferences.getFloat(PREF_LIBRARY_RADIO_NOVELTY, 0.5f))
    private val _libraryTrackTagFilter = MutableStateFlow<ImmutableList<TagPojo>>(persistentListOf())
    private val _localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())
    private val _region =
        MutableStateFlow(preferences.getString(PREF_REGION, null)?.let { Region.valueOf(it) } ?: Region.SE)
    private val _screenSize = MutableStateFlow<Size?>(null)
    private val _screenSizeDp = MutableStateFlow<DpSize?>(null)
    private val _showArtistsWithoutAlbums = MutableStateFlow(false)
    private val _trackSearchTerm = MutableStateFlow("")
    private val _trackSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _trackSortParameter = MutableStateFlow(TrackSortParameter.TITLE)

    val albumSearchTerm = _albumSearchTerm.asStateFlow()
    val albumSortOrder = _albumSortOrder.asStateFlow()
    val albumSortParameter = _albumSortParameter.asStateFlow()
    val artistSearchTerm = _artistSearchTerm.asStateFlow()
    val artistSortOrder = _artistSortOrder.asStateFlow()
    val artistSortParameter = _artistSortParameter.asStateFlow()
    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val contentSize: StateFlow<Size> = _contentSize.asStateFlow()
    val libraryAlbumTagFilter = _libraryAlbumTagFilter.asStateFlow()
    val libraryAvailabilityFilter = _libraryAvailabilityFilter.asStateFlow()
    val libraryDisplayType = _libraryDisplayType.asStateFlow()
    val libraryListType = _libraryListType.asStateFlow()
    val libraryRadioNovelty: StateFlow<Float> = _libraryRadioNovelty.asStateFlow()
    val libraryTrackTagFilter = _libraryTrackTagFilter.asStateFlow()
    val localMusicUri: StateFlow<Uri?> = _localMusicUri.asStateFlow()
    val region: StateFlow<Region> = _region.asStateFlow()
    val showArtistsWithoutAlbums = _showArtistsWithoutAlbums.asStateFlow()
    val trackSearchTerm = _trackSearchTerm.asStateFlow()
    val trackSortOrder = _trackSortOrder.asStateFlow()
    val trackSortParameter = _trackSortParameter.asStateFlow()

    init {
        Umlautify.setEnabled(preferences.getBoolean(PREF_UMLAUTIFY, false))
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun clearCustomStartDestination() {
        setStartDestination(getDefaultStartDestination())
    }

    fun createAlbumDirectory(albumTitle: String, artistString: String?): DocumentFile? {
        val subdirs = listOf(
            artistString?.sanitizeFilename() ?: context.getString(R.string.unknown_artist),
            albumTitle.sanitizeFilename(),
        )

        return getLocalMusicDirectory()?.makeFolder(context, subdirs.joinToString("/"), CreateMode.REUSE)
    }

    fun getLocalMusicDirectory(): DocumentFile? = _localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun getStartDestination(): String {
        val default = getDefaultStartDestination()

        return preferences.getString(PREF_START_DESTINATION, default) ?: default
    }

    fun setAlbumSearchTerm(value: String) {
        _albumSearchTerm.value = value
    }

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) {
        _albumSortParameter.value = sortParameter
        _albumSortOrder.value = sortOrder
    }

    fun setLibraryAvailabilityFilter(value: AvailabilityFilter) {
        _libraryAvailabilityFilter.value = value
    }

    fun setArtistSearchTerm(value: String) {
        _artistSearchTerm.value = value
    }

    fun setArtistSorting(sortParameter: ArtistSortParameter, sortOrder: SortOrder) {
        _artistSortParameter.value = sortParameter
        _artistSortOrder.value = sortOrder
    }

    fun setAutoImportLocalMusic(value: Boolean) {
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
    }

    fun setContentSize(size: Size) {
        _contentSize.value = size
    }

    fun setLibraryAlbumTagFilter(value: List<TagPojo>) {
        _libraryAlbumTagFilter.value = value.toImmutableList()
    }

    fun setLibraryDisplayType(value: DisplayType) {
        _libraryDisplayType.value = value
    }

    fun setLibraryListType(value: ListType) {
        _libraryListType.value = value
    }

    fun setLibraryRadioNovelty(value: Float) {
        preferences.edit().putFloat(PREF_LIBRARY_RADIO_NOVELTY, value).apply()
    }

    fun setLibraryTrackTagFilter(value: List<TagPojo>) {
        _libraryTrackTagFilter.value = value.toImmutableList()
    }

    fun setLocalMusicUri(value: Uri?) {
        preferences.edit().putString(PREF_LOCAL_MUSIC_URI, value?.toString()).apply()
    }

    fun setRegion(value: Region) {
        preferences.edit().putString(PREF_REGION, value.name).apply()
    }

    fun setScreenSize(dpSize: DpSize, size: Size) {
        _screenSize.value = size
        _screenSizeDp.value = dpSize
    }

    fun setShowArtistsWithoutAlbums(value: Boolean) {
        _showArtistsWithoutAlbums.value = value
    }

    fun setStartDestination(value: String) {
        preferences.edit().putString(PREF_START_DESTINATION, value).apply()
    }

    fun setTrackSearchTerm(value: String) {
        _trackSearchTerm.value = value
    }

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) {
        _trackSortParameter.value = sortParameter
        _trackSortOrder.value = sortOrder
    }

    fun setUmlautify(value: Boolean) {
        preferences.edit().putBoolean(PREF_UMLAUTIFY, value).apply()
    }

    private fun getDefaultStartDestination(): String {
        return if (preferences.getInt(PREF_APP_START_COUNT, 1) == 1)
            TutorialDestination.route
        else LibraryDestination.route
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_UMLAUTIFY -> Umlautify.setEnabled(preferences.getBoolean(PREF_UMLAUTIFY, false))
            PREF_LOCAL_MUSIC_URI -> _localMusicUri.value = preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri()
            PREF_LIBRARY_RADIO_NOVELTY -> _libraryRadioNovelty.value =
                preferences.getFloat(PREF_LIBRARY_RADIO_NOVELTY, 0.5f)
            PREF_AUTO_IMPORT_LOCAL_MUSIC -> _autoImportLocalMusic.value =
                preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
            PREF_REGION -> preferences.getString(PREF_REGION, null)
                ?.let { Region.valueOf(it) }
                ?.also { _region.value = it }
        }
    }
}
