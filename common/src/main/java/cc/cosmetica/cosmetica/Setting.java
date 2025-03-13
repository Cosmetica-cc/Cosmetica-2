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

package cc.cosmetica.cosmetica;

import cc.cosmetica.kupe.api.Text;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Represents a setting.
 */
public class Setting<T> {
    public Setting(String key, T defaultValue) {
        this.name = Text.translatable(key);
        this.setting = defaultValue;
    }

    public final Text name;
    private T setting;
    private boolean modified;

    public T get() {
        return this.setting;
    }

    public boolean isModified() {
        return modified;
    }

    public void set(T newValue) {
        this.setting = newValue;
        this.modified = true;
    }

    public void clean() {
        this.modified = false;
    }

    public static final Setting<Boolean> TOGGLE_OUTFIT_SCREEN = new Setting<>("setting.cosmetica.wheel", false);

    public static final List<Setting<?>> SETTINGS = ImmutableList.of(TOGGLE_OUTFIT_SCREEN);
}
