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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Cape provider for Cosmetica capes.
 */
public class CosmeticaCapeProvider implements GUIPlayer.CapeProvider {
    @Override
    public @Nullable GUIPlayer.CapeProperties getCapeTexture(UUID uuid) {
        Optional<Cosmetics> cosmetics = getCosmetics(uuid);

        if (cosmetics.isPresent()) {
            return new GUIPlayer.CapeProperties(cosmetics.get().getCloak()
                    .map(ImageCosmetic::getImage)
                    .map(c -> c.location)
                    .orElse(null));
        }

        return null;
    }

    @Override
    public @Nullable GUIPlayer.ElytraProperties getElytraTexture(UUID uuid) {
        Optional<Cosmetics> cosmetics = getCosmetics(uuid);

        if (cosmetics.isPresent()) {
            return cosmetics.get().getElytra()
                    .map(ImageCosmetic::getImage)
                    .map(c -> new GUIPlayer.ElytraProperties(c.location, false, true))
                    .orElse(GUIPlayer.ElytraProperties.DEFAULT);
        }

        return null;
    }

    /**
     * Get cosmetics for the given player uuid.
     * @param uuid the player uuid.
     * @return the player's cosmetics.
     */
    static Optional<Cosmetics> getCosmetics(UUID uuid) {
        if (Minecraft.getInstance().level != null) {
            Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                return Cosmetics.getCosmetics(player);
            }
        }

        // check if self
        if (Minecraft.getInstance().getUser().getProfileId().equals(uuid)) {
            return SelfCosmeticManager.getCosmetics();
        }

        return Optional.empty();
    }
}
