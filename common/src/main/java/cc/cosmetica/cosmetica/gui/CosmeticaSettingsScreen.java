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

import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.cosmetica.gui.widget.CycleButton;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CosmeticaSettingsScreen extends Screen {
    public CosmeticaSettingsScreen(List<Setting<?>> settings) {
        super(ID);
        this.settings = settings;
    }

    private final List<Setting<?>> settings;

    @Override
    protected Component[] buildScreen() {
        List<Component> settingComponents = new ArrayList<>();

        // add settings for outfit wheel
        this.settings.forEach(setting -> settingComponents.add(Setting(setting)));

        return settingComponents.toArray(new Component[0]);
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("setting", Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                        .set(MINIMUM_SIZE, fixed(Optional.of(new Dimensions(200, 0))))
                        .set(WIDTH, screen(50, 0)));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "settings");

    private static Component Setting(Setting<?> setting) {
        Component controller;
        @SuppressWarnings("rawtypes") Class clazz = setting.get().getClass();

        if (clazz == Boolean.class) {
            controller = new CycleButton(new State<>(setting), CycleButton::cycleBoolean);
        } else if (Enum.class.isAssignableFrom(clazz)) {
            controller = new CycleButton(new State<>(setting), CycleButton::cycleEnum);
        } else {
            throw new UnsupportedOperationException("Unsupported setting type: " + clazz);
        }

        // create component
        return new Div(new Label(setting.name), controller).tag("setting");
    }
}
