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

import cc.cosmetica.cosmetica.BrowseScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Align;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticsList extends Div {
	public CosmeticsList(Collection<CosmeticEntry> entries, boolean editable) {
		this.entries = entries.toArray(new CosmeticEntry[0]);
		this.editable = editable;
	}

	private final CosmeticEntry[] entries;
	private final boolean editable;

	@Override
	public List<Component> build() {
		return this.editable ? ImmutableList.of(
				new EntryList.Div(this.entries)
						.tag("width-200", "contents-wrapper"),
				new Button(Text.literal("+"), () ->
					Screens.setScreen(BrowseScreen.ID)
				).tag("width-200")
		) : ImmutableList.of(
				new EntryList.Div(this.entries)
						.tag("width-200", "contents-wrapper")
		);
	}

	@Override
	public @Nullable Stylesheet getStylesheet() {
		return new Stylesheet()
				.self(Style.create()
						.set(PADDING, fixed(new Margins(30, 10, 12, 10)))
						.set(Div.ALIGN_ITEMS, Align.STRETCH_START))
				.tag("contents-wrapper", Style.create()
						.set(FLEX, 1)
						.set(SCROLLBAR_POSITION, ScrollbarPosition.OUTSIDE))
				.tag("width-200", Style.create()
						.set(WIDTH, fixed(OptionalInt.of(200))));
	}
}
