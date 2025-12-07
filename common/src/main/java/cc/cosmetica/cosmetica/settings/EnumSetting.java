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

package cc.cosmetica.cosmetica.settings;

import cc.cosmetica.cosmetica.gui.widget.CycleButton;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;

/**
 * Setting which can take a number of enum values.
 * @param <T> the enum class type.
 */
public class EnumSetting<T extends Enum<T>> extends Setting<T> {
    public EnumSetting(String key, T defaultValue) {
        super(key, defaultValue);
        this.translationKeyBase = key;
    }

    private final String translationKeyBase;

    private T cycleEnum() {
        T value = this.get();
        T[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    @Override
    public Component createController() {
        return new CycleButton<>(this, this::cycleEnum, this.translationKeyBase);
    }

    @Override
    public Text createDescription(T value) {
        return null;
    }
}
