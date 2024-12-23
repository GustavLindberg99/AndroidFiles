package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.FileExplorerActivity
import io.github.gustavlindberg99.files.activity.App
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime

/**
 * Represents any file or folder.
 */
interface FileOrFolder {
    companion object {
        /**
         * Gets the FileOrFolder object at the given path.
         *
         * @param path  The path of the file or folder. Should not contain trailing slashes, otherwise there might be a bug where adding a file to an archive adds the archive to itself instead.
         *
         * @return The file or folder at the given path, or null if the path doesn't exist.
         */
        public fun fromPath(path: String): FileOrFolder? {
            return Directory.fromPath(path) ?: GeneralFile.fromPath(path)
        }
    }

    /**
     * Gets the absolute path of the file or folder.
     *
     * @return The absolute path of the file or folder.
     */
    public abstract fun absolutePath(): String

    /**
     * Gets the absolute path of the file or folder as a Path object.
     *
     * @return The Path object.
     */
    public fun path(): Path {
        return Paths.get(this.absolutePath())
    }

    /**
     * Gets the name of this file or folder as should be displayed if extensions should be shown.
     *
     * @return The name of this file or folder.
     */
    public abstract fun name(): String

    /**
     * Gets the name of this file or folder taking into account the "hide file extensions" setting.
     *
     * @return The name without the extension for files, the complete name (but not the path) for folders.
     */
    public abstract fun baseName(): String

    /**
     * Gets the extension of this file.
     *
     * @return The extension if it's a file, or an empty string if it's a folder.
     */
    public abstract fun extension(): String

    /**
     * The file type as a human-readable string.
     *
     * @return The file type as a human-readable string.
     */
    public abstract fun typeName(): String

    /**
     * Checks whether the file or folder is hidden.
     *
     * @return True if it's hidden, false if it isn't.
     */
    public abstract fun hidden(): Boolean

    /**
     * Gets the icon of the current file or folder.
     *
     * @return The icon of the current file or folder.
     */
    public abstract fun icon(): Drawable

    /**
     * Opens this file or folder, either in this app or in another app depending on the file type.
     *
     * @param activity  The activity to open it in if it should be opened in this app, or to launch the intent if it should be opened in another app.
     */
    public abstract fun open(activity: FileExplorerActivity)

    /**
     * Gets the parent folder of this file or folder.
     *
     * @return The parent folder, or null if the current folder is the root folder or if read permissions for the parent folder are missing.
     */
    public open fun parentFolder(): Folder? {
        return Folder.fromPath(this.path().parent.toString())
    }

    /**
     * Gets the archive that contains this file or folder (this isn't necessarily the direct parent folder if it's in a subdirectory in an archive). If this file itself is an archive, returns null if it's not nested and the archive above it if it's nested.
     *
     * @return The closest parent that's an archive, or null if this file or folder isn't in an archive.
     */
    public fun parentArchive(): Archive? {
        return parentArchiveOfPath(this.absolutePath())
    }

    /**
     * Checks whether this file or folder is inside an archive.
     *
     * @return True if this file or folder is inside an archive, false otherwise.
     */
    public fun isInArchive(): Boolean {
        return this.parentArchive() != null
    }

    /**
     * Renames the file.
     *
     * @param destination   The directory to move this file to.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public open fun move(destination: Folder) {
        if (destination is ThisPhoneFolder) {
            throw IOException(App.context.getString(R.string.cannotCreateFilesInThisPhoneFolder))
        }
        val destinationFile = destination.absolutePath() + "/" + this.name()
        val destinationArchive: Archive? = destination as? Archive ?: destination.parentArchive()
        val currentArchive = this.parentArchive()
        //Don't allow moving files onto directories or vice-versa (for some reason Files.move doesn't check for this automatically)
        if (destinationArchive == null && (
            this is GeneralFile && File(destinationFile).isDirectory
            ) || (
            this is Directory && File(destinationFile).isFile
            )
        ) {
            throw IOException(App.context.getString(R.string.entryBothFileAndDirectory))
        }
        if (currentArchive == null && destinationArchive == null) {
            val file = File(this.absolutePath())
            Files.move(
                file.toPath(),
                Paths.get(destinationFile),
                StandardCopyOption.REPLACE_EXISTING    //Always overwrite, if the user doesn't want this they will have answered no in the confirmation dialog and this function is never called.
            )
        }
        else if (currentArchive != null && currentArchive == destinationArchive) {
            currentArchive.renameOrDeleteFile(
                pathRelativeToParentArchive(this.absolutePath()),
                pathRelativeToParentArchive(destinationFile)
            )
        }
        else {
            this.copy(destination)
            this.delete()
        }
    }

    /**
     * Copies the file.
     *
     * @param destination   The directory to copy this file to.
     * @param newName       The new name that the copied file should have. If not specified, preserves the name of the original file.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public open fun copy(destination: Folder, newName: String = this.name()) {
        if (destination is ThisPhoneFolder) {
            throw IOException(App.context.getString(R.string.cannotCreateFilesInThisPhoneFolder))
        }
        val destinationArchive: Archive? = destination as? Archive ?: destination.parentArchive()
        val relativeDestinationPath =
            if (destination is Archive) ""
            else pathRelativeToParentArchive(destination.absolutePath()) + "/"
        if (destinationArchive != null) {
            val filesToAdd = mutableListOf<Pair<FileOrFolder, String>>()
            lateinit var registerFiles: (FileOrFolder, String, String) -> Unit
            registerFiles = {file: FileOrFolder, prefix: String, subName: String ->
                if (file is Directory) {
                    for (subFile in file.files()) {
                        registerFiles(subFile, "$prefix$subName/", subFile.name())
                    }
                }
                else {
                    filesToAdd.add(file to relativeDestinationPath + prefix + subName)
                }
            }
            registerFiles(this, "", newName)
            return destinationArchive.addFiles(filesToAdd)
        }
        else if (this is GeneralFile) {
            val inputStream = this.createInputStream()
            FileUtils.copyInputStreamToFile(
                inputStream,
                File(destination.absolutePath() + "/" + newName)
            )
        }
        else if (this is Directory) {
            val subDestinationPath = destination.absolutePath() + "/" + newName
            val dirToCreate = File(subDestinationPath)
            if (!dirToCreate.isDirectory && !dirToCreate.mkdirs()) {
                throw IOException(
                    String.format(
                        App.context.getString(R.string.failedToCreateDirectory),
                        subDestinationPath
                    )
                )
            }
            val subDestination = Directory.fromPath(subDestinationPath) ?: throw IOException(
                String.format(
                    App.context.getString(R.string.failedToCreateDirectory),
                    subDestinationPath
                )
            )
            for (file in this.files()) {
                file.copy(subDestination)
            }
        }
    }

    /**
     * Renames the file.
     *
     * @param newName   The new name.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public open fun rename(newName: String) {
        val file = File(this.absolutePath())
        val newFile = file.resolveSibling(newName)
        if (newFile.exists()) {
            throw IOException(App.context.getString(R.string.fileAlreadyExists))
        }
        val parentArchive = this.parentArchive()
        if (parentArchive != null) {
            parentArchive.renameOrDeleteFile(
                pathRelativeToParentArchive(this.absolutePath()),
                pathRelativeToParentArchive(newFile.toString())
            )
        }
        else {
            if (!file.renameTo(newFile)) {
                throw IOException(
                    String.format(
                        App.context.getString(R.string.failedToRename),
                        file.name,
                        newFile.name
                    )
                )
            }
        }
    }

    /**
     * Deletes this file and shows an error message if the deletion failed.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public open fun delete() {
        val parentArchive = this.parentArchive()
        if (parentArchive != null) {
            parentArchive.renameOrDeleteFile(
                pathRelativeToParentArchive(this.absolutePath()),
                null
            )
        }
        else {
            if (!File(this.absolutePath()).deleteRecursively()) {
                throw IOException(
                    String.format(
                        App.context.getString(R.string.failedToDelete),
                        this.absolutePath()
                    )
                )
            }
        }
    }

    /**
     * Gets the last modified date of this file.
     *
     * @return The last modified date of this file, or null if it's unavailable.
     */
    public fun lastModified(): FileTime? {
        val parentArchive: Archive? = this.parentArchive()
        if (parentArchive != null) {
            return parentArchive.entryLastModified(pathRelativeToParentArchive(this.absolutePath()))
        }
        else try {
            return Files.getLastModifiedTime(this.path())
        }
        catch (_: IOException) {
            return null
        }
    }
}