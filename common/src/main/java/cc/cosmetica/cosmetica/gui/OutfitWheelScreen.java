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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.mixin.KeyMappingAccessor;
import cc.cosmetica.cosmetica.util.Division;
import cc.cosmetica.cosmetica.util.TriangleBuilder;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import gg.cloaks.javaclient.model.Outfit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The wheel for switching outfits.
 */
public class OutfitWheelScreen extends Screen {
    public OutfitWheelScreen() {
        super(Text.translatable("screens.cosmetica.wheel").toMinecraftComponent());
        // todo maybe implement this as kupe screen so we can update outfitId automatically on outfit change
        this.outfitId = Optional.ofNullable(Cosmetica.OWN_COSMETICS.peek()).flatMap(Cosmetics::getOutfitId);
        this.options = Cosmetica.OWN_OUTFITS.peek();
    }

    // Important!
    // double for scroll wheel reasons. use getPage() to get the actual page.
    private double page = 0;
    private final Optional<String> outfitId;

    // scaling
    private double scaleFactor = 0.05;
    private long lastScaleTime = System.currentTimeMillis();
    private List<OutfitOption> options;

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick) {
        Canvas canvas = new PoseCanvas(stack, this.minecraft, null, partialTick);
        Supplier<TriangleBuilder> triangles = () -> new TriangleBuilder(
                Tesselator.getInstance().getBuilder(),
                QuadBuilder.Mode.POSITION_COLOUR,
                stack.last().pose()
        );

        // Draw text


        // Draw circles
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        double centreX = this.width / 2.0;
        double centreY = this.height / 2.0;
        double outerEdgeSize = this.scaleFactor * this.height / 2.5;
        double innerButtonSize = outerEdgeSize * 0.2;
        double innerEdgeSize = outerEdgeSize * 0.25;

        int selectedButton = getSelectedButton(mouseX, mouseY, innerButtonSize, innerEdgeSize, outerEdgeSize);
        this.drawCircles(triangles, centreX, centreY, outerEdgeSize, innerButtonSize, innerEdgeSize, selectedButton);

        RenderSystem.disableBlend();

        // Draw Icons
        this.drawThumbs(canvas, centreX, centreY, 0.5 * (outerEdgeSize + innerEdgeSize));

        // scale up
        if (this.scaleFactor < 1) {
            long currentTime = System.currentTimeMillis();
            // each 1 = 1 tick (50ms)
            double diffTime = (currentTime - this.lastScaleTime) / 50.0;
            this.lastScaleTime = currentTime;

            final double baseChangeRate = 0.2;
            double scaleChange = baseChangeRate - 0.01 * baseChangeRate * Math.exp(2 * this.scaleFactor);
            this.scaleFactor += scaleChange * diffTime;

            if (this.scaleFactor > 1) {
                this.scaleFactor = 1;
            }
        }
    }

    /**
     * Draw the thumbnails for selectable outfits.
     * @param canvas the canvas for rendering.
     * @param centreX the x position of the centre of the outfit ring.
     * @param centreY the y position of the centre of the outfit ring.
     * @param distance the distance from the outfit ring at which to render the icons.
     */
    private void drawThumbs(Canvas canvas, double centreX, double centreY, double distance) {
        float scale = this.height <= 380 ? 1.0f : 2.0f;

        final int nSectors = 8;
        final double theta = 2.0 * Math.PI / nSectors;
        final int currentOutfitIndex = this.outfitId.map(this::indexOf).orElse(-1);

        RenderSystem.enableTexture();

        for (int i = 0; i < nSectors; i++) {
            int index = i + this.getPage() * nSectors;

            if (index < this.options.size()) {
                double angle = theta * (i - 1.5);

                final float x = (float) (centreX + distance * Math.cos(angle));
                final float y = (float) (centreY + distance * Math.sin(angle));
                final float size = 32 * scale * (float)this.scaleFactor;

                final float x0 = x - size/2;
                final float y0 = y - size/2;
                final float x1 = x + size/2;
                final float y1 = y + size/2;

                OutfitOption outfit = this.options.get(index);
                Minecraft.getInstance().getTextureManager().bind(outfit.thumbnail.location);

                canvas.setTransparency(currentOutfitIndex == index || !outfit.usable ? 0.5f : 0.8f);

                QuadBuilder builder = canvas.drawQuads(QuadBuilder.Mode.POSITION_TEXTURE);
                builder.vertex(x0, y1).uv(0, 1).endVertex();
                builder.vertex(x1, y1).uv(1, 1).endVertex();
                builder.vertex(x1, y0).uv(1, 0).endVertex();
                builder.vertex(x0, y0).uv(0, 0).endVertex();
                builder.build();
            }
        }

        canvas.disableTransparency();
    }

    /**
     * Draw the circles (inner button and outer ring) in the GUI.
     * @param centreX the centre X of the circles.
     * @param centreY the centre Y of the circles.
     * @param outerEdgeSize the radius of the outer edge of the outer ring of outfit buttons.
     * @param innerButtonSize the radius of the inner button.
     * @param innerEdgeSize the radius of the inner edge of the outer ring of outfit buttons.
     * @param highlightedSector the sector of the outer ring to highlight. Use 0-7 to highlight one of the outer-ring sectors,
     *                          8 for the inner button, anything else highlights nothing.
     */
    private void drawCircles(Supplier<TriangleBuilder> builderSupplier,
                             double centreX, double centreY,
                             double outerEdgeSize, double innerButtonSize,
                             double innerEdgeSize, int highlightedSector) {
        final TriangleBuilder builder = builderSupplier.get();
        final int nOutfitSectors = 8;
        final int nRenderSectors = 64;
        final double theta = 2.0 * Math.PI / nRenderSectors;

        float shade = highlightedSector == 8 ? 1.0f : 0.2f;

        // Inner Circle
        for (int i = 0; i < nRenderSectors; i++) {
            double angle = theta * i;

            this.drawRenderSector(builder, theta, angle, centreX, centreY, innerButtonSize, shade);
        }

        int currentOutfitSector = this.indexOf(this.outfitId.orElse("")) - this.getPage() * nOutfitSectors;

        // Outer Circle
        for (int i = 0; i < nRenderSectors; i++) {
            double angle = theta * i;
            final int sector = SECTORS.get(angle);

            shade = (sector == currentOutfitSector) ? 0.0f : (
                    sector == highlightedSector ? 1 : (
                            (sector & 1) == 0 ? 0.2f : 0.4f
                    )
            );

            this.drawRenderArc(builder, theta, angle, centreX, centreY, innerEdgeSize, outerEdgeSize, shade);
        }

        builder.build();
    }

    /**
     * Draw a render sector triangle.
     */
    private void drawRenderSector(TriangleBuilder builder, double theta, double angle,
                                  double centreX, double centreY, double radius, float shade) {
        builder.vertex(centreX, centreY)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + radius * Math.cos(angle + theta), centreY + radius * Math.sin(angle + theta))
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + radius * Math.cos(angle), centreY + radius * Math.sin(angle))
                .colour(shade, shade, shade, 0.5f)
                .endVertex();
    }

    private void drawRenderArc(TriangleBuilder builder, double theta, double angle,
                               double centreX, double centreY, double innerRadius, double outerRadius,
                               float shade) {
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        final double cosNext = Math.cos(angle + theta);
        final double sinNext = Math.sin(angle + theta);

        // 2, 1, 0

        builder.vertex(centreX + innerRadius * cos, centreY + innerRadius * sin)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + outerRadius * cosNext, centreY + outerRadius * sinNext)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + outerRadius * cos, centreY + outerRadius * sin)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        // 3, 1, 2

        builder.vertex(centreX + innerRadius * cosNext, centreY + innerRadius * sinNext)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + outerRadius * cosNext, centreY + outerRadius * sinNext)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();

        builder.vertex(centreX + innerRadius * cos, centreY + innerRadius * sin)
                .colour(shade, shade, shade, 0.5f)
                .endVertex();
    }

    /**
     * Get the index of the given outfit in the list of unlocked outfits.
     * @param outfit the outfit id for which to find the index.
     * @return the index of the given outfit in the list of unlocked outfits. -1 if the outfit is not present.
     */
    private int indexOf(String outfit) {
        for (int i = 0; i < this.options.size(); i++) {
            if (this.options.get(i).id.equals(outfit)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void tick() {
        if (!isDown(Keybinds.SELECT_OUTFIT)) {
            this.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double outerEdgeSize = this.getOuterEdgeRadius();
        double innerButtonSize = outerEdgeSize * 0.2;
        double innerEdgeSize = outerEdgeSize * 0.25;

        int selectedButton = this.getSelectedButton((float) mouseX, (float) mouseY,
                innerButtonSize, innerEdgeSize, outerEdgeSize);

        if (button == 0 && selectedButton > -1) {
            if (selectedButton < 8) {
                int index = selectedButton + this.getPage() * 8;

                if (index < this.options.size()) {
                    OutfitOption outfit = this.options.get(index);

                    if (!outfit.id.equals(this.outfitId.orElse(""))) {
                        assert this.minecraft != null;

                        this.minecraft.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                        );

                        // switch outfit
                        CosmeticaAPI.performAsync(api -> api.outfitsControllerEquip(outfit.id));
                        // close the GUI
                        this.onClose();
                    }
                }
            }

            return true;
        }

        return false;
    }

    // TODO a way to scroll without needing a scroll wheel. A/D? < > Buttons?
    // Likely a <  Page 1/1   > design
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            this.page = Math.max(0, this.page - delta);
        } else {
            this.page = Math.min(this.getLastPage(), this.page - delta);
        }

        return true;
    }

    /**
     * Get the last page that exists in the wheel.
     * @return the last page that exists in the wheel.
     */
    private int getLastPage() {
        return (this.options.size() - 1) / 8;
    }

    private int getPage() {
        return (int) this.page;
    }

    /**
     * Get the radius of the outer edge of the outer ring of outfits.
     * @return the radius of the outer edge of the outer ring of outfits.
     */
    private double getOuterEdgeRadius() {
        return this.scaleFactor * this.height / 2.5;
    }

    /**
     * Get the selected button in the GUI.
     * @param mouseX the x position of the mouse on the screen.
     * @param mouseY the y position of the mouse on the screen.
     * @param innerButtonSize the radius of the central (inner) button.
     * @param innerEdgeSize the radius of the inner edge of the outer ring of buttons.
     * @param outerEdgeSize the radius of the outer edge of the outer ring of buttons.
     * @return the sector selected. -1 is returned if no button is selected, 8 is returned if the centre button is
     * selected, and 0-7 are returned for the buttons on the outer ring of buttons.
     */
    private int getSelectedButton(float mouseX, float mouseY,
                                  double innerButtonSize, double innerEdgeSize, double outerEdgeSize) {
        float[] mousePosPolar = rect2polar(mouseX - (float)(this.width / 2.0), mouseY - (float)(this.height / 2.0));

        if (mousePosPolar[0] < innerButtonSize) {
            return 8;
        } else if (mousePosPolar[0] >= innerEdgeSize && mousePosPolar[0] < outerEdgeSize) {
            return SECTORS.get(mousePosPolar[1]);
        }

        // no button selected
        return -1;
    }

    // Non-Render Non-Input Screen Methods

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * The angles of the start of each sector of the outer ring.
     */
    private static final Division<Integer> SECTORS = new Division<Integer>()
            .addSection(2 * Math.PI * (6.0/8.0), 0)
            .addSection(2 * Math.PI * (7.0/8.0), 1)
            .addSection(2 * Math.PI * (8.0/8.0), 2)
            .addSection(0, 2)
            .addSection(2 * Math.PI * (1.0/8.0), 3)
            .addSection(2 * Math.PI * (2.0/8.0), 4)
            .addSection(2 * Math.PI * (3.0/8.0), 5)
            .addSection(2 * Math.PI * (4.0/8.0), 6)
            .addSection(2 * Math.PI * (5.0/8.0), 7);

    /**
     * Check if the given key for the key mapping is down. This is preferred over .isDown() due to isDown only working is this.minecraft.screen == null.
     * @param mapping the mapping to check.
     * @return if the key mapping is down.
     */
    private static boolean isDown(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }

        InputConstants.Key key = ((KeyMappingAccessor)mapping).cosmetica$getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        int value = key.getValue();

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, value) != GLFW.GLFW_RELEASE;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, value) != GLFW.GLFW_RELEASE;
        }

        return false;
    }

    /**
     * Converts the given coordinates from rectangular (cartesian) to polar space.
     * @param x the x coordinate in cartesian coordinates.
     * @param y the y coordinate in cartesian coordinates.
     * @return a size-2 array containing [r, theta], where r is the distance from the origin, and theta is the angle
     * clockwise from the horizontal, in radians. If the case that r is 0, theta is also 0.
     */
    private static float[] rect2polar(float x, float y) {
        float r = (float) Math.sqrt(x * x + y * y);
        float theta = (float) Math.atan2(y, x);

        if (theta < 0) {
            theta += 2 * Math.PI; // Adjusted to ensure theta is between 0 and 2 pi
        }

        return new float[] {r, theta};
    }

    public static class OutfitOption {
        public OutfitOption(Outfit outfit) {
            this.id = outfit.getId();
            this.thumbnail = CosmeticaModel.getOrCreateImage("thumbs-o", // thumbs-outfit
                    this.id,
                    outfit.getThumbnail(),
                    1,
                    1);
            this.usable = outfit.isUsable();
        }
        private final String id;
        private final CachedImage thumbnail;
        private final boolean usable;
    }

    private class OutfitWheelContext implements Context {
        @Override
        public int getWidth(Text text) {
            return OutfitWheelScreen.this.font.width(text.getDisplayString());
        }

        @Override
        public int getLineHeight() {
            return OutfitWheelScreen.this.font.lineHeight;
        }

        @Override
        public int getTextHeight(Text text, int maxWidth) {
            return this.getLineHeight();
        }

        @Override
        public AbstractTexture getTexture(ResourceKey location) {
            return null;
        }

        @Override
        public Optional<Dimensions> getImageDimensions(ResourceKey location) throws IOException {
            return Optional.empty();
        }

        @Override
        public List<Renderable> split(Text text, int maxWidth) {
            return List.of();
        }

        @Override
        public int getViewWidth() {
            return 0;
        }

        @Override
        public int getViewHeight() {
            return 0;
        }
    }
}
