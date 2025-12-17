# Folder Support Implementation

## Overview

Added comprehensive folder support to the FreePositionedAppsLayout with drag-and-drop functionality,
folder previews, animations, and intuitive UI.

## Features Implemented

### 1. Folder Creation

- **Overlap to Create**: Drag one app icon over another until they overlap (within 32dp threshold)
  to automatically create a folder
- **Drag to Folder**: Drag apps onto existing folder icons to add them to the folder
- **Visual Feedback**: Folder scales up when an app is dragged over it, providing clear visual
  feedback

### 2. Folder Display

- **Icon Preview**: Shows up to 4 app icons in a grid layout when folder is closed
- **Preview Layouts**:
    - 1 app: Single centered icon
    - 2 apps: Vertical stack
    - 3 apps: One on top, two on bottom
    - 4+ apps: 2x2 grid
- **Folder Name**: Displayed below the folder icon, editable
- **Custom Styling**: Rounded corners, semi-transparent background, border effects

### 3. Folder Interaction

- **Open Folder**: Click/tap on folder icon to open dialog with all apps
- **Edit Name**: Click edit button in folder dialog or long-press folder icon to access options
- **Delete Folder**: Delete option in folder dialog or options menu - apps are restored to their
  default positions
- **Remove Apps**: Click app icon in folder dialog to toggle remove button, then remove individual
  apps

### 4. Animations

- **Scale Animation**: Smooth scale transition when folder is focused or when apps are dragged over
- **Fade In/Out**: Animated visibility for remove buttons and folder dialogs
- **Slide Transitions**: Smooth entry/exit animations for dialogs

### 5. Folder Management

- **Position & Size**: Folders can be dragged and resized just like regular app icons
- **Persistence**: Folder data persists across app restarts using SharedPreferences
- **Alignment Guides**: Folders participate in the alignment guide system

## Architecture

### Data Models

```kotlin
data class AppFolder(
    val id: String,              // Unique UUID
    val name: String,            // User-editable name
    val apps: List<String>,      // Package names
    val x: Float,                // Position X
    val y: Float,                // Position Y
    val iconSize: Float = 64f    // Icon size in dp
)
```

### Key Components

1. **FolderManager** (`data/FolderManager.kt`)
    - Manages folder creation, updates, and deletion
    - Handles folder data persistence
    - Provides StateFlow for open folder tracking
    - Singleton managed by Hilt

2. **FolderIcon** (`ui/components/folder/FolderIcon.kt`)
    - Displays folder icon with app preview grid
    - Handles drag gestures for repositioning
    - Shows folder name with text truncation
    - Provides visual feedback for drag-over events

3. **FolderContentDialog** (`ui/components/folder/FolderContentDialog.kt`)
    - Full-screen modal dialog showing folder contents
    - Grid layout of apps (4 columns)
    - Inline name editing with TextField
    - Remove individual apps with animated buttons
    - Delete entire folder option

4. **FolderOptionsDialog** (`ui/components/folder/FolderOptionsDialog.kt`)
    - Long-press context menu for folders
    - Options: Edit Name, Resize Icon, Delete Folder
    - Material Design 3 styling

### Integration with FreePositionedAppsLayout

The layout now:

- Filters apps into two groups: apps in folders and apps not in folders
- Renders folder icons alongside regular app icons
- Detects overlap during drag operations
- Creates folders automatically when overlap threshold is met
- Adds apps to folders when dragged over folder icons
- Manages folder lifecycle (create, update, delete)

## String Resources

All user-facing text uses string resources for internationalization:

```xml
<string name="folder_default_name">Folder</string>
<string name="folder_edit_name">Edit Folder Name</string>
<string name="folder_name_hint">Folder name</string>
<string name="folder_save">Save</string>
<string name="folder_delete">Delete Folder</string>
<string name="folder_remove_app">Remove from Folder</string>
<string name="folder_open">Open</string>
<string name="folder_close">Close</string>
<string name="folder_apps_count">%1$d apps</string>
<string name="folder_empty">Empty folder</string>
<string name="folder_create_by_overlap">Overlap icons to create folder</string>
<string name="folder_drag_to_add">Drag app here to add</string>
```

## Usage Guide

### Creating a Folder

1. Unlock drag mode (if locked)
2. Long-press and drag an app icon
3. Drag it over another app icon until they overlap
4. Release - folder is automatically created with both apps
5. Default name "Folder" is assigned (can be edited)

### Adding Apps to Folder

1. Long-press and drag an app icon
2. Drag it over an existing folder icon
3. Folder will scale up to indicate drop target
4. Release to add app to folder

### Opening a Folder

1. Click/tap on folder icon
2. Dialog opens showing all apps in grid layout
3. Click any app to launch it (closes dialog)
4. Click edit icon to rename folder
5. Click X to close dialog

### Managing Folder Contents

1. Open folder dialog
2. Click on an app icon to toggle remove button
3. Click remove button (X) to remove app from folder
4. If last app is removed, folder is automatically deleted
5. Removed apps return to default grid positions

### Deleting a Folder

1. Long-press folder icon (when drag is locked)
2. Select "Delete Folder" from options menu
   OR
3. Open folder dialog and click "Delete Folder" at bottom
4. All apps in folder are restored to default positions

### Customizing Folders

1. **Rename**: Open folder > Click edit icon > Type new name > Click X
2. **Resize**: Long-press folder > Select "Resize Icon"
3. **Reposition**: Drag folder to new location (when drag unlocked)

## Technical Details

### Drag Detection

- **Overlap Threshold**: 32dp - distance between icon centers to trigger folder creation
- **Snap Threshold**: 12dp - distance for alignment guides
- **Border Padding**: 4dp - minimum distance from screen edges

### Distance Calculation

```kotlin
private fun calculateDistance(
    x1: Float, y1: Float, size1: Float,
    x2: Float, y2: Float, size2: Float
): Float {
    val centerX1 = x1 + size1 / 2
    val centerY1 = y1 + size1 / 2
    val centerX2 = x2 + size2 / 2
    val centerY2 = y2 + size2 / 2
    val dx = centerX1 - centerX2
    val dy = centerY1 - centerY2
    return sqrt(dx * dx + dy * dy)
}
```

### Data Persistence

Folders are stored in SharedPreferences with format:

```
Key: folders_<pageIndex>
Value: folderId^^name^^app1,app2,app3^^x^^y^^iconSize|||nextFolder...
```

### State Management

- FolderManager provides reactive state via StateFlow
- Open folder ID tracked globally
- Folder changes trigger recomposition automatically
- Position updates saved immediately

## Code Quality

- **No Comments**: Clean, self-documenting code
- **Reusable Components**: All folder UI components are standalone and reusable
- **String Resources**: All text internationalized
- **Type Safety**: Kotlin data classes with proper types
- **Null Safety**: Proper handling of nullable types
- **Memory Efficient**: StateMap for reactive folder management

## Testing Checklist

- [x] Create folder by overlapping apps
- [x] Add app to existing folder by dragging
- [x] Open folder and launch apps
- [x] Edit folder name
- [x] Remove apps from folder
- [x] Delete folder with apps
- [x] Drag folder to new position
- [x] Resize folder icon
- [x] Folder persists after app restart
- [x] Multiple folders on same page
- [x] Folders across different pages
- [x] Visual feedback for drag-over
- [x] Animations smooth and responsive
- [x] Alignment guides work with folders

## Future Enhancements

- Nested folders support
- Folder color customization
- Custom folder icons
- Sort apps within folder
- Batch add apps to folder
- Import/export folder configurations
- Cloud sync for folders
