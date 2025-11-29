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

import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.Context;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.PointerEvents;
import cc.cosmetica.kupe.api.gui.SizedElement;
import cc.cosmetica.kupe.api.gui.TextBox;
import cc.cosmetica.kupe.api.gui.style.RootStylesheet;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import cc.cosmetica.kupe.impl.MinecraftBuiltinComponent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.POINTER_EVENTS;

public class SliderWidget extends MinecraftBuiltinComponent {
    public SliderWidget(State<Float> value, float precision, Function<Float, Text> textFunction) {
        this.value = value;
        this.textFunction = textFunction;
        this.precision = precision;
    }

    private final State<Float> value;
    private final Function<Float, Text> textFunction;
    private final float precision;
    private AbstractSliderButton cache;

    private float snapToPrecision(float f) {
        if (precision == 0) {
            return f;
        }
        return Math.round(f / precision) * precision;
    }

    @Override
    public AbstractWidget createMinecraftWidget(Region region, Context context) {
        float f = this.value.acquire(this);
        // don't change button when dragging
        return this.drag ? this.cache : (this.cache = new AbstractSliderButton(
                region.getX(),
                region.getY(),
                region.getWidth(),
                region.getHeight(), this.textFunction.apply(snapToPrecision(f)).toMinecraftComponent(), f) {
            @Override
            protected void updateMessage() {
                float nf = SliderWidget.this.value.peek();
                this.setMessage(SliderWidget.this.textFunction.apply(nf).toMinecraftComponent());
            }

            @Override
            protected void applyValue() {
                float newValue = snapToPrecision((float) this.value);
                if (SliderWidget.this.value.peek() != newValue) {
                    SliderWidget.this.value.set(newValue);
                }
            }
        });
    }

    private boolean drag = false;

    @Override
    public void mouseClicked(Element target, double x, double y, int button) {
        super.mouseClicked(target, x, y, button);
        if (target.getComponent() == this) {
            this.drag = true;
        }
    }

//    @Override
//    public void mouseMoved(Region region, double x, double y) {
//        super.mouseMoved(region, x, y);
//        if (this.drag) {// TODO allow mouseMoved to not only be on component
//            this.minecraftWidget.mouseDragged(x, y, GLFW.GLFW_MOUSE_BUTTON_1, 0, 0);
//        }
//    }


    int prevMouseX = 0;
    @Override
    public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
        super.render(canvas, region, padding, mouseX, mouseY);
        if (this.drag && (mouseX != prevMouseX)) {
            this.minecraftWidget.mouseDragged(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_1, 0, 0);
        }
        prevMouseX = mouseX;
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        super.mouseReleased(x, y, button);
        this.drag = false;
    }

    @Override
    public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
        return this.tryFixed(DEFAULT_DIMENSIONS, padding, context);
    }

    private static final Dimensions DEFAULT_DIMENSIONS = new Dimensions(200, 20);

    static {
        // Dragging out of the textbox
        RootStylesheet.setDefaultOverrides(TextBox.class, Style.create().set(POINTER_EVENTS, PointerEvents.ALL));
    }
}
