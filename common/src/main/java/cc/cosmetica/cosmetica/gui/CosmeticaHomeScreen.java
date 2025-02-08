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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.CosmeticsBrowser;
import cc.cosmetica.cosmetica.gui.widget.OutfitPlayer;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.gui.Align;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.Justify;
import cc.cosmetica.kupe.api.gui.style.CommonProperties;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticaHomeScreen extends Screen {
	public CosmeticaHomeScreen() {
		super(ID);
	}

	@Override
	protected Component[] buildScreen() {

		UUID cosmetics = Minecraft.getInstance().getUser().getGameProfile().getId();

		return new Component[] {
				new OutfitPlayer(cosmetics)
						.withStyle(Style.create().set(CommonProperties.WIDTH, percent(0.5f, 0))),
				new CosmeticsBrowser(Arrays.asList(
						new CosmeticEntry(
								new ResourceLocation("cosmetica:icon.png"),
								"asdfasdf",
								"Cosmetica",
								"Valoeghese"
						)
				)).withStyle(Style.create().set(CommonProperties.WIDTH, percent(0.5f, 0)))
		};
	}

	@Override
	public @Nullable Stylesheet getStylesheet() {
		return new Stylesheet()
				.tag("title", TITLE_DEFAULT_STYLE)
				.tag("body", Style.create()
						.set(CommonProperties.WIDTH, SCREEN_WIDTH)
						.set(CommonProperties.HEIGHT, SCREEN_HEIGHT)
						.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
						.set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
						.set(Div.ALIGN_ITEMS, Align.CENTRE));
	}

	public static final ResourceLocation ID = new ResourceLocation("cosmetica", "home");
}
