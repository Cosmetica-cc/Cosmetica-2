package cc.cosmetica.cosmetica.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ControlsScreen.class)
public class MixinControlsscreen {
    @Inject(at = @At("HEAD"), method = "keyPressed")
    private void onKeyPress(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("Key pressed: " + i + "/" + j + " (" + InputConstants.getKey(i, j).getName() + ")");
    }
}
