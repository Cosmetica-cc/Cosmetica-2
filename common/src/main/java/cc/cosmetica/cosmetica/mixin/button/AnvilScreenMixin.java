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

package cc.cosmetica.cosmetica.mixin.button;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.OutfitSelectAnvilScreen;
import cc.cosmetica.cosmetica.settings.CosmeticaSettings;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.impl.PoseCanvas;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> implements OutfitSelectAnvilScreen.NametaggableAnvil {
    @Shadow
    private EditBox name;

    public AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component component, ResourceLocation resourceLocation) {
        super(menu, inventory, component, resourceLocation);
    }

    @Unique
    private @Nullable String cosmetica$newName = null;
    @Unique
    private Button cosmetica$button;

    @Inject(at = @At("RETURN"), method = "subInit")
    private void onSubInit(CallbackInfo ci) {
        int originX = (this.width - this.imageWidth) / 2;
        int originY = (this.height - this.imageHeight) / 2;

        this.cosmetica$button = this.addButton(new Button(
                originX - 27,
                originY + 46,
                20,
                20,
                Text.literal("").toMinecraftComponent(),
                bn -> Screens.setScreen(new OutfitSelectAnvilScreen(this), OutfitSelectAnvilScreen.ID.translationKey("screens"))
        )
        {
            @Override
            public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTick) {
                if (this.visible) {
                    super.renderButton(stack, mouseX, mouseY, partialTick);
                    Canvas canvas = new PoseCanvas(stack, Minecraft.getInstance(), null, partialTick);
                    canvas.drawTexture(this.x, this.y, this.width, this.height, 0, new ResourceKey("cosmetica", "textures/button/lore.png"));
                }
            }
        });

        this.cosmetica$button.visible = false;
    }

    @Inject(at = @At("RETURN"), method = "slotChanged")
    private void onSlotChanged(AbstractContainerMenu menu, int i, ItemStack itemStack, CallbackInfo ci) {
        if (i == 0 && this.cosmetica$newName != null) {
            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "New name: {}", this.cosmetica$newName);
            this.name.setValue(this.cosmetica$newName);
            this.cosmetica$newName = null;
        }

        if (i == 0 && CosmeticaSettings.SHOW_ANVIL_BUTTON.peek()) {
            this.cosmetica$button.visible = itemStack.getItem() == Items.NAME_TAG;
        }
    }

    @Override
    public void cosmetica$setNewName(String name) {
        this.cosmetica$newName = name;
    }
}
