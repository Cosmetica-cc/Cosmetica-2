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

import cc.cosmetica.cosmetica.gui.BrowseScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticsList extends Div {
	public CosmeticsList(Collection<CosmeticEntry> entries, ListType listType) {
		this.entries = entries.toArray(new CosmeticEntry[0]);
		this.listType = listType;
	}

	protected final CosmeticEntry[] entries;
	private final ListType listType;

	@Override
	public List<Component> build() {
		return this.listType != ListType.LIST_ONLY ? ImmutableList.of(
				new EntryList.Div(this.entries)
						.tag("width-45vw", "contents-wrapper"),
				new Div(
						new Button(Text.literal("+"), () ->
								Screens.setScreen(BrowseScreen.ID)
						).setDisabled(this.listType == ListType.DISABLED || this.listType == ListType.OFFLINE).tag(this.listType.buttonTags)
				).tag("width-45vw").withStyle(Style.create().set(FLOW_DIRECTION, Axis2D.POSITIVE_Y))
		) : ImmutableList.of(
				new EntryList.Div(this.entries)
						.tag("width-45vw", "contents-wrapper")
		);
	}

	@Override
	public @Nullable Stylesheet getStylesheet() {
		return new Stylesheet()
				.self(Style.create()
						.set(PADDING, fixed(new Margins(30, 10, 12, 10)))
						.set(Div.ALIGN_ITEMS, Align.START))
				.tag("contents-wrapper", Style.create()
						.set(FLEX, 1)
						.set(SCROLLBAR_POSITION, ScrollbarPosition.OUTSIDE))
				.tag("width-45vw", Style.create()
						.set(WIDTH, screen(45, 0))
						.set(MAXIMUM_SIZE, fixed(new Dimensions(396, Integer.MAX_VALUE))))
				.tag("no-outfit-disabled", Style.create()
						.set(TOOLTIP, Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.noOutfitDisabled")))))
				.tag("no-outfit-offline", Style.create()
						.set(TOOLTIP, Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.offline")))));
	}

	public enum ListType {
		/**
		 * Outfits that are editable by the user.
		 */
		EDITABLE("width-45vw"),
		/**
		 * Outfits that can only be displayed.
		 */
		LIST_ONLY("width-45vw"),
		/**
		 * In a context where the list is usually editable, but no outfit is selected and therefore the button should be disabled.
		 */
		DISABLED("width-45vw", "no-outfit-disabled"),
		/**
		 * In a context where the list is usually editable, but the user is offline and therefore the button should be disabled.
		 */
		OFFLINE("width-45vw", "no-outfit-offline");

		ListType(String... buttonTags) {
			this.buttonTags = buttonTags;
		}

		private final String[] buttonTags;
	}
}
