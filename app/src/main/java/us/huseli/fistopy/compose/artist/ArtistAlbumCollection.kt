package us.huseli.fistopy.compose.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.fistopy.AlbumDownloadTask
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.FistopyTheme
import us.huseli.fistopy.compose.album.AlbumGridCell
import us.huseli.fistopy.compose.album.AlbumListCard
import us.huseli.fistopy.compose.album.SelectedAlbumsButtons
import us.huseli.fistopy.compose.scrollbar.ScrollbarGrid
import us.huseli.fistopy.compose.scrollbar.ScrollbarList
import us.huseli.fistopy.compose.utils.IsLoadingProgressIndicator
import us.huseli.fistopy.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.fistopy.dataclasses.album.AlbumUiState
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.artist.IArtist
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.getUmlautifiedString
import us.huseli.fistopy.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistAlbumCollection(
    uiStates: ImmutableList<AlbumUiState>,
    selectionCallbacks: AlbumSelectionCallbacks,
    displayType: DisplayType,
    isLoading: Boolean,
    selectedAlbumCount: () -> Int,
    downloadStateFlow: (String) -> StateFlow<AlbumDownloadTask.UiState?>,
    relatedArtists: ImmutableList<IArtist>,
    otherAlbums: ImmutableList<IAlbum>,
    otherAlbumsPreview: ImmutableList<IAlbum>,
    otherAlbumTypes: ImmutableList<AlbumType>,
    onClick: (AlbumUiState) -> Unit,
    onLongClick: (AlbumUiState) -> Unit,
    onOtherAlbumClick: (String) -> Unit,
    onOtherAlbumTypeClick: (AlbumType) -> Unit,
    onRelatedArtistClick: (IArtist) -> Unit,
) {
    val context = LocalContext.current
    var expandOtherAlbums by rememberSaveable { mutableStateOf(false) }

    SelectedAlbumsButtons(albumCount = selectedAlbumCount, callbacks = selectionCallbacks)
    if (isLoading) IsLoadingProgressIndicator()

    when (displayType) {
        DisplayType.LIST -> {
            ScrollbarList(
                contentPadding = PaddingValues(10.dp),
                contentType = "AlbumUiState",
            ) {
                if (uiStates.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.albums_in_library),
                            style = FistopyTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    items(uiStates, key = { it.id }, contentType = { "AlbumUiState" }) { state ->
                        AlbumListCard(
                            state = state,
                            downloadStateFlow = { downloadStateFlow(state.albumId) },
                            onClick = { onClick(state) },
                            onLongClick = { onLongClick(state) },
                            showArtist = false,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }

                if (uiStates.isNotEmpty() && (otherAlbumsPreview.isNotEmpty() || relatedArtists.isNotEmpty())) item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (otherAlbumsPreview.isNotEmpty()) {
                    OtherArtistAlbumsList(
                        isExpanded = expandOtherAlbums,
                        albumTypes = otherAlbumTypes,
                        albums = otherAlbums,
                        preview = otherAlbumsPreview,
                        onAlbumTypeClick = onOtherAlbumTypeClick,
                        onClick = onOtherAlbumClick,
                        header = {
                            OtherArtistAlbumsHeader(
                                expand = expandOtherAlbums,
                                onExpandToggleClick = { expandOtherAlbums = !expandOtherAlbums },
                                text = {
                                    Text(
                                        text = context.getUmlautifiedString(R.string.available_albums),
                                        style = FistopyTheme.typography.titleMedium,
                                        maxLines = 2,
                                    )
                                },
                            )
                        },
                    )
                }

                if (otherAlbumsPreview.isNotEmpty() && relatedArtists.isNotEmpty()) item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (relatedArtists.isNotEmpty()) item {
                    RelatedArtistsCardList(relatedArtists = relatedArtists, onClick = onRelatedArtistClick)
                }
            }
        }
        DisplayType.GRID -> {
            ScrollbarGrid(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                if (uiStates.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.albums_in_library),
                            style = FistopyTheme.typography.titleMedium,
                        )
                    }
                    items(uiStates, key = { it.id }) { state ->
                        OutlinedCard(
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.combinedClickable(
                                onClick = { onClick(state) },
                                onLongClick = { onLongClick(state) },
                            ),
                            border = CardDefaults.outlinedCardBorder().let {
                                if (state.isSelected) it.copy(width = it.width + 2.dp) else it
                            },
                        ) {
                            AlbumGridCell(
                                state = state,
                                downloadStateFlow = { downloadStateFlow(state.albumId) },
                                showArtist = false,
                            )
                        }
                    }
                }

                if (uiStates.isNotEmpty() && (otherAlbumsPreview.isNotEmpty() || relatedArtists.isNotEmpty())) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                if (otherAlbumsPreview.isNotEmpty()) OtherArtistAlbumsGrid(
                    isExpanded = expandOtherAlbums,
                    albums = otherAlbums,
                    preview = otherAlbumsPreview,
                    albumTypes = otherAlbumTypes,
                    onClick = onOtherAlbumClick,
                    onAlbumTypeClick = onOtherAlbumTypeClick,
                    onExpandToggleClick = { expandOtherAlbums = !expandOtherAlbums },
                )

                if (otherAlbumsPreview.isNotEmpty() && relatedArtists.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                if (relatedArtists.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    RelatedArtistsCardList(relatedArtists = relatedArtists, onClick = onRelatedArtistClick)
                }
            }
        }
    }
}
