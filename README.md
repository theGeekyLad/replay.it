# Replay.it

Here's an off-the-charts Android automation project that sits right there, watches you do something on your phone, something as simple as sending that WhatsApp morning wish to person X and literally _"replays"_ it just the same way as you'd done it, but at a later point in time as scheduled. 

_"Hang on hang on ... what really?"_ - an abstrusely bemused spectator.

If that seemed to bounce for a bit, check out [this YouTube video](https://youtu.be/mqhi7q1Otz0)! It covers a nice little automation scenario of pinging your friend _"hey"_ on WhatsApp and lets you have that replayed all over again at a desired time.

## Needs _(10-15m)_

### Java

Without Java you're not going to be able to run this project. How should you know if your system does have Java? Open a terminal session / Powershell and `java -version`. If that fails stating that the command wasn't found, get the latest Java SE JDK from [Oracle's official download page](https://www.oracle.com/in/java/technologies/javase-downloads.html).

Follow your platform specific installation instructions for getting it setup (barely takes while).

### USB Debugging

Without this enabled on your Android device, you'll not see anything happen! Now getting to this option differs with the device in question but most generally, here's what you need to do on your phone:
<br><br>
`Settings -> About phone -> Rapid taps on 'Build number' (enables 'Developer options') -> Go back a step -> System -> Developer options -> Toggle to enable -> Scroll down to 'USB debugging' -> Toggle to enable`

_**Note:** When the device is connected to the PC and you run this program for the very first time, you'd see a pop-up on your Android device for ADB authentication. Check the box that says "Allow always" and grant access._

## Get it! Run it! _(1-2m)_

### Linux + MacOS

- Head to the [releases](https://github.com/theGeekyLad/replay.it/releases) section and download the latest release
- Extract the ZIP file in downloads directory to `Downloads/replay.it`
- Connect your Android device to this PC over a data cable
- Fire up a terminal _(usually Ctrl+T)_
- `cd ~/Downloads/replay.it`
- `./adb-automate.sh`

### Windows

- Head to the [releases](https://github.com/theGeekyLad/replay.it/releases) section and download the latest release
- Extract the ZIP file in downloads folder to **replay.it** and open that extracted folder
- Connect your Android device to this PC over a data cable
- Double-click `adb-automate.bat`

## Bugs

It's important to note that a _"swipe"_ gesture is complete only after you perform a _"tap"_ anywhere on the screen once you've finished swiping.

---

Made with :heart: by `theGeekyLad`