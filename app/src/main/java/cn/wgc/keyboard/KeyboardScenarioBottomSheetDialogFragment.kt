package cn.wgc.keyboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class KeyboardScenarioBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.layout_keyboard_scenario_content, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            CustomKeyboardManager.bindHostWindow(
                root = requireView().findViewById(R.id.main),
                window = window,
            )
            EdgeToEdgeHelper.applyWindow(
                window = window,
                root = requireView().findViewById(R.id.main),
                config = demoConfig(),
            )
        }
    }

    private fun demoConfig(): EdgeToEdgeHelper.Config {
        return EdgeToEdgeHelper.Config(
            bottomInsetMode = EdgeToEdgeHelper.BottomInsetMode.ONLY_TRADITIONAL_NAVIGATION,
        )
    }
}
