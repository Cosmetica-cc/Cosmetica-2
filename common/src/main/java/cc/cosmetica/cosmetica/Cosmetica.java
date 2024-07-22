/*
 * Copyright 2024 Cosmetica
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

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.CosmeticaSession;
import cc.cosmetica.core.impl.Logging;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

public class Cosmetica {
	private static final Path CACHE_DIRECTORY;

	static {
		Path minecraftDir = findDefaultInstallDir("minecraft");

		if (Files.isDirectory(minecraftDir)) {
			CACHE_DIRECTORY = minecraftDir.resolve(".cosmetica");
		} else {
			CACHE_DIRECTORY = CosmeticaCoreExpectPlatform.getGameDirectory().resolve(".cosmetica");
		}

		// ensure it's made
		BlockModelManager.getLocation("dummy");
		// TODO maybe expose the cache directory field internally in core, or use an accessor
	}

	public static void init() {
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

	/*
	 * Adapted from code at https://github.com/FabricMC/fabric-installer
	 * Original license has been preserved for this method.
	 *
	 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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
	private static Path findDefaultInstallDir(String application) {
		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		Path dir;

		if (os.contains("win") && System.getenv("APPDATA") != null) {
			dir = Paths.get(System.getenv("APPDATA")).resolve("." + application);
		} else {
			String home = System.getProperty("user.home", ".");
			Path homeDir = Paths.get(home);

			if (os.contains("mac")) {
				dir = homeDir.resolve("Library").resolve("Application Support").resolve(application);
			} else {
				dir = homeDir.resolve("." + application);
			}
		}

		return dir.toAbsolutePath().normalize();
	}

	private static void startAuthentication() throws IOException {
		// check for cached token
		Path sessionsInfo = CACHE_DIRECTORY.resolve(".sessions");

		if (Files.isRegularFile(sessionsInfo)) {
			Properties properties = new Properties();

			try (BufferedInputStream b = new BufferedInputStream(Files.newInputStream(sessionsInfo))) {
				properties.load(b);
			}

			User user = Minecraft.getInstance().getUser();
			String token = properties.getProperty("jwt-" + user.getUuid());

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
		} else {
			Files.createFile(sessionsInfo);
		}

		// Log in
		// TODO switch to an executor?
		Thread t = new Thread(() -> {
			try {
				if (CosmeticaAPI.login()) {
					String token = CosmeticaAPI.getSessionToken();
					User user = Minecraft.getInstance().getUser();

					// Cache Token
					if (!token.isEmpty()) { // we are using async code, so near-redundant operation just in case.
						Properties properties = new Properties();
						properties.setProperty("jwt-" + user.getUuid(), token);

						try (BufferedOutputStream b = new BufferedOutputStream(Files.newOutputStream(sessionsInfo))) {
							properties.store(b, "Cosmetica Session Info");
						}
					}
				}
			} catch (IOException e) {
				Logging.getInstance().error("Failed to log in", e);
			}
		});
		t.setName("Cosmetica Login Worker");
		t.start();
	}
}
