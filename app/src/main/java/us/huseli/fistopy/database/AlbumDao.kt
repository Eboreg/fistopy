@file:Suppress("FunctionName")

package us.huseli.fistopy.database

import android.database.DatabaseUtils
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.AlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.artist.AlbumArtist
import us.huseli.fistopy.dataclasses.tag.AlbumTag
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.tag.TagPojo
import us.huseli.fistopy.dataclasses.tag.toAlbumTags
import us.huseli.fistopy.enums.AlbumSortParameter
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.SortOrder

@Dao
abstract class AlbumDao {
    @Query("DELETE FROM AlbumTag WHERE AlbumTag_albumId IN (:albumIds)")
    protected abstract suspend fun _deleteAlbumTags(vararg albumIds: String)

    @Transaction
    @RawQuery(observedEntities = [Album::class, Tag::class, AlbumTag::class, AlbumArtist::class])
    protected abstract fun _flowAlbumCombos(query: SupportSQLiteQuery): Flow<List<AlbumCombo>>

    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    protected abstract fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @Query("SELECT * FROM Album WHERE Album_spotifyId = :spotifyId")
    protected abstract suspend fun _getAlbumBySpotifyId(spotifyId: String): Album?

    @Query("SELECT * FROM Album WHERE Album_musicBrainzReleaseId = :releaseId OR Album_musicBrainzReleaseGroupId = :groupId")
    protected abstract suspend fun _getAlbumByMusicBrainzId(groupId: String, releaseId: String): Album?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertAlbumTags(vararg albumTags: AlbumTag)

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_isInLibrary = 1 AND Album_musicBrainzReleaseGroupId IS NOT NULL")
    protected abstract suspend fun _listMusicBrainzReleaseGroupAlbumCombos(): List<AlbumCombo>

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_isInLibrary = 1 AND Album_spotifyId IS NOT NULL")
    protected abstract suspend fun _listSpotifyAlbumCombos(): List<AlbumCombo>

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_isInLibrary = 1 AND Album_youtubePlaylist_id IS NOT NULL")
    protected abstract suspend fun _listYoutubeAlbumCombos(): List<AlbumCombo>

    @Query("UPDATE Album SET Album_spotifyId = :spotifyId WHERE Album_albumId = :albumId")
    protected abstract suspend fun _setAlbumSpotifyId(albumId: String, spotifyId: String)

    @Query("UPDATE Album SET Album_musicBrainzReleaseId = :releaseId, Album_musicBrainzReleaseGroupId = :groupId WHERE Album_albumId = :albumId")
    protected abstract suspend fun _setAlbumMusicBrainzIds(albumId: String, groupId: String, releaseId: String)

    @Query(
        """
        UPDATE Album SET Album_albumArt_fullUriString = :fullUri, Album_albumArt_thumbnailUriString = :thumbnailUri
        WHERE Album_albumId = :albumId
        """
    )
    protected abstract suspend fun _updateAlbumArt(albumId: String, fullUri: String, thumbnailUri: String)

    @Upsert
    protected abstract suspend fun _upsertAlbums(vararg albums: Album)


    /** PUBLIC METHODS ************************************************************************************************/

    @Query(
        """
        UPDATE Album SET Album_albumArt_fullUriString = NULL, Album_albumArt_thumbnailUriString = NULL
        WHERE Album_albumId IN (:albumIds)
        """
    )
    abstract suspend fun clearAlbumArt(vararg albumIds: String)

    @Query("DELETE FROM Album")
    abstract suspend fun clearAlbums()

    @Query("DELETE FROM Tag")
    abstract suspend fun clearTags()

    @Query(
        """
        DELETE FROM Album WHERE Album_isHidden = 1 AND Album_isLocal = 0
        AND NOT EXISTS(SELECT * FROM QueueTrackCombo WHERE QueueTrackCombo.Album_albumId = Album.Album_albumId)
        """
    )
    abstract suspend fun deleteHiddenNonLocalAlbums()

    @Query(
        """
        DELETE FROM Album WHERE Album_isInLibrary = 0
        AND NOT EXISTS(SELECT * FROM QueueTrackCombo WHERE QueueTrackCombo.Album_albumId = Album.Album_albumId)
        """
    )
    abstract suspend fun deleteTempAlbums()

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId = :albumId")
    abstract fun flowAlbumCombo(albumId: String): Flow<AlbumCombo?>

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: Collection<String> = emptyList(),
        availabilityFilter: AvailabilityFilter,
    ): Flow<List<AlbumCombo>> {
        val searchQuery = searchTerm
            .takeIf { it.isNotBlank() }
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(AlbumArtist_name) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }
            ?.let { " AND $it" }
            ?: ""
        val tagList = tagNames.joinToString(", ") { DatabaseUtils.sqlEscapeString(it) }
        val tagJoin =
            if (tagNames.isNotEmpty())
                "JOIN AlbumTag ON Album_albumId = AlbumTag_albumId AND AlbumTag_tagName IN ($tagList)"
            else ""
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND (Track_localUri IS NOT NULL OR Track_youtubeVideo_id IS NOT NULL))"
            AvailabilityFilter.ONLY_LOCAL -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND Track_localUri IS NOT NULL)"
        }

        return _flowAlbumCombos(
            SimpleSQLiteQuery(
                """
                SELECT AlbumCombo.* FROM AlbumCombo LEFT JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId $tagJoin
                WHERE Album_isInLibrary = 1 AND Album_isHidden = 0 AND Album_trackCount > 0
                $searchQuery $availabilityQuery
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sql(sortOrder)}
                """.trimIndent()
            )
        )
    }

    @Query(
        """
        SELECT AlbumCombo.* FROM AlbumCombo 
        JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId AND AlbumArtist_artistId = :artistId
        WHERE Album_isHidden = 0 AND Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_title)
        """
    )
    @Transaction
    abstract fun flowAlbumCombosByArtist(artistId: String): Flow<List<AlbumCombo>>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract fun flowAlbumWithTracks(albumId: String): Flow<AlbumWithTracksCombo?>

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> {
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND (Track_localUri IS NOT NULL OR Track_youtubeVideo_id IS NOT NULL))"
            AvailabilityFilter.ONLY_LOCAL -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND Track_localUri IS NOT NULL)"
        }

        return _flowTagPojos(
            SimpleSQLiteQuery(
                """
                SELECT Tag_name AS name, COUNT(*) AS itemCount
                FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
                JOIN Album ON Album_albumId = AlbumTag_albumId
                WHERE Album_isInLibrary = 1 AND Album_isHidden = 0
                $availabilityQuery
                GROUP BY Tag_name
                ORDER BY itemCount DESC, Tag_name ASC
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Tag ORDER BY Tag_name")
    abstract fun flowTags(): Flow<List<Tag>>

    @Query(
        """
        SELECT DISTINCT Tag.Tag_name FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
        WHERE AlbumTag_albumId = :albumId
        """
    )
    abstract fun flowTagNamesByAlbumId(albumId: String): Flow<List<String>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbum(albumId: String): Album?

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbumCombo(albumId: String): AlbumCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbumWithTracks(albumId: String): AlbumWithTracksCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_youtubePlaylist_id = :playlistId")
    abstract suspend fun getAlbumWithTracksByYoutubePlaylistId(playlistId: String): AlbumWithTracksCombo?


    @Transaction
    open suspend fun getOrCreateAlbumByMusicBrainzId(album: IAlbum, groupId: String, releaseId: String): Album {
        /**
         * If album is already in DB: update its MBID's if needed and return it.
         * Else if another album with this MBID exists in DB: return that one.
         * Else save as new album and return.
         */
        val existingAlbum = album as? Album ?: _getAlbumByMusicBrainzId(groupId, releaseId)

        return if (existingAlbum != null) {
            if (existingAlbum.musicBrainzReleaseId != releaseId || existingAlbum.musicBrainzReleaseGroupId != groupId) {
                _setAlbumMusicBrainzIds(existingAlbum.albumId, groupId, releaseId)
                existingAlbum.copy(musicBrainzReleaseId = releaseId, musicBrainzReleaseGroupId = groupId)
            } else existingAlbum
        } else {
            album.asSavedAlbum().copy(musicBrainzReleaseId = releaseId, musicBrainzReleaseGroupId = groupId).also {
                _upsertAlbums(it)
            }
        }
    }

    @Transaction
    open suspend fun getOrCreateAlbumBySpotifyId(album: IAlbum, spotifyId: String): Album {
        /**
         * If album is already in DB: update its Spotify ID if needed and return it.
         * Else if another album with this Spotify ID exists in DB: return that one.
         * Else save as new album and return.
         */
        val existingAlbum = album as? Album ?: _getAlbumBySpotifyId(spotifyId)

        return if (existingAlbum != null) {
            if (existingAlbum.spotifyId != spotifyId) {
                _setAlbumSpotifyId(existingAlbum.albumId, spotifyId)
                existingAlbum.copy(spotifyId = spotifyId)
            } else existingAlbum
        } else {
            album.asSavedAlbum().copy(spotifyId = spotifyId).also { _upsertAlbums(it) }
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTags(vararg tags: Tag)

    @Transaction
    @Query("SELECT * FROM AlbumCombo")
    abstract suspend fun listAlbumCombos(): List<AlbumCombo>

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId IN(:albumIds)")
    abstract suspend fun listAlbumCombos(vararg albumIds: String): List<AlbumCombo>

    @Query("SELECT Album_albumId FROM Album")
    abstract suspend fun listAlbumIds(): List<String>

    @Transaction
    @Query("SELECT * FROM Album")
    abstract suspend fun listAlbumsWithTracks(): List<AlbumWithTracksCombo>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId IN(:albumIds)")
    abstract suspend fun listAlbumsWithTracks(vararg albumIds: String): List<AlbumWithTracksCombo>

    @Query("SELECT Album_musicBrainzReleaseId FROM Album WHERE Album_isInLibrary = 1 AND Album_musicBrainzReleaseId IS NOT NULL")
    abstract suspend fun listMusicBrainzReleaseIds(): List<String>

    @Query("SELECT Album_spotifyId FROM Album WHERE Album_isInLibrary = 1 AND Album_spotifyId IS NOT NULL")
    abstract suspend fun listSpotifyAlbumIds(): List<String>

    @Query("SELECT Tag_name FROM Tag")
    abstract suspend fun listTagNames(): List<String>

    @Query("SELECT DISTINCT Tag.* FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName WHERE AlbumTag_albumId = :albumId")
    abstract suspend fun listTags(albumId: String): List<Tag>

    suspend fun mapMusicBrainzReleaseGroupAlbumCombos(): Map<String, AlbumCombo> {
        return _listMusicBrainzReleaseGroupAlbumCombos().associateBy { it.album.musicBrainzReleaseGroupId!! }
    }

    suspend fun mapSpotifyAlbumCombos(): Map<String, AlbumCombo> {
        return _listSpotifyAlbumCombos().associateBy { it.album.spotifyId!! }
    }

    suspend fun mapYoutubeAlbumCombos(): Map<String, AlbumCombo> {
        return _listYoutubeAlbumCombos().associateBy { it.album.youtubePlaylist!!.id }
    }

    @Transaction
    open suspend fun setAlbumTags(albumId: String, tags: Collection<Tag>) {
        _deleteAlbumTags(albumId)
        if (tags.isNotEmpty()) {
            insertTags(*tags.toTypedArray())
            _insertAlbumTags(*tags.toAlbumTags(albumId).toTypedArray())
        }
    }

    @Query("UPDATE Album SET Album_isHidden = :isHidden WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsHidden(isHidden: Boolean, vararg albumIds: String)

    @Query("UPDATE Album SET Album_isLocal = :isLocal WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsLocal(isLocal: Boolean, vararg albumIds: String)

    @Query("UPDATE Album SET Album_isHidden = 0 WHERE Album_isHidden = 1 AND Album_isLocal = 1 AND Album_isInLibrary = 1")
    abstract suspend fun unhideLocalAlbums()

    suspend fun updateAlbumArt(albumId: String, albumArt: MediaStoreImage) =
        _updateAlbumArt(albumId, albumArt.fullUriString, albumArt.thumbnailUriString)

    open suspend fun upsertAlbum(album: IAlbum): Album {
        val savedAlbum = album.asSavedAlbum()

        _upsertAlbums(savedAlbum)
        return savedAlbum
    }
}
