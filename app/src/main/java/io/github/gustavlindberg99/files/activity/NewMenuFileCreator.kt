package io.github.gustavlindberg99.files.activity

import android.widget.Toast
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.UrlFile
import io.github.gustavlindberg99.files.filesystem.parentArchiveOfPath
import io.github.gustavlindberg99.files.filesystem.pathRelativeToParentArchive
import io.github.gustavlindberg99.files.filesystem.writeIniFile
import io.github.gustavlindberg99.files.preferences.FileType
import java.io.File
import java.io.IOException

/**
 * Class that allows creating files from the New menu.
 *
 * @param _activity The activity to refresh when the files are created.
 */
class NewMenuFileCreator(private val _activity: FileExplorerActivity) {
    /**
     * Creates a file of the given type and prompts the user for the file name (done in copyFile). If the file type is a shortcut, first prompts the user for a target.
     *
     * @param fileType  The file type to create. If null, creates a directory.
     */
    public fun createFile(fileType: FileType?) {
        if (fileType == null) {
            this.copyOrCreateEmpty(null, null)
        }
        else if (fileType.extension == "url") {
            this._activity.showInputDialog(App.context.getString(R.string.enterUrl), "", true, {
                this.createUrlFile(it)
            })
        }
        else {
            val fileToCopy = GeneralFile.fromPath(
                App.context.getExternalFilesDir(null)
                    ?.resolve("newmenu/${fileType.extension}.${fileType.extension}")
                    ?.absolutePath
                    ?: "/"    //If the path couldn't be found, use something that guaranteed to not be a file
            )
            this.copyOrCreateEmpty(fileType, fileToCopy)
        }
    }

    /**
     * Allows to create a URL file from the New menu after the user has been prompted for a target URL. Prepares the URL file then calls createFileFromNewMenu.
     */
    private fun createUrlFile(url: String) {
        val urlIniContent = mapOf("InternetShortcut" to mapOf("URL" to url))
        try {
            val tempFile = File.createTempFile(
                "newInternetShortcut",
                ".url",
                App.context.cacheDir
            )
            writeIniFile(tempFile.absolutePath, urlIniContent)
            val fileToCopy: UrlFile =
                UrlFile.fromPath(tempFile.absolutePath) ?: throw IOException()
            this.copyOrCreateEmpty(FileType("url"), fileToCopy)
        }
        catch (e: IOException) {
            Toast.makeText(
                this._activity,
                String.format(
                    this._activity.getString(R.string.couldNotCreate),
                    "",
                    e.message ?: this._activity.getString(R.string.unknownError)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Prompts the user for which name they want to use, and if they submit, creates the file or directory (if they cancel, doesn't create anything).
     *
     * @param fileType      The file type to create. If null, creates a directory.
     * @param fileToCopy    If the newly created file should have contents by default, the file that contains that contents, otherwise null.
     */
    private fun copyOrCreateEmpty(fileType: FileType?, fileToCopy: GeneralFile?) {
        val newTypeText =
            this._activity.getString(R.string.createNew) + " " +
            (fileType?.description ?: this._activity.getString(R.string.folder))
        this._activity.showInputDialog(newTypeText, newTypeText, false, {
            //Create the file or directory
            val path = this._activity.currentFolder()
                .absolutePath() + "/" + it + if (fileType != null) ".${fileType.extension}" else ""
            val file = File(path)
            val parentArchive = parentArchiveOfPath(path)
            try {
                if (file.exists()) {
                    throw IOException(this._activity.getString(R.string.fileAlreadyExists))
                }
                if (fileType == null) {
                    if (parentArchive != null) {
                        val relativePath = pathRelativeToParentArchive(path)
                        parentArchive.createEmptyDirectory(relativePath)
                    }
                    else {
                        file.mkdirs()
                    }
                }
                else {
                    if (fileToCopy != null) {
                        val destination = Folder.fromPath(file.toPath().parent.toString())
                        if (destination != null) {
                            fileToCopy.copy(destination, it + "." + fileType.extension)
                        }
                    }
                    else if (parentArchive != null) {
                        val relativePath = pathRelativeToParentArchive(path)
                        parentArchive.createEmptyFile(relativePath)
                    }
                    else {
                        file.createNewFile()
                    }
                }

                //Show the newly created file in the activity
                this._activity.refresh()
            }
            catch (e: IOException) {
                Toast.makeText(
                    this._activity,
                    String.format(
                        this._activity.getString(R.string.couldNotCreate),
                        it,
                        e.message ?: this._activity.getString(R.string.unknownError)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}