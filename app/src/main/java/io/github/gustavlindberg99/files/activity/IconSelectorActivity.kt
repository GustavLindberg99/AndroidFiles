package io.github.gustavlindberg99.files.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.filesystem.Directory
import io.github.gustavlindberg99.files.preferences.Icon
import io.github.gustavlindberg99.files.preferences.Icon.Companion.getIconExtra
import io.github.gustavlindberg99.files.preferences.Icon.Companion.putExtra
import java.nio.file.Paths

/**
 * An activity to select an icon.
 */
class IconSelectorActivity : AppCompatActivity() {
    companion object {
        public const val RELATIVE_PATH_ROOT = "relativePathRoot"
        private const val SELECTED_ICON = "selectedIcon"

        /**
         * Creates a launcher that can launch this activity and get the selected icon as result.
         *
         * @param activity  The activity from which this activity should be launched.
         * @param callback  A callback that will be called when this activity finishes if an icon was selected, in which case the Windows path of that icon is passed as parameter. If the an icon from a file was selected and the RELATIVE_PATH_ROOT extra was passed to this activity, the relative path of that file with respect to RELATIVE_PATH_ROOT will be returned, otherwise the absolute path of the file will be returned. If no icon was selected, the callback won't be called.
         *
         * @return The activity result launcher.
         */
        public fun createResultLauncher(
            activity: ComponentActivity,
            callback: (Icon) -> Unit
        ): ActivityResultLauncher<Intent> = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), {
                val icon = it.data?.getIconExtra(SELECTED_ICON)
                if (icon != null) {
                    callback(icon)
                }
            }
        )
    }

    private val _iconList: FlexboxLayout by lazy { this.findViewById(R.id.IconSelectorActivity_iconList) }
    private val _relativePathRoot: String? by lazy { this.intent.getStringExtra(RELATIVE_PATH_ROOT) }
    private lateinit var _fileSelectorLauncher: ActivityResultLauncher<Intent>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_icon_selector)
        this.supportActionBar!!.elevation = 0.0f

        this._fileSelectorLauncher = OpenActivity.createResultLauncher(this, { file ->
            val relativePathRoot = this._relativePathRoot
            val path =
                if (relativePathRoot == null) file.absolutePath()
                else Paths.get(relativePathRoot).relativize(file.path()).toString()
            val workingDirectory =
                if (relativePathRoot == null) null
                else Directory.fromPath(relativePathRoot)
            displayIcons(listOf(Icon("$path,0", workingDirectory)))
        })
        val iconGroupSelector: RadioGroup =
            this.findViewById(R.id.IconSelectorActivity_iconGroupSelector)
        iconGroupSelector.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.IconSelectorActivity_builtInIcon -> displayIcons(Icon.builtInIcons())
                R.id.IconSelectorActivity_iconFromApp -> displayIcons(Icon.allAppIcons())
                R.id.IconSelectorActivity_iconFromFile -> this._iconList.removeAllViews()    //Only remove all views here, the rest is handled in the onClickListener (which should be run any time the radio button is clicked, even if it's already selected)
            }
        }
        this.findViewById<RadioButton>(R.id.IconSelectorActivity_iconFromFile).setOnClickListener {
            this.browseForIconFromFile()
        }

        displayIcons(Icon.builtInIcons())
    }

    /**
     * Displays a set of icons in the icons list.
     *
     * @param icons The Windows paths of the icons to display.
     */
    private fun displayIcons(icons: List<Icon>) {
        this._iconList.removeAllViews()
        for (icon in icons) {
            val view = ImageView(this)
            view.layoutParams = FlexboxLayout.LayoutParams(128, 128)
            view.setPadding(8, 8, 8, 8)
            val drawable = icon.drawable ?: continue
            view.setImageDrawable(drawable)
            view.setOnClickListener {
                this.selectIcon(icon)
            }
            this._iconList.addView(view)
        }
        if (this._iconList.childCount == 0) {
            Toast.makeText(this, R.string.fileContainsNoIcons, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exits the activity returning an intent with the selected icon.
     *
     * @param icon  The selected icon.
     */
    private fun selectIcon(icon: Icon) {
        val intent = Intent()
        intent.putExtra(SELECTED_ICON, icon)
        this.setResult(RESULT_OK, intent)
        this.finish()
    }

    /**
     * Opens an activity to browse for an icon from a file.
     */
    private fun browseForIconFromFile() {
        val intent = Intent(this, OpenActivity::class.java)
        val appFormats = Bundle()
        appFormats.putString(
            OpenActivity.FILE_TYPE_DESCRIPTION,
            this.getString(R.string.appIconFormats)
        )
        appFormats.putStringArray(
            OpenActivity.FILE_TYPE_EXTENSIONS,
            arrayOf("ico", "png", "jpg", "jpeg", "gif", "bmp", "heic", "hif")
        )
        val windowsFormats = Bundle()
        windowsFormats.putString(
            OpenActivity.FILE_TYPE_DESCRIPTION,
            this.getString(R.string.windowsIconFormats)
        )
        windowsFormats.putStringArray(OpenActivity.FILE_TYPE_EXTENSIONS, arrayOf("ico"))
        intent.putExtra(OpenActivity.FILE_TYPES, arrayOf(appFormats, windowsFormats))
        this._fileSelectorLauncher.launch(intent)
    }
}