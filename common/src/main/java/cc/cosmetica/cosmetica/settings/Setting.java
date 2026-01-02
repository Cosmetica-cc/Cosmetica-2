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
import gg.cloaks.javaclient.model.Settings;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a setting.
 */
public abstract class Setting<T> {
    public Setting(String key, T defaultValue) {
        this.name = Text.translatable(key);
        this.oldUserValue = this.userValue = defaultValue;
        this.actualValue = new State<>(defaultValue);
    }

    public final Text name;

    // Priority of Values
    private @Nullable T parentManagedValue = null;
    private @Nullable T packValue = null;
    private T userValue;
    private T oldUserValue;
    // ...
    private boolean modified;
    private boolean hidden;
    private boolean superHidden;

    protected final State<T> actualValue;

    public final T get() {
        return this.actualValue.peek();
    }

    public final boolean isVisible() {
        return !this.hidden && !this.superHidden;
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    void setSuperHidden(boolean hidden) {
        this.superHidden = hidden;
    }

    public final T getUserValue() {
        return this.userValue;
    }

    public final T acquire(Component component) {
        return this.actualValue.acquire(component);
    }

    public final Management getManagement() {
        return parentManagedValue != null ? Management.PARENT_SETTING : (packValue != null && !CosmeticaSettings.USE_CLOUD_SETTINGS.get()) ? Management.MODPACK : Management.USER;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean hasDescription() {
        return true;
    }

    /**
     * Set a new value from the user, that is, from a GUI, for example.
     */
    public void set(T newValue) {
        this.update(newValue);
        this.modified = !Objects.equals(this.userValue, this.oldUserValue);
    }

    /**
     * Set the user value from the api.
     */
    public void apiUpdate(T newValue, Settings.TypeEnum typeEnum) {
        if (typeEnum == Settings.TypeEnum.CLOUD) {
            this.packValue = null; // user is using cloud settings
            this.oldUserValue = newValue;

            // if user has modified it
            if (!this.isModified()) {
                this.modified = false;
                this.update(newValue);
            } else if (newValue == this.userValue) {
                this.modified = false;
            }
        } else {
            this.packValue = newValue;
            this.updateValue();
        }
    }

    private void update(T newValue) {
        this.userValue = newValue;
        this.updateValue();
    }

    /**
     * Update due to parent setting controlling child setting.
     * @param managedValue the new parent-managed value.
     */
    void parentManage(@Nullable T managedValue) {
        this.parentManagedValue = managedValue;
        this.updateValue();
    }

    void updateValue() {
        // parent managed value takes priority
        if (this.parentManagedValue != null) {
            this.actualValue.set(parentManagedValue);
            this.onUpdate();
        } else {
            // then pack managed value
            if (this != CosmeticaSettings.USE_CLOUD_SETTINGS &&
                    this.packValue != null && !CosmeticaSettings.USE_CLOUD_SETTINGS.get()) {
                this.actualValue.set(packValue);
                this.onUpdate();
            } else {
                this.actualValue.set(userValue);
                this.onUpdate();
            }
        }
    }

    protected void onUpdate() {}

    public void clean() {
        this.modified = false;
        this.oldUserValue = this.userValue;
    }

    abstract public Component createController();
    abstract public Text createDescription(T value);

    public enum Management {
        USER,
        MODPACK,
        PARENT_SETTING
    }
}
