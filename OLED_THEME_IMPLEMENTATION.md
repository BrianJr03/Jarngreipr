# OLED Black & White Theme Implementation

## Overview

This document describes the implementation of an OLED black and white theme system that is
toggleable throughout the app. The system provides a pure black background (#000000) for OLED
displays while maintaining the existing color theme functionality.

## Key Features

- **Toggleable OLED Mode**: Users can turn OLED mode on/off from Settings
- **Pure Black Backgrounds**: Uses #000000 for optimal OLED power savings
- **White & Gray Accents**: Clean, high-contrast UI elements
- **Backwards Compatible**: Existing themes and colors work normally when OLED mode is off
- **Persistent State**: OLED mode preference is saved across app restarts

## Architecture

### 1. Core Components

#### OledModeManager (`app/src/main/java/jr/brian/home/ui/theme/OledModeManager.kt`)

Manages the OLED mode state with SharedPreferences persistence.

```kotlin
class OledModeManager(context: Context) {
    var isOledModeEnabled: Boolean // Observable state
    fun setOledMode(enabled: Boolean)
    fun toggleOledMode()
}
```

#### LocalOledModeManager (`app/src/main/java/jr/brian/home/ui/theme/LocalOledModeManager.kt`)

Composition local for accessing OledModeManager throughout the app.

#### ThemeBlack Color (`app/src/main/java/jr/brian/home/ui/theme/Color.kt`)

- `ThemeBlack = Color(0xFF000000)` - Pure black for OLED displays

### 2. OLED-Aware Composable Functions

Located in `Color.kt`, these functions automatically return the appropriate color based on OLED
mode:

```kotlin
@Composable fun oledBackgroundColor(): Color
@Composable fun oledCardColor(): Color  
@Composable fun oledCardLightColor(): Color

// Convenient property accessors
val OledBackgroundColor @Composable get() = oledBackgroundColor()
val OledCardColor @Composable get() = oledCardColor()
val OledCardLightColor @Composable get() = oledCardLightColor()
```

**Behavior:**

- When OLED mode is **ON**: Returns `ThemeBlack` (#000000)
- When OLED mode is **OFF**: Returns original colors (`AppBackgroundDark`, `AppCardDark`,
  `AppCardLight`)

### 3. Updated CardGradient Function

The `cardGradient()` function in `app/src/main/java/jr/brian/home/ui/colors/CardGradient.kt` now
adapts to OLED mode:

**OLED Mode ON:**

- Unfocused: Pure black gradient
- Focused: Subtle theme color overlay (0.2/0.1 alpha)

**OLED Mode OFF:**

- Original gradient behavior with full theme colors

### 4. OLED Black & White Theme

Added to `ColorTheme.kt`:

```kotlin
val OLED_BLACK_WHITE = ColorTheme(
    id = "oled_black_white",
    name = "OLED Black & White",
    primaryColor = Color(0xFFFFFFFF),      // Pure white
    secondaryColor = Color(0xFFE0E0E0),    // Light gray
    lightTextColor = Color(0xFFFFFFFF)     // Pure white
)
```

This theme works especially well with OLED mode enabled for a true monochrome experience.

## How to Use

### For Users

1. Open **Settings** from the home screen
2. Find **"OLED Black Mode"** (second item after Color Theme)
3. Tap to toggle between ON and OFF
4. The UI instantly updates to pure black backgrounds
5. Can be combined with any color theme (especially OLED Black & White theme)

### For Developers

#### Replacing Background Colors in Composables

**OLD (Direct color usage):**

```kotlin
Scaffold(
    containerColor = AppBackgroundDark
)

Box(
    modifier = Modifier.background(color = AppCardDark)
)
```

**NEW (OLED-aware):**

```kotlin
Scaffold(
    containerColor = OledBackgroundColor
)

Box(
    modifier = Modifier.background(color = OledCardColor)
)
```

#### Replacing Card Gradients

**OLD:**

```kotlin
val gradient = Brush.linearGradient(
    colors = listOf(AppCardLight, AppCardDark)
)
```

**NEW:**

```kotlin
val gradient = Brush.linearGradient(
    colors = listOf(OledCardLightColor, OledCardColor)
)
```

Or use the centralized `cardGradient()` function:

```kotlin
val gradient = cardGradient(isFocused = false)
```

## Files Modified

### Core Theme Files

- `app/src/main/java/jr/brian/home/ui/theme/Color.kt` - Added OLED composables
- `app/src/main/java/jr/brian/home/ui/theme/ColorTheme.kt` - Added OLED_BLACK_WHITE theme
- `app/src/main/java/jr/brian/home/ui/theme/OledModeManager.kt` - NEW
- `app/src/main/java/jr/brian/home/ui/theme/LocalOledModeManager.kt` - NEW
- `app/src/main/java/jr/brian/home/ui/theme/Theme.kt` - Provider setup
- `app/src/main/java/jr/brian/home/ui/colors/CardGradient.kt` - OLED-aware gradients

### Screen Files

- `app/src/main/java/jr/brian/home/ui/screens/SettingsScreen.kt` - OLED toggle + OLED colors
- `app/src/main/java/jr/brian/home/ui/screens/FAQScreen.kt` - OLED colors
- `app/src/main/java/jr/brian/home/ui/screens/AppDrawerScreen.kt` - (Already uses composables)

### Component Files

All updated to use OLED-aware colors:

- `AppVisibilityDialog.kt`
- `AppOptionsMenu.kt`
- `DrawerOptionsDialog.kt`
- `OnScreenKeyboard.kt`
- `WallpaperOptionButton.kt`
- `AddToWidgetPageDialog.kt`
- `WidgetPageAppSelectionDialog.kt`
- `AppOverlay.kt`

### Resources

- `app/src/main/res/values/strings.xml` - Added OLED mode strings

## Benefits

### For Users

1. **Battery Savings**: Pure black (#000000) pixels are completely off on OLED displays
2. **Reduced Eye Strain**: Lower brightness in dark environments
3. **Aesthetic Appeal**: Clean, modern monochrome look
4. **Flexibility**: Can toggle on/off based on preference or environment

### For Developers

1. **Easy to Extend**: Simply use OLED-aware composables instead of direct colors
2. **Centralized Logic**: All OLED behavior in one place (OledModeManager)
3. **Type-Safe**: Compose-based implementation with proper typing
4. **Backwards Compatible**: Original colors still work, no breaking changes

## Testing Recommendations

1. **Toggle Testing**: Verify OLED mode toggles correctly in Settings
2. **Persistence Testing**: Close and reopen app to verify state is saved
3. **Visual Testing**: Check all screens in both OLED and normal modes
4. **Theme Combinations**: Test OLED mode with each color theme
5. **OLED Display Testing**: View on actual OLED device to verify true black

## Future Enhancements

Potential improvements:

1. Auto-enable OLED mode based on system dark mode
2. Schedule OLED mode for specific times
3. Different OLED intensity levels (pure black, near-black)
4. OLED-optimized animations
5. Custom OLED theme colors beyond black & white

## Technical Notes

- OLED mode state uses `mutableStateOf` for Compose reactivity
- SharedPreferences key: `"oled_mode_enabled"` in `"gaming_launcher_prefs"`
- Default state: OLED mode is OFF
- The OLED manager is provided at the root `LauncherTheme` composable
- All OLED-aware functions are `@Composable` to access LocalOledModeManager

## Migration Guide

If you have custom components using direct colors, migrate them:

1. Import OLED-aware colors:
   ```kotlin
   import jr.brian.home.ui.theme.OledBackgroundColor
   import jr.brian.home.ui.theme.OledCardColor
   import jr.brian.home.ui.theme.OledCardLightColor
   ```

2. Replace direct color usage:
   ```kotlin
   // Before
   .background(color = AppCardDark)
   
   // After
   .background(color = OledCardColor)
   ```

3. For gradients, use composables or `cardGradient()`:
   ```kotlin
   // Before
   Brush.linearGradient(colors = listOf(AppCardLight, AppCardDark))
   
   // After
   Brush.linearGradient(colors = listOf(OledCardLightColor, OledCardColor))
   ```

That's it! The OLED mode will automatically apply to your component.
