package cn.wgc.keyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class KeyboardScenarioFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.layout_keyboard_scenario_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mixedInputs = arguments?.getBoolean(ARG_MIXED_INPUTS, false) ?: false
        val systemHintView = view.findViewById<View>(R.id.scenarioSystemHint)
        val systemInput = view.findViewById<View>(R.id.scenarioSystemEditText)
        val bottomHintView = view.findViewById<View>(R.id.scenarioBottomHint)

        systemHintView.visibility = if (mixedInputs) View.VISIBLE else View.GONE
        systemInput.visibility = if (mixedInputs) View.VISIBLE else View.GONE
        bottomHintView.visibility = if (mixedInputs) View.VISIBLE else View.GONE
    }

    companion object {
        private const val ARG_MIXED_INPUTS = "mixed_inputs"

        fun newInstance(mixedInputs: Boolean): KeyboardScenarioFragment {
            return KeyboardScenarioFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MIXED_INPUTS, mixedInputs)
                }
            }
        }
    }
}
