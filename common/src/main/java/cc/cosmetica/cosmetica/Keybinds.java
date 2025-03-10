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
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class Keybinds {
    public static final String COSMETICA_CATEGORY = "key.categories.cosmetica";
    public static final Map<InputConstants.Key, KeyMapping> SPECIAL_MAP = new HashMap<>();

    public static KeyMapping CUSTOMISE = new KeyMapping(
                    "key.cosmetica.customise",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    COSMETICA_CATEGORY
    );

    public static KeyMapping SNIPE = registerSpecial(
            InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE),
            "snipe"
    );

    public static KeyMapping SELECT_OUTFIT = new KeyMapping(
            "key.cosmetica.select_outfit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            COSMETICA_CATEGORY
    );

    /**
     * Register a special key mapping that is placed on a different keybind map. This prevents it
     * from conflicting with other keybinds on that key.
     * @param defaultKey the key.
     * @param id the key's id.
     * @return the key mapping.
     */
    private static KeyMapping registerSpecial(InputConstants.Key defaultKey, String id) {
        KeyMapping mapping = new KeyMapping(
                "key.cosmetica." + id,
                // register it to unknown on the original map
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                COSMETICA_CATEGORY
        );

        SPECIAL_MAP.put(defaultKey, mapping);
        mapping.setKey(defaultKey);
        return mapping;
    }
}
