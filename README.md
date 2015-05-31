FireStarter
=========

[![MPLv2 License](http://img.shields.io/badge/license-MPLv2-blue.svg?style=flat-square)](https://www.mozilla.org/MPL/2.0/)

__FireStarter is a Non-Root Launcher Replacement / App-Drawer for Amazon FireTV:__

Visit discussion on XDA-Developers: 
 * [[APP] FireStarter | Non-Root Home Launcher Replacement / App-Drawer for FireTV](http://forum.xda-developers.com/fire-tv/themes-apps/app-root-home-launcher-replacement-app-t3118135)

### Features:
 
 * Similar to Redth's <a href="https://github.com/Redth/FiredTVLauncher" target="_blank">FiredTVLauncher</a> with __real home-detection__ .
 * __Even double-home-clicks are captured!!__
 * Completely configurable what happens on startup-, home-button-single-click or home-button-double-click.
 * Default: Starts itself on FireTV-Startup.
 * Default: Starts automatically when Home-Button is clicked.
 * Default: Starts amazon home when Home-Button is double-clicked. 
 * You can e.g. start Kodi on double-click and FireStarter on single-click.
 * Lists all user-installed apps including side-loaded apps.
 * Apps can be easily sorted by click-drag-and-drop.
 * Apps can be hidden from app drawer.
 * Automatic updates from app itself.
 * __No root required!__

### Install FireStarter:

 * If you don't know how to sideload/install apps via ADB, read a turoial (e.g. <a href="http://kodi.wiki/view/HOW-TO:Install_Kodi_on_Fire_TV" target="_blank">this one</a>)
 * <a href="https://github.com/sphinx02/FireStarter/releases" target="_blank">Download latest FireStarter APK</a> and sideload/install with adb: 
 * _adb install -r FireStarter-v2.3.apk_
 * Start FireStarter once with adb (or manual from settings menu): 
 * _adb shell am start -n "de.belu.firestarter/de.belu.firestarter.gui.MainActivity"_
 * ADB-Debugging needs to stay enabled (do not disable ADB-Debugging after installation).
 * Enjoy :)
 
### Changelog:

>
#### v2.3
 * Fixed ugly bug in settings (kind of endless loop), which caused huge cpu-load and weird behaviour of settings storage.
 * Fixed bug when sysapp were set visible and apps were reordered, sysapps were not hided again on settings change.
 * Allow greater double-click interval and action delay (up to 1000ms).

>
#### v2.2
 * Added adjustable double-click interval and action-delay
 * Reordered settings
 * When going to app-drawer, first icon is automatically focused (configurable in settings)
 * Update service (able to update itself)
 
>
#### v2.1
 * Fixed problems with FireTV-Stick
 * Added FireStarter version to info-view
 
>
#### v2.0
 * __Real Home-Button detection__, even double-home-button-clicks are captured
 * Completely new GUI with settings and additional Infos

### ToDo List:
 * Add better install instructions for users that dont know adb..
 * Add possibility to uninstall apps
 * Perhaps add possiblity to install and keep updated some apps via FireStarter (e.g. Kodi, Es File Explorer, ..)

### Screenshots:

![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_06.png "Screenshot of FireStarter")
![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_07.png "Screenshot of FireStarter")
![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_08.png "Screenshot of FireStarter")
![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_09.png "Screenshot of FireStarter")

### Why using it and how it works:
 * FireStarter is for all people who dont want to root (and therefore loose warranty) their FireTV's.
 * On the FireTV, Amazon allows no alternative default launchers and in the default launcher of Amazon, no sideloaded (via adb installed apps) are shown. They have to be started via the FireTV settings menu which is really inconvenient.
 * Solutions currently out there are either using root-rights to replace the home launcher or they are polling the top application in the background and then starting other apps if e.g. the Amazon default launcher is detected.
 * FireStarter uses the fact, that every time the home-button is clicked, there is a special output in the adb logcat log. FireStarter starts a local adb logcat session and waites for this output (which is only working as long adb is enabled in FireTV settings). This approach has the advantage, that the top activity dont has to change to detect a home-button click. FireStarter is therefore even able to detect a double-click and starting any actions on home-button single- or double-clicks.
 * Still not solved is the problem, that the default launcher flashes shortly before the right app is started. The default-behaviour of the home-button can still not be disabled.

### Credentials:

 * [markdown-editor](https://jbt.github.io/markdown-editor/) for markdown creation
 * [FiredTVLauncher](https://github.com/Redth/FiredTVLauncher) for a lot of brilliant ideas
 * [XDA-User g4rb4g3](http://forum.xda-developers.com/showpost.php?p=56319876&postcount=87) for the home-button detection idea
 
 
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KKQ6VU34YGKYS" target="_blank">Buy me a beer if you like it!</a>
