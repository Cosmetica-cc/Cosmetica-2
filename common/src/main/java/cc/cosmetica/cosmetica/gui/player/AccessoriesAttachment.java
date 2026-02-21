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

package cc.cosmetica.cosmetica.gui.player;

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.mixin.PlayerModelAccessor;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import cc.cosmetica.kupe.impl.KupeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public class AccessoriesAttachment implements GUIPlayer.Attachment<Collection<Accessory>> {
    private AccessoriesAttachment() {
    }

    @Override
    public void render(GUIPlayer component, PlayerModel playerModel, GUIPlayer.Posture posture, Canvas canvas, Collection<Accessory> configuration, Quaternionf cameraOrientation, MultiBufferSource bufferSource, int packedLight) {
        boolean elytra = false;
        boolean cloak = false;
        for (Iterator<GUIPlayer.Attachment<?>> attachments = component.getRenderingAttachments();
             attachments.hasNext(); ) {
            GUIPlayer.Attachment<?> attachment = attachments.next();

            if (attachment == GUIPlayer.ELYTRA) {
                elytra = true;
            }
            if (attachment == GUIPlayer.CAPE) {
                cloak = true;
            }
        }
        GUIPlayer.CapeProperties cape = component.getConfiguration(GUIPlayer.CAPE);

        for (Accessory accessory : configuration) {
            if (Minecraft.getInstance().screen instanceof KupeScreen) {
                if (cloak && cape != null && cape.getTexture().isPresent() && accessory.getFlags().contains(Accessory.Flag.HIDE_WITH_CLOAK)) {
                    continue;
                }
                if (elytra && accessory.getFlags().contains(Accessory.Flag.HIDE_WITH_ELYTRA)) {
                    continue;
                }
            }

            ModelPart part = null;

            // additional shifting for slim/thick arms
            float additionalXOffset = 0;

            switch (accessory.getAttachment()) {
                case HEAD:
                    part = playerModel.head;
                    break;
                case BODY:
                    part = playerModel.body;
                    break;
                case LEFT_ARM:
                    part = accessory.isMirrored() ?
                            playerModel.rightArm :
                            playerModel.leftArm;

                    // thin skin: shift
                    if (((PlayerModelAccessor) playerModel).isSlim()) {
                        additionalXOffset += 0.5f / 16.0f;
                    }
                    break;
                case RIGHT_ARM:
                    part = accessory.isMirrored() ?
                            playerModel.leftArm :
                            playerModel.rightArm;

                    // thin skin: shift
                    if (((PlayerModelAccessor) playerModel).isSlim()) {
                        additionalXOffset += 0.5f / 16.0f;
                    }
                    break;
                case LEFT_LEG:
                    part = accessory.isMirrored() ?
                            playerModel.rightLeg :
                            playerModel.leftLeg;
                    break;
                case RIGHT_LEG:
                    part = accessory.isMirrored() ?
                            playerModel.leftLeg :
                            playerModel.rightLeg;
                    break;
                case UNKNOWN_DEFAULT_OPEN_API:
                    Logging.getInstance().warnOnce(
                            "attachment_unknown_accessory_gui",
                            "Unknown attachment for accessory (GUI player): {}",
                            accessory.getName());
                    continue;
            }

            Vec3 offset = accessory.getOffset();

            if (part.visible) {
                accessory.getModel().renderOnPart(
                        part, canvas.getStack().getMinecraftStack(), bufferSource, packedLight,
                        (float) offset.x + additionalXOffset, (float) offset.y, (float) offset.z,
                        accessory.isMirrored()
                );
            }
        }
    }

    @Override
    public Collection<Accessory> getDynamicConfiguration(UUID uuid) {
        Optional<Cosmetics> cosmetics = CosmeticaCapeProvider.getCosmetics(uuid);

        return cosmetics.map(Cosmetics::getAccessories).orElse(null);
    }

    /**
     * Global instance of Accessory Attachment.
     */
    public static final AccessoriesAttachment INSTANCE = new AccessoriesAttachment();
}
