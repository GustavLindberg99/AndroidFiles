package io.github.gustavlindberg99.files.preferences

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.filesystem.Directory

/**
 * Gets the full path of an icon in shell32.dll at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun shell32Icon(iconIndex: Int) = "C:\\Windows\\System32\\shell32.dll,$iconIndex"

/**
 * Gets the full path of an icon in imageres.dll at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun imageresIcon(iconIndex: Int) = "C:\\Windows\\System32\\imageres.dll,$iconIndex"

/**
 * Gets the full path of an icon in msiexec.exe at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun msiexecIcon(iconIndex: Int) = "C:\\Windows\\System32\\msiexec.exe,$iconIndex"

/**
 * Gets icon in msi.dll at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun msiIcon(iconIndex: Int) = "C:\\Windows\\System32\\msi.dll,$iconIndex"

/**
 * Gets icon in url.dll at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun urlIcon(iconIndex: Int) = "C:\\Windows\\System32\\url.dll,$iconIndex"

/**
 * Gets the full path of an icon in msedge.exe at a specific index.
 *
 * @param iconIndex The index of the icon.
 *
 * @return The Windows path of the icon.
 */
public fun msedgeIcon(iconIndex: Int) =
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe,$iconIndex"

private val windowsIcons = mapOf(
    shell32Icon(0) to R.drawable.file,
    shell32Icon(1) to R.drawable.richtextfile,
    shell32Icon(3) to R.drawable.folder,
    shell32Icon(4) to R.drawable.folder,
    shell32Icon(7) to R.drawable.drive,
    shell32Icon(8) to R.drawable.drive,
    shell32Icon(11) to R.drawable.cddrive,
    shell32Icon(28) to R.drawable.shortcut,
    shell32Icon(29) to R.drawable.shortcut,
    shell32Icon(69) to R.drawable.settingsfile,
    shell32Icon(70) to R.drawable.textfile,
    shell32Icon(71) to R.drawable.settingsfile,
    shell32Icon(72) to R.drawable.settingsfile,
    shell32Icon(79) to R.drawable.drive,
    shell32Icon(86) to R.drawable.folder,
    shell32Icon(113) to R.drawable.cd,
    shell32Icon(116) to R.drawable.musicfile,
    shell32Icon(118) to R.drawable.mediafile,
    shell32Icon(124) to R.drawable.drive,
    shell32Icon(126) to R.drawable.documentsfolder,
    shell32Icon(127) to R.drawable.imagesfolder,
    shell32Icon(128) to R.drawable.musicfolder,
    shell32Icon(129) to R.drawable.videosfolder,
    shell32Icon(155) to R.drawable.folder,
    shell32Icon(180) to R.drawable.cd,
    shell32Icon(181) to R.drawable.cd,
    shell32Icon(182) to R.drawable.cd,
    shell32Icon(183) to R.drawable.cd,
    shell32Icon(184) to R.drawable.cd,
    shell32Icon(188) to R.drawable.cd,
    shell32Icon(190) to R.drawable.cd,
    shell32Icon(191) to R.drawable.drive,
    shell32Icon(196) to R.drawable.phone,
    shell32Icon(204) to R.drawable.cd,
    shell32Icon(205) to R.drawable.folder,
    shell32Icon(266) to R.drawable.folder,
    shell32Icon(310) to R.drawable.textfile,
    shell32Icon(311) to R.drawable.imagefile,
    shell32Icon(312) to R.drawable.musicfile,
    shell32Icon(313) to R.drawable.videofile,
    shell32Icon(314) to R.drawable.settings,
    imageresIcon(2) to R.drawable.file,
    imageresIcon(3) to R.drawable.folder,
    imageresIcon(4) to R.drawable.folder,
    imageresIcon(5) to R.drawable.folder,
    imageresIcon(8) to R.drawable.grayfolder,
    imageresIcon(9) to R.drawable.greenfolder,
    imageresIcon(13) to R.drawable.folder,
    imageresIcon(25) to R.drawable.cddrive,
    imageresIcon(27) to R.drawable.drive,
    imageresIcon(30) to R.drawable.drive,
    imageresIcon(31) to R.drawable.systemdrive,
    imageresIcon(33) to R.drawable.cd,
    imageresIcon(34) to R.drawable.cd,
    imageresIcon(35) to R.drawable.cd,
    imageresIcon(36) to R.drawable.cd,
    imageresIcon(42) to R.drawable.phone,
    imageresIcon(51) to R.drawable.cd,
    imageresIcon(56) to R.drawable.cd,
    imageresIcon(57) to R.drawable.cd,
    imageresIcon(58) to R.drawable.cd,
    imageresIcon(59) to R.drawable.cd,
    imageresIcon(62) to R.drawable.settingsfile,
    imageresIcon(64) to R.drawable.settingsfile,
    imageresIcon(85) to R.drawable.richtextfile,
    imageresIcon(97) to R.drawable.textfile,
    imageresIcon(103) to R.drawable.musicfolder,
    imageresIcon(107) to R.drawable.documentsfolder,
    imageresIcon(108) to R.drawable.imagesfolder,
    imageresIcon(110) to R.drawable.folder,
    imageresIcon(117) to R.drawable.folder,
    imageresIcon(125) to R.drawable.musicfile,
    imageresIcon(126) to R.drawable.imagefile,
    imageresIcon(127) to R.drawable.videofile,
    imageresIcon(128) to R.drawable.mediafile,
    imageresIcon(153) to R.drawable.folder,
    imageresIcon(154) to R.drawable.shortcut,
    imageresIcon(155) to R.drawable.shortcut,
    imageresIcon(163) to R.drawable.cd,
    imageresIcon(164) to R.drawable.cd,
    imageresIcon(165) to R.drawable.archive,
    imageresIcon(172) to R.drawable.folder,
    imageresIcon(175) to R.drawable.downloadsfolder,
    imageresIcon(176) to R.drawable.folder,
    imageresIcon(177) to R.drawable.folder,
    imageresIcon(178) to R.drawable.videosfolder,
    imageresIcon(179) to R.drawable.greenfolder,
    imageresIcon(200) to R.drawable.folder,
    msiexecIcon(0) to R.drawable.installer,
    msiIcon(2) to R.drawable.installer,
    msedgeIcon(13) to R.drawable.pdffile
)

private val cachedIcons = mutableMapOf<String, Drawable>()

/**
 * Gets all the built-in icons.
 *
 * @return A list of the Windows paths of the built-in icons.
 */
public fun builtInIcons(): List<String> {
    val resourceIds = windowsIcons.values.distinct()
    val result = resourceIds
        .map {resourceId -> windowsIcons.filterValues {it == resourceId}.keys.first()}
        .toMutableList()
    result.add(msedgeIcon(0))
    result.add(urlIcon(0))
    return result
}

/**
 * Gets all the icons for a specific app.
 *
 * @param app   The app to get the icons for.
 *
 * @return A list with two elements, one with the plain app icon and one with the app icon in a file icon.
 */
public fun appIcons(app: ApplicationInfo): List<String> {
    return listOf(
        "android:" + app.packageName + ",0",
        "android:" + app.packageName + ",1"
    )
}

/**
 * Gets all the icons for all installed apps.
 *
 * @return A list of the paths of all app icons.
 */
@SuppressLint("QueryPermissionsNeeded")
public fun allAppIcons(): List<String> {
    return App.context.packageManager.getInstalledApplications(0).flatMap {
        if (it.enabled && it.sourceDir.startsWith("/data/app")) appIcons(it) else listOf()
    }
}

/**
 * Gets the drawable of the icon.
 *
 * @param workingDirectory  The directory to use as working directory when resolving relative paths.
 * @param path              The Windows path of the icon.
 *
 * @return The drawable, or null if the icon isn't found.
 */
public fun iconFromPath(workingDirectory: Directory?, path: String): Drawable? {
    return cachedIcons[path]
        ?: iconFromDll(path)
        ?: iconFromApp(path)
        ?: iconFromFile(workingDirectory, path)
}

/**
 * Tries to parse the path of this icon as a path to a Windows system DLL (shell32.dll or imageres.dll), and if it succeeds returns the icon from the resources corresponding to that icon.
 *
 * @param path  The Windows path of the icon.
 *
 * @return The drawable if the parsing succeeded and the icon is supported by this app, null otherwise.
 */
private fun iconFromDll(path: String): Drawable? {
    val dllRegex = Regex(
        """^(?:(?:C:[/\\]Windows|%SystemRoot%)[/\\]System32[/\\])?([^/\\]+\.(?:dll|exe),[0-9]+)$""",
        RegexOption.IGNORE_CASE
    )
    val match = dllRegex.matchEntire(path)?.groupValues?.get(1)?.lowercase()
    val dllFile: String = if (match == null) path else "C:\\Windows\\System32\\$match"
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
            return iconFromApp("android:$defaultBrowser,$iconIndex")
        }
    }
    val resourceId: Int = windowsIcons[dllFile] ?: return null
    val result = AppCompatResources.getDrawable(App.context, resourceId)
    if (result != null) {
        cachedIcons[path] = result
    }
    return result
}

/**
 * Tries to parse the path of this icon as a package name of an app, and if it succeeds returns the corresponding icon from that app.
 *
 * @param path  The Windows path of the icon.
 *
 * @return The drawable if the parsing succeeded, null otherwise.
 */
private fun iconFromApp(path: String): Drawable? {
    val appRegex = Regex(
        """^android:([a-z0-9._]+),([01])$""",
        RegexOption.IGNORE_CASE
    )
    val appMatch: List<String> = appRegex.matchEntire(path)?.groupValues ?: return null
    val packageName = appMatch[1]
    val appIcon: Drawable = try {
        App.context.packageManager.getApplicationIcon(packageName)
    }
    catch (_: PackageManager.NameNotFoundException) {
        return null
    }
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
    cachedIcons[path] = result
    return result
}

/**
 * Gets the drawable from an icon file local on the Android device. If the path is an absolute Windows path, assumes that the drive letter refers to the current drive, otherwise resolves the relative path.
 *
 * @param workingDirectory  The directory to use as working directory when resolving relative paths.
 * @param path              The Windows path of the icon. Can be an absolute path or a relative path.
 *
 * @return The drawable if the file exists, null otherwise.
 */
private fun iconFromFile(workingDirectory: Directory?, path: String): Drawable? {
    val absolutePathRegex = Regex(
        """^[A-Z]:[/\\](.*)$""",
        RegexOption.IGNORE_CASE
    )
    val pathRelativeToDrive: String? =
        absolutePathRegex.matchEntire(path)?.groupValues?.get(1)

    val absoluteAndroidPathWithIndex: String =
        //It's a relative path (relative to the folder, not to the drive) or an Android path
        if (pathRelativeToDrive == null)
            if (path.startsWith("/")) path
            else workingDirectory?.resolveRelativePath(path) ?: return null
        //It's an absolute Windows path
        else
            workingDirectory?.drive()?.resolveRelativePath(pathRelativeToDrive) ?: return null

    val absolutePathWithoutIndex = absoluteAndroidPathWithIndex.replace(Regex(""",[0-9]+$"""), "")
    return Drawable.createFromPath(absolutePathWithoutIndex)
}
