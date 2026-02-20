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

package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.OutfitSelectScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class OutfitPlayer extends Component {
	public OutfitPlayer(UUID player, boolean authenticated, String outfitName, NametagConfig lore, NametagConfig nametag) {
		this.player = player;
		this.outfitName = outfitName;
		this.lore = lore;
		this.nametag = nametag;
		this.authenticated = authenticated;
	}

	private final UUID player;
	private final boolean authenticated;
	private final String outfitName;
	private final NametagConfig lore;
	private final NametagConfig nametag;
	// should only be one OutfitPlayer at a time. Share the setting between instances and keep it preserved
	public static final State<Boolean> showingElytra = new State<>(false);

	private boolean disable = false;
	private UnaryOperator<GUIPlayer> overrides = gp -> gp;
	private RotatableGUIPlayer guiPlayer;
	private boolean keepGuiPlayer;
	private int loreHandle;

	public OutfitPlayer setDisabled(boolean disabled) {
		this.disable = disabled;
		return this;
	}

	public OutfitPlayer configureOverrides(UnaryOperator<GUIPlayer> overrides) {
		this.overrides = overrides;
		return this;
	}

	public OutfitPlayer keepGuiPlayer() {
		this.keepGuiPlayer = true;
		return this;
	}

	@Override
	public List<Component> build() {
		RotatableGUIPlayer guiPlayer;
		if (this.keepGuiPlayer && this.guiPlayer != null) {
			guiPlayer = this.guiPlayer;
			guiPlayer.updateNametag(this.loreHandle, Text.literal(this.lore.getPrefix()), 0.75f);
		} else {
			guiPlayer = new RotatableGUIPlayer(player, showingElytra);
			guiPlayer.showNametag(true);
			this.loreHandle = guiPlayer.createNametag(Text.literal(this.lore.getPrefix()), 0.75f);
		}

		// set icons
		guiPlayer.icon(nametag.getIcon().getImage().isLoaded() ? nametag.getIcon().getImage() : null, nametag.isTransparentIcon())
				.loreIcon(lore.getIcon().getImage().isLoaded() ? lore.getIcon().getImage() : null);

		return Arrays.asList(
				new Div(
					// balance appearance on small resolutions by shifting everything down slightly
					new Div().withStyle(Style.create().set(HEIGHT, fixedSize(20))),
					this.overrides.apply(guiPlayer).withStyle(Style.create()
							.set(MIN_WIDTH, fixedSize(50))
							.set(MAXIMUM_SIZE, fixed(new Dimensions(90, 1000)))
							.set(WIDTH, (vw, vh, pw, ph) -> OptionalInt.of(10 + (int)(vw * 0.0625)))),
					new Label(Text.literal(this.outfitName)),
					new SlideToggle(
							showingElytra,
							Text.translatable("button.cosmetica.toggleCloak"),
							Text.translatable("button.cosmetica.toggleElytra")),
					new Button(Text.translatable("button.cosmetica.changeOutfit"), () -> Screens.setScreen(OutfitSelectScreen.ID))
							.setDisabled(!authenticated || disable)// hide tooltip if just disabled
							.withStyle(Cosmetica.authTooltipStyle(disable||authenticated)),
					// *.title ensures no "..." for consistency with Cosmetica's buttons
					new Button(Text.translatable("options.skinCustomisation.title"), () -> {
						Minecraft.getInstance().setScreen(new SkinCustomizationScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
					}).setDisabled(disable)
				).withStyle(Style.create()
						.set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
						.set(Div.ALIGN_ITEMS, Align.CENTRE))
		);
	}

	@Override
	public Stylesheet getStylesheet() {
		return new Stylesheet()
				.component(Button.class, Style.create()
						.set(WIDTH, fixed(OptionalInt.of(150)))
				);
	}
}
