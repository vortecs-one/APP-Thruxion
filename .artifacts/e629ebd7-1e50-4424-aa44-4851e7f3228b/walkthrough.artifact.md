# Walkthrough - Hide FAB when Keyboard is Displayed

I have updated `MainActivity` to automatically hide the main Floating Action Button (FAB) whenever the software keyboard is shown. This prevents the FAB from overlapping with the keyboard or other UI elements while the user is typing.

## Changes Made

### MainActivity

#### [MainActivity.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/MainActivity.kt)

Modified the `setupKeyboardListener` method to include FAB visibility toggling:

```diff
     private fun setupKeyboardListener() {
         val root = binding.root
         root.viewTreeObserver.addOnGlobalLayoutListener {
             val rect = Rect()
             root.getWindowVisibleDisplayFrame(rect)
             val screenHeight = root.rootView.height
             val keypadHeight = screenHeight - rect.bottom

             // If keyboard is visible (occupies more than 15% of the screen)
-            if (keypadHeight > screenHeight * 0.15)
-                binding.appBarMain.contentMain.bottomNavView?.visibility = View.GONE
-            else
-                binding.appBarMain.contentMain.bottomNavView?.visibility = View.VISIBLE
+            if (keypadHeight > screenHeight * 0.15) {
+                binding.appBarMain.contentMain.bottomNavView?.visibility = View.GONE
+                binding.appBarMain.fab?.hide()
+            } else {
+                binding.appBarMain.contentMain.bottomNavView?.visibility = View.VISIBLE
+                binding.appBarMain.fab?.show()
+            }
         }
     }
```

- Used `fab?.hide()` and `fab?.show()` to leverage Material Design's built-in animations for a smoother user experience.

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` and the build finished successfully, confirming no syntax errors or resource ID mismatches.

### Manual Verification Recommended
- Focus on any input field (e.g., in Profile or Settings) to trigger the keyboard.
- Verify that the Chat FAB disappears smoothly and reappears when the keyboard is dismissed.
