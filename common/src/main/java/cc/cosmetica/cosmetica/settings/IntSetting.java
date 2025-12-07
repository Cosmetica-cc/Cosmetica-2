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

import cc.cosmetica.cosmetica.gui.widget.SliderWidget;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;

/**
 * Integer-valued setting.
 */
public class IntSetting extends Setting<Integer> {
    public IntSetting(String key, int defaultValue) {
        super(key, defaultValue);
        this.description = Text.translatable(key + ".description");
    }

    private final Text description;

    @Override
    public Component createController() {
        State<Float> temp = new State<>((float)this.get());

        return new SliderWidget(temp, 1, i -> Text.literal(String.format("%d", i.intValue()))) {
            @Override
            public void mouseReleased(double x, double y, int button) {
                if (temp.peek().intValue() == IntSetting.this.get()) {
                    // set modified
                    IntSetting.this.set(temp.peek().intValue());
                }
            }
        }.setDisabled(this.getManagement() != Management.USER);
    }

    @Override
    public Text createDescription(Integer value) {
        return this.description;
    }
}
