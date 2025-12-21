package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import com.github.vincentrussell.ini.Ini
import io.github.gustavlindberg99.files.preferences.Icon
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Represents a desktop.ini file.
 *
 * @param _ini  The parsed contents of this file.
 * @param file  The File object corresponding to this file.
 */
class DesktopIniFile private constructor(private val _ini: Ini, file: File): GeneralFile(file) {
    companion object {
        /**
         * Gets the DesktopIniFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The desktop.ini at the given path, or null if the path doesn't exist or isn't a desktop.ini file.
         */
        public fun fromPath(path: String): DesktopIniFile? {
            val file = File(path)
            if (!file.exists() || file.name.lowercase() != "desktop.ini") {
                return null
            }
            val ini = Ini()
            try {
                ini.load(FileInputStream(path))
            }
            catch (_: IOException) {
                return null
            }
            return DesktopIniFile(ini, file)
        }
    }

    /**
     * Gets the icon of the directory that the desktop.ini file is in.
     *
     * @return The icon as a drawable, or null if there is no custom icon or if the custom icon could not be found.
     */
    public fun parentDirIcon(): Drawable? {
        val iconPath: Any? = this._ini.getValue(".ShellClassInfo", "IconResource")
        if (iconPath !is String) {
            return null
        }
        return Icon(iconPath, this.parentFolder() as? Directory).drawable
    }

    /**
     * Sets the icon of the directory that the desktop.ini file is in.
     *
     * @param icon  The icon to set.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public fun setParentDirIconPath(icon: Icon) {
        val contents = this._ini.sections.toList()
            .associateBy({it}, {this._ini.getSection(it).toMutableMap()}).toMutableMap()
        if (".ShellClassInfo" !in contents) {
            contents[".ShellClassInfo"] = mutableMapOf()
        }
        contents.getValue(".ShellClassInfo")["IconResource"] = icon.windowsPath
        writeIniFile(this.absolutePath(), contents)
    }
}