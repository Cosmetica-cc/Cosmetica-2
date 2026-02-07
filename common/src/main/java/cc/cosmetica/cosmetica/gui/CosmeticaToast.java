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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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
        PoseStack poseStack = graphics.pose();

        Canvas canvas = new PoseCanvas(graphics, Minecraft.getInstance(), null, 0);

        int i = this.width();

        canvas.drawTexture(0, 0, i, this.height(), 0, TEXTURE);

        Matrix4f arg = poseStack.last().pose();
        Vector4f pos = new Vector4f(18, 12, 0, 0);
        pos.mul(arg);

        if (this.description == null) {
            canvas.drawText(this.title, (int)pos.x() + 17, (int)pos.y(), -256);
        } else {
            canvas.drawText(this.title, (int)pos.x() + 17, (int)pos.y() - 6, -256);
            canvas.drawText(this.description, (int)pos.x() + 17, (int)pos.y() + 6, -1);
        }
        graphics.flush();
    }

    @Override
    public int width() {
        return 180;
    }

    private static final ResourceKey TEXTURE = new ResourceKey("cosmetica", "textures/toast.png");
}
