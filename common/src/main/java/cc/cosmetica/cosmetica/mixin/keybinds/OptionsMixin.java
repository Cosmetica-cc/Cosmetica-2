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

package cc.cosmetica.cosmetica.mixin.keybinds;

import cc.cosmetica.cosmetica.Keybinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add Cosmetica keybinds.
 */
@Mixin(Options.class)
public class OptionsMixin {
    @Shadow @Final @Mutable
    public KeyMapping[] keyMappings;

    @Unique
    private boolean cosmetica$modified = false;

    @Inject(at = @At("HEAD"), method = "load")
    private void onLoad(CallbackInfo ci) {
        // only run once!
        if (!this.cosmetica$modified) {
            this.cosmetica$modified = true;

            KeyMapping[] cosmeticaMappings = {
                    Keybinds.CUSTOMISE,
                    Keybinds.SNIPE,
                    Keybinds.SELECT_OUTFIT
            };

            KeyMapping[] newKeyMappings = new KeyMapping[this.keyMappings.length + cosmeticaMappings.length];
            System.arraycopy(keyMappings, 0, newKeyMappings, 0, keyMappings.length);
            System.arraycopy(cosmeticaMappings, 0, newKeyMappings, keyMappings.length, cosmeticaMappings.length);
            keyMappings = newKeyMappings;
        }
    }
}
