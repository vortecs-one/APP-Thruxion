# Walkthrough - Keep My Location Icon Visible on Map

I have fixed the issue where the "My Location" icon would disappear from the map when the keyboard was opened. The icon now remains visible and properly positioned above the search bar while typing.

## Changes Made

### ThruxionFragment

#### [ThruxionFragment.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/thruxion/ThruxionFragment.kt)

Modified the `setupKeyboardListener` to prevent it from hiding the location FAB:

```diff
             // If keyboard is visible (occupies more than 15% of the screen)
             if (keypadHeight > screenHeight * 0.15) {
                 // Keep only the top of the list (search bar and icons) visible
                 currentBinding.recyclerView.visibility = View.GONE
-                currentBinding.fabMyLocation.visibility = View.GONE
                 currentBinding.bottomListCard.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
             } else {
                 // Keyboard is hidden - restore full list
                 currentBinding.recyclerView.visibility = View.VISIBLE
-                currentBinding.fabMyLocation.visibility = View.VISIBLE

                 // Restore fixed height (240dp)
```

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` and the build finished successfully.

### Manual Verification Recommended
1. Open the Map screen (Thruxion tab).
2. Tap the search field to trigger the keyboard.
3. Observe that the "My Location" button (target icon) remains visible above the keyboard.
4. Dismiss the keyboard and verify it still behaves correctly.
