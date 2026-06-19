package cn.wgc.keyboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap

object CustomKeyboardManager {
    private val controllers = WeakHashMap<Activity, Controller>()
    private val dismissInstallers = WeakHashMap<Activity, OutsideDismissWindowCallback>()

    fun showFor(editText: CustomKeyboardEditText) {
        val activity = editText.context.findActivity() ?: return
        val controller = controllers.getOrPut(activity) { Controller(activity) }
        controller.show(editText)
    }

    fun hide(editText: CustomKeyboardEditText) {
        val activity = editText.context.findActivity() ?: return
        controllers[activity]?.hide()
    }

    fun installDismissOnOutsideTouch(
        activity: Activity,
        options: KeyboardDismissOptions = KeyboardDismissOptions()
    ) {
        val existing = dismissInstallers[activity]
        if (existing != null) {
            existing.options = options
            return
        }

        val callback = OutsideDismissWindowCallback(activity, activity.window.callback, options)
        activity.window.callback = callback
        dismissInstallers[activity] = callback
    }

    private fun Context.findActivity(): Activity? {
        var current = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private class Controller(activity: Activity) : CustomKeyboardView.Listener {
        private val contentRoot = activity.findViewById<FrameLayout>(android.R.id.content)
        private val pageContent: View? = contentRoot.getChildAt(0)
        private val handler = Handler(Looper.getMainLooper())
        private val deleteRunnable = object : Runnable {
            override fun run() {
                deleteOnce()
                handler.postDelayed(this, 55L)
            }
        }
        private var activeEditText: CustomKeyboardEditText? = null
        private var navBottom = 0
        private var forwardingEditText: CustomKeyboardEditText? = null
        private var forwardingSystemEditText: EditText? = null

        private val gestureNavFillView = View(activity).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            isClickable = false
        }

        private val touchOverlay = View(activity).apply {
            visibility = View.GONE
            isClickable = true
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                val action = event.actionMasked
                if (action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    forwardingEditText = activeEditText
                        ?.takeIf { it.containsRawPoint(x, y) }
                        ?: pageContent?.findKeyboardEditTextAt(x, y)
                    forwardingSystemEditText = if (forwardingEditText == null) {
                        pageContent?.findSystemEditTextAt(x, y)
                    } else {
                        null
                    }
                }

                val target = forwardingEditText
                if (target != null) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        target.requestFocus()
                        show(target)
                    }
                    target.dispatchTouchFromOverlay(event)
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        forwardingEditText = null
                    }
                } else if (forwardingSystemEditText != null) {
                    val systemTarget = forwardingSystemEditText
                    if (action == MotionEvent.ACTION_DOWN) {
                        hide()
                        systemTarget?.requestFocus()
                    }
                    systemTarget?.dispatchTouchFromOverlay(event)
                    if (action == MotionEvent.ACTION_UP) {
                        systemTarget?.post {
                            val inputMethodManager =
                                systemTarget.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            inputMethodManager?.showSoftInput(systemTarget, 0)
                        }
                        forwardingSystemEditText = null
                    } else if (action == MotionEvent.ACTION_CANCEL) {
                        forwardingSystemEditText = null
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    activeEditText?.clearFocus()
                    hide()
                }
                true
            }
        }

        private val keyboardView = CustomKeyboardView(activity).apply {
            visibility = View.GONE
            listener = this@Controller
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOverlayBounds() }
        }

        init {
            contentRoot.addView(
                touchOverlay,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            contentRoot.addView(
                gestureNavFillView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    Gravity.BOTTOM
                )
            )
            contentRoot.addView(
                keyboardView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
            ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { _, insets ->
                navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                updateKeyboardBottom()
                updateOverlayBounds()
                updateGestureNavFill()
                insets
            }
            ViewCompat.requestApplyInsets(contentRoot)
        }

        fun show(editText: CustomKeyboardEditText) {
            val targetChanged = activeEditText !== editText
            activeEditText = editText
            updateKeyboardBottom()
            keyboardView.visibility = View.VISIBLE
            keyboardView.configure(
                type = editText.keyboardType,
                keyGap = editText.keyGap,
                passwordVisible = editText.passwordVisible,
                disableSpace = editText.disableSpace,
                disableDot = editText.disableDot,
                alphaInitialMode = editText.alphaInitialMode,
                resetAlphaMode = targetChanged,
                randomNumberKeys = editText.randomNumberKeys,
                style = editText.keyboardStyle
            )
            hideSystemKeyboard(editText)
            keyboardView.post {
                keyboardView.ensureReadyAfterShown()
                updateOverlayBounds()
                updateGestureNavFill()
                touchOverlay.visibility = View.VISIBLE
                liftContentIfNeeded()
            }
        }

        fun hide() {
            stopContinuousDelete()
            keyboardView.visibility = View.GONE
            touchOverlay.visibility = View.GONE
            gestureNavFillView.visibility = View.GONE
            activeEditText = null
            pageContent?.animate()?.translationY(0f)?.setDuration(120L)?.start()
        }

        override fun onText(text: String) {
            val editText = activeEditText ?: return
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            val min = minOf(start, end)
            val max = maxOf(start, end)
            editText.text?.replace(min, max, text)
        }

        override fun onDeleteDown() {
            deleteOnce()
            handler.postDelayed(deleteRunnable, 420L)
        }

        override fun onDeleteUp() {
            stopContinuousDelete()
        }

        override fun onHide() {
            activeEditText?.clearFocus()
            hide()
        }

        override fun onTogglePassword() {
            val editText = activeEditText ?: return
            editText.passwordVisible = !editText.passwordVisible
            editText.transformationMethod = if (editText.passwordVisible) null else PasswordTransformationMethod.getInstance()
            editText.setSelection(editText.text?.length ?: 0)
            keyboardView.setPasswordVisible(editText.passwordVisible)
        }

        override fun onKeyboardChanged() {
            keyboardView.post { liftContentIfNeeded() }
        }

        private fun deleteOnce() {
            val editText = activeEditText ?: return
            val text = editText.text ?: return
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            if (start != end) {
                text.delete(minOf(start, end), maxOf(start, end))
            } else if (start > 0) {
                text.delete(start - 1, start)
            }
        }

        private fun stopContinuousDelete() {
            handler.removeCallbacks(deleteRunnable)
        }

        private fun updateKeyboardBottom() {
            val params = keyboardView.layoutParams as? FrameLayout.LayoutParams ?: return
            params.gravity = Gravity.BOTTOM
            params.bottomMargin = navBottom
            keyboardView.layoutParams = params
        }

        private fun updateGestureNavFill() {
            val params = gestureNavFillView.layoutParams as? FrameLayout.LayoutParams ?: return
            val shouldFill = keyboardView.visibility == View.VISIBLE && navBottom > 0 && navBottom <= dp(32)
            params.gravity = Gravity.BOTTOM
            params.height = if (shouldFill) navBottom else 0
            gestureNavFillView.layoutParams = params
            gestureNavFillView.visibility = if (shouldFill) View.VISIBLE else View.GONE
            gestureNavFillView.setBackgroundColor(keyboardView.keyboardBackgroundColor)
        }

        private fun updateOverlayBounds() {
            val params = touchOverlay.layoutParams as? FrameLayout.LayoutParams ?: return
            params.gravity = Gravity.TOP
            params.bottomMargin = if (keyboardView.visibility == View.VISIBLE) {
                navBottom + keyboardView.height
            } else {
                0
            }
            touchOverlay.layoutParams = params
        }

        private fun liftContentIfNeeded() {
            val editText = activeEditText ?: return
            if (keyboardView.visibility != View.VISIBLE || keyboardView.height == 0) return

            val keyboardRect = Rect()
            val editRect = Rect()
            keyboardView.getGlobalVisibleRect(keyboardRect)
            editText.getGlobalVisibleRect(editRect)
            val contentTranslation = pageContent?.translationY ?: 0f
            val editOriginalBottom = editRect.bottom - contentTranslation
            val overlap = editOriginalBottom + dp(12) - keyboardRect.top
            val target = if (overlap > 0) -overlap.toFloat() else 0f
            pageContent?.animate()?.translationY(target)?.setDuration(160L)?.start()
        }

        private fun View.containsRawPoint(x: Int, y: Int): Boolean {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            return rect.contains(x, y)
        }

        private fun View.findKeyboardEditTextAt(x: Int, y: Int): CustomKeyboardEditText? {
            if (!containsRawPoint(x, y)) return null
            if (this is CustomKeyboardEditText) return this
            if (this is ViewGroup) {
                for (index in childCount - 1 downTo 0) {
                    val found = getChildAt(index).findKeyboardEditTextAt(x, y)
                    if (found != null) return found
                }
            }
            return null
        }

        private fun View.findSystemEditTextAt(x: Int, y: Int): EditText? {
            if (!containsRawPoint(x, y)) return null
            if (this is EditText && this !is CustomKeyboardEditText) return this
            if (this is ViewGroup) {
                for (index in childCount - 1 downTo 0) {
                    val found = getChildAt(index).findSystemEditTextAt(x, y)
                    if (found != null) return found
                }
            }
            return null
        }

        private fun CustomKeyboardEditText.dispatchTouchFromOverlay(event: MotionEvent) {
            val location = IntArray(2)
            getLocationOnScreen(location)
            val forwarded = MotionEvent.obtain(event)
            forwarded.setLocation(
                event.rawX - location[0],
                event.rawY - location[1]
            )
            dispatchTouchEvent(forwarded)
            forwarded.recycle()
        }

        private fun EditText.dispatchTouchFromOverlay(event: MotionEvent) {
            val location = IntArray(2)
            getLocationOnScreen(location)
            val forwarded = MotionEvent.obtain(event)
            forwarded.setLocation(
                event.rawX - location[0],
                event.rawY - location[1]
            )
            dispatchTouchEvent(forwarded)
            forwarded.recycle()
        }

        private fun hideSystemKeyboard(editText: CustomKeyboardEditText) {
            editText.showSoftInputOnFocus = false
            val inputMethodManager = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(editText.windowToken, 0)
            handler.postDelayed({
                inputMethodManager?.hideSoftInputFromWindow(editText.windowToken, 0)
            }, 80L)
        }

        private fun dp(value: Int): Int {
            val density = keyboardView.resources.displayMetrics.density
            return (value * density + 0.5f).toInt()
        }
    }

    private class OutsideDismissWindowCallback(
        private val activity: Activity,
        private val delegate: Window.Callback,
        var options: KeyboardDismissOptions
    ) : Window.Callback {

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                handleOutsideTouch(event)
            }
            return delegate.dispatchTouchEvent(event)
        }

        private fun handleOutsideTouch(event: MotionEvent) {
            val focusedView = activity.currentFocus ?: return
            if (focusedView !is EditText || focusedView is CustomKeyboardEditText) return
            if (!options.dismissSystemKeyboard) return

            val x = event.rawX.toInt()
            val y = event.rawY.toInt()
            val navBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                ?.bottom ?: 0
            val inNavigationBar = navBottom > 0 && y >= activity.window.decorView.height - navBottom
            if (inNavigationBar || focusedView.containsRawPoint(x, y)) return

            val inputMethodManager = focusedView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(focusedView.windowToken, 0)
            if (options.clearSystemEditTextFocus) {
                focusedView.clearFocus()
            }
        }

        private fun View.containsRawPoint(x: Int, y: Int): Boolean {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            return rect.contains(x, y)
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean = delegate.dispatchKeyEvent(event)
        override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean = delegate.dispatchKeyShortcutEvent(event)
        override fun dispatchTrackballEvent(event: MotionEvent): Boolean = delegate.dispatchTrackballEvent(event)
        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean = delegate.dispatchGenericMotionEvent(event)
        override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean =
            delegate.dispatchPopulateAccessibilityEvent(event)

        override fun onCreatePanelView(featureId: Int): View? = delegate.onCreatePanelView(featureId)
        override fun onCreatePanelMenu(featureId: Int, menu: android.view.Menu): Boolean =
            delegate.onCreatePanelMenu(featureId, menu)

        override fun onPreparePanel(featureId: Int, view: View?, menu: android.view.Menu): Boolean =
            delegate.onPreparePanel(featureId, view, menu)

        override fun onMenuOpened(featureId: Int, menu: android.view.Menu): Boolean = delegate.onMenuOpened(featureId, menu)
        override fun onMenuItemSelected(featureId: Int, item: android.view.MenuItem): Boolean =
            delegate.onMenuItemSelected(featureId, item)

        override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams) =
            delegate.onWindowAttributesChanged(attrs)

        override fun onContentChanged() = delegate.onContentChanged()
        override fun onWindowFocusChanged(hasFocus: Boolean) = delegate.onWindowFocusChanged(hasFocus)
        override fun onAttachedToWindow() = delegate.onAttachedToWindow()
        override fun onDetachedFromWindow() = delegate.onDetachedFromWindow()
        override fun onPanelClosed(featureId: Int, menu: android.view.Menu) = delegate.onPanelClosed(featureId, menu)
        override fun onSearchRequested(): Boolean = delegate.onSearchRequested()
        override fun onSearchRequested(searchEvent: SearchEvent): Boolean = delegate.onSearchRequested(searchEvent)
        override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode? =
            delegate.onWindowStartingActionMode(callback)

        override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int): ActionMode? =
            delegate.onWindowStartingActionMode(callback, type)

        override fun onActionModeStarted(mode: ActionMode) = delegate.onActionModeStarted(mode)
        override fun onActionModeFinished(mode: ActionMode) = delegate.onActionModeFinished(mode)
    }
}
