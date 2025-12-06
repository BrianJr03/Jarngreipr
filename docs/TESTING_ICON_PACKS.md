# Testing Icon Pack Support

This guide will help you test the icon pack functionality in Járngreipr.

## Quick Test Steps

### 1. Check for Installed Icon Packs

Before testing, check if you have any icon packs installed:

1. Open **Settings** in Járngreipr
2. Scroll to **Icon Pack** under Appearance
3. Tap to expand
4. You should see:
    - "Default Icons" (always available)
    - Any installed icon packs

### 2. Install a Test Icon Pack

If you don't have any icon packs, install a free one:

**Recommended Free Icon Packs for Testing:**

1. **Whicons** - Simple white icons
    - Small download size
    - Good coverage of popular apps
    - [Play Store Link](https://play.google.com/store/apps/details?id=com.whicons.iconpack)

2. **Flight Lite** - Colorful flat icons
    - Free version available
    - Wide app support
    - [Play Store Link](https://play.google.com/store/apps/details?id=com.natewren.flightlite)

3. **Lines Free** - Clean line-based icons
    - Minimalist design
    - Good for testing
    - [Play Store Link](https://play.google.com/store/apps/details?id=com.natewren.linesfree)

### 3. Apply the Icon Pack

1. Open **Settings** in Járngreipr
2. Tap **Icon Pack**
3. Select an installed icon pack
4. Wait for "Loading..." to complete
5. Press back to return to the launcher
6. Observe icon changes

### 4. Verify Icon Changes

Check that:

- ✅ Common apps (Chrome, Gmail, etc.) show custom icons
- ✅ Apps without custom icons show default icons
- ✅ All apps are still launchable
- ✅ Icons load quickly on subsequent views

### 5. Switch Back to Default

1. Open **Settings**
2. Tap **Icon Pack**
3. Select **Default Icons**
4. Verify original icons are restored

## Detailed Testing Scenarios

### Scenario 1: First-Time Icon Pack Selection

**Steps:**

1. Fresh install of Járngreipr
2. Navigate to Settings → Icon Pack
3. Verify "Default Icons" is selected
4. Verify message "No icon packs installed" if none are present
5. Install an icon pack via Play Store
6. Return to Járngreipr
7. Verify icon pack now appears in the list
8. Select the icon pack
9. Verify loading indicator appears
10. Verify icons change on launcher

**Expected Result:**

- Icon pack applies successfully
- Common apps show custom icons
- No crashes or errors

### Scenario 2: Switching Between Multiple Icon Packs

**Prerequisites:** Install 2-3 different icon packs

**Steps:**

1. Select Icon Pack A
2. Verify icons change
3. Select Icon Pack B
4. Verify icons change to Icon Pack B's style
5. Select Icon Pack C
6. Verify icons change to Icon Pack C's style
7. Select Default Icons
8. Verify original icons return

**Expected Result:**

- Smooth transitions between icon packs
- Icons update correctly for each pack
- No memory leaks or performance issues

### Scenario 3: App with No Custom Icon

**Steps:**

1. Apply an icon pack
2. Find an app that doesn't have a custom icon (usually newer or less popular apps)
3. Verify the app shows its default icon
4. Verify the app still launches correctly

**Expected Result:**

- App shows default icon (not broken/missing)
- App functionality not affected

### Scenario 4: Icon Pack Uninstallation

**Steps:**

1. Apply an icon pack
2. Verify icons change
3. Uninstall the icon pack via Android settings
4. Return to Járngreipr
5. Observe behavior

**Expected Result:**

- Launcher should gracefully fall back to default icons
- No crashes
- Icon Pack setting should reset to "Default Icons"

### Scenario 5: Performance Test

**Steps:**

1. Have 50+ apps installed
2. Apply an icon pack
3. Measure time to load apps
4. Scroll through all apps
5. Switch to different pages
6. Return to apps list

**Expected Result:**

- Initial load: 1-3 seconds
- Subsequent views: Instant (cached)
- Smooth scrolling
- No lag or stuttering

### Scenario 6: Icon Pack with Partial Coverage

**Steps:**

1. Install an icon pack with limited app coverage
2. Apply the icon pack
3. Scroll through all apps
4. Note which apps have custom icons vs default

**Expected Result:**

- Some apps show custom icons
- Some apps show default icons
- Mixed display looks consistent
- No visual glitches

## Automated Testing Checklist

Use this checklist for thorough testing:

### Functionality

- [ ] Icon packs are detected and listed
- [ ] Default Icons option always appears first
- [ ] Selected icon pack is remembered after restart
- [ ] Icons load correctly on first launch
- [ ] Icons are cached on subsequent launches
- [ ] Switching icon packs updates all icons
- [ ] Resetting to default works correctly

### UI/UX

- [ ] Settings item is clearly labeled "Icon Pack"
- [ ] Current selection is shown in description
- [ ] Radio buttons indicate current selection
- [ ] Loading indicator appears during loading
- [ ] Icon pack names are readable
- [ ] Icon pack package names are shown
- [ ] Smooth animations when expanding/collapsing

### Edge Cases

- [ ] No icon packs installed: Shows appropriate message
- [ ] Icon pack without appfilter.xml: Falls back gracefully
- [ ] Corrupted icon pack: Doesn't crash
- [ ] Very large icon pack (1000+ icons): Loads without hanging
- [ ] Icon pack uninstalled while selected: Falls back to default
- [ ] Multiple apps with same package prefix: Correct icons

### Performance

- [ ] First load < 3 seconds for 100 apps
- [ ] Subsequent loads instant
- [ ] Memory usage reasonable (check with profiler)
- [ ] No memory leaks when switching packs
- [ ] Smooth UI during icon loading

### Compatibility

- [ ] Works with Nova Launcher icon packs
- [ ] Works with ADW Launcher icon packs
- [ ] Works with Apex Launcher icon packs
- [ ] Works with Action Launcher icon packs
- [ ] Works with custom icon packs

## Common Issues and Solutions

### Issue: Icons don't change after selecting icon pack

**Debug Steps:**

1. Check LogCat for errors:
   ```bash
   adb logcat | grep -i "iconpack"
   ```
2. Verify icon pack has `appfilter.xml`:
   ```bash
   adb shell pm list packages | grep <icon-pack-package>
   adb shell pm path <icon-pack-package>
   ```
3. Check if icon pack metadata exists

### Issue: App crashes when selecting icon pack

**Debug Steps:**

1. Check LogCat for stack trace
2. Verify icon pack is properly formatted
3. Check for null pointer exceptions in IconPackManager
4. Verify Resources can be loaded from icon pack

### Issue: Only some icons change

**Solution:** This is expected behavior. Not all icon packs include all apps.

### Issue: Icons load slowly

**Debug Steps:**

1. Check icon pack size (number of icons)
2. Verify caching is working
3. Profile with Android Studio Profiler
4. Check if XML parsing is efficient

## Manual Testing with ADB

### Check Installed Icon Packs

```bash
# List all installed packages
adb shell pm list packages

# Check for icon pack metadata
adb shell dumpsys package <icon-pack-package> | grep -i "meta-data"
```

### Force Clear Cache

```bash
# Clear app data to reset icon pack selection
adb shell pm clear jr.brian.home
```

### Monitor Performance

```bash
# Monitor memory usage
adb shell dumpsys meminfo jr.brian.home

# Monitor CPU usage
adb shell top | grep jr.brian.home
```

### Check DataStore

```bash
# View stored preferences
adb shell run-as jr.brian.home
cd files/datastore/
cat icon_pack_preferences.preferences_pb
```

## Testing on Different Devices

Test on various devices to ensure compatibility:

### Device Categories

1. **Low-end devices** (2GB RAM)
    - Test memory usage
    - Check loading times
    - Verify no crashes

2. **Mid-range devices** (4-6GB RAM)
    - Standard user experience
    - Smooth performance expected

3. **High-end devices** (8GB+ RAM)
    - Instant loading
    - Multiple icon packs switching

4. **Tablets**
    - Larger screen display
    - More visible icons
    - Grid layout considerations

5. **Different Android Versions**
    - Android 13 (minimum supported)
    - Android 14
    - Android 15+

## Reporting Issues

When reporting icon pack issues, include:

1. **Icon Pack Name and Package**: e.g., "Whicons (com.whicons.iconpack)"
2. **Device Info**: Model, Android version, RAM
3. **Steps to Reproduce**: Detailed steps
4. **Expected Behavior**: What should happen
5. **Actual Behavior**: What actually happens
6. **Screenshots**: If applicable
7. **Logs**: LogCat output if available

### LogCat Filter

```bash
adb logcat | grep -E "(IconPack|IconPackManager|IconPackPreferences)"
```

## Success Criteria

Icon pack support is working correctly if:

✅ All functional tests pass
✅ UI is responsive and intuitive
✅ Performance is acceptable (< 3s initial load)
✅ No crashes or errors in LogCat
✅ Memory usage is reasonable
✅ Works with popular icon packs
✅ Graceful fallback for unsupported packs
✅ Settings persist across restarts

## Next Steps

After testing:

1. Document any issues found
2. Create GitHub issues for bugs
3. Suggest UX improvements
4. Test with more icon packs
5. Gather user feedback

---

**Happy Testing!** 🎨📱
