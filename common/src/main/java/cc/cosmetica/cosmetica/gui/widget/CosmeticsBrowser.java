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

import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Border;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.style.CommonProperties;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticsBrowser extends Div {
	public CosmeticsBrowser(Collection<CosmeticEntry> entries) {
		this.entries = entries.toArray(new CosmeticEntry[0]);
	}

	private final CosmeticEntry[] entries;

	@Override
	public List<Component> build() {
		return ImmutableList.of(
				new Div(this.entries)
						.tag("width-200", "contents-wrapper"),
				new Button(Text.literal("+"), () -> {}).tag("width-200")
		);
	}

	@Override
	public @Nullable Stylesheet getStylesheet() {
		return new Stylesheet()
				.tag("contents-wrapper", Style.create()
						.set(BACKGROUND_COLOUR, OptionalInt.of(0x000000))
						.set(BORDER, Border.create(1, 0xFFFFFF))
						//.set(MAXIMUM_SIZE, (vw, vh, pw, ph) -> new Dimensions(Integer.MAX_VALUE, Math.max(0, ph - 20)))
						.set(FIXED_CONTAINER, false)
						.set(FLEX, 1)
						.set(PADDING, fixed(new Margins(1))))
				.tag("width-200", Style.create()
						.set(WIDTH, fixed(OptionalInt.of(200))));
	}
}
