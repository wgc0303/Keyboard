package cn.wgc.keyboard

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Space
import androidx.core.view.WindowCompat

class KeyboardScenarioCustomDialog(
    context: CustomDialogScenarioActivity,
) : Dialog(context, R.style.ThemeOverlay_CustomKeyboard_FullscreenDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(true)
        setContentView(R.layout.layout_dialog_keyboard_scenario)

        findViewById<ImageButton>(R.id.dialogCloseButton).setOnClickListener {
            dismiss()
        }
        findViewById<View>(R.id.dialogScenarioRoot).setOnClickListener {
            dismiss()
        }
        findViewById<View>(R.id.dialogScenarioCard).setOnClickListener { }
        findViewById<Space>(R.id.scenarioSpacer)?.layoutParams =
            findViewById<Space>(R.id.scenarioSpacer)?.layoutParams?.apply {
                height = dialogSpacerHeight()
            }
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            findViewById<View>(android.R.id.content)?.setPadding(0, 0, 0, 0)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            CustomKeyboardManager.bindHostWindow(
                root = findViewById(R.id.dialogScenarioContent),
                window = this,
            )
            EdgeToEdgeHelper.applyWindow(
                window = this,
                root = findViewById(R.id.dialogScenarioContent),
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
        val density = context.resources.displayMetrics.density
        return (48 * density + 0.5f).toInt()
    }
}
