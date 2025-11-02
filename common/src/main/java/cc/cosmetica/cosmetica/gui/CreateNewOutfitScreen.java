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

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

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
        boolean outfitPublic = this.outfitPublic.acquire(this);
        boolean disabled = this.disabled.acquire(this);

        return new Component[] {
                new TextBox(Text.translatable("label.create_new_outfit.outfitName"), this.outfitName, true, 64)
                        .setDisabled(disabled),
                new Button(Text.translatable(
                        "label.create_new_outfit.searchable",
                        outfitPublic ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()),
                        () -> this.outfitPublic.set(!outfitPublic))
                        .setDisabled(disabled),
                createSubmissionGroup(outfitPublic, disabled)
        };
    }

    public static String strip(String input) {
        if (input == null) {
            return null;
        }
        // Handle all unicode spaces
        return input.replaceAll("^[\\p{Space}]+|[\\p{Space}]+$", "");
    }

    private Component createSubmissionGroup(boolean outfitPublic, boolean disabled) {
        final int nameMinChars = 3;

        return new Div() {
            @Override
            public List<Component> build() {
                // todo check other character constraints
                // combined with trim in case \pSpace doesn't account for control characters - though I doubt they can type those
                String outfitName = strip(CreateNewOutfitScreen.this.outfitName.acquire(this).trim());
                boolean legalName = outfitName.length() >= nameMinChars;

                return Arrays.asList(
                        new Button(Text.translatable("label.cosmetica.create"), () -> {
                            // disable buttons/textbox
                            CreateNewOutfitScreen.this.disabled.set(true);
                            // make request
                            CreateOutfitDto dto = new CreateOutfitDto()
                                    .name(outfitName)
                                    ._public(outfitPublic)
                                    .accessories(Collections.emptyList());
                            // if successful close screen. if fail just re-enable buttons
                            CosmeticaAPI.outfits().requestAsync(dapi -> dapi.create(dto))
                                    .thenAcceptAsync(outfit -> Screens.closeCurrentScreen(), Minecraft.getInstance()) // should receive websocket update
                                    .exceptionally(Cosmetica.mainThreadExcept(err -> {
                                        Logging.getInstance().error("Error creating new outfit", err);
                                        CreateNewOutfitScreen.this.disabled.set(false);
                                    }));
                        }).setDisabled(disabled || !legalName),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen).setDisabled(disabled)
                                .withStyle(Style.create()
                                        .set(TOOLTIP, !legalName && !outfitName.isEmpty() ? Optional.of(new Tooltip(Text.translatable("cosmetica.tooltip.notLongEnough"))) : Optional.empty())
                                )
                );
            }
        };
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "create_new_outfit");
}
