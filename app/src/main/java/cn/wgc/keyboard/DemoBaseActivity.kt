package cn.wgc.keyboard

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class DemoBaseActivity : AppCompatActivity() {
    protected fun applyDemoWindow(rootId: Int) {
        EdgeToEdgeHelper.apply(this, findViewById(rootId), edgeToEdgeConfig())
        CustomKeyboardManager.installDismissOnOutsideTouch(this)
    }

    private fun edgeToEdgeConfig(): EdgeToEdgeHelper.Config {
        return EdgeToEdgeHelper.Config(
            statusBarColor = Color.WHITE,
            darkStatusBarIcons = true,
            navigationBarColor = Color.WHITE,
            darkNavigationBarIcons = true,
            applyTopInset = true,
            applyHorizontalInsets = true,
            bottomInsetMode = EdgeToEdgeHelper.BottomInsetMode.ONLY_TRADITIONAL_NAVIGATION,
        )
    }
}
