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
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.HasCosmeticsRenderState;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class AccessoriesAttachment implements GUIPlayer.Attachment<Collection<Accessory>> {
    private AccessoriesAttachment() {
    }

    @Override
    public void submitToRenderState(GUIPlayer guiPlayer, Collection<Accessory> accessories, Quaternionf quaternionf, AvatarRenderState renderState) {
        MutableCosmetics mc = MutableCosmetics.getOrCreate(renderState);
        mc.accessories = accessories;
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

    public static final class MutableCosmetics implements Cosmetics {
        public MutableCosmetics() {
            this.accessories = ImmutableList.of();
            this.nametagConfig = NametagConfig.EMPTY;
            this.lore = null;
        }

        public Collection<Accessory> accessories;
        private NametagConfig nametagConfig;
        private @Nullable NametagConfig lore;

        @Override
        public NametagConfig getNametag() {
            return this.nametagConfig;
        }

        @Override
        public Optional<NametagConfig> getLore() {
            return Optional.ofNullable(this.lore);
        }

        @Override
        public Collection<Accessory> getAccessories() {
            return this.accessories;
        }

        public void setNametagConfig(NametagConfig nametagConfig) {
            this.nametagConfig = nametagConfig;
        }
        public void setLore(@NotNull NametagConfig lore) {
            this.lore = lore.getPrefix().isEmpty() ? NametagConfig.EMPTY : lore;
        }
        // Useful
        public static MutableCosmetics getOrCreate(AvatarRenderState renderState) {
            HasCosmeticsRenderState state = (HasCosmeticsRenderState) renderState;
            Optional<Cosmetics> cosmetics = state.cosmeticacore$getCosmetics();

            MutableCosmetics mc;
            if (cosmetics.isPresent() && cosmetics.get() instanceof MutableCosmetics) {
                mc = (MutableCosmetics) cosmetics.get();
            } else {
                mc = new MutableCosmetics();
            }

            // TODO make this API in next cosmetica core version for newer minecraft
            state.cosmeticacore$setCosmetics(mc);
            return mc;
        }

        // Unused
        @Override
        public Optional<String> getOutfitName() {
            return Optional.empty();
        }
        @Override
        public Optional<String> getOutfitId() {
            return Optional.empty();
        }
        @Override
        public Optional<ImageCosmetic> getCloak() {
            return Optional.empty();
        }
        @Override
        public Optional<ImageCosmetic> getElytra() {
            return Optional.empty();
        }
        @Override
        public boolean isUpsideDown() {
            return false;
        }
        @Override
        public void enqueue(Runnable runnable, Runnable onError) {
            runnable.run();
        }
    }
}
