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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.settings.Setting;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.UpdateExternalCapeSettingDto;
import gg.cloaks.javaclient.model.UpdateSettingsDto;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * An unregistered screen for showing a list of settings.
 */
public class CosmeticaSettingsScreen extends Screen {
    public CosmeticaSettingsScreen(ResourceKey titleKey, State<List<Setting<?>>> settings) {
        super(titleKey);
        this.settings = settings;
    }

    private final State<List<Setting<?>>> settings;

    @Override
    protected Component[] buildScreen() {
        Component[] settings = this.settings.acquire(this).stream()
                .map(SettingBlock::new)
                .map(c -> c.tag("setting-block"))
                .toArray(Component[]::new);

        return new Component[] {
                new Div(
                        new Div(settings).withStyle(
                                Style.create()
                                        .set(MARGINS, fixed(new Margins(5, 0, 2, 0)))
                                        .set(Div.SCROLLBAR_POSITION, AbstractScrollContainer.ScrollbarPosition.OUTSIDE)
                        ),
                        new MenuEndSelection()
                                .withStyle(Style.create()
                                        .set(FLEX_SHRINK, 0)
                                        .set(MARGINS, fixed(Margins.NONE))
                                        .set(MIN_WIDTH, fixedSize(220)))
                ).withStyle(Style.create()
                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                        .set(MARGINS, fixed(new Margins(5, 0, 0, 0)))
                        .set(PADDING, fixed(new Margins(20, 0)))
                        .set(MIN_HEIGHT, screen(0, 100)))
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("setting-block", Style.create()
                        .set(MIN_WIDTH, fixedSize(200))
                        .set(WIDTH, screen(50, 0)));
    }

    @Override
    public void unmount() {
        // update settings
        boolean modifiedApi = CosmeticaSettings.API_SETTINGS.stream().anyMatch(Setting::isModified);

        if (modifiedApi) {
            UpdateSettingsDto dto = newDto();
            if (CosmeticaSettings.SHOW_ACCESSORIES.isModified()) {
                dto.setShowAccessories(CosmeticaSettings.SHOW_ACCESSORIES.getUserValue());
                CosmeticaSettings.SHOW_ACCESSORIES.clean();
            }
            if (CosmeticaSettings.SHOW_LORE.isModified()) {
                dto.setShowLore(CosmeticaSettings.SHOW_LORE.getUserValue());
                CosmeticaSettings.SHOW_LORE.clean();
            }
            if (CosmeticaSettings.SHOW_ICONS.isModified()) {
                dto.setShowIcons(CosmeticaSettings.SHOW_ICONS.getUserValue());
                CosmeticaSettings.SHOW_ICONS.clean();
            }
            if (CosmeticaSettings.SHOW_OFFLINE_ICONS.isModified()) {
                dto.setShowOfflineIcons(CosmeticaSettings.SHOW_OFFLINE_ICONS.getUserValue());
                CosmeticaSettings.SHOW_OFFLINE_ICONS.clean();
            }
            if (CosmeticaSettings.SHOW_SPECIAL_ICONS.isModified()) {
                dto.setShowSpecialIcons(CosmeticaSettings.SHOW_SPECIAL_ICONS.getUserValue());
                CosmeticaSettings.SHOW_SPECIAL_ICONS.clean();
            }
            if (CosmeticaSettings.SHOW_ONLINE_ACTIVITY.isModified()) {
                dto.setShowOnlineActivity(CosmeticaSettings.SHOW_ONLINE_ACTIVITY.getUserValue());
                CosmeticaSettings.SHOW_ONLINE_ACTIVITY.clean();
            }
            //TODO show a popup notice if updating settings fails or retry (have some model of latest in case multiple queue)?
            CosmeticaAPI.settings().requestAsync(api -> api.setCloud(dto))
                    .thenAcceptAsync(user -> {
                        CosmeticaSettings.updateSettings(user.getActiveSettings());
                        Logging.getInstance().debug(CosmeticaLogCategory.SETTINGS, "Updated settings to /cloud");
                    }, Minecraft.getInstance())
                    .exceptionally(e -> {
                        Logging.getInstance().error("Failed to update settings", e);
                        return null;
                    });
        }
    }

    public static final ResourceKey SETTINGS_SCREEN = new ResourceKey("cosmetica", "settings");

    public static UpdateSettingsDto newDto() {
        UpdateSettingsDto dto = new UpdateSettingsDto();
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
        return dto;
    }

    private static class SettingBlock<T> extends Div {
        private SettingBlock(Setting<T> setting) {
            this.setting = setting;
        }

        private final Setting<T> setting;

        @Override
        public List<Component> build() {
            T value = this.setting.acquire(this);

            // create text
            Text text = this.setting.name;
            if (this.setting.isModified()) {
                text = Text.literal(/*"§l" +*/ text.getDisplayString() + "*");
            }

            // return components
            Component main = new Div(
                    new Label(text),
                    this.setting.createController().tag("controller")
            ).tag("setting-display");

            return this.setting.hasDescription() ? ImmutableList.of(main, new Label(this.setting.createDescription(value)).tag("setting-description")) : ImmutableList.of(main);
        }

        @Override
        public Stylesheet getStylesheet() {
            Style.MutableStyle style = Style.create()
                    .set(FLEX_SHRINK, 0)
                    .set(Div.ALIGN_ITEMS, Align.STRETCH_CENTRE);
            if (this.setting.hasDescription()) {
                style.set(HEIGHT, fixedSize(50));
            }

            return new Stylesheet()
                    .tag("controller", Style.create().set(WIDTH, fixed(OptionalInt.of(100))))
                    .tag("setting-display", Style.create()
                            .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                            .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN))
                    .tag("setting-description", Style.create()
                            .set(MARGINS, fixed(new Margins(1, 0, 0, 0))))
                    .self(style);
        }
    }
}
