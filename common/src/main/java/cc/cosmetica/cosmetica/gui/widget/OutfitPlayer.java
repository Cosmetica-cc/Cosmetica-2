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
import cc.cosmetica.cosmetica.gui.OutfitSelectScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.WIDTH;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.fixed;

public class OutfitPlayer extends Component {
	public OutfitPlayer(UUID player, String outfitName, NametagConfig lore) {
		this.player = player;
		this.outfitName = outfitName;
		this.lore = lore;
	}

	private final UUID player;
	private final String outfitName;
	private final NametagConfig lore;
	private final State<Boolean> showingElytra = new State<>(false);

	@Override
	public List<Component> build() {
		return Arrays.asList(
				new Div(
					new RotatableGUIPlayer(player, this.showingElytra).showNametag(true).addNametag(Text.literal(this.lore.getPrefix()), 0.75f).withStyle(Style.create().set(WIDTH, fixed(OptionalInt.of(50)))),
					new Label(Text.literal(this.outfitName)),
					new SlideToggle(
							this.showingElytra,
							Text.translatable("button.cosmetica.toggleCape"),
							Text.translatable("button.cosmetica.toggleElytra")),
					new Button(Text.translatable("button.cosmetica.changeOutfit"), () -> Screens.setScreen(OutfitSelectScreen.ID)),
//					new Button(Text.translatable("button.cosmetica.styleNametag"), () -> {
//						Screens.setScreen(StyleNametagScreen.ID);
//					}),
					// *.title ensures no ... for consistency with Cosmetica's buttons
					new Button(Text.translatable("options.skinCustomisation.title"), () -> {
						Minecraft.getInstance().setScreen(new SkinCustomizationScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
					})
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
