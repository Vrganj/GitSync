package me.vrganj.gitsync;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class GitSync extends JavaPlugin implements CommandExecutor {
    private static final Component PREFIX = text("[", DARK_GRAY).append(text("GitSync", DARK_GREEN)).append(text("] ", DARK_GRAY));
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private List<Pattern> whitelist, blacklist;

    private static Pattern parsePattern(String pattern) {
        return Pattern.compile("^\\Q" + pattern.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q") + "\\E$");
    }

    private boolean isWhitelisted(String path) {
        return whitelist.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }

    private boolean isBlacklisted(String path) {
        return blacklist.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        Objects.requireNonNull(getCommand("gitsync")).setExecutor(this);
    }

    private void loadConfig() {
        reloadConfig();

        whitelist = new ArrayList<>();
        blacklist = new ArrayList<>();

        for (String pattern : getConfig().getStringList("whitelist")) {
            whitelist.add(parsePattern(pattern));
        }

        for (String pattern : getConfig().getStringList("blacklist")) {
            blacklist.add(parsePattern(pattern));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equals("?")) {
            sender.sendMessage(PREFIX.append(text("Usage: ", GRAY)).append(text("/gitsync <sync/reload>", GREEN)));
            return true;
        }

        if (args[0].equalsIgnoreCase("sync")) {
            if (!sender.hasPermission("gitsync.sync")) {
                sender.sendMessage(PREFIX.append(text("Insufficient permissions (gitsync.sync)", RED)));
                return false;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    sender.sendMessage(PREFIX.append(text("Fetching repository zipball...", GRAY)));
                    long start = System.currentTimeMillis();

                    var repository = getConfig().getString("repository");

                    if (repository == null) {
                        sender.sendMessage(PREFIX.append(text("Missing repository in config!", RED)));
                        return;
                    }

                    var token = getConfig().getString("token");

                    if (token == null) {
                        sender.sendMessage(PREFIX.append(text("Missing token in config!", RED)));
                        return;
                    }

                    var request = HttpRequest.newBuilder(new URI("https://api.github.com/repos/" + repository + "/zipball/master"))
                            .header("Accept", "application/vnd.github+json")
                            .header("Authorization", token)
                            .GET()
                            .build();

                    var res = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

                    if (res.statusCode() >= 400) {
                        sender.sendMessage(PREFIX.append(text("Failed to fetch. Probably invalid token.", RED)));
                        return;
                    }

                    var stream = new ZipInputStream(res.body());

                    ZipEntry entry;

                    while ((entry = stream.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }

                        var path = StringUtils.substringAfter(entry.getName(), "/");

                        if (isBlacklisted(path)) {
                            sender.sendMessage(PREFIX.append(text("Skipping ", GRAY).append(text(path, RED))));
                        } else if (isWhitelisted(path)) {
                            sender.sendMessage(PREFIX.append(text("Overwriting ", GRAY).append(text(path, GREEN))));
                            var file = new File(getDataFolder().getAbsoluteFile().getParentFile(), path);

                            file.getParentFile().mkdirs();

                            try (var out = new FileOutputStream(file)) {
                                stream.transferTo(out);
                            }
                        } else {
                            sender.sendMessage(PREFIX.append(text("Ignoring " + path, GRAY)));
                        }
                    }

                    sender.sendMessage(PREFIX.append(text("Finished sync in ", GRAY)).append(text((System.currentTimeMillis() - start) + " ms", GREEN)));
                } catch (IOException | InterruptedException | URISyntaxException e) {
                    sender.sendMessage(PREFIX.append(text("Something went wrong!", RED)));
                    e.printStackTrace();
                }
            });

            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("gitsync.reload")) {
                sender.sendMessage(PREFIX.append(text("Insufficient permissions (gitsync.reload)", RED)));
                return false;
            }

            reloadConfig();
            loadConfig();

            sender.sendMessage(PREFIX.append(text("Configuration reloaded!", GREEN)));
            return true;
        }

        return false;
    }
}
