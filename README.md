# तापक्रम रेकर्ड लग (Cold Chain & Temperature Log)

This Android application is part of the Smart Health / Unique Healthh ecosystem sharing the Firebase project `smart-health-dce40`.

## Authentication & Password Hashing
- Passwords are verified using salted SHA-256 matching sibling apps (`ambulance-sewa` and `unique-healthh`):
  `hash = SHA256(password + "SmartHealthNepalSecurityKey2026")`
- Supports automatic self-healing migration for legacy plaintext passwords upon successful login.

## Role & Menu Permissions (Access Control)
To access cold chain logs, store temperature logs, or temperature reports, users must have the appropriate permission keys in their `allowedMenus` array under the `users` node in Firebase Realtime Database:
- `coldchain_log`
- `store_temperature_log`
- `temperature_reports`
- Or `ADMIN` / `SUPER_ADMIN` role (automatic full access).

### Admin Instructions
An administrator must add `coldchain_log`, `store_temperature_log`, or `temperature_reports` strings to a user's `allowedMenus` array in the `users` node (via the existing Unique Healthh admin/user-management screen, or directly in the Firebase Console) before that user can access the temperature modules. Users without these permissions will see a clear explanatory message on the Access Denied screen.

## Firebase Configuration (`google-services.json`)
Ensure your `app/google-services.json` is downloaded from the Firebase Console for project `smart-health-dce40` with package name `com.aistudio.smarthealth.coldchain`.
