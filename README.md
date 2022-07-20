# GitSync
Sync files from a GitHub repository onto your
Minecraft server. Useful for configurations.

## Setup
- Load the plugin onto the server.
- Edit config.yml and add your GH personal access token.
- Add whitelisted and blacklisted files.
- Run /gitsync reload to reload the config.
- Run /gitsync sync to sync.
- tada!

## Permissions
You can probably guess what they're for...
- gitsync.*
- gitsync.sync
- gitsync.reload

## Building
```bash
./gradlew clean build
```
