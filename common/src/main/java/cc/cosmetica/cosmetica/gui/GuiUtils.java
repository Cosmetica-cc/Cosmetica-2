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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.kupe.api.gui.Border;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Optional;

/**
 * Gui utils.
 */
public class GuiUtils {
    private GuiUtils() {}

    public static void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    // Common Colours
    /**
     * Normal grey background.
     */
    public static final int NORMAL_COLOUR = 0x858585;
    /**
     * Highlight for grey backgrounds.
     */
    public static final int HIGHLIGHT_COLOUR = 0xA1A1A1;
    /**
     * Shading for grey backgrounds.
     */
    public static final int SHADE_COLOUR = 0x595959;

    public static final Optional<Border> POPOUT_BORDER = Border.create(Border.BorderConfig.split(1, HIGHLIGHT_COLOUR, SHADE_COLOUR));
    public static final Optional<Border> POP_IN_BORDER = Border.create(Border.BorderConfig.split(1, SHADE_COLOUR, HIGHLIGHT_COLOUR));
}
