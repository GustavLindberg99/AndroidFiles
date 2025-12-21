package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import io.github.gustavlindberg99.files.preferences.Icon
import java.io.File

/**
 * Represents a .ico file.
 *
 * @param file  The File object corresponding to this file.
 */
class IcoFile private constructor(file: File) : GeneralFile(file) {
    companion object {
        /**
         * Gets the IcoFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The ICO file at the given path, or null if the path doesn't exist or isn't a valid ICO file.
         */
        public fun fromPath(path: String): IcoFile? {
            val file = File(path)
            if (!file.exists() || file.extension.lowercase() != "ico") {
                return null
            }
            return IcoFile(file)
        }
    }

    public override fun icon(): Drawable {
        return Icon(this.name(), this.parentFolder() as? Directory).drawable ?: super.icon()
    }
}