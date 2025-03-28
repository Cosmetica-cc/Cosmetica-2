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
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.*;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import gg.cloaks.javaclient.api.DefaultApi;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Cosmetica {
	// States showing the actual latest cosmetica server data
	public static final State<@Nullable Cosmetics> OWN_COSMETICS = new State<>(null);
	public static final State<List<OutfitWheelScreen.OutfitOption>> OWN_OUTFITS = new State<>(ImmutableList.of());
	// Separate states that 'follow' the main state are maintained for cosmetic selections
	// to show visual updates faster than the C->S->C ping time.
	// These are prefixed with SELECTED_ to highlight this.
	public static final State<Optional<String>> SELECTED_OUTFIT_ID = new State<>(Optional.empty());
	public static final State<ImageCosmetic> SELECTED_ICON = new State<>(NametagConfig.NO_ICON);
	public static final State<String> SELECTED_LORE = new State<>("");

	public static void init() {
		Screens.setAllowDebug(true);

		// cosmetic states
		Cosmetics.registerCosmeticsChangeCallback((le, cosmetics) -> {
			if (le instanceof Player) {
				Minecraft.getInstance().tell(() -> {
					((StateHolder) le).cosmetica$setCosmeticState(cosmetics);
				});
			}
		});
		// updates to cosmetic stuff
		Cosmetics.registerUserDataFetchCallback((data, cosmetics) -> {
			Logging.getInstance().debug("Received own cosmetics");

			CosmeticaAPI.performAsync(DefaultApi::outfitsControllerGetOwn)
					.thenAccept(list -> Minecraft.getInstance().tell(() -> {
						OWN_OUTFITS.set(list.stream()
								.map(OutfitWheelScreen.OutfitOption::new)
								.collect(Collectors.toList()));
					}));

			Minecraft.getInstance().tell(() -> {
				OWN_COSMETICS.set(cosmetics);
				// can be updated by screens too.
				SELECTED_OUTFIT_ID.set(cosmetics.getOutfitId());
				SELECTED_ICON.set(cosmetics.getNametag().getIcon());
				SELECTED_LORE.set(cosmetics.getLore().orElse(NametagConfig.EMPTY).getPrefix());
			});
		});
		// log in
		Authentication.authenticate();

		registerScreens();
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
	 * Register (some) Cosmetica screens.
	 */
	private static void registerScreens() {
		// Use persistent screen object to keep state data
		// - we want to keep track of what the user was last doing
		Screens.registerScreen(CosmeticaHomeScreen.ID, new CosmeticaHomeScreen());
		Screens.registerScreen(StyleNametagScreen.ID, StyleNametagScreen::new);
		Screens.registerScreen(OutfitSelectScreen.ID, new OutfitSelectScreen());
	}
}
