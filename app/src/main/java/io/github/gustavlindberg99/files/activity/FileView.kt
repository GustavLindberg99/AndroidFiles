package io.github.gustavlindberg99.files.activity

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Drive
import io.github.gustavlindberg99.files.filesystem.FileOrFolder
import io.github.gustavlindberg99.files.filesystem.GeneralFile
import io.github.gustavlindberg99.files.filesystem.fileSizeToString

/**
 * A view representing a file or folder. The file must be set after creating the view object.
 *
 * @param context       The activity that this file view will be placed in. Should be an activity, but this isn't specified in the signature because views must have a constructor taking a context.
 * @param attrSet       Not used by this class, passed directly to the FrameLayout constructor.
 * @param defStyleAttr  Not used by this class, passed directly to the FrameLayout constructor.
 */
class FileView(context: Context, attrSet: AttributeSet?, defStyleAttr: Int):
    FrameLayout(context, attrSet, defStyleAttr) {
    private lateinit var _file: FileOrFolder

    private val _selectedMarker: View by lazy {this.findViewById(R.id.FileView_selectedMarker)}
    private val _iconView: ImageView by lazy {this.findViewById(R.id.FileView_fileIcon)}
    private val _nameView: TextView by lazy {this.findViewById(R.id.FileView_fileName)}
    private val _typeView: TextView by lazy {this.findViewById(R.id.FileView_fileType)}

    public constructor(context: Context): this(context, null, 0)
    public constructor(context: Context, attrSet: AttributeSet?): this(context, attrSet, 0)

    init {
        View.inflate(context, R.layout.view_file, this)

        if (context is Activity) {
            context.registerForContextMenu(this)
        }
    }

    /**
     * True if the file is selected, false otherwise.
     */
    public var fileSelected: Boolean
        get() = this._selectedMarker.visibility == View.VISIBLE
        set(value) {
            if (value) {
                this._selectedMarker.visibility = View.VISIBLE
            }
            else {
                this._selectedMarker.visibility = View.GONE
            }
        }

    /**
     * The file or folder that this view should represent. Setting it will redraw the necessary parts of the view.
     */
    public var file: FileOrFolder
        get() = this._file
        set(file) {
            this._file = file

            val icon: Drawable = file.icon()
            icon.alpha = if (file.hidden()) 128 else 255
            this._iconView.setImageDrawable(icon)

            this._nameView.text = file.baseName()

            this._typeView.text = when (file) {
                is Drive -> String.format(
                    this.context.getString(R.string.freeDiskSpace),
                    fileSizeToString(file.freeDiskSpace()),
                    fileSizeToString(file.totalDiskSpace())
                )

                is GeneralFile -> String.format(
                    "%s, %s",
                    file.typeName(),
                    fileSizeToString(file.size())
                )

                else -> file.typeName()
            }
        }
}