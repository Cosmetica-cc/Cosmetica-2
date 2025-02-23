package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.gui.Image;
import cc.cosmetica.kupe.api.maths.Region;

public class Icon extends Image {
    public Icon(ResourceKey texture) {
        super(texture);
    }

    @Override
    public void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        canvas.setTransparency(1.0f);
        super.paint(canvas, region, mouseX, mouseY);
        canvas.disableTransparency();
    }
}
