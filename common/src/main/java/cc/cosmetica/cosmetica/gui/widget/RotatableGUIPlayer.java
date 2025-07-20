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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import cc.cosmetica.kupe.api.gui.PointerEvents;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.POINTER_EVENTS;

/**
 * GUI player with shared extended interactivity, for Cosmetica's screens.
 */
public class RotatableGUIPlayer extends GUIPlayer {
    public RotatableGUIPlayer(@NotNull UUID uuid, @Nullable State<Boolean> showingElytra) {
        super(uuid, true);
        this.showingElytra = showingElytra;
    }

    private final @Nullable State<Boolean> showingElytra;
    private boolean drag = false;
    private double xStart = 0;
    private float yawStart = 0;

    public @Nullable CachedImage loreIcon, icon;

    public RotatableGUIPlayer icon(@Nullable CachedImage icon) {
        this.icon = icon;
        return this;
    }
    public RotatableGUIPlayer loreIcon(@Nullable CachedImage icon) {
        this.loreIcon = icon;
        return this;
    }

    @Override
    public List<Component> build() {
        if (showingElytra != null) {
            boolean showElytra = showingElytra.acquire(this);
            this.hideAttachments(showElytra ? CAPE : ELYTRA);
            this.showAttachments(showElytra ? ELYTRA : CAPE);
        }
        return super.build();
    }

    @Override
    public void mouseClicked(Element target, double x, double y, int button) {
        if (target.getComponent() == this) {
            xStart = x;
            yawStart = this.pose.yRotBody;
            drag = true;
        }
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        drag = false;
    }

    @Override
    public void unmount() {
        drag = false;
    }

    @Override
    public void mouseMoved(Region region, double x, double y) {
        if (drag) {
            this.pose.yRotBody = this.pose.yRotHead = yawStart - (float)(x - xStart);
        }
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet().self(Style.create().set(POINTER_EVENTS, PointerEvents.ALL));
    }
}
