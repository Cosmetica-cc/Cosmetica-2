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

import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticaSettingsScreen extends Screen {
    public CosmeticaSettingsScreen(ResourceKey titleKey, List<Setting<?>> settings) {
        super(titleKey);
        this.settings = settings;
    }

    private final List<Setting<?>> settings;

    @Override
    protected Component[] buildScreen() {
        List<Component> settingComponents = new ArrayList<>();

        // add settings for outfit wheel
        this.settings.forEach(setting -> settingComponents.add(new SettingBlock(setting)));

        return settingComponents.toArray(new Component[0]);
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(SettingBlock.class, Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                        .set(MIN_WIDTH, fixedSize(200))
                        .set(WIDTH, screen(50, 0)));
    }

    public static final ResourceKey SETTINGS_SCREEN = new ResourceKey("cosmetica", "settings");

    private static class SettingBlock extends Div {
        private SettingBlock(Setting<?> setting) {
            this.setting = new State<>(setting);
        }

        private final State<Setting<?>> setting;

        @Override
        public List<Component> build() {
            Setting<?> value = this.setting.acquire(this);

            // create text
            Text text = value.name;
            if (value.isModified()) {
                text = Text.literal("§l" + text.getDisplayString() + "*");
            }

            // create controller
            Component controller;
            @SuppressWarnings("rawtypes") Class clazz = value.get().getClass();

            if (clazz == Boolean.class) {
                controller = CycleButton(value, this::cycleBoolean);
            } else if (Enum.class.isAssignableFrom(clazz)) {
                controller = CycleButton(value, this::cycleEnum);
            } else {
                throw new UnsupportedOperationException("Unsupported setting type: " + clazz);
            }

            // return components
            return ImmutableList.of(new Label(text), controller);
        }

        public static Component CycleButton(Setting<?> value, Runnable cycle) {
            return new Button(
                    Text.translatable(value.name.getString() + "." + value.get()),
                    cycle
            ).withStyle(
                    Style.create().set(WIDTH, fixed(OptionalInt.of(100)))
            );
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void cycleEnum() {
            final Setting<?> setting = this.setting.peek();

            // next enum
            Enum<? extends Enum> value = (Enum) setting.get();
            Enum[] values = value.getDeclaringClass().getEnumConstants();
            value = values[(value.ordinal() + 1) % values.length];
            ((Setting)setting).set(value);

            // refresh
            this.setting.set(this.setting.peek());
        }

        @SuppressWarnings("unchecked")
        private void cycleBoolean() {
            final Setting<?> setting = this.setting.peek();

            // flip boolean
            Boolean b = (Boolean) setting.get();
            ((Setting<Boolean>)setting).set(!b);

            // refresh
            this.setting.set(this.setting.peek());
        }
    }
}
