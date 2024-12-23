package io.github.gustavlindberg99.files.activity

import androidx.appcompat.app.AppCompatActivity

/**
 * Dummy activity that is disabled by default and that can open any file type (see AndroidManifest.xml). To show the open with dialog, this class is temporarily enabled so that the system thinks a new app can open the file, then the file is opened and this activity is disabled again. Source: https://stackoverflow.com/a/23374389/4284627.
 */
class OpenWithTrigger: AppCompatActivity()