package cn.wgc.keyboard

import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object EdgeToEdgeHelper {

    fun apply(
        activity: ComponentActivity,
        root: View,
        config: Config = Config(),
    ) {
        activity.enableEdgeToEdge(
            statusBarStyle = config.statusBarStyle(),
            navigationBarStyle = config.navigationBarStyle(),
        )
        activity.window.statusBarColor = config.statusBarColor
        activity.window.navigationBarColor = config.navigationBarColor
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = config.darkStatusBarIcons
            isAppearanceLightNavigationBars = config.darkNavigationBarIcons
        }

        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        var wasImeVisible = false
        var scrollYBeforeIme = 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val tappableInsets = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = config.applyImeInset && insets.isVisible(WindowInsetsCompat.Type.ime())

            val leftInset = if (config.applyHorizontalInsets) maxOf(statusInsets.left, navigationInsets.left) else 0
            val rightInset = if (config.applyHorizontalInsets) maxOf(statusInsets.right, navigationInsets.right) else 0
            val topInset = if (config.applyTopInset) statusInsets.top else 0
            val navigationBottomInset = when (config.bottomInsetMode) {
                BottomInsetMode.ALWAYS -> navigationInsets.bottom
                BottomInsetMode.NEVER -> 0
                BottomInsetMode.ONLY_TRADITIONAL_NAVIGATION -> {
                    if (isTraditionalNavigation(navigationInsets.bottom, tappableInsets.bottom)) {
                        navigationInsets.bottom
                    } else {
                        0
                    }
                }
            }
            val imeBottomInset = if (isImeVisible) {
                imeInsets.bottom
            } else {
                0
            }
            val bottomInset = maxOf(navigationBottomInset, imeBottomInset)
            if (isImeVisible && !wasImeVisible) {
                scrollYBeforeIme = view.scrollY
            }

            view.setPadding(
                initialLeft + leftInset,
                initialTop + topInset,
                initialRight + rightInset,
                initialBottom + bottomInset,
            )
            if (imeBottomInset > 0) {
                scrollFocusedChildIntoImeSafeArea(view, imeBottomInset)
            } else if (wasImeVisible && !isImeVisible && view is ScrollView) {
                view.post {
                    view.smoothScrollTo(0, scrollYBeforeIme)
                }
            }
            wasImeVisible = isImeVisible
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun isTraditionalNavigation(navigationBottom: Int, tappableBottom: Int): Boolean {
        return navigationBottom > 0 && tappableBottom > 0
    }

    private fun scrollFocusedChildIntoImeSafeArea(root: View, imeBottomInset: Int) {
        val focused = root.findFocus() ?: return
        if (focused == root || !isDescendantOf(root, focused)) return
        root.post {
            val rootRect = Rect()
            val focusedRect = Rect()
            root.getGlobalVisibleRect(rootRect)
            focused.getGlobalVisibleRect(focusedRect)

            val visibleBottom = rootRect.bottom - imeBottomInset
            val overlap = focusedRect.bottom + root.dp(12) - visibleBottom
            if (overlap > 0 && root is ScrollView) {
                root.smoothScrollBy(0, overlap)
            } else {
                focused.requestRectangleOnScreen(
                    Rect(0, 0, focused.width, focused.height),
                    true,
                )
            }
        }
    }

    private fun isDescendantOf(parent: View, child: View): Boolean {
        var current = child.parent
        while (current is View) {
            if (current == parent) return true
            current = current.parent
        }
        return false
    }

    private fun View.dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    data class Config(
        @ColorInt val statusBarColor: Int = Color.WHITE,
        val darkStatusBarIcons: Boolean = true,
        @ColorInt val navigationBarColor: Int = Color.WHITE,
        val darkNavigationBarIcons: Boolean = true,
        val applyTopInset: Boolean = true,
        val applyHorizontalInsets: Boolean = true,
        val bottomInsetMode: BottomInsetMode = BottomInsetMode.ALWAYS,
        val applyImeInset: Boolean = true,
    ) {
        fun statusBarStyle(): SystemBarStyle {
            return if (darkStatusBarIcons) {
                SystemBarStyle.light(statusBarColor, statusBarColor)
            } else {
                SystemBarStyle.dark(statusBarColor)
            }
        }

        fun navigationBarStyle(): SystemBarStyle {
            return if (darkNavigationBarIcons) {
                SystemBarStyle.light(navigationBarColor, navigationBarColor)
            } else {
                SystemBarStyle.dark(navigationBarColor)
            }
        }
    }

    enum class BottomInsetMode {
        ALWAYS,
        NEVER,
        ONLY_TRADITIONAL_NAVIGATION,
    }
}
