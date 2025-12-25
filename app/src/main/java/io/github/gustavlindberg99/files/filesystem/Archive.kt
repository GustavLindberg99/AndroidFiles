package io.github.gustavlindberg99.files.filesystem

import io.github.gustavlindberg99.files.BuildConfig
import io.github.gustavlindberg99.files.R
import io.github.gustavlindberg99.files.activity.FileExplorerActivity
import io.github.gustavlindberg99.files.activity.App
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarFile
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.util.zip.ZipFile

/**
 * Abstract class representing any archive.
 *
 * @param getFile       A lambda that returns a File object that exists and can be opened. Useful for files inside archives since those files need to be extracted to a temporary folder before they can be opened.
 * @param absolutePath  The absolute path of this file.
 * @param size          The size of this file.
 */
abstract class Archive protected constructor(
    getFile: () -> File,
    absolutePath: String,
    size: Long
) : GeneralFile(getFile, absolutePath, size), Folder {
    companion object {
        /**
         * Gets the ArchiveFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The archive at the given path, or null if the path doesn't exist or isn't a valid archive.
         */
        public fun fromPath(path: String): Archive? {
            return ZipArchive.fromPath(path)
                ?: TarArchive.fromPath(path)
                ?: SevenZArchive.fromPath(path)
        }

        /**
         * Creates an object of a specific archive library type.
         *
         * @param path          The path of the archive to parse.
         * @param constructor   The constructor of the type to create.
         *
         * @return The library object for the specified archive, or null if the archive couldn't be parsed in that format.
         */
        @JvmStatic
        protected fun <T> archiveLibObject(path: String, constructor: (File) -> T): T? {
            val file = File(path)
            if (!file.exists()) {
                return null
            }
            return try {
                constructor(file)
            }
            catch (_: IOException) {
                null
            }
        }

        /**
         * Compresses the specified files or folders to an archive.
         *
         * @param files         The files to compress.
         * @param destination   The absolute path of the ZIP archive to compress them to.
         * @param constructor   The constructor of the output stream type specific to this type of archive.
         * @param putNextEntry  A lambda that puts an entry in the output stream with the path relative to the archive specified as argument, and writes the contents to that entry.
         * @param fileToChange  If null, keep all the files with the same names as they already have. If Pair<String, null>, ignore the file at the specified relative path. If Pair<String, String>, use the second argument as relative path when compressing the file with relative path equal to the first argument.
         * @param extraFiles    Extra files to compress with a relative path different from its own name.
         *
         * @throws IOException if an I/O error occurs when reading or writing.
         */
        @JvmStatic
        protected fun <T : Closeable> compress(
            files: Collection<FileOrFolder>,
            destination: String,
            constructor: (File) -> T,
            putNextEntry: (T, GeneralFile?, String) -> Unit,
            fileToChange: Pair<String, String?>?,
            extraFiles: Collection<Pair<FileOrFolder, String>>
        ) {
            //Get all the entries
            val entries = mutableListOf<Pair<GeneralFile?, String>>()
            lateinit var compressFiles: (Collection<FileOrFolder>, String) -> Unit
            compressFiles = { subFiles: Collection<FileOrFolder>, prefix: String ->
                for (file in subFiles) {
                    if (file is GeneralFile) {
                        val name = prefix + file.name()
                        if (
                            (fileToChange != null && (fileToChange.first == name || name.startsWith(
                                fileToChange.first + "/"
                            )))
                        ) {
                            val newName = name.replaceFirst(
                                fileToChange.first,
                                fileToChange.second ?: continue
                            )
                            entries.add(file to newName)
                        }
                        else {
                            entries.add(file to name)
                        }
                    }
                    else if (file is Directory) {
                        if (file.files().isEmpty()) {
                            entries.add(null to prefix + file.name() + "/")
                        }
                        else {
                            compressFiles(file.files(), prefix + file.name() + "/")
                        }
                    }
                }
            }
            compressFiles(files, "")
            for ((file, subDestination) in extraFiles) {
                if (file is GeneralFile) {
                    entries.add(file to subDestination)
                }
                else if (file is Directory) {
                    if (file.files().isEmpty()) {
                        entries.add(null to "$subDestination/")
                    }
                    else {
                        compressFiles(file.files(), "$subDestination/")
                    }
                }
            }

            //Make sure that there are no files and folders with the same name
            for (fileNamePair in entries) {
                val name = fileNamePair.second
                if (entries.any { it !== fileNamePair && it.second.startsWith("$name/") }) {
                    throw IOException(
                        String.format(
                            App.context.getString(R.string.entryBothFileAndDirectory),
                            name
                        )
                    )
                }
            }

            //Write the entries to the output stream. Use reversed because distinctBy keeps the first one, but we want to keep the last one so that files get overwritten.
            val outputStream = constructor(File(destination))
            for ((file, name) in entries.reversed().distinctBy { it.second }) {
                putNextEntry(outputStream, file, name)
            }
            outputStream.close()
        }
    }

    public override fun baseName(): String = super<GeneralFile>.baseName()
    public override fun extension(): String = super<GeneralFile>.extension()

    public override fun open(activity: FileExplorerActivity) {
        if (this.fileType().openWith()?.packageName == BuildConfig.APPLICATION_ID) {
            super<Folder>.open(activity)
        }
        else {
            super<GeneralFile>.open(activity)
        }
    }

    public override fun files(): Set<FileOrFolder> {
        val result = mutableSetOf<FileOrFolder>()
        val directories = mutableMapOf<Directory, MutableSet<FileOrFolder>>()
        for ((entry, rawPath, size) in this.entries()) {
            val path = rawPath.replace(Regex("/+"), "/").replace(Regex("/$"), "")
            val parentDirs = path.split("/").filter { it.isNotEmpty() }.dropLast(1)
            val absolutePath = this.absolutePath() + "/" + path
            val file =
                //If it's a directory entry, add an empty directory. If it's not supposed to be empty and it's already been added, nothing happens because if it's a set. If it will be added later, this empty directory will be removed first, see below.
                if (size == null) Directory({ setOf() }, absolutePath)
                else {
                    val getTempFile = { this.createTemporaryFileFromZipEntry(entry, path) }
                    val tempFilePath = getTempFile().absolutePath
                    val zip = Archive.archiveLibObject(tempFilePath, ::ZipFile)
                    val tar = Archive.archiveLibObject(tempFilePath, ::TarFile)
                    val sevenZ = Archive.archiveLibObject(tempFilePath, ::SevenZFile)
                    if (zip != null) ZipArchive(zip, getTempFile, absolutePath, size)
                    else if (tar != null) TarArchive(tar, getTempFile, absolutePath, size)
                    else if (sevenZ != null) SevenZArchive(sevenZ, getTempFile, absolutePath, size)
                    else GeneralFile(getTempFile, absolutePath, size)
                }

            var previousParent: Directory? = null
            for (i in parentDirs.indices) {
                lateinit var nextParent: Directory
                nextParent = Directory(
                    { directories.getValue(nextParent) },
                    this.absolutePath() + "/" + parentDirs.slice(0..i).joinToString("/")
                )
                if (nextParent !in directories) {
                    directories[nextParent] = mutableSetOf()
                }
                if (previousParent == null) {
                    if (nextParent in result) {
                        //If there's a directory entry for this directory, there's a risk that we already added an empty directory for it, in which case we remove it and then add a non-empty directory.
                        result.remove(nextParent)
                    }
                    result.add(nextParent)
                }
                else {
                    directories.getValue(previousParent).add(nextParent)
                }
                previousParent = nextParent
            }
            if (previousParent == null) {
                //If it's a file at root, just add it to the result
                result.add(file)
            }
            else {
                //Otherwise if it's in a subdirectory, add it to the last subdirectory
                directories.getValue(previousParent).add(file)
            }
        }

        return result
    }

    /**
     * Creates a temporary file from the given zip entry so that it can be opened.
     *
     * @param entry The zip entry to create the file from.
     *
     * @return A File object corresponding to the newly created temporary file, or null if an error occurred.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    private fun createTemporaryFileFromZipEntry(entry: Any, path: String): File {
        val inputStream = this.getInputStreamFromEntry(entry)
        val tempFile = File.createTempFile(
            "zipEntry",
            "." + File(path).extension,
            App.context.cacheDir
        )
        FileUtils.copyInputStreamToFile(inputStream, tempFile)
        return tempFile
    }

    /**
     * Adds new files to the archive. If a file with that name already exists, overwrites it.
     *
     * @param files Collection<Pair<files to add, relative path that they should have in the archive (without leading slash)>>
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public fun addFiles(files: Collection<Pair<FileOrFolder, String>>) {
        //This needs to be a temporary file so that we don't mess up the original if something goes wrong
        val tempArchive = File.createTempFile(
            "archiveEdit",
            "." + this.extension(),
            App.context.cacheDir
        )
        val compressFunction =
            if (this is ZipArchive) ZipArchive::compress
            else if (this is TarArchive) TarArchive::compress
            else if (this is SevenZArchive) SevenZArchive::compress
            else throw IOException()
        compressFunction(this.files(), tempArchive.absolutePath, null, files)
        this.applyChanges(tempArchive)
    }

    /**
     * Creates an empty directory in this archive.
     *
     * @param relativePath  The path of the directory to create relative to the root of this archive, without a leading slash.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public fun createEmptyDirectory(relativePath: String) {
        val emptyDirectory = Directory({ setOf() }, this.absolutePath() + "/" + relativePath)
        this.addFiles(listOf(emptyDirectory to relativePath))
    }

    /**
     * Creates an empty file in this archive.
     *
     * @param relativePath  The path of the file to create relative to the root of this archive, without a leading slash.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public fun createEmptyFile(relativePath: String) {
        val tempFile = File.createTempFile("emptyFile", null, App.context.cacheDir)
        val emptyFile = GeneralFile.fromPath(tempFile.absolutePath) ?: throw IOException()
        this.addFiles(listOf(emptyFile to relativePath))
    }

    /**
     * Renames the file at the old path to the new path, or if the new path is null, deletes the file at the old path. Both paths are relative to the root of the archive.
     *
     * @param oldPath   The path of the existing file to rename, without a leading slash.
     * @param newPath   The path that the file should have after renaming, without a leading slash.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public fun renameOrDeleteFile(oldPath: String, newPath: String?) {
        //This needs to be a temporary file so that we don't mess up the original if something goes wrong
        val tempArchive = File.createTempFile(
            "archiveEdit",
            "." + this.extension(),
            App.context.cacheDir
        )
        val compressFunction =
            if (this is ZipArchive) ZipArchive::compress
            else if (this is TarArchive) TarArchive::compress
            else if (this is SevenZArchive) SevenZArchive::compress
            else throw IOException()
        compressFunction(this.files(), tempArchive.absolutePath, oldPath to newPath, listOf())
        this.applyChanges(tempArchive)
    }

    /**
     * Moves a temporary archive onto this archive, overwriting this archive. Useful if changes to this archive were made in a temporary file and those changes should be applied.
     *
     * @param tempArchive   The temporary archive to overwrite this archive with.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    private fun applyChanges(tempArchive: File) {
        val parentArchive = this.parentArchive()
        if (parentArchive == null) {
            Files.move(tempArchive.toPath(), this.path(), StandardCopyOption.REPLACE_EXISTING)
        }
        else {
            val relativePath = pathRelativeToParentArchive(this.absolutePath())
            val tempFile = Archive.fromPath(tempArchive.absolutePath) ?: throw IOException()
            parentArchive.addFiles(listOf(tempFile to relativePath))
        }
    }

    /**
     * Gets a list of all non-directory entries.
     *
     * @return List<Triple<entry, path, size>>, where entry is the *ArchiveEntry object, path is the path of the entry relative to the archive file, and size is the uncompressed size. If it's a directory, size is null.
     */
    protected abstract fun entries(): List<Triple<Any, String, Long?>>

    /**
     * Gets an input stream from an entry.
     *
     * @param entry The *ArchiveEntry object to get the input stream from.
     *
     * @return An input stream from which the contents of the entry can be read.
     */
    protected abstract fun getInputStreamFromEntry(entry: Any): InputStream

    /**
     * Gets the compressed size of a file in the archive.
     *
     * @param relativePath  The relative path of the file to get the compressed size of.
     *
     * @return The compressed size of the file, or null if it wasn't found.
     */
    internal abstract fun entryCompressedSize(relativePath: String): Long?

    /**
     * Gets the last modified time of a file in the archive.
     *
     * @param relativePath  The relative path of the file to get the compressed size of.
     *
     * @return The last modified time of the file, or null if it wasn't found.
     */
    internal abstract fun entryLastModified(relativePath: String): FileTime?
}