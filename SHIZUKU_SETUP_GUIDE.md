# Shizuku Setup Guide for Control Pad Input Injection

This guide explains how to set up Shizuku to enable the Control Pad feature in Jarngreipr to inject real gamepad button presses.

## What is Shizuku?

Shizuku is a tool that allows apps to use system-level APIs (like input injection) without requiring root access. It works by running a service with elevated (shell/ADB) permissions that your app can communicate with.

## Prerequisites

- Android device running Android 11 or higher (recommended) or Android 6+ with ADB access
- A computer with ADB installed (for initial setup)
- USB cable to connect your device to the computer

---

## Step 1: Install Shizuku App

### Option A: Google Play Store
1. Open Google Play Store on your device
2. Search for "Shizuku"
3. Install the app by Rikka (developer: rikka.app)

### Option B: Direct Download
1. Visit [Shizuku GitHub Releases](https://github.com/RikkaApps/Shizuku/releases)
2. Download the latest APK
3. Install it on your device

---

## Step 2: Start Shizuku Service

There are multiple ways to start Shizuku. Choose the one that works best for your situation:

### Method 1: Wireless Debugging (Android 11+) - Recommended

This method allows Shizuku to persist across reboots without a computer.

1. **Enable Developer Options**
   - Go to `Settings > About Phone`
   - Tap `Build Number` 7 times
   - Enter your PIN/password if prompted

2. **Enable Wireless Debugging**
   - Go to `Settings > Developer Options`
   - Scroll down to `Wireless debugging` and enable it
   - Tap on `Wireless debugging` to enter the settings

3. **Start Shizuku**
   - Open the Shizuku app
   - Tap `Start via Wireless debugging`
   - Follow the pairing instructions:
     - In Wireless debugging settings, tap `Pair device with pairing code`
     - Enter the pairing code shown in Shizuku
   - Once paired, Shizuku will start automatically

4. **Enable Auto-start (Optional)**
   - In Shizuku app, enable `Start on boot` if available

### Method 2: ADB via Computer

Use this method if Wireless Debugging is not available on your device.

1. **Enable USB Debugging**
   - Go to `Settings > Developer Options`
   - Enable `USB debugging`

2. **Connect to Computer**
   - Connect your device via USB
   - Allow USB debugging when prompted on your device

3. **Run ADB Command**
   Open a terminal/command prompt on your computer and run:

   ```bash
   adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
   ```

   Or if that doesn't work:

   ```bash
   adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
   ```

4. **Verify Shizuku is Running**
   - Open the Shizuku app
   - It should show "Shizuku is running"

### Method 3: Root (if available)

If your device is rooted:

1. Open the Shizuku app
2. Tap `Start via root`
3. Grant root permission when prompted

---

## Step 3: Grant Permission to Jarngreipr

1. Open the **Jarngreipr** app
2. Navigate to **Control Pad** (via Settings or Back Button Shortcut)
3. You should see a status indicator at the top:
   - **Red** (Shizuku not running): Follow Step 2 to start Shizuku
   - **Orange** (Permission required): Tap the indicator to grant permission
   - **Yellow** (Connecting): Wait a moment for the service to connect
   - **Green** (Ready): Control Pad is ready to use!

4. When prompted by Shizuku, tap **Allow** to grant permission

---

## Step 4: Using the Control Pad

1. **Map Buttons**
   - Tap the `Edit` button at the bottom
   - Tap any of the 6 control pad slots
   - Select a physical button to map (A, B, X, Y, L1, L2, R1, R2, L3, R3, Start, Select)
   - Tap `Done` when finished

2. **Use Mapped Buttons**
   - When not in edit mode, tapping a control pad slot will inject the mapped gamepad button press
   - The button press will be recognized by games and apps as a real gamepad input

3. **Reset Mappings**
   - Enter edit mode by tapping `Edit`
   - Tap `Reset` to clear all mappings

---

## Troubleshooting

### Shizuku Status is Red (Not Running)

- Open the Shizuku app and check its status
- If Shizuku was started via ADB, you may need to restart it after a device reboot
- For persistent Shizuku, use the Wireless Debugging method on Android 11+

### Permission Not Granting

- Make sure Shizuku is running (check the Shizuku app)
- Try closing and reopening Jarngreipr
- Check if Shizuku shows Jarngreipr in its authorized apps list

### Button Presses Not Working

- Verify the Shizuku status indicator is **Green** (Ready)
- Make sure you've mapped a button to the control pad slot
- Some apps may not respond to injected gamepad inputs (rare)

### Shizuku Stops After Reboot

- **Wireless Debugging method**: Should auto-start if configured
- **ADB method**: You need to run the ADB command again after each reboot
- **Root method**: Should persist automatically

---

## Security Notes

- Shizuku only grants permissions to apps you explicitly authorize
- You can revoke Jarngreipr's permission anytime from the Shizuku app
- The input injection service only runs when Jarngreipr requests it

---

## Uninstalling

To completely remove Shizuku integration:

1. Open the Shizuku app
2. Go to `Authorized Applications`
3. Remove Jarngreipr from the list
4. Optionally, stop Shizuku service and uninstall the app

---

## Additional Resources

- [Shizuku Official Website](https://shizuku.rikka.app/)
- [Shizuku GitHub Repository](https://github.com/RikkaApps/Shizuku)
- [Shizuku Documentation](https://shizuku.rikka.app/guide/setup/)

---

## Button Mapping Reference

| Button | Android KeyCode | Description |
|--------|-----------------|-------------|
| A | KEYCODE_BUTTON_A | Primary action button |
| B | KEYCODE_BUTTON_B | Secondary/back button |
| X | KEYCODE_BUTTON_X | Tertiary action button |
| Y | KEYCODE_BUTTON_Y | Quaternary action button |
| L1 | KEYCODE_BUTTON_L1 | Left shoulder button |
| L2 | KEYCODE_BUTTON_L2 | Left trigger |
| R1 | KEYCODE_BUTTON_R1 | Right shoulder button |
| R2 | KEYCODE_BUTTON_R2 | Right trigger |
| L3 | KEYCODE_BUTTON_THUMBL | Left stick press |
| R3 | KEYCODE_BUTTON_THUMBR | Right stick press |
| Start | KEYCODE_BUTTON_START | Start/menu button |
| Select | KEYCODE_BUTTON_SELECT | Select/back button |
