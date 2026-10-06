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

package cc.cosmetica.cosmetica.fabric.mixin;

import cc.cosmetica.cosmetica.Behaviour;
import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.fabric.CosmeticaFabricKeyDuck;
import cc.cosmetica.cosmetica.fabric.FabricKeybinds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(KeyMapping.class)
public class KeyMappingMixinFabric implements CosmeticaFabricKeyDuck {
    @Shadow
    @Final
    private static Map<String, KeyMapping> ALL;
    @Shadow
    private InputConstants.Key key;

    @Unique
    private static Map<String, KeyMapping> cosmetica$buffer = new HashMap<>();

    @Override
    public InputConstants.Key cosmetica$getKey() {
        return this.key;
    }

    @Inject(at = @At("RETURN"), method = "set")
    private static void onSet(InputConstants.Key key, boolean bl, CallbackInfo ci) {
        @Nullable KeyMapping k = FabricKeybinds.SPECIAL_MAP.get(key);
        if (k != null) {
            k.setDown(bl);
        }
    }

    @Inject(at = @At("RETURN"), method = "click")
    private static void onClick(InputConstants.Key key, CallbackInfo ci) {
        @Nullable KeyMapping k = FabricKeybinds.SPECIAL_MAP.get(key);
        if (k != null) {
            ((Behaviour)k).cosmetica$invoke();
        }
    }

    /**
     * This version is compatible with other mods that modify the same code.
     */
    @Inject(at = @At("HEAD"), method = "resetMapping")
    private static void beforeResetMapping(CallbackInfo ci) {
        // Remove ALL before doing vanilla/modded action
        ALL.forEach((mapping, keyMapping) -> {
            if (keyMapping == Keybinds.SNIPE) {
                cosmetica$buffer.put(mapping, keyMapping);
            }
        });

        cosmetica$buffer.forEach((key, keyMapping) -> {
            ALL.remove(key);
        });

        FabricKeybinds.SPECIAL_MAP.clear();

        // Do process
        for(KeyMapping keyMapping : cosmetica$buffer.values()) {
            FabricKeybinds.SPECIAL_MAP.put(((CosmeticaFabricKeyDuck)keyMapping).cosmetica$getKey(), keyMapping);
        }
    }

    @Inject(at = @At("RETURN"), method = "resetMapping")
    private static void afterResetMapping(CallbackInfo ci) {
        // Afterwards, add back to ALL
        ALL.putAll(cosmetica$buffer);
    }
}
