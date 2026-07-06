package cn.wgc.keyboard

import android.os.Bundle
import android.widget.Button

class BottomSheetScenarioActivity : DemoBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "BottomSheet 宿主验证"
        setContentView(R.layout.activity_modal_scenario)
        applyDemoWindow(R.id.main)

        findViewById<Button>(R.id.showScenarioButton).apply {
            text = "再次打开 BottomSheet"
            setOnClickListener { openScenario() }
        }

        if (savedInstanceState == null) {
            findViewById<Button>(R.id.showScenarioButton).post { openScenario() }
        }
    }

    private fun openScenario() {
        KeyboardScenarioBottomSheetDialogFragment()
            .show(supportFragmentManager, "keyboard-bottom-sheet")
    }
}
