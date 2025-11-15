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

import cc.cosmetica.cosmetica.gui.GuiUtils;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The menu that appears as a dropdown.
 */
public class DropdownMenu<T> extends Div {
    /**
     * Create a dropdown menu for items.
     * @param state the selection state.
     * @param textMap the text map.
     * @param options the options to select.
     */
    public DropdownMenu(State<T> state, Function<T, Text> textMap, T ...options) {
        this.options = options;
        this.textMap = textMap;
        this.state = state;
    }

    private final T[] options;
    private final State<T> state;
    private final Function<T, Text> textMap;

    @Override
    public List<Component> build() {
        T selected = this.state.acquire(this);

        return Streams.zip(
                        Arrays.stream(options).map(this.textMap),
                        Arrays.stream(options),
                        AbstractMap.SimpleEntry::new)
                .map(option -> createOption(option.getValue(), option.getKey(), option.getValue() == selected))
                .collect(Collectors.toList());
    }

    protected Component createOption(T option, Text text, boolean selected) {
        Label result = new Label(text) {
            @Override
            public void mouseClicked(Element target, double x, double y, int button) {
                GuiUtils.playClick();
                // set always updates component, so prevent updates
                if (state.peek() != option) {
                    state.set(option);
                }
            }
        };
        if (selected) {
            result.tag("dropdown-item", "dropdown-selected");
        } else {
            result.tag("dropdown-item");
        }
        return result;
    }

    @Override
    public Stylesheet getStylesheet() {
        return getDropdownStylesheet();
    }

    // Method for hot swap.
    // TODO make into a field in production.
    public static Stylesheet getDropdownStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(Label.ALIGN_TEXT, Align.START)
                        .set(ALIGN_ITEMS, Align.STRETCH_START)
                        .set(MARGINS, fixed(new Margins(24,0,0,0)))
                        .set(PADDING, fixed(new Margins(1, 2)))
                        .set(BACKGROUND_COLOUR, OptionalInt.of(GuiUtils.NORMAL_COLOUR))
                        .set(BORDER, GuiUtils.POPOUT_BORDER)
                        .set(ALIGN_SELF, Optional.of(Align.END))
                        .set(WIDTH, percent(30, 0))
                        .set(MAXIMUM_SIZE, screen(100, 75, Dimensions::new)))
                .tag("dropdown-item", Style.create()
                        .set(PADDING, fixed(new Margins(1, 0))))
                .tag("dropdown-selected", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(GuiUtils.HIGHLIGHT_COLOUR)));
    }
}
