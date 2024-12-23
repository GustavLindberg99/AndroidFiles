package io.github.gustavlindberg99.files.filesystem

import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.App
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * If the given path corresponds to a file inside an archive, returns the FileOrFolder object corresponding to that path. This is necessary since the built-in java.io.File class used for checking if files exist doesn't support archives.
 *
 * @param path  The absolute path of the file or folder, including the archive's path. Example: /sdcard/archive.zip/file.txt.
 *
 * @return The file corresponding to the path if it's in an archive, or null if it doesn't exist or isn't in an archive.
 */
internal fun resolveFileInArchive(path: String): FileOrFolder? {
    var dirInArchive: Folder? = parentArchiveOfPath(path)
    if (dirInArchive == null) {
        return null
    }
    val parentDirs = pathRelativeToParentArchive(path).split("/").dropLast(1)
    for (dirName in parentDirs) {
        dirInArchive = dirInArchive?.files()?.find {it.name() == dirName} as? Folder
    }
    return dirInArchive?.files()?.find {it.name() == File(path).name}
}

/**
 * Gets the archive that contains the given path.
 *
 * @param path  The path to find the parent archive of.
 *
 * @return The archive containing the given path, or null if the path isn't in an archive.
 */
public fun parentArchiveOfPath(path: String): Archive? {
    val parentDirs = path.split("/").dropLast(1)
    for (i in parentDirs.indices.reversed()) {
        val archive = Archive.fromPath(parentDirs.slice(0..i).joinToString("/"))
        if (archive != null) {
            return archive
        }
    }
    return null
}

/**
 * Gets the path relative to the nearest parent archive (for example if the parameter is /sdcard/foo.zip/bar/baz.txt, returns bar/baz.txt).
 *
 * @param path  The absolute path of the file or folder to get the relative path of.
 *
 * @return The path relative to the parent archive without a leading slash. If the path doesn't correspond to a file or folder in an archive, returns the absolute path (even if it doesn't exist).
 */
public fun pathRelativeToParentArchive(path: String): String {
    val parentArchive = parentArchiveOfPath(path) ?: return path
    return path.replaceFirst(parentArchive.absolutePath() + "/", "")
}

/**
 * Writes contents to an ini file. If the file doesn't exist, creates it. If the file already exists, overwrites it.
 *
 * @param filePath  The absolute path of the file to write to.
 * @param contents  The contents to write as a Map<sections, Map<keys, values>>.
 *
 * @throws IOException if the file could not be written to.
 */
public fun writeIniFile(filePath: String, contents: Map<String, Map<String, Any>>) {
    var output = ""
    for ((sectionName, section) in contents) {
        output += "[$sectionName]\n"
        for ((key, value) in section) {
            output += "$key=$value\n"
        }
    }
    Files.write(Paths.get(filePath), output.encodeToByteArray())
}

/**
 * Converts a file size to a string with the appropriate unit (bytes, kB, MB, etc depending on the size).
 *
 * @param size  The file size to convert to a string.
 *
 * @return A human-readable string with the file size.
 */
public fun fileSizeToString(size: Long): String {
    val byte = 1L
    val kilobyte = 1024L
    val megabyte = 1024L * 1024L
    val gigabyte = 1024L * 1024L * 1024L
    val terabyte = 1024L * 1024L * 1024L * 1024L
    val (stringResource, unit) = when (size) {
        in 0..<kilobyte -> Pair(R.plurals.bytes, byte)
        in kilobyte..<megabyte -> Pair(R.plurals.kilobytes, kilobyte)
        in megabyte..<gigabyte -> Pair(R.plurals.megabytes, megabyte)
        in gigabyte..<terabyte -> Pair(R.plurals.gigabytes, gigabyte)
        else -> Pair(R.plurals.terabytes, terabyte)
    }
    val sizeInUnit = (size / unit).toInt()
    return App.context.resources.getQuantityString(stringResource, sizeInUnit, sizeInUnit)
}

/**
 * Gets the absolute paths of all SD cards (including the internal storage).
 *
 * @return The absolute paths of all the SD cards.
 */
internal fun sdCardPaths(): Set<String> {
    //This returns /sdcard/Android/data/(app name)/files on each SD card, so the path of the SD card itself is four levels up.
    return App.context.getExternalFilesDirs(null).mapNotNull {
        it?.parentFile?.parentFile?.parentFile?.parentFile?.absolutePath
    }.toSet()
}