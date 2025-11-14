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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.util.EquipUtil;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Label;
import cc.cosmetica.kupe.api.gui.Tooltip;
import gg.cloaks.javaclient.model.CreateOutfitAccessoryDto;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConfirmRemoveCosmeticScreen extends AbstractConfirmScreen {
    public ConfirmRemoveCosmeticScreen(Cosmetics parentOutfit, String itemId, String itemName, boolean mirrored) {
        super(Text.translatable("screens.cosmetica.confirmDeletion"));

        Objects.requireNonNull(parentOutfit.getOutfitId().orElse(null), "Cosmetics must represent an outfit to allow removals.");
        this.parentOutfit = parentOutfit;
        this.outfitId = parentOutfit.getOutfitId().get();
        this.removedItemId = itemId;
        this.removedItemName = itemName;
        this.removedItemMirrored = mirrored;
    }

    private final Cosmetics parentOutfit;
    private final String outfitId;
    private final String removedItemId;
    private final String removedItemName;
    private final boolean removedItemMirrored;

    @Override
    protected Label createConfirmLabel() {
        return new Label(Text.translatable("label.cosmetica.confirmRemove", removedItemName, parentOutfit.getOutfitName().get()));
    }

    @Override
    protected Tooltip getUpdatingTooltip() {
        return new Tooltip(Text.translatable("tooltip.cosmetica.updatingOutfit"));
    }

    @Override
    protected void onConfirm() {
        // build dto
        CreateOutfitDto dto = new CreateOutfitDto();
        boolean alreadyFoundItem = false;
        // Optionals
        if (parentOutfit.getCloak().isPresent()) {
            String cloak = parentOutfit.getCloak().get().getId();
            if (cloak.equals(removedItemId)) {
                dto.setCloak(null);
                alreadyFoundItem = true;
            }
        }
        if (parentOutfit.getElytra().isPresent()) {
            String elytra = parentOutfit.getElytra().get().getId();
            if (elytra.equals(removedItemId)) {
                dto.setElytra(null);
                alreadyFoundItem = true;
            }
        }

        // set accessories
        if (!alreadyFoundItem) {
            List<CreateOutfitAccessoryDto> accessories = new ArrayList<>();
            for (Accessory accessory : parentOutfit.getAccessories()) {
                if (!accessory.getId().equals(removedItemId) || accessory.isMirrored() != removedItemMirrored) {
                    CreateOutfitAccessoryDto caod = EquipUtil.dtoFromAccessory(accessory);
                    accessories.add(caod);
                }
            }
            dto.setAccessories(accessories);
        }

        this.setting.set(true);
        CosmeticaAPI.outfits().requestAsync(api -> api.modify(this.outfitId, dto))
                .thenAcceptAsync(o -> Screens.closeCurrentScreen(), Minecraft.getInstance())
                .exceptionally(Cosmetica.mainThreadExcept(ex -> {
                    Logging.getInstance().error("Error updating outfit {}", ex, this.outfitId);
                    this.setting.set(false);
                }));
    }
}
