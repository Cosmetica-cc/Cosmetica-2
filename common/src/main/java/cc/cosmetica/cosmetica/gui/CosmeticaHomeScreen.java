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
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.cosmetica.gui.widget.*;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Region;
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
		List<CosmeticEntry> entryList = new ArrayList<>();
		populateEntryList(entryList, cosmetics);

		return new Component[] {
				new Div(
						new LayeredSpace(true,
								new OutfitPlayer(self, Optional.ofNullable(cosmetics).flatMap(Cosmetics::getOutfitName).orElse("§7No Outfit")),
								new Div(
										new IconButton(
												new ResourceKey("cosmetica", "textures/gear.png"),
												() -> Screens.setScreen(new CosmeticaSettingsScreen(CosmeticaSettingsScreen.SETTINGS_SCREEN, Setting.SETTINGS), CosmeticaSettingsScreen.SETTINGS_SCREEN))
								).withStyle(Style.create()
										.set(Div.ALIGN_ITEMS, Align.START))
						).tag("main-section"),
						new CosmeticsBrowser(entryList, true).tag("main-section")
				).tag("main-content"),
				new MenuEndSelection()
		};
	}

	/**
	 * Create the GUI cosmetic list entries for each cosmetic the player is wearing.
	 * @param entryList the list to populate.
	 * @param cosmetics the cosmetics the player is wearing.
	 */
	static void populateEntryList(final List<CosmeticEntry> entryList, Cosmetics cosmetics) {
		if (cosmetics == null)
			return; // no cosmetics

		// TODO this should only show API cosmetics no? Or at least only allow editing if controlling manager is self.
		// some kind of notification if no internet
		// this also means for local player, even when null, we need to handle backup cosmetics no?

		if (cosmetics.getCloak().isPresent()) {
			ImageCosmetic cosmetic = cosmetics.getCloak().get();

			entryList.add(new CosmeticEntry(
					new ResourceKey("cosmetica", "icon.png"),//TODO replace this with cosmetica loading & swap on load? or do it in the texture itself
					cosmetic.getId(),
					cosmetic.getName(),
					cosmetic.getCreator().isPresent() ? cosmetic.getCreator().get().getName() : "Could not load creator"
			));
		}

		for (Accessory accessory : cosmetics.getAccessories()) {
			// todo settings can maybe be passed as a builder (core)
			CachedImage thumbnail = CosmeticaModel.getOrCreateImage("thumbs-a", accessory.getId(), accessory.getThumbnail(), 8, 3);

			// n.b. reference to CachedImage needs to be stored on the entry so it doesn't get GC'd
			entryList.add(new CosmeticEntry(
					thumbnail,
					accessory.getId(),
					accessory.getName(),
					accessory.getCreator().isPresent() ? accessory.getCreator().get().getName() : "Could not load creator"
			));
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
}
