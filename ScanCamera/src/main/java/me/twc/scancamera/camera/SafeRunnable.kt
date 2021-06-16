package me.twc.scancamera.camera

/**
 * @author 唐万超
 * @date 2021/06/08
 */
open class SafeRunnable(
    private val mSafe: Boolean = true,
    private val mRunnable: Runnable
) : Runnable {
    override fun run() {
        if (mSafe) safeRun() else mRunnable.run()
    }

    private fun safeRun() {
        try {
            mRunnable.run()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }
}