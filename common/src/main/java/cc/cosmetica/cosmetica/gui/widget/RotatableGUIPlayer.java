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

import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import cc.cosmetica.kupe.api.gui.PointerEvents;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.POINTER_EVENTS;

public class RotatableGUIPlayer extends GUIPlayer {
    public RotatableGUIPlayer(@NotNull UUID uuid) {
        super(uuid, true);
    }

    private boolean drag = false;
    private double xStart = 0;
    private float yawStart = 0;

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
