package io.github.gustavlindberg99.files.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.text.HtmlCompat
import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.preferences.Preferences
import io.github.gustavlindberg99.files.R

/**
 * The settings activity.
 */
class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val showHiddenFilesSwitch: SwitchCompat =
            this.findViewById(R.id.SettingsActivity_showHiddenFiles)
        showHiddenFilesSwitch.isChecked = Preferences.showHiddenFiles
        showHiddenFilesSwitch.setOnCheckedChangeListener {_, isChecked: Boolean ->
            Preferences.showHiddenFiles = isChecked
        }

        val showFileExtensionsSwitch: SwitchCompat =
            this.findViewById(R.id.SettingsActivity_showFileExtensions)
        showFileExtensionsSwitch.isChecked = Preferences.showFileExtensions
        showFileExtensionsSwitch.setOnCheckedChangeListener {_, isChecked: Boolean ->
            Preferences.showFileExtensions = isChecked
        }

        this.findViewById<Button>(R.id.SettingsActivity_fileTypesButton).setOnClickListener {
            val intent = Intent(this, FileTypeManagerActivity::class.java)
            this.startActivity(intent)
        }

        this.findViewById<Button>(R.id.SettingsActivity_helpButton).setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/GustavLindberg99/AndroidFiles/blob/master/README.md")
            )
            startActivity(browserIntent)
        }

        this.findViewById<Button>(R.id.SettingsActivity_feedbackButton).setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/GustavLindberg99/AndroidFiles/issues")
            )
            startActivity(browserIntent)
        }

        this.findViewById<Button>(R.id.SettingsActivity_aboutButton).setOnClickListener {
            val textView = TextView(this)
            textView.text = HtmlCompat.fromHtml(
                String.format(
                    this.getString(R.string.aboutString),
                    BuildConfig.VERSION_NAME,
                    "https://github.com/GustavLindberg99/AndroidFiles",
                    "https://github.com/GustavLindberg99/AndroidFiles/blob/master/LICENSE"
                ), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            textView.setTextColor(this.getColor(R.color.textColor))
            textView.setLinkTextColor(Color.BLUE)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            val value = TypedValue()
            if (this.theme.resolveAttribute(R.attr.dialogPreferredPadding, value, true)) {
                val padding = TypedValue.complexToDimensionPixelSize(
                    value.data,
                    this.resources.displayMetrics
                )
                textView.setPadding(padding, dpToPx(8.0), padding, 0)
            }
            textView.movementMethod = LinkMovementMethod.getInstance()
            AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setView(textView)
                .setPositiveButton(R.string.ok, {_: DialogInterface?, _: Int ->})
                .create()
                .show()
        }
    }

    private fun dpToPx(dp: Double): Int {
        return (dp * Resources.getSystem().displayMetrics.density + 0.5).toInt()
    }
}