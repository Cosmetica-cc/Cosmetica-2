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

import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.impl.PoseCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;

/**
 * Toast for Cosmetica errors.
 */
public class CosmeticaToast implements Toast {
    public CosmeticaToast(Text text, @Nullable Text description) {
        this.title = text;
        this.description = description;
        this.changed = true;
    }

    private final Text title;
    private final @Nullable Text description;
    private boolean changed;
    private long lastChanged;
    private Visibility wantedVisibility = Visibility.HIDE;

    @Override
    public Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    public void update(ToastManager toastManager, long l) {
        if (this.changed) {
            this.changed = false;
            this.lastChanged = l;
        }
        long displayTime = (long) (5000L * toastManager.getNotificationDisplayTimeMultiplier());

        this.wantedVisibility = l - this.lastChanged < displayTime ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, long l) {
        Matrix3x2fStack poseStack = graphics.pose();

        Canvas canvas = new PoseCanvas(graphics, Minecraft.getInstance(), null, 0);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE.toResourceLocation(), 0, 0, this.width(), this.height());

        Vector3f pos = new Vector3f(18, 12, 0);
        pos.mul(poseStack);

        if (this.description == null) {
            canvas.drawText(this.title, (int)pos.x(), (int)pos.y(), -256);
        } else {
            canvas.drawText(this.title, (int)pos.x(), (int)pos.y() - 6, -256);
            canvas.drawText(this.description, (int)pos.x(), (int)pos.y() + 6, -1);
        }
    }

    private static final ResourceKey BACKGROUND_SPRITE = new ResourceKey("minecraft", "toast/system");
}
