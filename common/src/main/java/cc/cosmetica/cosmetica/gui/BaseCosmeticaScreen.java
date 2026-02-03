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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Base class for Cosmetica screens that adds notification popups.
 * Based on {@link cc.cosmetica.kupe.api.Screen}.
 */
public abstract class BaseCosmeticaScreen extends Component {
    protected BaseCosmeticaScreen(ResourceKey id) {
        this.key = id.translationKey("screens");
    }

    protected BaseCosmeticaScreen(Text title) {
        this.key = title;
    }

    private final Text key;

    @Override
    public final List<Component> build() {
        return Arrays.asList(
                new Label(this.key).tag("title"),
                new Div(this.buildScreen()).tag("body"),
                new Div() {
                    @Override
                    public List<Component> build() {
                        return Arrays.asList(NOTIFICATIONS.acquire(this));
                    }
                }.tag("notifications")
        );
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        // the child screen may want to override the style
        // they can do that with this stylesheet
        return new Stylesheet()
                .tag("title", cc.cosmetica.kupe.api.Screen.TITLE_DEFAULT_STYLE)
                .tag("body", cc.cosmetica.kupe.api.Screen.BODY_DEFAULT_STYLE)
                .tag("notifications", Style.create()
                        .set(WIDTH, SCREEN_WIDTH)
                        .set(HEIGHT, SCREEN_HEIGHT)
                        .set(Z_INDEX, 10)
                        .set(Div.FLOW_DIRECTION, Axis2D.NEGATIVE_Y)
                        .set(Div.JUSTIFY_CONTENT, Justify.START)
                        .set(Div.ALIGN_ITEMS, Align.END)
                        .set(PADDING, fixed(new Margins(5, 0))))
                .component(Notification.class, Style.create()
                        .set(MARGINS, fixed(new Margins(2, 10)))
                        .set(WIDTH, screen(28, 0))
                        .set(MIN_WIDTH, fixedSize(50))
                        .set(HEIGHT, fixedSize(50))
                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE));
    }

    /**
     * Build the child components of this screen. These will be placed in the div.
     * @return the child components of this screen.
     */
    protected abstract Component[] buildScreen();

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Screens.closeCurrentScreen();
        }
        Minecraft.getInstance().getToasts().addToast( new CosmeticaToast(Text.literal("Failed to perform action")) );

        return false;
    }

    private static final State<Notification[]> NOTIFICATIONS = new State<>(new Notification[] {
//            new Notification(Text.literal("Test 0"), 0xf4f4f4, 0xef5858),
//            new Notification(Text.literal("Test 0"), 0xf4f4f4, 0x77d5ef),
//            new Notification(Text.literal("Test 0"), 0xf4f4f4, 0xd3d3d3)
    });

    /**
     * Notification box.
     */
    private static class Notification extends Div {
        public Notification(Text text, int textColour, int backgroundColour) {
            super(new Label(text));
            this.fgColour = textColour;
            this.backgroundColour = backgroundColour;

            // Extract components and normalize to [0,1]
            float r = ((backgroundColour >> 16) & 0xFF) / 255f;
            float g = ((backgroundColour >> 8) & 0xFF) / 255f;
            float b = (backgroundColour & 0xFF) / 255f;

            float max = Math.max(r, Math.max(g, b));
            float min = Math.min(r, Math.min(g, b));
            float delta = max - min;

            // Lightness
            float l = (max + min) / 2f;

            // Saturation
            float s;
            if (delta == 0f) {
                s = 0f;
            } else {
                s = delta / (1f - Math.abs(2f * l - 1f));
            }

            // Hue
            float h;
            if (delta == 0f) {
                h = 0f;
            } else if (max == r) {
                h = 60f * (((g - b) / delta) % 6f);
            } else if (max == g) {
                h = 60f * (((b - r) / delta) + 2f);
            } else {
                h = 60f * (((r - g) / delta) + 4f);
            }

            if (h < 0f) h += 360f;

            // Convert to ints
//            int H = Math.round(h);        // 0–360
//            int S = Math.round(s * 100f); // 0–100
//            int L = Math.round(l * 100f); // 0–100

            this.highlight = hslToRgbInt(h, s, l * 1.33f);
            this.shadow = hslToRgbInt(h, s, l * 0.5f);
        }

        private final int fgColour;
        private final int backgroundColour;
        private final int highlight;
        private final int shadow;

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .self(Style.create()
                            .set(Label.TEXT_COLOUR, this.fgColour)
                            .set(BACKGROUND_COLOUR, OptionalInt.of(this.backgroundColour))
                            .set(BORDER, Border.create(Border.BorderConfig.split(1, this.highlight, this.shadow))));
        }
    }

    private static int hslToRgbInt(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = l - c / 2f;

        float r1 = 0f, g1 = 0f, b1 = 0f;

        if (h < 60f) {
            r1 = c; g1 = x;
        } else if (h < 120f) {
            r1 = x; g1 = c;
        } else if (h < 180f) {
            g1 = c; b1 = x;
        } else if (h < 240f) {
            g1 = x; b1 = c;
        } else if (h < 300f) {
            r1 = x; b1 = c;
        } else {
            r1 = c; b1 = x;
        }

        int R = Math.round((r1 + m) * 255f);
        int G = Math.round((g1 + m) * 255f);
        int B = Math.round((b1 + m) * 255f);

        // Clamp (safety for rounding)
        R = Math.min(255, Math.max(0, R));
        G = Math.min(255, Math.max(0, G));
        B = Math.min(255, Math.max(0, B));

        return (R << 16) | (G << 8) | B;
    }

}
