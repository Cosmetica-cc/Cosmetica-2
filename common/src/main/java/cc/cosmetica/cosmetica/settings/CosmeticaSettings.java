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
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.Tooltip;
import cc.cosmetica.kupe.api.gui.style.Style;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import gg.cloaks.javaclient.model.*;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

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
    public static final Setting<Boolean> USE_CLOUD_SETTINGS = new BooleanSetting("setting.cosmetica.useCloudSettings", false, true) {
        @Override
        public Component createController() {
            // Will reload when all settings load so doesn't need to update itself (or any other settings)
            boolean useCloudSettings = this.get();
            return new Div() {
                private State<Boolean> disabled = new State<>(false);

                @Override
                public List<Component> build() {
                    boolean isDisabled = disabled.acquire(this);

                    return Collections.singletonList(
                            new Button(
                                    useCloudSettings ? Text.GUI_YES : Text.GUI_NO,
                                    () -> {
                                        disabled.set(true);
                                        USE_CLOUD_SETTINGS.set(!useCloudSettings);

                                        if (useCloudSettings) { // was true (-> false)
                                            // should be pack managed now
                                            if (modpackSettings == null) {
                                                Logging.getInstance().error("Modpack settings should not be null if cloud settings button is visible!");
                                            } else {
                                                // don't let settings update (TODO: we no longer apply packValue locally so this can be removed and code simplified perhaps)
//                                                API_SETTINGS.forEach(Setting::updateValue);
                                                applyLocalSettings();
                                            }
                                        } else { // was false (-> true)
                                            CosmeticaAPI.settings().requestAsync(api -> api.setCloud(new UpdateCloudSettingsDto()))
                                                    .thenAcceptAsync(CosmeticaSettings::updateSettings, Minecraft.getInstance());
                                        }
                                    }
                            ).setDisabled(isDisabled)
                                    .withStyle(Style.create()
                                            .set(TOOLTIP, !isDisabled ? Optional.empty() : Optional.of(
                                                    new Tooltip(Text.translatable("tooltip.cosmetica.updatingSettings"))
                                            )))
                    );
                }
            };
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
    public static final Setting<Boolean> VISIBILITY_OVERRIDES = new BooleanSetting("setting.cosmetica.visibilityOverrides", true, true);

    public static final List<Setting<?>> CLIENT_SETTINGS = new ArrayList<>(Arrays.asList(TOGGLE_OUTFIT_WHEEL, USE_CLOUD_SETTINGS));
    public static final List<Setting<?>> API_SETTINGS = ImmutableList.of(
            SHOW_ACCESSORIES,
            SHOW_LORE,
            SHOW_ICONS,
            SHOW_OFFLINE_ICONS,
            SHOW_SPECIAL_ICONS,
            USE_MODPACK_ICONS,
            SHOW_ONLINE_ACTIVITY,
            VISIBILITY_OVERRIDES);

    public static final State<List<Setting<?>>> DISPLAY_SETTINGS = new State<>(CLIENT_SETTINGS);
    private static @Nullable UpdateLocalSettingsDto modpackSettings;
    public static Setting<List<ExternalCapeSetting>> externalCapeSettings = new Setting<List<ExternalCapeSetting>>("", ImmutableList.of()) {
        @Override
        public Component createController() {
            // - could make ExternalCapesScreen use this and move the control code here
            throw new UnsupportedOperationException("Cannot directly create controller for external cape settings");
        }

        @Override
        public Text createDescription(List<ExternalCapeSetting> value) {
            throw new UnsupportedOperationException("Cannot directly create description for external cape settings");
        }
    };

    private static boolean loadedLocal = false;

    /**
     * Called on authentication and when cloud settings are re-applied.
     */
    public static void applyLocalSettings() {
        if (willApplyLocalSettings()) {
            Logging.getInstance().info( "Applying pack overrides...");

            CosmeticaAPI.settings().requestAsync(api -> api.setLocal(modpackSettings))
                    .thenAcceptAsync(CosmeticaSettings::updateSettings, Minecraft.getInstance())
                    .exceptionally(ex -> {
                        Logging.getInstance().error("Failed to apply modpack settings", ex);
                        return null;
                    });
        } else if (modpackSettings == null) {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Cannot apply pack overrides (modpack settings is null)");
        } else {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Cannot apply pack overrides (cloud settings is enabled)");
        }
    }

    public static boolean willApplyLocalSettings() {
        return !USE_CLOUD_SETTINGS.get() && modpackSettings != null;
    }

    public static void refreshLocalSettings() {
        Path localDir = CosmeticaCoreExpectPlatform.getConfigDirectory().resolve("cosmetica");

        try {
            Files.createDirectories(localDir);
        } catch (IOException e) {
            Logging.getInstance().error("Error creating cosmetica config directory", e);
        }

        UpdateLocalSettingsDto newModpackSettings = readModpackSettings(localDir);
        // cannot remove ability to use modpack settings from an instance once loaded (prevent race condition with button)]
        // can simplify in future if hide/show updates a relevant State
        if (newModpackSettings != null) {
            modpackSettings = newModpackSettings;
        }

        Path file = localDir.resolve("cosmetica.properties");
        Properties properties = new Properties();

        // Default properties
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

            TOGGLE_OUTFIT_WHEEL.apiUpdate(Boolean.parseBoolean(properties.getProperty("toggle_outfit_wheel", "false")), Settings.TypeEnum.CLOUD);
            USE_CLOUD_SETTINGS.apiUpdate(Boolean.parseBoolean(properties.getProperty("use_cloud_settings", "false")), Settings.TypeEnum.CLOUD);

            loadedLocal = true;
        }

        // Write properties to file
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Saving cosmetica.properties");
            properties.store(writer, "Per-profile cosmetica settings");
        } catch (IOException e) {
            Logging.getInstance().error("Error saving cosmetica.properties", e);
        }
    }

    private static void packManage(UpdateLocalSettingsDto dto, BiConsumer<UpdateLocalSettingsDto, Boolean> updater, JsonObject properties, String name) {
        if (properties.get(name) != null && properties.get(name) != JsonNull.INSTANCE) {
            updater.accept(dto, properties.get(name).getAsBoolean());
        } else {
            Logging.getInstance().error("Missing boolean property \"" + name + "\" in modpack config! Defaulting to false.");
            updater.accept(dto, false);
        }
    }

    @Nullable
    private static UpdateLocalSettingsDto readModpackSettings(Path parentFolder) {
        // by default, hide cloud settings
        USE_CLOUD_SETTINGS.setHidden(true);

        Path modpackSettings = parentFolder.resolve("pack_settings.json");
        // if file exists
        try (BufferedReader reader = Files.newBufferedReader(modpackSettings)) {
            JsonObject properties = new Gson().fromJson(reader, JsonObject.class);

            final String packId = properties.get("modpack_id").getAsString();
            final String packName = properties.get("modpack_name").getAsString();
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Setting modpack id {}", packId);
            MODPACK_ID.set(packId);

            // apply the settings
            if (properties.get("modpack_settings_enabled").getAsBoolean()) {
                USE_CLOUD_SETTINGS.setHidden(false);

                Logging.getInstance().info("Loading modpack overrides for pack {}", MODPACK_ID.peek());

                // Create request object
                UpdateLocalSettingsDto dto = new UpdateLocalSettingsDto();

                dto.setClientName(packName);
                packManage(dto, UpdateLocalSettingsDto::setShowAccessories,    properties, "show_accessories");
                packManage(dto, UpdateLocalSettingsDto::setShowLore,           properties, "show_lore");
                packManage(dto, UpdateLocalSettingsDto::setShowIcons,          properties, "show_icons");
                packManage(dto, UpdateLocalSettingsDto::setShowOfflineIcons,   properties, "show_offline_icons");
                packManage(dto, UpdateLocalSettingsDto::setShowSpecialIcons,   properties, "show_special_icons");
                packManage(dto, UpdateLocalSettingsDto::setUseModpackIcons,    properties, "use_modpack_icons");
                packManage(dto, UpdateLocalSettingsDto::setShowOnlineActivity, properties, "show_online_activity");
                packManage(dto, UpdateLocalSettingsDto::setAllowVisibilityOptionOverrides, properties, "visibility_overrides");

                // update external capes
                List<UpdateExternalCapeSettingDto> externalCapeUpdates = new ArrayList<>();
                for (JsonElement element : properties.get("external_capes").getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject externalCapeSetting = element.getAsJsonObject();

                        UpdateExternalCapeSettingDto dto_ = new UpdateExternalCapeSettingDto();
                        dto_.setService(externalCapeSetting.get("service").getAsString());
                        dto_.setEnabled(externalCapeSetting.get("enabled").getAsBoolean());
                        dto_.setReplace(false);

                        externalCapeUpdates.add(dto_);
                    }
                }
                dto.setExternalCapes(externalCapeUpdates);

                // not configured by modpack settings
                dto.setDisableRegionalEffectsPrompt(CosmeticaSettings.DISABLE_RSE_PROMPT.get());

                return dto;
            } else {
                Logging.getInstance().info("modpack_settings_enabled=false. Skipping modpack settings");
            }
        } catch (NoSuchFileException noSuchFile) {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Creating/Updating cosmetica pack settings template");

            JsonObject defaults = new JsonObject();
            // defaults
            defaults.addProperty("modpack_id", "my_modpack");
            defaults.addProperty("modpack_name", "My Modpack");
            defaults.addProperty("modpack_settings_enabled", false);
            defaults.addProperty("show_accessories", SHOW_ACCESSORIES.getUserValue());
            defaults.addProperty("show_lore", SHOW_LORE.getUserValue());
            defaults.addProperty("show_icons", SHOW_ICONS.getUserValue());
            defaults.addProperty("show_offline_icons", SHOW_OFFLINE_ICONS.getUserValue());
            defaults.addProperty("show_special_icons", SHOW_SPECIAL_ICONS.getUserValue());
            defaults.addProperty("use_modpack_icons", USE_MODPACK_ICONS.getUserValue());
            defaults.addProperty("show_online_activity", SHOW_ONLINE_ACTIVITY.getUserValue());
            defaults.addProperty("visibility_overrides", VISIBILITY_OVERRIDES.getUserValue());

            JsonArray arr = new JsonArray();
            // defaults from website
            for (ExternalCapeSetting.ServiceEnum service : ExternalCapeSetting.ServiceEnum.values()) {
                if (service != ExternalCapeSetting.ServiceEnum.UNKNOWN_DEFAULT_OPEN_API) {
                    JsonObject serviceObject = new JsonObject();
                    serviceObject.addProperty("service", service.getValue());
                    serviceObject.addProperty("enabled", true);
                    arr.add(serviceObject);
                }
            }
            defaults.add("external_capes", arr);

            // otherwise create/update a template
            // ".disabled" is a widely used extension to communicate 'remove this extension to activate'
            // so we use this for the template
            Path modpackSettingsTemplate = parentFolder.resolve("pack_settings.json.disabled");
            try (BufferedWriter writer = Files.newBufferedWriter(modpackSettingsTemplate)) {
                Gson g = new GsonBuilder().setPrettyPrinting().create();
                g.toJson(defaults, writer);
            } catch (IOException e) {
                Logging.getInstance().error("Error writing pack settings template", e);
            }
        } catch (IOException e) {
            Logging.getInstance().error("Error reading pack settings", e);
        }

        return null;
    }

    public static void clearSettings() {
        Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Clearing loaded settings...");
        DISPLAY_SETTINGS.set(CLIENT_SETTINGS);
        externalCapeSettings.set(ImmutableList.of());
    }

    public static void updateSettings(CosmeticaUser user) {
        @Nullable Settings settings = user.getActiveSettings();

        if (settings == null) {
            clearSettings();
        } else {
            Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Updating loaded settings for type " + settings.getType());
//            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//            for (StackTraceElement element : stackTrace) {
//                System.out.println(element);
//            }

            // Update setting values
            SHOW_LORE.apiUpdate(settings.isShowLore(), settings.getType());
            SHOW_ACCESSORIES.apiUpdate(settings.isShowAccessories(), settings.getType());
            SHOW_ICONS.apiUpdate(settings.isShowIcons(), settings.getType());
            SHOW_SPECIAL_ICONS.apiUpdate(settings.isShowSpecialIcons(), settings.getType());
            SHOW_OFFLINE_ICONS.apiUpdate(settings.isShowOfflineIcons(), settings.getType());
            SHOW_ONLINE_ACTIVITY.apiUpdate(settings.isShowOnlineActivity(), settings.getType());
            USE_MODPACK_ICONS.apiUpdate(settings.isUseModpackIcons(), settings.getType());
            DISABLE_RSE_PROMPT.apiUpdate(settings.isDisableRegionalEffectsPrompt(), settings.getType());
            VISIBILITY_OVERRIDES.apiUpdate(settings.isAllowVisibilityOptionOverrides(), settings.getType());

            boolean differentModpackId = MODPACK_ID.peek() != null && user.getModpackId() != null && !user.getModpackId().equals(MODPACK_ID.peek());
            USE_CLOUD_SETTINGS.setSuperHidden(differentModpackId);
            if (!differentModpackId && MODPACK_ID.peek() != null && settings.getType() == Settings.TypeEnum.CLOUD) {
                USE_CLOUD_SETTINGS.set(true);
            }

            // Create composite list
            List<Setting<?>> loggedInSettings = new ArrayList<>(CLIENT_SETTINGS);
            loggedInSettings.addAll(API_SETTINGS);

            DISPLAY_SETTINGS.set(loggedInSettings);

            externalCapeSettings.apiUpdate(settings.getExternalCapes(), settings.getType());
        }
    }
}
