package us.huseli.fistopy.dataclasses.artist

import us.huseli.fistopy.dataclasses.MediaStoreImage

interface IArtist {
    val name: String
    val spotifyId: String?
    val musicBrainzId: String?
    val image: MediaStoreImage?

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/artist/$it" }

    fun toSaveableArtist() = Artist(
        name = name,
        spotifyId = spotifyId,
        musicBrainzId = musicBrainzId,
        image = image,
    )
}
