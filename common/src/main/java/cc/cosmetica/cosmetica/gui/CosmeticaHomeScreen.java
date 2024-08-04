package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.style.Style;
import net.minecraft.resources.ResourceLocation;

public class CosmeticaHomeScreen extends Screen {
	public CosmeticaHomeScreen() {
		super(ID);
	}

	@Override
	protected Component[] build(Style.MutableStyle rootStyle) {
		return new Component[] {
				
		};
	}

	public static final ResourceLocation ID = new ResourceLocation("cosmetica", "home");
}
