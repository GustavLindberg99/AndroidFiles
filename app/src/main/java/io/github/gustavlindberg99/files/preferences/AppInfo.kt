package io.github.gustavlindberg99.files.preferences

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import io.github.gustavlindberg99.files.activity.App
import java.io.ByteArrayOutputStream

private const val APP_NAME_CACHE = "appNameCache"
private const val APP_ICON_CACHE = "appIconCache"

/**
 * Information about another app with caching functionality since on certain versions of Android the app only has access to this information when interacting with the other app.
 *
 * @param packageName   The package name of the app.
 */
class AppInfo(public val packageName: String) {
    /**
     * The app's human readable name, or null if it's unknown.
     */
    public val name: String? by lazy {
        val app = try {
            App.context.packageManager.getApplicationInfo(this.packageName, 0)
        }
        catch (_: PackageManager.NameNotFoundException) {
            //If the name wasn't found, return the cached value (or null if there is no cached value)
            return@lazy App.context
                .getSharedPreferences(APP_NAME_CACHE, AppCompatActivity.MODE_PRIVATE)
                .getString(this.packageName, null)
        }

        //Get the app name
        val result = App.context.packageManager.getApplicationLabel(app).toString()

        //Cache the app name in case the information gets lost
        App.context
            .getSharedPreferences(APP_NAME_CACHE, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putString(this.packageName, result)
            .apply()

        //Return the app name
        return@lazy result
    }

    /**
     * The app's icon, or null if it's unknown.
     */
    public val drawable: Drawable?
        get() {
            val result = try {
                App.context.packageManager.getApplicationIcon(this.packageName)
            }
            catch (_: PackageManager.NameNotFoundException) {
                val base64 = App.context
                    .getSharedPreferences(APP_ICON_CACHE, AppCompatActivity.MODE_PRIVATE)
                    .getString(this.packageName, null) ?: return null
                val data = Base64.decode(base64, Base64.DEFAULT)
                return BitmapDrawable(
                    App.context.resources,
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                )
            }
            val outputStream = ByteArrayOutputStream()
            result.toBitmap().compress(Bitmap.CompressFormat.PNG, 0, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            App.context
                .getSharedPreferences(APP_ICON_CACHE, AppCompatActivity.MODE_PRIVATE)
                .edit()
                .putString(this.packageName, base64)
                .apply()
            return result
        }

    override fun equals(other: Any?): Boolean {
        return other is AppInfo && this.packageName == other.packageName
    }

    override fun hashCode(): Int {
        return this.packageName.hashCode()
    }

    companion object {
        /**
         * Gets all apps, both the ones this app is allowed to see and the ones that are cached.
         */
        @SuppressLint("QueryPermissionsNeeded")
        public fun allApps(): List<AppInfo> {
            val result = mutableListOf<AppInfo>()
            for (app in App.context.packageManager.getInstalledApplications(0)) {
                if (app.enabled && app.sourceDir.startsWith("/data/app")) {
                    result.add(AppInfo(app.packageName))
                }
            }
            //TODO: get cached icons as well
            return result
        }
    }
}