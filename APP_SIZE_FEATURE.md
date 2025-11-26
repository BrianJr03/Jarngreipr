# App Icon Size Adjustment Feature

## Overview

Added a new settings option that allows users to adjust the size of app icons throughout the
launcher with a real-time preview.

## Features

- **Adjustable Range**: 48dp to 96dp (default: 64dp)
- **Real-time Preview**: Shows a sample app icon at the current size
- **Smooth Slider**: Material 3 slider with 13 size steps
- **Persistent**: Size preference saved across app restarts
- **Live Updates**: Changes apply immediately to all app icons

## Implementation Details

### 1. AppSizeManager (`app/src/main/java/jr/brian/home/data/AppSizeManager.kt`)

Manages app icon size state with SharedPreferences persistence.

```kotlin
class AppSizeManager(context: Context) {
    companion object {
        const val MIN_SIZE = 48f
        const val MAX_SIZE = 96f
        const val DEFAULT_SIZE = 64f
    }
    
    var appIconSize: Float // Observable state
    fun setAppSize(size: Float) // Updates size with validation
}
```

### 2. LocalAppSizeManager (`app/src/main/java/jr/brian/home/ui/theme/LocalAppSizeManager.kt`)

Composition local for accessing AppSizeManager throughout the app.

### 3. Settings UI (`SettingsScreen.kt`)

New **AppSizeSelectorItem** with:

- **Collapsed State**: Shows icon and description
- **Expanded State**: Shows:
    - Preview box with sample app icon at current size
    - Icon label and size indicator
    - Slider for size adjustment
    - Min/Max size labels (48dp - 96dp)
    - Current size display
    - Done button

### 4. AppGridItem Integration (`AppGridItem.kt`)

Updated to use dynamic icon size from AppSizeManager instead of hardcoded 64dp.

## User Experience

### Settings Location

The app size setting appears in Settings as the 3rd item:

1. Color Theme
2. OLED Black Mode
3. **App Icon Size** ← NEW
4. Wallpaper
5. Grid Layout
6. ...etc

### How to Use

1. Open **Settings**
2. Tap **"App Icon Size"**
3. See the preview with current size
4. Drag the slider to adjust size (48dp - 96dp)
5. Preview updates in real-time
6. Tap **"Done"** to close
7. All app icons throughout the launcher instantly resize

### Preview Section

The preview shows:

- Sample app icon (first app from the list)
- App name below icon
- Current size in dp displayed in theme color
- Clean black/OLED-aware background
- Border for visibility

### Slider Controls

- **Track**: Shows progress from min to max
- **Thumb**: Draggable indicator with theme color
- **Steps**: 13 discrete steps for smooth control
- **Labels**: Min (48dp) and Max (96dp) at ends
- **Current Value**: Displayed above slider in theme color

## Technical Details

### Storage

- **SharedPreferences key**: `"app_icon_size"`
- **File**: `"gaming_launcher_prefs"`
- **Type**: Float
- **Default**: 64f

### Size Range

- **Minimum**: 48dp (compact, fits more on screen)
- **Maximum**: 96dp (large, easy to see)
- **Steps**: 12 increments of 4dp each
- **Default**: 64dp (original size)

### Integration Points

- `LauncherTheme` - Provider setup
- `AppGridItem` - Applies size to app icons
- `SettingsScreen` - UI for adjustment
- All screens with app grids automatically update

## Benefits

### For Users

1. **Customization**: Adjust icons to personal preference
2. **Accessibility**: Larger icons for better visibility
3. **Screen Space**: Smaller icons to fit more apps
4. **Real-time Feedback**: See changes immediately
5. **Easy Reset**: Slider shows default position

### For Accessibility

- Users with vision impairments can increase icon size up to 96dp
- Those wanting more density can decrease to 48dp
- Smooth adjustment without jumping between presets

## Files Modified

### New Files

- `app/src/main/java/jr/brian/home/data/AppSizeManager.kt`
- `app/src/main/java/jr/brian/home/ui/theme/LocalAppSizeManager.kt`
- `APP_SIZE_FEATURE.md` (this file)

### Modified Files

- `app/src/main/java/jr/brian/home/ui/theme/Theme.kt` - Added provider
- `app/src/main/java/jr/brian/home/ui/screens/SettingsScreen.kt` - Added UI
- `app/src/main/java/jr/brian/home/ui/components/AppGridItem.kt` - Dynamic sizing
- `app/src/main/res/values/strings.xml` - Added strings

## String Resources

```xml
<string name="settings_app_size_title">App Icon Size</string>
<string name="settings_app_size_description">Adjust the size of app icons</string>
```

## Future Enhancements

Possible improvements:

1. Different sizes for different screens (home vs drawer)
2. Preset buttons (Small, Medium, Large)
3. Text size adjustment to match icon size
4. Grid spacing adjustment based on icon size
5. Import/Export size presets
6. Per-app size customization

## Code Example

### Using in a Composable

```kotlin
@Composable
fun MyComponent() {
    val appSizeManager = LocalAppSizeManager.current
    val iconSize = appSizeManager.appIconSize
    
    Image(
        painter = painter,
        modifier = Modifier.size(iconSize.dp)
    )
}
```

### Programmatically Set Size

```kotlin
appSizeManager.setAppSize(72f) // Sets to 72dp
```

## Design Decisions

### Why 48-96dp Range?

- **48dp**: Material Design minimum touch target
- **96dp**: Stays reasonable on most screen sizes
- **64dp default**: Matches original design

### Why Steps?

- Prevents awkward fractional sizes
- Makes slider easier to control
- Provides predictable increments
- 4dp steps feel natural

### Why Real-time Preview?

- Immediate visual feedback
- No need to navigate away to see effect
- Shows exact appearance
- Reduces trial-and-error

## Testing Recommendations

1. **Size Range**: Test all sizes from 48dp to 96dp
2. **Persistence**: Close and reopen app, verify size retained
3. **Multiple Apps**: Check various app icon types
4. **Grid Layout**: Verify icons don't overlap at large sizes
5. **OLED Mode**: Test preview in OLED mode
6. **Theme Colors**: Test slider appearance with different themes
7. **Navigation**: Ensure Done button properly closes expanded view

## Performance Notes

- Size changes are instant (no animation needed)
- Minimal overhead (single float in memory)
- SharedPreferences write is async
- Compose recomposition is efficient
- Preview icon loads via Coil (cached)

The feature is fully implemented and ready to use!
