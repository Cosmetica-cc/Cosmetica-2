package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.gui.OutfitWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class ClientWorldTickMixin {
    @Inject(at = @At("RETURN"), method = "renderLevel")
    private void onRenderLevel(CallbackInfo info) {
        if (Minecraft.getInstance().screen == null) {
            boolean set = false;
            while (Keybinds.SELECT_OUTFIT.consumeClick())
                set = true;

            if (set) {
                Minecraft.getInstance().setScreen(new OutfitWheelScreen());
            }
        }
    }
}
