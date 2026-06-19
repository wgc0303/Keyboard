package cn.wgc.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import cn.wgc.keyboard.library.R

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
    val keyboardBackgroundColor: Int
        get() = if (keyGap == 0) Color.WHITE else Color.parseColor("#F2F3F5")

    init {
        orientation = VERTICAL
        isClickable = true
        isFocusable = false
        setBackgroundColor(Color.WHITE)
        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.ck_keyboard_divider)
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
        randomNumberKeys: Boolean
    ) {
        val typeChanged = keyboardType != type
        val randomChanged = this.randomNumberKeys != randomNumberKeys
        keyboardType = type
        this.keyGap = keyGap
        this.passwordVisible = passwordVisible
        this.disableSpace = disableSpace
        this.disableDot = disableDot
        this.randomNumberKeys = randomNumberKeys
        if (shouldResetNumberKeys(typeChanged, resetAlphaMode, randomChanged)) {
            numberKeys = if (randomNumberKeys) ('0'..'9').toList().shuffled() else ('0'..'9').toList()
        }
        setBackgroundColor(keyboardBackgroundColor)
        showDividers = if (keyGap == 0) SHOW_DIVIDER_BEGINNING or SHOW_DIVIDER_MIDDLE or SHOW_DIVIDER_END else SHOW_DIVIDER_NONE
        setPadding(if (keyGap == 0) 0 else dp(6))
        alphaNumberShowingLetters = if (type == CustomKeyboardType.ALPHA_NUMBER) {
            if (typeChanged || resetAlphaMode) alphaInitialMode == AlphaKeyboardInitialMode.LETTER else alphaNumberShowingLetters
        } else {
            false
        }
        rebuildLettersWhenLaidOut = type == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters && width == 0
        rebuild()
        if (type == CustomKeyboardType.ALPHA_NUMBER && alphaNumberShowingLetters) {
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
            CustomKeyboardType.NUMBER -> iconKey(R.drawable.ic_ck_keyboard_hide) { listener?.onHide() }
            CustomKeyboardType.ID_CARD -> textKey("X")
            CustomKeyboardType.NUMBER_PASSWORD -> iconKey(
                if (passwordVisible) R.drawable.ic_invisible else R.drawable.ic_visible
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
        val rowHeight = dp(54)

        addFixedRow("QWERTYUIOP".map { letterKey(it.toString(), keyWidth) }, rowHeight)
        addFixedRow("ASDFGHJKL".map { letterKey(it.toString(), keyWidth) }, rowHeight)

        val third = mutableListOf<View>()
        val shiftIcon = if (uppercase) R.drawable.ic_ck_shift_active else R.drawable.ic_ck_shift
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
            setBackgroundColor(Color.WHITE)
            dividerDrawable = ContextCompat.getDrawable(context, R.drawable.ck_keyboard_divider)
            showDividers = if (keyGap == 0) SHOW_DIVIDER_MIDDLE else SHOW_DIVIDER_NONE
        }
        keys.forEach { key ->
            row.addView(
                key,
                LinearLayout.LayoutParams(0, dp(54), 1f).withMargins()
            )
        }
        addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).withRowMargins())
    }

    private fun addFixedRow(keys: List<View>, rowHeight: Int) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            setBackgroundColor(if (keyGap == 0) Color.parseColor("#F2F3F5") else Color.TRANSPARENT)
            dividerDrawable = ContextCompat.getDrawable(context, R.drawable.ck_keyboard_divider)
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
            setBackgroundColor(if (keyGap == 0) Color.parseColor("#F2F3F5") else Color.TRANSPARENT)
            dividerDrawable = ContextCompat.getDrawable(context, R.drawable.ck_keyboard_divider)
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
            textSize = if (text.length > 1) 15f else 20f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#111827"))
            setBackgroundResource(
                if (keyGap == 0 && flatEdge) R.drawable.ck_flat_edge_key_background
                else if (keyGap == 0) R.drawable.ck_flat_key_background
                else if (function) R.drawable.ck_function_key_background
                else R.drawable.ck_key_background
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
                if (keyGap == 0 && flatEdge) R.drawable.ck_flat_edge_key_background
                else if (keyGap == 0) R.drawable.ck_flat_key_background
                else if (function) R.drawable.ck_function_key_background
                else R.drawable.ck_key_background
            )
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = null
            setOnClickListener { click() }
        }
    }

    private fun deleteKey(flatEdge: Boolean = false): View {
        return iconKey(R.drawable.ic_delete, flatEdge = flatEdge) {}.apply {
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
}
