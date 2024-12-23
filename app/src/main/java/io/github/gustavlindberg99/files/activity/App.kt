package io.github.gustavlindberg99.files.activity

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment

/**
 * The application object for this app. Can be used to get the global context.
 */
class App: Application() {
    public override fun onCreate() {
        App.instance = this
        super.onCreate()
    }

    companion object {
        private lateinit var instance: App
        public val context: Context get() = instance.applicationContext

        /**
         * Checks if the app has permissions to read from storage.
         *
         * @return True if it has permissions to read from storage, false if it doesn't.
         */
        public fun hasStoragePermissions(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager()
            }
            else {
                return Environment.getExternalStorageDirectory().listFiles() != null
            }
        }
    }
}