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
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The menu that appears as a dropdown.
 */
public class DropdownMenu<T> extends Div {
    public DropdownMenu(Consumer<T> callback, Function<T, Text> textMap, T ...options) {
        this.options = options;
        this.textMap = textMap;
        this.callback = callback;
    }

    private T[] options;
    private Consumer<T> callback;
    private Function<T, Text> textMap;

    @Override
    public List<Component> build() {
        return Streams.zip(
                        Arrays.stream(options).map(this.textMap),
                        Arrays.stream(options),
                        AbstractMap.SimpleEntry::new)
                .map(option -> new Label(option.getKey()) {
                    @Override
                    public void mouseClicked(Element target, double x, double y, int button) {
                        GuiUtils.playClick();
                        callback.accept(option.getValue());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(Label.ALIGN_TEXT, Align.START)
                        .set(ALIGN_ITEMS, Align.STRETCH_START)
                        .set(MARGINS, fixed(new Margins(24,0,0,0)))
                        .set(PADDING, fixed(new Margins(1, 2)))
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0x858585))
                        .set(BORDER, GuiUtils.POPOUT_BORDER)
                        .set(ALIGN_SELF, Optional.of(Align.END))
                        .set(WIDTH, percent(30, 0)));
    }
}
