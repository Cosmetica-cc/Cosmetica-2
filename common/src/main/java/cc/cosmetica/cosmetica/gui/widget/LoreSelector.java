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
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.util.Lore;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.LoreOptions;
import gg.cloaks.javaclient.model.UpdateLoreDto;
import gg.cloaks.javaclient.model.UserConnection;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.Div.ALIGN_ITEMS;
import static cc.cosmetica.kupe.api.gui.Div.JUSTIFY_CONTENT;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class LoreSelector extends LayeredSpace {
    public LoreSelector(AtomicBoolean loreModified, State<LoreOptions> availableLores) {
        super(true);
        this.loreModified = loreModified;
        this.availableLores = availableLores;
    }

    // n.b. cosmetica-core places lore text in prefix field of NametagConfig

    private final AtomicBoolean loreModified;
    private final State<LoreOptions> availableLores;
    private State<Integer> lorePage = new State<>(0);
    private State<Boolean> colourSelectorOpen = new State<>(false);
    private State<UpdateLoreDto.ColorEnum> colour = new State<>(Cosmetica.SELECTED_LORE.peek().colour);

    @Override
    public List<Component> build() {
        LoreOptions loreOptions = this.availableLores.acquire(this);
        int page = lorePage.acquire(this);
        this.colour.set(Cosmetica.SELECTED_LORE.peek().colour);

        SelectableLore[] loreValues;
        switch (page) {
            case 0:
                loreValues = loreOptions.getTitles().stream()
                        .map(SelectableLore::new)
                        .toArray(SelectableLore[]::new);
                break;
            case 1:
                loreValues = loreOptions.getPronouns().stream()
                        .map(SelectablePronoun::new)
                        .toArray(SelectableLore[]::new);
                break;
            case 2:
            default:
                // im pretty sure this is allowed since it should clear the acquire when rebuilding
                List<UserConnection> connections = Cosmetica.OWN_CONNECTIONS.acquire(this);
                loreValues = connections.stream()
                        .map(SelectableConnection::new)
                        .toArray(SelectableLore[]::new);
                break;
        }

        // load selected state
        Function<Component, SelectableLore> selectedState = t -> Cosmetica.SELECTED_LORE.extract(t, loreObj -> {
            SelectableLore selected = null;
            for (SelectableLore lore : loreValues)
                if (lore.value.equals(loreObj.value)) {
                    selected = lore;
                    break;
                }
            return selected;
        });

        return ImmutableList.of(
                new Div() {
                    @Override
                    public List<Component> build() {
                        boolean open = LoreSelector.this.colourSelectorOpen.acquire(this);

                        if (open) {
                            return Arrays.asList(
                                    new DropdownMenu<>(
                                            LoreSelector.this.colour,
                                            colour -> Text.literal(new Lore(
                                                    colour.toString().toLowerCase(Locale.ROOT),
                                                    colour, CachedImage.NO_TEXTURE, "").formatted()),
                                            loreOptions.getColors().stream()
                                                    .map(UpdateLoreDto.ColorEnum::fromValue)
                                                    .toArray(UpdateLoreDto.ColorEnum[]::new)
                                    ),
                                    // Data Forwarder
                                    new Div() {
                                        @Override
                                        public List<Component> build() {
                                            UpdateLoreDto.ColorEnum colour = LoreSelector.this.colour.acquire(this);

                                            Lore lore = Cosmetica.SELECTED_LORE.peek();
                                            if (colour != lore.colour) {
                                                Cosmetica.SELECTED_LORE.set(new Lore(
                                                        lore.value,
                                                        lore.displayText,
                                                        colour,
                                                        lore.icon,
                                                        lore.service
                                                ));
                                            }
                                            return super.build();
                                        }
                                    }
                            );
                        } else {
                            return super.build();
                        }
                    }
                }.withStyle(Style.create().set(Z_INDEX, 10)),
                new Div(
                        new LoreHeader(Cosmetica.SELECTED_LORE::acquire, loreOptions.getColors()).tag("horizontal", "header"),
                        (page == 2 && loreValues.length == 0) ? new Div(
                                new Div().tag("flex-1"),
                                new Label(Text.translatable("label.lore.noConnections")),
                                new Button(Text.translatable("button.lore.connectDiscord"), LoreSelector::openConnectDiscord),
                                new Div().withStyle(Style.create().set(FLEX, 3))
                        ).tag("flex-1", "no-connections") :
                                new EntryList.Div(loreValues, selectedState)
                                        .selected(
                                                Style.create()
                                                        .set(BACKGROUND_COLOUR, OptionalInt.of(0xFFFFFF))
                                                        .set(Label.TEXT_COLOUR, 0x333333)
                                        ).tag("flex-1"),
                        new Div(
                                new Button(Text.translatable("button.lore.titles"), () -> {
                                    this.lorePage.set(0);
                                }).setDisabled(page == 0).tag("lore-type"),
                                new Button(Text.translatable("button.lore.pronouns"), () -> {
                                    this.lorePage.set(1);
                                }).setDisabled(page == 1).tag("lore-type"),
                                new Button(Text.translatable("button.lore.connections"), () -> {
                                    this.lorePage.set(2);
                                }).setDisabled(page == 2).tag("lore-type")
                        ).tag("horizontal", "lore-types")
                ).tag("lore-selector-body")
        );
    }

    @Override
    public Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(MARGINS, fixed(new Margins(30, 10, 12, 10))))
                .tag("lore-selector-body", Style.create()
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("header", Style.create()
                        .set(MARGINS, fixed(new Margins(0,0,2,0))))
                .tag("no-connections", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0))
                        .set(PADDING, fixed(new Margins(2)))
                        .set(Label.ALIGN_TEXT, Align.CENTRE))
                .tag("lore-type", Style.create()
                        .set(WIDTH, percent(33, 0)))
                .tag("lore-types", Style.create()
                        .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN));
    }

    private static void openConnectDiscord() {
        Cosmetica.openWebPanel("discord-connect");
    }

    private class LoreHeader extends Div {
        public LoreHeader(Function<Component, Lore> icon, List<String> unlockedColours) {
            this.icon = icon;
            this.unlockedColours = unlockedColours;
        }

        private final Function<Component, Lore> icon;
        private final List<String> unlockedColours;

        @Override
        public List<Component> build() {
            Lore lore = this.icon.apply(this);
            Text displayLore = lore.isNoLore() ? Text.translatable("label.lore.noLore") : Text.translatable("label.lore.lore", lore.formatted());

            List<Component> result = new ArrayList<>();

            result.add(new Label(displayLore).tag("flex-1"));

            // only show colouring button if you have multiple unlocked lore colours
            if (this.unlockedColours.size() > 1) {
                result.add(new IconButton(new ResourceKey("cosmetica", "textures/colour.png"), () -> {
                    boolean open = LoreSelector.this.colourSelectorOpen.peek();
                    LoreSelector.this.colourSelectorOpen.set(!open);
                }));
            }
            result.add(new IconButton(new ResourceKey("cosmetica", "textures/remove.png"), this::clearLore));

            return result;
        }

        private void clearLore() {
            Lore current = Cosmetica.SELECTED_LORE.peek();
            Lore next = Lore.none(current.colour);
            next.old = current.old == null ? current : current.old;
            Cosmetica.SELECTED_LORE.set(next);
            LoreSelector.this.loreModified.set(true);
        }
    }

    /**
     * Basic Selectable Lore option. Used for titles.
     */
    private class SelectableLore extends Div {
        public SelectableLore(String lore) {
            this.value = lore;
        }

        protected final String value;
        boolean allowDuplication = false;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                Lore current = Cosmetica.SELECTED_LORE.peek();

                if (allowDuplication || !Objects.equals(this.value, current.value)) {
                    LoreSelector.this.loreModified.set(true);
                    // we should already be on Minecraft thread.
                    Lore old = current.old == null ? current : current.old;
                    Cosmetica.SELECTED_LORE.set(this.createLore(current, old));
                }
            }
        }

        @Override
        public List<Component> build() {
            return ImmutableList.of(
                    new Label(Text.literal(value))
            );
        }

        Lore createLore(Lore current, Lore old) {
            Lore lore = new Lore(this.value, current.colour, CachedImage.NO_TEXTURE, "");
            lore.old = old;
            return lore;
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover effect
            if (region.addMargins(padding).shrinkMargins(new Margins(0,6,0,0)).contains(mouseX, mouseY) && !this.getStyle().get(BORDER).isPresent()) {
                canvas.drawRect(region.addMargins(padding), 0x707070);
            }
            super.render(canvas, region, padding, mouseX, mouseY);
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet().self(Style.create()
                    .set(PADDING, fixed(new Margins(1))));
        }
    }

    /**
     * Pronoun selectable lore option.
     */
    private class SelectablePronoun extends SelectableLore {
        public SelectablePronoun(String lore) {
            super(lore);
            allowDuplication = true;
        }

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
        }

        @Override
        public List<Component> build() {
            Lore current = Cosmetica.SELECTED_LORE.acquire(this);

            // count slashes
            int slashes = 0;
            for (char c : current.value.toCharArray()) {
                if (c == '/') slashes++;
            }

            return ImmutableList.of(new Div(
                    new Label(Text.literal(this.value)).tag("flex-1"),
                    new Button(Text.literal("+"), () -> super.mouseClicked(null, 0, 0, 0))
                            .setDisabled(slashes == 3) // max 4 pronouns
            ).tag("innerdiv"));
        }

        @Override
        Lore createLore(Lore current, Lore old) {
            Lore lore;

            if (current.getType().equals(UpdateLoreDto.TypeEnum.PRONOUNS)) {
                // count slashes
                int slashes = 0;
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < current.value.length(); i++) {
                    char c = current.value.charAt(i);
                    if (c == '/') slashes++;
                    // immediately on the fourth, cut off. replace the final pronoun.
                    // this is a backup case. this shouldn't be able to happen
                    if (slashes == 4) {
                        break;
                    }
                    sb.append(c);
                }

                lore = new Lore(sb + "/" + this.value, current.colour, CachedImage.NO_TEXTURE, Lore.PRONOUN_SERVICE);
            } else {
                lore = new Lore(this.value, current.colour, CachedImage.NO_TEXTURE, Lore.PRONOUN_SERVICE);
            }

            lore.old = old;
            return lore;
        }

        @Override
        public Stylesheet getStylesheet() {
            return super.getStylesheet()
                    .component(Button.class, Style.create()
                            .set(WIDTH, fixedSize(20)))
                    .tag("innerdiv", Style.create()
                            .set(FLOW_DIRECTION, Axis2D.POSITIVE_X)
                            .set(Label.ALIGN_TEXT, Align.START)
                            .set(WIDTH, percent(67, 0)));
        }
    }

    /**
     * Connection selectable lore option.
     */
    private class SelectableConnection extends SelectableLore {
        public SelectableConnection(UserConnection connection) {
            super(connection.getServiceId() /* Connection lore is set by service id. */);
            this.username = Text.literal(connection.getUsername());
            this.serviceName = Text.literal("§7" + connection.getServiceName());
            this.texture = ThumbnailCache.getOrCreateImage( // should still be ok to store by connection id as if a regular thumb
                    new CosmeticaTexture.Builder(connection.getIconUrl(), Cosmetica.LOADING_TEXTURE)
                            .failToLoadTexture(Cosmetica.FALLBACK_TEXTURE), false);
        }

        private final Text username;
        private final Text serviceName;
        private final CachedImage texture;

        @Override
        public List<Component> build() {
            return ImmutableList.of(
                    new Image(new ResourceKey(this.texture.location)).setTransparent(1),
                    new Div(
                            new Label(this.username),
                            new Label(this.serviceName)
                    ).tag("label-column")
            );
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .component(Image.class, Style.create()
                            .set(WIDTH, fixedSize(24))
                            .set(HEIGHT, fixedSize(24))
                            .set(MARGINS, fixed(new Margins(3, 5, 3, 0)))
                    )
                    .tag("label-column", Style.create()
                            .set(ALIGN_ITEMS, Align.STRETCH_START))
                    .self(Style.create()
                            .set(PADDING, fixed(new Margins(3)))
                            .set(Label.ALIGN_TEXT, Align.START)
                            .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X));
        }

        @Override
        Lore createLore(Lore current, Lore old) {
            return new Lore(this.value, this.username.getDisplayString(), current.colour, this.texture, this.value /*serviceId*/);
        }
    }
}
