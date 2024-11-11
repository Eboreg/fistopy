package us.huseli.fistopy.dataclasses.artist

import us.huseli.fistopy.dataclasses.MediaStoreImage

data class UnsavedArtistCredit(
    override val image: MediaStoreImage? = null,
    override val joinPhrase: String = "/",
    override val position: Int = 0,
    override val name: String,
    override val spotifyId: String? = null,
    override val musicBrainzId: String? = null,
) : IArtistCredit
