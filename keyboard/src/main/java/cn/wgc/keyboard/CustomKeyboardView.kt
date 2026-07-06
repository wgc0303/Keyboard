package cn.wgc.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding

internal class CustomKeyboardView(context: Context) : LinearLayout(context) {

    interface Listener {
        fun onText(text: String)
        fun onDeleteDown()
        fun onDeleteUp()
        fun onHide()
        fun onTogglePassword()
        fun onKeyboardChanged()
    }

    var listener: Listener? = null

    private var keyboardType = CustomKeyboardType.NUMBER
    private var keyGap = 0
    private var passwordVisible = false
    private var disableSpace = false
    private var disableDot = false
    private var alphaNumberShowingLetters = false
    private var uppercase = true
    private var rebuildLettersWhenLaidOut = false
    private var randomNumberKeys = false
    private var numberKeys = ('0'..'9').toList()
    private var style = CustomKeyboardStyle.default(context)
    val keyboardBackgroundColor: Int
        get() = if (keyGap == 0) style.keyboardBackgroundColor else style.spacedKeyboardBackgroundColor
    val systemNavFillColor: Int
        get() = style.systemNavFillColor

    init {
        orientation = VERTICAL
        isClickable = true
        isFocusable = false
        setBackgroundColor(style.keyboardBackgroundColor)
        dividerDrawable = dividerDrawable()
        showDividers = SHOW_DIVIDER_BEGINNING or SHOW_DIVIDER_MIDDLE or SHOW_DIVIDER_END
        setPadding(0)
    }

    fun configure(
        type: CustomKeyboardType,
        keyGap: Int,
        passwordVisible: Boolean,
        disableSpace: Boolean,
        disableDot: Boolean,
        alphaInitialMode: AlphaKeyboardInitialMode,
        resetAlphaMode: Boolean,
        randomNumberKeys: Boolean,
        style: CustomKeyboardStyle
    ) {
        val typeChanged = keyboardType != type
        val randomChanged = this.randomNumberKeys != randomNumberKeys
        val styleChanged = this.style != style
        keyboardType = type
        this.keyGap = keyGap
        this.passwordVisible = passwordVisible
        this.disableSpace = disableSpace
        this.disableDot = disableDot
        this.randomNumberKeys = randomNumberKeys
        this.style = style
        if (shouldResetNumberKeys(typeChanged, resetAlphaMode, randomChanged)) {
            numberKeys = if (randomNumberKeys) ('0'..'9').toList().shuffled() else ('0'..'9').toList()
        }
        setBackgroundColor(keyboardBackgroundColor)
        dividerDrawable = dividerDrawable()
        showDividers = if (keyGap == 0) SHOW_DIVIDER_BEGINNING or SHOW_DIVIDER_MIDDLE or SHOW_DIVIDER_END else SHOW_DIVIDER_NONE
        setPadding(if (keyGap == 0) 0 else style.panelPaddingWhenSpaced)
        alphaNumberShowingLetters = if (type == CustomKeyboardType.ALPHA_NUMBER) {
            if (typeChanged || resetAlphaMode) alphaInitialMode == AlphaKeyboardInitialMode.LETTER else alphaNumberShowingLetters
        } else {
            false
        }
        rebuildLettersWhenLaidOut = type == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters && width == 0
        rebuild()
        if ((type == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters) || styleChanged) {
            post { rebuild() }
        }
    }

    fun setPasswordVisible(visible: Boolean) {
        passwordVisible = visible
        rebuild()
    }

    fun ensureReadyAfterShown() {
        if (rebuildLettersWhenLaidOut && keyboardType == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters) {
            rebuildLettersWhenLaidOut = false
            rebuild()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (
            keyboardType == CustomKeyboardType.ALPHA_NUMBER &&
            alphaNumberShowingLetters &&
            (w != oldw || rebuildLettersWhenLaidOut)
        ) {
            rebuildLettersWhenLaidOut = false
            rebuild()
        }
    }

    private fun rebuild() {
        removeAllViews()
        if (keyboardType == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters) {
            buildLetters()
        } else {
            buildNumbers()
        }
        listener?.onKeyboardChanged()
    }

    private fun buildNumbers() {
        addWeightedRow(listOf(textKey(numberKeys[1].toString()), textKey(numberKeys[2].toString()), textKey(numberKeys[3].toString())))
        addWeightedRow(listOf(textKey(numberKeys[4].toString()), textKey(numberKeys[5].toString()), textKey(numberKeys[6].toString())))
        addWeightedRow(listOf(textKey(numberKeys[7].toString()), textKey(numberKeys[8].toString()), textKey(numberKeys[9].toString())))

        val left = when (keyboardType) {
            CustomKeyboardType.NUMBER -> iconKey(style.hideKeyboardIconRes) { listener?.onHide() }
            CustomKeyboardType.ID_CARD -> textKey("X")
            CustomKeyboardType.NUMBER_PASSWORD -> iconKey(
                if (passwordVisible) style.invisibleIconRes else style.visibleIconRes
            ) { listener?.onTogglePassword() }
            CustomKeyboardType.ALPHA_NUMBER -> functionTextKey("ABC") {
                alphaNumberShowingLetters = true
                rebuild()
            }
        }
        addWeightedRow(listOf(left, textKey(numberKeys[0].toString()), deleteKey()))
    }

    private fun shouldResetNumberKeys(typeChanged: Boolean, resetAlphaMode: Boolean, randomChanged: Boolean): Boolean {
        return typeChanged || resetAlphaMode || randomChanged || keyboardType != CustomKeyboardType.ALPHA_NUMBER || !alphaNumberShowingLetters
    }

    private fun buildLetters() {
        val parentWidth = (parent as? View)?.width ?: 0
        val available = (
            width.takeIf { it > 0 }
                ?: parentWidth.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels
            ) - paddingLeft - paddingRight
        val dividerWidth = if (keyGap == 0) dp(1) else 0
        val keyWidth = (((available - dividerWidth * 9) / 10f) - keyGap).toInt().coerceAtLeast(dp(22))
        val rowHeight = style.keyHeight

        addFixedRow("QWERTYUIOP".map { letterKey(it.toString(), keyWidth) }, rowHeight)
        addFixedRow("ASDFGHJKL".map { letterKey(it.toString(), keyWidth) }, rowHeight)

        val third = mutableListOf<View>()
        val shiftIcon = if (uppercase) style.shiftActiveIconRes else style.shiftIconRes
        third += iconKey(shiftIcon, function = true, flatEdge = true) {
            uppercase = !uppercase
            rebuild()
        }
        "ZXCVBNM".forEach { third += letterKey(it.toString(), keyWidth) }
        third += deleteKey(flatEdge = true)
        addEdgeFilledLetterRow(third, rowHeight)

        val fourth = listOf(
            functionTextKey("123", flatEdge = true) {
                alphaNumberShowingLetters = false
                rebuild()
            },
            functionTextKey("space", enabled = !disableSpace) {
                listener?.onText(" ")
            }.withFixedWidth((keyWidth * 5.6f).toInt()),
            functionTextKey(".", enabled = !disableDot, flatEdge = true) {
                listener?.onText(".")
            }
        )
        addEdgeFilledLetterRow(fourth, rowHeight)
    }

    private fun addWeightedRow(keys: List<View>) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            setBackgroundColor(style.keyboardBackgroundColor)
            dividerDrawable = dividerDrawable()
            showDividers = if (keyGap == 0) SHOW_DIVIDER_MIDDLE else SHOW_DIVIDER_NONE
        }
        keys.forEach { key ->
            row.addView(
                key,
                LinearLayout.LayoutParams(0, style.keyHeight, 1f).withMargins()
            )
        }
        addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, style.keyHeight).withRowMargins())
    }

    private fun addFixedRow(keys: List<View>, rowHeight: Int) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            setBackgroundColor(if (keyGap == 0) style.letterRowBackgroundColor else Color.TRANSPARENT)
            dividerDrawable = dividerDrawable()
            showDividers = if (keyGap == 0) SHOW_DIVIDER_MIDDLE else SHOW_DIVIDER_NONE
        }
        keys.forEach { key ->
            val width = (key.tag as? Int) ?: dp(32)
            row.addView(key, LinearLayout.LayoutParams(width, rowHeight).withMargins())
        }
        addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight).withRowMargins())
    }

    private fun addEdgeFilledLetterRow(keys: List<View>, rowHeight: Int) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            setBackgroundColor(if (keyGap == 0) style.letterRowBackgroundColor else Color.TRANSPARENT)
            dividerDrawable = dividerDrawable()
            showDividers = if (keyGap == 0) SHOW_DIVIDER_MIDDLE else SHOW_DIVIDER_NONE
        }
        keys.forEach { key ->
            val width = key.tag as? Int
            val params = if (width != null) {
                LinearLayout.LayoutParams(width, rowHeight).withMargins()
            } else {
                LinearLayout.LayoutParams(0, rowHeight, 1f).withMargins()
            }
            row.addView(key, params)
        }
        addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight).withRowMargins())
    }

    private fun textKey(text: String): View = baseTextKey(text, false, true) {
        listener?.onText(text)
    }

    private fun letterKey(text: String, width: Int): View {
        val value = if (uppercase) text.uppercase() else text.lowercase()
        return baseTextKey(value, false, true) {
            listener?.onText(value)
        }.withFixedWidth(width)
    }

    private fun functionTextKey(text: String, enabled: Boolean = true, flatEdge: Boolean = false, click: () -> Unit): View =
        baseTextKey(text, true, enabled, flatEdge, click)

    private fun baseTextKey(
        text: String,
        function: Boolean,
        enabled: Boolean,
        flatEdge: Boolean = false,
        click: () -> Unit
    ): AppCompatTextView {
        return AppCompatTextView(context).apply {
            this.text = text
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(style.keyTextColor)
            setBackgroundResource(
                if (keyGap == 0 && flatEdge) style.flatEdgeKeyBackgroundRes
                else if (keyGap == 0) style.flatKeyBackgroundRes
                else if (function) style.functionKeyBackgroundRes
                else style.keyBackgroundRes
            )
            isEnabled = true
            includeFontPadding = false
            minWidth = 0
            minHeight = 0
            if (enabled) {
                setOnClickListener { click() }
            } else {
                setOnClickListener { }
            }
        }
    }

    private fun iconKey(iconRes: Int, function: Boolean = true, flatEdge: Boolean = false, click: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundResource(
                if (keyGap == 0 && flatEdge) style.flatEdgeKeyBackgroundRes
                else if (keyGap == 0) style.flatKeyBackgroundRes
                else if (function) style.functionKeyBackgroundRes
                else style.keyBackgroundRes
            )
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = null
            setOnClickListener { click() }
        }
    }

    private fun deleteKey(flatEdge: Boolean = false): View {
        return iconKey(style.deleteIconRes, flatEdge = flatEdge) {}.apply {
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        listener?.onDeleteDown()
                        isPressed = true
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        listener?.onDeleteUp()
                        isPressed = false
                        true
                    }
                    else -> true
                }
            }
        }
    }

    private fun View.withFixedWidth(width: Int): View {
        tag = width
        return this
    }

    private fun LinearLayout.LayoutParams.withMargins(): LinearLayout.LayoutParams {
        val half = keyGap / 2
        setMargins(half, 0, half, 0)
        return this
    }

    private fun LayoutParams.withRowMargins(): LayoutParams {
        val half = keyGap / 2
        setMargins(0, half, 0, half)
        return this
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun dividerDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(style.dividerColor)
            setSize(dp(1), dp(1))
        }
    }
}
