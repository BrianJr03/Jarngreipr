** 3.1.1 Hotfix**
- "System Wallpaper" in Appearance now actually shows the Android wallpaper again instead of a black screen

**New Jarngreipr Frontend Features**
- Edit Info now has a Marquee slot so you can set a custom marquee image per game

**Canvas**
- New `Last Played Widget` shows your recently played ROMs (via Jarngreipr FE) with thumbnails and a Random button that picks one for you
- Long-press the widget to tweak how many rows show, toggle the Random button, pick a scale, and set a background color
- Long-press any row to launch or remove it from the list

**Launcher Fixes**
- Opening the launcher no longer pauses audio from other apps when Jarngreipr's background music is muted or set to 0% volume
- Mute toggle for background music now persists across app restarts
- The bottom screen no longer falls back to the system launcher after a ROM search opens results on the top screen
- Backup and Restore now preserves your dock and app drawer button colors
- Backup no longer exports the dock color as 0, so Restore no longer turns the dock transparent
- Resuming one display no longer drags the other back to the front, each screen stays where you left it (new "Resume Companion Display" toggle in Settings -> Extras restores the old behavior if you want it)

**Jarngreipr Frontend Fixes**
- Game tiles now refresh automatically after scraping, no need to tap Refresh Library

**To try the Jarngreipr Frontend**
- `Settings` -> `Extras` -> `Enable Frontend`
- Still a WIP but functional

3.1.1
