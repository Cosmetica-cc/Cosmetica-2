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

package cc.cosmetica.cosmetica.mixin.attach;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.cosmetica.StateHolder;
import cc.cosmetica.kupe.api.State;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerMixin implements StateHolder {
    @Unique
    private final State<@Nullable Cosmetics> cosmetica$cosmeticsState = new State<>(null);

    @Override
    public State<@Nullable Cosmetics> cosmetica$getCosmeticState() {
        return this.cosmetica$cosmeticsState;
    }

    @Override
    public void cosmetica$setCosmeticState(@Nullable Cosmetics cosmetics) {
        this.cosmetica$cosmeticsState.set(cosmetics);
    }


}
