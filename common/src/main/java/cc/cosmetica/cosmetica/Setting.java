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

import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.Settings;

import java.util.ArrayList;
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
    private void update(T newValue) {
        this.setting = newValue;
    }

    public void clean() {
        this.modified = false;
    }

    // Client Settings
    public static final Setting<Boolean> TOGGLE_OUTFIT_WHEEL = new Setting<>("setting.cosmetica.wheel", false);
    // API Settings
    public static final Setting<Boolean> SHOW_LORE = new Setting<>("setting.cosmetica.showLore", true);
    public static final Setting<Boolean> SHOW_ACCESSORIES = new Setting<>("setting.cosmetica.showAccessories", true);
    public static final Setting<Boolean> SHOW_ICONS = new Setting<>("setting.cosmetica.showIcons", true);
    public static final Setting<Boolean> SHOW_SPECIAL_ICONS = new Setting<>("setting.cosmetica.showSpecialIcons", true);
    public static final Setting<Boolean> SHOW_OFFLINE_ICONS = new Setting<>("setting.cosmetica.showOfflineIcons", true);
    public static final Setting<Boolean> SHOW_ONLINE_ACTIVITY = new Setting<>("setting.cosmetica.showOnlineActivity", true);

    private static final List<Setting<?>> CLIENT_SETTINGS = ImmutableList.of(TOGGLE_OUTFIT_WHEEL);
    private static final List<Setting<?>> API_SETTINGS = ImmutableList.of(TOGGLE_OUTFIT_WHEEL);

    public static final State<List<Setting<?>>> SETTINGS = new State<>(CLIENT_SETTINGS);

    public static void clearSettings() {
        SETTINGS.set(Setting.CLIENT_SETTINGS);
    }

    public static void updateSettings(Settings settings) {
        // Update setting values
        SHOW_LORE.update(settings.isShowLore());
        SHOW_ACCESSORIES.update(settings.isShowAccessories());
        SHOW_ICONS.update(settings.isShowIcons());
        SHOW_SPECIAL_ICONS.update(settings.isShowSpecialIcons());
        SHOW_OFFLINE_ICONS.update(settings.isShowOfflineIcons());
        SHOW_ONLINE_ACTIVITY.update(settings.isShowOnlineActivity());

        // Create composite list
        List<Setting<?>> loggedInSettings = new ArrayList<>(CLIENT_SETTINGS);
        loggedInSettings.addAll(API_SETTINGS);

        SETTINGS.set(loggedInSettings);
    }
}
