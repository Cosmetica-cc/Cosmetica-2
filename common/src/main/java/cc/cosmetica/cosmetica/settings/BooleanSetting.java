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

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Setting which can be true or false.
 */
public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String key, Boolean defaultValue, boolean defaultText) {
        super(key, defaultValue);
        this.defaultText = defaultText;
        this.baseKey = key;
    }

    private final boolean defaultText;
    private final String baseKey;
    private @Nullable BooleanSetting dependency;

    // < Dependencies >
    public BooleanSetting dependsOn(BooleanSetting other) {
        this.dependency = other;
        return this;
    }

    @Override
    public Boolean get() {
        if (this.dependency != null) {
            return this.dependency.get() && super.get();
        }
        return super.get();
    }

    @Override
    public Management getManagement() {
        if (this.dependency != null && !this.dependency.get()) {
            return Management.PARENT_SETTING;
        } else {
            return super.getManagement();
        }
    }

    @Override
    public boolean isModified() {
        if (this.dependency != null) {
            return this.dependency.isModified() || super.isModified();
        }

        return super.isModified();
    }

    // < /Dependencies >

    private boolean cycleBoolean() {
        return !this.get();
    }

    @Override
    public boolean hasDescription() {
        return this.defaultText;
    }

    @Override
    public Component createController(State<Boolean> updater) {
        return new CycleButton<>(updater, this::cycleBoolean, defaultText ? null : this.baseKey);
    }

    @Override
    public Text createDescription(Boolean value) {
        return Text.translatable(this.baseKey + "." + value + ".description");
    }
}
