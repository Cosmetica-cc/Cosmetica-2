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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.core.api.*;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.cosmetica.gui.widget.*;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticaHomeScreen extends Screen {
	public CosmeticaHomeScreen() {
		super(ID);
	}

	@Override
	protected Component[] buildScreen() {
		UUID self = Minecraft.getInstance().getUser().getGameProfile().getId();

		Cosmetics cosmetics = Cosmetica.OWN_COSMETICS.acquire(this);
		boolean authenticated = CosmeticaAPI.isAuthenticated();

		List<CosmeticEntry> entries = new ArrayList<>();
		populateEntryList(entries, cosmetics, authenticated ? 2 : 1);

		return new Component[] {
				new Div(
						new LayeredSpace(true,
								new OutfitPlayer(self,
										authenticated,
										Optional.ofNullable(cosmetics).flatMap(Cosmetics::getOutfitName).orElse("§7No Outfit"),
										Optional.ofNullable(cosmetics).flatMap(Cosmetics::getLore).orElse(NametagConfig.EMPTY),
										Optional.ofNullable(cosmetics).map(Cosmetics::getNametag).orElse(NametagConfig.EMPTY)),
								new Div(
										new IconButton(
												new ResourceKey("cosmetica", "textures/gear.png"),
												() -> Screens.setScreen(new CosmeticaSettingsScreen(CosmeticaSettingsScreen.SETTINGS_SCREEN, Setting.SETTINGS), CosmeticaSettingsScreen.SETTINGS_SCREEN)),
										new IconButton(
												new ResourceKey("minecraft", "textures/item/name_tag.png"),
												() -> Screens.setScreen(StyleNametagScreen.ID))
												.setDisabled(!authenticated)
												.withStyle(Cosmetica.authTooltip(authenticated))
								).withStyle(Style.create()
										.set(Div.ALIGN_ITEMS, Align.START)
										.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
						).tag("main-section"),
						new CosmeticsBrowser(entries, true).tag("main-section")
				).tag("main-content"),
				new MenuEndSelection()
		};
	}

	/**
	 * Create the GUI cosmetic list entries for each cosmetic the player is wearing.
	 * @param entryList the list to populate.
	 * @param cosmetics the cosmetics the player is wearing.
	 * @param editable whether cosmetics are editable. Can be 0 (not editable) 1 (editable) or 2 (offline).
	 */
	static void populateEntryList(final List<CosmeticEntry> entryList, Cosmetics cosmetics, int editable) {
		if (cosmetics == null)
			return; // no cosmetics

		// (wip) this should only show API cosmetics and only allow editing if own cosmetics.
		// some kind of notification if no internet
		// this also means for local player, even when null, we need to handle backup cosmetics no?

		boolean showSeparateElytra = true;
		if (cosmetics.getCloak().isPresent()) {
			ImageCosmetic cloak = cosmetics.getCloak().get();

			String message = "Cloak";
			if (cloak.getId().equals(cosmetics.getElytra().map(ImageCosmetic::getId).orElse(null))) {
				message = "Cloak + Elytra";
				showSeparateElytra = false;
			}

			entryList.add(new CosmeticEntry(
					cosmetics,
					getOrCreateThumb(cloak.getThumbnail(), "thumbs-c", cloak.getId(), 3), // TODO in core give ticks per frame (expose AnimatedTextureCosmetic)
					cloak.getId(),
					cloak.getName(),
					message, //cloak.getCreator().isPresent() ? cloak.getCreator().get().getName() : "Could not load creator"
					editable,
					CosmeticEntry.Category.CAPE
			));
		}

		if (showSeparateElytra && cosmetics.getElytra().isPresent()) {
			ImageCosmetic elytra = cosmetics.getElytra().get();

			entryList.add(new CosmeticEntry(
					cosmetics,
					getOrCreateThumb(elytra.getThumbnail(), "thumbs-c", elytra.getId(), 3), // TODO in core give ticks per frame (expose AnimatedTextureCosmetic)
					elytra.getId(),
					elytra.getName(),
					"Elytra", //elytra.getCreator().isPresent() ? elytra.getCreator().get().getName() : "Could not load creator"
					editable,
					CosmeticEntry.Category.CAPE
			));
		}

		for (Accessory accessory : cosmetics.getAccessories()) {
			// texture for thumbnail
			CachedImage thumbnail = getOrCreateThumb(accessory.getThumbnail(), "thumbs-a", accessory.getId(), accessory.getJsonObject().getTicksPerFrame().intValue());

			// n.b. reference to CachedImage needs to be stored on the entry so it doesn't get GC'd
			entryList.add(new CosmeticEntry(
					cosmetics,
					thumbnail,
					accessory.getId(),
					accessory.getName(),
					accessory.getCreator().isPresent() ? accessory.getCreator().get().getName() : "Could not load creator",
					editable,
					CosmeticEntry.Category.ACCESSORY
			));
		}
	}

	private static CachedImage getOrCreateThumb(@Nullable String thumbnail, String category, String id, int ticksPerFrame) {
		if (thumbnail == null) {
			return NO_THUMBNAIL;
		} else {
			return ThumbnailCache.getOrCreateImage(category, id,
					new CosmeticaTexture.Builder(thumbnail, Cosmetica.LOADING_TEXTURE)
							.frames(8, ticksPerFrame)
							.failToLoadTexture(Cosmetica.FALLBACK_TEXTURE)
							.autoAnimate(CosmeticaTexture.AutoAnimate.NEVER_TILESHEETS));
		}
	}

	@Override
	public @NotNull Stylesheet getStylesheet() {
		return super.getStylesheet()
				.tag("main-content", Style.create()
						.set(FLEX, 1)
						.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
						.set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
						.set(Div.ALIGN_ITEMS, Align.CENTRE))
				.tag("main-section", Style.create()
						.set(WIDTH, screen(50, 0))
						.set(HEIGHT, percent(0, 100)));
	}

	public static final ResourceKey ID = new ResourceKey("cosmetica", "home");
	public static final CachedImage NO_THUMBNAIL = new CachedImage(Cosmetica.FALLBACK_TEXTURE, 0);
}
