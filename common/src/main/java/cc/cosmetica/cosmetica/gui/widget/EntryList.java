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

import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.gui.Align;
import cc.cosmetica.kupe.api.gui.Border;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * GUI widgets with many entries which can be selected.
 * Warning: These components will override tags on the entries to manage selection.
 */
public final class EntryList {
    private static List<Component> retag(final Component self, List<Component> components, @Nullable Function<Component, ? extends @Nullable Component> acquireSelected) {
        if (acquireSelected != null) {
            @Nullable Component theSelected = acquireSelected.apply(self);

            for (Component component : components) {
                if (component == theSelected) {
                    component.tag("entrylist-selected");
                } else {
                    component.tag();
                }
            }
        }

        return components;
    }

    private static Stylesheet makeStylesheet(@Nullable Style style) {
        return new Stylesheet()
                .self(Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0x000000))
                        .set(PADDING, fixed(new Margins(1)))
                        .set(MIN_HEIGHT, fixedSize(0))
                        .set(Div.ALIGN_ITEMS, Align.STRETCH_START))
                .tag("entrylist-selected", style != null ? style : Style.create()
                        .set(BORDER, Border.create(1, 0xFFFFFF)));
    }

    /**
     * A div which can have its entries selected.
     * Warning: This component will override tags on the entries to manage selection.
     */
    public static class Div extends cc.cosmetica.kupe.api.gui.Div {
        public Div(Component... entries) {
            super(entries);
            this.selected = null;
        }

        public Div(Component[] entries, State<? extends @Nullable Component> selected) {
            super(entries);
            this.selected = selected::acquire;
        }

        public Div selected(Style style) {
            this.selectedStyle = style;
            return this;
        }

        private final @Nullable Function<Component, @Nullable Component> selected;
        private Style selectedStyle;

        @Override
        public List<Component> build() {
            return retag(this, super.build(), this.selected);
        }

        @Override
        public Stylesheet getStylesheet() {
            return makeStylesheet(this.selectedStyle);
        }
    }

    /**
     * A grid which can have its entries selected.
     * Warning: This component will override tags on the entries to manage selection.
     */
    public static class Grid extends cc.cosmetica.kupe.api.gui.Grid {
        public Grid(Component... entries) {
            super(entries);
            this.selected = null;
        }

        public Grid(Component[] entries, Function<Component, ? extends @Nullable Component> selected) {
            super(entries);
            this.selected = selected;
        }

        private final @Nullable Function<Component, ? extends @Nullable Component> selected;

        @Override
        public List<Component> build() {
            return retag(this, super.build(), this.selected);
        }

        @Override
        public Stylesheet getStylesheet() {
            return makeStylesheet(null);
        }
    }
}
