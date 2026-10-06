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

package cc.cosmetica.cosmetica.forge;

import cc.cosmetica.cosmetica.Keybinds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.ModList;

/**
 * Forge implementation of {@link cc.cosmetica.cosmetica.CosmeticaExpectPlatform}.
 */
public class CosmeticaExpectPlatformImpl {
    public static boolean isModLoaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    public static KeyMapping registerSpecial(InputConstants.Key defaultKey, String id) {
        // forge already handles deconflicting for us
        KeyMapping mapping = new KeyMapping(
                "key.cosmetica." + id,
                defaultKey.getType(),
                defaultKey.getValue(),
                Keybinds.COSMETICA_CATEGORY
        );

        return mapping;
    }
}
