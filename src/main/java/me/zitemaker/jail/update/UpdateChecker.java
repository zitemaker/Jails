package me.zitemaker.jail.update;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpdateChecker {
    private static final int SPIGOTMC_RESOURCE_ID = 123183;
    private static final String API_BASE_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final int TIMEOUT_MS = 5000; // 5 seconds timeout

    private final URL apiUrl;
    private final Executor mainThreadExecutor;
    private final Logger logger;

    public UpdateChecker(JavaPlugin plugin) {
        JavaPlugin plugin1 = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.logger = plugin.getLogger();
        this.mainThreadExecutor = r -> plugin.getServer().getScheduler().runTask(plugin, r);

        URL tempUrl = null;
        try {
            tempUrl = new URL(API_BASE_URL + SPIGOTMC_RESOURCE_ID);
        } catch (final IOException ex) {
            logger.log(Level.SEVERE, "Failed to construct API URL for resource ID: " + SPIGOTMC_RESOURCE_ID, ex);
        }
        this.apiUrl = tempUrl;
    }

    public CompletableFuture<String> fetchRemoteVersion() {
        if (apiUrl == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) apiUrl.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(TIMEOUT_MS);
                        connection.setReadTimeout(TIMEOUT_MS);
                        connection.setUseCaches(false);

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            String version = reader.readLine();
                            if (version == null || version.trim().isEmpty()) {
                                logger.warning("Received empty version response from SpigotMC API");
                                return null;
                            }
                            return version.trim();
                        }
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Failed to fetch plugin version for resource ID: " + SPIGOTMC_RESOURCE_ID, ex);
                        return null;
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }).thenApplyAsync(Function.identity(), mainThreadExecutor)
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Unexpected error during version check", throwable);
                    return null;
                });
    }

    public CompletableFuture<Boolean> isUpdateAvailable(String currentVersion) {
        return fetchRemoteVersion()
                .thenApply(remoteVersion -> {
                    if (remoteVersion == null || currentVersion == null) {
                        return false;
                    }
                    return !currentVersion.trim().equals(remoteVersion.trim());
                });
    }

    private UpdateChecker() {
        throw new AssertionError("UpdateChecker should not be instantiated directly");
    }
}