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

import cc.cosmetica.cosmetica.settings.Setting;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Button to cycle between setting options.
 */
public class CycleButton<T> extends Div {
    public CycleButton(Setting<T> setting, Supplier<T> cycle, @Nullable String translationKeyBase) {
        this.setting = setting;
        this.cycle = cycle;
        this.translationKeyBase = translationKeyBase;
    }

    private final Setting<T> setting;
    private final Supplier<T> cycle;
    private final @Nullable String translationKeyBase;

    @Override
    public List<Component> build() {
        T value = this.setting.acquire(this);

        Text text;
        if (this.translationKeyBase == null) {
            if (value instanceof Boolean) {
                text = ((Boolean) value) ? Text.GUI_YES : Text.GUI_NO;
            } else {
                text = Text.literal(value.toString());
            }
        } else {
            text = Text.translatable(translationKeyBase + "." + value);
        }

        return Collections.singletonList(new Button(text, () -> this.setting.set(cycle.get()))
                .setDisabled(this.setting.getManagement() != Setting.Management.USER));
    }
}
