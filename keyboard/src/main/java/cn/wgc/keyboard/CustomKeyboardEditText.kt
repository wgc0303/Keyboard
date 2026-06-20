package cn.wgc.keyboard

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import cn.wgc.keyboard.library.R

open class CustomKeyboardEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var keyboardInputFilter: InputFilter? = null

    /** 键盘类型，对应 XML 属性 ck_keyboardType。 */
    var keyboardType: CustomKeyboardType = CustomKeyboardType.NUMBER
        set(value) {
            field = value
            configureInputType()
            applyKeyboardFilter()
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /** 键盘按键间距，对应 XML 属性 ck_keyGap。 */
    var keyGap: Int = 0
        set(value) {
            field = value
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /** 字母数字键盘是否禁用空格输入，对应 XML 属性 ck_disableSpace。 */
    var disableSpace: Boolean = false
        set(value) {
            field = value
            applyKeyboardFilter()
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /** 字母数字键盘是否禁用点号输入，对应 XML 属性 ck_disableDot。 */
    var disableDot: Boolean = false
        set(value) {
            field = value
            applyKeyboardFilter()
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /** 字母数字键盘首次弹出时显示数字还是字母面板，对应 XML 属性 ck_alphaInitialMode。 */
    var alphaInitialMode: AlphaKeyboardInitialMode = AlphaKeyboardInitialMode.NUMBER
        set(value) {
            field = value
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /** 数字密码键盘是否随机打乱 0-9 数字键，对应 XML 属性 ck_randomNumberKeys。 */
    var randomNumberKeys: Boolean = false
        set(value) {
            field = value
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    /**
     * 键盘外观配置，对应 XML 中的颜色、尺寸、背景和图标类 ck_* 属性。
     *
     * 包含 ck_keyboardBackgroundColor、ck_spacedKeyboardBackgroundColor、ck_letterRowBackgroundColor、
     * ck_keyTextColor、ck_dividerColor、ck_keyHeight、ck_panelPaddingWhenSpaced、
     * ck_keyBackground、ck_functionKeyBackground、ck_flatKeyBackground、ck_flatEdgeKeyBackground、
     * ck_deleteIcon、ck_visibleIcon、ck_invisibleIcon、ck_shiftIcon、ck_shiftActiveIcon、ck_hideKeyboardIcon。
     */
    var keyboardStyle: CustomKeyboardStyle = CustomKeyboardStyle.default(context)
        set(value) {
            field = value
            if (hasFocus()) CustomKeyboardManager.showFor(this)
        }

    var passwordVisible: Boolean = false
        internal set

    init {
        keyboardInputFilter = KeyboardInputFilter()
        val array = context.obtainStyledAttributes(attrs, R.styleable.CustomKeyboardEditText, defStyleAttr, 0)
        val defaultStyle = CustomKeyboardStyle.default(context)
        keyboardType = CustomKeyboardType.fromAttr(array.getInt(R.styleable.CustomKeyboardEditText_ck_keyboardType, 0))
        keyGap = array.getDimensionPixelSize(R.styleable.CustomKeyboardEditText_ck_keyGap, 0)
        disableSpace = array.getBoolean(R.styleable.CustomKeyboardEditText_ck_disableSpace, false)
        disableDot = array.getBoolean(R.styleable.CustomKeyboardEditText_ck_disableDot, false)
        alphaInitialMode = AlphaKeyboardInitialMode.fromAttr(
            array.getInt(R.styleable.CustomKeyboardEditText_ck_alphaInitialMode, 0)
        )
        randomNumberKeys = array.getBoolean(R.styleable.CustomKeyboardEditText_ck_randomNumberKeys, false)
        keyboardStyle = CustomKeyboardStyle(
            keyboardBackgroundColor = array.getColor(
                R.styleable.CustomKeyboardEditText_ck_keyboardBackgroundColor,
                defaultStyle.keyboardBackgroundColor
            ),
            spacedKeyboardBackgroundColor = array.getColor(
                R.styleable.CustomKeyboardEditText_ck_spacedKeyboardBackgroundColor,
                defaultStyle.spacedKeyboardBackgroundColor
            ),
            letterRowBackgroundColor = array.getColor(
                R.styleable.CustomKeyboardEditText_ck_letterRowBackgroundColor,
                defaultStyle.letterRowBackgroundColor
            ),
            keyTextColor = array.getColor(
                R.styleable.CustomKeyboardEditText_ck_keyTextColor,
                defaultStyle.keyTextColor
            ),
            dividerColor = array.getColor(
                R.styleable.CustomKeyboardEditText_ck_dividerColor,
                defaultStyle.dividerColor
            ),
            keyHeight = array.getDimensionPixelSize(
                R.styleable.CustomKeyboardEditText_ck_keyHeight,
                defaultStyle.keyHeight
            ),
            panelPaddingWhenSpaced = array.getDimensionPixelSize(
                R.styleable.CustomKeyboardEditText_ck_panelPaddingWhenSpaced,
                defaultStyle.panelPaddingWhenSpaced
            ),
            keyBackgroundRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_keyBackground,
                defaultStyle.keyBackgroundRes
            ),
            functionKeyBackgroundRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_functionKeyBackground,
                defaultStyle.functionKeyBackgroundRes
            ),
            flatKeyBackgroundRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_flatKeyBackground,
                defaultStyle.flatKeyBackgroundRes
            ),
            flatEdgeKeyBackgroundRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_flatEdgeKeyBackground,
                defaultStyle.flatEdgeKeyBackgroundRes
            ),
            deleteIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_deleteIcon,
                defaultStyle.deleteIconRes
            ),
            visibleIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_visibleIcon,
                defaultStyle.visibleIconRes
            ),
            invisibleIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_invisibleIcon,
                defaultStyle.invisibleIconRes
            ),
            shiftIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_shiftIcon,
                defaultStyle.shiftIconRes
            ),
            shiftActiveIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_shiftActiveIcon,
                defaultStyle.shiftActiveIconRes
            ),
            hideKeyboardIconRes = array.getResourceId(
                R.styleable.CustomKeyboardEditText_ck_hideKeyboardIcon,
                defaultStyle.hideKeyboardIconRes
            )
        )
        array.recycle()

        showSoftInputOnFocus = false
        configureInputType()
        applyKeyboardFilter()
    }

    override fun setFilters(filters: Array<out InputFilter>?) {
        val keyboardFilter = keyboardInputFilter
        if (keyboardFilter == null) {
            super.setFilters(filters)
            return
        }
        val externalFilters = filters
            ?.filterNot { it === keyboardFilter }
            ?.toTypedArray()
            ?: emptyArray()
        super.setFilters(externalFilters + keyboardFilter)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            CustomKeyboardManager.showFor(this)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        showSoftInputOnFocus = false
        val handled = super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP && hasFocus()) {
            CustomKeyboardManager.showFor(this)
        }
        return handled
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        showSoftInputOnFocus = false
        if (hasWindowFocus && hasFocus()) {
            hideSystemKeyboard()
            postDelayed({ hideSystemKeyboard() }, 80L)
            CustomKeyboardManager.showFor(this)
        }
    }

    private fun configureInputType() {
        inputType = when (keyboardType) {
            CustomKeyboardType.NUMBER_PASSWORD -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            CustomKeyboardType.ALPHA_NUMBER -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        showSoftInputOnFocus = false
    }

    private fun applyKeyboardFilter() {
        filters = filters
    }

    private fun hideSystemKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private inner class KeyboardInputFilter : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            if (source.isEmpty()) return null

            val builder = StringBuilder(end - start)
            var changed = false
            for (index in start until end) {
                val char = source[index]
                val accepted = normalizeAcceptedChar(char)
                if (accepted == null) {
                    changed = true
                } else {
                    builder.append(accepted)
                    if (accepted != char) changed = true
                }
            }

            return if (!changed) null else builder.toString()
        }

        private fun normalizeAcceptedChar(char: Char): Char? {
            return when (keyboardType) {
                CustomKeyboardType.NUMBER,
                CustomKeyboardType.NUMBER_PASSWORD -> char.takeIf { it in '0'..'9' }

                CustomKeyboardType.ID_CARD -> when {
                    char in '0'..'9' -> char
                    char == 'x' || char == 'X' -> 'X'
                    else -> null
                }

                CustomKeyboardType.ALPHA_NUMBER -> when {
                    char in '0'..'9' -> char
                    char in 'a'..'z' || char in 'A'..'Z' -> char
                    char == ' ' && !disableSpace -> char
                    char == '.' && !disableDot -> char
                    else -> null
                }
            }
        }
    }
}
