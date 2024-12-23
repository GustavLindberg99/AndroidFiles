package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.StatFs
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.R
import java.io.File
import java.io.IOException

/**
 * Represents a drive (i.e. the internal storage or an external SD card).
 *
 * @param _file The File object corresponding to this directory.
 */
class Drive private constructor(private val _file: File): Directory(_file) {
    companion object {
        /**
         * Gets the Drive object at the given path. Can't be a constructor because it must be able to return null.
         *
         * @param path  The absolute path of the folder.
         *
         * @return The drive at the given path, or null if the path doesn't exist or isn't a drive.
         */
        public fun fromPath(path: String): Drive? {
            //If it's not a drive, return null
            if (path !in sdCardPaths()) {
                return null
            }

            val file = File(path)
            return Drive(file)
        }

        /**
         * Gets the internal storage folder.
         *
         * @return The internal storage folder.
         */
        public fun internalStorageFolder(): Drive {
            return Drive(Environment.getExternalStorageDirectory())
        }
    }

    public override fun name(): String {
        if (this.isInternalStorage()) {
            return App.context.getString(R.string.internalStorage)
        }
        else {
            return super.name()
        }
    }

    public override fun typeName(): String {
        return App.context.getString(R.string.internalStorage)
    }

    public override fun hidden(): Boolean {
        return false
    }

    public override fun icon(): Drawable {
        return AppCompatResources.getDrawable(
            App.context,
            if (this.isInternalStorage()) R.drawable.systemdrive
            else R.drawable.drive
        )!!
    }

    public override fun parentFolder(): Folder {
        return ThisPhoneFolder
    }

    public override fun move(destination: Folder) {
        throw IOException(App.context.getString(R.string.cannotMoveSdCard))
    }

    public override fun rename(newName: String) {
        throw IOException(App.context.getString(R.string.cannotRenameSdCard))
    }

    public override fun delete() {
        throw IOException(App.context.getString(R.string.cannotDeleteSdCard))
    }

    /**
     * Checks if this drive is the internal storage drive.
     *
     * @return True if it's the internal storage drive, false if it's an external SD card.
     */
    private fun isInternalStorage(): Boolean {
        return this.absolutePath() == Environment.getExternalStorageDirectory().toString()
    }

    /**
     * Gets the free disk space on the drive.
     *
     * @return The free disk space in bytes.
     */
    public fun freeDiskSpace(): Long {
        val stat = StatFs(this._file.absolutePath)
        return stat.availableBytes
    }

    /**
     * Gets the total disk space on the drive (free and occupied).
     *
     * @return The total disk space in bytes.
     */
    public fun totalDiskSpace(): Long {
        val stat = StatFs(this._file.absolutePath)
        return stat.totalBytes
    }
}