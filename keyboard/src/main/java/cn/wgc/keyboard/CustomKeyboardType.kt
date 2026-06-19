package cn.wgc.keyboard

enum class CustomKeyboardType {
    NUMBER,
    ID_CARD,
    NUMBER_PASSWORD,
    ALPHA_NUMBER;

    companion object {
        fun fromAttr(value: Int): CustomKeyboardType = when (value) {
            1 -> ID_CARD
            2 -> NUMBER_PASSWORD
            3 -> ALPHA_NUMBER
            else -> NUMBER
        }
    }
}
