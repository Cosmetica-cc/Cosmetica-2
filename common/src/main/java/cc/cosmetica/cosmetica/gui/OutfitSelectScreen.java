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

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.cosmetica.gui.widget.OutfitCount;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import gg.cloaks.javaclient.api.PremiumApi;
import gg.cloaks.javaclient.model.PlanRestrictions;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The menu outfit select screen. For the wheel, see {@link OutfitWheelScreen}.
 */
public class OutfitSelectScreen extends Component implements AnimatedTextureScreen {
    public OutfitSelectScreen() {
        this.title = ID.translationKey("screens");
        this.outfitLimit = new State<>(-1);

        CosmeticaAPI.premiumApi().requestAsync(PremiumApi::getRestrictions)
                .thenApply(PlanRestrictions::getMaxOutfits)
                .thenApply(BigDecimal::intValue)
                .thenAcceptAsync(this.outfitLimit::set, Minecraft.getInstance());
//        Cosmetica.fetchOutfits();
    }

    private final Text title;
    private final State<Integer> outfitLimit;

    @Override
    public List<Component> build() {
        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.acquire(this);

        SelectableOutfit[] components = options.stream()
                .map(SelectableOutfit::new)
                .toArray(SelectableOutfit[]::new);

        return Arrays.asList(
                new Div(
                        new Label(this.title),
                        new OutfitCount(this.outfitLimit)
                ).tag("title"),
                new Div(
                        new EntryList.Grid(
                                components,
                                grid -> Cosmetica.SELECTED_OUTFIT_ID.extract(grid, id -> find(components, id.orElse("")))
                        ).withStyle(Style.create()
                                .set(WIDTH, screen(75, 0))
                                .set(MIN_WIDTH, screen(75, 0))
                                .set(MIN_HEIGHT, screen(0, 60))
                                .set(EntryList.Grid.COLUMN_GAP, 2)
                                .set(EntryList.Grid.ROW_GAP, 2)
                                .set(BACKGROUND_COLOUR, OptionalInt.empty())),
                        new Button(Text.translatable("label.cosmetica.newOutfit"), ()->Screens.setScreen(CreateNewOutfitScreen.ID)),
                        new Button(Text.GUI_DONE, Screens::closeCurrentScreen)
                ).tag("body")
        );
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return new Stylesheet()
                .tag("body", Screen.BODY_DEFAULT_STYLE)
                .tag("title", Screen.TITLE_DEFAULT_STYLE)
                .tag("body", Style.create()
                        // 15(title margin) + 6(related to text height) + 2(extra gap)
                        .set(MARGINS, fixed(new Margins(15 + 6 + 2, 0, 0, 0))))
                .component(SelectableOutfit.class, Style.create()
//                        .set(POINTER_EVENTS, PointerEvents.ALL)
                        .set(WIDTH, fixed(OptionalInt.of(69 * 2/3)))
                        .set(HEIGHT, fixed(OptionalInt.of(69))));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "outfit_select");
    public static final ResourceKey NEW_OUTFIT_ICON = new ResourceKey("cosmetica", "textures/new_outfit.png");

    private static SelectableOutfit find(SelectableOutfit[] components, String id) {
        if (id.isEmpty()) return null;

        for (SelectableOutfit outfit : components) {
            if (outfit.option.id.equals(id)) {
                return outfit;
            }
        }

        // none matched
        return null;
    }

    /**
     * A selectable outfit item in the menu.
     */
    static class SelectableOutfit extends LayeredSpace {
        SelectableOutfit(OutfitWheelScreen.OutfitOption option) {
            super(true);
            this.option = option;
        }

        private final OutfitWheelScreen.OutfitOption option;
        // todo perhaps transparency more cleanly done as a reactive state on the icon
        private Image icon;

        @Override
        public List<Component> build() {
            final int deleteButtonSize = 15;
            final ResourceKey deleteTexture = new ResourceKey("cosmetica", "textures/remove.png");

            return Arrays.asList(
                    new Image(new ResourceKey(option.thumbnail.location))
                            .crop(0, 0.1667f, 0, 0.1667f)
                            .setTransparent(option.usable ? 1.0f : 0.5f),
                    (this.icon = new Image(deleteTexture) {
                        @Override
                        public void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
                            if (region.contains(mouseX, mouseY)) {
                                boolean selected = SelectableOutfit.this.option.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""));
                                // don't draw icon on selected item
                                if (!selected) {
                                    canvas.setTransparency(1.0f);
                                    canvas.setTexture(deleteTexture);

                                    PolyBuilder builder = canvas.drawQuads(PolyBuilder.Mode.POSITION_COLOUR_TEXTURE);

                                    // anticlockwise
                                    builder.vertex(region.getX(), region.getEndY(), 0).colour(1.0f, 0.2f, 0.2f, 0.8f).uv(0, 1).endVertex();
                                    builder.vertex(region.getEndX(), region.getEndY(), 0).colour(1.0f, 0.2f, 0.2f, 0.8f).uv(1, 1).endVertex();
                                    builder.vertex(region.getEndX(), region.getY(), 0).colour(1.0f, 0.2f, 0.2f, 0.8f).uv(1, 0).endVertex();
                                    builder.vertex(region.getX(), region.getY(), 0).colour(1.0f, 0.2f, 0.2f, 0.8f).uv(0, 0).endVertex();

                                    builder.build();
                                }
                            } else {
                                super.paint(canvas, region, mouseX, mouseY);
                            }
                        }

                        @Override
                        public void mouseClicked(Element target, double x, double y, int button) {
                            if (target.getComponent() == this) {
                                // delete outfit confirm
                                // can't delete current outfit
                                if (SelectableOutfit.this.option.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""))) return;
                                // play click sound
                                GuiUtils.playClick();
                                // delete
                                Screens.setScreen(new ConfirmRemoveOutfitScreen(
                                        SelectableOutfit.this.option.id,
                                        SelectableOutfit.this.option.name
                                ), Text.translatable("screens.cosmetica.confirmDeletion"));
                            }
                        }
                    }).withStyle(Style.create().set(MARGINS, fixed(new Margins(0, 0, 69-deleteButtonSize, 69*2/3 - deleteButtonSize))))
            );
        }

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (!this.option.usable) return;
            // require first child clicked (i.e. the image, not the delete)
            if (!target.getParent().isPresent() || target.getParent().get().getComponent() != this || target.getParent().get().getChildren().get(0) != target)
                return;
            if (this.option.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""))) return;
            // play click sound
            GuiUtils.playClick();
            // visually set & make request
            this.option.equipAsync();
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover
            if (region.contains(mouseX, mouseY)) {
                boolean selected = this.option.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""));

                // not selected delete button
                if (selected || !region.shrinkMargins(new Margins(0, 0, 69-15, 69*2/3-15)).contains(mouseX, mouseY)) {
                    // selected icon
                    canvas.setTransparency(0.5f);
                    canvas.drawRect(region, 0x77FFFFFF);
                    canvas.disableTransparency();

                    if (selected) {
                        // don't show icon
                        this.icon.setTransparent(0.0f);
                    } else {
                        this.icon.setTransparent(1.0f);
                    }
                }
                // commented to show complete logic. icon overrides rendering to tint in this case, so not necessary.
//                else {
//                    if (selected)  this.icon.setTransparent(0.0f);
//                    if (!selected) this.icon.setTransparent(0.8f);
//                }
            } else {
                // not selected; don't show icon
                this.icon.setTransparent(0.0f);
            }

            super.render(canvas, region, padding, mouseX, mouseY);
        }
    }
}
