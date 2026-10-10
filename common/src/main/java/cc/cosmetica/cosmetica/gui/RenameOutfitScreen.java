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
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Tooltip;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RenameOutfitScreen extends AbstractOutfitEditScreen {
    public RenameOutfitScreen(String id, String name, boolean isPublic) {
        super(
                TITLE,
                Text.translatable("button.cosmetica.renameOutfit"),
                new Tooltip(Text.translatable("tooltip.cosmetica.updatingOutfit")),
                Text.translatable("toast.cosmetica.outfitUpdateError"),
                name, isPublic);

        this.id = id;
        this.originalName = name;
        this.originalPublic = isPublic;
    }

    private final String id;
    private final String originalName;
    private final boolean originalPublic;

    @Override
    protected CompletableFuture<Void> submit(String outfitName, boolean outfitPublic) {
        // make request
        CreateOutfitDto dto = new CreateOutfitDto()
                .name(outfitName)
                ._public(outfitPublic)
                .accessories(Collections.emptyList());

        return CosmeticaAPI.outfits().requestAsync(dapi -> dapi.modify(this.id, dto))
                .thenAcceptAsync(outfit -> {
                    // websocket will update in most cases
                    if (!CosmeticaAPI.isWebsocketConnected()) {
                        // otherwise, refresh manually
                        List<OutfitWheelScreen.OutfitOption> outfits = Cosmetica.OWN_OUTFITS.peek();
                        boolean modified = false;
                        // find the outfit index and replace it
                        for (int i = 0; i < outfits.size(); i++) {
                            OutfitWheelScreen.OutfitOption option = outfits.get(i);

                            if (option.id.equals(this.id)) {
                                OutfitWheelScreen.OutfitOption updatedOption = new OutfitWheelScreen.OutfitOption(outfit);
                                outfits.set(i, updatedOption);
                                modified = true;
                                break;
                            }
                        }
                        if (modified) {
                            // send update
                            Cosmetica.OWN_OUTFITS.set(Cosmetica.OWN_OUTFITS.peek());
                        } else {
                            Logging.getInstance().warn("Couldn't find outfit {} ({}) to update", outfit.getId(), outfit.getName());
                        }
                    }
                }, Minecraft.getInstance());
    }

    @Override
    protected Optional<Text> checkNameRequirements(String name, boolean outfitPublic) {
        Optional<Text> t = super.checkNameRequirements(name, outfitPublic);
        if (t.isPresent()) {
            return t;
        }

        if (this.originalPublic == outfitPublic && this.originalName.equals(name)) {
            // disable creation with no tooltip
            return Optional.of(Text.empty());
        }

        return t;
    }

    public static final Text TITLE = Text.translatable("screens.cosmetica.outfit_settings");
}
