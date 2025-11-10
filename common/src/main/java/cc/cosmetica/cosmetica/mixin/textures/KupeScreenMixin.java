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

package cc.cosmetica.cosmetica.mixin.textures;

import cc.cosmetica.kupe.impl.KupeScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/**
 * On cosmetica screens animate textures even when not in game (Minecraft saves effort by only animating textures in game).
 * All relevant cosmetica screens use Kupe gui lib and are additionally
 * marked with {@link cc.cosmetica.cosmetica.gui.AnimatedTextureScreen}.
 */
@Mixin(value = KupeScreen.class, remap = false)
public abstract class KupeScreenMixin extends Screen {
    protected KupeScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void tick() {
        super.tick();
        assert this.minecraft != null;
        if (this.minecraft.level == null) {
            this.minecraft.getProfiler().push("textures");
            this.minecraft.getTextureManager().tick();
            this.minecraft.getProfiler().pop();
        }
    }
}
