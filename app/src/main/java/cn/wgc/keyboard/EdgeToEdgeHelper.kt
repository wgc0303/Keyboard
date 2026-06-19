package cn.wgc.keyboard

import android.graphics.Color
import android.view.View
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

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val tappableInsets = insets.getInsets(WindowInsetsCompat.Type.tappableElement())

            val leftInset = if (config.applyHorizontalInsets) maxOf(statusInsets.left, navigationInsets.left) else 0
            val rightInset = if (config.applyHorizontalInsets) maxOf(statusInsets.right, navigationInsets.right) else 0
            val topInset = if (config.applyTopInset) statusInsets.top else 0
            val bottomInset = when (config.bottomInsetMode) {
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

            view.setPadding(
                initialLeft + leftInset,
                initialTop + topInset,
                initialRight + rightInset,
                initialBottom + bottomInset,
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun isTraditionalNavigation(navigationBottom: Int, tappableBottom: Int): Boolean {
        return navigationBottom > 0 && tappableBottom > 0
    }

    data class Config(
        @ColorInt val statusBarColor: Int = Color.WHITE,
        val darkStatusBarIcons: Boolean = true,
        @ColorInt val navigationBarColor: Int = Color.WHITE,
        val darkNavigationBarIcons: Boolean = true,
        val applyTopInset: Boolean = true,
        val applyHorizontalInsets: Boolean = true,
        val bottomInsetMode: BottomInsetMode = BottomInsetMode.ALWAYS,
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
