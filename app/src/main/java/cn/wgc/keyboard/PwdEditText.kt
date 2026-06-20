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
import cn.wgc.keyboard.CustomKeyboardType.NUMBER_PASSWORD

class PwdEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : CustomKeyboardEditText(context, attrs, defStyleAttr) {

    var passwordLength: Int = DEFAULT_PASSWORD_LENGTH
        set(value) {
            field = value.coerceAtLeast(1)
            updateLengthFilter()
            requestLayout()
            invalidate()
        }

    var boxGap: Int = dp(8)
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    var sideGap: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    var boxColor: Int = Color.parseColor("#F5F5F5")
        set(value) {
            field = value
            invalidate()
        }

    var boxCornerRadius: Float = dp(8).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    var dotRadius: Float = dp(5).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    var plainTextSize: Float = sp(18)
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    var cipherTextColor: Int = Color.parseColor("#222222")
        set(value) {
            field = value
            invalidate()
        }

    var plainTextColor: Int = Color.parseColor("#222222")
        set(value) {
            field = value
            invalidate()
        }

    var cursorStrokeColor: Int = Color.parseColor("#9E9E9E")
        set(value) {
            field = value
            invalidate()
        }

    var cursorStrokeWidth: Float = dp(1).toFloat()
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

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
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        val availableBoxAreaWidth = (contentWidth - sideGap * 2).coerceAtLeast(0)
        val boxSize = minOf(
            measuredBoxSize,
            ((availableBoxAreaWidth - totalGap()).toFloat() / passwordLength).coerceAtLeast(0f),
            contentHeight.toFloat().coerceAtLeast(0f),
        )
        val boxesWidth = boxSize * passwordLength + totalGap()
        val startX = paddingLeft + sideGap + (availableBoxAreaWidth - boxesWidth) / 2f
        val startY = paddingTop + (contentHeight - boxSize) / 2f

        boxPaint.color = boxColor
        dotPaint.color = cipherTextColor
        textPaint.color = plainTextColor
        textPaint.textSize = plainTextSize
        focusStrokePaint.color = cursorStrokeColor
        focusStrokePaint.strokeWidth = cursorStrokeWidth

        for (index in 0 until passwordLength) {
            val left = startX + index * (boxSize + boxGap)
            boxRect.set(left, startY, left + boxSize, startY + boxSize)
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
            val cursorIndex = value.length.coerceIn(0, passwordLength - 1)
            val left = startX + cursorIndex * (boxSize + boxGap)
            boxRect.set(left, startY, left + boxSize, startY + boxSize)
            drawCursor(canvas, boxRect)
        }
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

    private companion object {
        const val DEFAULT_PASSWORD_LENGTH = 8
    }
}
