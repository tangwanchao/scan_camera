package me.twc.scancamera.camera

import android.os.Handler
import android.os.HandlerThread
import me.twc.utils.logD

/**
 * @author 唐万超
 * @date 2021/06/08
 *
 */
open class CameraThread(
    threadName: String = "CameraThread"
) {

    private var mThread: HandlerThread? = HandlerThread(threadName).apply {
        start()
    }
    private var mHandler: Handler? = mThread?.looper?.let { Handler(it) }

    /**
     * 相机操作加入子线程队列
     *
     * @param delayMillis 入队的延迟
     * @param safe [true : 自动捕获[runnable]中的所有异常],[false: 常规方式运行[runnable]]
     * @param runnable 需要在子线程中运行的 [runnable]
     */
    @Synchronized
    fun enqueue(
        delayMillis: Long = 0,
        safe: Boolean = true,
        runnable: Runnable
    ) {
        val handler = mHandler
        if (handler == null) {
            logD("入队失败,线程已经退出")
            return
        }
        val isSuccess = handler.postDelayed(SafeRunnable(safe, runnable), delayMillis)
        if (!isSuccess) {
            logD("入队失败,线程已经退出")
        }
    }

    fun quit() {
        try {
            mThread?.quit()
            mThread = null
            mHandler = null
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }
}