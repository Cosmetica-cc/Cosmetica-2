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

import cc.cosmetica.cosmetica.Keybinds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Ensure cosmetica keybinds don't conflict with other keybinds.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {
    @Shadow
    @Final
    private static Map<String, Integer> CATEGORY_SORT_ORDER;

    @Inject(at = @At("RETURN"), method = "click")
    private static void onClick(InputConstants.Key key, CallbackInfo ci) {
        Keybinds.COSMETICA_MAP.(key);
    }

    @Inject(at = @At("RETURN"), method = "set")
    private static void onSet(InputConstants.Key key, boolean bl, CallbackInfo ci) {
        Keybinds.set(key, bl);
    }

    @Inject(at = @At("HEAD"), method = "resetMapping")
    private static void beforeReset(CallbackInfo ci) {
        Keybinds.clearMappings();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), method = "resetMapping")
    private static Object set(Map map, Object key, Object keyMapping) {
        return (key instanceof InputConstants.Key && keyMapping instanceof SpecialKeyMapping) ? SpecialKeyMapping.putMapping((InputConstants.Key) key, (SpecialKeyMapping) keyMapping) : map.put(key, keyMapping);
    }

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void onClInit(CallbackInfo ci) {
        // To put cosmetica above misc
        //CATEGORY_SORT_ORDER.put(CosmeticaKeybinds.COSMETICA_CATEGORY, CATEGORY_SORT_ORDER.size());
        //CATEGORY_SORT_ORDER.replace(CATEGORY_MISC, CATEGORY_SORT_ORDER.size());

        // To put cosmetica below misc
        CATEGORY_SORT_ORDER.put(Keybinds.COSMETICA_CATEGORY, CATEGORY_SORT_ORDER.size() + 1);
    }
}
