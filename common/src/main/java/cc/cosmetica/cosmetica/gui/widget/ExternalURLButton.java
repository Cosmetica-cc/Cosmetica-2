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

package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.PointerEvents;
import cc.cosmetica.kupe.api.gui.Tooltip;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.POINTER_EVENTS;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.TOOLTIP;

public class ExternalURLButton extends IconButton {
    /**
     * Create a new minecraft button to open an external URL.
     * @param texture the ResourceKey for the texture.
     * @param page the external url to open. If this starts with a slash, it will be interpreted as a cosmetica url.
     * @param defaultTooltip specify what tooltip should be attached when not overridden with 'copied URL'.
     */
    public ExternalURLButton(ResourceKey texture, String page, @Nullable Tooltip defaultTooltip) {
        this(texture, page, defaultTooltip, new AtomicReference<>());
    }

    private ExternalURLButton(ResourceKey texture, String page, @Nullable Tooltip defaultTooltip, AtomicReference<ExternalURLButton> hack) {
        super(texture, page.length() > 1 && page.charAt(0) == '/' ? () -> hack.get().openCosmetica() : () -> hack.get().openGeneric());

        hack.set(this);
        if (page.length() > 1 && page.charAt(0) == '/') {
            this.page = page.substring(1);
        } else {
            this.page = page;
        }

        this.defaultTooltip = defaultTooltip;
    }

    private final State<Boolean> clicked = new State<>(false);
    private final String page;
    private final @Nullable Tooltip defaultTooltip;

    private void openCosmetica() {
        Cosmetica.openWebPanel(page);
        this.clicked.set(true);
    }

    private void openGeneric() {
        Cosmetica.copyAndOpenURL(page);
        this.clicked.set(true);
    }

    @Override
    public void mouseMoved(Region region, double x, double y) {
        if (this.clicked.peek() && !region.contains((int)x, (int)y)) {
            this.clicked.set(false);
        }
    }

    @Override
    public List<Component> build() {
        this.clicked.acquire(this);
        return ImmutableList.of();
    }

    @Override
    public Stylesheet getStylesheet() {
        // style sheet is only called by kupe after build, but I don't want to break the expected contract
        boolean clicked = this.clicked.peek();
        return new Stylesheet()
                .self(Style.create()
                        .set(TOOLTIP, clicked ? Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.copiedURL"))) : Optional.ofNullable(this.defaultTooltip))
                        .set(POINTER_EVENTS, PointerEvents.ALL));
    }
}
