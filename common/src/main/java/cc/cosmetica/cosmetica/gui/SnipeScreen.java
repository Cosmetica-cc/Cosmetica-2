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

import cc.cosmetica.core.api.*;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.StateHolder;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.CosmeticsList;
import cc.cosmetica.cosmetica.gui.widget.RotatableGUIPlayer;
import cc.cosmetica.cosmetica.gui.widget.SlideToggle;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import gg.cloaks.javaclient.api.OutfitsApi;
import gg.cloaks.javaclient.model.OutfitAccessory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Shows the cosmetics of an inspected cosmetics holder.
 * Unregistered. Pass ID as second parameter when setting kupe screen.
 */
public class SnipeScreen extends Screen implements AnimatedTextureScreen {
    public static final ResourceKey ID = new ResourceKey("cosmetica", "snipe");

    public SnipeScreen(LivingEntity entity) {
        super(entity instanceof Player ?
                Text.literal(entity.getDisplayName().getString()) :
                Text.literal(Cosmetics.getCosmetics(entity).flatMap(Cosmetics::getOutfitName).orElse("Outfit"))
        );

        // TODO armour stands dont have an autoupdated cosmetic state currently. we subscribe to automatic outfit updates, so this should be done?
        this.cosmetics = ((StateHolder)entity).cosmetica$getCosmeticState();
        this.playerUUID = entity instanceof Player ? entity.getUUID() : null;
        // TODO use a central state for authenticated in case re-auth. compose states with isSetting
        this.isSettingOrUnauthenticated = new State<>(!CosmeticaAPI.isAuthenticated());
        this.showNametag = entity instanceof Player;
    }

    private final State<Cosmetics> cosmetics;
    private final State<Boolean> isSettingOrUnauthenticated;
    private final State<Boolean> showingElytra = new State<>(false);
    private final @Nullable UUID playerUUID;
    private final boolean showNametag;

    @Override
    protected Component[] buildScreen() {
        @Nullable Cosmetics outfit = this.cosmetics.acquire(this);
        UUID player = playerUUID == null ? Minecraft.getInstance().getUser().getProfileId() : playerUUID;

        // we can do something similar to home screen.
        List<CosmeticEntry> entryList = new ArrayList<>();
        RotatableGUIPlayer guiPlayer = new RotatableGUIPlayer(player, this.showingElytra);

        if (outfit != null) {
            // Cosmetics list
            CosmeticEntry.populateEntryList(entryList, outfit, CosmeticEntry.Type.LISTED);

            // GUI player configuration
            if (playerUUID == null) {
                // specify outfit cosmetics to show
                guiPlayer.configureOverride(AccessoriesAttachment.INSTANCE, outfit.getAccessories());
            }
            guiPlayer.configureOverride(GUIPlayer.CAPE, outfit.getCloak().map(ImageCosmetic::getImage).map(ci -> ci.location).map(GUIPlayer.CapeProperties::new).orElse(new GUIPlayer.CapeProperties((ResourceKey) null)));
            guiPlayer.configureOverride(GUIPlayer.ELYTRA, outfit.getElytra().map(ImageCosmetic::getImage).map(ci -> ci.location).map(location -> new GUIPlayer.ElytraProperties(new ResourceKey(location), false, true)).orElse(GUIPlayer.ElytraProperties.DEFAULT));

            if (this.showNametag) {
                guiPlayer.showNametag(true);

                Optional<NametagConfig> lore = outfit.getLore();
                NametagConfig icon = outfit.getNametag();

                if (lore.isPresent()) {
                    guiPlayer.addNametag(Text.literal(lore.get().getPrefix()), 0.75f);
                    if (lore.get().getIcon().getImage().location != CachedImage.NO_TEXTURE.location) {
                        guiPlayer.loreIcon(lore.get().getIcon().getImage());
                    }
                }

                if (icon.getIcon().getImage().location != CachedImage.NO_TEXTURE.location) {
                    guiPlayer.icon(icon.getIcon().getImage(), icon.isTransparentIcon());
                }
            }
        }

        return new Component[] {
                new Div(
                        new Div(
                                new Div().withStyle(Style.create().set(HEIGHT, fixedSize(10))),
                                    guiPlayer.withStyle(Style.create().set(WIDTH, screen(12, 0))),
                                new SlideToggle(
                                        this.showingElytra,
                                        Text.translatable("button.cosmetica.toggleCloak"),
                                        Text.translatable("button.cosmetica.toggleElytra"))
                                        .withStyle(Style.create().set(MARGINS, fixed(new Margins(5,0,0,0))))
                        ).tag("main-section")
                                .withStyle(Style.create()
                                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)),
                        new CosmeticsList(entryList, CosmeticsList.ListType.LIST_ONLY)
                                .tag("main-section")
                ).tag("main-content"),
                new StealTheirLookButton(
                        outfit, this.isSettingOrUnauthenticated,
                        Text.translatable("button.cosmetica.stealHisLook"),
                        () -> {
                            String outfitId = outfit == null ? "" : outfit.getOutfitId().orElse("");
                            if (outfitId.isEmpty()) {
                                this.isSettingOrUnauthenticated.set(true);
                                CosmeticaAPI.outfits().requestAsync(OutfitsApi::unequip)
                                        .thenAccept(__ -> {
                                            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Cleared Cosmetics by Steal-their-look.");
                                            Minecraft.getInstance().tell(Screens::closeCurrentScreen);
                                        })
                                        .exceptionally(err -> {
                                            Logging.getInstance().error("Failed to unequip cosmetics!", err);
                                            Minecraft.getInstance().tell(()->this.isSettingOrUnauthenticated.set(false));
                                            return null;
                                        });
                                return;
                            }

                            // test stealtheirlookscreen: never take the quick option
                            // if not empty : either own cosmetics (e.g. armour stand) or not own cosmetics (need to select a slot)
                            // do match by value and swap the id for own for better user experience
                            String ownedOutfit = findIdenticalOwnedOutfit(outfit);
                            if (ownedOutfit != null) {
                                this.isSettingOrUnauthenticated.set(true);
                                // can set cosmetics immediately
                                CosmeticaAPI.outfits().requestAsync(api->api.equip(ownedOutfit))
                                        .thenAccept(__ -> {
                                            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Set Cosmetics by Steal-their-look.");
                                            Minecraft.getInstance().tell(Screens::closeCurrentScreen);
                                        })
                                        .exceptionally(err -> {
                                            Logging.getInstance().error("Failed to set cosmetics!", err);
                                            Minecraft.getInstance().tell(()->this.isSettingOrUnauthenticated.set(false));
                                            return null;
                                        });
                            } else {
                                // don't disable this button when entering replace outfit screen!
                                Screens.setScreen(new ReplaceOutfitSlotScreen(outfit), ReplaceOutfitSlotScreen.STEAL_THEIR_LOOK);
                            }
                        }),
                new Button(Text.GUI_DONE, Screens::closeCurrentScreen)
        };
    }

    private @Nullable String findIdenticalOwnedOutfit(Cosmetics toWear) {
        // exact id check
        if (Cosmetica.OWN_OUTFITS.peek().stream().anyMatch(option -> option.id.equals(toWear.getOutfitId().orElse("")))) {
            return toWear.getOutfitId().orElse("");
        }

        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.peek();
        for (OutfitWheelScreen.OutfitOption owned : options) {
            if (compare(owned, toWear)) {
                return owned.id;
            }
        }

        return null; // no match
    }

    private boolean compare(OutfitWheelScreen.OutfitOption owned, Cosmetics toWear) {
        // check cape and elytra are equivalent
        String toWearCloak = toWear.getCloak().map(ImageCosmetic::getId).orElse("");
        String toWearElytra = toWear.getElytra().map(ImageCosmetic::getId).orElse("");

        if (!owned.capeId.equals(toWearCloak) || !owned.elytraId.equals(toWearElytra)) {
            return false;
        }

        // check accessories are equivalent, regardless of order.
        // eliminate a to-wear accessory one at a time through outfit accessories
        List<Accessory> accessories = new LinkedList<>(toWear.getAccessories());//good remove operation but iterable

        // there is such a small number of accessories and this is run once. O(n * m) is fine.
        findOwnedAccessories:
        for (OutfitAccessory accessory : owned.accessories) {
            // if an accessory is not present in accessories, return false. Else delete it: it is found.
            // we can't use id as a primary search then check offset because you can equip the same outfit multiple times
            Vec3 offset = attachmentTransform(
                    accessory.getAccessory().getAttachment(),
                    accessory.getOffset().get(0).doubleValue(),
                    accessory.getOffset().get(1).doubleValue(),
                    accessory.getOffset().get(2).doubleValue()
            );

            Iterator<Accessory> accessoriesIterator = accessories.iterator();
            while (accessoriesIterator.hasNext()) {
                Accessory accessory1 = accessoriesIterator.next();

                if (accessory1.getId().equals(accessory.getAccessory().getId())) {
                    Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Matching ID found. Checking offsets..");
                    // compare offsets
                    Vec3 offset1 = accessory1.getOffset();
                    if (offset.equals(offset1)) {
                        accessoriesIterator.remove();
                        continue findOwnedAccessories;// found
                    }
                }
            }

            return false;// no match found. (EARLY CONTINUE for found)
        }

        return accessories.isEmpty(); // all accessories were identical (no non-matched accessories remain)
    }

    // Accessory#attachmentTransform
    private static Vec3 attachmentTransform(gg.cloaks.javaclient.model.Accessory.AttachmentEnum attachment, double x, double y, double z) {
        double dy;
        double dx;

        switch (attachment) {
            case HEAD:
                dy = 8.0;
                dx = 8.0;
                break;
            case RIGHT_ARM:
                dy = 0.0;
                dx = 8.0;
                break;
            case LEFT_ARM:
                dy = 0.0;
                dx = 7.0;
                break;
            default:
                dy = -2.0;
                dx = 8.0;
                break;
        }

        return new Vec3(
                (x + dx) / 16.0,
                (y + dy) / 16.0,
                (z + 8.0) / 16.0
        );
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("main-content", Style.create()
                        .set(FLEX, 1)
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                        .set(Div.ALIGN_ITEMS, Align.CENTRE))
                .tag("main-section", Style.create()
                        .set(WIDTH, screen(50, 0))
                        .set(HEIGHT, (vw,vh,pw,ph) -> OptionalInt.of(ph*50/100 + 100)))
                .tag("body", Style.create()
                        .set(PADDING, fixed(new Margins(0, 0, 10, 0))));
    }

    /**
     * Reloads when your own cosmetics change (prevent having to reload whole screen). This might be overkill optimisation.
     */
    private static class StealTheirLookButton extends Button {
        public StealTheirLookButton(@Nullable Cosmetics outfit, State<Boolean> disabled, Text text, Runnable onClicked) {
            super(text, onClicked);
            this.outfit = outfit;
            this.disabledState = disabled;
        }
        private final @Nullable Cosmetics outfit;
        private final State<Boolean> disabledState;

        @Override
        public List<Component> build() {
            @Nullable Cosmetics cosmetics1 = Cosmetica.OWN_COSMETICS.acquire(this);
            boolean overrideDisabled = this.disabledState.acquire(this);

            boolean disabled = outfit == null ? (overrideDisabled || cosmetics1 == null) : (overrideDisabled || (cosmetics1 != null && cosmetics1.getOutfitId().equals(outfit.getOutfitId())));
            this.setDisabled(disabled);
            this.withStyle(Style.create().set(TOOLTIP, disabled ? Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.outfitAlreadySelected"))) : Optional.empty()));
            return super.build();
        }
    }
}
