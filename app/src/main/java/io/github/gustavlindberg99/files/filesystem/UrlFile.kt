package io.github.gustavlindberg99.files.filesystem

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import com.github.vincentrussell.ini.Ini
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.FileExplorerActivity
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Represents an internet shortcut.
 *
 * @param _url  The target of this internet shortcut.
 * @param file  The File object for the shortcut itself.
 */
class UrlFile private constructor(private val _url: String, file: File) : GeneralFile(file) {
    companion object {
        /**
         * Gets the UrlFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The URL file at the given path, or null if the path doesn't exist or isn't a valid URL file.
         */
        public fun fromPath(path: String): UrlFile? {
            val file = File(path)
            if (!file.exists() || file.extension.lowercase() != "url") {
                return null
            }
            val ini = Ini()
            try {
                ini.load(FileInputStream(path))
            }
            catch (_: IOException) {
                return null
            }
            val url: Any? = ini.getValue("InternetShortcut", "URL")
            if (url !is String) {
                return null
            }
            return UrlFile(url, file)
        }
    }

    public override fun icon(): Drawable {
        val mainIcon = super.icon()
        val shortcutIcon = AppCompatResources.getDrawable(App.context, R.drawable.shortcut)
        return LayerDrawable(arrayOf(mainIcon, shortcutIcon))
    }

    public override fun open(activity: FileExplorerActivity) {
        if (this.fileType().openWith()?.packageName != BuildConfig.APPLICATION_ID) {
            return super.open(activity)
        }

        val target = Uri.parse(this._url)
        val browserIntent = Intent(Intent.ACTION_VIEW, target)
        activity.startActivity(browserIntent)
    }

    /**
     * Gets the target of this internet shortcut.
     *
     * @return The URL target of this internet shortcut as a string.
     */
    public fun targetUrl(): String {
        return this._url
    }
}