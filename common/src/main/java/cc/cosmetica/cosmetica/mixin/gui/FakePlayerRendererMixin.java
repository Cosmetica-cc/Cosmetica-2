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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.impl.NametagRenderer;
import cc.cosmetica.cosmetica.gui.widget.RotatableGUIPlayer;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.Context;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import cc.cosmetica.kupe.impl.fakeplayer.FakePlayerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FakePlayerRenderer.class, remap = false)
public class FakePlayerRendererMixin {
    @Shadow public List<GUIPlayer.Nametag> nametags;

    @Unique private @Nullable CachedImage cosmetica$icon0 = null;
    @Unique private @Nullable CachedImage cosmetica$icon1 = null;

    @Inject(at = @At("HEAD"), method = "drawLivingEntity")
    private void onDrawLiving(GUIPlayer player, Context context, float rotation, float delta, PoseStack stack,
                              MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (player instanceof RotatableGUIPlayer) {
            // don't need to reset to null provided one renderer per gui player.
            this.cosmetica$icon0 = ((RotatableGUIPlayer)player).icon;
            this.cosmetica$icon1 = ((RotatableGUIPlayer)player).loreIcon;
        }
    }

    @Inject(at = @At("HEAD"), method = "renderNametag")
    private void onRenderNametag(GUIPlayer.Nametag nametag, Canvas canvas, MultiBufferSource bufferSource,
                                 int packedLight, CallbackInfo ci) {
        if (nametag == this.nametags.get(0)) {
            if (this.cosmetica$icon0 != null) {
                NametagRenderer.prepareIcon(this.cosmetica$icon0, 2, true);
            }
        } else if (nametag == this.nametags.get(1)) {
            if (this.cosmetica$icon1 != null) {
                NametagRenderer.prepareIcon(this.cosmetica$icon1, 2, true);
            }
        }
    }
}
