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
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.ThumbnailCache;
import cc.cosmetica.cosmetica.mixin.keybinds.KeyMappingAccessor;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.cosmetica.util.Division;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.PolyBuilder;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.cloaks.javaclient.api.OutfitsApi;
import gg.cloaks.javaclient.api.UsersApi;
import gg.cloaks.javaclient.model.Outfit;
import gg.cloaks.javaclient.model.OutfitAccessory;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * The wheel for switching outfits.
 */
public class OutfitWheelScreen extends Screen {
    public OutfitWheelScreen() {
        super(Text.translatable("screens.cosmetica.wheel").toMinecraftComponent());
        // todo maybe implement this as kupe screen so we can update outfit list automatically on outfit change
        this.options = Cosmetica.OWN_OUTFITS.peek();

        if (page > this.getLastPage()) {
            page = 0;
        }
    }

    // Important!
    // double for scroll wheel reasons. use getPage() to get the actual page.
    // remember page
    private static double page = 0;

    // scaling
    private double scaleFactor = 0.05;
    private long lastScaleTime = System.currentTimeMillis();
    private List<OutfitOption> options;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Canvas canvas = new PoseCanvas(graphics, this.minecraft, null, partialTick);

        // Measurements
        final double centreX = this.width / 2.0;
        final double centreY = this.height / 2.0;
        final double outerEdgeSize = this.getOuterEdgeRadius();
        final double innerButtonSize = outerEdgeSize * 0.2;
        final double innerEdgeSize = outerEdgeSize * 0.25;

        // Draw text
        final int titleHeight = this.getTitleHeight();
        Component title = this.getPageLabel();
        graphics.drawCenteredString(this.font, title, this.width / 2, titleHeight, 0xffffff);

        {
            int[] pageChangeButton = new int[3];
            this.getPageButtonDimensions(pageChangeButton, titleHeight, title);

            int left = pageChangeButton[0];
            int right = pageChangeButton[1];
            int pcWidth = pageChangeButton[2];

//            boolean previousPage = this.getPage() > 0;
//            boolean nextPage = this.getPage() < this.getLastPage();
            // wrap around
            boolean previousPage = this.getLastPage() > 0;
            boolean nextPage = previousPage;

            boolean hoveredY = mouseY >= titleHeight && mouseY <= titleHeight + this.font.lineHeight + 1;
            boolean hoveredPrevPage = hoveredY && mouseX >= left-pcWidth/2 && mouseX <= left+pcWidth/2+1;
            boolean hoveredNextPage = hoveredY && mouseX >= right-pcWidth/2 && mouseX <= right+pcWidth/2+1;

            graphics.drawCenteredString(this.font, Text.literal("<").toMinecraftComponent(), left, titleHeight, previousPage ? (hoveredPrevPage ? 0x888888 : 0xffffff) : 0x888888);
            graphics.drawCenteredString(this.font, Text.literal(">").toMinecraftComponent(), right, titleHeight, nextPage ? (hoveredNextPage ? 0x888888 : 0xffffff) : 0x888888);

        }
        // Draw circles
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();

        int selectedButton = getSelectedButton(mouseX, mouseY, innerButtonSize, innerEdgeSize, outerEdgeSize);
        this.drawCircles(canvas, centreX, centreY, outerEdgeSize, innerButtonSize, innerEdgeSize, selectedButton);

        RenderSystem.disableBlend();

        // Draw Icons
        this.drawThumbs(canvas, centreX, centreY, 0.5 * (outerEdgeSize + innerEdgeSize), (float) (0.5 * (outerEdgeSize - innerEdgeSize)));

        // scale up
        if (this.scaleFactor < 1) {
            long currentTime = System.currentTimeMillis();
            // each 1 = 1 tick (50ms)
            double diffTime = (currentTime - this.lastScaleTime) / 50.0;
            this.lastScaleTime = currentTime;

            final double baseChangeRate = 0.3;
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
     * @param scale the size of the icons.
     */
    private void drawThumbs(Canvas canvas, double centreX, double centreY, double distance, float scale) {

        final int nSectors = SECTORS.count();
        final double theta = 2.0 * Math.PI / nSectors;
        final int currentOutfitIndex = Cosmetica.SELECTED_OUTFIT_ID.peek().map(this::indexOf).orElse(-1);

        for (int i = 0; i < nSectors; i++) {
            int index = i + this.getPage() * nSectors;

            if (index < this.options.size()) {
                double angle = theta * (i - 1.5); // offset for centre & rotation

                final float x = (float) (centreX + distance * Math.cos(angle));
                final float y = (float) (centreY + distance * Math.sin(angle));

                final float x0 = x - scale /2;
                final float y0 = y - scale /2;
                final float x1 = x + scale /2;
                final float y1 = y + scale /2;

                OutfitOption outfit = this.options.get(index);
                canvas.setTexture(new ResourceKey(outfit.thumbnail.location));

                canvas.setTransparency(currentOutfitIndex == index || !outfit.usable ? 0.5f : 0.8f);

                PolyBuilder builder = canvas.drawQuads(PolyBuilder.Mode.POSITION_TEXTURE);
                builder.vertex(x0, y1).uv(0, 1).endVertex();
                builder.vertex(x1, y1).uv(1, 1).endVertex();
                builder.vertex(x1, y0).uv(1, 0).endVertex();
                builder.vertex(x0, y0).uv(0, 0).endVertex();
                builder.build();
            }
        }

        // centre
        final float x0 = (float) (centreX) - scale /3;
        final float y0 = (float) (centreY) - scale /3;
        final float x1 = (float) (centreX) + scale /3;
        final float y1 = (float) (centreY) + scale /3;

        canvas.setTexture(new ResourceKey(NO_OUTFIT_LOCATION));
        canvas.setTransparency(0.8f);

        PolyBuilder builder = canvas.drawQuads(PolyBuilder.Mode.POSITION_TEXTURE);
        builder.vertex(x0, y1).uv(0, 1).endVertex();
        builder.vertex(x1, y1).uv(1, 1).endVertex();
        builder.vertex(x1, y0).uv(1, 0).endVertex();
        builder.vertex(x0, y0).uv(0, 0).endVertex();
        builder.build();

        canvas.disableTransparency();
    }

    private static final ResourceLocation NO_OUTFIT_LOCATION = new ResourceKey("minecraft", "textures/item/barrier.png").toResourceLocation();

    /**
     * Draw the circles (inner button and outer ring) in the GUI.
     * @param canvas the canvas on which to draw.
     * @param centreX the centre X of the circles.
     * @param centreY the centre Y of the circles.
     * @param outerEdgeSize the radius of the outer edge of the outer ring of outfit buttons.
     * @param innerButtonSize the radius of the inner button.
     * @param innerEdgeSize the radius of the inner edge of the outer ring of outfit buttons.
     * @param highlightedSector the sector of the outer ring to highlight. Use 0-7 to highlight one of the outer-ring sectors,
     *                          8 for the inner button, anything else highlights nothing.
     */
    private void drawCircles(Canvas canvas,
                             double centreX, double centreY,
                             double outerEdgeSize, double innerButtonSize,
                             double innerEdgeSize, int highlightedSector) {
        final PolyBuilder builder = canvas.drawTriangles(PolyBuilder.Mode.POSITION_COLOUR);
        final int nOutfitSectors = SECTORS.count();
        final int nRenderSectors = 64;
        final double theta = 2.0 * Math.PI / nRenderSectors;

        float shade = highlightedSector == 8 ? 1.0f : 0.4f;

        // Inner Circle
        for (int i = 0; i < nRenderSectors; i++) {
            double angle = theta * i;

            this.drawRenderSector(builder, theta, angle, centreX, centreY, innerButtonSize, shade);
        }

        int currentOutfitSector = this.indexOf(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse("")) - this.getPage() * nOutfitSectors;

        // Outer Circle
        for (int i = 0; i < nRenderSectors; i++) {
            double angle = theta * i;
            final int sector = SECTORS.get(angle);

            shade = (sector == currentOutfitSector) ? 0.0f : (
                    sector == highlightedSector ? 1 : (
                            (sector & 1) == 0 ? 0.3f : 0.4f
                    )
            );

            this.drawRenderArc(builder, theta, angle, centreX, centreY, innerEdgeSize, outerEdgeSize, shade);
        }

        builder.build();
    }

    /**
     * Draw a render sector triangle.
     */
    private void drawRenderSector(PolyBuilder builder, double theta, double angle,
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

    private void drawRenderArc(PolyBuilder builder, double theta, double angle,
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
        if (!CosmeticaSettings.TOGGLE_OUTFIT_WHEEL.get()) {
            if (!isDown(Keybinds.SELECT_OUTFIT)) {
                this.onClose();
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        InputConstants.Key k = InputConstants.getKey(key, scan);
        KeyMapping.set(k, true);
        KeyMapping.click(k);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mod) {
        InputConstants.Key k = InputConstants.getKey(key, scan);
        KeyMapping.set(k, false);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) { // left click
            if (button == 1) {
                page = (int)page + 1;
                if (page > this.getLastPage()) {
                    page = 0;
                }

                GuiUtils.playClick();
                return true;
            }
            return false;
        }
        assert this.minecraft != null; // shut up compiler

        double outerEdgeSize = this.getOuterEdgeRadius();
        double innerButtonSize = outerEdgeSize * 0.2;
        double innerEdgeSize = outerEdgeSize * 0.25;

        // First check for page buttons
        int titleHeight = this.getTitleHeight();
        boolean hoveredY = mouseY >= titleHeight && mouseY <= titleHeight + this.font.lineHeight + 1;

        if (hoveredY) {
            int[] measurements = new int[3];
            this.getPageButtonDimensions(measurements, titleHeight, this.getPageLabel());

            int left = measurements[0];
            int right = measurements[1];
            int pcWidth = measurements[2];

            boolean hoveredPrevPage = mouseX >= left-pcWidth/2 && mouseX <= left+pcWidth/2+1;
            boolean hoveredNextPage = mouseX >= right-pcWidth/2 && mouseX <= right+pcWidth/2+1;

            if (hoveredNextPage) {
                page = (int)page + 1;
                if (page > this.getLastPage()) {
                    page = 0;
                }

                GuiUtils.playClick();
                return true;
            } else if (hoveredPrevPage) {
                page = (int)page - 1;
                if (page < 0) page = this.getLastPage();

                GuiUtils.playClick();
                return true;
            }
        }

        // Then check for selection buttons
        int selectedButton = this.getSelectedButton((float) mouseX, (float) mouseY,
                innerButtonSize, innerEdgeSize, outerEdgeSize);

        if (selectedButton > -1) {
            if (selectedButton < 8) {
                int index = selectedButton + this.getPage() * SECTORS.count();

                if (index < this.options.size()) {
                    OutfitOption outfit = this.options.get(index);

                    if (!outfit.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""))) {
                        GuiUtils.playClick();

                        // switch outfit
                        outfit.equipAsync();
                    }
                }
            } else if (selectedButton == 8) {
                GuiUtils.playClick();
                clearOutfit();
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int prevPage = this.getPage();

        // wrap around
        page = (page - delta) % (this.getLastPage() + 1);
        if (page < 0) {
            page = this.getLastPage() + 1 + page;
        }

        // audio feedback for scrolling
        if (prevPage != this.getPage()) {
            GuiUtils.playClick();
        }
        return true;
    }

    /**
     * Get the last page that exists in the wheel.
     * @return the last page that exists in the wheel.
     */
    private int getLastPage() {
        return (this.options.size() - 1) / SECTORS.count();
    }

    private int getPage() {
        return (int) page;
    }

    /**
     * Get the radius of the outer edge of the outer ring of outfits.
     * @return the radius of the outer edge of the outer ring of outfits.
     */
    private double getOuterEdgeRadius() {
//        float guiScale = Minecraft.getInstance().options.guiScale / (Minecraft.getInstance().options.guiScale < 3 ? 3.0f : 4.0f);
        return this.scaleFactor * (Minecraft.getInstance().options.guiScale().get() == 4 ? (this.height / 2.5) : (this.height / 3.0));
    }

    private int getTitleHeight() {
//        float guiScale = Minecraft.getInstance().options.guiScale / (Minecraft.getInstance().options.guiScale < 3 ? 3.0f : 4.0f);
        return this.height / 2 - (int)(Minecraft.getInstance().options.guiScale().get() == 4 ? (this.height / 2.5) : (this.height / 3.0)) - 12;
    }

    private Component getPageLabel() {
        return Text.translatable("label.wheel.page", String.valueOf(this.getPage() + 1), String.valueOf(this.getLastPage() + 1)).toMinecraftComponent();
    }

    private void getPageButtonDimensions(int[] result, int titleHeight, Component title) {
        int titleWidth = this.font.width(title);
        int pcWidth = this.font.width(Text.literal("<").toMinecraftComponent());
        int leftPageChange = this.width/2 - titleWidth/2 - 12;
        int rightPageChange = this.width/2 + titleWidth/2 + 12;
        // results
        result[0] = leftPageChange;
        result[1] = rightPageChange;
        result[2] = pcWidth;
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
        return CosmeticaSettings.TOGGLE_OUTFIT_WHEEL.get();
    }

    /**
     * The angles of the start of each sector of the outer ring.
     */
//    private static final Division<Integer> SECTORS = new Division<Integer>()
//            .addSection(2 * Math.PI * (6.0/8.0), 0)
//            .addSection(2 * Math.PI * (7.0/8.0), 1)
//            .addSection(2 * Math.PI * (8.0/8.0), 2)
//            .addSection(0, 2)
//            .addSection(2 * Math.PI * (1.0/8.0), 3)
//            .addSection(2 * Math.PI * (2.0/8.0), 4)
//            .addSection(2 * Math.PI * (3.0/8.0), 5)
//            .addSection(2 * Math.PI * (4.0/8.0), 6)
//            .addSection(2 * Math.PI * (5.0/8.0), 7);
    private static final Division<Integer> SECTORS = new Division<Integer>()
            .addSection(2 * Math.PI * (4.0/6.0), 0)
            .addSection(2 * Math.PI * (5.0/6.0), 1)
            .addSection(2 * Math.PI * (6.0/6.0), 2)
            .addSection(0, 2)
            .addSection(2 * Math.PI * (1.0/6.0), 3)
            .addSection(2 * Math.PI * (2.0/6.0), 4)
            .addSection(2 * Math.PI * (3.0/6.0), 5);

    static void clearOutfit() {
        if (Cosmetica.SELECTED_OUTFIT_ID.peek().isPresent()) {
            // Equip nothing
            CosmeticaAPI.outfits().requestAsync(OutfitsApi::unequip)
                    .thenAcceptAsync(user -> {
                        SelfCosmeticManager.clear();
                        Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Unequipped Outfit successfully");
                    }, Minecraft.getInstance())
                    .exceptionally(e -> {
                        new RuntimeException("Outfits Controller Unequip", e).printStackTrace();
                        return null;
                    });
        }
    }

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
            this.name = outfit.getName();
            this.thumbnail = outfit.getThumbnail() == null ? CosmeticEntry.NO_THUMBNAIL :
                    ThumbnailCache.getOrCreateImage(
                            new CosmeticaTexture.Builder(outfit.getThumbnail() + "?width=276", Cosmetica.LOADING_TEXTURE)
                                .frames(8, 1)
                                .ignoreTilesheet(true)
                                .failToLoadTexture(Cosmetica.FALLBACK_TEXTURE)
                                .autoAnimate(CosmeticaTexture.AutoAnimate.NEVER),
                            false
                    );
            this.usable = outfit.isUsable();
            this.accessories = outfit.getAccessories();
            this.capeId = outfit.getCloak() == null ? "" : outfit.getCloak().getId();
            this.elytraId = outfit.getElytra() == null ? "" : outfit.getElytra().getId();
        }
        final String id;
        final String name;
        final CachedImage thumbnail;
        final boolean usable;
        final List<OutfitAccessory> accessories;
        final String capeId;
        final String elytraId;

        void equipAsync() {
            // visually switch immediately
            // -> we should always show currently equipped outfit in wheel and not whats rendering! (as that is delayed by load)
            Cosmetica.SELECTED_OUTFIT_ID.set(Optional.of(this.id));

            CosmeticaAPI.outfits().requestAsync(api -> api.equip(this.id))
                    .thenAcceptAsync(user -> {
                        if (this.capeId.isEmpty() || this.elytraId.isEmpty()) {
                            // refresh external capes
                            CosmeticaAPI.users().requestAsync(UsersApi::getSelf)
                                    .thenAcceptAsync(user_ -> {
                                        SelfCosmeticManager.update(new PlayerResponse().user(user_).isUser(true));
                                    }, Minecraft.getInstance())
                                    .exceptionally(ex -> {
                                        return null;
                                    });
                        } else {
                            SelfCosmeticManager.update(new PlayerResponse().user(user).isUser(true));
                        }
                        Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Equip Success!");
                    }, Minecraft.getInstance())
                    .exceptionally(except -> {
                        new RuntimeException("Outfits Controller Equip", except).printStackTrace();
                        return null;
                    });
        }
    }
}
