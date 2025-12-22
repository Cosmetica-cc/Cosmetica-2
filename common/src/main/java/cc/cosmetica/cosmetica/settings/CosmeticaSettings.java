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

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.kupe.api.State;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.ExternalCapeSetting;
import gg.cloaks.javaclient.model.Settings;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Settings of the mod and api.
 */
public final class CosmeticaSettings {
    private CosmeticaSettings() {
    }

    // Client profile settings
    public static final Setting<Boolean> TOGGLE_OUTFIT_WHEEL = new BooleanSetting("setting.cosmetica.wheel", false, false);
    /**
     * In a modpack with managed settings, use cloud settings instead.
     */
    public static final Setting<Boolean> USE_CLOUD_SETTINGS = new BooleanSetting("setting.cosmetica.cloud", false, true) {
        @Override
        protected void onUpdate() {
            API_SETTINGS.forEach(Setting::updateValue);
        }
    };

    // Internal settings
    // - from api
    public static final Setting<Boolean> DISABLE_RSE_PROMPT = new BooleanSetting("setting.cosmetica.disableRSEPrompt", false, true);
    // - from local settings
    public static final State<@org.jetbrains.annotations.Nullable String> MODPACK_ID = new State<>(null);

    // API Settings
    public static final Setting<Boolean> USE_MODPACK_ICONS = new BooleanSetting("setting.cosmetica.useModpackIcons", true, true);
    public static final Setting<Boolean> SHOW_ACCESSORIES = new BooleanSetting("setting.cosmetica.showAccessories", true, true);
    public static final Setting<Boolean> SHOW_LORE = new BooleanSetting("setting.cosmetica.showLore", true, true);
    public static final Setting<Boolean> SHOW_SPECIAL_ICONS = new BooleanSetting("setting.cosmetica.showSpecialIcons", true, true)
            .forceWhenOff(USE_MODPACK_ICONS, false);
    public static final Setting<Boolean> SHOW_OFFLINE_ICONS = new BooleanSetting("setting.cosmetica.showOfflineIcons", true, true);
    public static final Setting<Boolean> SHOW_ICONS = new BooleanSetting("setting.cosmetica.showIcons", true, true)
            .forceWhenOff(SHOW_SPECIAL_ICONS, false)
            .forceWhenOff(SHOW_OFFLINE_ICONS, false);
    public static final Setting<Boolean> SHOW_ONLINE_ACTIVITY = new BooleanSetting("setting.cosmetica.showOnlineActivity", true, true);

    public static final List<Setting<?>> CLIENT_SETTINGS = new ArrayList<>(Arrays.asList(TOGGLE_OUTFIT_WHEEL, USE_CLOUD_SETTINGS));
    public static final List<Setting<?>> API_SETTINGS = ImmutableList.of(
            SHOW_ACCESSORIES,
            SHOW_LORE,
            SHOW_ICONS,
            SHOW_OFFLINE_ICONS,
            SHOW_SPECIAL_ICONS,
            USE_MODPACK_ICONS,
            SHOW_ONLINE_ACTIVITY);

    public static final State<List<Setting<?>>> DISPLAY_SETTINGS = new State<>(CLIENT_SETTINGS);
    public static State<List<ExternalCapeSetting>> externalCapeSettings = new State<>(ImmutableList.of());

    public static void clearSettings() {
        DISPLAY_SETTINGS.set(CLIENT_SETTINGS);
        externalCapeSettings.set(ImmutableList.of());
    }

    private static boolean loadedLocal = false;
    public static void refreshLocalSettings() {
        Path localDir = CosmeticaCoreExpectPlatform.getConfigDirectory().resolve("cosmetica");

        try {
            Files.createDirectories(localDir);
        } catch (IOException e) {
            Logging.getInstance().error("Error creating cosmetica config directory", e);
        }

        readModpackSettings(localDir);

        Path file = localDir.resolve("cosmetica.properties");
        Properties properties = new Properties();

        // Save properties
        properties.setProperty("toggle_outfit_wheel", String.valueOf(TOGGLE_OUTFIT_WHEEL.get()));
        properties.setProperty("use_cloud_settings", String.valueOf(USE_CLOUD_SETTINGS.get()));

        // Overwrite with file properties if loading local
        if (!loadedLocal) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            } catch (NoSuchFileException e) {
                Logging.getInstance().info("cosmetica.properties does not exist yet");
            } catch (IOException e) {
                Logging.getInstance().error("Error reading cosmetica.properties", e);
                return; // don't over-write? file system probably restricted somehow
            }

            loadedLocal = true;
        }

        // Write properties to file
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            properties.store(writer, "Per-profile cosmetica settings");
        } catch (IOException e) {
            Logging.getInstance().error("Error saving cosmetica.properties", e);
        }
    }

    private static void packManage(Setting<Boolean> setting, Properties properties, String name) {
        setting.packManage(Boolean.parseBoolean(properties.getProperty(name)));
    }

    private static void readModpackSettings(Path parentFolder) {
        // by default, hide cloud settings
        USE_CLOUD_SETTINGS.setHidden(true);

        Properties properties = new Properties();
        // defaults
        properties.setProperty("modpack_id", "my_modpack");
        properties.setProperty("apply_overrides", String.valueOf(false));
        properties.setProperty("show_accessories", String.valueOf(SHOW_ACCESSORIES.getUserValue()));
        properties.setProperty("show_lore", String.valueOf(SHOW_LORE.getUserValue()));
        properties.setProperty("show_icons", String.valueOf(SHOW_ICONS.getUserValue()));
        properties.setProperty("show_offline_icons", String.valueOf(SHOW_OFFLINE_ICONS.getUserValue()));
        properties.setProperty("show_special_icons", String.valueOf(SHOW_SPECIAL_ICONS.getUserValue()));
        properties.setProperty("use_modpack_icons", String.valueOf(USE_MODPACK_ICONS.getUserValue()));
        properties.setProperty("show_online_activity", String.valueOf(SHOW_ONLINE_ACTIVITY.getUserValue()));

        Path modpackSettings = parentFolder.resolve("pack_settings.properties");
        // if file exists
        try (BufferedReader reader = Files.newBufferedReader(modpackSettings)) {
            properties.load(reader);
            MODPACK_ID.set(properties.getProperty("modpack_id"));

            // apply the settings
            if (Boolean.parseBoolean(properties.getProperty("apply_overrides"))) {
                USE_CLOUD_SETTINGS.setHidden(false);

                packManage(SHOW_ACCESSORIES,        properties, "show_accessories");
                packManage(SHOW_LORE,               properties, "show_lore");
                packManage(SHOW_ICONS,              properties, "show_icons");
                packManage(SHOW_OFFLINE_ICONS,      properties, "show_offline_icons");
                packManage(SHOW_SPECIAL_ICONS,      properties, "show_special_icons");
                packManage(USE_MODPACK_ICONS,       properties, "use_modpack_icons");
                packManage(SHOW_ONLINE_ACTIVITY,    properties, "show_online_activity");
            }
        } catch (NoSuchFileException noSuchFile) {
            // otherwise create/update a template
            // ".disabled" is a widely used extension to communicate 'remove this extension to activate'
            // so we use this for the template
            Path modpackSettingsTemplate = parentFolder.resolve("pack_settings.properties.disabled");
            try (BufferedWriter writer = Files.newBufferedWriter(modpackSettingsTemplate)) {
                properties.store(writer, "Cosmetica Modpack Settings");
            } catch (IOException e) {
                Logging.getInstance().error("Error writing pack settings template", e);
            }
        } catch (IOException e) {
            Logging.getInstance().error("Error reading pack settings", e);
        }
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
            USE_MODPACK_ICONS.update(settings.isUseModpackIcons());
            DISABLE_RSE_PROMPT.update(settings.isDisableRegionalEffectsPrompt());

            // Create composite list
            List<Setting<?>> loggedInSettings = new ArrayList<>(CLIENT_SETTINGS);
            loggedInSettings.addAll(API_SETTINGS);

            DISPLAY_SETTINGS.set(loggedInSettings);

            externalCapeSettings.set(settings.getExternalCapes());
        }
    }
}
