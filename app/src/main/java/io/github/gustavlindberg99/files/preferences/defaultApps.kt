package io.github.gustavlindberg99.files.preferences

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.activity.App
import java.io.File

/**
 * Gets the default browser.
 *
 * @return The package name of the default browser, or null if there is no default browser.
 */
public fun defaultBrowser(): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
    val result = App.context.packageManager
        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
    if (result == "android") {
        return null
    }
    return result
}

/**
 * Gets an intent to open the specified file.
 *
 * @param file      The file to open.
 * @param editable  True if the app opening the file should be allowed to edit it, false otherwise.
 *
 * @return The intent to open the file.
 */
public fun openFileIntent(file: File, editable: Boolean = true): Intent {
    val uri = FileProvider.getUriForFile(
        App.context,
        BuildConfig.APPLICATION_ID + ".provider",
        file
    )
    val mime = App.context.contentResolver.getType(uri)

    val intent = Intent()
    intent.setAction(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, mime)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (editable) {
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    return intent
}