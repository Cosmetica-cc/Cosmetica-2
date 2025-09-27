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

import cc.cosmetica.core.api.*;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.ConfirmRemoveCosmeticScreen;
import cc.cosmetica.cosmetica.util.Thumbnail;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticEntry extends Component {
	public CosmeticEntry(Cosmetics cosmetics, ResourceKey icon, String id, String name, String owner, Type type, Category category) {
		this.parentOutfit = cosmetics;
		this.image = null;
		this.icon = icon;
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.editable = type;
		this.category = category;
	}

	public CosmeticEntry(Cosmetics cosmetics, CachedImage image, String id, String name, String owner, Type type, Category category) {
		this.parentOutfit = cosmetics;
		this.image = image;
		this.icon = new ResourceKey(image.location);
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.editable = type;
		this.category = category;
	}

	// need to hold onto cached image so it doesn't get GC'd
	private final CachedImage image;
	//
	private final Cosmetics parentOutfit;
	private final ResourceKey icon;
	private final String id;
	private final String name;
	private final String owner;
	private final Type editable;
	private final Category category;

	@Override
	public List<Component> build() {
		List<Component> content = new ArrayList<>(Arrays.asList(
				new Image(this.icon).setTransparent(1.0f),
				new Div(
						new Label(Text.literal(this.name)),
						new Label(Text.literal(this.owner))
				).tag("centry_names")
		));

		// add remove button if editable
		if (this.editable.hasRemoveButton()) {
			content.add(
					new Button(Text.literal("-"), () -> {
						Screens.setScreen(new ConfirmRemoveCosmeticScreen(this.parentOutfit, this.id, this.name), Text.translatable("screens.cosmetica.confirmDeletion"));
					}).setDisabled(this.editable == Type.REMOVABLE_OFFLINE)
					  .withStyle(Style.create().set(TOOLTIP,
							this.editable == Type.REMOVABLE ? Optional.empty()
									: Optional.of(new Tooltip(Text.translatable("cosmetica.offline")))
					  ))
					  .tag("button_subtract")
			);
		} else if (this.editable == Type.EQUIPPABLE) {
			content.add(
					new Button(Text.literal("+"), () -> {
						// TODO equip
					}).tag("button_add")
			);
		}

		return ImmutableList.of(new Div(content.toArray(new Component[content.size()])).tag("centry_root"));
	}

	@Override
	protected void paintBackground(Canvas canvas, Region region, Margins padding) {
//		RenderSystem.disableAlphaTest();
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
					.set(MAXIMUM_SIZE, fixed(new Dimensions(20, 20))))
			.tag("button_subtract", Style.create()
					.set(ALIGN_SELF, Optional.of(Align.START)))
			.tag("button_add", Style.create()
					.set(MARGINS, fixed(new Margins(0,10,0,0))))
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

	public enum Category {
		ACCESSORY,
		CAPE
	}

	public static final CachedImage NO_THUMBNAIL = new CachedImage(Cosmetica.FALLBACK_TEXTURE, 0);

	/**
	 * Create the GUI cosmetic list entries for each cosmetic the player is wearing.
	 * @param entryList the list to populate.
	 * @param cosmetics the cosmetics the player is wearing.
	 * @param type control the type of widget to show for the cosmetics.
	 */
	public static void populateEntryList(final List<CosmeticEntry> entryList, Cosmetics cosmetics, Type type) {
		if (cosmetics == null)
			return; // no cosmetics

		// (wip) this should only show API cosmetics and only allow editing if own cosmetics.
		// some kind of notification if no internet
		// this also means for local player, even when null, we need to handle backup cosmetics no?

		boolean showSeparateElytra = true;
		if (cosmetics.getCloak().isPresent()) {
			ImageCosmetic cloak = cosmetics.getCloak().get();

			String message = "Cloak";
			if (cloak.getId().equals(cosmetics.getElytra().map(ImageCosmetic::getId).orElse(null))) {
				message = "Cloak + Elytra";
				showSeparateElytra = false;
			}

			entryList.add(new CosmeticEntry(
					cosmetics,
					getOrCreateThumb(cloak.getThumbnail(), "thumbs-c", cloak.getId(), 3), // TODO in core give ticks per frame (expose AnimatedTextureCosmetic)
					cloak.getId(),
					cloak.getName(),
					message, //cloak.getCreator().isPresent() ? cloak.getCreator().get().getName() : "Could not load creator"
					type,
					CosmeticEntry.Category.CAPE
			));
		}

		if (showSeparateElytra && cosmetics.getElytra().isPresent()) {
			ImageCosmetic elytra = cosmetics.getElytra().get();

			entryList.add(new CosmeticEntry(
					cosmetics,
					getOrCreateThumb(elytra.getThumbnail(), "thumbs-c", elytra.getId(), 3), // TODO in core give ticks per frame (expose AnimatedTextureCosmetic)
					elytra.getId(),
					elytra.getName(),
					"Elytra", //elytra.getCreator().isPresent() ? elytra.getCreator().get().getName() : "Could not load creator"
					type,
					CosmeticEntry.Category.CAPE
			));
		}

		for (Accessory accessory : cosmetics.getAccessories()) {
			// texture for thumbnail
			CachedImage thumbnail = getOrCreateThumb(accessory.getThumbnail(), "thumbs-c", accessory.getId(), accessory.getJsonObject().getTicksPerFrame().intValue());

			// n.b. reference to CachedImage needs to be stored on the entry so it doesn't get GC'd
			entryList.add(new CosmeticEntry(
					cosmetics,
					thumbnail,
					accessory.getId(),
					accessory.getName(),
					accessory.getCreator().isPresent() ? accessory.getCreator().get().getName() : "Could not load creator",
					type,
					CosmeticEntry.Category.ACCESSORY
			));
		}
	}

	/**
	 * Populate entry list of cosmetics for browse.
	 * @param entryList       the entrylist to populate.
	 * @param cosmetics       the list of cosmetics on the browse page.
	 * @param equipOntoOutfit the outfit to equip onto.
	 */
	public static void populateBrowseList(final List<CosmeticEntry> entryList, List<gg.cloaks.javaclient.model.Cosmetic> cosmetics, @Nullable Cosmetics equipOntoOutfit) {
		for (gg.cloaks.javaclient.model.Cosmetic cosmetic : cosmetics) {
			System.out.println( ((Thumbnail)cosmetic).getThumbnail() );
			entryList.add(new CosmeticEntry(
					equipOntoOutfit, // TODO handle null lol
					getOrCreateThumb(/*"https://cdn.valoeghese.nz/gumi.png"*/ ((Thumbnail)cosmetic).getThumbnail() /*FIXME thumbnail*/, "thumbs-c", cosmetic.getId(), 1 /*ticks per frame FIXME*/),
					cosmetic.getId(),
					cosmetic.getName(),
					cosmetic.getCreator() == null ? "Could not load creator" : cosmetic.getCreator().getUsername(),
					Type.EQUIPPABLE,
					"accessory".equals(cosmetic.getType()) ? Category.ACCESSORY : Category.CAPE
			));
		}
	}

	private static CachedImage getOrCreateThumb(@Nullable String thumbnail, String category, String id, int ticksPerFrame) {
		if (thumbnail == null) {
			return NO_THUMBNAIL;
		} else {
			return ThumbnailCache.getOrCreateImage(category, id,
					new CosmeticaTexture.Builder(thumbnail, Cosmetica.LOADING_TEXTURE)
							.frames(8, ticksPerFrame)
							.failToLoadTexture(Cosmetica.FALLBACK_TEXTURE)
							.autoAnimate(CosmeticaTexture.AutoAnimate.NEVER_TILESHEETS));
		}
	}

	public enum Type {
		LISTED,
		REMOVABLE,
		REMOVABLE_OFFLINE,
		EQUIPPABLE;

		boolean hasRemoveButton() {
			return this == REMOVABLE || this == REMOVABLE_OFFLINE;
		}

		public static Type removable(boolean authenticated) {
			return authenticated ? REMOVABLE : REMOVABLE_OFFLINE;
		}
	}
}
