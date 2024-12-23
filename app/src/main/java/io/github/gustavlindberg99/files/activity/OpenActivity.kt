package io.github.gustavlindberg99.files.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Directory
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.preferences.FileType

/**
 * Class to show an activity that allows selecting a file.
 *
 * The FILE_TYPES extra can be passed to this activity's intent. If it is, the user will be shown a list of file types to filter. It should be passed as Array<Bundle<FILE_TYPE_DESCRIPTION: String, FILE_TYPE_EXTENSIONS: Array<String>>.
 */
class OpenActivity: BrowseActivity() {
    companion object {
        public const val FILE_TYPES = "fileTypes"
        public const val FILE_TYPE_DESCRIPTION = "fileTypeDescription"
        public const val FILE_TYPE_EXTENSIONS = "fileTypeExtensions"
        public const val ALLOW_SELECTING_DIRECTORIES = "allowDirectories"
        private const val SELECTED_FILE_PATH = "selectedFilePath"

        /**
         * Creates a launcher that can launch this activity and get the selected file as result.
         *
         * @param activity  The activity from which this activity should be launched.
         * @param callback  A callback that will be called when this activity finishes if a file was selected, in which case that file is passed as parameter. If no file was selected or if the selected file doesn't exist, the callback won't be called.
         *
         * @return The activity result launcher.
         */
        public fun createResultLauncher(
            activity: ComponentActivity,
            callback: (FileOrFolder) -> Unit
        ): ActivityResultLauncher<Intent> = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), {
                val intent: Intent? = it.data
                val absolutePath: String? = intent?.getStringExtra(SELECTED_FILE_PATH)
                if (absolutePath != null) {
                    val selectedFile: FileOrFolder? = FileOrFolder.fromPath(absolutePath)
                    val allowSelectingDirectories: Boolean =
                        intent.getBooleanExtra(ALLOW_SELECTING_DIRECTORIES, false)
                    if (selectedFile == null) {
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.fileDoesNotExist),
                                absolutePath
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else if (!allowSelectingDirectories && selectedFile is Directory) {
                        Toast.makeText(activity, R.string.cannotSelectDirectory, Toast.LENGTH_LONG)
                            .show()
                    }
                    else {
                        callback(selectedFile)
                    }
                }
            }
        )
    }

    protected override val finishButtonText = R.string.open

    protected override val fileTypeInputItems by lazy {
        //Suppress deprecation warnings because getParcelableArrayExtra is deprecated but the new way isn't available until API level 33
        @Suppress("DEPRECATION")
        this.intent.getParcelableArrayExtra(FILE_TYPES)
            ?.filterIsInstance<Bundle>()
            ?.map {
                (it.getString(FILE_TYPE_DESCRIPTION) ?: return@map null) to
                (it.getStringArray(FILE_TYPE_EXTENSIONS)?.map {ext -> FileType(ext)}
                    ?: return@map null)
            }?.filterNotNull() ?: listOf()
    }

    protected override fun finishButtonListener(fileBaseName: String) {
        val allowSelectingDirectories: Boolean =
            this.intent.getBooleanExtra(ALLOW_SELECTING_DIRECTORIES, false)
        val folder: Folder = this.currentFolder()
        val intent = Intent()
        if (fileBaseName == "") {
            if (allowSelectingDirectories) {
                intent.putExtra(SELECTED_FILE_PATH, folder.absolutePath())
            }
            else {
                Toast.makeText(this, R.string.cannotSelectDirectory, Toast.LENGTH_LONG).show()
                return
            }
        }
        else {
            intent.putExtra(SELECTED_FILE_PATH, folder.absolutePath() + "/" + fileBaseName)
        }
        intent.putExtra(ALLOW_SELECTING_DIRECTORIES, allowSelectingDirectories)
        this.setResult(RESULT_OK, intent)
        this.finish()
    }
}