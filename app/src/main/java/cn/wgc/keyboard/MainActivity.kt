package cn.wgc.keyboard

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    protected open fun edgeToEdgeConfig(): EdgeToEdgeHelper.Config {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        EdgeToEdgeHelper.apply(this, findViewById(R.id.main), edgeToEdgeConfig())
    }
}