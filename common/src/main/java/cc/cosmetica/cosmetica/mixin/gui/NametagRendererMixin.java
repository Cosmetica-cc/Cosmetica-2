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

package cc.cosmetica.cosmetica.mixin.gui;

import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.NametagRenderer;
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import cc.cosmetica.cosmetica.util.NametagUtil;
import cc.cosmetica.kupe.impl.fakeplayer.GuiPlayerAvatarRenderState;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NametagRenderer.class)
public class NametagRendererMixin {
    @Inject(at = @At("RETURN"), method = "shiftNametags", cancellable = true)
    private static void onShiftNametags(
            AvatarRenderState state,
            PlayerModel model,
            Vec3 position,
            HumanoidAccessoriesLayer.ArmourEquipper equipper,
            boolean elytra,
            CallbackInfoReturnable<Vec3> returnable) {
        if (state instanceof GuiPlayerAvatarRenderState) {
            Vec3 returnValue = returnable.getReturnValue();
            double yShift = returnValue.y - position.y;

            returnable.setReturnValue(new Vec3(
                    returnValue.x,
                    position.y + Math.min(yShift, NametagUtil.nametagShiftCap()),
                    returnValue.z
            ));
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0),
    method = "submitLore")
    private static void test(AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState arg4, CallbackInfo info, @Local NametagConfig lore) {
        System.out.println("ATTACHMENT capture " + state.nameTagAttachment);
        System.out.println("LORE capture " + lore.getPrefix());
        System.out.println("LIGHT capture " + state.lightCoords);
        System.out.println("CAMERA capture at " + arg4.pos + " fd " + arg4.depthFar + " smartCull " + arg4.smartCull);
        System.out.println("MATRIX capture at " + stack.last().pose());
    }
}
