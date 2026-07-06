package cn.wgc.keyboard

import android.os.Bundle
import android.widget.Button

class CustomDialogScenarioActivity : DemoBaseActivity() {
    private var scenarioDialog: KeyboardScenarioCustomDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "自定义 Dialog 宿主验证"
        setContentView(R.layout.activity_modal_scenario)
        applyDemoWindow(R.id.main)

        findViewById<Button>(R.id.showScenarioButton).apply {
            text = "再次打开自定义 Dialog"
            setOnClickListener { openScenario() }
        }

        if (savedInstanceState == null) {
            findViewById<Button>(R.id.showScenarioButton).post { openScenario() }
        }
    }

    override fun onDestroy() {
        scenarioDialog?.dismiss()
        scenarioDialog = null
        super.onDestroy()
    }

    private fun openScenario() {
        scenarioDialog?.dismiss()
        scenarioDialog = KeyboardScenarioCustomDialog(this).also { it.show() }
    }
}
