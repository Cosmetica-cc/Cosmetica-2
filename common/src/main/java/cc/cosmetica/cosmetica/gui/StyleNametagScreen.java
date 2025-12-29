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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.IconSelector;
import cc.cosmetica.cosmetica.gui.widget.LoreSelector;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.gui.widget.RotatableGUIPlayer;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.cosmetica.util.Lore;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Align;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.Justify;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.api.IconsApi;
import gg.cloaks.javaclient.api.LoreApi;
import gg.cloaks.javaclient.model.Icon;
import gg.cloaks.javaclient.model.LoreOptions;
import gg.cloaks.javaclient.model.PlayerResponse;
import gg.cloaks.javaclient.model.UpdateLoreDto;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class StyleNametagScreen extends Screen implements AnimatedTextureScreen {
    public StyleNametagScreen() {
        super(ID);

        // refresh available lores
        CosmeticaAPI.lore().requestAsync(LoreApi::getLoreOptions)
                .thenAcceptAsync(loreOptions -> availableLores.set(loreOptions), Minecraft.getInstance());
        // refresh available icons
        CosmeticaAPI.icons().requestAsync(IconsApi::get)
                .thenAcceptAsync(icons -> {
                    List<IconSelector.IconOption> newAvailableIcons = new ArrayList<>();
                    for (Icon icon : icons) {
                        newAvailableIcons.add(new IconSelector.IconOption(ImageCosmetic.fromIcon(icon), icon.isUnlocked()));
                    }
                    Logging.getInstance().debug(CosmeticaLogCategory.GUI, "loaded {} available icons", newAvailableIcons.size());
                    availableIcons.set(newAvailableIcons);
                }, Minecraft.getInstance());
    }

    private final AtomicBoolean iconDirty = new AtomicBoolean(false);
    private final AtomicBoolean loreDirty = new AtomicBoolean(false);

    @Override
    protected Component[] buildScreen() {
        UUID self = Minecraft.getInstance().getUser().getGameProfile().getId();

        // subscribe to the *cosmetics change*
        //Cosmetics cosmetics = Cosmetica.OWN_COSMETICS.acquire(this);

        return new Component[] {
                new Div(
                        new LoreSelector(this.loreDirty, availableLores)
                                .tag("flex-1"),
                        new RotatableGUIPlayer(self, null)
                        {
                            private int nametag = -1;
                            @Override
                            public List<Component> build() {
                                Lore lore = Cosmetica.SELECTED_LORE.acquire(this);
                                CachedImage icon = Cosmetica.SELECTED_ICON.extract(this, ic -> !ic.getImage().isLoaded() ? null : ic.getImage());
                                this.icon(icon);
                                this.loreIcon(!lore.icon.isLoaded() ? null : lore.icon);

                                if (nametag == -1) {
                                    nametag = this.createNametag(Text.literal(lore.formatted()), 0.75f);
                                } else {
                                    this.updateNametag(nametag, Text.literal(lore.formatted()), 0.75f);
                                }
                                return super.build();
                            }
                        }.showNametag(true).tag("preview-player"),
                        new IconSelector(this.iconDirty, availableIcons)
                                .tag("flex-1")
                ).tag("horizontal", "flex-1", "main-content"),
                new MenuEndSelection()
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("preview-player", Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(50)))
                        .set(Z_INDEX, 10)
                        .set(ALIGN_SELF, Optional.of(Align.CENTRE)))
                .tag("horizontal", Style.create()
                        .set(WIDTH, percent(100, 0))
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                .tag("main-content", Style.create()
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_AROUND)
                        .set(Div.ALIGN_ITEMS, Align.STRETCH_CENTRE))
                .tag("flex-1", Style.create()
                        .set(FLEX, 1));
    }

    @Override
    public void unmount() {
        // lore is set
        if (this.loreDirty.compareAndSet(true, false)) {
            Lore selectedLore = Cosmetica.SELECTED_LORE.peek();

            CosmeticaAPI.lore().requestAsync(this.updateLoreFunction(selectedLore))
                    .exceptionally(e -> {
                        // Prevent race condition by resetting on the minecraft thread
                        Minecraft.getInstance().tell(() -> {
                            @Nullable Lore old = Cosmetica.SELECTED_LORE.peek().old;

                            if (old != null) {
                                Cosmetica.SELECTED_LORE.set(old);
                            }
                        });

                        Logging.getInstance().error("Could not set lore", e); return null;
                    });
        }

        // icon is set
        if (this.iconDirty.compareAndSet(true, false)) {
            ImageCosmetic selectedIcon = Cosmetica.SELECTED_ICON.peek();
            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Updating Icon to {}", selectedIcon.getName());
            CosmeticaAPI.icons().requestAsync(api -> api.equip(selectedIcon.getId()))
                    .thenAcceptAsync(user -> {
                        SelfCosmeticManager.update(new PlayerResponse().user(user).isUser(true));
                        Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Equipped icon " + selectedIcon.getId());
                    }, Minecraft.getInstance())
                    .exceptionally(e -> {
                        // TODO is there a race condition
                        assert Cosmetica.OWN_COSMETICS.peek() != null; // trust me bro
                        Logging.getInstance().error("Could not set icon", e);
                        Minecraft.getInstance().execute(() ->
                            Cosmetica.SELECTED_ICON.set(Cosmetica.OWN_COSMETICS.peek().getNametag().getIcon())
                        );
                        return null;
                    });
        }
    }

    private Function<LoreApi, ?> updateLoreFunction(Lore newLore) {
        if (newLore.isNoLore()) {
            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Removing lore");
            return LoreApi::removeLore;
        } else {
            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Updating Lore to {}", newLore.value);
            UpdateLoreDto update = new UpdateLoreDto();
            update.content(newLore.value);
            update.color(newLore.colour);
            update.type(newLore.getType());
            return api -> api.updateLore(update);
        }
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "name_tag");


    private static final LoreOptions UNLOADED = new LoreOptions();
    // preserve available lores/icons list. don't load it every time the page is opened (but do refresh it)
    private static State<LoreOptions> availableLores = new State<>(UNLOADED);
    private static State<List<IconSelector.IconOption>> availableIcons = new State<>(ImmutableList.of());
}
