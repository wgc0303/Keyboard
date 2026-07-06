package cn.wgc.keyboard

import android.os.Bundle
import android.widget.Button

class DialogScenarioActivity : DemoBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Dialog 宿主验证"
        setContentView(R.layout.activity_modal_scenario)
        applyDemoWindow(R.id.main)

        findViewById<Button>(R.id.showScenarioButton).setOnClickListener {
            openScenario()
        }

        if (savedInstanceState == null) {
            findViewById<Button>(R.id.showScenarioButton).post { openScenario() }
        }
    }

    private fun openScenario() {
        KeyboardScenarioDialogFragment().show(supportFragmentManager, "keyboard-dialog")
    }
}
