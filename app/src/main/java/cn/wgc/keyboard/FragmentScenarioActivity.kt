package cn.wgc.keyboard

import android.os.Bundle

open class FragmentScenarioActivity : DemoBaseActivity() {
    protected open fun createFragment(): KeyboardScenarioFragment {
        return KeyboardScenarioFragment()
    }

    protected open fun scenarioTitle(): String {
        return "Fragment 宿主验证"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = scenarioTitle()
        setContentView(R.layout.activity_fragment_scenario)
        applyDemoWindow(R.id.main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, createFragment())
                .commit()
        }
    }
}
