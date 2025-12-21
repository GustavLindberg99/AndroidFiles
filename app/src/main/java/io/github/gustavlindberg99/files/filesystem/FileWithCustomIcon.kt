package io.github.gustavlindberg99.files.filesystem

import io.github.gustavlindberg99.files.preferences.Icon
import java.io.IOException

/**
 * Represents any file or folder that can have a custom icon.
 */
interface FileWithCustomIcon {
    /**
     * Sets the icon of this file or folder to the specified icon.
     *
     * @param icon  The Windows path of the icon to set.
     *
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public abstract fun setIcon(icon: Icon)
}