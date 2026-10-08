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

package cc.cosmetica.cosmetica;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;

/**
 * Adapter for methods for cosmetica.
 */
public class CosmeticaExpectPlatform {
    @ExpectPlatform
    public static boolean isModLoaded(String mod) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getVersion() {
        throw new AssertionError();
    }

    /**
     * Register a special key mapping that is placed on a different keybind map. This prevents it
     * from conflicting with other keybinds on that key.
     * @param defaultKey the key.
     * @param id the key's id.
     * @return the key mapping.
     */
    @ExpectPlatform
    public static KeyMapping registerSpecial(InputConstants.Key defaultKey, String id) {
        throw new AssertionError();
    }
}
