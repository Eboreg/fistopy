package us.huseli.fistopy.repositories

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories @Inject constructor(
    val album: AlbumRepository,
    val artist: ArtistRepository,
    val discogs: DiscogsRepository,
    val itemSelection: ItemSelectionRepository,
    val lastFm: LastFmRepository,
    val localMedia: LocalMediaRepository,
    val message: MessageRepository,
    val musicBrainz: MusicBrainzRepository,
    val player: PlayerRepository,
    val playlist: PlaylistRepository,
    val settings: SettingsRepository,
    val spotify: SpotifyRepository,
    val track: TrackRepository,
    val youtube: YoutubeRepository,
)
