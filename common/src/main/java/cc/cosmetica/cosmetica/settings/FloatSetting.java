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

package cc.cosmetica.cosmetica.settings;

import cc.cosmetica.cosmetica.gui.CosmeticaSettingsScreen;
import cc.cosmetica.cosmetica.gui.widget.SliderWidget;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;

import java.util.Objects;

public class FloatSetting extends Setting<Float> {
    public FloatSetting(String key, float defaultValue, float precision) {
        super(key, defaultValue);
        this.precision = precision;
        this.description = Text.translatable(key + ".description");
    }

    private final float precision;
    private final Text description;

    @Override
    public Component createController(State<Float> updater) {
        State<Float> temp = new State<>(updater.peek());

        return new SliderWidget(temp, this.precision, f -> Text.literal(String.format("%.1f", f))) {
            @Override
            public void mouseReleased(double x, double y, int button) {
                if (!Objects.equals(temp.peek(), FloatSetting.this.get())) {
                    // set modified
                    updater.set(temp.peek());
                }
            }
        };
    }

    @Override
    public Text createDescription(Float value) {
        return this.description;
    }
}
