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
import cc.cosmetica.kupe.api.PolyBuilder;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.Image;
import cc.cosmetica.kupe.api.maths.Region;

/**
 * Image which can be clicked. Not to be confused with {@link IconButton} which is a button with an image overlay.
 */
public class ClickableImage extends Image {
    public ClickableImage(ResourceKey texture, Runnable onClick) {
        super(texture);
        this.texture = texture;
        this.onClick = onClick;
    }

    private final ResourceKey texture;
    private final Runnable onClick;
    private boolean disabled = false;
    private float opacity = 1.0f;

    protected boolean canDrawDelete() {
        return true;
    }

    public ClickableImage setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @Override
    public Image setTransparent(float opacity) {
        this.opacity = opacity;
        return super.setTransparent(opacity);
    }

    @Override
    public void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        if (this.disabled) {
            PolyBuilder builder = canvas.drawQuads(PolyBuilder.Mode.POSITION_COLOUR_TEXTURE);

            // anticlockwise
            final float shade = 0.4f;
            builder.vertex(region.getX(), region.getEndY(), 0).colour(shade, shade, shade, this.opacity).uv(0, 1).endVertex();
            builder.vertex(region.getEndX(), region.getEndY(), 0).colour(shade, shade, shade, this.opacity).uv(1, 1).endVertex();
            builder.vertex(region.getEndX(), region.getY(), 0).colour(shade, shade, shade, this.opacity).uv(1, 0).endVertex();
            builder.vertex(region.getX(), region.getY(), 0).colour(shade, shade, shade, this.opacity).uv(0, 0).endVertex();

            builder.build();
        } else if (region.contains(mouseX, mouseY)) {
            if (this.canDrawDelete()) {
                canvas.setTransparency(1.0f);
                canvas.setTexture(this.texture);

                PolyBuilder builder = canvas.drawQuads(PolyBuilder.Mode.POSITION_COLOUR_TEXTURE);

                // anticlockwise
                builder.vertex(region.getX(), region.getEndY(), 0).colour(1.0f, 0.2f, 0.2f, this.opacity).uv(0, 1).endVertex();
                builder.vertex(region.getEndX(), region.getEndY(), 0).colour(1.0f, 0.2f, 0.2f, this.opacity).uv(1, 1).endVertex();
                builder.vertex(region.getEndX(), region.getY(), 0).colour(1.0f, 0.2f, 0.2f, this.opacity).uv(1, 0).endVertex();
                builder.vertex(region.getX(), region.getY(), 0).colour(1.0f, 0.2f, 0.2f, this.opacity).uv(0, 0).endVertex();

                builder.build();
            }
        } else {
            super.paint(canvas, region, mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(Element target, double x, double y, int button) {
        if (target.getComponent() == this) {
            this.onClick.run();
        }
    }
}
