package io.github.gustavlindberg99.files.filesystem

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.attribute.FileTime

/**
 * Represents a TAR archive.
 *
 * @param _tarFile      The parsed contents of this file.
 * @param getFile       A lambda that returns a File object that exists and can be opened. Useful for files inside archives since those files need to be extracted to a temporary folder before they can be opened.
 * @param absolutePath  The absolute path of this file.
 * @param size          The size of this file.
 */
class TarArchive internal constructor(
    private val _tarFile: TarFile,
    getFile: () -> File,
    absolutePath: String,
    size: Long
): Archive(getFile, absolutePath, size) {
    companion object {
        /**
         * Gets the TarFile object at the given path.
         *
         * @param path  The path of the file.
         *
         * @return The TAR file at the given path, or null if the path doesn't exist or isn't a valid TAR file.
         */
        public fun fromPath(path: String): TarArchive? {
            val file = File(path)
            //Require the extension to be tar because otherwise for some reason Apache Commons Compress considers plain text files to be valid empty tar files
            if (!file.exists() || file.extension.lowercase() != "tar") {
                return resolveFileInArchive(path) as? TarArchive
            }
            val tarFile = Archive.archiveLibObject(path, ::TarFile) ?: return null
            return TarArchive(tarFile, {file}, path, file.length())
        }

        /**
         * Compresses the specified files or folders to a TAR archive.
         *
         * @param files         The files to compress.
         * @param destination   The absolute path of the TAR archive to compress them to.
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
                {TarArchiveOutputStream(FileOutputStream(it))},
                {out, file, name ->
                    println("Hello World: $name, ${file?.size()}")
                    val entry = TarArchiveEntry(name)
                    if (file != null) {
                        entry.size = file.size()
                    }
                    out.putArchiveEntry(entry)
                    file?.writeToOutputStream(out)
                    out.closeArchiveEntry()
                },
                fileToChange,
                extraFiles
            )
        }
    }

    protected override fun entries(): List<Triple<Any, String, Long?>> {
        return this._tarFile.entries.toList()
            .map {Triple(it, it.name, if (it.isDirectory) null else it.realSize)}
    }

    protected override fun getInputStreamFromEntry(entry: Any): InputStream {
        return this._tarFile.getInputStream(entry as TarArchiveEntry)
    }

    internal override fun entryCompressedSize(relativePath: String): Long? {
        return this._tarFile.entries.toList().find {it.name == relativePath}?.size
    }

    internal override fun entryLastModified(relativePath: String): FileTime? {
        val timestamp = this._tarFile.entries.toList()
            .find {it.name == relativePath}
            ?.lastModifiedDate?.time ?: return null
        return FileTime.fromMillis(timestamp)
    }
}