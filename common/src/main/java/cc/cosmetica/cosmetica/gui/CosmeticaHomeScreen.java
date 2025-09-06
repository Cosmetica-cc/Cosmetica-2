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

public class CosmeticaHomeScreen extends AbstractHomeScreen {
	public CosmeticaHomeScreen() {
		super(ID);
	}

	@Override
	protected @NotNull Component createRightMenu(Cosmetics cosmetics, boolean authenticated) {
		List<CosmeticEntry> entries = new ArrayList<>();
		CosmeticEntry.populateEntryList(entries, cosmetics, authenticated ? 1 : 2);

		return new CosmeticsList(entries, true);
	}



	public static final ResourceKey ID = new ResourceKey("cosmetica", "home");
}
