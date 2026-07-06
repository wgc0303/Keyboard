package cn.wgc.keyboard

import android.os.Bundle

class ActivityScenarioActivity : DemoBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Activity 宿主验证"
        setContentView(R.layout.layout_keyboard_scenario_content)
        applyDemoWindow(R.id.main)
    }
}
