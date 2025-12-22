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
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.State;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.cloaks.javaclient.model.ExternalCapeSetting;
import gg.cloaks.javaclient.model.Settings;
import gg.cloaks.javaclient.model.UpdateExternalCapeSettingDto;
import gg.cloaks.javaclient.model.UpdateLocalSettingsDto;

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
import java.util.stream.Collectors;

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

    private static void packManage(Setting<Boolean> setting, JsonObject properties, String name) {
        setting.packManage(properties.get(name).getAsBoolean());
    }

    private static void readModpackSettings(Path parentFolder) {
        // by default, hide cloud settings
        USE_CLOUD_SETTINGS.setHidden(true);

        Path modpackSettings = parentFolder.resolve("pack_settings.properties");
        // if file exists
        try (BufferedReader reader = Files.newBufferedReader(modpackSettings)) {
            JsonObject properties = new Gson().fromJson(reader, JsonObject.class);

            final String packId = properties.get("modpack_id").getAsString();
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Setting modpack id {}", packId);
            MODPACK_ID.set(packId);

            // apply the settings
            if (properties.get("apply_overrides").getAsBoolean()) {
                Logging.getInstance().info("Applying modpack overrides for pack {}", MODPACK_ID.peek());
                USE_CLOUD_SETTINGS.setHidden(false);

                packManage(SHOW_ACCESSORIES,        properties, "show_accessories");
                packManage(SHOW_LORE,               properties, "show_lore");
                packManage(SHOW_ICONS,              properties, "show_icons");
                packManage(SHOW_OFFLINE_ICONS,      properties, "show_offline_icons");
                packManage(SHOW_SPECIAL_ICONS,      properties, "show_special_icons");
                packManage(USE_MODPACK_ICONS,       properties, "use_modpack_icons");
                packManage(SHOW_ONLINE_ACTIVITY,    properties, "show_online_activity");

                // update external capes
                // TODO allow external capes to be managed

                // Send request
                Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Pack overrides applied locally. Sending request to server...");

                UpdateLocalSettingsDto dto = new UpdateLocalSettingsDto();
                dto.setClientName("cosmetica");
                dto.setDisableRegionalEffectsPrompt(CosmeticaSettings.DISABLE_RSE_PROMPT.get());
                dto.setExternalCapes(CosmeticaSettings.externalCapeSettings.peek().stream()
                        .map(setting -> {
                            UpdateExternalCapeSettingDto dto_ = new UpdateExternalCapeSettingDto();
                            dto_.setService(setting.getService().getValue());
                            dto_.setReplace(setting.isReplace());
                            dto_.setEnabled(setting.isEnabled());
                            return dto_;
                        })
                        .collect(Collectors.toList()));
                dto.setShowAccessories(CosmeticaSettings.SHOW_ACCESSORIES.get());
                dto.setShowIcons(CosmeticaSettings.SHOW_ICONS.get());
                dto.setShowLore(CosmeticaSettings.SHOW_LORE.get());
                dto.setShowOnlineActivity(CosmeticaSettings.SHOW_ONLINE_ACTIVITY.get());
                dto.setShowSpecialIcons(CosmeticaSettings.SHOW_SPECIAL_ICONS.get());
                dto.setShowOfflineIcons(CosmeticaSettings.SHOW_OFFLINE_ICONS.get());
            }
        } catch (NoSuchFileException noSuchFile) {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Creating/Updating cosmetica pack settings template");

            JsonObject defaults = new JsonObject();
            // defaults
            defaults.addProperty("modpack_id", "my_modpack");
            defaults.addProperty("apply_overrides", false);
            defaults.addProperty("show_accessories", SHOW_ACCESSORIES.getUserValue());
            defaults.addProperty("show_lore", SHOW_LORE.getUserValue());
            defaults.addProperty("show_icons", SHOW_ICONS.getUserValue());
            defaults.addProperty("show_offline_icons", SHOW_OFFLINE_ICONS.getUserValue());
            defaults.addProperty("show_special_icons", SHOW_SPECIAL_ICONS.getUserValue());
            defaults.addProperty("use_modpack_icons", USE_MODPACK_ICONS.getUserValue());
            defaults.addProperty("show_online_activity", SHOW_ONLINE_ACTIVITY.getUserValue());

            JsonArray arr = new JsonArray();
            // defaults from website
            for (ExternalCapeSetting.ServiceEnum service : ExternalCapeSetting.ServiceEnum.values()) {
                JsonObject serviceObject = new JsonObject();
                serviceObject.addProperty("service", service.getValue());
                serviceObject.addProperty("enabled", true);
                arr.add(serviceObject);
            }
            defaults.add("external_capes", arr);

            // otherwise create/update a template
            // ".disabled" is a widely used extension to communicate 'remove this extension to activate'
            // so we use this for the template
            Path modpackSettingsTemplate = parentFolder.resolve("pack_settings.properties.disabled");
            try (BufferedWriter writer = Files.newBufferedWriter(modpackSettingsTemplate)) {
                Gson g = new GsonBuilder().setPrettyPrinting().create();
                g.toJson(defaults, writer);
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
