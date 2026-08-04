# Improve Focus Management for Map Search Bar

The goal is to ensure the map search bar loses focus when the user interacts with other UI elements, such as the Chat FAB or the map itself. This fixes the issue where the search bar remains "attached" to the keyboard even when opening the chat.

## Proposed Changes

### MainActivity

#### [MODIFY] [MainActivity.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/MainActivity.kt)
- Update the FAB click listener to clear focus from the window's current focus owner.

### Thruxion UI

#### [MODIFY] [ThruxionFragment.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/thruxion/ThruxionFragment.kt)
- Clear focus from `editSearch` in `map.addOnMapClickListener`.
- Add `binding.editSearch.clearFocus()` to common UI interaction points like `fabMyLocation` and `btnMapLayers`.

### Thruxion Layout

#### [MODIFY] [fragment_thruxion.xml](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/res/layout/fragment_thruxion.xml)
- Add `android:focusable="true"` and `android:focusableInTouchMode="true"` to the root layout to allow focus to be "stolen" from the search bar when tapping empty areas.

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to verify compilation.

### Manual Verification
1. **Search to Chat**: Focus search bar -> Tap Chat FAB. Search bar should hide.
2. **Search to Map**: Focus search bar -> Tap Map. Search bar should lose focus.
3. **Search to List**: Focus search bar -> Tap empty area of the bottom list. Search bar should lose focus.
