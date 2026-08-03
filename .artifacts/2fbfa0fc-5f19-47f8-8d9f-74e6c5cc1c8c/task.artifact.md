# Tasks: Chat UI & Repository Fixes

- [x] **Phase 1: Data Layer Fixes**
    - [x] Modify `ChatRepositoryImpl.sendMessage` for optimistic insertion.
    - [x] Update `ChatRepositoryImpl.sendImage` to store metadata locally.
- [x] **Phase 2: UI Layer Fixes**
    - [x] Update `WhatsAppMessageBubble` to hide "[Image]" text for images.
- [x] **Phase 3: Keyboard & Layout Fixes**
    - [x] Improve `imePadding` and scroll-to-bottom logic in `ChatScreen.kt`.
    - [x] Adjust `ChatDialogFragment` surface height and padding for keyboard compatibility.
- [ ] **Verification**
    - [ ] Verify message appears instantly on send.
    - [ ] Verify image bubble does not show placeholder text.
    - [ ] Verify chat input stays above keyboard.
