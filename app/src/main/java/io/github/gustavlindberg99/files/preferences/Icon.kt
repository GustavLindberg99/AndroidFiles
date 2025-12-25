package io.github.gustavlindberg99.files.preferences

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.filesystem.Directory

private const val WINDOWS_PATH = "windowsPath"
private const val WORKING_DIRECTORY = "workingDirectory"

private val windowsIcons = mapOf(
    Icon.shell32Icon(0) to R.drawable.file,
    Icon.shell32Icon(1) to R.drawable.richtextfile,
    Icon.shell32Icon(3) to R.drawable.folder,
    Icon.shell32Icon(4) to R.drawable.folder,
    Icon.shell32Icon(7) to R.drawable.drive,
    Icon.shell32Icon(8) to R.drawable.drive,
    Icon.shell32Icon(11) to R.drawable.cddrive,
    Icon.shell32Icon(28) to R.drawable.shortcut,
    Icon.shell32Icon(29) to R.drawable.shortcut,
    Icon.shell32Icon(69) to R.drawable.settingsfile,
    Icon.shell32Icon(70) to R.drawable.textfile,
    Icon.shell32Icon(71) to R.drawable.settingsfile,
    Icon.shell32Icon(72) to R.drawable.settingsfile,
    Icon.shell32Icon(79) to R.drawable.drive,
    Icon.shell32Icon(86) to R.drawable.folder,
    Icon.shell32Icon(113) to R.drawable.cd,
    Icon.shell32Icon(116) to R.drawable.musicfile,
    Icon.shell32Icon(118) to R.drawable.mediafile,
    Icon.shell32Icon(124) to R.drawable.drive,
    Icon.shell32Icon(126) to R.drawable.documentsfolder,
    Icon.shell32Icon(127) to R.drawable.imagesfolder,
    Icon.shell32Icon(128) to R.drawable.musicfolder,
    Icon.shell32Icon(129) to R.drawable.videosfolder,
    Icon.shell32Icon(155) to R.drawable.folder,
    Icon.shell32Icon(180) to R.drawable.cd,
    Icon.shell32Icon(181) to R.drawable.cd,
    Icon.shell32Icon(182) to R.drawable.cd,
    Icon.shell32Icon(183) to R.drawable.cd,
    Icon.shell32Icon(184) to R.drawable.cd,
    Icon.shell32Icon(188) to R.drawable.cd,
    Icon.shell32Icon(190) to R.drawable.cd,
    Icon.shell32Icon(191) to R.drawable.drive,
    Icon.shell32Icon(196) to R.drawable.phone,
    Icon.shell32Icon(204) to R.drawable.cd,
    Icon.shell32Icon(205) to R.drawable.folder,
    Icon.shell32Icon(266) to R.drawable.folder,
    Icon.shell32Icon(310) to R.drawable.textfile,
    Icon.shell32Icon(311) to R.drawable.imagefile,
    Icon.shell32Icon(312) to R.drawable.musicfile,
    Icon.shell32Icon(313) to R.drawable.videofile,
    Icon.shell32Icon(314) to R.drawable.settings,
    Icon.imageresIcon(2) to R.drawable.file,
    Icon.imageresIcon(3) to R.drawable.folder,
    Icon.imageresIcon(4) to R.drawable.folder,
    Icon.imageresIcon(5) to R.drawable.folder,
    Icon.imageresIcon(8) to R.drawable.grayfolder,
    Icon.imageresIcon(9) to R.drawable.greenfolder,
    Icon.imageresIcon(13) to R.drawable.folder,
    Icon.imageresIcon(25) to R.drawable.cddrive,
    Icon.imageresIcon(27) to R.drawable.drive,
    Icon.imageresIcon(30) to R.drawable.drive,
    Icon.imageresIcon(31) to R.drawable.systemdrive,
    Icon.imageresIcon(33) to R.drawable.cd,
    Icon.imageresIcon(34) to R.drawable.cd,
    Icon.imageresIcon(35) to R.drawable.cd,
    Icon.imageresIcon(36) to R.drawable.cd,
    Icon.imageresIcon(42) to R.drawable.phone,
    Icon.imageresIcon(51) to R.drawable.cd,
    Icon.imageresIcon(56) to R.drawable.cd,
    Icon.imageresIcon(57) to R.drawable.cd,
    Icon.imageresIcon(58) to R.drawable.cd,
    Icon.imageresIcon(59) to R.drawable.cd,
    Icon.imageresIcon(62) to R.drawable.settingsfile,
    Icon.imageresIcon(64) to R.drawable.settingsfile,
    Icon.imageresIcon(85) to R.drawable.richtextfile,
    Icon.imageresIcon(97) to R.drawable.textfile,
    Icon.imageresIcon(103) to R.drawable.musicfolder,
    Icon.imageresIcon(107) to R.drawable.documentsfolder,
    Icon.imageresIcon(108) to R.drawable.imagesfolder,
    Icon.imageresIcon(110) to R.drawable.folder,
    Icon.imageresIcon(117) to R.drawable.folder,
    Icon.imageresIcon(125) to R.drawable.musicfile,
    Icon.imageresIcon(126) to R.drawable.imagefile,
    Icon.imageresIcon(127) to R.drawable.videofile,
    Icon.imageresIcon(128) to R.drawable.mediafile,
    Icon.imageresIcon(153) to R.drawable.folder,
    Icon.imageresIcon(154) to R.drawable.shortcut,
    Icon.imageresIcon(155) to R.drawable.shortcut,
    Icon.imageresIcon(163) to R.drawable.cd,
    Icon.imageresIcon(164) to R.drawable.cd,
    Icon.imageresIcon(165) to R.drawable.archive,
    Icon.imageresIcon(172) to R.drawable.folder,
    Icon.imageresIcon(175) to R.drawable.downloadsfolder,
    Icon.imageresIcon(176) to R.drawable.folder,
    Icon.imageresIcon(177) to R.drawable.folder,
    Icon.imageresIcon(178) to R.drawable.videosfolder,
    Icon.imageresIcon(179) to R.drawable.greenfolder,
    Icon.imageresIcon(200) to R.drawable.folder,
    Icon.msiexecIcon(0) to R.drawable.installer,
    Icon.msiIcon(2) to R.drawable.installer,
    Icon.msedgeIcon(13) to R.drawable.pdffile
)

private val cachedIcons = mutableMapOf<String, Drawable>()


/**
 * A class representing an icon.
 *
 * @param windowsPath       The Windows path of the icon. Examples:
 *  - `C:\Windows\System32\shell32.dll,314`: A system icon, uses the specified DLL on Windows and a predefined icon on Android. See the `windowsIcons` map for which icons are supported on Android.
 *  - `D:\Pictures\Icons\icon.ico,0`: An absolute path to an icon on this phone. The drive letter is the drive letter used by AndroidDrive, and this app assumes it refers to the current phone (otherwise it's not possible to access it from Android anyway).
 *  - `Icons\icon.ico,0`: A relative path to an icon on this phone. In this case it's the same as the previous example if the path is relative to `/sdcard/Pictures`.
 *  - `android:com.example.app,0`: The icon of the app with the given ID. Only usable from Android, not from Windows.
 *  - `android:com.example.app,1`: A file icon with the icon of the given app in the top left corner. Only usable from Android, not from Windows.
 *
 * @param _workingDirectory The directory to use as working directory when resolving relative paths.
 */
class Icon(public val windowsPath: String, private val _workingDirectory: Directory? = null) {
    public val drawable: Drawable? by lazy {
        cachedIcons[this.windowsPath]
            ?: this.iconFromDll()
            ?: this.iconFromApp()
            ?: this.iconFromFile()
    }

    override fun equals(other: Any?): Boolean {
        return other is Icon && this.windowsPath == other.windowsPath
    }

    override fun hashCode(): Int {
        return this.windowsPath.hashCode()
    }

    companion object {
        fun Intent.getIconExtra(name: String): Icon? {
            val bundle = this.getBundleExtra(name) ?: return null
            val windowsPath = bundle.getString(WINDOWS_PATH) ?: return null
            val workingDirectory = bundle.getString(WORKING_DIRECTORY)
            if (workingDirectory == null) {
                return Icon(windowsPath)
            }
            else {
                return Icon(windowsPath, Directory.fromPath(workingDirectory))
            }
        }

        fun Intent.putExtra(name: String, value: Icon): Intent {
            val bundle = Bundle()
            bundle.putString(WINDOWS_PATH, value.windowsPath)
            bundle.putString(WORKING_DIRECTORY, value._workingDirectory?.absolutePath())
            this.putExtra(name, bundle)
            return this
        }

        fun SharedPreferences.getIcon(key: String): Icon? {
            //Use `key` instead of `$key/$WINDOWS_PATH` for backwards compatibility
            val windowsPath = this.getString(key, null) ?: return null
            val workingDirectory = this.getString("$key/$WORKING_DIRECTORY", null)
            if (workingDirectory == null) {
                return Icon(windowsPath)
            }
            else {
                return Icon(windowsPath, Directory.fromPath(workingDirectory))
            }
        }

        fun SharedPreferences.getIcon(key: String, defValue: Icon): Icon {
            return this.getIcon(key) ?: defValue
        }

        fun SharedPreferences.Editor.putIcon(key: String, value: Icon): SharedPreferences.Editor {
            this.putString(key, value.windowsPath)
            this.putString("$key/$WORKING_DIRECTORY", value._workingDirectory?.absolutePath())
            return this
        }

        /**
         * Gets the full path of an icon in shell32.dll at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun shell32Icon(iconIndex: Int) =
            Icon("C:\\Windows\\System32\\shell32.dll,$iconIndex")

        /**
         * Gets the full path of an icon in imageres.dll at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun imageresIcon(iconIndex: Int) =
            Icon("C:\\Windows\\System32\\imageres.dll,$iconIndex")

        /**
         * Gets the full path of an icon in msiexec.exe at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun msiexecIcon(iconIndex: Int) =
            Icon("C:\\Windows\\System32\\msiexec.exe,$iconIndex")

        /**
         * Gets icon in msi.dll at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun msiIcon(iconIndex: Int) = Icon("C:\\Windows\\System32\\msi.dll,$iconIndex")

        /**
         * Gets icon in url.dll at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun urlIcon(iconIndex: Int) = Icon("C:\\Windows\\System32\\url.dll,$iconIndex")

        /**
         * Gets the full path of an icon in msedge.exe at a specific index.
         *
         * @param iconIndex The index of the icon.
         *
         * @return The icon.
         */
        public fun msedgeIcon(iconIndex: Int) =
            Icon("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe,$iconIndex")

        /**
         * Gets all the icons with drawables built in to this app.
         *
         * @return A list of the built-in icons.
         */
        public fun builtInIcons(): List<Icon> {
            val resourceIds = windowsIcons.values.distinct()
            val result = resourceIds
                .map { resourceId -> windowsIcons.filterValues { it == resourceId }.keys.first() }
                .toMutableList()
            result.add(Icon.msedgeIcon(0))
            result.add(Icon.urlIcon(0))
            return result
        }

        /**
         * Gets all the icons for a specific app.
         *
         * @param app   The app to get the icons for.
         *
         * @return A list with two elements, one with the plain app icon and one with the app icon in a file icon.
         */
        public fun appIcons(app: AppInfo): List<Icon> {
            return listOf(
                "android:${app.packageName},0",
                "android:${app.packageName},1"
            ).map { Icon(it) }
        }

        /**
         * Gets all the icons for all installed apps.
         *
         * @return A list of all app icons.
         */
        public fun allAppIcons(): List<Icon> {
            return AppInfo.allApps().flatMap { Icon.appIcons(it) }
        }
    }

    /**
     * Tries to parse the path of this icon as a path to a Windows system DLL (shell32.dll or imageres.dll), and if it succeeds returns the icon from the resources corresponding to that icon.
     *
     * @return The drawable if the parsing succeeded and the icon is supported by this app, null otherwise.
     */
    private fun iconFromDll(): Drawable? {
        val dllRegex = Regex(
            """^(?:(?:C:[/\\]Windows|%SystemRoot%)[/\\]System32[/\\])?([^/\\]+\.(?:dll|exe),[0-9]+)$""",
            RegexOption.IGNORE_CASE
        )
        val match = dllRegex.matchEntire(this.windowsPath)?.groupValues?.get(1)?.lowercase()
        val dllFile =
            if (match == null) this else Icon("C:\\Windows\\System32\\$match")
        if (dllFile in listOf(
                //Edge
                msedgeIcon(0),
                //url.dll
                urlIcon(0),
                //Internet Explorer
                shell32Icon(220),
                shell32Icon(242)
            )
        ) {
            val defaultBrowser = defaultBrowser()
            if (defaultBrowser != null) {
                val iconIndex = if (dllFile == urlIcon(0)) "1" else "0"
                return Icon(
                    "android:$defaultBrowser,$iconIndex",
                    this._workingDirectory
                ).iconFromApp()
            }
        }
        val resourceId: Int = windowsIcons[dllFile] ?: return null
        val result = AppCompatResources.getDrawable(App.context, resourceId)
        if (result != null) {
            cachedIcons[this.windowsPath] = result
        }
        return result
    }

    /**
     * Tries to parse the path of this icon as a package name of an app, and if it succeeds returns the corresponding icon from that app.
     *
     * @return The drawable if the parsing succeeded, null otherwise.
     */
    private fun iconFromApp(): Drawable? {
        val appRegex = Regex(
            """^android:([a-z0-9._]+),([01])$""",
            RegexOption.IGNORE_CASE
        )
        val appMatch: List<String> =
            appRegex.matchEntire(this.windowsPath)?.groupValues ?: return null
        val packageName = appMatch[1]
        val appIcon: Drawable = AppInfo(packageName).drawable ?: return null
        val iconIndex = appMatch[2]
        val result = if (iconIndex == "1") {
            val bitmap = Bitmap.createBitmap(
                appIcon.intrinsicWidth,
                appIcon.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            appIcon.setBounds(0, canvas.height / 6, canvas.width / 2, canvas.height * 2 / 3)
            appIcon.draw(canvas)
            val fileIcon = AppCompatResources.getDrawable(App.context, R.drawable.file)!!
            LayerDrawable(arrayOf(fileIcon, BitmapDrawable(App.context.resources, bitmap)))
        }
        else {
            appIcon
        }
        cachedIcons[this.windowsPath] = result
        return result
    }

    /**
     * Gets the drawable from an icon file local on the Android device. If the path is an absolute Windows path, assumes that the drive letter refers to the current drive, otherwise resolves the relative path.
     *
     * @return The drawable if the file exists, null otherwise.
     */
    private fun iconFromFile(): Drawable? {
        val absolutePathRegex = Regex(
            """^[A-Z]:[/\\](.*)$""",
            RegexOption.IGNORE_CASE
        )
        val pathRelativeToDrive: String? =
            absolutePathRegex.matchEntire(this.windowsPath)?.groupValues?.get(1)

        val absoluteAndroidPathWithIndex: String =
            //It's a relative path (relative to the folder, not to the drive) or an Android path
            if (pathRelativeToDrive == null)
                if (this.windowsPath.startsWith("/")) this.windowsPath
                else this._workingDirectory?.resolveRelativePath(this.windowsPath) ?: return null
            //It's an absolute Windows path
            else
                this._workingDirectory?.drive()?.resolveRelativePath(pathRelativeToDrive)
                    ?: return null

        val absolutePathWithoutIndex =
            absoluteAndroidPathWithIndex.replace(Regex(""",[0-9]+$"""), "")
        return Drawable.createFromPath(absolutePathWithoutIndex)
    }
}