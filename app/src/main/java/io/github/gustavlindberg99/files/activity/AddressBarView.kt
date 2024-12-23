package io.github.gustavlindberg99.files.activity

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Folder

/**
 * A view containing the address bar.
 *
 * @param context       The activity that this file view will be placed in.
 * @param attrSet       Not used by this class, passed directly to the FrameLayout constructor.
 * @param defStyleAttr  Not used by this class, passed directly to the FrameLayout constructor.
 */
class AddressBarView(context: Context, attrSet: AttributeSet?, defStyleAttr: Int):
    FrameLayout(context, attrSet, defStyleAttr) {
    private val _buttonBar: LinearLayout by lazy {this.findViewById(R.id.AddressBarView_buttonBar)}
    private val _collapsedParents: TextView by lazy {this.findViewById(R.id.AddressBarView_collapsedParents)}
    private val _parentFolderButton: Button by lazy {this.findViewById(R.id.AddressBarView_parentFolderButton)}
    private val _slash: TextView by lazy {this.findViewById(R.id.AddressBarView_slash)}
    private val _currentFolderButton: Button by lazy {this.findViewById(R.id.AddressBarView_currentFolderButton)}
    private val _editPath: EditText by lazy {this.findViewById(R.id.AddressBarView_editPath)}

    private val _navButton: ImageButton by lazy {this.findViewById(R.id.AddressBarView_navButton)}
    private val _menuButton: ImageButton by lazy {this.findViewById(R.id.AddressBarView_menuButton)}
    private val _settingsButton: ImageButton by lazy {this.findViewById(R.id.AddressBarView_settingsButton)}

    public constructor(context: Context): this(context, null, 0)
    public constructor(context: Context, attrSet: AttributeSet?): this(context, attrSet, 0)

    init {
        View.inflate(context, R.layout.view_address_bar, this)

        this._currentFolderButton.setOnClickListener {
            this.editing = true
        }

        this._menuButton.setOnClickListener {
            this._menuButton.showContextMenu(this._menuButton.x, this._menuButton.y)
        }

        this._parentFolderButton.setOnDragListener {_, event ->
            if (event.action == DragEvent.ACTION_DROP) {
                this._parentFolderButton.showContextMenu(event.x, event.y)
            }
            return@setOnDragListener true
        }

        if (context is Activity) {
            context.registerForContextMenu(this._menuButton)
            context.registerForContextMenu(this._parentFolderButton)
        }
    }

    /**
     * True if the text input is currently visible, false if the buttons are currently visible.
     */
    public var editing: Boolean
        get() = this._editPath.visibility == View.VISIBLE
        set(value) {
            if (value) {
                this._buttonBar.visibility = View.GONE
                this._editPath.visibility = View.VISIBLE
                this._editPath.requestFocus()
            }
            else {
                this._buttonBar.visibility = View.VISIBLE
                this._editPath.visibility = View.GONE
            }
        }

    /**
     * Sets the folder currently selected in the address bar.
     *
     * @param folder    The folder to set.
     */
    public fun setSelectedFolder(folder: Folder) {
        this._editPath.setText(folder.absolutePath())
        this._currentFolderButton.text = folder.name()

        val parentFolder = folder.parentFolder()
        if (parentFolder == null) {
            this._collapsedParents.visibility = View.GONE
            this._parentFolderButton.visibility = View.GONE
            this._slash.visibility = View.GONE
        }
        else {
            this._collapsedParents.visibility =
                if (parentFolder.parentFolder() == null) View.GONE else View.VISIBLE
            this._parentFolderButton.visibility = View.VISIBLE
            this._parentFolderButton.text = parentFolder.name()
            this._slash.visibility = View.VISIBLE
        }
    }

    /**
     * Register a callback to be invoked when the selected folder is changed.
     *
     * @param callback  The callback that will be run. The parameter that will be passed is selected folder, or null if the selected folder doesn't exist or is not a folder.
     */
    public fun setOnSelectedFolderChangedListener(callback: (Folder?) -> Unit) {
        this._editPath.setOnKeyListener {_, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val folder = Folder.fromPath(this._editPath.text.toString().trim())
                callback(folder)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    /**
     * Register a callback to be invoked when the parent folder button is clicked.
     *
     * @param callback  The callback that will run.
     */
    public fun setOnParentFolderButtonClickListener(callback: (View) -> Unit) {
        this._parentFolderButton.setOnClickListener(callback)
    }

    /**
     * Register a callback to be invoked when the nav button is clicked.
     *
     * @param callback  The callback that will run.
     */
    public fun setOnNavButtonClickListener(callback: (View) -> Unit) {
        this._navButton.setOnClickListener(callback)
    }

    /**
     * Register a callback to be invoked when the settings button is clicked.
     *
     * @param callback  The callback that will run.
     */
    public fun setOnSettingsButtonClickListener(callback: (View) -> Unit) {
        this._settingsButton.setOnClickListener(callback)
    }

    /**
     * Checks if a view is the parent folder button.
     *
     * @param view  The view to check.
     *
     * @return True if it's the parent folder button, false if it isn't.
     */
    public fun viewIsParentFolderButton(view: View): Boolean {
        return view == this._parentFolderButton
    }
}