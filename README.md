# Copy As Snippet for Burp Suite

Are you tired of removing junk headers and cookies from the PoCs you copy from Burp? I definitely am, which is why this extension exists!

## Features
- Copy the request/response from the context menu in the text format
  - Automatic junk header/cookie removal
  - HTTP parameter removal (GET, POST forms, JSON)
  - JWTs and sensitive cookies are replaced with `REDACTED` (configurable via extension settings)
- Configure an editor keyboard shortcut to skip the context menu
- Supports multiple presets. All presets are shared between projects. If the default preset doesn't scrub all junk headers, you can always create your own!

**Note**: The default junk header/cookie list is not meant to be universal. For example, you can completely ignore cache-related headers in one PoC, but not in another. Create, use and share presets, and feel free to suggest changes to the default one via GitHub issues!

![Settings view](readme/settings-view.png)

![Preset editor](readme/preset-editor.png)

## Building locally

Uses OpenJDK 21.0.10 with Gradle 9.3.1. To build your own copy, just run `gradle build` from the repository root, then grab the JAR from `build/libs/`.
