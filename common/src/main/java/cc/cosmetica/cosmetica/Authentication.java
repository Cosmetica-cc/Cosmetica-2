package cc.cosmetica.cosmetica;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.Logging;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

/**
 * Handles authentication.
 */
public final class Authentication {
    private static final ResourceLocation SESSIONS = new ResourceLocation("cosmetica", ".sessions");

    static void authenticate() {
        // cosmetica.token is used by core as for testing. we want to keep this behaviour for our testing.
        if (!System.getProperties().containsKey("cosmetica.token")) {
            // log in
            try {
                startAuthentication();
            } catch (IOException e) {
                Logging.getInstance().error("Failed to log into Cosmetica", e);
            }
        }
    }

    /**
     * Start authenticating the mod with Cosmetica. Preferably uses the cached token for the current user.
     * @throws IOException if an IOException occurs while trying to access the session info.
     */
    private static void startAuthentication() throws IOException {
        // check for cached token
        Path sessionsInfo = BlockModelManager.getCacheFile(SESSIONS);
        Properties properties = new Properties();

        if (Files.isRegularFile(sessionsInfo)) {
            try (BufferedInputStream b = new BufferedInputStream(Files.newInputStream(sessionsInfo))) {
                properties.load(b);
            }

            User user = Minecraft.getInstance().getUser();
            String token = properties.getProperty("jwt-" + user.getUuid());

            if (token != null) {
                // parse jwt to check if expired
                try {
                    byte[] info = Base64.getDecoder().decode(token.split("\\.")[1]);
                    JsonObject object = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(info))).getAsJsonObject();
                    // get timestamp of expiry
                    String exp = object.get("exp").getAsString();

                    if (Long.parseLong(exp) - Instant.now().getEpochSecond() > 0) {
                        // use cached jwt
                        CosmeticaAPI.authenticate(token);
                        return;
                    }
                } catch (JsonParseException | IndexOutOfBoundsException e) {
                    throw new RuntimeException("Malformed JWT", e);
                }
            }
        } else {
            Files.createFile(sessionsInfo);
        }

        // Log in
        // TODO switch to an executor?
        Thread t = new Thread(() -> Authentication.logIn(sessionsInfo, properties));
        t.setName("Cosmetica Login Worker");
        t.start();
    }

    private static void logIn(Path sessionInfoPath, Properties sessionInfo) {
        try {
            if (CosmeticaAPI.login()) {
                String token = CosmeticaAPI.getSessionToken();
                User user = Minecraft.getInstance().getUser();

                // Cache Token
                if (!token.isEmpty()) { // we are using async code, so near-redundant operation just in case.
                    sessionInfo.setProperty("jwt-" + user.getUuid(), token);

                    try (BufferedOutputStream b = new BufferedOutputStream(Files.newOutputStream(sessionInfoPath))) {
                        sessionInfo.store(b, "Cosmetica Session Info");
                    }
                }
            }
        } catch (IOException e) {
            Logging.getInstance().error("Failed to log in", e);
        }
    }
}
