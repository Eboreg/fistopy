package us.huseli.fistopy.dataclasses.artist

interface IAlbumArtistCredit : IArtistCredit {
    val albumId: String

    fun withAlbumId(albumId: String): IAlbumArtistCredit

    fun withArtistId(artistId: String) = AlbumArtistCredit(
        albumId = albumId,
        artistId = artistId,
        name = name,
        spotifyId = spotifyId,
        musicBrainzId = musicBrainzId,
        joinPhrase = joinPhrase,
        image = image,
        position = position,
    )
}
