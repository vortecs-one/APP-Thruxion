# Hide FAB when Keyboard is Displayed

The goal is to hide the Floating Action Button (FAB) in `MainActivity` when the software keyboard is visible to improve UI clarity and prevent overlapping.

## User Review Required

> [!NOTE]
> The current implementation already hides the `BottomNavigationView` when the keyboard is visible. I will extend this logic to also hide the FAB.

## Proposed Changes

### MainActivity

#### [MODIFY] [MainActivity.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/MainActivity.kt)
- Update `setupKeyboardListener` to toggle the visibility of the FAB.
- Use `fab.hide()` and `fab.show()` for smooth animations.
- Ensure all possible locations of the FAB (depending on screen configuration) are handled if accessible via binding.

## Verification Plan

### Automated Tests
- I will verify if the code compiles by running a gradle build (optional, but good for sanity).

### Manual Verification
- Deploy the app and focus an `EditText` (e.g., in the Profile screen or Reflow screen).
- Observe that the FAB in `MainActivity` disappears when the keyboard is shown and reappears when hidden.
- Note: This change affects the main Chat FAB. FABs used as local UI elements (like the "Send" button in Chat or the "Edit Image" button in Profile) will remain visible if they are necessary for interaction while the keyboard is up.
