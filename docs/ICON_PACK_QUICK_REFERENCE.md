# Icon Pack Quick Reference

Quick reference for developers working with icon pack support in Járngreipr.

## 📁 File Structure

```
app/src/main/java/jr/brian/home/
├── model/
│   └── IconPack.kt                          # Data models
├── data/
│   ├── IconPackPreferences.kt               # Settings storage
│   └── IconPackManager.kt                   # Core functionality
├── ui/
│   ├── components/
│   │   └── settings/
│   │       └── IconPackSelectorItem.kt      # UI component
│   └── screens/
│       └── SettingsScreen.kt                # (Modified)
├── viewmodels/
│   └── MainViewModel.kt                     # (Modified)
└── MainActivity.kt                          # (Modified)

docs/
├── ICON_PACKS.md                            # Complete guide
├── ICON_PACK_QUICK_START.md                 # User guide
├── TESTING_ICON_PACKS.md                    # Testing guide
├── ICON_PACK_FAQ.md                         # FAQ
├── ICON_PACK_IMPLEMENTATION_SUMMARY.md      # Technical summary
├── ICON_PACK_CHANGES.md                     # Change log
└── ICON_PACK_QUICK_REFERENCE.md             # This file
```

## 🔑 Key Classes

### IconPackManager

**Location**: `data/IconPackManager.kt`

```kotlin
class IconPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconPackPreferences: IconPackPreferences
)

// Main methods
suspend fun getInstalledIconPacks(): List<IconPack>
suspend fun loadIconPack(packageName: String?)
suspend fun getIconForPackage(packageName: String, defaultIcon: Drawable): Drawable
suspend fun setSelectedIconPack(packageName: String?)
fun getSelectedIconPack(): String?
```

### IconPackPreferences

**Location**: `data/IconPackPreferences.kt`

```kotlin
class IconPackPreferences @Inject constructor(
    @ApplicationContext private val context: Context
)

// Main methods
val selectedIconPackFlow: Flow<String?>
fun getSelectedIconPack(): String?
suspend fun setSelectedIconPack(packageName: String?)
```

### IconPackSelectorItem

**Location**: `ui/components/settings/IconPackSelectorItem.kt`

```kotlin
@Composable
fun IconPackSelectorItem(
    iconPackManager: IconPackManager,
    focusRequester: FocusRequester = FocusRequester(),
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    onIconPackChanged: () -> Unit = {}
)
```

## 🔄 Data Flow

```
User Action
    ↓
IconPackSelectorItem
    ↓
IconPackManager.setSelectedIconPack()
    ↓
IconPackPreferences (DataStore)
    ↓
IconPackManager.loadIconPack()
    ↓
Parse appfilter.xml
    ↓
Build mappings
    ↓
Cache icons
    ↓
HomeViewModel.loadAllApps()
    ↓
UI Updates
```

## 🎯 Common Operations

### Get Icon for App

```kotlin
val icon = iconPackManager.getIconForPackage(
    packageName = "com.example.app",
    defaultIcon = originalIcon
)
```

### Change Icon Pack

```kotlin
iconPackManager.setSelectedIconPack("com.iconpack.example")
iconPackManager.loadIconPack("com.iconpack.example")
homeViewModel.loadAllApps(context)
```

### Check Current Icon Pack

```kotlin
val currentPack = iconPackManager.getSelectedIconPack()
// Returns: package name or null
```

### List Available Icon Packs

```kotlin
val iconPacks = iconPackManager.getInstalledIconPacks()
// Returns: List<IconPack>
```

## 🧪 Testing Commands

### Build

```bash
./gradlew clean assembleDebug
```

### Install

```bash
./gradlew installDebug
```

### Check Icon Packs

```bash
adb shell pm list packages | grep -i icon
```

### View Logs

```bash
adb logcat | grep -E "(IconPack|IconPackManager)"
```

### Clear App Data

```bash
adb shell pm clear jr.brian.home
```

## 📝 Adding New Features

### Add Icon Pack Metadata Detection

Edit `IconPackManager.isIconPack()`:

```kotlin
private fun isIconPack(packageName: String, metaData: Bundle?): Boolean {
    if (metaData == null) return false
    
    val iconPackKeys = listOf(
        "com.novalauncher.theme",
        // Add new metadata key here
        "your.new.metadata.key"
    )
    
    return iconPackKeys.any { metaData.containsKey(it) }
}
```

### Add Custom Icon Resolution

Edit `IconPackManager.findIconInPack()`:

```kotlin
private fun findIconInPack(packageName: String): Drawable? {
    // Add custom logic here
    // Example: Try different naming conventions
}
```

### Add Icon Pack Preview

Edit `IconPackSelectorItem`:

```kotlin
@Composable
private fun IconPackOption(...) {
    // Add preview image
    Image(
        painter = rememberAsyncImagePainter(iconPack.previewIcon),
        // ...
    )
}
```

## 🔍 Debugging

### Common Issues

#### Icons Not Loading

**Check**: LogCat for errors

```bash
adb logcat | grep IconPackManager
```

#### Icon Pack Not Detected

**Check**: Package metadata

```bash
adb shell dumpsys package <icon-pack-package> | grep meta-data
```

#### Performance Issues

**Check**: Cache size

```kotlin
// Add to IconPackManager
fun getCacheSize(): Int = iconCache.size
```

### Debug Points

1. **Icon Pack Detection**: `getInstalledIconPacks()`
2. **Loading**: `loadIconPack()`
3. **Parsing**: `parseAppFilter()`
4. **Resolution**: `getIconForPackage()`
5. **Caching**: `iconCache` map

## ⚡ Performance Tips

### Optimization Checklist

- [x] Icons cached in memory
- [x] XML parsed once per session
- [x] Icons loaded on background thread
- [x] Minimal UI thread work
- [x] Efficient component lookup

### Memory Management

```kotlin
// Clear cache if needed
private fun clearIconPack() {
    currentIconPackResources = null
    componentToDrawableMap.clear()
    iconCache.clear()
}
```

## 🎨 UI Integration

### Settings Screen

```kotlin
// In SettingsContent composable
item {
    IconPackSelectorItem(
        iconPackManager = homeViewModel.iconPackManager,
        isExpanded = expandedItem == "icon_pack",
        onExpandChanged = { expandedItem = if (it) "icon_pack" else null },
        onIconPackChanged = {
            homeViewModel.loadAllApps(context)
        }
    )
}
```

### Custom Icon Display

```kotlin
// Anywhere you need an app icon
val icon = remember(app.packageName) {
    iconPackManager.getIconForPackage(
        app.packageName,
        app.icon
    )
}

Image(
    painter = rememberAsyncImagePainter(icon),
    contentDescription = app.label
)
```

## 📊 Metrics

### Performance Targets

- First load: < 3 seconds
- Subsequent loads: < 100ms
- Memory: < 10MB cache
- CPU: Minimal background usage

### Monitoring

```kotlin
// Add timing logs
val startTime = System.currentTimeMillis()
loadIconPack(packageName)
val duration = System.currentTimeMillis() - startTime
Log.d("IconPack", "Load time: ${duration}ms")
```

## 🔗 Related Files

### Modified for Integration

- `MainViewModel.kt` - Icon pack manager injection
- `MainActivity.kt` - Icon pack initialization
- `SettingsScreen.kt` - UI integration
- `strings.xml` - Localized strings

### Dependencies

- Hilt - Dependency injection
- DataStore - Preferences
- Coroutines - Async operations
- Coil - Image loading

## 📚 Documentation Links

- **User Guide**: [ICON_PACK_QUICK_START.md](ICON_PACK_QUICK_START.md)
- **Technical Guide**: [ICON_PACKS.md](ICON_PACKS.md)
- **Testing**: [TESTING_ICON_PACKS.md](TESTING_ICON_PACKS.md)
- **FAQ**: [ICON_PACK_FAQ.md](ICON_PACK_FAQ.md)

## 🚀 Quick Start

### For New Developers

1. **Read the code**:
    - Start with `IconPackManager.kt`
    - Then `IconPackSelectorItem.kt`
    - See integration in `MainViewModel.kt`

2. **Test locally**:
   ```bash
   ./gradlew installDebug
   # Install an icon pack
   # Try it in Settings
   ```

3. **Make changes**:
    - Edit the files
    - Test thoroughly
    - Submit PR

### For Testing

1. **Install icon packs**:
    - Whicons (free)
    - Flight Lite (free)
    - Lines Free (free)

2. **Test scenarios**:
    - Apply icon pack
    - Switch between packs
    - Reset to default
    - Check performance

3. **Report issues**:
    - Include icon pack name
    - Steps to reproduce
    - LogCat output

## 🎯 Key Points

✅ **Singleton**: IconPackManager is app-wide singleton
✅ **Cached**: Icons cached for performance
✅ **Async**: All loading on background threads
✅ **Fallback**: Always falls back to default icons
✅ **Reactive**: Uses Flow for reactive updates
✅ **Tested**: Clean build, no errors

## 📞 Support

- 📖 Documentation: `docs/` folder
- 🐛 Issues: GitHub Issues
- 💬 Discussions: GitHub Discussions
- ☕ Support: [Buy Me a Coffee](https://www.buymeacoffee.com/brianjr03)

---

**Last Updated**: December 2025
**Version**: 0.9.3-beta
**Status**: ✅ Complete
