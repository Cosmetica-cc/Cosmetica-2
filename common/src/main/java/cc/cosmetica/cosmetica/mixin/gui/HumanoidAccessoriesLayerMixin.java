package cc.cosmetica.cosmetica.mixin.gui;

import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import cc.cosmetica.cosmetica.util.NametagUtil;
import cc.cosmetica.kupe.impl.fakeplayer.GuiPlayerAvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HumanoidAccessoriesLayer.class)
public class HumanoidAccessoriesLayerMixin {
    @Inject(at = @At("HEAD"), cancellable = true, method = "createArmourEquipper")
    private static void onCreateArmourEquipper(HumanoidRenderState state, EquipmentAssetManager equipmentAssets, CallbackInfoReturnable<HumanoidAccessoriesLayer.ArmourEquipper> info) {
        if (state instanceof GuiPlayerAvatarRenderState guitar) {
            HumanoidAccessoriesLayer.HumanoidRenderEquipper base = new HumanoidAccessoriesLayer.HumanoidRenderEquipper(state, equipmentAssets);
            info.setReturnValue(new NametagUtil.GuiPlayerEquipper(base, guitar.elytraProperties != null));
        }
    }
}
