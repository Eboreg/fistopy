package us.huseli.fistopy.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.fistopy.database.AlbumDao
import us.huseli.fistopy.database.ArtistDao
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.database.PlaylistDao
import us.huseli.fistopy.database.QueueDao
import us.huseli.fistopy.database.TrackDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database = Database.build(context)

    @Provides
    fun provideArtistDao(database: Database): ArtistDao = database.artistDao()

    @Provides
    fun provideTrackDao(database: Database): TrackDao = database.trackDao()

    @Provides
    fun provideAlbumDao(database: Database): AlbumDao = database.albumDao()

    @Provides
    fun providePlaylistDao(database: Database): PlaylistDao = database.playlistDao()

    @Provides
    fun provideQueueDao(database: Database): QueueDao = database.queueDao()
}
