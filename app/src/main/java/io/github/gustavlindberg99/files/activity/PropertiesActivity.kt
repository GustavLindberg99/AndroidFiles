package io.github.gustavlindberg99.files.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Directory
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.FileWithCustomIcon
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.LnkFile
import io.github.gustavlindberg99.files.filesystem.ThisPhoneFolder
import io.github.gustavlindberg99.files.filesystem.UrlFile
import io.github.gustavlindberg99.files.filesystem.fileSizeToString
import io.github.gustavlindberg99.files.preferences.iconFromPath
import java.io.File
import java.io.IOException
import java.nio.file.attribute.FileTime
import java.text.DateFormat
import kotlin.concurrent.thread

class PropertiesActivity: AppCompatActivity() {
    companion object {
        public const val FILE_PATH = "filePath"
    }

    private lateinit var _file: FileOrFolder
    private val _fileName: EditText by lazy {this.findViewById(R.id.PropertiesActivity_fileName)}

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_properties)

        val path = this.intent?.getStringExtra(FILE_PATH)
        val file = if (path == null) null else FileOrFolder.fromPath(path)
        if (file == null) {
            Toast.makeText(this, R.string.couldNotViewProperties, Toast.LENGTH_LONG).show()
            this.finish()
            return
        }
        this._file = file

        this.initializeIcon()
        this.initializeFileName()
        this.initializeType()
        this.initializeSizeAndContents()
        this.initializeCompressedSize()
        this.initializeLastModified()
        this.initializeTarget()
        this.initializeMediaFolder()
    }

    public override fun onStop() {
        super.onStop()
        val newName = this._fileName.text.toString()
        if (newName != this._file.name()) {
            try {
                this._file.rename(newName)
            }
            catch (e: IOException) {
                Toast.makeText(
                    this,
                    String.format(
                        this.getString(R.string.couldNotRename),
                        e.message ?: this.getString(R.string.unknownError)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Initializes icon button with the current icon, and enable clicking it to change the icon if possible.
     */
    private fun initializeIcon() {
        val file = this._file
        val iconButton: ImageButton = this.findViewById(R.id.PropertiesActivity_iconButton)
        iconButton.setImageDrawable(file.icon())
        if (file is FileWithCustomIcon) {
            iconButton.isEnabled = true
            val iconSelectorLauncher = IconSelectorActivity.createResultLauncher(this, {iconPath ->
                val icon = iconFromPath(
                    file as? Directory ?: file.parentFolder() as? Directory,
                    iconPath
                )
                try {
                    file.setIcon(iconPath)
                    iconButton.setImageDrawable(icon)
                }
                catch (e: IOException) {
                    Toast.makeText(
                        this,
                        String.format(
                            this.getString(R.string.couldNotChangeIcon),
                            e.message ?: this.getString(R.string.unknownError)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            iconButton.setOnClickListener {
                val intent = Intent(this, IconSelectorActivity::class.java)
                intent.putExtra(IconSelectorActivity.RELATIVE_PATH_ROOT, this._file.absolutePath())
                iconSelectorLauncher.launch(intent)
            }
        }
        else {
            iconButton.isEnabled = false
        }
    }

    /**
     * Initializes the text field containing the name of this file. The callback to rename the file is set up in onStop().
     */
    private fun initializeFileName() {
        this._fileName.setText(this._file.name())
        if (this._file is ThisPhoneFolder || this._file is Drive) {
            this._fileName.isEnabled = false
        }
    }

    /**
     * Initializes the row containing the file's type.
     */
    private fun initializeType() {
        val typeRow: TextView = this.findViewById(R.id.PropertiesActivity_typeRow)
        val typeString =
            if (this._file.extension().isEmpty()) this._file.typeName()
            else String.format("%s (.%s)", this._file.typeName(), this._file.extension())
        typeRow.text = String.format(this.getString(R.string.type), typeString)
    }

    /**
     * Initializes the rows containing the file's size and this folder's contents.
     */
    private fun initializeSizeAndContents() {
        val sizeRow: TextView = this.findViewById(R.id.PropertiesActivity_sizeRow)
        val contentsRow: TextView = this.findViewById(R.id.PropertiesActivity_contentsRow)
        val file = this._file
        if (file is GeneralFile) {
            val size = file.size()
            sizeRow.text =
                String.format(this.getString(R.string.size), fileSizeToString(size), size)
            contentsRow.visibility = View.GONE
        }
        else if (file is Directory) {
            if (file is Drive) {
                sizeRow.text = String.format(
                    this.getString(R.string.freeDiskSpace),
                    fileSizeToString(file.freeDiskSpace()),
                    fileSizeToString(file.totalDiskSpace())
                )
            }
            thread {
                val (size, files, folders) = file.contentsStatistics()
                this.runOnUiThread {
                    if (file !is Drive) {
                        sizeRow.text = String.format(
                            this.getString(R.string.size),
                            fileSizeToString(size),
                            size
                        )
                    }
                    contentsRow.text = String.format(
                        this.getString(R.string.contents),
                        files,
                        folders
                    )
                }
            }
        }
        else {
            sizeRow.visibility = View.GONE
            contentsRow.visibility = View.GONE
        }
    }

    /**
     * Initializes the row containing the file's compressed size.
     */
    private fun initializeCompressedSize() {
        val compressedSizeRow: TextView =
            this.findViewById(R.id.PropertiesActivity_compressedSizeRow)
        val compressedSize = (this._file as? GeneralFile)?.compressedSize()
        if (compressedSize != null) {
            compressedSizeRow.text = String.format(
                this.getString(R.string.compressedSize),
                fileSizeToString(compressedSize),
                compressedSize
            )
        }
        else {
            compressedSizeRow.visibility = View.GONE
        }
    }

    /**
     * Initializes the row containing the file's last modified date.
     */
    private fun initializeLastModified() {
        val lastModifiedRow: TextView =
            this.findViewById(R.id.PropertiesActivity_lastModifiedRow)
        val lastModified: FileTime? = this._file.lastModified()
        if (this._file !is ThisPhoneFolder && lastModified != null) {
            lastModifiedRow.text = String.format(
                this.getString(R.string.lastModified),
                DateFormat.getDateInstance().format(lastModified.toMillis())
            )
        }
        else {
            lastModifiedRow.visibility = View.GONE
        }
    }

    /**
     * Initializes the row containing the shortcut's target.
     */
    private fun initializeTarget() {
        val targetRow: TextView = this.findViewById(R.id.PropertiesActivity_targetRow)
        val file = this._file
        if (file is LnkFile) {
            targetRow.text = String.format(
                this.getString(R.string.target),
                file.target()?.absolutePath()
            )
        }
        else if (file is UrlFile) {
            targetRow.text = String.format(this.getString(R.string.target), file.targetUrl())
        }
        else {
            targetRow.visibility = View.GONE
        }
    }

    /**
     * Initializes the switch allowing to choose if this directory is a media folder.
     */
    private fun initializeMediaFolder() {
        val mediaFolderSwitch: SwitchCompat by lazy {this.findViewById(R.id.PropertiesActivity_mediaFolderSwitch)}
        val file = this._file
        if (file is Directory) {
            mediaFolderSwitch.isEnabled = file.canBeMediaDirectory()
            mediaFolderSwitch.isChecked = file.isMediaDirectory()
            mediaFolderSwitch.setOnCheckedChangeListener {_, isChecked: Boolean ->
                val nomediaFile = File(file.absolutePath() + "/.nomedia")
                try {
                    if (isChecked) {
                        if (!nomediaFile.delete()) {
                            throw IOException(
                                String.format(
                                    this.getString(R.string.failedToDelete),
                                    nomediaFile.absolutePath
                                )
                            )
                        }
                    }
                    else {
                        if (!nomediaFile.createNewFile()) {
                            throw IOException(
                                String.format(
                                    this.getString(R.string.failedToCreateFile),
                                    nomediaFile.absolutePath
                                )
                            )
                        }
                    }
                }
                catch (e: IOException) {
                    Toast.makeText(
                        this,
                        String.format(
                            this.getString(R.string.nomediaError),
                            e.message ?: this.getString(R.string.unknownError)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                mediaFolderSwitch.isChecked = !isChecked
            }
        }
        else {
            mediaFolderSwitch.visibility = View.GONE
        }
    }
}