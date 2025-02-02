package io.github.gustavlindberg99.files.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Archive
import io.github.gustavlindberg99.files.filesystem.Directory
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.preferences.FileType
import org.apache.commons.io.FileUtils
import org.apache.commons.validator.routines.UrlValidator
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * An activity that allows saving a given file. Can be opened with another app's "open with" or "share" functionality.
 */
class SaveAsActivity: BrowseActivity() {
    private lateinit var _inputStream: InputStream
    private var _uri: Uri? = null

    private val _fileType: FileType? by lazy {
        val uri = this._uri
        val extension =
            //If we're saving an internet shortcut by sharing a web page, the extension is always .url
            if (uri == null) "url"
            //If we're saving a file, find that file's extension
            else MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(this.contentResolver.getType(uri))
        if (extension.isNullOrEmpty()) null
        else FileType(extension)
    }

    protected override val finishButtonText = R.string.save

    protected override val fileTypeInputItems by lazy {
        val fileType = this._fileType
        if (fileType == null) listOf()
        else listOf(fileType.description to listOf(fileType))
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Suppress deprecation warnings because getParcelableExtra is deprecated but the new way isn't available until API level 33
        val uri: Uri? = this.intent.data
            ?: @Suppress("DEPRECATION") this.intent.getParcelableExtra(Intent.EXTRA_STREAM)
        val text: String? = this.intent.getStringExtra("android.intent.extra.TEXT")
        val title: String? = this.intent.getStringExtra("android.intent.extra.TITLE")
        val uriPath: String? = uri?.path
        val inputStream = if (uri == null) null else this.contentResolver.openInputStream(uri)
        if ((uriPath == null || inputStream == null) && (text == null || !UrlValidator.getInstance().isValid(text))) {
            Toast.makeText(this, R.string.saveAsFileMissing, Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        this._uri = uri
        this._inputStream = inputStream ?: "[InternetShortcut]\nURL=$text".byteInputStream()

        val fileName: String = uriPath?.split("/")?.last() ?: title
        ?: (this.getString(R.string.createNew) + " " + this.getString(R.string.internetShortcut))
        this.setFileNameInputText(fileName)
    }

    protected override fun finishButtonListener(fileBaseName: String) {
        val selectedType: FileType? = this.selectedFileTypes()?.first()
        val fileName =
            if (selectedType == null || fileBaseName.endsWith("." + selectedType.extension)) fileBaseName
            else fileBaseName + "." + selectedType.extension
        val destinationFile = File(this.currentFolder().absolutePath() + "/" + fileName)
        if (fileName.isEmpty() || fileName.contains("/")) {
            Toast.makeText(
                this,
                if (fileName.isEmpty()) R.string.emptyNotAllowed
                else R.string.slashNotAllowed,
                Toast.LENGTH_LONG
            ).show()
        }
        else if (destinationFile.isFile) {
            AlertDialog.Builder(this)
                .setTitle(R.string.saveAs)
                .setMessage(R.string.overwriteConfirmation)
                .setPositiveButton(R.string.yes, {_, _ -> this.saveFile(fileName)})
                .setNegativeButton(R.string.no, {_, _ ->})
                .show()
        }
        else if (destinationFile.isDirectory) {
            Toast.makeText(
                this,
                String.format(this.getString(R.string.entryBothFileAndDirectory), fileName),
                Toast.LENGTH_LONG
            ).show()
        }
        else {
            this.saveFile(fileName)
        }
    }

    /**
     * Saves the file opened by this activity to the given file. If the file already exists, overwrites it without asking for confirmation.
     *
     * @param fileName  The name of the file to save, including the extension but excluding the complete path (which folder it should be saved in is deduced from which folder is currently open).
     */
    private fun saveFile(fileName: String) {
        val folder: Folder = this.currentFolder()
        val archive: Archive? = (folder as? Archive) ?: folder.parentArchive()
        val destinationFile = File(folder.absolutePath() + "/" + fileName)

        try {
            if (archive != null) {
                //This implementation works for regular directories as well, but isn't the fastest way of doing for regular directories
                val tempFile = File.createTempFile(
                    "openedFile",
                    "." + destinationFile.extension,
                    this.cacheDir
                )
                FileUtils.copyInputStreamToFile(this._inputStream, tempFile)
                val tempGeneralFile =
                    GeneralFile.fromPath(tempFile.absolutePath) ?: throw IOException()
                tempGeneralFile.copy(archive, fileName)
            }
            else if (folder is Directory) {
                FileUtils.copyInputStreamToFile(this._inputStream, destinationFile)
            }
            else {
                throw IOException(this.getString(R.string.cannotCreateFilesInThisPhoneFolder))
            }
        }
        catch (e: IOException) {
            Toast.makeText(
                this,
                String.format(
                    this.getString(R.string.couldNotSave),
                    e.message ?: this.getString(R.string.unknownError)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
        this.finish()
    }
}