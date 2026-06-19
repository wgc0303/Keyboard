package cn.wgc.keyboard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    protected open fun edgeToEdgeConfig(): EdgeToEdgeHelper.Config {
        return EdgeToEdgeHelper.Config(
            statusBarColor = Color.WHITE,
            darkStatusBarIcons = true,
            navigationBarColor = Color.WHITE,
            darkNavigationBarIcons = true,
            applyTopInset = true,
            applyHorizontalInsets = true,
            bottomInsetMode = EdgeToEdgeHelper.BottomInsetMode.NEVER,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        EdgeToEdgeHelper.apply(this,findViewById(R.id.main), edgeToEdgeConfig())
    }
}