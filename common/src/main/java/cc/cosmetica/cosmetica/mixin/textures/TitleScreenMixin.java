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

package cc.cosmetica.cosmetica.mixin.textures;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TODO Compat with essential: animate on title screen.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
//    @Inject(method = "tick", at = @At("HEAD"))
//    private void onTick(CallbackInfo ci) {
//        if (CosmeticaExpectPlatform.isModLoaded("essential")) { // TODO how to check on forge
//            this.minecraft.getTextureManager().tick();
//        }
//    }
}
