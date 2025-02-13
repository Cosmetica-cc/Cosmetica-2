/*
 * Copyright 2024 Cosmetica
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
import cc.cosmetica.kupe.api.State;
import org.jetbrains.annotations.Nullable;

/**
 * Cast a {@linkplain net.minecraft.world.entity.player.Player player} to this to access a hookable cosmetics state.
 */
public interface StateHolder {
    /**
     * Get a state containing the current cosmetics.
     */
    State<@Nullable Cosmetics> cosmetica$getCosmeticState();

    /**
     * Update the cosmetics state on this entity.
     * @param cosmetics the cosmetics to update with.
     */
    void cosmetica$setCosmeticState(@Nullable Cosmetics cosmetics);
}
