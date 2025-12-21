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

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.api.*;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.*;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.cosmetica.util.Lore;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import cc.cosmetica.kupe.api.gui.Tooltip;
import cc.cosmetica.kupe.api.gui.style.Style;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.api.AuthApi;
import gg.cloaks.javaclient.api.OutfitsApi;
import gg.cloaks.javaclient.api.UsersApi;
import gg.cloaks.javaclient.model.UpdateLoreDto;
import gg.cloaks.javaclient.model.UserConnection;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

public class Cosmetica {
	// States showing the actual latest cosmetica server data
	public static final State<@Nullable Cosmetics> OWN_COSMETICS = new State<>(null);
	public static final State<List<OutfitWheelScreen.OutfitOption>> OWN_OUTFITS = new State<>(ImmutableList.of());
	public static final State<List<UserConnection>> OWN_CONNECTIONS = new State<>(ImmutableList.of());
	// Separate states that 'follow' the main state are maintained for cosmetic selections
	// to show visual updates faster than the C->S->C ping time.
	// These are prefixed with SELECTED_ to highlight this.
	public static final State<Optional<String>> SELECTED_OUTFIT_ID = new State<>(Optional.empty());
	public static final State<ImageCosmetic> SELECTED_ICON = new State<>(NametagConfig.NO_ICON);
	public static final State<Lore> SELECTED_LORE = new State<>(Lore.none(UpdateLoreDto.ColorEnum.WHITE));

	/**
	 * Cosmetic manager for when the user is offline.
	 */
	private static CacheCosmeticManager cacheCosmeticManager;

	public static CacheCosmeticManager getCacheCosmeticManager() { return cacheCosmeticManager; }

	public static void init() {
		Screens.setAllowDebug(true);

		Path cosmeticaConfigDir = CosmeticaCoreExpectPlatform.getConfigDirectory().resolve("cosmetica");
		Path cosmeticCacheDir = cosmeticaConfigDir.resolve("offlineCache");
		try {
			Files.createDirectories(cosmeticCacheDir);
		} catch (IOException e) {
			Logging.getInstance().error("Unable to create Cosmetica config directory", e);
		}
		cacheCosmeticManager = new CacheCosmeticManager(cosmeticCacheDir);
		CosmeticManagers.registerCosmeticManager(10, cacheCosmeticManager);

		// register gui accessory attachment
		GUIPlayer.registerAttachment(AccessoriesAttachment.INSTANCE);

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
			if (data == null) {
				Logging.getInstance().debug(CosmeticaLogCategory.EVENTS, "Own cosmetics cleared");
				Minecraft.getInstance().execute(() -> {
					OWN_COSMETICS.set(null);
				});
				return;
			}

			Logging.getInstance().debug(CosmeticaLogCategory.EVENTS, "Received own cosmetics");

			fetchOutfits();

			// pretty sure we should definitely be a user. is it possible for this code to run on cracked?
			List<UserConnection> connections;
			Lore userLore;

			if (data.isIsUser()) {
				// save data
				cacheCosmeticManager.save(data.getUser());

				// update settings
				if (data.getUser().getActiveSettings() != null) {
					CosmeticaSettings.updateSettings(data.getUser().getActiveSettings());
				} else {
					CosmeticaAPI.users().requestAsync(UsersApi::getSelf).thenAccept(user -> {
						if (user.getActiveSettings() != null) {
							CosmeticaSettings.updateSettings(user.getActiveSettings());
						}
					});
				}

				// load connections
				connections = data.getUser().getConnections();
				gg.cloaks.javaclient.model.Lore lore = data.getUser().getLore();
				userLore = lore == null ? Lore.none(UpdateLoreDto.ColorEnum.WHITE) : new Lore(
						lore.getContent(),
						UpdateLoreDto.ColorEnum.fromValue(lore.getColor().getValue()),
						lore.getIconUrl() == null ? CachedImage.NO_TEXTURE :
								CosmeticaModel.getOrCreateImage("lore", lore.getService(),
										new CosmeticaTexture.Builder(lore.getIconUrl(), FALLBACK_TEXTURE).frames(1, 1)),
						lore.getType() == gg.cloaks.javaclient.model.Lore.TypeEnum.CONNECTION ? lore.getService()
						: (lore.getType() == gg.cloaks.javaclient.model.Lore.TypeEnum.PRONOUNS ? Lore.PRONOUN_SERVICE : "")
				);
			} else {
				connections = ImmutableList.of();
				userLore = Lore.none(UpdateLoreDto.ColorEnum.WHITE);
			}

			Minecraft.getInstance().tell(() -> {
				OWN_COSMETICS.set(cosmetics);
				OWN_CONNECTIONS.set(connections);
				// can be updated by screens too.
				SELECTED_OUTFIT_ID.set(cosmetics.getOutfitId());
				SELECTED_ICON.set(cosmetics.getNametag().getIcon());
				SELECTED_LORE.set(userLore);
			});
		});
		// log in
		Authentication.authenticate();

		registerScreens();
	}

	// TODO better way to refresh outfits
	public static void fetchOutfits() {
		CosmeticaAPI.outfits().requestAsync(OutfitsApi::getOwn)
				.thenAccept(list -> Minecraft.getInstance().tell(() -> {
					OWN_OUTFITS.set(list.stream()
							.map(OutfitWheelScreen.OutfitOption::new)
							.collect(Collectors.toList()));
				}));
	}

	public static void openWebPanel(String targetPage) {
		CosmeticaAPI.auth().requestAsync(AuthApi::generateExchangeToken)
				.thenAccept(token -> {
					Cosmetica.copyAndOpenURL(System.getProperty("cosmetica.website", "https://cosmetica.cc") + "/login?token=" + token + "&state=" + targetPage);
				})
				.exceptionally(ex -> {
					Logging.getInstance().error("Unable to open " + targetPage + " page", ex);
					return null;
				});
	}

	public static void copyAndOpenURL(String url) {
		try {
			Minecraft.getInstance().keyboardHandler.setClipboard(url);
			Util.getPlatform().openUri(url);
		} catch (Exception e) {
			throw new RuntimeException("bruh", e);
		}
	}

	/*public static Style authTooltip(Component acquirer, boolean authenticated) {
		Optional<LoginResult> result = Authentication.LOGIN_RESULT.acquire(acquirer);

		return Style.create().set(TOOLTIP, authenticated ?
				Optional.empty() :
				Optional.of(new Tooltip(
                        result.map(loginResult -> Text.translatable("tooltip.cosmetica.offline." + loginResult.getCode().toString().toLowerCase(Locale.ROOT)))
								.orElseGet(() -> Text.translatable("tooltip.cosmetica.offline.offline"))
				)));
	}*/

	public static Style authTooltipStyle(boolean authenticated) {
		return Style.create().set(TOOLTIP, authenticated ?
				Optional.empty() :
				Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.offline"))));
	}

	public static Optional<Tooltip> authTooltip(boolean authenticated) {
		return authenticated ?
				Optional.empty() :
				Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.offline")));
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
		Screens.registerScreen(HomeScreen.ID, HomeScreen::new);
		Screens.registerScreen(BrowseScreen.ID, BrowseScreen::new);
		Screens.registerScreen(StyleNametagScreen.ID, StyleNametagScreen::new);
		Screens.registerScreen(OutfitSelectScreen.ID, new OutfitSelectScreen());
		Screens.registerScreen(CreateNewOutfitScreen.ID, CreateNewOutfitScreen::new);
	}

	public static <T> Function<T, Void> mainThreadExcept(Consumer<T> tConsumer) {
		return t -> {
			Minecraft.getInstance().execute(()->tConsumer.accept(t));
			return null;
		};
	}

	public static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("cosmetica", "icon.png");
	public static final ResourceLocation LOADING_TEXTURE = new ResourceLocation("cosmetica", "textures/loading.png");
}
