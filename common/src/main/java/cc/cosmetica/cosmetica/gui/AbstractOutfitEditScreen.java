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

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import gg.cloaks.javaclient.ApiException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

public abstract class AbstractOutfitEditScreen extends Screen {
    public AbstractOutfitEditScreen(Text title,
                                    Text submitText,
                                    Tooltip processingTooltip,
                                    Text errorText,
                                    String initialName,
                                    boolean initialPublic) {
        super(title);
        this.outfitName = new State<>(initialName);
        this.outfitPublic = new State<>(initialPublic);
        this.updating = new State<>(false);
        this.submitText = submitText;
        this.processingTooltip = processingTooltip;
        this.errorText = errorText;
    }

    private final State<String> outfitName;
    private final State<Boolean> outfitPublic;
    private final State<Boolean> updating;
    private final Text submitText;
    private final Tooltip processingTooltip;
    private final Text errorText;

    @Override
    protected Component[] buildScreen() {
        boolean outfitPublic = this.outfitPublic.acquire(this);
        boolean disabled = this.updating.acquire(this);

        // TODO pop up if too many outfits
        return new Component[] {
                new TextBox(Text.translatable("label.create_new_outfit.outfitName"), this.outfitName, true, 100)
                        .setDisabled(disabled),
                new Button(Text.translatable(
                        "button.createNewOutfit.searchable",
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
        return new Div() {
            @Override
            public List<Component> build() {
                // todo check other character constraints
                // combined with trim in case \pSpace doesn't account for control characters - though I doubt they can type those
                String outfitName = strip(AbstractOutfitEditScreen.this.outfitName.acquire(this).trim());
                Optional<Text> nameErrorTooltip = AbstractOutfitEditScreen.this.checkNameRequirements(outfitName, outfitPublic);
                boolean legalName = !nameErrorTooltip.isPresent();

                return Arrays.asList(
                        new Button(AbstractOutfitEditScreen.this.submitText, () -> {
                            // disable buttons/textbox
                            AbstractOutfitEditScreen.this.updating.set(true);

                            // if successful close screen. if fail just re-enable buttons
                            submit(outfitName, outfitPublic)
                                    .thenAccept(v -> Screens.closeCurrentScreen())
                                    .exceptionally(Cosmetica.mainThreadExcept(err -> {
                                        Logging.getInstance().error("Error creating or editing outfit", err);
                                        AbstractOutfitEditScreen.this.updating.set(false);

                                        // Show Toast

                                        if (err instanceof CompletionException) {
                                            err = err.getCause();
                                        }

                                        if (err instanceof ApiException) {
                                            int code = ((ApiException) err).getCode();

                                            if (code == 404) {
                                                Cosmetica.showToast(
                                                        Text.translatable("toast.cosmetica.outfit404"),
                                                        null
                                                );
                                            } else {
                                                Cosmetica.showToast(
                                                        AbstractOutfitEditScreen.this.errorText,
                                                        Text.literal("Error code " + code)
                                                );
                                            }
                                        } else {
                                            Cosmetica.showToast(
                                                    AbstractOutfitEditScreen.this.errorText,
                                                    Text.literal(err.getClass().getSimpleName()));
                                        }
                                    }));
                        }).setDisabled(disabled || !legalName)
                                .withStyle(Style.create()
                                        .set(TOOLTIP, legalName || nameErrorTooltip.get() == Text.empty() ? (disabled ? Optional.of(AbstractOutfitEditScreen.this.processingTooltip) : Optional.empty())
                                                : Optional.of(new Tooltip(nameErrorTooltip.get())))
                                ),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen).setDisabled(disabled)
                                .withStyle(Style.create()
                                        .set(TOOLTIP, disabled ? Optional.of(AbstractOutfitEditScreen.this.processingTooltip) : Optional.empty())
                                )
                );
            }
        };
    }

    protected Optional<Text> checkNameRequirements(String name, boolean outfitPublic) {
        final int nameMinChars = 3;

        if (name.length() < nameMinChars) {
            return Optional.of(Text.translatable("tooltip.cosmetica.notLongEnough"));
        }

        return Optional.empty();
    }

    /**
     * Submit outfit and handle success behaviour (except closing the screen, which is handled by the base).
     * @param outfitName the outfit name setting.
     * @param outfitPublic the outfit public setting.
     * @return a completable future which resolves with or without an exception.
     */
    abstract protected CompletableFuture<Void> submit(String outfitName, boolean outfitPublic);
}
