# Walkthrough - Improved Focus Management for Map Search

I have enhanced the focus management for the map search bar to ensure it automatically hides when you interact with other parts of the app. This prevents the search bar from staying "attached" to the keyboard when you open the chat or move around the map.

## Changes Made

### MainActivity

#### [MainActivity.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/MainActivity.kt)
Updated the Chat FAB listener to explicitly clear focus from any active input (like the map search) before showing the chat popup.
```diff
         binding.appBarMain.fab?.setOnClickListener {
+            // Clear focus from any active view (e.g. Map Search Bar) before showing chat
+            currentFocus?.clearFocus()
             ChatDialogFragment.newInstance().show(supportFragmentManager, "ChatDialog")
         }
```

### Thruxion Fragment

#### [fragment_thruxion.xml](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/res/layout/fragment_thruxion.xml)
Made the root layout focusable so that tapping empty areas of the screen can take focus away from the search bar.
```diff
 <androidx.constraintlayout.widget.ConstraintLayout
     ...
+    android:focusable="true"
+    android:focusableInTouchMode="true">
```

#### [ThruxionFragment.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/thruxion/ThruxionFragment.kt)
Added `clearFocus()` calls to key interaction points:
- Tapping on the map now clears the search focus.
- Tapping the "My Location" FAB clears the search focus.
- Tapping the "Map Layers" button clears the search focus.

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` and the build finished successfully.

### Manual Verification Recommended
1. **Search to Chat**: Tap the search bar (keyboard appears) -> Tap Chat FAB. Verify search bar hides immediately.
2. **Search to Map**: Tap search bar -> Tap any empty area on the map. Verify keyboard disappears and search focus is lost.
3. **Search to Layers**: Tap search bar -> Tap the layers button. Verify search focus is cleared.
