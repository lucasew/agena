## 2024-09-05 - ADB Backup Flag Intentionally Enabled
**Vulnerability:** The application was configured with `android:allowBackup="true"`, permitting unencrypted data extraction via ADB.
**Learning:** This configuration was reportedly intentional, likely for development or debugging convenience. However, leaving it enabled in a production build poses a significant security risk by exposing sensitive user data (like browsing history) to anyone with physical access to the device.
**Prevention:** Security best practices must be enforced even if they override previous design decisions that conflict with user safety. Risky debugging features should be strictly limited to debug builds, not production releases. Sentinel has overridden this setting to enforce a secure default.
