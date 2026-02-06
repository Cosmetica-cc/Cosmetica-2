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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Label;
import cc.cosmetica.kupe.api.gui.Tooltip;
import gg.cloaks.javaclient.ApiException;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletionException;

public final class ConfirmRemoveOutfitScreen extends AbstractConfirmScreen {
    public ConfirmRemoveOutfitScreen(String outfitId, String outfitName) {
        super(Text.translatable("screens.cosmetica.confirmDeletion"));
        this.outfitId = outfitId;
        this.outfitName = outfitName;
    }

    private final String outfitId;
    private final String outfitName;

    @Override
    protected Label createConfirmLabel() {
        return new Label(Text.translatable("label.cosmetica.confirmDelete", outfitName));
    }

    @Override
    protected Tooltip getUpdatingTooltip() {
        return new Tooltip(Text.translatable("tooltip.cosmetica.updatingOutfit"));
    }

    @Override
    protected void onConfirm() {
        this.setting.set(true);
        CosmeticaAPI.outfits().requestAsync(api -> {
                    api.delete(outfitId);
                    return (Void)null;
                })
                .thenAcceptAsync(none -> {
                    // Always update!
                    Cosmetica.OWN_OUTFITS.peek().removeIf(o -> outfitId.equals(o.id));
                    Cosmetica.OWN_OUTFITS.set(Cosmetica.OWN_OUTFITS.peek());
                    // Close screen
                    Screens.closeCurrentScreen();
                }, Minecraft.getInstance())
                .exceptionally(err -> {
                    if (err instanceof CompletionException) {
                        err = err.getCause();
                    }

                    if (err instanceof ApiException) {
                        int code = ((ApiException) err).getCode();
                        // outfit doesn't exist
                        if (code == 404) {
                            Cosmetica.showToast(
                                    Text.translatable("toast.cosmetica.outfit404"),
                                    null
                            );
                            Minecraft.getInstance().execute(Screens::closeCurrentScreen);
                        }
                        // no auth
                        // else if (code == 401)
                        // default: error
                        else {
                            Logging.getInstance().error("Error deleting outfit", code);
                            Minecraft.getInstance().execute(() -> setting.set(false));

                            Cosmetica.showToast(
                                    Text.translatable("toast.cosmetica.outfitDeleteError"),
                                    Text.literal("Error code " + code)
                            );
                        }
                    } else {
                        Logging.getInstance().error("Failed to remove outfit", err);

                        Cosmetica.showToast(
                                Text.translatable("toast.cosmetica.outfitDeleteError"),
                                Text.literal(err.getClass().getSimpleName())
                        );
                    }
                    return null;
                });
    }
}
