# Files
This is customizable file explorer app for Android. For example, you can customize file icons and how different file types are displayed. It's also compatible with Windows and [AndroidDrive](https://github.com/GustavLindberg99/AndroidDrive), allowing you to browse and customize the files and folders on your phone both from your phone and your computer.

## Installation
This app can be downloaded on Google Play [here](https://play.google.com/store/apps/details?id=io.github.gustavlindberg99.files).

## Usage
### Basic Usage
The app must have permissions to access the fileystem to be able to work. If it doesn't already have it, it will ask for this permission the first time you open it.

When you start the app, it will show the files and folders in your internal storage. You can click on a file or folder to open it.

You can select files by long clicking on them. You can then move or copy them to another folder by dragging them once they're selected, or show a context menu with options by long clicking on them again or by clicking on the three dots icon on the top right. If you click on the three dots icon when no file or folder is selected, options for the folder you're currently in will be shown.

The top bar shows two folder names: the name of the parent folder of the folder you're in, then a slash, then the name of the current folder you're in. You can click on the name of the parent folder to open the parent folder. You can click on the name of the current folder to enter a path manually.

If you have multiple SD cards, you can see a list of them in the "This phone" folder (which is the parent folder of all folders).

### Hidden files and file extensions
By default, file extensions are hidden. You can show them by clicking on the gear icon on the top right and enabling "Show file extensions".

By default, files whose names begin with a dot are hidden and not shown (on Android, naming a file with a name that begins with a dot means that it's meant to be hidden). You can show such files by clicking on the gear icon on the top right and enabling "Show hidden files".

For compatibility with Windows and [AndroidDrive](https://github.com/GustavLindberg99/AndroidDrive), files named `desktop.ini` are also considered hidden (unlike files beginning with a dot, this is a Windows feature, not an Android feature). They will also be shown if you choose "Show hidden files". If you don't use Windows or AndroidDrive, you can ignore this feature, because in that case it's unlikely that you will have any files named `desktop.ini`.

### Customizing file types
This app allows you to customize file types by extension. You can choose what description a file type has and which icon it will be displayed with. To do so, click on the gear icon on the top right and choose "Manage file types". This allows you to change any settings related to file types that are specific to this app.

The app that is used to open files of a specific type is a system-wide setting and not specific to its app. You can customize this by long-clicking twice on a file of the type you're interested in, and choosing "Open with".

### Customizing the New menu
You can create new files by clicking on the three dots on the top right when no file is selected, then clicking on "New". By default, this list contains common file types, but this can be customized. To show or hide a specific file type in the New menu, click on the gear icon on the top right and choose "Manage file types", then click on the file type you're interested in, then enable or disable "Show in New menu".

You can also customize what a new file contains by default when it's created. To do so, go to Internal Storage/Android/data/io.github.gustavlindberg99.files/files/newmenu and create a file of the desired type with the same name and extension. For example for ZIP files, it should be called zip (so its name with extension is zip.zip). By default, there are three types for which this is the case: ZIP, TAR and 7Z, and their corresponding files are in this folder.

### Customizing folder icons
You can change the icon of a specific folder by long-clicking twice on the folder, selecting "Properties", then clicking on an icon. Custom icons are compatible with Windows, so if you change the icon of a folder from this app, that icon will be visible if you browse it on Windows using [AndroidDrive](https://github.com/GustavLindberg99/AndroidDrive). The opposite also works, but some Windows icons are not supported by this app. For copyright reasons, the icons displayed in this app don't look exactly like the ones displayed in Windows.

### Media folders
Some apps check if folders are media folders. For example, Google Photos only shows photos in folders that are media folders. This is a system-wide Android feature that is not specific to this app: folders that contain a file named `.nomedia` or whose direct or indirect parents contain such a file are not considered media folders, all other folders are considered media folders.

However, this app makes it easy to choose which folders are and aren't media folders: long-click twice on a folder, select Properties and select or deselect "Media folder". There's no need to create or delete `.nomedia` files manually. Also, since `.nomedia` starts with a dot, such files are hidden by default, allowing you to focus on files with actual content.

### Supported file types
This app supports certain file types itself (meaning you don't need another app to open them).

Archives (compressed folders) in the ZIP, TAR and 7Z formats are fully supported by this app. You can manage files in them like you could in regular folders.

Internet shortcuts in the URL format are also fully supported by this app. If you open an internet shortcut, that page will be opened in your default browser. You can also create new internet shortcuts by clicking on the three dots on the top right, then New > Internet shortcut (this option only exists if the "Show in New menu" option is enabled for URL files, which it is by default, it can be enabled and disabled by going to the gear icon then "Handle file types"). The URL format is compatible with Windows, so you create and use the same internet shortcuts on your Windows computer with [AndroidDrive](https://github.com/GustavLindberg99/AndroidDrive).

Windows shortcuts (LNK files) can be opened by this app. This means that if you have a shortcut to a folder that folder will be opened in this app when you click on the shortcut, and if you have a shortut to a file that file will be opened in the default app for that file type. You can however not create or modify shortcuts with this app. Creating and modifying shortcuts on your phone must be done on your Windows computer using [AndroidDrive](https://github.com/GustavLindberg99/AndroidDrive).
