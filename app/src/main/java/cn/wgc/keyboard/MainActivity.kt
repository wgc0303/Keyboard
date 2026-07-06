package cn.wgc.keyboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EdgeToEdgeHelper.apply(this, findViewById(R.id.main), edgeToEdgeConfig())
        CustomKeyboardManager.installDismissOnOutsideTouch(this)

        findViewById<Button>(R.id.openActivityScenarioButton).setOnClickListener {
            startActivity(Intent(this, ActivityScenarioActivity::class.java))
        }
        findViewById<Button>(R.id.openFragmentScenarioButton).setOnClickListener {
            startActivity(Intent(this, FragmentScenarioActivity::class.java))
        }
        findViewById<Button>(R.id.openMixedFragmentScenarioButton).setOnClickListener {
            startActivity(Intent(this, MixedFragmentScenarioActivity::class.java))
        }
        findViewById<Button>(R.id.openDialogScenarioButton).setOnClickListener {
            startActivity(Intent(this, DialogScenarioActivity::class.java))
        }
        findViewById<Button>(R.id.openCustomDialogScenarioButton).setOnClickListener {
            startActivity(Intent(this, CustomDialogScenarioActivity::class.java))
        }
        findViewById<Button>(R.id.openBottomSheetScenarioButton).setOnClickListener {
            startActivity(Intent(this, BottomSheetScenarioActivity::class.java))
        }
    }
}
