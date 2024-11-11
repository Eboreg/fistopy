package us.huseli.fistopy.interfaces

interface ISelectableItem {
    val id: String
    val isPlayable: Boolean
    val isSelected: Boolean

    fun withIsSelected(value: Boolean): ISelectableItem
}
