package us.huseli.fistopy.dataclasses.tag

import androidx.compose.runtime.Immutable

@Immutable
data class TagPojo(
    val name: String,
    val itemCount: Int,
)
