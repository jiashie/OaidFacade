package com.jiashie.oaidfacade

import android.os.Handler
import android.os.Looper

internal object TimeoutChecker {
    private val handler = Handler(Looper.getMainLooper())
    private val runnableMap: MutableMap<TimeoutCheckable, Runnable> =
        HashMap<TimeoutCheckable, Runnable>()

    fun check(timeout: Long, task: TimeoutCheckable) {
        val runnable = Runnable {
            if (!task.isDone) {
                task.onTimeout()
            }
            remove(task)
        }
        handler.postDelayed(runnable, timeout)
        runnableMap[task] = runnable
    }

    fun remove(task: TimeoutCheckable) {
        runnableMap.remove(task)?.also {
            handler.removeCallbacks(it)
        }
    }
}
internal interface TimeoutCheckable {
    /**
     * 任务是否已执行
     */
    val isDone: Boolean

    /**
     * 任务超时
     */
    fun onTimeout()
}