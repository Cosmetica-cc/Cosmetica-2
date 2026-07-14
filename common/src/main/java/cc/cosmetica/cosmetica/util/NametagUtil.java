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

import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final class GuiPlayerEquipper implements HumanoidAccessoriesLayer.ArmourEquipper {
        public GuiPlayerEquipper(HumanoidAccessoriesLayer.ArmourEquipper parent, boolean elytra) {
            this.elytra = elytra;
            this.parent = parent;
        }

        private final boolean elytra;
        private final HumanoidAccessoriesLayer.ArmourEquipper parent;

        @Override
        public boolean hasItemInSlot(EquipmentSlot equipmentSlot) {
            return (equipmentSlot == EquipmentSlot.CHEST && elytra) || this.parent.hasItemInSlot(equipmentSlot);
        }

        @Override
        public Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> getLayers(EquipmentSlot equipmentSlot) {
            var map = this.parent.getLayers(equipmentSlot);
            if (equipmentSlot == EquipmentSlot.CHEST && elytra) {
                map = new HashMap<>(map);
                map.computeIfAbsent(EquipmentClientInfo.LayerType.WINGS, _ -> new ArrayList<>())
                        .add(null); // make size > 0 to trick it
            }
            return map;
        }

        @Override
        public boolean hasLeftShoulderEntity() {
            return this.parent.hasLeftShoulderEntity();
        }

        @Override
        public boolean hasRightShoulderEntity() {
            return this.parent.hasRightShoulderEntity();
        }
    }
}
