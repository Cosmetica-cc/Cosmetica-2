package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Label;
import cc.cosmetica.kupe.api.gui.Tooltip;
import gg.cloaks.javaclient.ApiException;
import net.minecraft.client.Minecraft;

public final class ConfirmRemoveOutfitScreen extends AbstractConfirmScreen {
    public ConfirmRemoveOutfitScreen(String outfitId, String outfitName) {
        super(Text.translatable("screens.cosmetica.confirmDeletion"));
        this.outfitId = outfitId;
        this.outfitName = outfitName;
    }

    private final String outfitId;
    private final String outfitName;

    @Override
    protected Label createConfirmLabel() {
        return new Label(Text.translatable("label.cosmetica.confirmDelete", outfitName));
    }

    @Override
    protected Tooltip getUpdatingTooltip() {
        return new Tooltip(Text.translatable("tooltip.cosmetica.updatingOutfit"));
    }

    @Override
    protected void onConfirm() {
        this.setting.set(true);
        CosmeticaAPI.performAsync(api -> {
                    api.outfitsControllerDelete(outfitId);
                    return (Void)null;
                })
                .thenAcceptAsync(none -> Screens.closeCurrentScreen(), Minecraft.getInstance())
                .exceptionally(err -> {
                    if (err instanceof ApiException) {
                        int code = ((ApiException) err).getCode();
                        // outfit doesn't exist
                        if (code == 404) {
                            Minecraft.getInstance().execute(Screens::closeCurrentScreen);
                        }
                        // no auth
                        // else if (code == 401)
                        // default: error
                        else {
                            Logging.getInstance().error("Error deleting outfit", code);
                            Minecraft.getInstance().execute(() -> setting.set(false));
                        }
                    }
                });
    }
}
