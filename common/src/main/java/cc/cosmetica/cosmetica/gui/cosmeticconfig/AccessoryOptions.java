package cc.cosmetica.cosmetica.gui.cosmeticconfig;

public final class AccessoryOptions implements CosmeticOptions {
    public AccessoryOptions(double[] x, double[] y, double[] z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private final double[] x;
    private final double[] y;
    private final double[] z;

    public double[] getXRange() {
        return this.x;
    }

    public double[] getYRange() {
        return this.y;
    }

    public double[] getZRange() {
        return this.z;
    }
}
