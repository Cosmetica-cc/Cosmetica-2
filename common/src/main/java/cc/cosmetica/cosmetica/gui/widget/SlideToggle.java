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

import cc.cosmetica.cosmetica.gui.GuiUtils;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.Context;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.SizedElement;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;

import java.util.List;

public class SlideToggle extends Component {
    public SlideToggle(State<Boolean> state, Text leftLabel, Text rightLabel) {
        this.state = state;
        this.leftLabel = leftLabel;
        this.rightLabel = rightLabel;
    }

    private final State<Boolean> state;
    private final Text leftLabel, rightLabel;

    private float ease = 1;
    private long time = System.currentTimeMillis();

    // not including labels
    @Override
    public Dimensions minimumSize(List<? extends SizedElement> children, Margins padding, int vw, int vh) {
        return new Dimensions(BUTTON_WIDTH, 20);
    }

    @Override
    public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
        return new Dimensions(BUTTON_WIDTH, 20);
    }

    @Override
    public List<Component> build() {
        return ImmutableList.of();
    }

    @Override
    public void mouseClicked(Element target, double x, double y, int button) {
        if (target.getComponent() == this) {
            GuiUtils.playClick();
            this.state.set(!this.state.peek());
            this.ease = 1 - this.ease;
        }
    }

    // draw button and labels
    @Override
    protected void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
        int centreX = region.getX() + region.getWidth()/2;
        int centreY = region.getY() + region.getHeight()/2;
        int y = region.getY();
        int x0 = centreX - BUTTON_WIDTH/2;

        // text
        int lengthLeft = canvas.getDrawingContext().getWidth(this.leftLabel);
        int textHeight = canvas.getDrawingContext().getLineHeight();
        canvas.drawText(this.leftLabel, x0 - lengthLeft - 1, centreY - textHeight/2, 0xFFFFFF);
        canvas.drawText(this.rightLabel, centreX + BUTTON_WIDTH/2 + 2, centreY - textHeight/2, 0xFFFFFF);

        final float btn = 0.44f;
        final float highlight = 0.65f;
        final float shade = 0.34f;

        // outline
        canvas.drawRect(x0, y, BUTTON_WIDTH, region.getHeight(), 0, 0, 0, 0);
        // box
        boolean b = state.peek();
        int miniBoxX = x0 + 1;
        if (b) {
            miniBoxX += (int)Mth.lerp(ease, 0, KNOB_WIDTH);
        } else {
            miniBoxX += (int)Mth.lerp(ease, KNOB_WIDTH, 0);
        }

        canvas.drawRect(miniBoxX, y + 1, KNOB_WIDTH, region.getHeight() - 2, 0, btn, btn, btn);
        // highlights and shadows
        canvas.drawRect(miniBoxX, y + 1, KNOB_WIDTH - 1, 1, 0, highlight, highlight, highlight);
        canvas.drawRect(miniBoxX, y + 1, 1, region.getHeight() - 3, 0, highlight, highlight, highlight);
        canvas.drawRect(miniBoxX + 1, region.getFinalY() - 1, KNOB_WIDTH - 1, 1, 0, shade, shade, shade);
        canvas.drawRect(miniBoxX + KNOB_WIDTH - 1, y + 2, 1, region.getHeight() - 3, 0, shade, shade, shade);

        // update ease
        long newTime = System.currentTimeMillis();
        if (ease < 1) {
            ease = Math.min(1, ease + (newTime - time) * 0.001f / 0.1f); // 0.5s
        }
        time = newTime;
    }

    private static final int KNOB_WIDTH = 18;
    private static final int BUTTON_WIDTH = KNOB_WIDTH * 2 + 2;
}
