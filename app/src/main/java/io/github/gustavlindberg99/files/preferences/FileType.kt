package io.github.gustavlindberg99.files.preferences

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.filesystem.Drive
import java.io.File

private const val FILE_TYPE_DESCRIPTIONS = "fileTypeDescriptions"
private const val ALWAYS_SHOW_EXT = "alwaysShowExt"
private const val SHOW_IN_NEW_MENU = "showInNewMenu"
private const val OPEN_WITH = "openWith"
private const val ICON_PATH = "iconPath"

/**
 * A class representing a file type.
 *
 * @param extension The extension of this file type without the dot.
 */
class FileType(public val extension: String) {
    companion object {
        /**
         * Gets all known file types.
         *
         * @return A list of all known file types.
         */
        public fun getAll(): List<FileType> {
            return listOf(FILE_TYPE_DESCRIPTIONS, ALWAYS_SHOW_EXT, OPEN_WITH, ICON_PATH)
                .asSequence()
                .map {App.context.getSharedPreferences(it, Context.MODE_PRIVATE).all.keys}
                .flatten()
                .sorted()
                .distinct()
                .map {FileType(it)}
                .toList()
        }
    }

    public override fun equals(other: Any?): Boolean {
        return other is FileType && other.extension.lowercase() == this.extension.lowercase()
    }

    public override fun hashCode(): Int {
        return extension.lowercase().hashCode()
    }

    public var description: String
        get() = App.context
            .getSharedPreferences(FILE_TYPE_DESCRIPTIONS, AppCompatActivity.MODE_PRIVATE)
            .getString(this.extension.lowercase(), null)
            ?: if (this.extension.isEmpty()) App.context.getString(R.string.file)
            else String.format(
                App.context.getString(R.string.defaultFileTypeDescription),
                this.extension.uppercase()
            )
        set(value) = App.context
            .getSharedPreferences(FILE_TYPE_DESCRIPTIONS, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putString(this.extension.lowercase(), value)
            .apply()

    public var alwaysShowExt: Boolean
        get() {
            val preferences = App.context
                .getSharedPreferences(ALWAYS_SHOW_EXT, AppCompatActivity.MODE_PRIVATE)
            if (!preferences.contains(this.extension.lowercase()) && this.openWith() != null) {
                //If the file type is known by the system but not by this app, add it and don't show the extension
                this.alwaysShowExt = false
                return false
            }
            return preferences.getBoolean(this.extension.lowercase(), true)
        }
        set(value) = App.context
            .getSharedPreferences(ALWAYS_SHOW_EXT, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putBoolean(this.extension.lowercase(), value)
            .apply()

    public var showInNewMenu: Boolean
        get() = App.context
            .getSharedPreferences(SHOW_IN_NEW_MENU, AppCompatActivity.MODE_PRIVATE)
            .getBoolean(this.extension.lowercase(), false)
        set(value) = App.context
            .getSharedPreferences(SHOW_IN_NEW_MENU, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putBoolean(this.extension.lowercase(), value)
            .apply()

    public var iconPath: String
        get() {
            val openWith = this.openWith()
            val defaultIconPath: String =
                if (openWith == null) shell32Icon(0)
                else appIcons(openWith)[1]
            return App.context
                .getSharedPreferences(ICON_PATH, AppCompatActivity.MODE_PRIVATE)
                .getString(this.extension.lowercase(), null) ?: defaultIconPath
        }
        set(value) = App.context
            .getSharedPreferences(ICON_PATH, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putString(this.extension.lowercase(), value)
            .apply()

    /**
     * Gets the file type's icon as a drawable.
     *
     * @return The file type's icon.
     */
    public fun icon(): Drawable {
        return iconFromPath(Drive.internalStorageFolder(), this.iconPath)
            ?: AppCompatResources.getDrawable(App.context, R.drawable.file)!!
    }

    /**
     * Checks if this file type's icon should be read from the file itself.
     *
     * @return True if the file itself contains the icon, false if the icon is set in the app settings.
     */
    public fun hasCustomIcon(): Boolean {
        return this.extension.lowercase() == "lnk" || this.extension.lowercase() == "ico"
    }

    /**
     * Gets the app that's supposed to open this file type.
     *
     * @return The app that's supposed to open this file type, or null if no app is set.
     */
    public fun openWith(): ApplicationInfo? {
        if (this.extension.isEmpty()) {
            return null
        }
        val file =
            File(Environment.getExternalStorageDirectory().toString() + "/." + this.extension)
        val intent = openFileIntent(file)
        var packageName: String? = App.context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (packageName == null || packageName == "android") {
            if (this.extension.lowercase() in listOf("lnk", "url", "zip", "tar", "7z")) {
                packageName = BuildConfig.APPLICATION_ID
            }
            else {
                return null
            }
        }
        return App.context.packageManager.getApplicationInfo(packageName, 0)
    }

    /**
     * Removes this file type from the list of known file types.
     */
    public fun remove() {
        for (setting in listOf(FILE_TYPE_DESCRIPTIONS, ALWAYS_SHOW_EXT, OPEN_WITH, ICON_PATH)) {
            App.context
                .getSharedPreferences(setting, AppCompatActivity.MODE_PRIVATE)
                .edit()
                .remove(this.extension.lowercase())
                .apply()
        }
    }
}