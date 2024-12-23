package io.github.gustavlindberg99.files.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Archive
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.LnkFile
import io.github.gustavlindberg99.files.filesystem.UrlFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder

/**
 * The main activity, in which the list of files is displayed.
 */
class MainActivity: FileExplorerActivity(R.layout.activity_main) {
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = this.intent.data
        if (uri != null) {
            this.handleOpenFile(uri)
        }
    }

    protected override fun onFileClick(view: FileView) {
        view.file.open(this)
    }

    protected override fun fileShouldBeShown(file: FileOrFolder): Boolean {
        return true
    }

    /**
     * Handles a request from another app to open a file with this app.
     *
     * @param uri   The URI to open.
     */
    private fun handleOpenFile(uri: Uri) {
        val path: String? = this.createTemporaryFileIfNeeded(uri)
        val file: GeneralFile? =
            if (path == null) null
            else LnkFile.fromPath(path)
                ?: UrlFile.fromPath(path)
                ?: Archive.fromPath(path)
        if (file == null) {
            Toast.makeText(
                this,
                String.format(
                    this.getString(R.string.couldNotOpen),
                    this.getString(R.string.formatNotSupported)
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
        else {
            file.open(this)
        }
        if (file !is Folder && (file !is LnkFile || file.target() !is Folder)) {
            this.finish()
        }
    }

    /**
     * If the URI doesn't correspond to a file in the file system, creates a temporary file for it from an input stream.
     *
     * @param uri   The URI to read the file or input stream from.
     *
     * @return If the URI corresponds to a file in the file system, returns the path of that file, otherwise returns the path to a temporary file with the same contents as the input stream at the URI. If the URI is invalid, returns null.
     */
    private fun createTemporaryFileIfNeeded(uri: Uri): String? {
        val uriPath: String = uri.path ?: return null
        val decodedUriPath: String = URLDecoder.decode(uriPath, "UTF-8")
        val inputStream: InputStream = this.contentResolver.openInputStream(uri) ?: return null

        //If it corresponds to a file, just return the path to that file
        val internalStorageFolder = Drive.internalStorageFolder()
        for (fileAtRoot in internalStorageFolder.files()) {
            val fileNameRegex = Regex("(^|/|:)" + Regex.escape(fileAtRoot.name()) + "(/|$)")
            for (path in listOf(uriPath, decodedUriPath)) {
                for (match in fileNameRegex.findAll(path)) {
                    val startIndex = match.range.first + (match.groups[1]?.value?.length ?: 0)
                    val fileSystemPath =
                        internalStorageFolder.absolutePath() + "/" + path.substring(startIndex)
                    if (File(fileSystemPath).readBytes() contentEquals inputStream.readBytes()) {
                        return fileSystemPath
                    }
                    else {
                        inputStream.reset()
                    }
                }
            }
        }

        //If it doesn't correspond to a file, create a temporary file and return the temporary file's path
        try {
            val tempFile = File.createTempFile(
                "openedFile",
                "." + File(uriPath).extension,
                this.cacheDir
            )
            FileUtils.copyInputStreamToFile(inputStream, tempFile)
            return tempFile.absolutePath
        }
        catch (_: IOException) {
            return null
        }
    }
}