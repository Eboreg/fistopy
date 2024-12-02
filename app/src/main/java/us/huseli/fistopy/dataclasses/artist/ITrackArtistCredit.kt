package us.huseli.fistopy.dataclasses.artist

interface ITrackArtistCredit : IArtistCredit {
    val trackId: String

    fun withTrackId(trackId: String): ITrackArtistCredit

    fun withArtistId(artistId: String) = TrackArtistCredit(
        trackId = trackId,
        artistId = artistId,
        name = name,
        spotifyId = spotifyId,
        musicBrainzId = musicBrainzId,
        joinPhrase = joinPhrase,
        image = image,
        position = position,
    )
}