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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.jetbrains.annotations.Nullable;

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
    public Visibility render(PoseStack poseStack, ToastComponent toastComponent, long l) {
        if (this.changed) {
            this.changed = false;
            this.lastChanged = l;
        }

        Canvas canvas = new PoseCanvas(poseStack, toastComponent.getMinecraft(), null, 0);
        canvas.setTexture(TEXTURE);

        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        int i = this.width();
        int j = 12;

        GuiComponent.blit(poseStack, 0, 0, i, this.height(), 0, 0, 360, 64, 360, 64);

        Matrix4f arg = poseStack.last().pose();
        Vector4f pos = new Vector4f(18, 12, 0, 0);
        pos.transform(arg);

        if (this.description == null) {
            canvas.drawText(this.title, (int)pos.x() + 17, (int)pos.y(), -256);
        } else {
            canvas.drawText(this.title, (int)pos.x() + 17, (int)pos.y() - 6, -256);
            canvas.drawText(this.description, (int)pos.x() + 17, (int)pos.y() + 6, -1);
        }

        return l - this.lastChanged < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public int width() {
        return 180;
    }

    private static final ResourceKey TEXTURE = new ResourceKey("cosmetica", "textures/toast.png");
}
