CM Loader
This is a fast, lightweight RCM loader for the Switch.

There are 2 versions available (Linux and Android).

How to Use
Linux
Run the injector as root:

bash

sudo ./injector payload.bin
(Your Switch needs to be in RCM mode for this, you already know).

Android
Install the app. Same deal, put your Switch in RCM mode.
A popup will show on your phone asking to allow USB permissions. (You might have to reopen the app after this step).
Click on the select button to choose your payload.
Note: Every time you enter RCM mode and your phone is connected via USB-C, the app will automatically open. You might have to reselect the payload again.

How to Compile
Linux
bash

gcc main.c -lusb-1.0 -o injector
Android
Open the android/ folder in Android Studio.
Wait for sync.
Build.
