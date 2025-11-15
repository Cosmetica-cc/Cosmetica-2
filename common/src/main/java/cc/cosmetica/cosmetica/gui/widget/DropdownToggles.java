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
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.Streams;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Dropdown toggles.
 */
public class DropdownToggles<T> extends Div {
    /**
     * Create a dropdown menu with the given items.
     * @param state a state for selected toggles.
     * @param textMap the text map.
     * @param options the options to select.
     */
    public DropdownToggles(State<Set<T>> state, Function<T, Text> textMap, T ...options) {
        this.options = options;
        this.state = state;
        this.textMap = textMap;
    }

    private final T[] options;
    private final State<Set<T>> state;
    private final Function<T, Text> textMap;

    @Override
    public List<Component> build() {
        Set<T> selected = this.state.acquire(this);

        return Streams.zip(
                        Arrays.stream(options).map(this.textMap),
                        Arrays.stream(options),
                        AbstractMap.SimpleEntry::new)
                .map(option -> createOption(option.getValue(), option.getKey(), selected.contains(option.getValue())))
                .collect(Collectors.toList());
    }

    protected Component createOption(T option, Text text, boolean selected) {
        return new Div() {
            @Override
            public List<Component> build() {
                return Arrays.asList(
                        new Label(text),
                        new Image(new ResourceKey("cosmetica", selected ? "textures/checkbox_check.png" : "textures/checkbox_empty.png"))
                                .withStyle(Style.create()
                                        .set(HEIGHT, fixedSize(12))
                                        .set(WIDTH, fixedSize(12)))
                );
            }

            @Override
            public void mouseClicked(Element target, double x, double y, int button) {
                GuiUtils.playClick();
                // set always updates component
                // TODO debouncing?
                if (state.peek().contains(option)) {
                    state.peek().remove(option);
                } else {
                    state.peek().add(option);
                }
                state.set(state.peek());
            }
        }.tag("dropdown-item");
    }

    @Override
    public Stylesheet getStylesheet() {
        return DropdownMenu.getDropdownStylesheet()
                .self(Style.create()
                        .set(WIDTH, percent(50, 0)))
                .tag("dropdown-item", Style.create()
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                        .set(Div.ALIGN_ITEMS, Align.CENTRE)
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X));
    }
}
