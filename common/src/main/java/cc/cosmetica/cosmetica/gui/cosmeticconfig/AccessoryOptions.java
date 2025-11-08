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

public final class AccessoryOptions extends CosmeticOptions {
    public AccessoryOptions(double[] x, double[] y, double[] z) {
        this.x = new Range(x);
        this.y = new Range(y);
        this.z = new Range(z);
    }

    private final Range x;
    private final Range y;
    private final Range z;

    public Range getXRange() { return this.x; }

    public Range getYRange() { return this.y; }

    public Range getZRange() { return this.z; }

    /**
     * Helper for range operations for Accessory offset sliders.
     */
    public class Range {
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
}
