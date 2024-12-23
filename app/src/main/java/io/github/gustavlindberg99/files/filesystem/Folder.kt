package io.github.gustavlindberg99.files.filesystem

import io.github.gustavlindberg99.files.activity.FileExplorerActivity

/**
 * Represents any folder, both directories and special folders.
 */
interface Folder: FileOrFolder {
    companion object {
        /**
         * Gets the Folder object at the given path. Can't be a constructor because it must be able to return null.
         *
         * @param path  The path of the folder.
         *
         * @return The folder at the given path, or null if the path doesn't exist or isn't a folder.
         */
        public fun fromPath(path: String): Folder? {
            if (path == "/") {
                return ThisPhoneFolder
            }
            else {
                return Directory.fromPath(path) ?: Archive.fromPath(path)
            }
        }
    }

    public override fun baseName(): String {
        return this.name()
    }

    public override fun extension(): String {
        return ""
    }

    public override fun open(activity: FileExplorerActivity) {
        activity.openFolder(this)
    }

    /**
     * @return A list of the files and folders contained by this folder.
     */
    public abstract fun files(): Set<FileOrFolder>

    /**
     * Gets the drive that this directory is on.
     *
     * @return The drive, or null if the drive wasn't found.
     */
    public fun drive(): Drive? {
        var parent: Folder? = this
        while (parent != null) {
            if (parent is Drive) {
                return parent
            }
            parent = parent.parentFolder()
        }
        return null
    }
}