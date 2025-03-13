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

package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.style.Style;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.WIDTH;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.fixed;

public class CycleButton extends Div {
    /**
     * Create a new cycle button for the given setting.
     */
    public CycleButton(State<Setting<?>> setting, Consumer<CycleButton> cycle) {
        this.cycle = cycle;
        this.setting = setting;
    }

    private final Consumer<CycleButton> cycle;
    private final State<Setting<?>> setting;

    @Override
    public List<Component> build() {
        Setting<?> value = this.setting.acquire(this);

        return ImmutableList.of(
                new Button(
                        Text.translatable(value.name.getString() + "." + value.get()),
                        () -> this.cycle.accept(this)
                ).withStyle(Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(100))))
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void cycleEnum() {
        Enum<? extends Enum> value = (Enum) this.setting.peek().get();
        Enum[] values = value.getDeclaringClass().getEnumConstants();
        value = values[(value.ordinal() + 1) % values.length];
        ((Setting)this.setting.peek()).set(value);
        // update state
        this.setting.set(this.setting.peek());
    }

    @SuppressWarnings("unchecked")
    public void cycleBoolean() {
        Boolean b = (Boolean) this.setting.peek().get();
        ((Setting<Boolean>)this.setting.peek()).set(!b);
        // update state
        this.setting.set(this.setting.peek());
    }
}
