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

import java.util.ArrayList;
import java.util.List;

/**
 * Setting which can be true or false.
 */
public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String key, Boolean defaultValue, boolean defaultText) {
        super(key, defaultValue);
        this.defaultText = defaultText;
        this.baseKey = key;
    }

    private final boolean defaultText;
    private final String baseKey;
    private final List<ManagedSetting<?>> dependents = new ArrayList<>();

    // < Dependencies >
    public <T> BooleanSetting forceWhenOff(Setting<T> other, T value) {
        this.dependents.add(new ManagedSetting<>(other, value));
        return this;
    }

    @Override
    protected void onUpdate() {
        if (this.get()) {
            this.dependents.forEach(ManagedSetting::release);
        } else {
            this.dependents.forEach(ManagedSetting::manage);
        }
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
    public Component createController() {
        // TODO make settings update based on dependants (move states to setting somehow?)
        return new CycleButton<>(this, this::cycleBoolean, defaultText ? null : this.baseKey);
    }

    @Override
    public Text createDescription(Boolean value) {
        return Text.translatable(this.baseKey + "." + value + ".description");
    }

    private static class ManagedSetting<T> {
        public ManagedSetting(Setting<T> setting, T value) {
            this.setting = setting;
            this.value = value;
        }

        private final Setting<T> setting;
        private final T value;

        public void manage() {
            this.setting.parentManage(this.value);
        }

        public void release() {
            this.setting.parentManage(null);
        }
    }
}
