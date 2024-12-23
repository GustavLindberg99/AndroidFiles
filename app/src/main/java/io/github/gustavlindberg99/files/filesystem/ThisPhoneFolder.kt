package io.github.gustavlindberg99.files.filesystem

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import io.github.gustavlindberg99.files.activity.App
import io.github.gustavlindberg99.files.R
import java.io.IOException

/**
 * Represents the This Phone folder.
 */
object ThisPhoneFolder: Folder {
    public override fun equals(other: Any?): Boolean {
        return other is ThisPhoneFolder
    }

    public override fun hashCode(): Int {
        return this.absolutePath().hashCode()
    }

    public override fun absolutePath(): String {
        return "/"
    }

    public override fun name(): String {
        return App.context.getString(R.string.thisPhone)
    }

    public override fun typeName(): String {
        return Build.MODEL
    }

    public override fun hidden(): Boolean {
        return false
    }

    public override fun files(): Set<FileOrFolder> {
        return sdCardPaths().mapNotNull {Drive.fromPath(it)}.toSet()
    }

    public override fun icon(): Drawable {
        return AppCompatResources.getDrawable(App.context, R.drawable.phone)!!
    }

    public override fun parentFolder(): Folder? {
        return null
    }

    public override fun move(destination: Folder) {
        throw IOException(App.context.getString(R.string.cannotMoveThisPhoneFolder))
    }

    public override fun copy(destination: Folder, newName: String) {
        throw IOException(App.context.getString(R.string.cannotCopyThisPhoneFolder))
    }

    public override fun rename(newName: String) {
        throw IOException(App.context.getString(R.string.cannotRenameThisPhoneFolder))
    }

    public override fun delete() {
        throw IOException(App.context.getString(R.string.cannotDeleteThisPhoneFolder))
    }
}