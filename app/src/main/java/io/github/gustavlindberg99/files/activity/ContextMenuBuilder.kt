package io.github.gustavlindberg99.files.activity

import android.app.AlertDialog
import android.content.Intent
import android.view.ContextMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Directory
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.LnkFile
import io.github.gustavlindberg99.files.filesystem.SevenZArchive
import io.github.gustavlindberg99.files.filesystem.TarArchive
import io.github.gustavlindberg99.files.filesystem.ThisPhoneFolder
import io.github.gustavlindberg99.files.filesystem.ZipArchive
import io.github.gustavlindberg99.files.preferences.FileType
import io.github.gustavlindberg99.files.preferences.Preferences
import java.io.File
import java.io.IOException

/**
 * Class for creating any context menu in the file explorer activity.
 *
 * @param _activity         The activity to create the context menu in.
 * @param _menu             The context menu to add the items to.
 * @param _refreshLauncher  An ActivityResultLauncher that refreshes the file explorer activity when the secondary activity finishes.
 */
class ContextMenuBuilder(
    private val _activity: FileExplorerActivity,
    private val _menu: ContextMenu,
    private val _refreshLauncher: ActivityResultLauncher<Intent>,
    private val _newMenuFileCreator: NewMenuFileCreator
) {
    /**
     * Adds the appropriate items to the context menu depending on the current state of the activity.
     *
     * @param destination   The destination file or folder if the context menu is opened because the user dropped a file or folder onto another file or folder. Null otherwise.
     */
    public fun createContextMenus(destination: FileOrFolder?) {
        val selectedFiles = this._activity.selectedFiles()

        if (destination == null) {
            if (selectedFiles.isEmpty() && this._activity.currentFolder() !is ThisPhoneFolder) {
                this.addNewMenu()
            }
            if (selectedFiles.size == 1) {
                this.addOpenMenuItem()
                if (selectedFiles.all {it is GeneralFile}) {
                    this.addOpenWithMenuItem()
                }
            }
            if (selectedFiles.isEmpty()) {
                if (this._activity.currentFolder() in Preferences.pinnedFolders) {
                    this.addUnpinMenuItem()
                }
                else {
                    this.addPinMenuItem()
                }
            }
            else if (selectedFiles.all {it is Folder}) {
                if (selectedFiles.all {it in Preferences.pinnedFolders}) {
                    this.addUnpinMenuItem()
                }
                else if (selectedFiles.all {it !in Preferences.pinnedFolders}) {
                    this.addPinMenuItem()
                }
            }
            if (!selectedFiles.isEmpty() && selectedFiles.all {it !is Drive && it !is ThisPhoneFolder}) {
                this.addDeleteMenuItem()
            }
            if (selectedFiles.size == 1 && selectedFiles.first() !is Drive) {
                this.addRenameMenuItem()
            }
            if (!selectedFiles.isEmpty() && this._activity.currentFolder() !is ThisPhoneFolder) {
                this.addCompressMenu()
            }
            this.addPropertiesMenuItem()
        }
        else if (destination is Folder) {
            this.addMoveHereMenuItem(destination)
            this.addCopyHereMenuItem(destination)
            this._menu.add(R.string.cancel)
        }
        else if (destination is LnkFile) {
            val target = destination.target()
            if (target is Folder) {
                this.addMoveHereMenuItem(target)
                this.addCopyHereMenuItem(target)
                this._menu.add(R.string.cancel)
            }
        }
    }

    /**
     * Adds a submenu allowing to create new files and folders.
     */
    private fun addNewMenu() {
        val newMenu = this._menu.addSubMenu(R.string.createNew)

        newMenu.add(R.string.folder).setOnMenuItemClickListener {
            this._newMenuFileCreator.createFile(null)
            true
        }
        for (fileType in FileType.getAll()) {
            if (fileType.showInNewMenu) {
                newMenu.add(fileType.description).setOnMenuItemClickListener {
                    this._newMenuFileCreator.createFile(fileType)
                    true
                }
            }
        }
    }

    /**
     * Adds a menu item allowing to open the selected file(s).
     */
    private fun addOpenMenuItem() {
        this._menu.add(R.string.open).setOnMenuItemClickListener {
            for (file in this._activity.selectedFiles()) {
                file.open(this._activity)
            }
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to open the selected file(s) with an app of their choice.
     */
    private fun addOpenWithMenuItem() {
        this._menu.add(R.string.openWith).setOnMenuItemClickListener {
            this._activity.selectedFiles().filterIsInstance<GeneralFile>().firstOrNull()
                ?.showOpenWithDialog(this._activity)
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to pin the selected folder(s) to the navigation drawer.
     */
    private fun addPinMenuItem() {
        this._menu.add(R.string.pin).setOnMenuItemClickListener {
            if (this._activity.selectedFiles().isEmpty()) {
                Preferences.pinnedFolders += listOf(this._activity.currentFolder())
            }
            else {
                Preferences.pinnedFolders += this._activity.selectedFiles()
                    .filterIsInstance<Folder>()
            }
            this._activity.updatePinnedFolders()
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to unpin the selected folder(s) from the navigation drawer.
     */
    private fun addUnpinMenuItem() {
        this._menu.add(R.string.unpin).setOnMenuItemClickListener {
            if (this._activity.selectedFiles().isEmpty()) {
                Preferences.pinnedFolders -= listOf(this._activity.currentFolder())
            }
            else {
                Preferences.pinnedFolders -= this._activity.selectedFiles()
                    .filterIsInstance<Folder>()
            }
            this._activity.updatePinnedFolders()
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to delete the selected file(s).
     */
    private fun addDeleteMenuItem() {
        this._menu.add(R.string.delete).setOnMenuItemClickListener {
            val files = this._activity.selectedFiles()
            AlertDialog.Builder(this._activity)
                .setTitle(R.string.delete)
                .setMessage(
                    if (files.size == 1) String.format(
                        this._activity.getString(R.string.deleteConfirmation),
                        files.first().baseName()
                    )
                    else this._activity.getString(R.string.multipleDeleteConfirmation)
                )
                .setPositiveButton(R.string.yes, {_, _ ->
                    for (file in files) {
                        try {
                            file.delete()
                            this._activity.refresh()
                        }
                        catch (e: IOException) {
                            Toast.makeText(
                                this._activity,
                                String.format(
                                    this._activity.getString(R.string.couldNotDelete),
                                    file.name(),
                                    e.message ?: this._activity.getString(R.string.unknownError)
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
                .setNegativeButton(R.string.no, {_, _ ->})
                .show()
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to rename the selected file.
     */
    private fun addRenameMenuItem() {
        this._menu.add(R.string.rename).setOnMenuItemClickListener {
            val file = this._activity.selectedFiles().first()
            val originalName = file.baseName()
            val extensionToAdd =
                if (originalName == file.name() || file.extension().isEmpty()) ""
                else "." + file.extension()
            this._activity.showInputDialog(
                this._activity.getString(R.string.rename) + " " + originalName,
                originalName,
                false,
                {
                    try {
                        file.rename(it + extensionToAdd)
                        this._activity.refresh()
                    }
                    catch (e: IOException) {
                        Toast.makeText(
                            this._activity,
                            String.format(
                                this._activity.getString(R.string.couldNotRename),
                                e.message ?: this._activity.getString(R.string.unknownError)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a submenu allowing to compress the selected files and folders.
     */
    private fun addCompressMenu() {
        val compressMenu = this._menu.addSubMenu(R.string.compress)

        for ((extension, compressFunction) in mapOf(
            "zip" to ZipArchive::compress,
            "tar" to TarArchive::compress,
            "7z" to SevenZArchive::compress
        )) {
            compressMenu.add(FileType(extension).description).setOnMenuItemClickListener {
                val archiveBaseName = this._activity.selectedFiles().firstOrNull()?.absolutePath()
                    ?: return@setOnMenuItemClickListener false
                var archiveName = "$archiveBaseName.$extension"
                var duplicateCounter = 2
                while (File(archiveName).exists()) {
                    archiveName = "$archiveBaseName ($duplicateCounter).$extension"
                    duplicateCounter++
                }
                try {
                    compressFunction(this._activity.selectedFiles(), archiveName, null, listOf())
                }
                catch (e: IOException) {
                    Toast.makeText(
                        this._activity,
                        String.format(
                            this._activity.getString(R.string.failedToCompress),
                            e.message ?: this._activity.getString(R.string.unknownError)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                this._activity.refresh()
                return@setOnMenuItemClickListener true
            }
        }
    }

    /**
     * Adds a menu item allowing to view the properties of the selected file.
     */
    private fun addPropertiesMenuItem() {
        this._menu.add(R.string.properties).setOnMenuItemClickListener {
            val intent = Intent(this._activity, PropertiesActivity::class.java)
            val file =
                this._activity.selectedFiles().firstOrNull() ?: this._activity.currentFolder()
            intent.putExtra(PropertiesActivity.FILE_PATH, file.absolutePath())
            this._refreshLauncher.launch(intent)
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to move the selected file(s) to the specified destination.
     *
     * @param destination   The folder to move the files to.
     */
    private fun addMoveHereMenuItem(destination: Folder) {
        this._menu.add(R.string.moveHere).setOnMenuItemClickListener {
            for (file in this._activity.selectedFiles()) {
                this.askForOverwriteConfirmation(file, destination, R.string.move, {
                    try {
                        file.move(destination)
                        this._activity.refresh()
                    }
                    catch (e: IOException) {
                        Toast.makeText(
                            this._activity,
                            String.format(
                                this._activity.getString(R.string.couldNotMove),
                                file.baseName(),
                                e.message ?: this._activity.getString(R.string.unknownError)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Adds a menu item allowing to copy the selected file(s) to the specified destination.
     *
     * @param destination   The folder to copy the files to.
     */
    private fun addCopyHereMenuItem(destination: Folder) {
        this._menu.add(R.string.copyHere).setOnMenuItemClickListener {
            for (file in this._activity.selectedFiles()) {
                this.askForOverwriteConfirmation(file, destination, R.string.copy, {
                    try {
                        file.copy(destination)
                        this._activity.refresh()
                    }
                    catch (e: IOException) {
                        Toast.makeText(
                            this._activity,
                            String.format(
                                this._activity.getString(R.string.couldNotCopy),
                                file.baseName(),
                                e.message ?: this._activity.getString(R.string.unknownError)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
            return@setOnMenuItemClickListener true
        }
    }

    /**
     * Checks if any files will be overwritten, and if so, asks for conformation to overwrite them. If not, calls the callback directly.
     *
     * @param file          The file or folder that will be copied.
     * @param destination   The folder that it will be copied to.
     * @param titleResource The resource ID of the string to use as title for the dialog.
     * @param callback      The callback to run if there's no risk of overwriting anything, or if the user says that they do want to overwrite.
     */
    private fun askForOverwriteConfirmation(
        file: FileOrFolder,
        destination: Folder,
        titleResource: Int,
        callback: () -> Unit
    ) {
        val path = destination.absolutePath() + "/" + file.name()
        if (file is GeneralFile && GeneralFile.fromPath(path) != null) {
            AlertDialog.Builder(this._activity)
                .setTitle(titleResource)
                .setMessage(R.string.overwriteConfirmation)
                .setPositiveButton(R.string.yes, {_, _ -> callback()})
                .setNegativeButton(R.string.no, {_, _ ->})
                .show()
        }
        else if (file is Directory && Directory.fromPath(path) != null) {
            AlertDialog.Builder(this._activity)
                .setTitle(titleResource)
                .setMessage(R.string.mergeConfirmation)
                .setPositiveButton(R.string.yes, {_, _ -> callback()})
                .setNegativeButton(R.string.no, {_, _ ->})
                .show()
        }
        else {
            //This will be called if the destination doesn't exist yet (in which case we don't need to ask for confirmation), or if trying to overwrite a file with a folder or vice-versa (in which case the copy/move function will fail anyway).
            callback()
        }
    }
}