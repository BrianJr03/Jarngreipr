### Quality of Life Improvements

**Display & Visual Fixes**
- Fixed background dimming issues preventing ES-DE Display Settings from locking at 100%
- Fixed header/app dock visual bugs
- Improved horizontal scrolling on homepage
- Added separate blur level controls for system and game backgrounds
- Added separate dimming level controls for system and game backgrounds
- Added background scale mode options (Crop/Fit) for system and game images
- Added ES-DE Display Settings section in main settings screen

**App Search Enhancements**
- Added horizontal QWERTY keyboard layout in app search
- Enhanced search functionality with new layout manager
- Added keyboard layout toggle with onboarding

**Music & Audio Improvements**
- Fixed system music playing during game browsing after game exit ([#112](https://github.com/BrianJr03/Jarngreipr/issues/112))
- Added music volume control (0-100%)
- Improved music lifecycle management with activity visibility tracking
- Enhanced music cross-fade behavior between systems
- Fixed music state restoration after game exit
- Better handling of music during video playback
- Support for normalized system names in music folders
- Added system-specific music toggle (play unique music per system vs continuous queue)
- Added music loop toggle (loop tracks when playlist ends)
- Improved audio focus handling with request/abandon methods

**Video Playback**
- Fixed video playback issues
- Added new video player activity with scale mode selector (Fill Screen/Fit Video)
- Added proper video lifecycle management
- Improved video launch event handling
- Better integration with power states

**Screensaver & Power**
- Prevented powered-off screensaver from affecting settings screen
- Improved power section settings
- Added power screen with music volume control
- Enhanced screensaver UI visibility toggles
- Added volume slider on powered-off screen
- Added double-click to power off screen when game is running with persist enabled

**ES-DE Improvements**
- Addressed photo loading delays when switching between games
- Enhanced ES-DE settings screen with collapsible sections
- Added marquee press shortcut functionality
- Added marquee page visibility and overlay mode controls
- Added separate marquee visibility toggles for system and game
- Improved wallpaper container performance with memory-optimized ImageLoader
- Added custom media path support
- Added single image/logo path options for system and game
- Added empty folder cleanup tool with systeminfo.txt handling
- Fixed issues [#113](https://github.com/BrianJr03/Jarngreipr/issues/113) and [#114](https://github.com/BrianJr03/Jarngreipr/issues/114)
- Added "All" and randomization options for image types
- Added option to exclude effects from home screen
- Added expanded logo alignment options (Top Left, Top Right, Bottom Left, Bottom Right, Free Position)
- Added free-position marquee with drag-to-reposition support
- Added marquee position lock toggle
- Added marquee minimum width control for narrow/portrait images
- Added game description overlay mode per tab
- Added Description image type option
- Added Android Games background scale control

**UI/UX Enhancements**
- Added new dimmed dialog component
- Improved gesture modifiers
- Enhanced settings sections with collapsible UI
- Added icon shape and tab animation toggles
- Improved app drawer and widget layouts
- Added app drawer opacity control
- Added marquee long-press gesture in overlay mode
- Added wallpaper double-click to toggle screensaver UI
- Better pager scroll progress tracking
- Added UI hiding option for game browsing
- Improved settings organization with collapsible sections
- Added App Drawer floating action button (FAB) with customization
- Added FAB color picker
- Added per-tab FAB visibility controls
- Added bottom fling trigger for gestures
- Added widget picker card component
- Improved shortcut selection screen

**Code Architecture**
- Refactored MainActivity with composable helper functions
- Added ESDECleanupManager for folder management
- Added FolderCleanupManager with recursive deletion support
- Added SearchLayoutManager for keyboard preferences
- Improved dependency injection with ESDEImageLoader
- Enhanced state management across components
- Better separation of concerns in UI components
- Added AppDrawerFabManager for FAB state management
- Added TabAnimationManager for tab animation preferences
- Added ShortcutOption model for shortcut handling
- Added SettingsConstants for centralized settings keys
- Added DisplayUtils for display-related utilities
- Updated Proguard rules

**Bug Fixes**
- Fixed MediaPlayer release timing issues
- Fixed notification badge note in README
- Improved error handling in music playback
- Better handling of edge cases in folder cleanup
- Fixed various dialog dimming inconsistencies
- Improved video player lifecycle handling

1.7.3