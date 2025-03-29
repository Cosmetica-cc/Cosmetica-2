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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class LoreSelector extends Div {
    public LoreSelector(AtomicBoolean loreModified, State<LoreOptions> availableLores) {
        this.loreModified = loreModified;
        this.availableLores = availableLores;
    }

    // n.b. cosmetica-core places lore text in prefix field of NametagConfig

    private final AtomicBoolean loreModified;
    private final State<LoreOptions> availableLores;
    private State<Integer> lorePage = new State<>(0);

    @Override
    public List<Component> build() {
        LoreOptions loreOptions = this.availableLores.acquire(this);
        int page = lorePage.acquire(this);

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
                if (lore.lore.equals(loreObj.text)) {
                    selected = lore;
                    break;
                }
            return selected;
        });

        return ImmutableList.of(
                new LoreHeader(selectedState).tag("horizontal", "header"),
//                page == 2 ? new Div(
//                        new Div().tag("flex-1"),
//                        new Label(Text.translatable("label.lore.referToWebsite")),
//                        new Button(Text.translatable("button.lore.openWebPanel"), Cosmetica::openWebPanel),
//                        new Div().withStyle(Style.create().set(FLEX, 3))
//                ).tag("flex-1", "refer-to-website") :
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
        );
    }

    @Override
    public Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(MARGINS, fixed(new Margins(30, 10, 12, 10)))
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("header", Style.create()
                        .set(MARGINS, fixed(new Margins(0,0,2,0))))
                .tag("refer-to-website", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0))
                        .set(PADDING, fixed(new Margins(2)))
                        .set(Label.ALIGN_TEXT, Align.CENTRE))
                .tag("lore-type", Style.create()
                        .set(WIDTH, percent(33, 0)))
                .tag("lore-types", Style.create()
                        .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN))
                .component(SelectableLore.class, Style.create()
                        .set(PADDING, fixed(new Margins(1))));
    }

    private static class LoreHeader extends Div {
        public LoreHeader(Function<Component, @Nullable SelectableLore> icon) {
            this.icon = icon;
        }

        private final Function<Component, @Nullable SelectableLore> icon;

        @Override
        public List<Component> build() {
            @Nullable SelectableLore lore = this.icon.apply(this);

            Text displayLore = lore == null ? Text.translatable("label.lore.no_lore") : Text.translatable("label.lore.lore", lore.lore);

            return ImmutableList.of(
                    new Label(displayLore).tag("flex-1"),
//                        new IconButton(new ResourceKey("cosmetica", "textures/colour.png"), () -> {}),
                    new IconButton(new ResourceKey("cosmetica", "textures/remove.png"), () -> {})
            );
        }
    }

    /**
     * Basic Selectable Lore option. Used for titles.
     */
    private class SelectableLore extends Div {
        public SelectableLore(String lore) {
            this.lore = lore;
        }

        protected final String lore;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                Lore current = Cosmetica.SELECTED_LORE.peek();

                if (!Objects.equals(this.lore, current.text)) {
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
                    new Label(Text.literal(lore))
            );
        }

        Lore createLore(Lore current, Lore old) {
            Lore lore = new Lore(this.lore, current.colour, CachedImage.NO_TEXTURE, "");
            lore.old = old;
            return lore;
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover effect
            if (region.shrinkMargins(new Margins(0,6,0,0)).contains(mouseX, mouseY) && !this.getStyle().get(BORDER).isPresent()) {
                canvas.drawRect(region.addMargins(padding), 0x707070);
            }
            super.render(canvas, region, padding, mouseX, mouseY);
        }
    }

    /**
     * Pronoun selectable lore option.
     */
    private class SelectablePronoun extends SelectableLore {
        public SelectablePronoun(String lore) {
            super(lore);
        }

        @Override
        Lore createLore(Lore current, Lore old) {
            Lore lore;

            if (current.getType().equals(UpdateLoreDto.TypeEnum.PRONOUNS)) {
                // count slashes
                int slashes = 0;
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < current.text.length(); i++) {
                    char c = current.text.charAt(i);
                    if (c == '/') slashes++;
                    // immediately on the fourth, cut off. replace the final pronoun.
                    if (slashes == 4) {
                        break;
                    }
                    sb.append(c);
                }

                lore = new Lore(sb + "/" + this.lore, current.colour, CachedImage.NO_TEXTURE, Lore.PRONOUN_SERVICE);
            } else {
                lore = new Lore(this.lore, current.colour, CachedImage.NO_TEXTURE, Lore.PRONOUN_SERVICE);
            }

            lore.old = old;
            return lore;
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
            this.texture = CachedImage.NO_TEXTURE;
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
                    )
            );
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .component(Image.class, Style.create()
                            .set(WIDTH, fixedSize(24))
                            .set(HEIGHT, fixedSize(24))
                            .set(PADDING, fixed(new Margins(3, 0)))
                    )
                    .self(Style.create()
                            .set(PADDING, fixed(new Margins(3)))
                            .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X));
        }

        @Override
        Lore createLore(Lore current, Lore old) {
            return new Lore(this.lore, current.colour, CachedImage.NO_TEXTURE, this.lore /*serviceId*/);
        }
    }
}
