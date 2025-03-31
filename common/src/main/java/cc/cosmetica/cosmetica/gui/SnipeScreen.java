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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.cosmetica.StateHolder;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Shows the cosmetics of an inspected cosmetics holder.
 * Unregistered. Pass ID as second parameter when setting kupe screen.
 */
public class SnipeScreen extends Screen {
    public static final ResourceKey ID = new ResourceKey("cosmetica", "snipe");

    public SnipeScreen(LivingEntity entity) {
        super(entity instanceof Player ?
                Text.literal(entity.getDisplayName().getString()) :
                Text.literal(Cosmetics.getCosmetics(entity).flatMap(Cosmetics::getOutfitName).orElse("Outfit"))
        );

        // TODO armour stands dont have a cosmetic state currently. we subscribe to automatic outfit updates, so this should be done?
        this.cosmetics = ((StateHolder)entity).cosmetica$getCosmeticState();
    }

    private final State<Cosmetics> cosmetics;

    @Override
    protected Component[] buildScreen() {
        Cosmetics outfit = this.cosmetics.acquire(this);

        return new Component[0];
    }
}
