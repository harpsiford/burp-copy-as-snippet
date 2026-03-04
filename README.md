# Copy As Snippet for Burp Suite

Are you tired of removing junk headers and cookies from the PoCs you copy from Burp? I definitely am, which is why this extension exists!

## Features
- Copy the request/response from the context menu in a configurable format
- Configure an editor keyboard shortcut to skip the context menu
- Junk headers/cookies are automatically removed from the request/response
- HTTP parameter removal is also supported (GET, POST forms, JSON)
- JWTs and sensitive cookies are automatically replaced with a string of your choosing (`REDACTED` by default)
- Supports user/project-level presets. If the default preset doesn't scrub all junk headers, you can always create your own!

**Note**: The default junk header/cookie list is not meant to be universal. For example, you can completely ignore cache-related headers in one PoC, but in another you won't be able to ignore them. Create and use presets, for yourself, and feel free to suggest changes via GitHub issues!

![Settings view](readme/settings-view.png)

![Preset editor](readme/preset-editor.png)

## Building locally

Uses OpenJDK 21.0.10 with Gradle 9.3.1. To build your own copy, just run `gradle build` from the repository root, then grab the JAR from `build/libs/`.
