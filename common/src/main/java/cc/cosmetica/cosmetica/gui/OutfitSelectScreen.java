package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Keybinds;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.maths.Region;

import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.BACKGROUND_COLOUR;

/**
 * The menu outfit select screen. For the wheel, see {@link OutfitWheelScreen}.
 */
public class OutfitSelectScreen extends Screen {
    public OutfitSelectScreen() {
        super(ID);
    }

    @Override
    protected Component[] buildScreen() {
        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.acquire(this);

        return new Component[] {
                new EntryList.Grid(

                ).withStyle(Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.empty()))
        };
    }

    @Override
    protected void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        Keybinds.processKeybinds();
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "outfit_select");
}
