# Icon Pack FAQ

Frequently Asked Questions about Icon Pack support in Járngreipr.

## General Questions

### What are icon packs?

Icon packs are apps that contain custom icons for other apps. They let you change how your app icons
look on your launcher, giving you a consistent style across all your apps.

### Do I need to pay for icon packs?

No! There are many free icon packs available on the Play Store. However, some premium icon packs
offer more icons and styles.

### Will this slow down my launcher?

No! Icons are cached after first load, so after the initial setup (which takes just a few seconds),
icon packs don't affect performance.

## Installation & Setup

### How do I install an icon pack?

1. Open the Google Play Store
2. Search for "icon pack" or a specific pack name (e.g., "Whicons")
3. Install the icon pack like any other app
4. Go back to Járngreipr and apply it in Settings

### Where do I find icon packs?

Search the Play Store for:

- "icon pack"
- "launcher icons"
- Specific pack names like "Whicons", "Lines", "Flight"

Look for packs that mention launcher compatibility.

### How do I apply an icon pack?

1. Open Járngreipr
2. Tap the Settings icon (⚙️)
3. Scroll to "Icon Pack" under Appearance
4. Tap to expand and select your pack
5. Wait for loading to complete
6. Press back - done!

### Can I try different icon packs without uninstalling?

Yes! You can switch between icon packs instantly. Just:

1. Go to Settings → Icon Pack
2. Select a different pack
3. The icons update immediately

### How do I remove an icon pack?

To go back to default icons:

1. Settings → Icon Pack
2. Select "Default Icons"

To uninstall the icon pack app itself:

1. Long press the icon pack app
2. Select "App Info"
3. Tap "Uninstall"

## Compatibility

### Which icon packs work with Járngreipr?

Most icon packs designed for launchers work! Look for packs that support:

- Nova Launcher
- ADW Launcher
- Apex Launcher
- Action Launcher

These use the standard format that Járngreipr supports.

### Do all apps get custom icons?

No, not all apps will have custom icons in every icon pack. Icon pack developers choose which apps
to include, usually focusing on popular apps first.

Less common apps will keep their original icons.

### What happens to apps without custom icons?

They simply show their default icon. There's no broken image or missing icon - it just looks like
normal.

### Can I use multiple icon packs at once?

Not currently. You can only apply one icon pack at a time. However, you can switch between them
easily!

## Troubleshooting

### My icon pack isn't showing in the list

**Possible causes:**

1. **Not a launcher icon pack** - Make sure it's designed for launchers, not just wallpapers
2. **Just installed** - Try closing and reopening Járngreipr
3. **Incompatible format** - Some very old or proprietary packs might not work

**Solution:**

- Make sure the pack is installed
- Restart Járngreipr
- Try a different icon pack to verify the feature works

### Icons didn't change after selecting a pack

**Try these steps:**

1. Wait 5-10 seconds (initial loading takes time)
2. Press back and return to home screen
3. Check if any icons changed (popular apps like Chrome/Gmail usually change first)
4. Try a different icon pack

### Only a few icons changed

**This is normal!** Icon packs don't include every single app. Popular apps (Chrome, Gmail, YouTube,
etc.) are usually included, while newer or less common apps might not be.

### Icons look blurry or pixelated

This usually means:

1. The icon pack uses low-resolution icons
2. Try a different icon pack for better quality
3. Vector-based icon packs usually look best

### App crashes when selecting icon pack

**Please report this!**

1. Note which icon pack caused the crash
2. Try a different icon pack to see if it works
3. Submit a bug report with the pack name

### Icons reverted to default

**Possible causes:**

1. Icon pack was uninstalled
2. Icon pack was updated and format changed
3. Launcher data was cleared

**Solution:**

- Reinstall the icon pack
- Reapply it in settings

### Loading takes a long time

**First time:** Loading can take 3-5 seconds for large icon packs (1000+ icons)

**Every time:** If it's always slow:

1. Try a smaller icon pack
2. Restart your device
3. Clear launcher cache and try again

## Features & Limitations

### Can I customize individual app icons?

Not yet! Currently, the icon pack applies to all apps. Individual app customization is planned for a
future update.

### Do calendar icons show the date?

Not currently. Calendar icons are static for now. Dynamic calendar icons are planned for future
updates.

### Do clock icons show the time?

Not currently. Clock icons are static. Dynamic clock icons are planned for future updates.

### Can I create my own icon pack?

Yes! See the [Creating Your Own Icon Pack](ICON_PACKS.md#creating-your-own-icon-pack) section in the
detailed documentation.

### Can I mix icons from different packs?

Not currently. You can only apply one icon pack at a time. This feature may be added in the future.

### Do icon packs affect app functionality?

No! Icon packs only change how icons look. They don't affect:

- App launching
- App performance
- App permissions
- App data

## Performance

### How much storage do icon packs use?

Icon packs themselves typically use:

- **Small packs**: 5-20 MB
- **Medium packs**: 20-50 MB
- **Large packs**: 50-100+ MB

Járngreipr caches icons in memory (not storage), typically using 5-10 MB of RAM.

### Will icon packs drain my battery?

No! Icon packs are loaded once and cached, so they don't use ongoing CPU or battery.

### How many icon packs can I install?

As many as you want! You can install multiple icon packs and switch between them. Each one is a
separate app.

## Privacy & Security

### Do icon packs access my data?

Icon packs themselves are just images. Járngreipr reads the icons but doesn't send any data
anywhere.

### Are icon packs safe?

Icon packs from the Play Store go through Google's security checks. Always download from trusted
sources.

### What permissions do icon packs need?

Most icon packs don't need any special permissions. They just contain images.

## Popular Icon Packs

### What are some good free icon packs to start with?

**For Minimalists:**

- Whicons (white line icons)
- Lines Free (clean line art)
- Ameixa (monochrome)

**For Color Lovers:**

- Flight Lite (colorful flat)
- Urmun (material design)
- Moonshine (detailed)

**For Material Design Fans:**

- Delta (adaptive icons)
- Rundo (rounded)
- Rugos (material style)

### What's the difference between free and paid icon packs?

**Free packs typically include:**

- 500-2000 icons
- Most popular apps
- Basic updates

**Paid packs often include:**

- 3000-8000+ icons
- More obscure apps
- Faster updates
- Additional wallpapers
- Support for icon requests

### How do I find high-quality icon packs?

Look for:

- High ratings (4+ stars)
- Many reviews
- Recent updates
- Clear screenshots
- Active developer

Popular developers:

- Jahir Fiquitiva
- Nate Wren
- Vertumus
- Lucas Kendi

## Advanced

### Can I request icons for apps not in a pack?

Yes! Many icon pack developers accept icon requests through their app or website. Look for an "Icon
Request" option in the icon pack app.

### How do icon packs work technically?

Icon packs contain:

1. PNG/Vector images for each app
2. An `appfilter.xml` file that maps apps to icons
3. Metadata that identifies it as an icon pack

Járngreipr reads this file and replaces app icons with the custom ones.

### Why doesn't every app in my icon pack show up?

Icon packs often include 1000s of icons, but:

- You might only have 50-100 apps installed
- Many icons are for region-specific apps
- Some icons are for older/discontinued apps

### Can I export my icon pack selection?

Currently, the selection is saved in Járngreipr's settings. If you backup your app data, the icon
pack selection is included.

## Getting Help

### Where can I get more help?

- 📖 **Detailed Guide**: [ICON_PACKS.md](ICON_PACKS.md)
- 🚀 **Quick Start**: [ICON_PACK_QUICK_START.md](ICON_PACK_QUICK_START.md)
- 🐛 **Report Bug**: [GitHub Issues](https://github.com/BrianJr03/Jarngreipr/issues)
- ☕ **Support Developer**: [Buy Me a Coffee](https://www.buymeacoffee.com/brianjr03)

### How do I report a bug?

1. Go to [GitHub Issues](https://github.com/BrianJr03/Jarngreipr/issues)
2. Click "New Issue"
3. Include:
    - Icon pack name and package
    - Device and Android version
    - Steps to reproduce
    - Screenshots if applicable

### How can I suggest improvements?

Same process as bugs! Create a GitHub issue with your suggestion.

---

## Still Have Questions?

If your question isn't answered here:

1. Check the [detailed documentation](ICON_PACKS.md)
2. Search existing [GitHub Issues](https://github.com/BrianJr03/Jarngreipr/issues)
3. Create a new issue with your question

**Happy customizing!** 🎨
