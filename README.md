# FireStarter

FireStarter is a Non-Root Launcher Replacement / App-Drawer for Amazon FireTV:

 * Similar to Redth's <a href="https://github.com/Redth/FiredTVLauncher" target="_blank">FiredTVLauncher</a> with better home-detection and auto start on IkonoTV.
 * Starts itself on FireTV-Startup.
 * Starts automatically when Home-Button is pressed.
 * Starts itself when the IkonoTV app is started.
 * IkonoTV icon is automatically replaced (only in current apps).
 * Lists all user-installed apps including side-loaded apps.
 * Apps can be easily sorted by click-drag-and-drop.
 * **No root required!**

Install FireStarter:

 * Install IkonoTV from offical FireTV app store
 * If you don't know how to sideload/install apps via ADB, read a turoial (e.g. <a href="http://www.howtogeek.com/216386/how-to-sideload-android-apps-onto-your-amazon-fire-tv-and-fire-tv-stick/" target="_blank">this one</a>)
 * <a href="https://github.com/sphinx02/FireStarter/releases" target="_blank">Download latest FireStarter APK</a> and sideload/install with adb: 
 * _adb install FireStarter_v1.0.apk_
 * Start FireStarter once with adb (or manual from settings menu): 
 * _adb shell am start -n "de.belu.firestarter/de.belu.firestarter.gui.MainActivity"_
 * IkonoTV icon is replaced at next startup (hold select + play button for a few seconds)
 * Enjoy :)

ToDo List:

 * Add better install instructions for users that dont know adb..
 * Select on which app FireStarter starts itself (currently fixed to IkonoTV)
 * Add possibility to uninstall apps
 * Perhaps add possiblity to install and keep updated some apps via FireStarter (e.g. Kodi, Es File Explorer, ..)

Screenshots:

![Screenshot of FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_01.png "Screenshot of FireStarter")
![Screenshot of Amazon Home with FireStarter](https://raw.githubusercontent.com/sphinx02/FireStarter/master/firestarter_screenshot_02.png "Screenshot of Amazon Home with FireStarter")

### Credentials:

 * [markdown-editor](https://jbt.github.io/markdown-editor/) for markdown creation
 * [FiredTVLauncher](https://github.com/Redth/FiredTVLauncher) for a lot of brilliant ideas
 
 
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KKQ6VU34YGKYS" target="_blank">Buy me a beer if you like it!</a>
