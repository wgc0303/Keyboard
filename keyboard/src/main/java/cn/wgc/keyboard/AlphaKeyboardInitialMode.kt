package cn.wgc.keyboard

enum class AlphaKeyboardInitialMode {
    NUMBER,
    LETTER;

    companion object {
        fun fromAttr(value: Int): AlphaKeyboardInitialMode = when (value) {
            1 -> LETTER
            else -> NUMBER
        }
    }
}
