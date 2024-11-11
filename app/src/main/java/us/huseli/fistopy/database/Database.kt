package us.huseli.fistopy.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.fistopy.BuildConfig
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.artist.AlbumArtist
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.Artist
import us.huseli.fistopy.dataclasses.artist.ArtistCombo
import us.huseli.fistopy.dataclasses.artist.TrackArtist
import us.huseli.fistopy.dataclasses.artist.TrackArtistCredit
import us.huseli.fistopy.dataclasses.playlist.Playlist
import us.huseli.fistopy.dataclasses.radio.Radio
import us.huseli.fistopy.dataclasses.radio.RadioCombo
import us.huseli.fistopy.dataclasses.spotify.SpotifyTrackAudioFeatures
import us.huseli.fistopy.dataclasses.tag.AlbumTag
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.PlaylistTrack
import us.huseli.fistopy.dataclasses.track.PlaylistTrackCombo
import us.huseli.fistopy.dataclasses.track.QueueTrack
import us.huseli.fistopy.dataclasses.track.QueueTrackCombo
import us.huseli.fistopy.dataclasses.track.RadioTrack
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.interfaces.ILogger
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [
        Tag::class,
        Track::class,
        Album::class,
        Playlist::class,
        PlaylistTrack::class,
        QueueTrack::class,
        AlbumTag::class,
        Artist::class,
        AlbumArtist::class,
        TrackArtist::class,
        Radio::class,
        RadioTrack::class,
        SpotifyTrackAudioFeatures::class,
    ],
    views = [
        AlbumArtistCredit::class,
        TrackArtistCredit::class,
        TrackCombo::class,
        AlbumCombo::class,
        RadioCombo::class,
        PlaylistTrackCombo::class,
        QueueTrackCombo::class,
        ArtistCombo::class,
    ],
    exportSchema = false,
    version = 127,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun radioDao(): RadioDao
    abstract fun spotifyDao(): SpotifyDao

    companion object {
        fun build(context: Context): Database {
            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class DBQueryCallback : QueryCallback, ILogger {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        if (
                            !sqlQuery.startsWith("BEGIN DEFERRED TRANSACTION")
                            && !sqlQuery.startsWith("TRANSACTION SUCCESSFUL")
                            && !sqlQuery.startsWith("END TRANSACTION")
                            && !sqlQuery.contains("room_table_modification_log")
                            && !sqlQuery.startsWith("DROP TRIGGER IF EXISTS")
                        ) {
                            var index = 0

                            log("Database", sqlQuery.replace(Regex("\\?")) { "'${bindArgs.getOrNull(index++)}'" })
                        }
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(DBQueryCallback(), executor)
            }

            return builder.build()
        }
    }
}
