package cn.wgc.keyboard

import android.content.Context
import android.graphics.Color
import cn.wgc.keyboard.library.R

data class CustomKeyboardStyle(
    val keyboardBackgroundColor: Int,
    val spacedKeyboardBackgroundColor: Int,
    val letterRowBackgroundColor: Int,
    val keyTextColor: Int,
    val dividerColor: Int,
    val keyHeight: Int,
    val panelPaddingWhenSpaced: Int,
    val keyBackgroundRes: Int,
    val functionKeyBackgroundRes: Int,
    val flatKeyBackgroundRes: Int,
    val flatEdgeKeyBackgroundRes: Int,
    val deleteIconRes: Int,
    val visibleIconRes: Int,
    val invisibleIconRes: Int,
    val shiftIconRes: Int,
    val shiftActiveIconRes: Int,
    val hideKeyboardIconRes: Int
) {
    companion object {
        fun default(context: Context): CustomKeyboardStyle {
            fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

            return CustomKeyboardStyle(
                keyboardBackgroundColor = Color.WHITE,
                spacedKeyboardBackgroundColor = Color.parseColor("#F2F3F5"),
                letterRowBackgroundColor = Color.parseColor("#F2F3F5"),
                keyTextColor = Color.parseColor("#111827"),
                dividerColor = Color.parseColor("#E8E8E8"),
                keyHeight = dp(54),
                panelPaddingWhenSpaced = dp(6),
                keyBackgroundRes = R.drawable.ck_key_background,
                functionKeyBackgroundRes = R.drawable.ck_function_key_background,
                flatKeyBackgroundRes = R.drawable.ck_flat_key_background,
                flatEdgeKeyBackgroundRes = R.drawable.ck_flat_edge_key_background,
                deleteIconRes = R.drawable.ic_delete,
                visibleIconRes = R.drawable.ic_visible,
                invisibleIconRes = R.drawable.ic_invisible,
                shiftIconRes = R.drawable.ic_ck_shift,
                shiftActiveIconRes = R.drawable.ic_ck_shift_active,
                hideKeyboardIconRes = R.drawable.ic_ck_keyboard_hide
            )
        }
    }
}
