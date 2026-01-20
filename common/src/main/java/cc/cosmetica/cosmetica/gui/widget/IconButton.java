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

import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.Context;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.SizedElement;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IconButton extends Button {
    public IconButton(ResourceKey texture, Runnable onClicked) {
        this(texture, onClicked, null);
    }

    /**
     * Create a new minecraft button with the given icon overlayed.
     * @param texture the ResourceKey for the texture.
     * @param onClicked the function to run on click.
     * @param onMouseMoved the function to run when the mouse moves on this screen.
     */
    public IconButton(ResourceKey texture, Runnable onClicked, @Nullable MouseMotionListener onMouseMoved) {
        super(Text.literal(""), onClicked);
        this.texture = texture;
        this.onMouseMoved = onMouseMoved;
    }

    private final ResourceKey texture;
    private final MouseMotionListener onMouseMoved;

    @Override
    public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
        return this.tryFixed(DEFAULT_DIMENSIONS, padding, context);
    }

    @Override
    public void mouseMoved(Region region, double x, double y) {
        if (this.onMouseMoved != null) {
            this.onMouseMoved.accept(region, x, y);
        }
    }

    @Override
    public void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        // Button
        super.paint(canvas, region, mouseX, mouseY);
        // Image
        canvas.setTransparency(this.disabled ? 0.8f : 1.0f);
        canvas.drawTexture(
                region.getX(), region.getY(), region.getWidth(), region.getHeight(),
                0, this.texture);
        canvas.disableTransparency();
    }

    private static final Dimensions DEFAULT_DIMENSIONS = new Dimensions(20, 20);

    @FunctionalInterface
    public interface MouseMotionListener {
        void accept(Region region, double x, double y);
    }
}
