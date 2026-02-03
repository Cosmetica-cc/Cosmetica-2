package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;

public class CosmeticaToast implements Toast {
    public CosmeticaToast(Text text) {
        this.text = text;
        this.changed = true;
    }

    private final Text text;
    private boolean changed;
    private long lastChanged;

    @Override
    public Visibility render(PoseStack poseStack, ToastComponent toastComponent, long l) {
        if (this.changed) {
            this.changed = false;
            this.lastChanged = l;
        }

        Canvas canvas = new PoseCanvas(poseStack, toastComponent.getMinecraft(), null, 0);
        canvas.setTexture(new ResourceKey(TEXTURE));

        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        int i = this.width();
        int j = 12;

        toastComponent.blit(poseStack, 0, 0, 0, 64, i, this.height());

        Matrix4f arg = poseStack.last().pose();
        Vector4f pos = new Vector4f(18, 12, 0, 0);
        pos.transform(arg);

        canvas.drawText(this.text, (int)pos.x(), (int)pos.y(), -256);

        return l - this.lastChanged < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
}
