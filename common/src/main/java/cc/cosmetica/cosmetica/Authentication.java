/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.cosmetica;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.LoginResult;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gg.cloaks.javaclient.ApiException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static cc.cosmetica.cosmetica.settings.CosmeticaSettings.willApplyLocalSettings;

/**
 * Handles authentication.
 */
public final class Authentication {
    private static final ResourceLocation SESSIONS = new ResourceLocation("cosmetica", ".sessions");
    private static final ScheduledExecutorService LOGIN_SCHEDULER = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        private int counter = 1;

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r);
            t.setName("Cosmetica Login Worker " + (counter++));
            return t;
        }
    });
    private static volatile boolean authenticating = false;
    private static final AtomicInteger RETRIES = new AtomicInteger(0);
    private static final Object lock = new Object();
    public static final State<Optional<LoginResult>> LOGIN_RESULT = new State<>(Optional.empty());
    private static long lastInvalidation = System.currentTimeMillis() - 1000L;

    public static final AtomicBoolean everAuthenticated = new AtomicBoolean(false);
    public static final AtomicBoolean showedUnauthenticatedToast = new AtomicBoolean(false);

    static void authenticate() {
        // download current settings and update settings on authentication change
        CosmeticaAPI.addAuthenticationChangeCallback(reason -> {
            // clear settings when deauthenticating
            if (!CosmeticaAPI.isAuthenticated()) {
                Minecraft.getInstance().execute(CosmeticaSettings::clearSettings);

                // delete invalid tokens
                if (reason == CosmeticaAPI.AuthChangeReason.ERROR_401) {
                    // de-duplicate invalidations for subsequent blind api calls
                    synchronized (lock) {
                        if (System.currentTimeMillis() - lastInvalidation > 1000L) {
                            invalidateToken();
                        } else {
                            Logging.getInstance().debug(CosmeticaLogCategory.LOGIN, "Skipping token invalidation as token already invalidated within last second.");
                        }
                        lastInvalidation = System.currentTimeMillis();
                    }
                }
            }

            // Allow manual token setting for testing
            if (!System.getProperties().containsKey("cosmetica.token")) {
                boolean startAuth = false;

                synchronized (lock) {
                    // Try re-login when deauthenticated, and clear self cosmetics if cannot reauthenticate
                    if (!authenticating && !CosmeticaAPI.isAuthenticated()) {
                        authenticating = true;
                        startAuth = true;
                    } else if (CosmeticaAPI.isAuthenticated()) {
                        RETRIES.set(0);
                        authenticating = false;
                        everAuthenticated.set(true);

                        if (showedUnauthenticatedToast.compareAndSet(true, false)) {
                            Cosmetica.showToast(
                                    Text.translatable("toast.cosmetica.reconnected"),
                                    null
                            );
                        }
                    }
                }

                if (startAuth) {
                    startAuthentication();
                }
            }
        });

        // cosmetica.token is used by core as for testing. we want to keep this behaviour for our testing.
        if (!System.getProperties().containsKey("cosmetica.token")) {
            authenticating = true;
            startAuthentication();
        }
    }

    /**
     * Start authenticating the mod with Cosmetica. Preferably uses the cached token for the current user.
     */
    private static void startAuthentication() {
        // check for cached token
        Path sessionsInfo = BlockModelManager.getCacheFile(SESSIONS, null);
        Properties properties = new Properties();

        boolean login = false;
        try {
            login = logInFromCache(sessionsInfo, properties);
        } catch (IOException e) {
            Logging.getInstance().error("Failed to log into Cosmetica via cache", e);
        }

        if (!login) {
            LOGIN_SCHEDULER.schedule(
                    () -> Authentication.repeatLogInFromApi(sessionsInfo, properties),
                    0,
                    TimeUnit.SECONDS
            );
        }
    }

    private static void invalidateToken() {
        Logging.getInstance().info("Cosmetica authentication has expired. Will reauthenticate!");

        // Get file location and ensure it's valid
        Path sessionsInfo = BlockModelManager.getCacheFile(SESSIONS, null);
        Properties properties = new Properties();

        if (!Files.isRegularFile(sessionsInfo)) {
            Logging.getInstance().warn("Tried to invalidate token but sessions path doesn't exist");
            return;
        }

        // Load file
        try (BufferedInputStream b = new BufferedInputStream(Files.newInputStream(sessionsInfo))) {
            properties.load(b);
        } catch (IOException e) {
            Logging.getInstance().error("Failed to load cosmetica sessions", e);
            return;
        }

        // Remove property
        User user = Minecraft.getInstance().getUser();
        String tokenKey = jwtKey(user.getUuid());
        properties.remove(tokenKey);

        // Store
        try (BufferedOutputStream boss = new BufferedOutputStream(Files.newOutputStream(sessionsInfo))) {
            properties.store(boss, "Cosmetica Session Info");
            Logging.getInstance().debug(CosmeticaLogCategory.LOGIN, "Invalidated token");
        } catch (IOException e) {
            Logging.getInstance().error("Failed to save cosmetica sessions", e);
        }
    }

    private static String jwtKey(String uuid) {
        return "jwt-" + uuid.replace("-", "");
    }

    private static String jwtKey(UUID uuid) {
        return "jwt-" + uuid.toString().replace("-", "");
    }

    private static void repeatLogInFromApi(Path sessionsInfo, Properties properties) {
        if (!Authentication.logInFromApi(sessionsInfo, properties)) {
            final int[] retryCounts = new int[]{1, 5, 10, 30, 60};

            int retries = RETRIES.getAndIncrement();
            if (retries >= retryCounts.length) {
                Logging.getInstance().info("Retrying cosmetica login in {} seconds", retryCounts[retryCounts.length - 1]);
                LOGIN_SCHEDULER.schedule(
                        () -> Authentication.repeatLogInFromApi(sessionsInfo, properties),
                        retryCounts[retryCounts.length - 1],
                        TimeUnit.SECONDS
                );
            } else {
                if (retries == 2) {
                    Logging.getInstance().debug(LoggingCategory.COSMETICS, "Clearing cosmetics due to 2 failed retries.");
                    SelfCosmeticManager.clear();
                }

                Logging.getInstance().info("Retrying cosmetica login in {} seconds", retryCounts[retries]);
                LOGIN_SCHEDULER.schedule(
                        () -> Authentication.repeatLogInFromApi(sessionsInfo, properties),
                        retryCounts[retries],
                        TimeUnit.SECONDS
                );
            }
        }
    }

    /**
     * Try authenticating the mod using the cached token for the current user.
     * @param sessionInfoPath the path to the cache file.
     * @param sessionInfo the properties file to load into.
     * @return whether the login was successful.
     * @throws IOException if an IOException occurs while trying to access the session info.
     */
    private static boolean logInFromCache(Path sessionInfoPath, Properties sessionInfo) throws IOException {
        if (Files.isRegularFile(sessionInfoPath)) {
            try (BufferedInputStream b = new BufferedInputStream(Files.newInputStream(sessionInfoPath))) {
                sessionInfo.load(b);
            }

            User user = Minecraft.getInstance().getUser();
            String token = sessionInfo.getProperty(jwtKey(user.getUuid()));

            if (token != null) {
                // parse jwt to check if expired
                try {
                    byte[] info = Base64.getDecoder().decode(token.split("\\.")[1]);
                    JsonObject object = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(info))).getAsJsonObject();
                    // get timestamp of expiry
                    String exp = object.get("exp").getAsString();

                    if (Long.parseLong(exp) - Instant.now().getEpochSecond() > 0) {
                        // use cached jwt
                        Logging.getInstance().debug(CosmeticaLogCategory.LOGIN, "Using cached JWT for auth");
                        CosmeticaAPI.authenticate(token, "Cosmetica Official Mod", !willApplyLocalSettings(), CosmeticaSettings.MODPACK_ID.peek());
                        return true;
                    }
                } catch (JsonParseException | IndexOutOfBoundsException e) {
                    throw new RuntimeException("Malformed JWT", e);
                }
            }
        } else {
            Files.createDirectories(sessionInfoPath.getParent());
            Files.createFile(sessionInfoPath);
        }

        return false;
    }

    /**
     * Try log in with the api.
     * @param sessionInfoPath the path to the file to store session info in.
     * @param sessionInfo the properties data in which to store session info.
     * @return whether the current login was a success.
     */
    private static boolean logInFromApi(Path sessionInfoPath, Properties sessionInfo) {
        Logging.getInstance().debug(CosmeticaLogCategory.LOGIN, "Logging in to Cosmetica...");

        try {
            LoginResult result = CosmeticaAPI.login("Cosmetica Official Mod", !willApplyLocalSettings(), CosmeticaSettings.MODPACK_ID.peek());
            Logging.getInstance().debug(CosmeticaLogCategory.LOGIN, "LoginResult received");

            Minecraft.getInstance().execute(() -> {
                LoginResult message = result;
                if (result.getException().isPresent() && result.getException().get() instanceof ApiException) {
                    if (result.getException().get().getCause() instanceof UnknownHostException) {
                        // internally represent no internet := "success" but success:false
                        message = new LoginResult(false, LoginResult.Code.SUCCESS, result.getMessage(), (UnknownHostException)result.getException().get().getCause());
                    }
                }
                if (!LOGIN_RESULT.peek().isPresent()
                        || LOGIN_RESULT.peek().get().getCode() != message.getCode()
                        || LOGIN_RESULT.peek().get().isSuccess() != message.isSuccess()) {
                    LOGIN_RESULT.set(Optional.of(message));
                }
            });
            if (result.isSuccess()) {
                String token = CosmeticaAPI.getSessionToken(); // will only be empty if someone deauthenticated in between
                User user = Minecraft.getInstance().getUser();

                // Cache Token
                if (!token.isEmpty()) { // we are using async code, so near-redundant operation just in case.
                    sessionInfo.setProperty(jwtKey(user.getUuid()), token);

                    try (BufferedOutputStream b = new BufferedOutputStream(Files.newOutputStream(sessionInfoPath))) {
                        sessionInfo.store(b, "Cosmetica Session Info");
                    }
                }

                return true;
            }
        } catch (UnknownHostException e) {
            Logging.getInstance().error("Failed to log in", e);
            Minecraft.getInstance().execute(() -> {
                // internally represent no internet := "success" but success:false
                LOGIN_RESULT.set(Optional.of(new LoginResult(false, LoginResult.Code.SUCCESS, e.getMessage(), e)));
            });
        } catch (IOException e) {
            Logging.getInstance().error("Failed to log in", e);
        }

        return false;
    }
}
