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

import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.settings.Setting;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * An unregistered screen for showing a list of settings.
 */
public class CosmeticaSettingsScreen extends Screen {
    public CosmeticaSettingsScreen(ResourceKey titleKey, State<List<Setting<?>>> settings) {
        super(titleKey);
        this.settings = settings;
    }

    private final State<List<Setting<?>>> settings;

    @Override
    protected Component[] buildScreen() {
        Component[] settings = this.settings.acquire(this).stream()
                .map(SettingBlock::new)
                .map(c -> c.tag("setting-block"))
                .toArray(Component[]::new);

        return new Component[] {
                new Div(settings),
                new MenuEndSelection()
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("setting-block", Style.create()
                        .set(MIN_WIDTH, fixedSize(200))
                        .set(WIDTH, screen(50, 0)));
    }

    public static final ResourceKey SETTINGS_SCREEN = new ResourceKey("cosmetica", "settings");

    private static class SettingBlock<T> extends Div {
        private SettingBlock(Setting<T> setting) {
            this.setting = setting;
            this.state = new State<>(this.originalValue = setting.get());
        }

        private final Setting<T> setting;
        private final State<T> state;
        private final T originalValue;

        @Override
        public List<Component> build() {
            T value = this.state.acquire(this);
            this.setting.set(value);
            // mark unmodified
            if (value == originalValue) {
                this.setting.clean();
            }

            // create text
            Text text = this.setting.name;
            if (this.setting.isModified()) {
                text = Text.literal("§l" + text.getDisplayString() + "*");
            }

            // return components
            Component main = new Div(
                    new Label(text),
                    this.setting.createController(this.state).tag("controller")
            ).tag("setting-display");

            return this.setting.hasDescription() ? ImmutableList.of(main, new Label(this.setting.createDescription(value))) : ImmutableList.of(main);
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .tag("controller", Style.create().set(WIDTH, fixed(OptionalInt.of(100))))
                    .tag("setting-display", Style.create()
                            .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                            .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN))
                    .self(Style.create()
                            .set(Div.ALIGN_ITEMS, Align.STRETCH_CENTRE));
        }
    }
}
