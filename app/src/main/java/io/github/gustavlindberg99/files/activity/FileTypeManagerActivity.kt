package io.github.gustavlindberg99.files.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import io.github.gustavlindberg99.files.preferences.FileType
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.preferences.iconFromPath

/**
 * Activity to manage file types.
 */
class FileTypeManagerActivity: AppCompatActivity() {
    private val _fileTypeList: ListView by lazy {this.findViewById(R.id.FileTypeManagerActivity_fileTypeList)}
    private val _adapter by lazy {ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)}

    private val _fileTypeExtension: TextView by lazy {this.findViewById(R.id.FileTypeManagerActivity_fileTypeExtension)}
    private val _fileTypeDescription: EditText by lazy {this.findViewById(R.id.FileTypeManagerActivity_fileTypeDescription)}
    private val _alwaysShowExt: SwitchCompat by lazy {this.findViewById(R.id.FileTypeManagerActivity_alwaysShowExt)}
    private val _showInNewMenu: SwitchCompat by lazy {this.findViewById(R.id.FileTypeManagerActivity_showInNewMenu)}
    private val _openWithText: TextView by lazy {this.findViewById(R.id.FileTypeManagerActivity_openWithText)}
    private val _openWithAppName: TextView by lazy {this.findViewById(R.id.FileTypeManagerActivity_openWithAppName)}
    private val _openWithHelpText: TextView by lazy {this.findViewById(R.id.FileTypeManagerActivity_openWithHelpText)}
    private val _iconText: TextView by lazy {this.findViewById(R.id.FileTypeManagerActivity_iconText)}
    private val _iconButton: ImageButton by lazy {this.findViewById(R.id.FileTypeManagerActivity_iconButton)}
    private val _addExtensionButton: Button by lazy {this.findViewById(R.id.FileTypeManagerActivity_addExtensionButton)}
    private val _deleteExtensionButton: Button by lazy {this.findViewById(R.id.FileTypeManagerActivity_deleteExtensionButton)}

    private var _selectedFileType: FileType? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_file_type_manager)
        this.supportActionBar!!.elevation = 0.0f

        this._fileTypeList.adapter = this._adapter
        this._fileTypeList.setOnItemClickListener {_, _, position: Int, _ ->
            val selectedFileType = FileType.getAll().getOrNull(position)
            if (selectedFileType == null) {
                this.deselectFileType()
            }
            else {
                this.selectFileType(selectedFileType)
            }
        }

        this._fileTypeDescription.addTextChangedListener {
            if (this._fileTypeDescription.hasFocus()) {    //Only update the description if the user types something, not if the text is set programmatically when selecting a new file type
                this._selectedFileType?.description = this._fileTypeDescription.text.toString()
            }
        }
        this._alwaysShowExt.setOnClickListener {
            this._selectedFileType?.alwaysShowExt = this._alwaysShowExt.isChecked
        }
        this._showInNewMenu.setOnClickListener {
            this._selectedFileType?.showInNewMenu = this._showInNewMenu.isChecked
        }

        val iconSelectorLauncher = IconSelectorActivity.createResultLauncher(this, {iconPath ->
            val icon = iconFromPath(Drive.internalStorageFolder(), iconPath)
            this._selectedFileType?.iconPath = iconPath
            this._iconButton.setImageDrawable(icon)
        })
        this._iconButton.setOnClickListener {
            val intent = Intent(this, IconSelectorActivity::class.java)
            iconSelectorLauncher.launch(intent)
        }

        this._addExtensionButton.setOnClickListener {
            this.addExtension()
        }
        this._deleteExtensionButton.setOnClickListener {
            this.removeExtension()
        }

        this.deselectFileType()

        val fileTypes = FileType.getAll()
        for (fileType in fileTypes) {
            this._adapter.add(fileType.extension)
        }
    }

    /**
     * Selects the file type at a given position in the list view.
     *
     * @param selectedFileType  The file type to select.
     */
    private fun selectFileType(selectedFileType: FileType) {
        this._fileTypeExtension.text = selectedFileType.extension
        this._fileTypeDescription.clearFocus()    //Otherwise the line below will call the text changed listener and copy the newly selected file type's description to the deselected file type
        this._fileTypeDescription.setText(selectedFileType.description)
        this._alwaysShowExt.isChecked = selectedFileType.alwaysShowExt
        this._showInNewMenu.isChecked = selectedFileType.showInNewMenu
        this._iconButton.setImageDrawable(selectedFileType.icon())
        val openWithApp: ApplicationInfo? = selectedFileType.openWith()
        if (openWithApp == null) {
            this._openWithAppName.text = this.getString(R.string.unknownApp)
            this._openWithAppName.setCompoundDrawables(null, null, null, null)
        }
        else {
            this._openWithAppName.text = this.packageManager.getApplicationLabel(openWithApp)
            val appIcon: Drawable = this.packageManager.getApplicationIcon(openWithApp)
            appIcon.setBounds(0, 0, 40, 40)
            this._openWithAppName.setCompoundDrawables(appIcon, null, null, null)
        }

        this._fileTypeDescription.isEnabled = true
        this._alwaysShowExt.isEnabled = true
        this._showInNewMenu.isEnabled = true
        this.setOpenWithEnabled(true)
        this.setChangeIconEnabled(!selectedFileType.hasCustomIcon())
        this._deleteExtensionButton.isEnabled = !selectedFileType.hasCustomIcon()

        this._selectedFileType = selectedFileType
    }

    private fun deselectFileType() {
        this._fileTypeExtension.text = this.getString(R.string.selectExtension)

        this._fileTypeDescription.isEnabled = false
        this._alwaysShowExt.isEnabled = false
        this._showInNewMenu.isEnabled = false
        this.setOpenWithEnabled(false)
        this.setChangeIconEnabled(false)
        this._deleteExtensionButton.isEnabled = false

        this._selectedFileType = null
    }

    /**
     * Sets whether the open with button is enabled.
     *
     * @param enabled   True to enable it, false to disable it.
     */
    private fun setOpenWithEnabled(enabled: Boolean) {
        if (enabled) {
            this._openWithText.setTextColor(this.getColor(R.color.textColor))
            this._openWithAppName.setTextColor(this.getColor(R.color.textColor))
            this._openWithHelpText.setTextColor(this.getColor(R.color.textColor))
        }
        else {
            this._openWithText.setTextColor(this.getColor(android.R.color.darker_gray))
            this._openWithAppName.setTextColor(this.getColor(android.R.color.darker_gray))
            this._openWithHelpText.setTextColor(this.getColor(android.R.color.darker_gray))
        }
    }

    /**
     * Sets whether the change icon button is enabled.
     *
     * @param enabled   True to enable it, false to disable it.
     */
    private fun setChangeIconEnabled(enabled: Boolean) {
        this._iconButton.isClickable = enabled
        if (enabled) {
            this._iconText.setTextColor(this.getColor(R.color.textColor))
        }
        else {
            this._iconText.setTextColor(this.getColor(android.R.color.darker_gray))
            this._iconButton.setImageDrawable(null)
        }
    }

    /**
     * Prompts the user for which extension they want to add, and adds that extension to the list of known extensions.
     */
    private fun addExtension() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setHint(R.string.addFileType)

        AlertDialog.Builder(this)
            .setTitle(R.string.enterExtension)
            .setView(input)
            .setPositiveButton(R.string.ok, {_, _ ->
                val extension = input.text.toString().trim()
                val fileType = FileType(extension)
                fileType.alwaysShowExt = false
                this._adapter.add(extension)
                this._adapter.sort {a, b -> a.compareTo(b)}
                val index = FileType.getAll().indexOf(fileType)
                this._fileTypeList.setSelection(index)
                this.selectFileType(fileType)
            })
            .setNegativeButton(R.string.cancel, {dialog: DialogInterface, _ -> dialog.cancel()})
            .show()
    }

    /**
     * Asks the user for confirmation then deletes the currently selected extension from the list of known extensions.
     */
    private fun removeExtension() {
        val fileTypeToDelete = this._selectedFileType ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.deleteFileType)
            .setMessage(
                String.format(
                    this.getString(R.string.deleteFileTypeConfirmation),
                    fileTypeToDelete.extension
                )
            )
            .setPositiveButton(R.string.yes, {_, _ ->
                val index = FileType.getAll().indexOf(fileTypeToDelete)
                fileTypeToDelete.remove()
                this._adapter.remove(fileTypeToDelete.extension)
                val newSelectedFileType = FileType.getAll().getOrNull(index)
                if (newSelectedFileType == null) {
                    this.deselectFileType()
                }
                else {
                    this.selectFileType(newSelectedFileType)
                }
            })
            .setNegativeButton(R.string.no, {_, _ ->})
            .show()
    }
}