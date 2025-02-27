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
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * A gui widget with many entries which can be selected.
 * Warning: This component will override tags on the entries to manage selection.
 */
public class EntryList extends Div {
    public EntryList(Component... entries) {
        super(entries);
        this.selected = null;
    }

    public EntryList(Component[] entries, State<@Nullable Component> selected) {
        super(entries);
        this.selected = selected;
    }

    private final @Nullable State<@Nullable Component> selected;

    @Override
    public List<Component> build() {
        List<Component> components = super.build();

        if (this.selected != null) {
            @Nullable Component selected = this.selected.acquire(this);

            for (Component component : components) {
                if (component == selected) {
                    component.tag();
                } else {
                    component.tag("entrylist-selected");
                }
            }
        }

        return components;
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0x000000))
                        .set(BORDER, Border.create(1, 0xFFFFFF))
                        .set(PADDING, fixed(new Margins(1)))
                        .set(FIXED_CONTAINER, false)
                        .set(Div.ALIGN_ITEMS, Align.STRETCH_START))
                .tag("entrylist-selected", Style.create()
                        .set(BORDER, Border.create(1, 0xFFFFFF)));
    }
}
