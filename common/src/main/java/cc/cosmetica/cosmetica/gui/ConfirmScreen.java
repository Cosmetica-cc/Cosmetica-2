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
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.CommonProperties;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import gg.cloaks.javaclient.model.CreateOutfitAccessoryDto;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class ConfirmScreen extends Screen {
    public ConfirmScreen(Cosmetics parentOutfit, String itemId, String itemName) {
        super(Text.translatable("screens.cosmetica.confirmDeletion"));
        Objects.requireNonNull(parentOutfit.getOutfitId().orElse(null), "Cosmetics must represent an outfit to allow removals.");
        this.parentOutfit = parentOutfit;
        this.outfitId = parentOutfit.getOutfitId().get();
        this.removedItemId = itemId;
        this.removedItemName = itemName;
    }

    private final Cosmetics parentOutfit;
    private final String outfitId;
    private final String removedItemId;
    private final String removedItemName;
    private State<Boolean> setting = new State<>(false);

    @Override
    protected Component[] buildScreen() {
        boolean setting = this.setting.acquire(this);

        return new Component[] {
                new Label(Text.translatable("label.cosmetica.confirmRemove", removedItemName, parentOutfit.getOutfitName().get())),
                new Div(
                    new Button(Text.GUI_PROCEED, () -> {
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
                                if (!accessory.getId().equals(removedItemId)) {
                                    CreateOutfitAccessoryDto caod = new CreateOutfitAccessoryDto();
                                    caod.id(accessory.getId());
                                    caod.mirrored(accessory.isMirrored());
                                    caod.setOffset(Arrays.asList(
                                            BigDecimal.valueOf(accessory.getOffset().x),
                                            BigDecimal.valueOf(accessory.getOffset().y),
                                            BigDecimal.valueOf(accessory.getOffset().z)
                                    ));
                                    accessories.add(caod);
                                }
                            }
                            dto.setAccessories(accessories);
                        }

                        this.setting.set(true);
                        CosmeticaAPI.performAsync(api -> api.outfitsControllerModify(this.outfitId, dto))
                                .thenAcceptAsync(o -> Screens.closeCurrentScreen(), Minecraft.getInstance())
                                .exceptionally(Cosmetica.mainThreadExcept(ex -> {
                                    Logging.getInstance().error("Error updating outfit {}", ex, this.outfitId);
                                    this.setting.set(false);
                                }));
                    }).setDisabled(setting).withStyle(Style.create()
                            .set(CommonProperties.TOOLTIP,
                                    !setting ? Optional.empty() : Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.updatingOutfit"))))),
                    new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                ).tag("horizontal")
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .self(Style.create()
                        .set(Label.ALIGN_TEXT, Align.CENTRE))
                .tag("horizontal", Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(MARGINS, fixed(new Margins(12, 0, 0, 0))))
                .component(Button.class, Style.create()
                        .set(WIDTH, fixedSize(150)));
    }
}
