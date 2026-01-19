# GameBiller TV Smart Lock - Deployment Guide

This guide explains how to install and configure the GameBiller TV Smart Lock app on Android Standard (Android TV) screens for Gaming Cafes.

## 🎯 Objective
To effectively "lock" the TV screen (show a black overlay) when a customer's session ends, forcing them to pay to continue playing.

## ✅ Prerequisites
1.  **Android TV:** A TV running Android TV OS (Sony, TCL, Mi TV, OnePlus, etc.) or an Android TV Box.
2.  **USB Drive:** To transfer the installation file (APK).
3.  **Mouse (Optional but Recommended):** Some TV remotes are clunky; a USB mouse makes setup faster.
4.  **Backend Connection:** Ensure the TV is on the same network as the GameBiller server (or the server is accessible via internet).

---

## 🛠️ Step 1: Installation

### Method 1: Easy Install (Recommended - No USB needed)
This method uses the "Downloader" app found on all Android TVs.

1.  **Open Google Play Store** on the TV.
2.  Search for and install **"Downloader by AFTVnews"**.
3.  Open **Downloader**.
4.  In the URL bar, type your direct download code/URL:
    *   **Code:** `[YOUR_SHORT_CODE]` (You must generate this, see below)
    *   *Or URL:* `https://gamebiller.com/tv.apk`
5.  Click **Go**. The file will download.
6.  Click **Install**. (You may need to toggle "Allow from this source" in settings when prompted).

> **For Admin/Developer:** To get a numeric code, upload your APK to your website or Google Drive (Direct Link), then paste that URL at [https://aftv.news](https://aftv.news) to generate a 5-digit code.

### Method 2: USB Sideloading (Alternative)
Use this if the TV is not connected to the internet securely or Downloader fails.

1.  **Prepare the APK:**
    *   Copy the `app-release.apk` to a USB drive.
    *   Insert the USB drive into the TV.

2.  **Install a File Manager:**
    *   On the TV, open **Google Play Store**.
    *   Install **"File Commander"** (or similar).

3.  **Install:**
    *   Open **File Commander**.
    *   Select the USB Drive -> Click the APK file -> **Install**.

---

## 🔐 Step 2: Kiosk Mode Setup (CRITICAL)

To prevent customers from bypassing the lock screen, you must set this app as the "Home" app.

1.  **Press the "Home" Button** on the TV remote.
2.  A popup will appear: **"Select a Home app"**.
3.  Choose **GameBiller TV**.
4.  Select **"ALWAYS"** (Do not select "Just Once").

> **Note:** Now, whenever the TV turns on or someone presses "Home", it will go to the Lock Screen instead of the default Android menu.

---

## 🛡️ Step 3: Grant Permissions

For the lock screen to cover apps like YouTube or HDMI inputs perfectly, you must grant special permissions.

1.  Go to TV **Settings**.
2.  Navigate to **Apps** -> **Special App Access** -> **Display over other apps** (sometimes called "Draw over other apps").
3.  Find **GameBiller TV** in the list.
4.  Toggle the switch to **ON** (Enabled).

---

## 🔗 Step 4: Pairing the Device

1.  Launch the app (Press Home).
2.  You will see the **"Device Pairing"** screen with a code input box.
3.  **Using the TV Remote:**
    *   Enter the unique **Station Code** generated from your GameBiller Admin Panel (under "Stations" -> "Add Station").
    *   Example Code: `ST91-E421`
4.  Click **"Pair Device"**.
5.  **Success:**
    *   If the session is STOPPED, the screen will turn **Black** (Locked).
    *   If the session is RUNNING, the screen will allow you to view HDMI/Inputs.

---

## 🎮 Step 5: Daily Usage

*   **When Customer Arrives:**
    *   Go to Admin Panel -> Start Session.
    *   TV Unlocks automatically within 10-12 seconds.
    *   Customer plays game (HDMI 1).

*   **When Time Expires:**
    *   Admin Panel session stops (or timer ends).
    *   TV Locks automatically (Black Screen with "Session Ended" message).
    *   Customer cannot continue playing.

---

## ❓ Troubleshooting

**Q: How do I exit to Android Settings if the screen is locked?**
A: Since the app is the launcher, it's tricky.
*   **Method 1:** Uninstall the app via ADB (`adb uninstall com.gamebiller.tvlock`).
*   **Method 2:** Reboot the TV into **Safe Mode** (usually hold Power + Vol Down during boot).
*   **Method 3:** Go to Admin Panel, **Start a Session** (to unlock it), then use the remote to access Settings quickly.

**Q: The TV is locked but I can still hear the game sound.**
A: The app puts a black *overlay* on the screen. It cannot mute HDMI audio on all TV models due to hardware limitations. The visual block is usually sufficient to stop gameplay.
