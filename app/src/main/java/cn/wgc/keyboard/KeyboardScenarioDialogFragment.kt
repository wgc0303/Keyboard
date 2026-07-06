package cn.wgc.keyboard

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Space
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment

class KeyboardScenarioDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_CustomKeyboard_FullscreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.layout_dialog_keyboard_scenario, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.dialogCloseButton).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.dialogScenarioRoot).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.dialogScenarioCard).setOnClickListener { }
        view.findViewById<Space>(R.id.scenarioSpacer)?.layoutParams =
            view.findViewById<Space>(R.id.scenarioSpacer)?.layoutParams?.apply {
                height = dialogSpacerHeight()
            }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            findViewById<View>(android.R.id.content)?.setPadding(0, 0, 0, 0)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            CustomKeyboardManager.bindHostWindow(
                root = requireView().findViewById(R.id.dialogScenarioContent),
                window = this,
            )
            EdgeToEdgeHelper.applyWindow(
                window = this,
                root = requireView().findViewById(R.id.dialogScenarioContent),
                config = demoConfig(),
            )
        }
    }

    private fun demoConfig(): EdgeToEdgeHelper.Config {
        return EdgeToEdgeHelper.Config(
            bottomInsetMode = EdgeToEdgeHelper.BottomInsetMode.ONLY_TRADITIONAL_NAVIGATION,
        )
    }

    private fun dialogSpacerHeight(): Int {
        val density = resources.displayMetrics.density
        return (48 * density + 0.5f).toInt()
    }
}
