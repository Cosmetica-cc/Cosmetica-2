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
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.builtin.OutfitCosmeticsHolder;
import cc.cosmetica.core.builtin.manager.ApiCosmeticManager;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.cosmetica.gui.widget.IconButton;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.gui.widget.OutfitPlayer;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import gg.cloaks.javaclient.api.UsersApi;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Base for home-screen-like screens. Screens that are too different (snipe) shouldn't extend this.
 */
public abstract class AbstractHomeScreen extends BaseCosmeticaScreen implements AnimatedTextureScreen {
    protected AbstractHomeScreen(ResourceKey id) {
        super(id);
    }

    @Override
    protected Component[] buildScreen() {
        UUID self = Minecraft.getInstance().getUser().getGameProfile().getId();

        Cosmetics cosmetics = Cosmetica.OWN_COSMETICS.acquire(this);
        boolean authenticated = CosmeticaAPI.isAuthenticated();

        // load cache cosmetics
        if (cosmetics == null && !authenticated) {
            cosmetics = Cosmetica.getCacheCosmeticManager().getCosmetics(null);
        }
        Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Loaded cosmetics for screen: " + cosmetics + " with " + (cosmetics == null ? 0 : cosmetics.getAccessories().size()) + " accessories");

        return new Component[] {
                new LayeredSpace(true,
                    new Div(
                            this.createOutfitPlayer(self, authenticated, cosmetics).tag("main-section"),
                            this.createRightMenu(cosmetics, authenticated).tag("main-section")
                    ).tag("main-content"),
                    new Div(
                            new IconButton(
                                    new ResourceKey("cosmetica", "textures/gear.png"),
                                    () -> Screens.setScreen(new CosmeticaSettingsScreen(CosmeticaSettingsScreen.SETTINGS_SCREEN, CosmeticaSettings.DISPLAY_SETTINGS), CosmeticaSettingsScreen.SETTINGS_SCREEN)),
                            new IconButton(
                                    new ResourceKey("cosmetica", "textures/cape.png"),
                                    () -> Screens.setScreen(new ExternalCapesScreen(CosmeticaSettings.externalCapeSettings), ExternalCapesScreen.ID))
                                    .setDisabled(!authenticated)
                                    .withStyle(Cosmetica.authTooltipStyle(authenticated)),
                            new IconButton(
                                    new ResourceKey("minecraft", "textures/item/name_tag.png"),
                                    () -> Screens.setScreen(StyleNametagScreen.ID))
                                    .setDisabled(!authenticated)
                                    .withStyle(Cosmetica.authTooltipStyle(authenticated)),
                            new Div().withStyle(Style.create().set(FLEX, 1)),
                            new IconButton(
                                    new ResourceKey("cosmetica", "textures/reload.png"),
                                    () -> {
                                        Logging.getInstance().info("Reloading all cosmetics");
                                        reloadDisabled.set(true);
                                        // enable after 15 seconds
                                        BUTTON_SCHEDULER.schedule(() -> {
                                            Minecraft.getInstance().execute(() -> reloadDisabled.set(false));
                                        }, 15, TimeUnit.SECONDS);

                                        // Own cosmetics
                                        if (CosmeticaAPI.isAuthenticated()) {
                                            CosmeticaAPI.users().requestAsync(UsersApi::getSelf)
                                                    .thenAcceptAsync(user -> SelfCosmeticManager.update(new PlayerResponse().isUser(true).user(user)),
                                                            Minecraft.getInstance())
                                                    .exceptionally(ex -> {
                                                        Logging.getInstance().error("Failed to reload own cosmetics", ex);
                                                        return null;
                                                    });
                                        } else {
                                            SelfCosmeticManager.clear();
                                        }

                                        int players = 0;
                                        int outfits = 0;

                                        // Player cosmetics
                                        ClientLevel level = Minecraft.getInstance().level;
                                        if (level != null) {
                                            for (Entity entity : level.entitiesForRendering()) {
                                                if (entity instanceof RemotePlayer) {
                                                    // only reload if cosmetics have already been loaded
                                                    if (Cosmetics.getCosmetics((LivingEntity) entity).isPresent()) {
                                                        GameProfile profile = ((AbstractClientPlayer) entity).getGameProfile();
                                                        ApiCosmeticManager.lookUpGameProfile(profile);
                                                        players++;
                                                    }
                                                } else if (entity instanceof OutfitCosmeticsHolder) {
                                                    ((OutfitCosmeticsHolder) entity).cosmeticacore$reloadCosmetics();
                                                    outfits++;
                                                }
                                            }
                                        }

                                        Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Started reload for {} remote players and {} outfit holders", players, outfits);
                                    }) {
                                @Override
                                public List<Component> build() {
                                    // only rebuild this component for disabled/not disabled
                                    boolean disabled = reloadDisabled.acquire(this);
                                    setDisabled(disabled);
                                    return ImmutableList.of();
                                }
                            }.withStyle(Style.create().set(TOOLTIP, Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.reloadCosmetics")))))
                    ).withStyle(Style.create()
                            .set(Div.ALIGN_ITEMS, Align.START)
                            .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                ).tag("main-content-wrapper"),
                createMenuEndSelection()
        };
    }

    protected Component createMenuEndSelection() {
        return new MenuEndSelection();
    }

    protected Component createOutfitPlayer(UUID self, boolean authenticated, Cosmetics cosmetics) {
        OutfitPlayer player = new OutfitPlayer(self,
                authenticated,
                Optional.ofNullable(cosmetics).flatMap(Cosmetics::getOutfitName).orElse("§7No Outfit"),
                Optional.ofNullable(cosmetics).flatMap(Cosmetics::getLore).orElse(NametagConfig.EMPTY),
                Optional.ofNullable(cosmetics).map(Cosmetics::getNametag).orElse(NametagConfig.EMPTY));
        // add cache cosmetics to outfit player
        if (!authenticated && cosmetics != null) {
            player.configureOverrides(p -> p
                    .configureOverride(AccessoriesAttachment.INSTANCE, cosmetics.getAccessories())
                    .configureOverride(GUIPlayer.CAPE, cosmetics.getCloak().map(ImageCosmetic::getImage).map(c -> c.location).map(GUIPlayer.CapeProperties::new).orElse(new GUIPlayer.CapeProperties((ResourceKey) null)))
                    .configureOverride(GUIPlayer.ELYTRA, cosmetics.getElytra().map(ImageCosmetic::getImage).map(c -> new GUIPlayer.ElytraProperties(c.location, false, true)).orElse(GUIPlayer.ElytraProperties.DEFAULT)));
        }
        return player;
    }

    /**
     * Create the right hand menu.
     * @apiNote main-section tag is automatically applied.
     */
    @NotNull
    abstract protected Component createRightMenu(Cosmetics cosmetics, boolean authenticated);

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("main-content-wrapper", Style.create()
                        .set(FLEX, 1))
                .tag("main-content", Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                        .set(Div.ALIGN_ITEMS, Align.CENTRE))
                .tag("main-section", Style.create()
                        .set(WIDTH, screen(50, 0))
                        .set(HEIGHT, percent(0, 100)));
    }

    private static State<Boolean> reloadDisabled = new State<>(false);
    private static final ScheduledExecutorService BUTTON_SCHEDULER = Executors.newScheduledThreadPool(1);
}
