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
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix right shift bug on some IME keyboards.
 * This would have been out of scope for Cosmetica, but right shift is a default keybind. We would rather people using
 * keyboards affected by this bug are able to use the mod.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow protected abstract void keyPress(long l, @KeyEvent.Action int j, KeyEvent arg);

    @Inject(at = @At("HEAD"), method = "keyPress", cancellable = true)
    private void onKeyPress(long window, int k, KeyEvent event, CallbackInfo ci) {
        if (event.key() == -1 && event.scancode() == 310) {
            this.keyPress(window, k, new KeyEvent(344, 54, event.modifiers()));
            ci.cancel();
        }
    }
}
