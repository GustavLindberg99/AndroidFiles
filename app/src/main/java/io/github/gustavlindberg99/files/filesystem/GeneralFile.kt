package io.github.gustavlindberg99.files.filesystem

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import io.github.gustavlindberg99.files.preferences.FileType
import io.github.gustavlindberg99.files.activity.OpenWithTrigger
import io.github.gustavlindberg99.files.preferences.Preferences
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.FileExplorerActivity
import io.github.gustavlindberg99.files.preferences.openFileIntent
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Represents any file (but not folders).
 *
 * @param _getFile      A lambda that returns a File object that exists and can be opened, and that may throw an IOException. Useful for files inside archives since those files need to be extracted to a temporary folder before they can be opened.
 * @param _absolutePath The absolute path of this file.
 * @param _size         The size of this file.
 */
open class GeneralFile internal constructor(
    private val _getFile: () -> File,
    private val _absolutePath: String,
    private val _size: Long
): FileOrFolder {
    /**
     * Convenience constructor that can be used if the file isn't in an archive and can be accessed directly.
     *
     * @param file  The File object corresponding to this file. Used to deduce the other parameters in the primary constructor.
     */
    internal constructor(file: File): this({file}, file.absolutePath, file.length())

    companion object {
        /**
         * Gets the RegularFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The file at the given path, or null if the path doesn't exist.
         */
        public fun fromPath(path: String): GeneralFile? {
            val file = File(path)
            if (file.isDirectory) {
                return null
            }
            if (!file.exists()) {
                return resolveFileInArchive(path) as? GeneralFile
            }
            return LnkFile.fromPath(path)
                ?: IcoFile.fromPath(path)
                ?: UrlFile.fromPath(path)
                ?: DesktopIniFile.fromPath(path)
                ?: Archive.fromPath(path)
                ?: GeneralFile(file)
        }
    }

    public override fun equals(other: Any?): Boolean {
        return other is GeneralFile && this.absolutePath() == other.absolutePath()
    }

    public override fun hashCode(): Int {
        return this.absolutePath().hashCode()
    }

    public override fun absolutePath(): String {
        return this._absolutePath
    }

    public override fun name(): String {
        return File(this.absolutePath()).name
    }

    public override fun baseName(): String {
        if (Preferences.showFileExtensions || this.fileType().alwaysShowExt) {
            return this.name()
        }
        else {
            return File(this.absolutePath()).nameWithoutExtension.ifEmpty {this.name()}
        }
    }

    public override fun extension(): String {
        return File(this.absolutePath()).extension
    }

    public override fun typeName(): String {
        return this.fileType().description
    }

    public override fun hidden(): Boolean {
        return (this.name().startsWith(".") || this.name() == "desktop.ini") && !this.isInArchive()
    }

    public override fun icon(): Drawable {
        return this.fileType().icon()
    }

    public override fun open(activity: FileExplorerActivity) {
        val file = try {
            this._getFile()
        }
        catch (e: IOException) {
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.couldNotOpen),
                    e.message ?: activity.getString(R.string.unknownError)
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val intent = openFileIntent(file, editable = !this.isInArchive())
        activity.startActivity(intent)
    }

    /**
     * Shows a dialog to allow the user to select which app to open this file with.
     *
     * @param activity  The activity to use to launch the intent.
     */
    public fun showOpenWithDialog(activity: Activity) {
        val file = try {
            this._getFile()
        }
        catch (e: IOException) {
            Toast.makeText(
                activity,
                String.format(
                    activity.getString(R.string.couldNotOpen),
                    e.message ?: activity.getString(R.string.unknownError)
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val componentName = ComponentName(activity, OpenWithTrigger::class.java)
        activity.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        val intent = openFileIntent(file, editable = !this.isInArchive())
        activity.startActivity(intent)
        activity.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Gets the file's size.
     *
     * @return The file's size in bytes.
     */
    public fun size(): Long {
        return this._size
    }

    /**
     * Gets the compressed size of this file if it's in an archive.
     *
     * @return The compressed size of this file, or null if it's not in an archive or if this information is unavailable.
     */
    public fun compressedSize(): Long? {
        val parentArchive: Archive = this.parentArchive() ?: return null
        return parentArchive.entryCompressedSize(pathRelativeToParentArchive(this.absolutePath()))
    }

    /**
     * Gets the information about this file's type.
     *
     * @return The information about this file's type.
     */
    public fun fileType(): FileType {
        return FileType(this.extension())
    }

    /**
     * Writes the contents of this file to an output stream.
     *
     * @param outputStream  The output stream to write to.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    internal fun writeToOutputStream(outputStream: OutputStream) {
        val file = this._getFile()
        val path = Paths.get(file.absolutePath)
        Files.copy(path, outputStream)
    }

    /**
     * Creates an input stream with the contents of this file.
     *
     * @return An input stream with the contents of this file.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    internal fun createInputStream(): InputStream {
        val file = this._getFile()
        return FileInputStream(file)
    }

    /**
     * Copies this file to a 7Z archive.
     *
     * @param outputStream  The 7Z output stream to create the entry in.
     * @param name          The path that the file should have inside the 7Z archive, relative to the archive root.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    internal fun copyTo7zFile(outputStream: SevenZOutputFile, name: String) {
        val file = this._getFile()
        val entry = outputStream.createArchiveEntry(file, name)
        outputStream.putArchiveEntry(entry)
        val inputStream = FileInputStream(file)
        outputStream.write(inputStream)
        outputStream.closeArchiveEntry()
    }
}