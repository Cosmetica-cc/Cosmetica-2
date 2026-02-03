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
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.fixedSize;

public abstract class AbstractConfirmScreen extends BaseCosmeticaScreen {
    protected AbstractConfirmScreen(Text title) {
        super(title);
    }

    protected State<Boolean> setting = new State<>(false);

    @Override
    protected final Component[] buildScreen() {
        boolean setting = this.setting.acquire(this);

        return new Component[] {
                createConfirmLabel(),
                new Div(
                        new Button(Text.GUI_PROCEED, this::onConfirm).setDisabled(setting)
                          .withStyle(Style.create().set(CommonProperties.TOOLTIP,
                                                !setting ? Optional.empty() : Optional.of(this.getUpdatingTooltip()))),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                ).tag("horizontal")
        };
    }

    /**
     * Get the main warning label to display on the page.
     * @return the label to show on the page.
     */
    abstract protected Label createConfirmLabel();
    /**
     * Get the tooltip to show after the confirm button is pressed.
     * @return the tooltip to show while processing after the confirm button is pressed.
     */
    abstract protected Tooltip getUpdatingTooltip();
    /**
     * Action to perform on 'onConfirm'. May update {@link AbstractConfirmScreen#setting}.
     */
    abstract protected void onConfirm();

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
