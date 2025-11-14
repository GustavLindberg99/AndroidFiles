package io.github.gustavlindberg99.files.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.LnkFile
import io.github.gustavlindberg99.files.preferences.FileType

abstract class BrowseActivity: FileExplorerActivity(R.layout.activity_browse) {
    private val _fileNameInput: EditText by lazy {this.findViewById(R.id.BrowseActivity_fileNameInput)}
    private val _finishButton: Button by lazy {this.findViewById(R.id.BrowseActivity_finishButton)}
    private val _fileTypeInput: Spinner by lazy {this.findViewById(R.id.BrowseActivity_fileTypeInput)}

    protected abstract val finishButtonText: Int
    protected abstract val fileTypeInputItems: List<Pair<String, List<FileType>>>

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar!!.elevation = 0.0f

        this._finishButton.setText(this.finishButtonText)

        this._finishButton.setOnClickListener {
            this.finishButtonListener(this._fileNameInput.text.toString())
        }

        this._fileTypeInput.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                this@BrowseActivity.refresh()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                this@BrowseActivity.refresh()
            }
        }
    }

    protected override fun onStart() {
        super.onStart()

        //This needs to go in onStart and not in onCreate because fileTypeInputItems is uninitialized in onCreate
        val fileTypesInList = this.fileTypeInputItems.map {(description, types) ->
            description + " (" + types.joinToString("; ") {"*.${it.extension}"} + ")"
        } + listOf(this.getString(R.string.allFiles) + " (*.*)")
        this._fileTypeInput.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fileTypesInList
        )
    }

    protected override fun fileShouldBeShown(file: FileOrFolder): Boolean {
        val selectedTypes = this.selectedFileTypes()
        return file is Folder || selectedTypes == null || FileType(file.extension()) in selectedTypes || (file is LnkFile && file.target() is Folder)
    }

    protected override fun onFileClick(view: FileView) {
        val file: FileOrFolder = view.file
        val lnkTarget: FileOrFolder? = (file as? LnkFile)?.target()
        if (file is Folder) {
            this.openFolder(file)
        }
        else if (lnkTarget is Folder) {
            this.openFolder(lnkTarget)
        }
        else {
            this.finishButtonListener(file.name())
        }
    }

    /**
     * A callback that should be called when the finish button is pressed.
     *
     * @param fileBaseName  The file name entered in the file name input.
     */
    protected abstract fun finishButtonListener(fileBaseName: String)

    /**
     * Gets the file types that are selected in the file type input.
     *
     * @return A list containing all the file types in the selected option of the file type input, or null if the "All files" option is selected.
     */
    public fun selectedFileTypes(): List<FileType>? {
        return this.fileTypeInputItems.getOrNull(this._fileTypeInput.selectedItemPosition)?.second
    }

    /**
     * Sets the text in the file name input.
     *
     * @param fileBaseName  The file name to set.
     */
    public fun setFileNameInputText(fileBaseName: String) {
        this._fileNameInput.setText(fileBaseName)
    }
}