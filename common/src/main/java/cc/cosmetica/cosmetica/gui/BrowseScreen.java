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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.AccessoryOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CapeOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CosmeticOptions;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.cosmetica.gui.widget.*;
import cc.cosmetica.cosmetica.util.EquipUtil;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Vec3;
import cc.cosmetica.kupe.impl.fakeplayer.CapeAttachment;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.CreateOutfitAccessoryDto;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import gg.cloaks.javaclient.model.Outfit;
import gg.cloaks.javaclient.model.SearchCosmeticsDto;
import gg.cloaks.javaclient.model.SearchCosmeticsDto.AttachmentsEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.Div.*;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Screen for browsing and applying new cosmetics.
 * Should be seamless with {@link HomeScreen}.
 */
public class BrowseScreen extends AbstractHomeScreen {
    public BrowseScreen() {
        super(ID);
    }

    private final DebounceState<String> query = new DebounceState<>("", 600);
    private final State<Menu> menu = new State<>(Menu.NONE);
    private final State<Sort> sort = new State<>(Sort.NEWEST);
    private final DebounceState<Integer> page = new DebounceState<>(1, 400);
    private final State<Integer> pageCap = new State<>(1);
    private final State<Set<SearchCosmeticsDto.AttachmentsEnum>> filter = new State<>(new HashSet<>());

    /**
     * The state for the item being configured before being equipped.
     */
    private final State<Optional<SelectedCosmeticTriple>> configuring = new State<>(Optional.empty());
    private final State<@Nullable Cosmetic> configuringDownloaded = new State<>(null);
    private final State<Triple<Vec3, Boolean, Boolean>> options = new State<>(Triple.of(Vec3.XP, false, false));

    // TODO could be used in outfit player for preview before configure?
    private final State<@Nullable CosmeticEntry> selected = new State<>(null);
//    private GUIPlayer player;


    @Override
    protected Component createMenuEndSelection() {
        return new MenuEndSelection() {
            @Override
            public List<Component> build() {
                // Don't allow clicking done when configuring (must cancel or equip)
                // TODO maybe move cancel/equip to the menu end selection region
                Optional<SelectedCosmeticTriple> configuring = BrowseScreen.this.configuring.acquire(this);
                this.disabled = configuring.isPresent();
                return super.build();
            }
        };
    }

    @Override
    protected Component createOutfitPlayer(UUID self, boolean authenticated, Cosmetics cosmetics) {
        // don't recreate this component to preserve rotation in gui player
        // TODO maybe use a state instead
        // Would be cleaner, but clutters more states in the codebase and means more gui rebuilding
        OutfitPlayer player = (OutfitPlayer)BrowseScreen.super.createOutfitPlayer(self, authenticated, cosmetics);
        player.keepGuiPlayer();

        return new Component() {
            @Override
            public List<Component> build() {
                // downloaded cosmetic should be cleared when triple cleared
                Triple<Vec3, Boolean, Boolean> options = BrowseScreen.this.options.acquire(this);
                Optional<SelectedCosmeticTriple> configuring = BrowseScreen.this.configuring.acquire(this);
                @Nullable Cosmetic downloaded = BrowseScreen.this.configuringDownloaded.acquire(this);

                return ImmutableList.of(
                        player.configureOverrides(guiPlayer -> {
                                    // Could Squeeze extra performance by bypassing state system
                                    // And modifying gui player directly, but it seems to be fast enough
                                    // BrowseScreen.this.player = guiPlayer;
                            guiPlayer.configureOverride(GUIPlayer.ELYTRA, null);
                            guiPlayer.configureOverride(GUIPlayer.CAPE, null);
                            guiPlayer.configureOverride(AccessoriesAttachment.INSTANCE, null);

                            if (downloaded != null && configuring.isPresent()) {
                                if (downloaded instanceof Accessory) {
                                    List<Accessory> accessories = new ArrayList<>(cosmetics.getAccessories());
                                    accessories.add((Accessory) downloaded);
                                    if (downloaded instanceof Accessory.Adjustable) {
                                        Accessory.Adjustable newAccessory = (Accessory.Adjustable) downloaded;
                                        AccessoryOptions ranges = (AccessoryOptions)configuring.get().getMiddle();
                                        newAccessory.setOffset(
                                                newAccessory.getBaseOffset().add(
                                                        ranges.getXRange().clampMap(options.getLeft().getX())/16.0,
                                                        ranges.getYRange().clampMap(options.getLeft().getY())/16.0,
                                                        ranges.getZRange().clampMap(options.getLeft().getZ())/16.0)
                                        );
                                        newAccessory.setMirrored(options.getMiddle());
                                    } else {
                                        // this shouldn't happen, but it's not worth crashing the game over
                                        Logging.getInstance().warnOnce("browse-adjust", "Accessory in browse screen not adjustable. Adjustments will not appear in preview");
                                    }
                                    guiPlayer.configureOverride(AccessoriesAttachment.INSTANCE, accessories);
                                } else if (downloaded instanceof ImageCosmetic) {
                                    // assume cape for now, as it's the only ImageCosmetic in the search menu
                                    // that exists as of this release of Cosmetica
                                    if (options.getMiddle()) {
                                        // cloak toggle
                                        guiPlayer.configureOverride(GUIPlayer.CAPE, ((ImageCosmetic) downloaded).getImage().location);
                                    }
                                    if (options.getRight()) {
                                        // elytra toggle
                                        guiPlayer.configureOverride(GUIPlayer.ELYTRA, new GUIPlayer.ElytraProperties(
                                                ((ImageCosmetic) downloaded).getImage().location,
                                                false,
                                                true
                                        ));
                                    }
                                }
                            }
                            return guiPlayer;
                        })
                        .setDisabled(true)
                );
            }
        };
    }

    @Override
    protected @NotNull Component createRightMenu(Cosmetics cosmetics, boolean authenticated) {
        /*
         * Browser layout.
         */
        return new LayeredSpace(
                true,
                new Div() {
                    @Override
                    public List<Component> build() {
                        Menu menu = BrowseScreen.this.menu.acquire(this);

                        if (menu == Menu.SORT) {
                            return Arrays.asList(
                                    new DropdownMenu<>(sort, Sort::text, Sort.values())
                            );
                        } else if (menu == Menu.FILTER) {
                            return Arrays.asList(
                                    new DropdownToggles<>(filter, value -> {
                                        switch (value) {
                                        case CLOAK:
                                            return Text.translatable("label.cosmetica.filter.cloaks");
                                        case ELYTRA:
                                            return Text.translatable("label.cosmetica.filter.elytras");
                                        case HEAD:
                                            return Text.translatable("label.cosmetica.filter.head");
                                        case BODY:
                                            return Text.translatable("label.cosmetica.filter.body");
                                        case ARM:
                                            return Text.translatable("label.cosmetica.filter.arm");
                                        case LEG:
                                            return Text.translatable("label.cosmetica.filter.leg");
                                        default:
                                            return Text.translatable("label.cosmetica.filter.unknown");
                                        }
                                    }, AttachmentsEnum.CLOAK, AttachmentsEnum.ELYTRA, AttachmentsEnum.HEAD, AttachmentsEnum.BODY, AttachmentsEnum.ARM, AttachmentsEnum.LEG)
                            );
                        }
                        return super.build();
                    }
                }.withStyle(Style.create().set(Z_INDEX, 10)),
                new Div( // container of all the browse area
                        // -- global header moved to Results only
                        new LayeredSpace( // container for what can appear in search contents
                                true,
                                new Div(
                                        new Div( // top 'head'
                                                new TextBox(
                                                        Text.translatable("label.browse.search"), // todo better format for translation strings?
                                                        this.query,
                                                        true,
                                                        32).onEnter(BrowseScreen.this.query::setNow).tag("searchbar"),
                                                new IconButton(new ResourceKey("cosmetica", "textures/filter.png"), ()-> this.open(Menu.FILTER)).tag("btn-search-adjust"),  // filter
                                                new IconButton(new ResourceKey("cosmetica", "textures/sort.png"), ()-> this.open(Menu.SORT)).tag("btn-search-adjust") // sort
                                        ).withStyle(Style.create()
                                                .set(WIDTH, percent(100, 0))
                                                .set(MIN_HEIGHT, fixedSize(20))
                                                .set(PADDING, fixed(new Margins(1, 1, 0, 1)))
                                                .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                                                .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)),
                                        new Results().tag("results")
                                ),
                                new ConfigureCosmetic()
                        ).tag("results-wrapper")
                ).withStyle(Style.create()
                        .set(WIDTH, fixedSize(250))
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
        ).withStyle(Style.create()
                .set(PADDING, fixed(new Margins(30, 10, 12, 10))));
    }

    private void open(Menu menu) {
        if (this.menu.peek() == menu) {
            this.menu.set(Menu.NONE);
        } else {
            this.menu.set(menu);
        }
    }

    @Override
    public void unmount() {
        // Clear non-persistent state (user sub-action)
        this.configuring.set(Optional.empty());
        this.configuringDownloaded.set(null);
        this.menu.set(Menu.NONE);
        // We keep search query, sort, and filters
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("results-wrapper", Style.create()
                        .set(HEIGHT, percent(0, 100)))
                .tag("results", Style.create()
                        .set(MARGINS, fixed(new Margins(4, 0, 0, 0)))
                        .set(HEIGHT, (vw, vh, pw, ph) -> OptionalInt.of(ph - 22)))
                .tag("btn-search-adjust", Style.create()
                        .set(HEIGHT, fixedSize(20))
                        .set(WIDTH, fixedSize(20)))
                .tag("searchbar", Style.create()
                        .set(WIDTH, (vw, vh, pw, ph) -> OptionalInt.of(pw - 22 * 2)));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "browse");

    private enum Menu {
        NONE,
        SORT,
        FILTER
    }

    private enum Sort {
        NEWEST("label.cosmetica.sort.newest", SearchCosmeticsDto.SortByEnum.NEWEST),
        OLDEST("label.cosmetica.sort.oldest", SearchCosmeticsDto.SortByEnum.OLDEST),
        MOST_POPULAR("label.cosmetica.sort.most_popular", SearchCosmeticsDto.SortByEnum.MOST_POPULAR),
        LEAST_POPULAR("label.cosmetica.sort.least_popular", SearchCosmeticsDto.SortByEnum.LEAST_POPULAR);

        Sort(String translationKey, SearchCosmeticsDto.SortByEnum sortByEnum) {
            this.text = Text.translatable(translationKey);
            this.dtoEnumValue = sortByEnum;
        }

        private final Text text;
        private final SearchCosmeticsDto.SortByEnum dtoEnumValue;

        Text text() {
            return this.text;
        }
    }

    /**
     * Browser results. Automatically updates.
     */
    private class Results extends Component {
        private final State<List<Component>> pageResults = new State<>(Collections.emptyList());
        private volatile int searchId = 0;

        @Override
        public List<Component> build() {
            // Acquire states
            final String query = BrowseScreen.this.query.acquire(this);
            final Sort sort = BrowseScreen.this.sort.acquire(this);
            final Set<SearchCosmeticsDto.AttachmentsEnum> filter = BrowseScreen.this.filter.acquire(this);
            // reset page when query/sort/filter changes
            BrowseScreen.this.page.setNow(1);

            // Layout
            return ImmutableList.of(
                    new Div() {
                        @Override
                        public List<Component> build() {
                            int page = BrowseScreen.this.page.acquire(this);

                            @Nullable Cosmetics outfit = Cosmetica.OWN_COSMETICS.acquire(this);

                            // ! Can be removed on website whilst this screen is open
                            if (outfit == null) {
                                Screens.closeCurrentScreen();
                            } else {
                                // Build Search
                                final int nextId = Results.this.searchId + 1;
                                Results.this.searchId = nextId;

                                SearchCosmeticsDto dto = new SearchCosmeticsDto();
                                //dto.set
                                dto.setQuery(query);
                                dto.setAttachments(new ArrayList<>(filter));
                                dto.setSortBy(sort.dtoEnumValue);
                                dto.setPageSize(BigDecimal.valueOf(20L));
                                dto.setPage(BigDecimal.valueOf(page)); // TODO paging

                                // Send Search
                                CosmeticaAPI.search().requestAsync(api -> api.searchCosmetics(dto))
                                        .thenAcceptAsync(cosmetics -> {
                                            ArrayList next = new ArrayList();
                                            CosmeticEntry.populateBrowseList(next, cosmetics.getResults(), outfit, (data, options, envelope, submit) -> {
                                                BrowseScreen.this.configuring.set(Optional.of(new SelectedCosmeticTriple(data, options, submit)));
                                                // Set up Preview
                                                switch (envelope.getType()) {
                                                case COSMETIC:
                                                case TEXTURE_COSMETIC:
                                                case UNKNOWN_DEFAULT_OPEN_API:
                                                    break;
                                                case ANIMATED_TEXTURE_COSMETIC:
                                                    assert envelope.getAnimatedTextureCosmetic() != null; // guaranteed by API
                                                    BrowseScreen.this.configuringDownloaded.set(ImageCosmetic.fromAPI(envelope.getAnimatedTextureCosmetic(), "cape"));
                                                    break;
                                                case ACCESSORY:
                                                    assert envelope.getAccessory() != null; // guaranteed
                                                    BrowseScreen.this.configuringDownloaded.set(Accessory.fromAccessory(envelope.getAccessory()));
                                                    break;
                                                }
                                            });
                                            if (nextId == searchId) Results.this.pageResults.set(next);
                                            BrowseScreen.this.pageCap.set(cosmetics.getEstimatedPages().intValue());
                                            BrowseScreen.this.selected.set(null);
                                        }, Minecraft.getInstance())
                                        .exceptionally(ex -> {
                                            Logging.getInstance().error("Error performing search for " + query, ex);
                                            // TODO show error visually
                                            return null;
                                        });
                            }

                            return ImmutableList.of(
                                    new EntryList.DynamicDiv(Results.this.pageResults, BrowseScreen.this.selected::acquire),
                                    new Div() { // Page buttons and page label
                                        @Override
                                        public List<Component> build() {
                                            int page = BrowseScreen.this.page.acquireInstant(this);
                                            int pageCap = BrowseScreen.this.pageCap.acquire(this);
                                            return ImmutableList.of(
                                                    new IconButton(new ResourceKey("cosmetica", page <= 1 ? "textures/page-left-disabled.png" : "textures/page-left.png"), () -> { int p = BrowseScreen.this.page.peek(); if (p > 1) BrowseScreen.this.page.set(BrowseScreen.this.page.peek() - 1); })
                                                            .setDisabled(page <= 1).tag("page-button"),
                                                    new Label(Text.literal(page + " / " + pageCap)),
                                                    new IconButton(new ResourceKey("cosmetica", page >= pageCap ? "textures/page-right-disabled.png" : "textures/page-right.png"), () -> { int p = BrowseScreen.this.page.peek(); if (p < BrowseScreen.this.pageCap.peek()) BrowseScreen.this.page.set(p + 1); })
                                                            .setDisabled(page >= pageCap).tag("page-button")
                                            );
                                        }
                                    }.tag("page-turner")
                            );
                        }
                    }.tag("results-container")
            );
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .tag("results-container", Style.create()
                            .set(HEIGHT, screen(0, 70))
                            .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                            .set(WIDTH, fixedSize(250)))
                    .tag("page-turner", Style.create()
                            .set(HEIGHT, fixedSize(12))
                            .set(FLOW_DIRECTION, Axis2D.POSITIVE_X)
                            .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                            .set(MARGINS, fixed(new Margins(0, 6, 0, 0)))
                            .set(WIDTH, percent(100, 0)))
                    .tag("page-button", Style.create()
                            .set(WIDTH, fixedSize(30)))
                    .component(EntryList.DynamicDiv.class, Style.create()
                            .set(WIDTH, fixedSize(250))
                            .set(HEIGHT, (vw, vh, rw, rh) -> OptionalInt.of(rh - 13)));
        }
    }

    private class ConfigureCosmetic extends Div {
        @Override
        public List<Component> build() {
            Optional<SelectedCosmeticTriple> configuring = BrowseScreen.this.configuring.acquire(ConfigureCosmetic.this);

            if (configuring.isPresent()) {
                SelectedCosmeticTriple triple = configuring.get();

                // TODO handle max number of cosmetics on outfit (also remove original check due to cape replacement)
                // Because outfit can change whilst browsing.
                State<Float> xOffset = new State<>(0.5f);
                State<Float> yOffset = new State<>(0.5f);
                State<Float> zOffset = new State<>(0.5f);
                State<Boolean> mirroredOrCloak = new State<>(triple.getMiddle() instanceof CapeOptions);
                State<Boolean> elytra = new State<>(true);
                // lock for 'is setting'
                State<Boolean> settingLock = new State<>(false);

                return Arrays.asList(
                        DataForwarder.merge(
                                BrowseScreen.this.options,
                                xOffset, yOffset, zOffset,
                                mirroredOrCloak,
                                elytra),
                        new Div() {
                    @Override
                    public List<Component> build() {
                        // need to have a reference to original outfit
                        // outfit should usually != null as the screen will be closed by Results.
                        // In such a case, there will likely be one frame in which this is called with outfit == null
                        @Nullable Cosmetics outfit = Cosmetica.OWN_COSMETICS.acquire(this);

                        // image
                        List<Component> children = new ArrayList<>();
                        children.add(
                                new Image(new ResourceKey(triple.getLeft().getThumbnail().location))
                                        .setTransparent(1.0f));
                        // name
                        children.add(
                                new Label(Text.literal(triple.getLeft().getName()))
                                        .withStyle(Style.create().set(MARGINS, fixed(new Margins(0,0,6,0)))));
                        // settings
                        CosmeticOptions options = triple.getMiddle();
                        boolean mirrored; // move scope to outer block
                        if (options instanceof AccessoryOptions) {
                            AccessoryOptions ao = (AccessoryOptions) options;

                            // mirrored
                            mirrored = mirroredOrCloak.acquire(this);
                            children.add(new Button(Text.translatable("button.cosmetica.equip.mirrored", mirrored ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()), () -> mirroredOrCloak.set(!mirrored)));

                            // axis positions
                            if (ao.getXRange().getRange() > 0) {
                                float precision = 0.5f / (float) ao.getXRange().getRange();
                                children.add(new SliderWidget(xOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.x", String.format("%.1f", ao.getXRange().clampMap(f_)))));
                            }
                            if (ao.getYRange().getRange() > 0) {
                                float precision = 0.5f / (float) ao.getYRange().getRange();
                                children.add(new SliderWidget(yOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.y", String.format("%.1f", ao.getYRange().clampMap(f_)))));
                            }
                            if (ao.getZRange().getRange() > 0) {
                                float precision = 0.5f / (float) ao.getZRange().getRange();
                                children.add(new SliderWidget(zOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.z", String.format("%.1f", ao.getZRange().clampMap(f_)))));
                            }
                        } else {
                            // to ensure effectively final value in greater scope
                            // preferred over peek in lambda to guarantee no issues from race conditions
                            mirrored = false;
                            if (options instanceof CapeOptions) {
                                CapeOptions co = (CapeOptions) options;

                                if (co.isCloak()) {
                                    boolean isCloak = mirroredOrCloak.acquire(this);
                                    children.add(new Button(Text.translatable("button.cosmetica.equip.isCloak", isCloak ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()), () -> mirroredOrCloak.set(!isCloak)));
                                }

                                if (co.isElytra()) {
                                    boolean isElytra = elytra.acquire(this);
                                    children.add(new Button(Text.translatable("button.cosmetica.equip.isElytra", isElytra ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()), () -> elytra.set(!isElytra)));
                                }
                            }
                        }
                        // space
                        children.add(new Div().tag("flex-1"));
                        // submit
                        final Button submitButton = new Button(Text.translatable("button.cosmetica.equip"), () -> {
                            // Create the outfit changes
                            CreateOutfitDto dto = new CreateOutfitDto();

                            if (options instanceof AccessoryOptions) {
                                if (outfit == null) {
                                    throw new IllegalStateException("Outfit is null but submit button was pressed?");
                                }

                                AccessoryOptions ao = (AccessoryOptions) options;

                                // Equip accessory. Should be no illegal duplicates as button will be disabled.
                                // Existing Accessory List
                                List<CreateOutfitAccessoryDto> accessoryDtos = new ArrayList<>();
                                for (Accessory accessory : outfit.getAccessories()) {
                                    accessoryDtos.add(EquipUtil.dtoFromAccessory(accessory));
                                }
                                // Add new accessory
                                CreateOutfitAccessoryDto newAccessoryDto = new CreateOutfitAccessoryDto()
                                        .id(triple.getLeft().getId())
                                        .mirrored(mirrored)
                                        .offset(Arrays.asList(
                                                BigDecimal.valueOf(ao.getXRange().clampMap(xOffset.peek())),
                                                BigDecimal.valueOf(ao.getYRange().clampMap(yOffset.peek())),
                                                BigDecimal.valueOf(ao.getZRange().clampMap(zOffset.peek()))
                                        ));
                                accessoryDtos.add(newAccessoryDto);

                                dto.setAccessories(accessoryDtos);
                            } else if (options instanceof CapeOptions) {
                                CapeOptions co = (CapeOptions) options;
                                if (co.isCloak() && mirroredOrCloak.peek()) {
                                    dto.setCloak(triple.getLeft().getId());
                                }
                                if (co.isElytra() && elytra.peek()) {
                                    dto.setElytra(triple.getLeft().getId());
                                }
                            }

                            // lock on this part of screen
                            settingLock.set(true);
                            // submit
                            triple.getRight().apply(dto)
                                    .thenAcceptAsync(newOutfit -> {
                                        // Go back to search
                                        BrowseScreen.this.configuring.set(Optional.empty());
                                        BrowseScreen.this.configuringDownloaded.set(null);
                                    }, Minecraft.getInstance())
                                    .exceptionally(Cosmetica.mainThreadExcept(ex -> {
                                        // Unlock
                                        settingLock.set(false);
                                        // TODO error notification
                                    }));
                        });

                        boolean isSetting = settingLock.acquire(this);

                        // Handle this rare case!
                        // Should be closed next frame by Results anyway.
                        if (outfit == null) {
                            submitButton.setDisabled(true);
                        } else if (isSetting) {
                            submitButton.setDisabled(true);
                            submitButton.withStyle(Style.create()
                                    .set(TOOLTIP, Optional.of(new Tooltip(
                                            Text.translatable("tooltip.cosmetica.equipping")
                                    ))));
                        }
                        // TODO check max outfits
                        // check outfit duplicates
                        else if (options instanceof AccessoryOptions) {
                            for (Accessory existingAccessory : outfit.getAccessories()) {
                                if (existingAccessory.getId().equals(triple.getLeft().getId())) {
                                    if (existingAccessory.isMirrored() == mirrored) {
                                        submitButton.setDisabled(true);
                                        submitButton.withStyle(Style.create()
                                                .set(TOOLTIP, Optional.of(new Tooltip(
                                                        mirrored ?
                                                                Text.translatable("tooltip.cosmetica.alreadyEquippedAccessoryMirrored") :
                                                                Text.translatable("tooltip.cosmetica.alreadyEquippedAccessory")
                                                ))));
                                    }
                                }
                            }
                        }
                        children.add(
                                new Div(
                                    submitButton.tag("flex-1"),
                                    new Div().withStyle(Style.create().set(WIDTH, fixedSize(4))),
                                    // TODO consistency on whether cancel should be allowed?
                                    // Better: prevent equip button from other cosmetics being pressed until a response, with a more generic message
                                    new Button(Text.GUI_CANCEL, () -> {
                                        BrowseScreen.this.configuring.set(Optional.empty());
                                        BrowseScreen.this.configuringDownloaded.set(null);
                                    }).setDisabled(isSetting).tag("flex-1")
                                ).withStyle(Style.create()
                                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                        );

                        return children;
                    }
                }.withStyle(Style.create())
                .tag("flex-1", "configure-main"));
            } else {
                return ImmutableList.of();
            }
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .component(Image.class, Style.create()
                            .set(WIDTH, fixedSize(50))
                            .set(HEIGHT, fixedSize(50)))
                    .tag("configure-main", Style.create()
                            .set(BACKGROUND_COLOUR, OptionalInt.of(0x858585))
                            .set(BORDER, GuiUtils.POPOUT_BORDER)
                            .set(PADDING, fixed(new Margins(1))))
                    .tag("flex-1", Style.create()
                            .set(FLEX, 1));
        }
    }

    // Because ImmutableTriple is a final class, extend MutableTriple for ease of implementation for now
    private static class SelectedCosmeticTriple extends MutableTriple<CosmeticEntry.CosmeticData, CosmeticOptions, Function<CreateOutfitDto, CompletableFuture<Outfit>>> {
        public SelectedCosmeticTriple(CosmeticEntry.CosmeticData image, CosmeticOptions envelope, Function<CreateOutfitDto, CompletableFuture<Outfit>> submit) {
            super(image, envelope, submit);
        }

        @Override
        public void setLeft(CosmeticEntry.CosmeticData left) {
            throw new UnsupportedOperationException("SelectedCosmeticTriple is immutable");
        }

        @Override
        public void setMiddle(CosmeticOptions middle) {
            throw new UnsupportedOperationException("SelectedCosmeticTriple is immutable");
        }

        @Override
        public void setRight(Function<CreateOutfitDto, CompletableFuture<Outfit>> right) {
            throw new UnsupportedOperationException("SelectedCosmeticTriple is immutable");
        }
    }
}

