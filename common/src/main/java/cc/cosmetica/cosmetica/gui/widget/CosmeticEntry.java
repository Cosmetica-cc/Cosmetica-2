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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.RootStylesheet;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticEntry extends Component {
	public CosmeticEntry(ResourceKey icon, String id, String name, String owner, int editable) {
		this.image = null;
		this.icon = icon;
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.editable = editable;
	}

	public CosmeticEntry(CachedImage image, String id, String name, String owner, int editable) {
		this.image = image;
		this.icon = new ResourceKey(image.location);
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.editable = editable;
	}

	// need to hold onto cached image so it doesn't get GC'd
	private final CachedImage image;
	//
	private final ResourceKey icon;
	private final String id;
	private final String name;
	private final String owner;
	private final int editable;

	@Override
	public List<Component> build() {
		List<Component> content = new ArrayList<>(Arrays.asList(
				new Image(this.icon).setTransparent(1.0f),
				new Div(
						new Label(Text.literal(this.name)),
						new Label(Text.literal(this.owner))
				).tag("centry_names")
		));

		// add X if editable
		if (this.editable > 0) {
			content.add(new Button(Text.literal("X"), () -> {})
					.setDisabled(this.editable != 1)
					.withStyle(Style.create().set(TOOLTIP,
							this.editable == 1 ? Optional.empty()
									: Optional.of(new Tooltip(Text.translatable("cosmetica.offline")))
					)));
		}

		return ImmutableList.of(new Div(content.toArray(new Component[content.size()])).tag("centry_root"));
	}

	@Override
	protected void paintBackground(Canvas canvas, Region region, Margins padding) {
//		RenderSystem.disableDepthTest();
		super.paintBackground(canvas, region, padding);
	}

	@Override
	public Stylesheet getStylesheet() {
		return STYLE;
	}

	private static final Stylesheet STYLE = new Stylesheet()
			.component(Image.class, Style.create()
					.set(PADDING, fixed(new Margins(2)))
					.set(WIDTH, fixedSize(38))// debug: see images while loading texture is not yet added
					.set(HEIGHT, fixedSize(38))
					.set(MIN_WIDTH, fixedSize(38))
					.set(MIN_HEIGHT, fixedSize(38)))
			.component(Button.class, Style.create()
					.set(ALIGN_SELF, Optional.of(Align.START))
					.set(MAXIMUM_SIZE, fixed(new Dimensions(20, 20))))
			.tag("centry_root", Style.create()
					.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
					.set(Div.ALIGN_ITEMS, Align.CENTRE)
					.set(BACKGROUND_COLOUR, OptionalInt.of(0x858585))
					.set(BORDER, Border.create(Border.BorderConfig.split(1, 0xA1A1A1, 0x595959))))
			.tag("centry_names", Style.create()
					.set(Div.ALIGN_ITEMS, Align.STRETCH_START)
					.set(FLEX, 1));

	static {
		RootStylesheet.setDefaultOverrides(CosmeticEntry.class, Style.create()
				.set(MAXIMUM_SIZE, fixed(new Dimensions(Integer.MAX_VALUE, 40))));
	}
}
