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

import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.mixin.AbstractScrollContainerAccessor;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import cc.cosmetica.kupe.impl.MinecraftBuiltinComponent;
import cc.cosmetica.kupe.impl.StateManagerImpl;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CapeServerSettingsScreen extends Screen {
    public CapeServerSettingsScreen() {
        super(ID);
        this.servers = new State<>(ImmutableList.of(
                new Div().tag("cape-server"),
                new Div().tag("cape-server"),
                new Div().tag("cape-server")
        ));
    }

    private final State<List<Component>> servers;

    @Override
    protected Component[] buildScreen() {
        return new Component[] {
                new CapeServerList(this.servers),
                new MenuEndSelection()
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(CapeServerList.class, Style.create()
                        .set(HEIGHT, screen(0, 60)))
                .tag("cape-server", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(GuiUtils.NORMAL_COLOUR))
                        .set(BORDER, GuiUtils.POPOUT_BORDER)
                        .set(HEIGHT, fixedSize(40))
                        .set(WIDTH, screen(60, 0)));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "cape_server_settings");

    // This iteration uses generic component children. Could squeeze more performance by hardcoding
    // child paints so that we don't have to do a resize on drag.
    // Only allows moving items vertically.
    private static class CapeServerList extends AbstractScrollContainer {
        CapeServerList(State<List<Component>> children) {
            this.children = children;
        }

        private State<List<Component>> children;
        private @Nullable Component dragging = null;
        // rootY updated in paint()
        // clickY updated on click
        // draggingOffset updated in both
        private int draggingOffset, clickY, rootY;
        // ghostRegion updated on click
        private Region ghostRegion;
        // elementHeight updated on resize
        private int elementHeight;

        @Override
        public Dimensions minimumSize(List<? extends SizedElement> children, Margins padding, int vw, int vh) {
            return this.size(children, SizedElement::getMinimumSize, padding);
        }

        @Override
        public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
            return this.size(children, SizedElement::getPreferredSize, padding);
        }

        private Dimensions size(List<? extends SizedElement> children, Function<SizedElement, Dimensions> getDimensions, Margins padding) {
            int elementWidth = 0;
            int elementHeight = 0;

            // find largest size
            for (SizedElement element : children) {
                Dimensions dimensions = getDimensions.apply(element);

                if (dimensions.getHeight() > elementHeight) {
                    elementHeight = dimensions.getHeight();
                }
                if (dimensions.getWidth() > elementWidth) {
                    elementWidth = dimensions.getWidth();
                }
            }

            return new Dimensions(elementWidth + padding.horizontal(), elementHeight * children.size() + padding.vertical());
        }


        @Override
        protected boolean hasVerticalOverflow() {
            return false;
        }

        @Override
        public List<Component> build() {
            return this.children.acquire(this);
        }

        @Override
        public void resize(Region contentRegion, SizedElement sizedElement, List<? extends ResizableElement> children, Context context) {
            // determine child region height (must be same)
            int elementHeight = 0;

            for (SizedElement element : children) {
                Dimensions dimensions = element.getPreferredSize();

                if (dimensions.getHeight() > elementHeight) {
                    elementHeight = dimensions.getHeight();
                }
            }
            this.elementHeight = elementHeight;

            // lay out stuff
            int y = contentRegion.getY();
            for (ResizableElement element : children) {
                final int width = Math.min(
                        Math.max(
                                Math.min(element.getPreferredSize().getWidth(),
                                        element.getMaximumSize().getWidth()),
                                element.getMinimumSize().getWidth()
                        )
                        , contentRegion.getWidth());

                final int height = // TODO final Math.min may be unnecessary due to elementHeight being derived from largest preferred size
                        Math.min(
                                Math.max(
                                        Math.min(element.getPreferredSize().getHeight(),
                                                element.getMaximumSize().getHeight()), // preferred doesn't account for max?
                                        element.getMinimumSize().getHeight() // in case min > max
                                )
                                , elementHeight);

                if (element.getComponent() == this.dragging) {
                    // dragging layout
                    element.setRenderRegion(new Region(contentRegion.getX(), Math.min(Math.max(y + this.draggingOffset, contentRegion.getY()), contentRegion.getY() + (this.children.peek().size() - 1) * this.elementHeight), width, height).shrinkMargins(element.getPadding()));
                } else {
                    // normal layout
                    element.setRenderRegion(new Region(contentRegion.getX(), y, width, height).shrinkMargins(element.getPadding()));
                }

                y += elementHeight;
            }

            // set overflow flag
            this.overflow = y > contentRegion.getEndY();
            this.maxScroll = y - contentRegion.getEndY();
            if (this.maxScroll < 0) this.maxScroll = 0;
            // scrollbar grab from AbstractScrollContainer
            this.grabbed = false;
        }

        @Override
        protected void paint(Canvas canvas, Region region, int mouseX, int mouseY) {
            this.rootY = region.getY();

            if (this.dragging != null) {
                if (false) {
                    border = Border.create(Border.BorderConfig.split(2, 0xadadad, 0x5e5e5e)).orElseThrow(IllegalStateException::new);
                    border.paint(canvas, this.ghostRegion, this.getStyle());
                }

                int newDraggingOffset = this.getInnerClickY(mouseY) - this.clickY;

                if (newDraggingOffset != this.draggingOffset) {
                    this.draggingOffset = newDraggingOffset;

                    // check if order has changed
                    int newIndex = this.getInnerClickY(mouseY) / this.elementHeight;
                    if (newIndex < 0) {
                        newIndex = 0;
                    } else if (newIndex >= this.children.peek().size()) {
                        newIndex = this.children.peek().size() - 1;
                    }

                    this.draggingOffset += this.elementHeight * ((this.clickY/this.elementHeight) - newIndex);

                    int oldIndex = this.children.peek().indexOf(this.dragging);

                    if (oldIndex != newIndex) {
                        List<Component> newOrder = new ArrayList<>(this.children.peek());
                        newOrder.remove(this.dragging);
                        newOrder.add(newIndex, this.dragging);
                        this.children.set(newOrder);
                    } else {
                        // yes the whole screen
                        StateManagerImpl.scheduleResize();
                    }
                }
            }
            super.paint(canvas, region, mouseX, mouseY);
        }

        private static Border border;

        // drag
        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            super.mouseClicked(target, x, y, button);

            if (!this.grabbed && !(target.getComponent() instanceof MinecraftBuiltinComponent)) {
                this.clickY = getInnerClickY((int)y);

                // click clicking a component inside
                if (this.clickY > 0) {
                    int index = this.clickY / this.elementHeight;

                    if (index < this.children.peek().size()) {
                        this.draggingOffset = 0;
                        this.dragging = this.children.peek().get(index);
                    }
                }
            }
        }

        private int getInnerClickY(int outerY) {
            return (int)(outerY - this.rootY + this.maxScroll * ((AbstractScrollContainerAccessor)this).getScrollPercent());
        }

        @Override
        public void mouseReleased(double x, double y, int button) {
            super.mouseReleased(x, y, button);

            if (this.dragging != null) {
                // TODO re-arrange children
                this.dragging = null;
            }
        }
    }
}
