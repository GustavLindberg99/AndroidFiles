package io.github.gustavlindberg99.files.activity

import android.app.AlertDialog
import android.content.ClipData
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.ContextMenu
import android.view.DragEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.allViews
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.LnkFile
import io.github.gustavlindberg99.files.filesystem.ThisPhoneFolder
import io.github.gustavlindberg99.files.preferences.Preferences
import io.github.gustavlindberg99.files.preferences.StoragePermissionsRequest
import org.apache.commons.validator.routines.UrlValidator
import java.io.File
import kotlin.concurrent.thread

private const val SAVED_HISTORY = "history"

/**
 * Abstract base class for any file explorer, both the main activity and file browse dialogs.
 */
abstract class FileExplorerActivity(private val _layoutResId: Int): AppCompatActivity() {
    private val _history = mutableListOf<Folder>()

    private val _drawerLayout: DrawerLayout by lazy {this.findViewById(R.id.FileExplorerActivity_drawerLayout)}
    private val _navLayout: LinearLayout by lazy {this.findViewById(R.id.FileExplorerActivity_navLayout)}
    private val _addressBar: AddressBarView by lazy {this.findViewById(R.id.FileExplorerActivity_addressBar)}

    private val _swipeRefreshLayout: SwipeRefreshLayout by lazy {this.findViewById(R.id.FileExplorerActivity_swipeRefreshLayout)}
    private val _fileList: LinearLayout by lazy {this.findViewById(R.id.FileExplorerActivity_fileList)}
    private val _loadingText: TextView by lazy {this.findViewById(R.id.FileExplorerActivity_loadingText)}
    private val _emptyFolderText: TextView by lazy {this.findViewById(R.id.FileExplorerActivity_emptyFolderText)}

    private val _hiddenFileViews = mutableListOf<FileView>()
    private var _longClickedView: FileView? = null
    private var _currentlyLoadingId: Any? = null

    private lateinit var _storagePermissionsRequest: StoragePermissionsRequest
    private lateinit var _refreshLauncher: ActivityResultLauncher<Intent>
    private lateinit var _newMenuFileCreator: NewMenuFileCreator

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(this._layoutResId)

        this._refreshLauncher =
            this.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), {
                this.refresh()
            })

        this._newMenuFileCreator = NewMenuFileCreator(this)

        val savedHistory = savedInstanceState?.getStringArrayList(SAVED_HISTORY)
        if (savedHistory != null) {
            this._history.clear()
            this._history.addAll(savedHistory.mapNotNull {Folder.fromPath(it)})
        }

        this._addressBar.setOnSelectedFolderChangedListener {folder: Folder? ->
            if (folder == null) {
                Toast.makeText(this, R.string.folderNotFound, Toast.LENGTH_LONG).show()
            }
            else {
                folder.open(this)
                this._addressBar.editing = false
            }
        }

        this._addressBar.setOnParentFolderButtonClickListener {
            this.currentFolder().parentFolder()?.open(this)
        }

        this.onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            public override fun handleOnBackPressed() {
                this@FileExplorerActivity.onBackButtonPressed()
            }
        })

        this._addressBar.setOnNavButtonClickListener {
            if (this._drawerLayout.isDrawerOpen(GravityCompat.START)) {
                this._drawerLayout.closeDrawer(GravityCompat.START)
            }
            else {
                this._drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        this._addressBar.setOnSettingsButtonClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            this._refreshLauncher.launch(intent)
        }

        this._swipeRefreshLayout.setOnRefreshListener {
            this.refresh()
            this._swipeRefreshLayout.isRefreshing = false
        }

        this._storagePermissionsRequest = StoragePermissionsRequest(this)
        this._storagePermissionsRequest.setOnPermissionsGrantedListener {
            this.openFolder(Drive.internalStorageFolder())
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val history = this._history.map {it.absolutePath()}
        outState.putStringArrayList(SAVED_HISTORY, ArrayList(history))
    }

    protected override fun onResume() {
        super.onResume()

        this.updatePinnedFolders()

        //If no folder is open yet, try opening the external storage folder
        if (this._history.isEmpty()) {
            this._storagePermissionsRequest.requestPermissions()
        }
        //Otherwise refresh the current folder (otherwise it will just show "Loading...")
        else {
            this.refresh()
        }
    }

    public override fun onCreateContextMenu(
        menu: ContextMenu,
        view: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, view, menuInfo)

        val destination: FileOrFolder? =
            if (view is FileView && !view.fileSelected)
                view.file
            else if (this._addressBar.viewIsParentFolderButton(view))
                this.currentFolder().parentFolder()
            else null

        ContextMenuBuilder(
            this,
            menu,
            this._refreshLauncher,
            this._newMenuFileCreator
        ).createContextMenus(destination)
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this._storagePermissionsRequest.handleRequestPermissionsResult(requestCode)
    }

    /**
     * Function that should be called when the back button is pressed.
     */
    private fun onBackButtonPressed() {
        if (this._drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this._drawerLayout.closeDrawer(GravityCompat.START)
        }
        else if (this._addressBar.editing) {
            this._addressBar.editing = false
        }
        else if (!this.selectedFiles().isEmpty()) {
            for (view in this.fileViews()) {
                view.fileSelected = false
            }
        }
        else if (this._history.size > 1) {
            this._history.removeAt(this._history.lastIndex)
            this.refresh()
        }
        else {
            //Default implementation of handleOnBackPressed() in the simplest case (which is always the case here), see https://stackoverflow.com/a/34950074/4284627
            this.finishAfterTransition()
        }
    }

    /**
     * Updates the nav view so that it contains the current pinned folders.
     */
    public fun updatePinnedFolders() {
        this._navLayout.removeAllViews()
        for (pinnedFolder in Preferences.pinnedFolders) {
            val view = TextView(this)
            view.text = pinnedFolder.name()
            view.setTextColor(this.getColor(R.color.textColor))
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            view.setPadding(8)
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                pinnedFolder.icon(),
                null,
                null,
                null
            )
            view.setOnClickListener {
                this.openFolder(pinnedFolder)
                this._drawerLayout.closeDrawers()
            }
            this._navLayout.addView(view)
        }
    }

    /**
     * Gets all the file views that are currently visible.
     *
     * @return All the views that are currently visible.
     */
    private fun fileViews(): List<FileView> {
        return this._fileList.allViews.filterIsInstance<FileView>().toList()
    }

    /**
     * Returns which files are selected.
     *
     * @return A collection containing all selected files.
     */
    public fun selectedFiles(): Collection<FileOrFolder> {
        return this.fileViews().filter {it.fileSelected}.map {it.file}
    }

    /**
     * Opens the specified folder.
     *
     * @param folder    The folder to open.
     */
    public fun openFolder(folder: Folder) {
        val file = File(folder.absolutePath())
        if (folder !is ThisPhoneFolder && file.isDirectory && file.listFiles() == null) {
            Toast.makeText(this, R.string.folderNotFound, Toast.LENGTH_LONG).show()
        }
        else {
            this._history.add(folder)
            this.refresh()
        }
    }

    /**
     * Gets the currently opened folder.
     *
     * @return The currently opened folder.
     */
    public fun currentFolder(): Folder {
        return this._history.last()
    }

    /**
     * Refreshes the current folder.
     */
    public fun refresh() {
        val unrefreshedFolder = this.currentFolder()
        val folder = Folder.fromPath(unrefreshedFolder.absolutePath()) ?: unrefreshedFolder

        //Set the address bar path
        this._addressBar.setSelectedFolder(folder)

        var finished = false
        thread {
            //Set the currently loading ID to an object that's unique for this scope (it doesn't matter what it is, only its memory address matters)
            this._currentlyLoadingId = folder

            //List the files and create the views on a separate thread because this might be slow
            val viewsToAdd = this.viewsInFolder(folder)
            finished = true

            //When finished, do all the UI stuff on the UI thread
            this.runOnUiThread {
                //If another folder has already been opened while this one was loading, don't do anything
                if (this._currentlyLoadingId === folder) {
                    this._currentlyLoadingId = null

                    //Remove all views except the first one which is the "This folder is empty" view
                    this._fileList.removeViews(1, this.fileViews().size)

                    //Keep the old views because inflating new views is slow. When they will get used again, their file will be set to a new value.
                    this._hiddenFileViews += this.fileViews()

                    //Add the new views
                    for (view in viewsToAdd) {
                        this._fileList.addView(view)
                    }
                    this._emptyFolderText.visibility =
                        if (viewsToAdd.isEmpty()) View.VISIBLE else View.GONE

                    //Hide the loading text
                    this._loadingText.visibility = View.GONE
                    this._swipeRefreshLayout.visibility = View.VISIBLE
                }
            }
        }

        //Wait for 300ms before showing the loading view so that it doesn't flash if the folder loads fast enough
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 300 && !finished);

        //If it's not finished loading, show the loading view
        if (!finished) {
            this._loadingText.visibility = View.VISIBLE
            this._swipeRefreshLayout.visibility = View.GONE
        }
    }

    /**
     * Checks if the specified file or folder should be shown. If the filename starts with a dot and the preference to show hidden files is off, the file won't be shown anyway.
     *
     * @param file  The file or folder to check if it should be shown.
     *
     * @return True if the file or folder should be shown, false otherwise.
     */
    protected abstract fun fileShouldBeShown(file: FileOrFolder): Boolean

    /**
     * Gets the file views that should be displayed when opening a given folder.
     *
     * @param folder    The folder to get the files in.
     *
     * @return A list of the views that should be displayed when opening the given folder.
     */
    private fun viewsInFolder(folder: Folder): List<FileView> {
        val files: List<FileOrFolder> =
            folder.files().sortedBy {it.name().lowercase()}.sortedBy {it is GeneralFile}

        val result = mutableListOf<FileView>()
        for (file in files) {
            if ((file.hidden() && !Preferences.showHiddenFiles) || !this.fileShouldBeShown(file)) {
                continue
            }
            result.add(this.createFileView(file))
        }
        return result
    }

    /**
     * Creates a file view for the specified file or folder.
     *
     * @param file  The file or folder to create the view for.
     *
     * @return The created view.
     */
    private fun createFileView(file: FileOrFolder): FileView {
        val view: FileView = this._hiddenFileViews.removeFirstOrNull() ?: FileView(this)
        view.file = file
        view.setOnClickListener {
            if (this.selectedFiles().isEmpty()) {
                this.onFileClick(view)
            }
            else {
                view.fileSelected = !view.fileSelected
            }
        }
        view.setOnLongClickListener {this.onFileLongClick(view)}
        view.setOnDragListener {_, event -> this.onFileDrag(view, event)}
        return view
    }

    /**
     * Function that should be called when a file view is clicked on and no files are currently selected (if files are currently selected, clicking on a file toggles its selected status).
     *
     * @param view  The view that has been clicked on.
     */
    protected abstract fun onFileClick(view: FileView)

    /**
     * Function that should be called when a file view is long clicked on.
     *
     * @param view  The view that has been clicked on.
     *
     * @return True for compatibility with setOnLongClickListener.
     */
    private fun onFileLongClick(view: FileView): Boolean {
        if (view.file in this.selectedFiles()) {
            val dragData =
                ClipData.newPlainText(
                    view.file.baseName(),
                    view.file.absolutePath()
                )
            val dragShadow = View.DragShadowBuilder(view)
            view.startDragAndDrop(dragData, dragShadow, null, 0)
            view.updateDragShadow(null)    //Initially hide the drag shadow, we don't want it to be shown immediately because it might be a long click to show the context menu
            this._longClickedView = view
        }
        else {
            view.fileSelected = true
        }
        return true
    }

    /**
     * Function that should be called when a drag event occurs on a file view.
     *
     * @param view  The view that has been clicked on.
     * @param event The drag event.
     *
     * @return True for compatibility with setOnDragListener.
     */
    private fun onFileDrag(view: FileView, event: DragEvent): Boolean {
        if (event.action == DragEvent.ACTION_DROP) {
            val file = view.file
            if (//If it's dropped onto itself, check if it's left the initial view. If it hasn't, it's a regular long click in which case show the context menu, otherwise do nothing.
                (file in this.selectedFiles() && this._longClickedView == view)
                //If it's dropped onto something else, it's a copy/move. Always show the context menu in this case, the main activity will handle showing the correct options.
                || (file !in this.selectedFiles() && (file is Folder || (file is LnkFile && file.target() is Folder)))
            ) {
                view.showContextMenu(event.x, event.y)
            }
            this._longClickedView = null
        }
        else if (event.action == DragEvent.ACTION_DRAG_EXITED && _longClickedView != null) {
            //If the drag moves out of the original view, show the drag shadow because then we're sure that it's a drag and drop and not a simple long click
            view.updateDragShadow(View.DragShadowBuilder(_longClickedView))
            this._longClickedView = null
        }
        return true
    }

    /**
     * Shows a dialog allowing the user to input text.
     *
     * @param title         The title of the dialog.
     * @param defaultValue  The value to show by default.
     * @param isUrl         If true, a URL will be expected as input. If false, a file name will be expected as input.
     * @param callback      The callback to run when the dialog is closed. The parameter contains the selected name and is guaranteed to be valid. If the dialog is canceled or if an invalid name is specified, the callback is never run.
     */
    public fun showInputDialog(
        title: String,
        defaultValue: String,
        isUrl: Boolean,
        callback: (String) -> Unit
    ) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        if (isUrl) {
            input.inputType = input.inputType or InputType.TYPE_TEXT_VARIATION_URI
            input.setHint(R.string.hintPasteFromBrowser)
        }
        input.setText(defaultValue)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(R.string.ok, {_, _ ->
                val name = input.text.toString().trim()
                if (isUrl) {
                    if (UrlValidator().isValid(name)) {
                        callback(name)
                    }
                    else {
                        Toast.makeText(this, R.string.invalidUrl, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                else {
                    if (validateName(this, name)) {
                        callback(name)
                    }
                }
            })
            .setNegativeButton(R.string.cancel, {dialog: DialogInterface, _ -> dialog.cancel()})
            .show()
    }

    /**
     * Checks that the given file/folder name is valid and not already in use, and shows the appropriate error message otherwise.
     *
     * @param name  The name to validate without the path. The file/folder is assumed to be inside the current folder.
     *
     * @return True if the name is valid, false otherwise.
     */
    private fun validateName(activity: FileExplorerActivity, name: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(activity, R.string.emptyNotAllowed, Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.contains("/")) {
            Toast.makeText(activity, R.string.slashNotAllowed, Toast.LENGTH_SHORT).show()
            return false
        }
        if (File(activity.currentFolder().absolutePath() + "/" + name).exists()) {
            Toast.makeText(
                activity,
                String.format(activity.getString(R.string.alreadyExists), name),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }
}