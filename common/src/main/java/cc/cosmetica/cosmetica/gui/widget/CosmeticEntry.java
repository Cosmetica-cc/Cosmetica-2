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

package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.CommonProperties;
import cc.cosmetica.kupe.api.gui.style.RootStylesheet;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticEntry extends Component {
	public CosmeticEntry(ResourceLocation icon, String id, String name, String owner) {
		this.icon = icon;
		this.id = id;
		this.name = name;
		this.owner = owner;
	}

	private final ResourceLocation icon;
	private final String id;
	private final String name;
	private final String owner;

	@Override
	public List<Component> build() {
		return ImmutableList.of(new Div(
				new Image(this.icon),
				new Div(
						new Label(Text.literal(this.name)),
						new Label(Text.literal(this.owner))
				).tag("centry_names"),
				new Button(Text.literal("X"), () -> {})
		).tag("centry_root"));
	}

	@Override
	public @Nullable Stylesheet getStylesheet() {
		return STYLE;
	}

	private static final Stylesheet STYLE = new Stylesheet()
			.component(Image.class, Style.create()
					.set(PADDING, fixed(new Margins(2)))
					.set(CommonProperties.HEIGHT, fixed(OptionalInt.of(38))))
			.component(Button.class, Style.create()
					.set(ALIGN_SELF, Optional.of(Align.START))
					.set(CommonProperties.MAXIMUM_SIZE, fixed(new Dimensions(20, 20))))
			.tag("centry_root", Style.create()
					.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
					.set(Div.ALIGN_ITEMS, Align.CENTRE)
					.set(CommonProperties.BACKGROUND_COLOUR, OptionalInt.of(0xFCBA03)))
			.tag("centry_names", Style.create()
					.set(FLEX, 1));

	static {
		RootStylesheet.setDefaultOverrides(CosmeticEntry.class, Style.create()
				.set(CommonProperties.MAXIMUM_SIZE, fixed(new Dimensions(Integer.MAX_VALUE, 40))));
	}
}
