# GitSync
Sync files from a GitHub repository onto your
Minecraft server. Fairly useful for configurations shared by multiple users.

![image](https://user-images.githubusercontent.com/43708436/179966143-c67f65b0-4a7f-48e9-97c2-a8036d43f592.png)

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
