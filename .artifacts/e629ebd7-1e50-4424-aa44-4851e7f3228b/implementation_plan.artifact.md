# Fix My Location Icon disappearing on Map when Keyboard is opened

The goal is to keep the "My Location" FAB visible on the map even when the software keyboard is displayed. Currently, a keyboard listener in `ThruxionFragment` explicitly hides this button.

## Proposed Changes

### ThruxionFragment

#### [MODIFY] [ThruxionFragment.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/thruxion/ThruxionFragment.kt)
- Remove the lines in `setupKeyboardListener` that set `fabMyLocation.visibility` to `View.GONE` and `View.VISIBLE`.
- This will ensure the button remains visible and properly positioned (it is constrained to the bottom of the map area, which resizes when the keyboard appears).

## Verification Plan

### Automated Tests
- Build the app to ensure no regressions.

### Manual Verification
- Navigate to the Thruxion (Map) screen.
- Tap on the search bar to open the keyboard.
- Verify that both the "My Location" icon (target symbol) and the "Map Layers" icon (yellow world symbol) remain visible above the search bar.
- Verify that the "My Location" button still works while the keyboard is open.
