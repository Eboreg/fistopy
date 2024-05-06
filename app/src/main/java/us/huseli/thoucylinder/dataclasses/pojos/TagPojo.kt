package us.huseli.thoucylinder.dataclasses.pojos

import androidx.compose.runtime.Immutable

@Immutable
data class TagPojo(
    val name: String,
    val itemCount: Int,
)
