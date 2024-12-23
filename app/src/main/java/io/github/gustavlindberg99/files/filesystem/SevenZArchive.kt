package io.github.gustavlindberg99.files.filesystem

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.attribute.FileTime

/**
 * Represents a 7Z archive.
 *
 * @param _7zFile       The parsed contents of this file.
 * @param getFile       A lambda that returns a File object that exists and can be opened. Useful for files inside archives since those files need to be extracted to a temporary folder before they can be opened.
 * @param absolutePath  The absolute path of this file.
 * @param size          The size of this file.
 */
class SevenZArchive internal constructor(
    private val _7zFile: SevenZFile,
    getFile: () -> File,
    absolutePath: String,
    size: Long
): Archive(getFile, absolutePath, size) {
    companion object {
        /**
         * Gets the SevenZFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The 7Z file at the given path, or null if the path doesn't exist or isn't a valid 7Z file.
         */
        public fun fromPath(path: String): SevenZArchive? {
            val file = File(path)
            if (!file.exists()) {
                return resolveFileInArchive(path) as? SevenZArchive
            }
            val sevenZFile = Archive.archiveLibObject(path, ::SevenZFile) ?: return null
            return SevenZArchive(sevenZFile, {file}, path, file.length())
        }

        /**
         * Compresses the specified files or folders to a 7Z archive.
         *
         * @param files         The files to compress.
         * @param destination   The absolute path of the 7Z archive to compress them to.
         * @param fileToChange  If null, keep all the files with the same names as they already have. If Pair<String, null>, ignore the file at the specified relative path. If Pair<String, String>, use the second argument as relative path when compressing the file with relative path equal to the first argument.
         * @param extraFiles    Extra files to compress with a relative path different from its own name.
         *
         * @throws IOException if an I/O error occurs when reading or writing.
         */
        public fun compress(
            files: Collection<FileOrFolder>,
            destination: String,
            fileToChange: Pair<String, String?>? = null,
            extraFiles: Collection<Pair<FileOrFolder, String>> = listOf()
        ) {
            Archive.compress(
                files,
                destination,
                ::SevenZOutputFile,
                {out, file, name -> file?.copyTo7zFile(out, name)},
                fileToChange,
                extraFiles
            )
        }
    }

    protected override fun entries(): List<Triple<Any, String, Long?>> {
        return this._7zFile.entries.toList()
            .map {Triple(it, it.name, if (it.isDirectory) null else it.size)}
    }

    protected override fun getInputStreamFromEntry(entry: Any): InputStream {
        return this._7zFile.getInputStream(entry as SevenZArchiveEntry)
    }

    internal override fun entryCompressedSize(relativePath: String): Long? {
        return null
    }

    internal override fun entryLastModified(relativePath: String): FileTime? {
        try {
            val timestamp = this._7zFile.entries.toList()
                .find {it.name == relativePath}
                ?.lastModifiedDate?.time ?: return null
            return FileTime.fromMillis(timestamp)
        }
        catch(_: UnsupportedOperationException){
            return null
        }
    }
}