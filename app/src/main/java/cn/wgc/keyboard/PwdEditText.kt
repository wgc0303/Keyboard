package cn.wgc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.InputFilter
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import cn.wgc.keyboard.CustomKeyboardType.NUMBER_PASSWORD

class PwdEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : CustomKeyboardEditText(context, attrs, defStyleAttr) {

    /** 密码长度，对应 XML 属性 pwd_length。 */
    var passwordLength: Int = DEFAULT_PASSWORD_LENGTH
        set(value) {
            field = value.coerceAtLeast(1)
            updateLengthFilter()
            requestLayout()
            invalidate()
        }

    /** 相邻两个密码框之间的间距，对应 XML 属性 pwd_boxGap。 */
    var boxGap: Int = dp(8)
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    /** 第一个/最后一个密码框到控件左右边缘的预留间距，对应 XML 属性 pwd_sideGap。 */
    var sideGap: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    /** 密码框填充颜色，对应 XML 属性 pwd_boxColor。 */
    var boxColor: Int = Color.parseColor("#F5F5F5")
        set(value) {
            field = value
            invalidate()
        }

    /** 密码框圆角半径，对应 XML 属性 pwd_boxCornerRadius。 */
    var boxCornerRadius: Float = dp(8).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    /** 密文圆点半径，对应 XML 属性 pwd_dotRadius。 */
    var dotRadius: Float = dp(5).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    /** 明文显示时的字体大小，对应 XML 属性 pwd_plainTextSize。 */
    var plainTextSize: Float = sp(18)
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    /** 密文圆点颜色，对应 XML 属性 pwd_cipherTextColor。 */
    var cipherTextColor: Int = Color.parseColor("#222222")
        set(value) {
            field = value
            invalidate()
        }

    /** 明文显示时的字符颜色，对应 XML 属性 pwd_plainTextColor。 */
    var plainTextColor: Int = Color.parseColor("#222222")
        set(value) {
            field = value
            invalidate()
        }

    /** 光标框描边颜色，对应 XML 属性 pwd_cursorStrokeColor。 */
    var cursorStrokeColor: Int = Color.parseColor("#9E9E9E")
        set(value) {
            field = value
            invalidate()
        }

    /** 光标框描边宽度，对应 XML 属性 pwd_cursorStrokeWidth。 */
    var cursorStrokeWidth: Float = dp(1).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    /** 是否允许点击密码框定位并替换单个字符，对应 XML 属性 pwd_enableCursorPositionByTouch。 */
    var enableCursorPositionByTouch: Boolean = false

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val focusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val boxRect = RectF()
    private val cursorRect = RectF()
    private val textBounds = Rect()
    private var measuredBoxSize = dp(52).toFloat()
    private var pendingTouchCursorIndex: Int? = null

    init {
        keyboardType = NUMBER_PASSWORD
        isCursorVisible = false
        background = null
        includeFontPadding = false
        minWidth = 0
        minHeight = 0
        minimumWidth = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
        transformationMethod = PasswordTransformationMethod.getInstance()

        val array = context.obtainStyledAttributes(attrs, R.styleable.PwdEditText, defStyleAttr, 0)
        passwordLength = array.getInt(R.styleable.PwdEditText_pwd_length, DEFAULT_PASSWORD_LENGTH).coerceAtLeast(1)
        boxGap = array.getDimensionPixelSize(R.styleable.PwdEditText_pwd_boxGap, dp(8))
        sideGap = array.getDimensionPixelSize(R.styleable.PwdEditText_pwd_sideGap, 0)
        boxColor = array.getColor(R.styleable.PwdEditText_pwd_boxColor, boxColor)
        boxCornerRadius = array.getDimension(R.styleable.PwdEditText_pwd_boxCornerRadius, boxCornerRadius)
        dotRadius = array.getDimension(R.styleable.PwdEditText_pwd_dotRadius, dotRadius)
        plainTextSize = array.getDimension(R.styleable.PwdEditText_pwd_plainTextSize, plainTextSize)
        cipherTextColor = array.getColor(R.styleable.PwdEditText_pwd_cipherTextColor, cipherTextColor)
        plainTextColor = array.getColor(R.styleable.PwdEditText_pwd_plainTextColor, plainTextColor)
        cursorStrokeColor = array.getColor(R.styleable.PwdEditText_pwd_cursorStrokeColor, cursorStrokeColor)
        cursorStrokeWidth = array.getDimension(R.styleable.PwdEditText_pwd_cursorStrokeWidth, cursorStrokeWidth)
        enableCursorPositionByTouch = array.getBoolean(
            R.styleable.PwdEditText_pwd_enableCursorPositionByTouch,
            false,
        )
        array.recycle()

        updateLengthFilter()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val horizontalPadding = paddingLeft + paddingRight
        val verticalPadding = paddingTop + paddingBottom

        val defaultBoxSize = dp(52)
        val heightBasedBoxSize = (heightSize - verticalPadding).toFloat().coerceAtLeast(0f)
        val heightBasedWidth = measureWidthForBoxSize(heightBasedBoxSize, horizontalPadding)
        val shouldFitByWidth = when {
            widthMode == MeasureSpec.EXACTLY -> true
            heightMode == MeasureSpec.EXACTLY && widthMode == MeasureSpec.AT_MOST -> heightBasedWidth > widthSize
            else -> false
        }
        val boxSize = when {
            shouldFitByWidth -> boxSizeForWidth(widthSize, horizontalPadding)
            heightMode == MeasureSpec.EXACTLY -> heightBasedBoxSize
            else -> defaultBoxSize.toFloat()
        }
        measuredBoxSize = boxSize

        val desiredWidth = measureWidthForBoxSize(boxSize, horizontalPadding)
        val desiredHeight = (boxSize + verticalPadding).toInt()
        val measuredWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }
        val measuredHeight = if (
            heightMode == MeasureSpec.EXACTLY &&
            !(widthMode == MeasureSpec.AT_MOST && shouldFitByWidth)
        ) {
            heightSize
        } else {
            when (heightMode) {
                MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
                else -> desiredHeight
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val value = text?.toString().orEmpty()
        val isPlainText = passwordVisible
        val layout = calculateBoxLayout()

        boxPaint.color = boxColor
        dotPaint.color = cipherTextColor
        textPaint.color = plainTextColor
        textPaint.textSize = plainTextSize
        focusStrokePaint.color = cursorStrokeColor
        focusStrokePaint.strokeWidth = cursorStrokeWidth

        for (index in 0 until passwordLength) {
            boxRect.set(layout.rectFor(index))
            canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint)

            if (index < value.length) {
                if (isPlainText) {
                    drawPlainText(canvas, value[index].toString(), boxRect)
                } else {
                    canvas.drawCircle(boxRect.centerX(), boxRect.centerY(), dotRadius, dotPaint)
                }
            }
        }

        if (hasFocus()) {
            val cursorIndex = selectionStart.coerceAtLeast(0).coerceIn(0, passwordLength - 1)
            boxRect.set(layout.rectFor(cursorIndex))
            drawCursor(canvas, boxRect)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (enableCursorPositionByTouch && event.actionMasked == MotionEvent.ACTION_DOWN) {
            pendingTouchCursorIndex = findTouchedCursorIndex(event.x, event.y)
        }
        val handled = super.onTouchEvent(event)
        if (enableCursorPositionByTouch && event.actionMasked == MotionEvent.ACTION_UP) {
            pendingTouchCursorIndex?.let(::applyTouchCursorIndex)
            pendingTouchCursorIndex = null
        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            pendingTouchCursorIndex = null
        }
        return handled
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        invalidate()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        invalidate()
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        invalidate()
    }

    private fun drawPlainText(canvas: Canvas, text: String, rect: RectF) {
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val baseline = rect.centerY() - textBounds.exactCenterY()
        canvas.drawText(text, rect.centerX(), baseline, textPaint)
    }

    private fun drawCursor(canvas: Canvas, rect: RectF) {
        if (cursorStrokeWidth <= 0f) return
        val inset = cursorStrokeWidth / 2f
        cursorRect.set(rect)
        cursorRect.inset(inset, inset)
        val radius = (boxCornerRadius - inset).coerceAtLeast(0f)
        canvas.drawRoundRect(cursorRect, radius, radius, focusStrokePaint)
    }

    private fun findTouchedCursorIndex(x: Float, y: Float): Int {
        val layout = calculateBoxLayout()
        if (y < layout.startY || y > layout.startY + layout.boxSize) {
            return textLength()
        }

        for (index in 0 until passwordLength) {
            if (layout.rectFor(index).contains(x, y)) {
                return index
            }
        }
        return textLength()
    }

    private fun applyTouchCursorIndex(index: Int) {
        val length = textLength()
        val target = index.coerceIn(0, length)
        if (target < length) {
            setSelection(target, target + 1)
        } else {
            setSelection(length)
        }
    }

    private fun textLength(): Int {
        return text?.length ?: 0
    }

    private fun calculateBoxLayout(): BoxLayout {
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        val availableBoxAreaWidth = (contentWidth - sideGap * 2).coerceAtLeast(0)
        val boxSize = minOf(
            measuredBoxSize,
            ((availableBoxAreaWidth - totalGap()).toFloat() / passwordLength).coerceAtLeast(0f),
            contentHeight.toFloat().coerceAtLeast(0f),
        )
        val boxesWidth = boxSize * passwordLength + totalGap()
        return BoxLayout(
            boxSize = boxSize,
            startX = paddingLeft + sideGap + (availableBoxAreaWidth - boxesWidth) / 2f,
            startY = paddingTop + (contentHeight - boxSize) / 2f,
        )
    }

    private fun updateLengthFilter() {
        filters = arrayOf(InputFilter.LengthFilter(passwordLength))
    }

    private fun totalGap(): Int {
        return boxGap * (passwordLength - 1)
    }

    private fun totalSideGap(): Int {
        return sideGap * 2
    }

    private fun measureWidthForBoxSize(boxSize: Float, horizontalPadding: Int): Int {
        return (boxSize * passwordLength + totalGap() + totalSideGap() + horizontalPadding).toInt()
    }

    private fun boxSizeForWidth(widthSize: Int, horizontalPadding: Int): Float {
        return ((widthSize - horizontalPadding - totalSideGap() - totalGap()).toFloat() / passwordLength)
            .coerceAtLeast(0f)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun sp(value: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value.toFloat(),
            resources.displayMetrics,
        )
    }

    private inner class BoxLayout(
        val boxSize: Float,
        val startX: Float,
        val startY: Float,
    ) {
        fun rectFor(index: Int): RectF {
            val left = startX + index * (boxSize + boxGap)
            return RectF(left, startY, left + boxSize, startY + boxSize)
        }
    }

    private companion object {
        const val DEFAULT_PASSWORD_LENGTH = 8
    }
}
