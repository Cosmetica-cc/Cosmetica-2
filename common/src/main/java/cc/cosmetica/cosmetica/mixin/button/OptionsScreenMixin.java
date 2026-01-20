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
import cc.cosmetica.cosmetica.gui.HomeScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the cosmetica button.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	protected OptionsScreenMixin(Component component) {
		super(component);
	}

	@Inject(at=@At("RETURN"), method="init")
	private void onInit(CallbackInfo info) {
		for (GuiEventListener element: this.children()) {
			if (element instanceof AbstractWidget) {
				Component message = ((AbstractWidget)element).getMessage();

				if (message.getContents() instanceof TranslatableContents tc) {
					if (tc.getKey().equals("options.skinCustomisation")) {
						this.removeWidget(element);

						this.addRenderableWidget(
								Button.builder(Text.translatable("button.cosmetica.home").toMinecraftComponent(), button -> Screens.setScreen(HomeScreen.ID))
										.pos(this.width / 2 - 155, this.height / 6 + 48 - 6)
										.size(150, 20)
										.build()
						);
						return;
					}
				}
			}
		}

		Logging.getInstance().warn("Failed to find skin customisation button. Unable to replace with Cosmetica button.");
	}
}
