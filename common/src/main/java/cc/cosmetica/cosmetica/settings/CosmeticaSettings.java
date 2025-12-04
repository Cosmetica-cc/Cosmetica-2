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
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.Settings;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings of the mod and api.
 */
public final class CosmeticaSettings {
    private CosmeticaSettings() {
    }

    // Client Settings
    public static final Setting<Boolean> TOGGLE_OUTFIT_WHEEL = new BooleanSetting("setting.cosmetica.wheel", false, false);
    // API Settings
    public static final Setting<Boolean> SHOW_LORE = new BooleanSetting("setting.cosmetica.showLore", true, true);
    public static final Setting<Boolean> SHOW_ACCESSORIES = new BooleanSetting("setting.cosmetica.showAccessories", true, true);
    public static final Setting<Boolean> SHOW_ICONS = new BooleanSetting("setting.cosmetica.showIcons", true, true);
    public static final Setting<Boolean> SHOW_SPECIAL_ICONS = new BooleanSetting("setting.cosmetica.showSpecialIcons", true, true);
    public static final Setting<Boolean> SHOW_OFFLINE_ICONS = new BooleanSetting("setting.cosmetica.showOfflineIcons", true, true);
    public static final Setting<Boolean> SHOW_ONLINE_ACTIVITY = new BooleanSetting("setting.cosmetica.showOnlineActivity", true, true);

    private static final List<Setting<?>> CLIENT_SETTINGS = ImmutableList.of(TOGGLE_OUTFIT_WHEEL);
    private static final List<Setting<?>> API_SETTINGS = ImmutableList.of(TOGGLE_OUTFIT_WHEEL);

    public static final State<List<Setting<?>>> SETTINGS = new State<>(CLIENT_SETTINGS);

    public static void clearSettings() {
        SETTINGS.set(CLIENT_SETTINGS);
    }

    public static void updateSettings(@Nullable Settings settings) {
        if (settings == null) {
            clearSettings();
        } else {
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
}
