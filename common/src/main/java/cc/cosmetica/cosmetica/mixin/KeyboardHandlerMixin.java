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

package cc.cosmetica.cosmetica.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix right shift bug on some IME keyboards.
 * This would have been out of scope for Cosmetica, but right shift is a default keybind. We would rather people using
 * keyboards affected by this bug are able to use the mod.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(at = @At("HEAD"), method = "keyPress", cancellable = true)
    private void onKeyPress(long window, int i, int j, int k, int m, CallbackInfo ci) {
        if (i == -1 && j == 310) {
            ((KeyboardHandler)(Object)this).keyPress(window, 344, 54, k, m);
            ci.cancel();
        }
    }
}
