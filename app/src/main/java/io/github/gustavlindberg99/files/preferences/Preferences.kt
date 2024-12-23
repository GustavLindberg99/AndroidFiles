package io.github.gustavlindberg99.files.preferences

import androidx.appcompat.app.AppCompatActivity
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.filesystem.Folder
import io.github.gustavlindberg99.files.filesystem.ThisPhoneFolder

private const val PREFERENCES = "preferences"
private const val SHOW_HIDDEN_FILES = "showHiddenFiles"
private const val SHOW_FILE_EXTENSIONS = "showFileExtensions"
private const val PINNED_FOLDERS = "pinnedFolders"

/**
 * A wrapper class for SharedPreferences allowing to access each preference directly.
 */
object Preferences {
    public var showHiddenFiles: Boolean
        get() = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getBoolean(SHOW_HIDDEN_FILES, false)
        set(value) = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_HIDDEN_FILES, value)
            .apply()

    public var showFileExtensions: Boolean
        get() = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getBoolean(SHOW_FILE_EXTENSIONS, false)
        set(value) = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_FILE_EXTENSIONS, value)
            .apply()

    public var pinnedFolders: List<Folder>
        get() = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .getStringSet(PINNED_FOLDERS, null)
            ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, {it}))
            ?.mapNotNull {Folder.fromPath(it)} ?: listOf(ThisPhoneFolder)
        set(value) = App.context
            .getSharedPreferences(PREFERENCES, AppCompatActivity.MODE_PRIVATE)
            .edit()
            .putStringSet(PINNED_FOLDERS, value.map {it.absolutePath()}.toSet())
            .apply()
}