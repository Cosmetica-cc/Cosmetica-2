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
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
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

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long l) {
        if (this.changed) {
            this.changed = false;
            this.lastChanged = l;
        }

        PoseStack poseStack = graphics.pose();

        Canvas canvas = new PoseCanvas(graphics, toastComponent.getMinecraft(), null, 0);

        int i = this.width();

        graphics.blit(TEXTURE, 0, 0, 0, 64, i, this.height());

        Matrix4f arg = poseStack.last().pose();
        Vector4f pos = new Vector4f(18, 12, 0, 0);
        pos.mul(arg);

        if (this.description == null) {
            canvas.drawText(this.title, (int)pos.x(), (int)pos.y(), -256);
        } else {
            canvas.drawText(this.title, (int)pos.x(), (int)pos.y() - 6, -256);
            canvas.drawText(this.description, (int)pos.x(), (int)pos.y() + 6, -1);
        }

        return l - this.lastChanged < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
}
