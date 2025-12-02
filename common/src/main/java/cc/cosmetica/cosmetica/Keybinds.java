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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.HomeScreen;
import cc.cosmetica.cosmetica.gui.OutfitWheelScreen;
import cc.cosmetica.cosmetica.gui.SnipeScreen;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.cosmetica.util.Sniper;
import cc.cosmetica.kupe.api.Screens;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    // marks whether the current menu was opened by customise key (default right shift)
    // persistent state by processKeybinds
    private static boolean rightShiftMenu = false;
    /**
     * Process the Cosmetica Keybinds on client (world) tick.
     */
    public static void processKeybinds() {
        Screen screen = Minecraft.getInstance().screen;

        // Outfit Wheel
        boolean set = false;
        while (Keybinds.SELECT_OUTFIT.consumeClick())
            set = true;

        if (set) {
            if (screen == null) {
                Minecraft.getInstance().setScreen(new OutfitWheelScreen());
            } else if (Setting.TOGGLE_OUTFIT_WHEEL.get() && screen instanceof OutfitWheelScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        }

        // Right Shift
        set = false;
        while (Keybinds.CUSTOMISE.consumeClick()) {
            set = true;
        }

//        System.out.println(Keybinds.CUSTOMISE.isDown());
//        if(set)System.out.println("set");
        // closing the in-game cosmetica menu brings you back to gameplay
        if (screen == null) rightShiftMenu = false;

        if (Keybinds.CUSTOMISE.isDown()) {
            Logging.getInstance().debug(CosmeticaLogCategory.KEYBINDS, "rsm = " + rightShiftMenu + ", consumed click = " + set);
        }


        if (set) {
            if (screen == null) {
                rightShiftMenu = true;
                Screens.setScreen(HomeScreen.ID);
            } else if (rightShiftMenu) {
                // to-do: make cosmetica menu screens allow right shift, but not other keys
                Minecraft.getInstance().setScreen(null);
            }
        }

        // Snipe
        set = false;
        while (Keybinds.SNIPE.consumeClick())
            set = true;

        if (set && screen == null) {
            LivingEntity entity = Sniper.getTarget();

            // Could implement StateHolder base case on LE and replace with entity!=null
            if (entity instanceof StateHolder && (entity instanceof Player || Cosmetics.getCosmetics(entity).isPresent())) {
                Screens.setScreen(new SnipeScreen(entity), SnipeScreen.ID);
            }
        }
    }
}
