package cn.wgc.keyboard

class MixedFragmentScenarioActivity : FragmentScenarioActivity() {
    override fun createFragment(): KeyboardScenarioFragment {
        return KeyboardScenarioFragment.newInstance(mixedInputs = true)
    }

    override fun scenarioTitle(): String {
        return "Fragment 混合输入验证"
    }
}
