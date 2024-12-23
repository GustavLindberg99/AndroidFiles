package io.github.gustavlindberg99.files.preferences

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.github.gustavlindberg99.files.activity.App

private const val STORAGE_PERMISSIONS_REQUEST_CODE = 100

/**
 * Class representing a storage permissions request. Instances must be created in Activity::onCreate() because it calls registerForActivityResult.
 *
 * @param _activity The activity that sends the permissions request.
 */
class StoragePermissionsRequest(private val _activity: AppCompatActivity) {
    private val _permissionsLauncher =
        this._activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), {
            this.handleStoragePermissionsResult()
        })

    private var _onPermissionsGrantedCallback: () -> Unit = {}

    /**
     * Register a callback to be invoked when storage permissions have been granted.
     *
     * @param callback  The callback that will be run.
     */
    public fun setOnPermissionsGrantedListener(callback: () -> Unit) {
        this._onPermissionsGrantedCallback = callback
    }

    /**
     * If the app doesn't have storage permissions, requests the permissions and runs the callback when the permissions have been granted. If the app already has storage permissions, runs the callback immediately.
     */
    public fun requestPermissions() {
        if (!App.hasStoragePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.requestStoragePermissionAfter30()
            }
            else {
                this.requestStoragePermissionBefore30()
            }
        }
        else {
            this._onPermissionsGrantedCallback()
        }
    }

    /**
     * If the request code corresponds to this class' request code, handles the storage permissions result. Intended to be called from Activity::onRequestPermissionsResult().
     *
     * @param requestCode   The request code of the permissions request.
     */
    public fun handleRequestPermissionsResult(requestCode: Int){
        if (requestCode == STORAGE_PERMISSIONS_REQUEST_CODE) {
            this.handleStoragePermissionsResult()
        }
    }

    /**
     * Function to be run when the user has granted or denied storage permissions.
     */
    private fun handleStoragePermissionsResult() {
        if (App.hasStoragePermissions()) {
            initializeDefaultFileTypes()
            this._onPermissionsGrantedCallback()
        }
        else {
            //The user denied the permission. It's not possible for this app to do anything without it, so open the permission settings.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", App.context.packageName, null)
            intent.setData(uri)
            this._activity.startActivity(intent)
        }
    }

    /**
     * Requests the necessary permissions to access storage on Android versions < 30.
     */
    private fun requestStoragePermissionBefore30() {
        ActivityCompat.requestPermissions(
            this._activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSIONS_REQUEST_CODE
        )
    }

    /**
     * Requests the necessary permissions to access storage on Android versions >= 30.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestStoragePermissionAfter30() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.setData(
                Uri.parse("package:" + this._activity.applicationContext.packageName)
            )
            this._permissionsLauncher.launch(intent)
        }
        catch (_: Exception) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            this._permissionsLauncher.launch(intent)
        }
    }
}