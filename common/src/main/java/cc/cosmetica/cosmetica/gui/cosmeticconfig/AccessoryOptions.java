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

package cc.cosmetica.cosmetica.gui.cosmeticconfig;

import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class AccessoryOptions extends CosmeticOptions {
    public AccessoryOptions(double[] x, double[] y, double[] z,
                            boolean hideWithHelmet, boolean hideWithChestplate, boolean hideWithLeggings, boolean hideWithBoots,
                            boolean hideWithCloak, boolean hideWithElytra, boolean hideWithParrot) {
        this.x = new Range(x);
        this.y = new Range(y);
        this.z = new Range(z);

        this.hideWithHelmet = new VisibilityOption(
                "button.configureCosmetic.hide_with_helmet",
                0x1,
                hideWithHelmet
        );
        this.hideWithChestplate = new VisibilityOption(
                "button.configureCosmetic.hide_with_chestplate",
                0x2,
                hideWithChestplate
        );
        this.hideWithLeggings = new VisibilityOption(
                "button.configureCosmetic.hide_with_leggings",
                0x4,
                hideWithLeggings
        );
        this.hideWithBoots = new VisibilityOption(
                "button.configureCosmetic.hide_with_boots",
                0x8,
                hideWithBoots
        );
        this.hideWithCloak = new VisibilityOption(
                "button.configureCosmetic.hide_with_cloak",
                0x10,
                hideWithCloak
        );
        this.hideWithElytra = new VisibilityOption(
                "button.configureCosmetic.hide_with_elytra",
                0x20,
                hideWithElytra
        );
        this.hideWithParrot = new VisibilityOption(
                "button.configureCosmetic.hide_with_parrot",
                0x40,
                hideWithParrot
        );
    }

    private final Range x;
    private final Range y;
    private final Range z;

    private final VisibilityOption hideWithHelmet;
    private final VisibilityOption hideWithChestplate;
    private final VisibilityOption hideWithLeggings;
    private final VisibilityOption hideWithBoots;
    private final VisibilityOption hideWithCloak;
    private final VisibilityOption hideWithElytra;
    private final VisibilityOption hideWithParrot;

    public Range getXRange() { return this.x; }

    public Range getYRange() { return this.y; }

    public Range getZRange() { return this.z; }

    public void forAllVisibilityOptions(Consumer<VisibilityOption> consumer) {
        consumer.accept(hideWithHelmet);
        consumer.accept(hideWithChestplate);
        consumer.accept(hideWithLeggings);
        consumer.accept(hideWithBoots);
        consumer.accept(hideWithCloak);
        consumer.accept(hideWithElytra);
        consumer.accept(hideWithParrot);
    }

    /**
     * Helper for range operations for Accessory offset sliders.
     */
    public static final class Range {
        Range(double[] d) {
            this.span = d;
        }

        private final double[] span;

        public double getRange() {
            return span[1] - span[0];
        }

        public double map(double d) {
            return span[0] + d * (span[1] - span[0]);
        }

        public double clamp(double d) {
            return d < span[0] ? span[0] : (d > span[1] ? span[1] : d);
        }

        public double clampMap(double d) {
            return clamp(map(d));
        }
    }

    public static final class VisibilityOption {
        public VisibilityOption(String key, int mask, boolean defaultValue) {
            this.key = key;
            this.mask = mask;
            this.defaultValue = defaultValue;
            this.userValue = new State<>(defaultValue);
        }

        private final String key;
        private final int mask;
        private final boolean defaultValue;
        private final State<Boolean> userValue;

        public String getTranslationKey() {
            return this.key;
        }

        public void configureUserValue(AtomicInteger flags) {
            if (this.userValue.peek()) {
                flags.set(flags.get() | mask);
            } else {
                flags.set(flags.get() & ~mask);
            }
        }

        public boolean getDefaultValue() {
            return this.defaultValue;
        }

        public boolean getUserValue() {
            return this.userValue.peek();
        }

        // Create controller, only for user value.
        public Div createController() {
            return new Div() {
                @Override
                public List<Component> build() {
                    boolean value = VisibilityOption.this.userValue.acquire(this);
                    return Arrays.asList(new Button(
                            Text.translatable(getTranslationKey(), value ? Text.GUI_YES.getDisplayString() : Text.GUI_NO.getDisplayString()),
                            () -> userValue.set(!userValue.peek())
                    ));
                }
            };
        }
    }
}
