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

import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;

import javax.annotation.Nullable;

/**
 * Represents a setting.
 */
public abstract class Setting<T> {
    public Setting(String key, T defaultValue) {
        this.name = Text.translatable(key);
        this.setting = defaultValue;
    }

    public final Text name;
    private T setting;
    private @Nullable T managedValue = null;
    private boolean modified;

    public T get() {
        return managedValue != null && !CosmeticaSettings.USE_CLOUD_SETTINGS.get() ? managedValue : setting;
    }

    public boolean isManaged() {
        return managedValue != null && !CosmeticaSettings.USE_CLOUD_SETTINGS.get();
    }

    public boolean isModified() {
        return modified;
    }

    /**
     * Set a new value from the client.
     */
    public void set(T newValue) {
        this.setting = newValue;
        this.modified = true;
    }

    /**
     * Update from API.
     */
    void update(T newValue) {
        this.setting = newValue;
    }

    void manage(T managedValue) {
        this.managedValue = managedValue;
    }

    public void clean() {
        this.modified = false;
    }

    abstract public Component createController(State<T> updater);
    abstract public Text createDescription(T value);
}
