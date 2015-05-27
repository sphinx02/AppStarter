FireStarter
=========

[![MPLv2 License](http://img.shields.io/badge/license-MPLv2-blue.svg?style=flat-square)](https://www.mozilla.org/MPL/2.0/)

__FireStarter is a Non-Root Launcher Replacement / App-Drawer for Amazon FireTV:__

Visit discussion on XDA-Developers: 
 * [[APP] FireStarter v2.0 | Non-Root Home Launcher Replacement / App-Drawer for FireTV](http://forum.xda-developers.com/fire-tv/themes-apps/app-root-home-launcher-replacement-app-t3118135)

### Features:
 
 * Similar to Redth's <a href="https://github.com/Redth/FiredTVLauncher" target="_blank">FiredTVLauncher</a> with __real home-detection__ 
 * __Even double-home-click are captured!!__
 * Completely configurable what happens on startup-, home-button-single-click or home-button-double-click
 * Default: Starts itself on FireTV-Startup.
 * Default: Starts automatically when Home-Button is clicked.
 * Default: Starts amazon home when Home-Button is double-clicked. 
 * You can e.g. start Kodi on double-click and FireStarter on single-click.
 * Lists all user-installed apps including side-loaded apps.
 * Apps can be easily sorted by click-drag-and-drop.
 * Apps can be hidden from app drawer
 * __No root required!__

### Install FireStarter:

 * If you don't know how to sideload/install apps via ADB, read a turoial (e.g. <a href="http://www.howtogeek.com/216386/how-to-sideload-android-apps-onto-your-amazon-fire-tv-and-fire-tv-stick/" target="_blank">this one</a>)
 * <a href="https://github.com/sphinx02/FireStarter/releases" target="_blank">Download latest FireStarter APK</a> and sideload/install with adb: 
 * _adb install -r FireStarter-v2.0.apk_
 * Start FireStarter once with adb (or manual from settings menu): 
 * _adb shell am start -n "de.belu.firestarter/de.belu.firestarter.gui.MainActivity"_
 * ADB-Debugging needs to stay enabled (do not disable ADB-Debugging after installation).
 * Enjoy :)
 
### Changelog:

#### v2.0
 * __Real Home-Button detection__, even double-home-button-clicks are captured
 * Completely new GUI with settings and additional Infos

### ToDo List:
 * Add better install instructions for users that dont know adb..
 * Add possibility to uninstall apps
 * Perhaps add possiblity to install and keep updated some apps via FireStarter (e.g. Kodi, Es File Explorer, ..)

### Screenshots:

![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_03.png "Screenshot of FireStarter")
![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_04.png "Screenshot of FireStarter")
![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_05.png "Screenshot of FireStarter")

### Credentials:

 * [markdown-editor](https://jbt.github.io/markdown-editor/) for markdown creation
 * [FiredTVLauncher](https://github.com/Redth/FiredTVLauncher) for a lot of brilliant ideas
 * [XDA-User g4rb4g3](http://forum.xda-developers.com/showpost.php?p=56319876&postcount=87) for the home-button detection idea
 
 
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KKQ6VU34YGKYS" target="_blank">Buy me a beer if you like it!</a>
