package io.github.gustavlindberg99.files.preferences

import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.App
import org.apache.commons.io.FileUtils
import java.io.IOException

/**
 * Checks no file types have been set yet, and if they haven't, sets the file types that the app should know of by default.
 */
public fun initializeDefaultFileTypes() {
    //If there already are file types available, the app has already been initialized in which case there's no need to do anything
    if (FileType.getAll().isNotEmpty()) {
        return
    }

    //Initialize the file types
    val lnk = FileType("lnk")
    lnk.description = App.context.getString(R.string.shortcut)
    lnk.alwaysShowExt = false

    val url = FileType("url")
    url.description = App.context.getString(R.string.internetShortcut)
    url.alwaysShowExt = false
    url.showInNewMenu = true
    url.iconPath = urlIcon(0)

    val nomedia = FileType("nomedia")
    nomedia.description = App.context.getString(R.string.configurationSettings)
    nomedia.alwaysShowExt = true
    nomedia.iconPath = shell32Icon(72)

    val ini = FileType("ini")
    ini.description = App.context.getString(R.string.configurationSettings)
    ini.alwaysShowExt = true
    ini.iconPath = shell32Icon(72)

    val apk = FileType("apk")
    apk.description = App.context.getString(R.string.appInstallerPackage)
    apk.iconPath = msiexecIcon(0)

    val txt = FileType("txt")
    txt.description = App.context.getString(R.string.textDocument)
    txt.alwaysShowExt = false
    txt.showInNewMenu = true
    txt.iconPath = shell32Icon(70)

    val pdf = FileType("pdf")
    pdf.alwaysShowExt = false
    pdf.iconPath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe,13"

    val ico = FileType("ico")
    ico.description = App.context.getString(R.string.icon)
    ico.alwaysShowExt = false

    for (archiveExtension in listOf("zip", "tar", "7z")) {
        val archiveType = FileType(archiveExtension)
        archiveType.description = String.format(
            App.context.getString(R.string.archiveFormat),
            archiveExtension.uppercase()
        )
        archiveType.alwaysShowExt = false
        archiveType.showInNewMenu = true
        archiveType.iconPath = imageresIcon(165)
    }

    for (imageExtension in listOf("png", "jpg", "jpeg", "gif", "bmp", "heic", "hif", "svg")) {
        val imageType = FileType(imageExtension)
        imageType.description = String.format(
            App.context.getString(R.string.imageFormat),
            imageExtension.uppercase()
        )
        imageType.alwaysShowExt = false
        imageType.iconPath = shell32Icon(311)
    }

    for (audioExtension in listOf("mp3", "m4a", "wma")) {
        val audioType = FileType(audioExtension)
        audioType.description = String.format(
            App.context.getString(R.string.audioFormat),
            audioExtension.uppercase()
        )
        audioType.alwaysShowExt = false
        audioType.iconPath = shell32Icon(116)
    }

    for (videoExtension in listOf("mp4", "m4v", "wmv", "mkv")) {
        val videoType = FileType(videoExtension)
        videoType.description = String.format(
            App.context.getString(R.string.videoFormat),
            videoExtension.uppercase()
        )
        videoType.alwaysShowExt = false
        videoType.iconPath = shell32Icon(313)
    }

    //Create default files for the New menu
    val newMenuDir = App.context.getExternalFilesDir(null)?.resolve("newmenu")
    if (newMenuDir != null) {
        newMenuDir.mkdirs()
        for (fileName in App.context.assets.list("newmenu") ?: arrayOf()) {
            try {
                val inputStream = App.context.assets.open("newmenu/$fileName")
                FileUtils.copyInputStreamToFile(inputStream, newMenuDir.resolve(fileName))
            }
            catch (_: IOException) {
                //Ignore, just continue with the next file
            }
        }
    }
}