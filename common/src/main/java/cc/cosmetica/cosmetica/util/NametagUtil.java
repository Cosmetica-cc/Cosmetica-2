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

package cc.cosmetica.cosmetica.util;

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Collection;

public class NametagUtil {

    public static void shiftNametags(PoseStack stack, GUIPlayer player, int nametags) {
        // shift nametags up
        if (!player.pose.upsideDown) {
            float hatTopY = 0;

            Collection<Accessory> accessories = player.getConfiguration(AccessoriesAttachment.INSTANCE);
            if (accessories == null) {
                return;
            }

            for (Accessory accessory : accessories) {
                if (accessory.getAttachment() == gg.cloaks.javaclient.model.Accessory.AttachmentEnum.HEAD) {
//                    if (!accessory.getFlags().contains(Accessory.Flag.HIDE_WITH_HELMET) || !wearingHelmet) {
                    hatTopY = Math.max(hatTopY, (float) accessory.getModel().getBoundingBox().maxY);
//                    }
                }
            }

            if (hatTopY > 0) {
                float normalizedAngleMultiplier = (float) -(Math.abs(Math.toRadians(player.pose.xRot)) / 1.57 - 1);
                float lookAngleMultiplier;

                if (player.pose.sneaking) { // Gliding with elytra, swimming, or crouching
                    lookAngleMultiplier = 0;
                } else {
                    lookAngleMultiplier = normalizedAngleMultiplier;
                }

                double shift = Math.max(hatTopY * lookAngleMultiplier, 0) / 16.0;

                // use the same shift always to give the user feedback as to how the shift works when lore is equipped
                double cap = 0.275; // avoid nametags going off the screen in the GUI
                // nametags > 1 ? 0.25 : 0.35

                stack.translate(0, Math.min(shift, cap), 0);
            }
        }
    }
}
