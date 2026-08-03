# Implementation Plan: Chat UI & Repository Fixes

This plan addresses three specific issues reported after the Oversec integration:
1. Sent messages not appearing in the chat.
2. Images displaying a "[image]" legend.
3. Chat UI not adjusting correctly when the keyboard opens.

## Proposed Changes

### [Component] Data Layer

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/data/repository/ChatRepositoryImpl.kt)
- **Fix Message Persistence**: Modify `sendMessage` to insert the user's message into the local Room database *before* attempting the network call. This ensures the message appears immediately (Optimistic UI).
- **Network Resilience**: In case of network failure, the message remains in the local DB.
- **Image Encryption**: Ensure `sendImage` also applies Oversec encryption to the placeholder/metadata if enabled.

### [Component] UI Layer (Compose)

#### [MODIFY] [ChatScreen.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/chat/ChatScreen.kt)
- **Image Bubble Fix**: In `WhatsAppMessageBubble`, hide the text row if the message type is `IMAGE` and the content is the default `[Image]` placeholder.
- **Keyboard Handling**:
    - Move `imePadding()` to a more appropriate level if needed.
    - Ensure the `LazyColumn` scrolls to the bottom when the keyboard visibility changes by improving the `LaunchedEffect` trigger.
    - Use `WindowInsets.ime` in the `ChatDetailScreen` to ensure the input row stays above the keyboard.

#### [MODIFY] [ChatDialogFragment.kt](file:///home/vortecs/Documents/APPs/APP-QHago/app/src/main/java/com/thruxion/app/ui/chat/ChatDialogFragment.kt)
- Adjust the `Box` and `Surface` modifiers to ensure the keyboard doesn't overlap the chat input.

## Verification Plan

### Manual Verification
1. **Message Appearance**: Send a text message and verify it appears immediately in the chat bubble.
2. **Image Legend**: Send an image and verify that the "[Image]" text is not visible below/beside the image.
3. **Keyboard adjustment**: Open the keyboard in the chat detail screen and verify that:
    - The input field stays visible above the keyboard.
    - The message list adjusts and scrolls to the bottom.
