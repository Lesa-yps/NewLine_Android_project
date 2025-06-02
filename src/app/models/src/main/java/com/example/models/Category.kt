package com.example.models


data class Category(
    var id: Int,
    val userId: Int,
    val name: NonEmptyString,
    val color: Color
) {
    constructor(id: Int, userId: Int, nameStr: String, colorStr: String) :
            this(id, userId, NonEmptyString(nameStr), Color(colorStr))
}


data class Color(val hex: String) {
    init {
        require(isValidHex(hex)) { "Неверный формат цвета: $hex. Ожидаемый формат: #RRGGBB or #AARRGGBB" }
    }

    companion object {
        private val HEX_COLOR_REGEX = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$".toRegex()

        fun isValidHex(hex: String): Boolean {
            return HEX_COLOR_REGEX.matches(hex)
        }
    }

    override fun toString(): String = hex
}