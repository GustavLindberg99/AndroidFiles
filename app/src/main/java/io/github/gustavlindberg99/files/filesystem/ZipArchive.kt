package io.github.gustavlindberg99.files.filesystem

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Represents a ZIP archive.
 *
 * @param _zipFile      The parsed contents of this file.
 * @param getFile       A lambda that returns a File object that exists and can be opened. Useful for files inside archives since those files need to be extracted to a temporary folder before they can be opened.
 * @param absolutePath  The absolute path of this file.
 * @param size          The size of this file.
 */
class ZipArchive internal constructor(
    private val _zipFile: ZipFile,
    getFile: () -> File,
    absolutePath: String,
    size: Long
): Archive(getFile, absolutePath, size) {
    companion object {
        /**
         * Gets the ZipFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The ZIP file at the given path, or null if the path doesn't exist or isn't a valid ZIP file.
         */
        public fun fromPath(path: String): ZipArchive? {
            val file = File(path)
            if (!file.exists()) {
                return resolveFileInArchive(path) as? ZipArchive
            }
            val zipFile = Archive.archiveLibObject(path, ::ZipFile) ?: return null
            try {
                //Workaround for https://issuetracker.google.com/issues/374313520
                zipFile.entries().toList()
            }
            catch (_: Exception) {
                return null
            }
            return ZipArchive(zipFile, {file}, path, file.length())
        }

        /**
         * Compresses the specified files or folders to a ZIP archive.
         *
         * @param files         The files to compress.
         * @param destination   The absolute path of the ZIP archive to compress them to.
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
                {ZipOutputStream(FileOutputStream(it))},
                {out, file, name ->
                    out.putNextEntry(ZipEntry(name))
                    file?.writeToOutputStream(out)
                    out.closeEntry()
                },
                fileToChange,
                extraFiles
            )
        }
    }

    protected override fun entries(): List<Triple<Any, String, Long?>> {
        return this._zipFile.entries().toList()
            .map {Triple(it, it.name, if (it.isDirectory) null else it.size)}
    }

    protected override fun getInputStreamFromEntry(entry: Any): InputStream {
        return this._zipFile.getInputStream(entry as ZipEntry)
    }

    internal override fun entryCompressedSize(relativePath: String): Long? {
        return this._zipFile.entries().toList().find {it.name == relativePath}?.compressedSize
    }

    internal override fun entryLastModified(relativePath: String): FileTime? {
        return this._zipFile.entries().toList().find {it.name == relativePath}?.lastModifiedTime
    }
}