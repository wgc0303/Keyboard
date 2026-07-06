package cn.wgc.keyboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
import android.view.ActionMode
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
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cn.wgc.keyboard.library.R
import java.util.WeakHashMap

object CustomKeyboardManager {
    private val controllers = WeakHashMap<FrameLayout, Controller>()
    private val dismissInstallers = WeakHashMap<Activity, OutsideDismissWindowCallback>()

    private const val HOST_WINDOW_TAG = R.id.ck_host_window

    fun showFor(editText: CustomKeyboardEditText) {
        val contentRoot = editText.findHostContentRoot() ?: return
        val controller = controllers.getOrPut(contentRoot) {
            Controller(
                contentRoot = contentRoot,
                hostActivity = editText.context.findActivity(),
                hostWindow = editText.findHostWindow(),
            )
        }
        controller.show(editText)
    }

    fun hide(editText: CustomKeyboardEditText) {
        val contentRoot = editText.findHostContentRoot() ?: return
        controllers[contentRoot]?.hide()
    }

    fun installDismissOnOutsideTouch(
        activity: Activity,
        options: KeyboardDismissOptions = KeyboardDismissOptions(),
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

    fun bindHostWindow(root: View, window: Window?) {
        root.setTag(HOST_WINDOW_TAG, window)
    }

    private fun Context.findActivity(): Activity? {
        var current = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private fun CustomKeyboardEditText.findHostContentRoot(): FrameLayout? {
        val rootContent = rootView.findViewById<View>(android.R.id.content) as? FrameLayout
        if (rootContent != null) return rootContent

        var current: View? = this
        var candidate: FrameLayout? = null
        while (current != null) {
            if (current is FrameLayout) {
                candidate = current
            }
            current = current.parent as? View
        }
        return candidate ?: context.findActivity()?.findViewById(android.R.id.content)
    }

    private fun CustomKeyboardEditText.findHostWindow(): Window? {
        var current: View? = this
        while (current != null) {
            val taggedWindow = current.getTag(HOST_WINDOW_TAG) as? Window
            if (taggedWindow != null) return taggedWindow
            current = current.parent as? View
        }
        val rootTaggedWindow = rootView.getTag(HOST_WINDOW_TAG) as? Window
        if (rootTaggedWindow != null) return rootTaggedWindow
        return context.findActivity()?.window
    }

    private class Controller(
        private val contentRoot: FrameLayout,
        hostActivity: Activity?,
        hostWindow: Window?,
    ) : CustomKeyboardView.Listener {
        private val hostWindow: Window? = hostWindow ?: hostActivity?.window
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
        private var backPressedCallback: OnBackPressedCallback? = null
        private var originalNavigationBarColor: Int? = null
        private var originalLightNavigationBarIcons: Boolean? = null

        private val gestureNavFillView = View(contentRoot.context).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            isClickable = false
        }

        @SuppressLint("ClickableViewAccessibility")
        private val touchOverlay = View(contentRoot.context).apply {
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

        private val keyboardView = CustomKeyboardView(contentRoot.context).apply {
            visibility = View.GONE
            listener = this@Controller
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOverlayBounds() }
        }

        init {
            contentRoot.addView(
                touchOverlay,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            contentRoot.addView(
                gestureNavFillView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    Gravity.BOTTOM,
                ),
            )
            contentRoot.addView(
                keyboardView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM,
                ),
            )
            ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { _, insets ->
                navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                updateKeyboardBottom()
                updateOverlayBounds()
                updateGestureNavFill()
                insets
            }
            ViewCompat.requestApplyInsets(contentRoot)
            installBackPressedCallback(hostActivity)
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
                style = editText.keyboardStyle,
            )
            hideSystemKeyboard(editText)
            keyboardView.post {
                keyboardView.ensureReadyAfterShown()
                updateOverlayBounds()
                updateGestureNavFill()
                updateTraditionalNavigationBarAppearance()
                touchOverlay.visibility = View.VISIBLE
                backPressedCallback?.isEnabled = true
                liftContentIfNeeded()
            }
        }

        fun hide() {
            stopContinuousDelete()
            keyboardView.visibility = View.GONE
            touchOverlay.visibility = View.GONE
            gestureNavFillView.visibility = View.GONE
            restoreTraditionalNavigationBarAppearance()
            backPressedCallback?.isEnabled = false
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
            editText.transformationMethod =
                if (editText.passwordVisible) null else PasswordTransformationMethod.getInstance()
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

        private fun installBackPressedCallback(activity: Activity?) {
            val componentActivity = activity as? ComponentActivity ?: return
            val activityContentRoot = componentActivity.findViewById<FrameLayout>(android.R.id.content)
            if (activityContentRoot !== contentRoot) return
            backPressedCallback = object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    hide()
                }
            }
            componentActivity.onBackPressedDispatcher.addCallback(componentActivity, backPressedCallback!!)
        }

        private fun updateGestureNavFill() {
            val params = gestureNavFillView.layoutParams as? FrameLayout.LayoutParams ?: return
            val shouldFill = keyboardView.visibility == View.VISIBLE && navBottom > 0 && !isTraditionalNavigation()
            params.gravity = Gravity.BOTTOM
            params.height = if (shouldFill) navBottom else 0
            gestureNavFillView.layoutParams = params
            gestureNavFillView.visibility = if (shouldFill) View.VISIBLE else View.GONE
            gestureNavFillView.setBackgroundColor(keyboardView.systemNavFillColor)
        }

        private fun updateTraditionalNavigationBarAppearance() {
            if (keyboardView.visibility != View.VISIBLE || navBottom <= 0 || !isTraditionalNavigation()) return
            val window = hostWindow ?: return
            if (originalNavigationBarColor == null) {
                originalNavigationBarColor = window.navigationBarColor
                originalLightNavigationBarIcons =
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars
            }
            window.navigationBarColor = keyboardView.systemNavFillColor
        }

        private fun restoreTraditionalNavigationBarAppearance() {
            val window = hostWindow ?: return
            originalNavigationBarColor?.let { color ->
                window.navigationBarColor = color
                originalLightNavigationBarIcons?.let { isLight ->
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = isLight
                }
            }
            originalNavigationBarColor = null
            originalLightNavigationBarIcons = null
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
                event.rawY - location[1],
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
                event.rawY - location[1],
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

        private fun isTraditionalNavigation(): Boolean {
            if (navBottom <= 0) return false
            return (hostWindow?.let { window ->
                val rootInsets = ViewCompat.getRootWindowInsets(window.decorView) ?: return@let false
                rootInsets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom > 0
            } ?: false) || (
                ViewCompat.getRootWindowInsets(contentRoot)
                    ?.getInsets(WindowInsetsCompat.Type.tappableElement())
                    ?.bottom ?: 0
                ) > 0
        }
    }

    private class OutsideDismissWindowCallback(
        private val activity: Activity,
        private val delegate: Window.Callback,
        var options: KeyboardDismissOptions,
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
