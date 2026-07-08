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
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.kupe.api.gui.GUIPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NametagConfigAttachment implements GUIPlayer.Attachment<@NotNull NametagConfig> {
    public NametagConfigAttachment(
            BiConsumer<AccessoriesAttachment.MutableCosmetics, NametagConfig> attachFn,
            Function<@NotNull Cosmetics, @Nullable NametagConfig> fieldFn) {
        this.attachFn = attachFn;
        this.fieldFn = fieldFn;
    }

    private final BiConsumer<AccessoriesAttachment.MutableCosmetics, NametagConfig> attachFn;
    private final Function<Cosmetics, NametagConfig> fieldFn;

    @Override
    public void submitToRenderState(GUIPlayer player, @NotNull NametagConfig nametagConfig, Quaternionf quaternionf, AvatarRenderState renderState) {
        AccessoriesAttachment.MutableCosmetics mc = AccessoriesAttachment.MutableCosmetics.getOrCreate(renderState);
        this.attachFn.accept(mc, nametagConfig);
    }

    @Override
    public @NotNull NametagConfig getDynamicConfiguration(UUID uuid) {
        Optional<Cosmetics> cosmetics = CosmeticaCapeProvider.getCosmetics(uuid);
        return cosmetics.map(this.fieldFn).orElse(NametagConfig.EMPTY);
    }

    public static final NametagConfigAttachment ICON = new NametagConfigAttachment(AccessoriesAttachment.MutableCosmetics::setNametagConfig, Cosmetics::getNametag);
    public static final NametagConfigAttachment LORE = new NametagConfigAttachment(AccessoriesAttachment.MutableCosmetics::setLore, c -> c.getLore().orElse(null));
}
