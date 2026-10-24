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
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

public class CreateOutfitScreen extends AbstractOutfitEditScreen {
    public CreateOutfitScreen() {
        super(
                ID.translationKey("screens"),
                Text.translatable("label.cosmetica.create"),
                new Tooltip(Text.translatable("tooltip.cosmetica.creatingOutfit")),
                Text.translatable("toast.cosmetica.outfitCreateError"),
                "", true);
    }

    @Override
    protected CompletableFuture<Void> submit(String outfitName, boolean outfitPublic) {
        // make request
        CreateOutfitDto dto = new CreateOutfitDto()
                .name(outfitName)
                ._public(outfitPublic)
                .accessories(Collections.emptyList());

        return CosmeticaAPI.outfits().requestAsync(dapi -> dapi.create(dto))
                .thenAcceptAsync(outfit -> {
                    // websocket will update in most cases
                    if (!CosmeticaAPI.isWebsocketConnected()) {
                        // otherwise, refresh manually
                        Cosmetica.OWN_OUTFITS.peek().add(new OutfitWheelScreen.OutfitOption(outfit));
                        Cosmetica.OWN_OUTFITS.set(Cosmetica.OWN_OUTFITS.peek());
                    }
                }, Minecraft.getInstance());
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "create_new_outfit");
}
