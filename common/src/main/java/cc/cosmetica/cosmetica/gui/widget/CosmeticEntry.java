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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.*;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.ConfirmRemoveCosmeticScreen;
import cc.cosmetica.cosmetica.gui.GuiUtils;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.AccessoryOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CapeOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CosmeticOptions;
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
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.cosmetica.gui.GuiUtils.NORMAL_COLOUR;
import static cc.cosmetica.cosmetica.gui.GuiUtils.SHADE_COLOUR;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticEntry extends Component {
	public CosmeticEntry(Cosmetics cosmetics, @Nullable CosmeticEnvelope cosmetic, CachedImage image, String id, String name, String owner, Type type, Attachment attachment, @Nullable CosmeticEntry.EquipCallback onEquipButton, boolean mirrored) {
		this.parentOutfit = cosmetics;
		this.image = image;
		this.icon = new ResourceKey(image.location);
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.type = type;
		this.attachment = attachment;
		this.cosmetic = cosmetic;
		this.onEquipButton = onEquipButton;
		// Only important for modifiable lists (own cosmetics)
		this.mirrored = mirrored;

		if (cosmetic == null && type.hasEquipButton()) {
			throw new IllegalArgumentException("Cannot have null cosmetic envelope for equippable item");
		}
		if (onEquipButton == null && type.hasEquipButton()) {
			throw new IllegalArgumentException("Cannot have null on equip callback for equippable item");
		}
	}

	// need to hold onto cached image so it doesn't get GC'd
	private final CachedImage image;
	//
	private final Cosmetics parentOutfit;
	private final ResourceKey icon;
	private final String id;
	private final String name;
	private final String owner;
	private final boolean mirrored;
	private final Type type;
	private final Attachment attachment;
	private List<ResourceKey> infoIcons = new ArrayList<>();
	private final @Nullable CosmeticEnvelope cosmetic;
	private final @Nullable EquipCallback onEquipButton;

	private CosmeticEntry setInfoIcons(List<ResourceKey> infoIcons) {
		this.infoIcons = infoIcons;
		return this;
	}

	@Override
	public List<Component> build() {
		Component attachmentIcon = new Image(this.attachment.icon).setTransparent(1.0f).tag("centry_info_icon")
				.withStyle(Style.create()
						.set(TOOLTIP, Optional.of(new Tooltip(this.attachment.tooltip())))
						.set(MARGINS, fixed(new Margins(0, 4, 0, 0))));

		List<Component> infoIcons = this.infoIcons.stream()
				.map(i -> new Image(i).setTransparent(1.0f).tag("centry_info_icon").withStyle(
						Style.create().set(TOOLTIP, Optional.of(new Tooltip(iconTooltip(i))))
				))
				.collect(Collectors.toList());

		List<Component> content = new ArrayList<>(Arrays.asList(
				new Image(this.icon).setTransparent(1.0f).tag("centry_main_icon"),
				new Div(
						new Div(
								new Label(Text.literal(this.name))
										.withStyle(Style.create().set(Label.TEXT_WRAP, fixed(OptionalInt.empty())))
						).withStyle(Style.create().set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)),
						this.type == Type.EXTERNAL ?
								new Div(attachmentIcon, new Label(Text.literal(this.owner))).tag("info_icons") :
								new Div(merge(attachmentIcon, infoIcons).toArray(new Component[0])).tag("info_icons")
				).tag("centry_names")
		));

		// add remove button if editable
		if (this.type.hasRemoveButton()) {
			content.add(
					new Button(Text.literal("-"), () -> {
						Screens.setScreen(new ConfirmRemoveCosmeticScreen(this.parentOutfit, this.id, this.name, this.mirrored), Text.translatable("screens.cosmetica.confirmDeletion"));
					}).setDisabled(this.type == Type.REMOVABLE_OFFLINE)
					  .withStyle(Cosmetica.authTooltipStyle(this.type == Type.REMOVABLE))
					  .tag("button_subtract")
			);
		} else if (this.type.hasEquipButton()) {
			Button b = (Button) new Button(Text.literal("+"), () -> {
				// See: Constructor
				assert this.cosmetic != null;
				assert this.onEquipButton != null;
				// Create cosmetic options
				CosmeticOptions options;
				switch (this.attachment.category()) {
					case ACCESSORY:
						List<BigDecimal> offset = Objects.requireNonNull(this.cosmetic.getAccessory()).getOffset();
						options = new AccessoryOptions(
								new double[] { offset.get(0).doubleValue(), offset.get(3).doubleValue() },
								new double[] { offset.get(1).doubleValue(), offset.get(4).doubleValue() },
								new double[] { offset.get(2).doubleValue(), offset.get(5).doubleValue() }
						);
						break;
					case CAPE:
						int flags = Objects.requireNonNull(this.cosmetic.getAnimatedTextureCosmetic()).getFlags().intValue();
						options = new CapeOptions(flags);
						break;
					case UNKNOWN: // Should never get here, as we disable the button for unknown types in {@link populateBrowseList}
					default:
						throw new IllegalArgumentException("Unknown type for " + this.name + " (" + this.id + "), cannot equip!");
                }
				// Give feedback
				this.onEquipButton.accept(new CosmeticData(this.name, this.id, this.image), options, this.cosmetic, dto -> {
					return CosmeticaAPI.outfits().requestAsync(api -> api.modify(this.parentOutfit.getOutfitId().orElseThrow(IllegalStateException::new), dto));
				});
			}).tag("button_add");

			if (this.type == Type.EQUIPPABLE_UNSUPPORTED) {
				b.setDisabled(true);
				b.withStyle(Style.create().set(TOOLTIP, Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.outdated")))));
			}

			content.add(b);
		}

		return ImmutableList.of(new Div(content.toArray(new Component[content.size()])).tag("centry_root", this.type == Type.EXTERNAL ? "external_colour" : "centry_normal_colour"));
	}

	@Override
	public Stylesheet getStylesheet() {
		return STYLE;
	}

	private static final Stylesheet STYLE = new Stylesheet()
			.component(Button.class, Style.create()
					.set(MAXIMUM_SIZE, fixed(new Dimensions(20, 20))))
			.tag("centry_main_icon", Style.create()
					.set(PADDING, fixed(new Margins(2)))
					.set(WIDTH, fixedSize(38))
					.set(HEIGHT, fixedSize(38))
					.set(MIN_WIDTH, fixedSize(38))
					.set(MIN_HEIGHT, fixedSize(38)))
			.tag("button_subtract", Style.create()
					.set(ALIGN_SELF, Optional.of(Align.START)))
			.tag("button_add", Style.create()
					.set(MARGINS, fixed(new Margins(0,10,0,0))))
			.tag("centry_root", Style.create()
					.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
					.set(Div.ALIGN_ITEMS, Align.CENTRE))
			.tag("centry_normal_colour", Style.create()
					.set(BACKGROUND_COLOUR, OptionalInt.of(NORMAL_COLOUR))
					.set(BORDER, GuiUtils.POPOUT_BORDER))
			.tag("external_colour", Style.create()
					.set(BACKGROUND_COLOUR, OptionalInt.of(SHADE_COLOUR))
					.set(BORDER, Border.create(Border.BorderConfig.split(1, NORMAL_COLOUR, 0x343434))))
			.tag("centry_names", Style.create()
					.set(Div.ALIGN_ITEMS, Align.STRETCH_START)
					.set(FLEX, 1))
			.tag("centry_info_icon", Style.create()
					.set(WIDTH, fixedSize(14))
					.set(HEIGHT, fixedSize(14)))
			.tag("info_icons", Style.create()
					.set(MARGINS, fixed(new Margins(2, 0, 0, 0)))
					.set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X));

	static {
		RootStylesheet.setDefaultOverrides(CosmeticEntry.class, Style.create()
				.set(MAXIMUM_SIZE, fixed(new Dimensions(Integer.MAX_VALUE, 40))));
	}

	private static <T> List<T> merge(T a, List<T> b) {
		ArrayList<T> result = new ArrayList<>();
		result.add(a);
		result.addAll(b);
		return result;
	}

	private static Text iconTooltip(ResourceKey infoIcon) {
		String[] path = infoIcon.getPath().split("/");
		return Text.translatable("tooltip.cosmetica.icons." + path[path.length - 1].substring(0, path[path.length - 1].length() - 4));
	}

	public enum Category {
		ACCESSORY,
		CAPE,
		UNKNOWN
	}

	public enum Attachment {
		HEAD_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_head.png")),
		TORSO_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_body.png")),
		LEFT_ARM_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_left_arm.png")),
		RIGHT_ARM_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_right_arm.png")),
		LEFT_LEG_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_left_leg.png")),
		RIGHT_LEG_ACCESSORY(Category.ACCESSORY, new ResourceKey("cosmetica", "textures/icon/accessory_right_leg.png")),
		CLOAK(Category.CAPE, new ResourceKey("cosmetica", "textures/icon/cape_cloak.png")),
		ELYTRA(Category.CAPE, new ResourceKey("cosmetica", "textures/icon/cape_elytra.png")),
		CLOAK_ELYTRA(Category.CAPE, new ResourceKey("cosmetica", "textures/icon/cape_cape.png")),
		UNKNOWN(Category.UNKNOWN, new ResourceKey("cosmetica", "icon.png"));

		Attachment(Category metaCategory, ResourceKey icon) {
			this.category = metaCategory;
			this.icon = icon;
		}

		private final Category category;
		private final ResourceKey icon;

		public Category category() {
			return this.category;
		}

		public Text tooltip() {
			return CosmeticEntry.iconTooltip(this.icon);
		}

		public static Attachment accessory(gg.cloaks.javaclient.model.Accessory.AttachmentEnum attachmentEnum) {
			switch (attachmentEnum) {
			case HEAD:
				return HEAD_ACCESSORY;
			case BODY:
				return TORSO_ACCESSORY;
			case LEFT_ARM:
				return LEFT_ARM_ACCESSORY;
			case RIGHT_ARM:
				return RIGHT_ARM_ACCESSORY;
			case LEFT_LEG:
				return LEFT_LEG_ACCESSORY;
			case RIGHT_LEG:
				return RIGHT_LEG_ACCESSORY;
			case UNKNOWN_DEFAULT_OPEN_API:
			default:
				return UNKNOWN;
            }
		}

		public static Attachment cape(CapeOptions options) {
			if (options.isElytra()) {
				return options.isCloak() ? CLOAK_ELYTRA : ELYTRA;
			} else {
				return options.isCloak() ? CLOAK : UNKNOWN;
			}
		}
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

		boolean showSeparateElytra = true;

		// Shows both API cosmetics and external capes
		// Distinguish them by changing type!

		if (cosmetics.getCloak().isPresent()) {
			ImageCosmetic cloak = cosmetics.getCloak().get();

			if (cloak.getId().equals(cosmetics.getElytra().map(ImageCosmetic::getId).orElse(null))) {
				showSeparateElytra = false;
			}

			entryList.add(new CosmeticEntry(
					cosmetics,
					null,
					!cloak.getThumbnail().isPresent() ? NO_THUMBNAIL : getOrCreateThumb(cloak.getThumbnail().get(), cloak.getImage().getFramePeriod(), false),
					cloak.getId(),
					cloak.getName(),
					cloak.getCreator().isPresent() ? cloak.getCreator().get().getName() : "Could not load creator",
					cloak.isExternal() ? Type.EXTERNAL : type,
					showSeparateElytra ? Attachment.CLOAK : Attachment.CLOAK_ELYTRA,
					null,
					false
			));
		}

		if (showSeparateElytra && cosmetics.getElytra().isPresent()) {
			ImageCosmetic elytra = cosmetics.getElytra().get();

			entryList.add(new CosmeticEntry(
					cosmetics,
					null,
					!elytra.getThumbnail().isPresent() ? NO_THUMBNAIL : getOrCreateThumb(elytra.getThumbnail().get(), elytra.getImage().getFramePeriod(), false),
					elytra.getId(),
					elytra.getName(),
					elytra.getCreator().isPresent() ? elytra.getCreator().get().getName() : "Could not load creator",
					elytra.isExternal() ? Type.EXTERNAL : type,
					Attachment.ELYTRA,
					null,
					false
			));
		}

		for (Accessory accessory : cosmetics.getAccessories()) {
			// texture for thumbnail
			CachedImage thumbnail = getOrCreateThumb(accessory.getThumbnail().orElseThrow(IllegalStateException::new), accessory.getJsonObject().getTicksPerFrame().intValue(), false);

			// n.b. reference to CachedImage needs to be stored on the entry so it doesn't get GC'd
			entryList.add(new CosmeticEntry(
					cosmetics,
					null,
					thumbnail,
					accessory.getId(),
					accessory.getName(),
					accessory.getCreator().isPresent() ? accessory.getCreator().get().getName() : "Could not load creator",
					type,
					Attachment.accessory(accessory.getAttachment()),
					null,
					accessory.isMirrored()
			));
		}
	}

	/**
	 * Populate entry list of cosmetics for browse.
	 * @param entryList       the entrylist to populate.
	 * @param cosmetics       the list of cosmetics on the browse page.
	 * @param equipOntoOutfit the outfit to equip onto.
	 * @param equipCallback   called when a cosmetic equip button is pressed.
	 */
	public static void populateBrowseList(final List<CosmeticEntry> entryList, List<CosmeticEnvelope> cosmetics, @NotNull Cosmetics equipOntoOutfit, EquipCallback equipCallback) {
		Objects.requireNonNull(equipOntoOutfit, "Must have outfit to browse.");
		Objects.requireNonNull(equipCallback, "Must have equip callback.");

		for (CosmeticEnvelope envelope : cosmetics) {
			if (envelope.getCosmetic() != null) {
				gg.cloaks.javaclient.model.Cosmetic cosmetic = envelope.getCosmetic();

				entryList.add(new CosmeticEntry(
						equipOntoOutfit, // TODO handle null lol
						envelope,
						CachedImage.NO_TEXTURE,
						cosmetic.getId(),
						cosmetic.getName(),
						cosmetic.getCreator() == null ? "Could not load creator" : cosmetic.getCreator().getUsername(),
						Type.EQUIPPABLE_UNSUPPORTED,
						Attachment.UNKNOWN,
						equipCallback,
						false
				));
			} else if (envelope.getTextureCosmetic() != null) {
				TextureCosmetic cosmetic = envelope.getTextureCosmetic();

				entryList.add(new CosmeticEntry(
						equipOntoOutfit, // TODO handle null lol
						envelope,
						getOrCreateThumb(cosmetic.getThumbnail(), 1, true),
						cosmetic.getId(),
						cosmetic.getName(),
						cosmetic.getCreator() == null ? "Could not load creator" : cosmetic.getCreator().getUsername(),
						Type.EQUIPPABLE_UNSUPPORTED,
						Attachment.UNKNOWN,
						equipCallback,
						false
				));
			} else if (envelope.getAnimatedTextureCosmetic() != null) {
				AnimatedTextureCosmetic cosmetic = envelope.getAnimatedTextureCosmetic();
				Attachment attachment = "cape".equals(cosmetic.getType()) ? Attachment.cape(new CapeOptions(cosmetic.getFlags().intValue())) : Attachment.UNKNOWN;

				entryList.add(new CosmeticEntry(
						equipOntoOutfit, // TODO handle null lol
						envelope,
						getOrCreateThumb(cosmetic.getThumbnail(), cosmetic.getTicksPerFrame().intValue(), true),
						cosmetic.getId(),
						cosmetic.getName(),
						cosmetic.getCreator() == null ? "Could not load creator" : cosmetic.getCreator().getUsername(),
						attachment.category() == Category.UNKNOWN ? Type.EQUIPPABLE_UNSUPPORTED : Type.EQUIPPABLE,
						attachment,
						equipCallback,
						false
				));
			} else if (envelope.getAccessory() != null) {
				gg.cloaks.javaclient.model.Accessory cosmetic = envelope.getAccessory();
				Attachment attachment = "accessory".equals(cosmetic.getType()) ? Attachment.accessory(cosmetic.getAttachment()) : Attachment.UNKNOWN;

				CosmeticEntry entry;
				entryList.add(entry = new CosmeticEntry(
						equipOntoOutfit, // TODO handle null lol
						envelope,
						getOrCreateThumb(cosmetic.getThumbnail(), cosmetic.getTicksPerFrame().intValue(), true),
						cosmetic.getId(),
						cosmetic.getName(),
						cosmetic.getCreator() == null ? "Could not load creator" : cosmetic.getCreator().getUsername(),
						attachment.category() == Category.UNKNOWN ? Type.EQUIPPABLE_UNSUPPORTED : Type.EQUIPPABLE,
						attachment,
						equipCallback,
						false
				));

				if (attachment.category() == Category.ACCESSORY) {
					// add info icons
					List<ResourceKey> infoIcons = new ArrayList<>();
					int flags = cosmetic.getFlags().intValue();
					for (Accessory.Flag flag : Accessory.Flag.values()) {
						if (flag.isSet(flags)) {
							infoIcons.add(new ResourceKey("cosmetica", "textures/icon/" + flag.toString().toLowerCase(Locale.ROOT) + ".png"));
						}
					}
					entry.setInfoIcons(infoIcons);
				}
			}
		}
	}

	private static CachedImage getOrCreateThumb(@Nullable String thumbnail, int ticksPerFrame, boolean browseCache) {
		if (thumbnail == null) {
			return NO_THUMBNAIL;
		} else {
			return ThumbnailCache.getOrCreateImage(
					new CosmeticaTexture.Builder(thumbnail, Cosmetica.LOADING_TEXTURE)
							.frames(8, ticksPerFrame)
							.ignoreTilesheet(true)
							.failToLoadTexture(Cosmetica.FALLBACK_TEXTURE)
							.autoAnimate(CosmeticaTexture.AutoAnimate.NEVER_TILESHEETS), browseCache);
		}
	}

	public enum Type {
		/**
		 * Lists the item only. No additional widgets.
		 */
		LISTED,
		/**
		 * Like listed, but different styling to show the item is external.
		 */
		EXTERNAL,
		/**
		 * Removable item from its outfit.
		 */
		REMOVABLE,
		/**
		 * Removable item, but you are offline.
		 */
		REMOVABLE_OFFLINE,
		/**
		 * Item that is equippable to its outfit.
		 */
		EQUIPPABLE,
		/**
		 * Item that is equippable but unsupported (outdated mod version?).
		 */
		EQUIPPABLE_UNSUPPORTED;

		boolean hasRemoveButton() {
			return this == REMOVABLE || this == REMOVABLE_OFFLINE;
		}

		boolean hasEquipButton() {
			return this == EQUIPPABLE || this == EQUIPPABLE_UNSUPPORTED;
		}

		public static Type removable(boolean authenticated) {
			return authenticated ? REMOVABLE : REMOVABLE_OFFLINE;
		}
	}

	/**
	 * Equip callback for
	 */
	@FunctionalInterface
	public interface EquipCallback {
		/**
		 * Called when the equip button for a cosmetic is called.
		 * @param cosmetic basic data (thumbnail, id, and name) of the cosmetic being equipped.
		 * @param options bounds for the customisation options of the cosmetic being equipped onto the outfit.
		 * @param envelope the full cosmetic envelope of the cosmetic.
		 * @param submit function to submit the equip request.
		 */
		void accept(CosmeticData cosmetic, CosmeticOptions options, CosmeticEnvelope envelope, Function<CreateOutfitDto, CompletableFuture<Outfit>> submit);
	}

	/**
	 * Basic parsed cosmetic data pojo. Less data than the usual cosmetic data objects.
	 */
	public static class CosmeticData {
        public CosmeticData(String name, String id, CachedImage thumbnail) {
            this.name = name;
			this.id = id;
            this.thumbnail = thumbnail;
        }

		private final String name;
		private final String id;
		private final CachedImage thumbnail;

		public String getName() {
			return this.name;
		}

		public String getId() {
			return this.id;
		}

		public CachedImage getThumbnail() {
			return this.thumbnail;
		}
	}
}
