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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IconButton extends Button {
    /**
     * Create a new minecraft button with the given icon overlaid.
     * @param texture the ResourceKey for the texture.
     * @param onClicked the function to run on click.
     */
    public IconButton(@NotNull ResourceKey texture, Runnable onClicked) {
        super(Text.literal(""), onClicked);
        this.texture = texture;
        this.hoverTexture = texture;
    }

    private final ResourceKey texture;
    private ResourceKey hoverTexture;

    public IconButton hoverTexture(@NotNull ResourceKey texture) {
        this.hoverTexture = texture;
        return this;
    }

    @Override
    public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
        return this.tryFixed(DEFAULT_DIMENSIONS, padding, context);
    }

    @Override
    public void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        // Button
        super.paint(canvas, region, mouseX, mouseY);
        // Image
        boolean useHoverTexture = this.isOccluding(region, canvas.getScissor().orElse(region), mouseX, mouseY, false);
        canvas.setTransparency(this.disabled ? 0.8f : 1.0f);
        canvas.drawTexture(
                region.getX(), region.getY(), region.getWidth(), region.getHeight(),
                0, useHoverTexture ? this.hoverTexture : this.texture);
        canvas.disableTransparency();
    }

    private static final Dimensions DEFAULT_DIMENSIONS = new Dimensions(20, 20);
}
