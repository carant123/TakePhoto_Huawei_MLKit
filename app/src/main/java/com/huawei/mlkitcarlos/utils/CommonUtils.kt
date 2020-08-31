package com.huawei.mlkitcarlos.utils

import android.content.Context

object CommonUtils {
    private const val TAG = "CommonUtils"
    fun dp2px(context: Context, dipValue: Float): Float {
        return dipValue * context.getResources().getDisplayMetrics().density + 0.5f
    }
}