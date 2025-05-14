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
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.TextBox;
import gg.cloaks.javaclient.api.DefaultApi;
import gg.cloaks.javaclient.model.CreateOutfitDto;
import net.minecraft.client.Minecraft;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.Collections;

public class CreateNewOutfitScreen extends Screen {
    public CreateNewOutfitScreen() {
        super(ID);
        this.outfitName = new State<>("");
        this.outfitPublic = new State<>(true);
        this.disabled = new State<>(false);
    }

    private final State<String> outfitName;
    private final State<Boolean> outfitPublic;
    private final State<Boolean> disabled;

    @Override
    protected Component[] buildScreen() {
        String outfitName = this.outfitName.acquire(this);
        boolean outfitPublic = this.outfitPublic.acquire(this);
        boolean disabled = this.disabled.acquire(this);

        return new Component[] {
                new TextBox(Text.translatable("label.create_new_outfit.outfitName"), this.outfitName, true, 64)
                        .setDisabled(disabled),
                new Button(Text.translatable(
                        "label.create_new_outfit.public",
                        outfitPublic ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()),
                        () -> this.outfitPublic.set(!outfitPublic))
                        .setDisabled(disabled),
                new Div(
                        new Button(Text.translatable("label.cosmetica.create"), () -> {
                            // disable buttons/textbox
                            this.disabled.set(true);
                            // make request
                            CreateOutfitDto dto = new CreateOutfitDto()
                                    .name(outfitName)
                                    ._public(outfitPublic)
                                    .accessories(Collections.emptyList());
                            // if successful close screen. if fail just re-enable buttons
                            CosmeticaAPI.performAsync(dapi -> dapi.outfitsControllerCreate(dto))
                                    .thenAcceptAsync(outfit -> Screens.closeCurrentScreen(), Minecraft.getInstance()) // should receive websocket update
                                    .exceptionally(Cosmetica.mainThreadExcept(err -> {
                                        Logging.getInstance().error("Error creating new outfit", err);
                                        this.disabled.set(false);
                                    }));
                        }).setDisabled(disabled),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen).setDisabled(disabled)
                )
        };
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "create_new_outfit");
}
