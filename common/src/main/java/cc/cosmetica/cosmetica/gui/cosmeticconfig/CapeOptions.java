package cc.cosmetica.cosmetica.gui.cosmeticconfig;

public final class CapeOptions implements CosmeticOptions {
    public CapeOptions(boolean cloak, boolean elytra) {
        this.cloak = cloak;
        this.elytra = elytra;
    }

    private final boolean cloak;
    private final boolean elytra;

    public boolean isCloak() {
        return cloak;
    }

    public boolean isElytra() {
        return elytra;
    }
}
