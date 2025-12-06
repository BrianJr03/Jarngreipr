# Icon Pack Support

Járngreipr now supports custom icon packs, allowing you to personalize the look of your app icons!

## Overview

Icon pack support enables you to:

- Use third-party icon packs from the Play Store or other sources
- Apply consistent theming across all your app icons
- Switch between different icon packs easily
- Fall back to default icons when a custom icon isn't available

## How It Works

### Architecture

The icon pack system consists of several components:

1. **IconPackManager**: Handles loading and caching icon packs
2. **IconPackPreferences**: Stores the user's selected icon pack
3. **IconPackSelectorItem**: UI component for selecting icon packs in settings
4. **Icon Pack Loading**: Parses `appfilter.xml` from icon packs to map apps to custom icons

### Icon Pack Detection

The launcher detects icon packs by checking for common metadata keys:

- `com.novalauncher.theme`
- `com.teslacoilsw.launcher.iconpack`
- `com.anddoes.launcher.theme`
- `ADW.THEMES`
- `org.adw.launcher.icons.ACTION_PICK_ICON`

## Installing Icon Packs

### Step 1: Install an Icon Pack

Download and install any icon pack that supports the standard Android icon pack format. Popular
options include:

- **Whicons** - Minimalist white icons
- **Lines** - Clean line-based icons
- **Flight Lite** - Colorful flat icons
- **Urmun** - Material Design icons
- **Moonshine** - Detailed icon pack
- **Delta** - Adaptive icon pack
- **Rundo** - Rounded icons
- **Ameixa** - Monochrome icons

You can find these and many more on the Google Play Store.

### Step 2: Select the Icon Pack in Járngreipr

1. Open **Settings** (gear icon in search bar)
2. Scroll to the **Icon Pack** option under the Appearance section
3. Tap to expand the list of installed icon packs
4. Select your preferred icon pack
5. Wait a moment while the icons are loaded
6. Return to the home screen to see your new icons!

### Step 3: Reset to Default Icons

To go back to the original icons:

1. Open **Settings**
2. Tap on **Icon Pack**
3. Select **Default Icons** at the top of the list

## Technical Details

### Icon Pack Format

Icon packs follow the standard Android icon pack specification:

#### appfilter.xml Structure

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item component="ComponentInfo{com.package.name/.ActivityName}" 
          drawable="icon_name" />
    <!-- More mappings -->
</resources>
```

Each `item` maps a component (package name + activity) to a drawable resource name.

### How Icons Are Resolved

1. **Check Cache**: First, check if the icon is already cached in memory
2. **Look Up Component**: Find the app's launch activity component
3. **Query appfilter.xml**: Look up the component in the icon pack's mappings
4. **Load Drawable**: Load the custom drawable from the icon pack
5. **Fallback**: If no custom icon exists, use the default app icon

### Performance Optimizations

- **In-Memory Cache**: Icons are cached after first load to improve performance
- **Lazy Loading**: Icons are only loaded when needed
- **Background Processing**: Icon pack parsing happens on background threads
- **Efficient Storage**: Only stores package name for selected icon pack

## Creating Your Own Icon Pack

If you want to create your own icon pack for use with Járngreipr:

### 1. Create an Android App

Create a new Android project with:

- `minSdk = 21` or higher
- No activities (or a simple settings activity)

### 2. Add Metadata

Add to your `AndroidManifest.xml`:

```xml
<application>
    <meta-data
        android:name="com.novalauncher.theme"
        android:value="true" />
</application>
```

### 3. Create appfilter.xml

Create `res/xml/appfilter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Example: Chrome -->
    <item 
        component="ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}" 
        drawable="chrome" />
    
    <!-- Example: Gmail -->
    <item 
        component="ComponentInfo{com.google.android.gm/.ConversationListActivityGmail}" 
        drawable="gmail" />
    
    <!-- Add more app mappings -->
</resources>
```

### 4. Add Drawable Resources

Add your custom icons to `res/drawable/`:

- `chrome.png` (or `.xml` for vector drawables)
- `gmail.png`
- etc.

Recommended sizes:

- **192x192** for raster icons
- **Vector drawables** preferred for best quality at any size

### 5. Finding Component Names

To find an app's component name:

```bash
# List all launchable activities
adb shell pm list packages -f | grep <package-name>
adb shell dumpsys package <package-name> | grep "android.intent.action.MAIN" -A 5
```

Or use tools like:

- **Icon Request App** - Generates component info
- **Activity Launcher** - Shows all activities
- **Logcat** when launching apps

### 6. Build and Install

Build your icon pack APK and install it on your device. It will automatically appear in Járngreipr's
icon pack selector!

## Troubleshooting

### Icons Not Changing

If icons aren't changing after selecting an icon pack:

1. **Check Icon Pack Compatibility**: Ensure the icon pack includes an `appfilter.xml` file
2. **Reload Apps**: Go back to the launcher and pull down to refresh
3. **Check Package Name**: Verify the icon pack installed correctly
4. **Clear Cache**: Try switching to default icons and back

### Only Some Icons Change

This is normal! Icon packs don't always include every app. Apps without custom icons will show their
default icons.

### Icon Pack Not Detected

If an icon pack doesn't appear in the list:

1. **Verify Metadata**: The icon pack must have proper metadata tags
2. **Reinstall**: Try uninstalling and reinstalling the icon pack
3. **Check Permissions**: Ensure Járngreipr has necessary permissions

### Performance Issues

If you experience lag after applying an icon pack:

1. **Large Icon Pack**: Some packs with thousands of icons may take time to load
2. **Wait for Cache**: First load is slower; subsequent loads will be faster
3. **Restart Launcher**: Close and reopen Járngreipr

## Code Integration

### For Developers

If you're forking or modifying Járngreipr, here's how the icon pack system integrates:

#### Key Files

- `model/IconPack.kt` - Data models
- `data/IconPackManager.kt` - Icon pack loading and caching
- `data/IconPackPreferences.kt` - Settings storage
- `ui/components/settings/IconPackSelectorItem.kt` - UI component
- `viewmodels/MainViewModel.kt` - Integration point

#### Usage in Code

```kotlin
// Get icon with fallback
val icon = iconPackManager.getIconForPackage(
    packageName = "com.example.app",
    defaultIcon = originalIcon
)

// Change icon pack
iconPackManager.setSelectedIconPack("com.iconpack.example")

// Reload apps to apply changes
homeViewModel.loadAllApps(context)
```

## Future Enhancements

Potential future improvements to icon pack support:

- [ ] Icon pack preview in settings
- [ ] Multiple icon pack mixing
- [ ] Custom icon picker for individual apps
- [ ] Icon pack search functionality
- [ ] Automatic icon updates when pack updates
- [ ] Icon pack themes (light/dark variants)
- [ ] Calendar icon support with date updates
- [ ] Clock icon support with time updates

## Resources

### Icon Pack Standards

- [Nova Launcher Icon Pack Specification](https://help.teslacoilsw.com/icon-pack-overview)
- [ADW Launcher Theme Guide](https://www.adw.org/themes)
- [Icon Pack Template](https://github.com/jahirfiquitiva/IconPackStudioTemplate)

### Tools

- [Icon Pack Studio](https://github.com/jahirfiquitiva/IconPackStudioTemplate) - Create icon packs
  easily
- [Blueprint](https://github.com/jahirfiquitiva/Blueprint) - Icon pack dashboard template
- [Icon Request](https://github.com/jahirfiquitiva/IconRequest) - Icon request system

## Contributing

If you'd like to improve icon pack support:

1. Test with various icon packs
2. Report compatibility issues
3. Suggest improvements
4. Submit pull requests

## License

Icon pack support code is part of Járngreipr and follows the same license.

---

**Note**: Icon packs are third-party applications. Járngreipr is not responsible for the content or
quality of icon packs. Always download icon packs from trusted sources.
