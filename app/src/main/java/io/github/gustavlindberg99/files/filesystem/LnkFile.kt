package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.FileExplorerActivity
import io.github.gustavlindberg99.files.preferences.iconFromPath
import mslinks.ShellLink
import mslinks.ShellLinkException
import java.io.File
import java.io.IOException

/**
 * Represents a .lnk shortcut.
 *
 * @param _lnk  The parsed information about this shortcut.
 * @param file  The File object for the shortcut itself.
 */
class LnkFile private constructor(private val _lnk: ShellLink, file: File): GeneralFile(file) {
    companion object {
        /**
         * Gets the LnkFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The LNK file at the given path, or null if the path doesn't exist or isn't a valid LNK file.
         */
        public fun fromPath(path: String): LnkFile? {
            val file = File(path)
            if (!file.exists() || file.extension.lowercase() != "lnk") {
                return null
            }
            val lnk = try {
                ShellLink(path)
            }
            catch (_: IOException) {
                return null
            }
            catch (_: ShellLinkException) {
                return null
            }
            return LnkFile(lnk, file)
        }
    }

    public override fun icon(): Drawable {
        val customIconPath = this._lnk.iconLocation + "," + this._lnk.header.iconIndex
        val customIcon: Drawable? =
            iconFromPath(this.parentFolder() as? Directory, customIconPath)
        val target = this.target()
        val mainIcon: Drawable =
            customIcon ?: if (target != null && target != this) target.icon()
            else AppCompatResources.getDrawable(App.context, R.drawable.file)!!
        val shortcutIcon = AppCompatResources.getDrawable(App.context, R.drawable.shortcut)
        return LayerDrawable(arrayOf(mainIcon, shortcutIcon))
    }

    public override fun open(activity: FileExplorerActivity) {
        if (this.fileType().openWith()?.packageName != BuildConfig.APPLICATION_ID) {
            return super.open(activity)
        }

        val target = this.target()
        if (target == null) {
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.targetDoesNotExist),
                    this._lnk.resolveTarget()
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
        else {
            target.open(activity)
        }
    }

    /**
     * Gets the target of the LNK file as it says in the file, even if it's another LNK file.
     *
     * @return  The target as a FileOrFolder object, or null if the target doesn't exist.
     */
    private fun directTarget(): FileOrFolder? {
        val targetWindowsPath = this._lnk.resolveTarget()
        val targetAndroidPath = targetWindowsPath
            .replace('\\', '/')
            .replace(
                Regex("""^[A-Z]:"""),
                this.parentFolder()?.drive()?.absolutePath() ?: return null
            )
        return FileOrFolder.fromPath(targetAndroidPath)
    }

    /**
     * Gets the target of the LNK file after following all links.
     *
     * @return  The target as a FileOrFolder object, or null if the target doesn't exist or is a circular reference. Guaranteed not to be an instance of LnkFile.
     */
    public fun target(): FileOrFolder? {
        var target: FileOrFolder = this
        do {
            if (target !is LnkFile) {
                return target
            }
            target = target.directTarget() ?: return null
        } while (target != this)
        return null
    }
}