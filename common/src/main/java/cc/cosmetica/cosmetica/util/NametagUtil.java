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
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Iterator;

public class NametagUtil {
    public static int extraSpaceTaken = 69;
    public static boolean isSnipe = false;

    public static double nametagShiftCap() {
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        int outfitPlayerHeightApprox = 2 * (isSnipe ?
                (int)(width * (12.0 / 100)) :
                Math.min(90, Math.max(50, 10 + (int)(width * 0.0625)))
        );

        int remainingSpace = (height - (outfitPlayerHeightApprox + extraSpaceTaken))/2;

        // avoid nametags going off the screen in the GUI

        // 40 => 0.275
        // (50 => 0.5) - unused
        // 65 => 0.74

        final double m = (0.74 - 0.275) / (65 - 40);
        final double c = 0.275 - m * 40;

        return Math.max(0.275, m * remainingSpace + c);
    }

    private static final class GuiPlayerEquipper implements HumanoidAccessoriesLayer.ArmourEquipper {
        public GuiPlayerEquipper(boolean elytra) {
            this.elytra = elytra;
        }

        private final boolean elytra;

        @Override
        public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
            if (equipmentSlot != EquipmentSlot.CHEST) {
                return ItemStack.EMPTY;
            }

            return this.elytra ? new ItemStack(Items.ELYTRA) : ItemStack.EMPTY;
        }

        @Override
        public boolean hasLeftShoulderEntity() {
            return false;
        }

        @Override
        public boolean hasRightShoulderEntity() {
            return false;
        }
    }
}
