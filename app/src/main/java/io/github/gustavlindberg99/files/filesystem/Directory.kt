package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.R
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

/**
 * Represents any directory (i.e. not special folders).
 *
 * @param _getFiles     A lambda that returns the files and subfolders in this directory.
 * @param _absolutePath The absolute path of this directory.
 */
open class Directory internal constructor(
    private val _getFiles: () -> Set<FileOrFolder>?,
    private val _absolutePath: String
): Folder {
    /**
     * Convenience constructor that can be used if the directory isn't in an archive and can be accessed directly.
     *
     * @param file  The File object corresponding to this directory. Used to deduce the other parameters in the primary constructor.
     */
    internal constructor(file: File): this(
        {file.listFiles()?.mapNotNull {FileOrFolder.fromPath(it.absolutePath)}?.toSet()},
        file.absolutePath
    )

    companion object {
        /**
         * Gets the Directory object at the given path. Can't be a constructor because it must be able to return null.
         *
         * @param path  The path of the folder.
         *
         * @return The directory at the given path, or null if the path doesn't exist or isn't a directory.
         */
        public fun fromPath(path: String): Directory? {
            if (!Files.isDirectory(Paths.get(path))) {
                return resolveFileInArchive(path) as? Directory
            }
            val file = File(path)

            //The entire Directory class can't inherit from FileWithCustomIcon because that would allow changing the icon of a drive or a directory inside an archive.
            return Drive.fromPath(path) ?: object: Directory(file), FileWithCustomIcon {
                public override fun setIcon(iconPath: String) {
                    val desktopIniPath = this.absolutePath() + "/desktop.ini"
                    val desktopIniFile = DesktopIniFile.fromPath(desktopIniPath)
                    if (desktopIniFile != null) {
                        desktopIniFile.setParentDirIconPath(iconPath)
                    }
                    else {
                        val desktopIniContent =
                            mapOf(".ShellClassInfo" to mapOf("IconResource" to iconPath))
                        writeIniFile(desktopIniPath, desktopIniContent)
                    }
                }
            }
        }
    }

    public override fun equals(other: Any?): Boolean {
        return other is Directory && this.absolutePath() == other.absolutePath()
    }

    public override fun hashCode(): Int {
        return this.absolutePath().hashCode()
    }

    public override fun absolutePath(): String {
        return this._absolutePath
    }

    public override fun name(): String {
        return File(this._absolutePath).name
    }

    public override fun typeName(): String {
        return App.context.getString(R.string.folder)
    }

    public override fun hidden(): Boolean {
        return this.name().startsWith(".") && !this.isInArchive()
    }

    public override fun files(): Set<FileOrFolder> {
        return this._getFiles() ?: setOf()
    }

    public override fun icon(): Drawable {
        val desktopIniFile = DesktopIniFile.fromPath(this.absolutePath() + "/desktop.ini")
        return desktopIniFile?.parentDirIcon()
            ?: AppCompatResources.getDrawable(App.context, R.drawable.folder)!!
    }

    /**
     * Gets the total size of all the files in this directory, as well as the number of files and the number of directories it contains (either directly or indirectly, for example a directory containing a subdirectory which itself contains a subdirectory will have a directory count of 2). Can be very slow, so should be wrapped in a thread when used.
     *
     * @return Triple<The total size of all the files in this directory, The number of files contained in this directory or its subdirectories, the number of directories contained in this directory or its subdirectories>.
     */
    public fun contentsStatistics(): Triple<Long, Int, Int> {
        var size: Long = 0
        var files: Int = 0
        var folders: Int = 0
        for (file in this.files()) {
            if (file is GeneralFile) {
                size += file.size()
                files++
            }
            else if (file is Directory) {
                val (newSize, newFiles, newFolders) = file.contentsStatistics()
                size += newSize
                files += newFiles
                folders += newFolders + 1
            }
        }
        return Triple(size, files, folders)
    }

    /**
     * Checks if this directory is a media directory, i.e. there is no .nomedia file in this directory or any of its parent directories and it's not in an archive.
     *
     * @return True if it's a media directory, false if it isn't.
     */
    public fun isMediaDirectory(): Boolean {
        return this.canBeMediaDirectory() && !File(this.absolutePath() + "/.nomedia").isFile
    }

    /**
     * Checks if this directory can be a media directory (regardless of whether it currently is), i.e. that it's not in an archive and that the parent directory is a media directory.
     *
     * @return True if it can be a media directory, false otherwise.
     */
    public fun canBeMediaDirectory(): Boolean {
        if (this.isInArchive()) {
            return false
        }
        val parentFolder = this.parentFolder()
        return parentFolder !is Directory || parentFolder.isMediaDirectory()
    }

    /**
     * Gets the absolute path from a path relative to this directory.
     *
     * @param relativePath  A relative path relative to this directory.
     *
     * @return The corresponding absolute path.
     */
    public fun resolveRelativePath(relativePath: String): String {
        return this.path().resolve(relativePath).absolutePathString()
    }
}