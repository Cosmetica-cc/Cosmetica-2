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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.AccessoryOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CapeOptions;
import cc.cosmetica.cosmetica.gui.cosmeticconfig.CosmeticOptions;
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.DropdownMenu;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.cosmetica.gui.widget.SliderWidget;
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
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.*;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.Div.ALIGN_ITEMS;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Screen for browsing and applying new cosmetics.
 * Should be seamless with {@link HomeScreen}.
 */
public class BrowseScreen extends AbstractHomeScreen {
    public BrowseScreen() {
        super(ID);
        super.lockActions = true;
    }

    private final State<String> searchQuery = new State<>("");
    private final State<Menu> menu = new State<>(Menu.NONE);
    private final State<Sort> sort = new State<>(Sort.RECENT);
    /**
     * The state for the item being configured before being equipped.
     */
    private final State<Optional<SelectedCosmeticTriple>> configuring = new State<>(Optional.empty());

    // TODO use in outfit player for preview (or similar)
    private final State<@Nullable CosmeticEntry> selected = new State<>(null);

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
                        System.out.println(menu);

                        if (menu == Menu.SORT) {
                            return Arrays.asList(
                                    new DropdownMenu<>(sort::set, Sort::text, Sort.values())
                            );
                        }
                        return super.build();
                    }
                }.withStyle(Style.create().set(Z_INDEX, 10)),
                new Div( // container of all the browse area
                        new Div( // top 'head'
                                new TextBox(
                                        Text.translatable("label.browse.search"), // todo better format for translation strings?
                                        this.searchQuery,
                                        true,
                                        32).tag("searchbar"),
                                new Button(Text.literal(" "), ()-> this.open(Menu.SORT)).tag("btn-search-adjust"), // sort
                                new Button(Text.literal(" "), ()-> this.open(Menu.FILTER)).tag("btn-search-adjust")  // filter
                        ).withStyle(Style.create()
                                .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                                .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)),
                        new LayeredSpace( // container for what can appear in search contents
                                true,
                                new Results(),
                                new ConfigureCosmetic()
                        ).tag("results")
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
        this.configuring.set(Optional.empty());
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("results", Style.create()
                        .set(MARGINS, fixed(new Margins(10, 0, 0, 0)))
                        .set(HEIGHT, (vw, vh, pw, ph) -> OptionalInt.of(ph - 22)))
                .tag("btn-search-adjust", Style.create()
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
        RECENT("label.cosmetica.sort.recent"),
        POPULAR("label.cosmetica.sort.popular");
//        OFFICIAL("label.cosmetica.sort.official");

        Sort(String translationKey) {
            this.text = Text.translatable(translationKey);
        }

        private final Text text;

        Text text() {
            return this.text;
        }
    }

    /**
     * Browser results. Automatically updates.
     */
    private class Results extends Component {
        private final State<List<Component>> pageResults = new State<>(Collections.emptyList());
        private volatile int state = 0;

        @Override
        public List<Component> build() {
            // Acquire states
            String query = BrowseScreen.this.searchQuery.acquire(this);
            Sort sort = BrowseScreen.this.sort.acquire(this);

            @Nullable Cosmetics outfit = Cosmetica.OWN_COSMETICS.acquire(this);

            // ! Can be removed on website whilst this screen is open
            if (outfit == null) {
                Screens.closeCurrentScreen();
            } else {
                // Build Search
                final int nextState = this.state + 1;
                this.state = nextState;

                SearchCosmeticsDto dto = new SearchCosmeticsDto();
                //dto.set
                dto.setName(query);

                // Send Search
                CosmeticaAPI.search().requestAsync(api -> api.searchCosmetics(dto))
                        .thenAcceptAsync(cosmetics -> {
                            ArrayList next = new ArrayList();
                            CosmeticEntry.populateBrowseList(next, cosmetics, outfit, (image, envelope, submit) -> {
                                BrowseScreen.this.configuring.set(Optional.of(new SelectedCosmeticTriple(image, envelope, submit)));
                            });
                            this.pageResults.set(next);
                            BrowseScreen.this.selected.set(null);
                        }, Minecraft.getInstance())
                        .exceptionally(ex -> {
                            Logging.getInstance().error("Error performing search for " + query, ex);
                            // TODO show error visually
                            return null;
                        });
            }

            // Layout
            return ImmutableList.of(
                    new EntryList.DynamicDiv(this.pageResults, BrowseScreen.this.selected::acquire)
            );
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .component(EntryList.DynamicDiv.class, Style.create()
                            .set(HEIGHT, screen(0, 70)));
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

                return Arrays.asList(new Div() {
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
                                float precision = 0.1f / (float) ao.getXRange().getRange();
                                children.add(new SliderWidget(xOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.x", Double.toString(ao.getXRange().clampMap(f_)))));
                            }
                            if (ao.getYRange().getRange() > 0) {
                                float precision = 0.1f / (float) ao.getYRange().getRange();
                                children.add(new SliderWidget(yOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.y", Double.toString(ao.getYRange().clampMap(f_)))));
                            }
                            if (ao.getZRange().getRange() > 0) {
                                float precision = 0.1f / (float) ao.getZRange().getRange();
                                children.add(new SliderWidget(zOffset, precision, f_ -> Text.translatable("button.cosmetica.equip.z", Double.toString(ao.getZRange().clampMap(f_)))));
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
                                    new Button(Text.GUI_CANCEL, () -> BrowseScreen.this.configuring.set(Optional.empty())).setDisabled(isSetting).tag("flex-1")
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

