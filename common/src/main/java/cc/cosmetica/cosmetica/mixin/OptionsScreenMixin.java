package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.CosmeticaHomeScreen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

	@Shadow
	@Final
	private Options options;

	@Inject(at=@At("RETURN"), method="init")
	private void onInit(CallbackInfo info) {
		for (GuiEventListener element: this.children) {
			if (element instanceof AbstractWidget) {
				Component message = ((AbstractWidget)element).getMessage();

				if (message instanceof TranslatableComponent) {
					if (((TranslatableComponent)message).getKey().equals("options.skinCustomisation")) {
						this.children.remove(element);
						this.buttons.remove((AbstractWidget) element);

						this.addButton(new Button(
								this.width / 2 - 155, this.height / 6 + 48 - 6,
								150, 20,
								Text.translatable("cosmetica.cosmetics").toMinecraftComponent(),
								button -> Screens.setScreen(CosmeticaHomeScreen.ID)));
						break;
					}
				}
			}
		}

		Logging.getInstance().warn("Failed to find skin customisation button. Unable to replace with Cosmetica button.");
	}
}
