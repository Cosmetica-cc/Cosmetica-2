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
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.CosmeticaHomeScreen;
import cc.cosmetica.cosmetica.gui.CosmeticaSettingsScreen;
import cc.cosmetica.cosmetica.gui.StyleNametagScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public class Cosmetica {
	public static final State<@Nullable Cosmetics> OWN_COSMETICS = new State<>(null);
	private static final ResourceLocation SESSIONS = new ResourceLocation("cosmetica", ".sessions");

	public static void init() {
		Screens.setAllowDebug(true);

		// cosmetic states
		Cosmetics.registerCosmeticsChangeCallback((le, cosmetics) -> {
			if (le == null) {
				System.out.println("Received own cosmetics");
				Minecraft.getInstance().tell(() -> {
					OWN_COSMETICS.set(cosmetics);
				});
			} else if (le instanceof Player) {
				Minecraft.getInstance().tell(() -> {
					((StateHolder) le).cosmetica$setCosmeticState(cosmetics);
				});
			}
		});

		// cosmetica.token is used by core as for testing. we want to keep this behaviour for our testing.
		if (!System.getProperties().containsKey("cosmetica.token")) {
			// log in
			try {
				startAuthentication();
			} catch (IOException e) {
				Logging.getInstance().error("Failed to log into Cosmetica", e);
			}
		}

		registerScreens();
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
		Thread t = new Thread(() -> {
			try {
				if (CosmeticaAPI.login()) {
					String token = CosmeticaAPI.getSessionToken();
					User user = Minecraft.getInstance().getUser();

					// Cache Token
					if (!token.isEmpty()) { // we are using async code, so near-redundant operation just in case.
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

	/**
	 * Based on NativeImage#load (lambda method_22801)
	 */
	public static void downloadWebpToPng(String source, @NotNull File destination,
										 Function<InputStream, NativeImage> load, Consumer<NativeImage> onLoad) {
		HttpURLConnection connection = null;
		Logging.getInstance().debug("WEBP: Downloading {} to {}", source, destination);

		try {
			connection = (HttpURLConnection)(new URL(source))
					.openConnection(Minecraft.getInstance().getProxy());
			connection.setDoInput(true);
			connection.setDoOutput(false);
			connection.connect();

			if (connection.getResponseCode() / 100 == 2) {
				// Cosmetica: Transform Webp to Png
				// https://github.com/haraldk/TwelveMonkeys?tab=readme-ov-file#advanced-usage
				BufferedImage image;

				try (ImageInputStream input = ImageIO.createImageInputStream(connection.getInputStream())) {
					Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

					if (!readers.hasNext()) {
						throw new IllegalArgumentException("No reader for input");
					}

					ImageReader reader = readers.next();

					try {
						reader.setInput(input);
						image = reader.read(0);
					} finally {
						// avoid memory leaks
						reader.dispose();
					}
				}

				// crop cosmetica thumbnails!
				if (source.contains("cloaks.gg")) {
					image = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
				}

				// successful read
				Files.createDirectories(destination.getParentFile().toPath());
				ImageIO.write(image, "png", destination);

				InputStream inputStream = new FileInputStream(destination);

				Minecraft.getInstance().execute(() -> {
					NativeImage nativeImage = load.apply(inputStream);

					if (nativeImage != null) {
						onLoad.accept(nativeImage);
					}
				});
			}
		} catch (Exception exception) {
			Logging.getInstance().error("Couldn't download WEBP texture at " + source, exception);
		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	public static void openWebPanel() {
		// todo login to website
		copyAndOpenURL("https://cosmetica.cc/home");
	}

	public static void copyAndOpenURL(String url) {
		try {
			Minecraft.getInstance().keyboardHandler.setClipboard(url);
			Util.getPlatform().openUri(url);
		} catch (Exception e) {
			throw new RuntimeException("bruh", e);
		}
	}

	// ============== //
	//  Registration  //
	// ============== //

	/**
	 * Register Cosmetica's screens.
	 */
	private static void registerScreens() {
		// Use persistent screen object to keep state data
		// - we want to keep track of what the user was last doing
		// - this can be changed at any time by switching to the factory registerScreen
		Screens.registerScreen(CosmeticaHomeScreen.ID, new CosmeticaHomeScreen());
		Screens.registerScreen(CosmeticaSettingsScreen.ID, new CosmeticaSettingsScreen());
		Screens.registerScreen(StyleNametagScreen.ID, new StyleNametagScreen());
	}
}
