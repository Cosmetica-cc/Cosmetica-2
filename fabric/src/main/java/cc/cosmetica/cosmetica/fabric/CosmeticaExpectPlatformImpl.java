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

package cc.cosmetica.cosmetica.fabric;

import cc.cosmetica.cosmetica.Keybinds;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/**
 * Fabric implementation of {@link cc.cosmetica.cosmetica.CosmeticaExpectPlatform}.
 */
public class CosmeticaExpectPlatformImpl {
    public static boolean isModLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }

    public static KeyMapping registerSpecial(InputConstants.Key defaultKey, String id) {
        KeyMapping mapping = new KeyMapping(
                "key.cosmetica." + id,
                // register it to unknown on the original map
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                Keybinds.COSMETICA_CATEGORY
        );

        FabricKeybinds.SPECIAL_MAP.put(defaultKey, mapping);
        mapping.setKey(defaultKey);
        return mapping;
    }
}