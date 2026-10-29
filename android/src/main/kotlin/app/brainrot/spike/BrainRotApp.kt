package app.brainrot.spike

import android.app.Application
import android.os.SystemClock

class BrainRotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        processStartElapsedMs = SystemClock.elapsedRealtime()
    }

    companion object {
        /** When this process started. A widget tap that arrives with a tiny "process age" had to cold-start the app. */
        @Volatile var processStartElapsedMs: Long = 0L
    }
}
