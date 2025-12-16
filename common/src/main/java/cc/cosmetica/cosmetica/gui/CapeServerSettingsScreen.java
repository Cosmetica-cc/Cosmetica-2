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

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.mixin.AbstractScrollContainerAccessor;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import cc.cosmetica.kupe.impl.MinecraftBuiltinComponent;
import cc.cosmetica.kupe.impl.StateManagerImpl;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.cloaks.javaclient.model.ExternalCapeSetting;
import gg.cloaks.javaclient.model.UpdateExternalCapeSettingDto;
import gg.cloaks.javaclient.model.UpdateSettingsDto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class CapeServerSettingsScreen extends Screen {
    public CapeServerSettingsScreen(List<ExternalCapeSetting> externalCapeSettings) {
        // TODO make externalCapeSettings a state so this updates from website
        super(ID);
        this.servers = new State<>(externalCapeSettings.stream()
                .map(CapeSetting::new)
                .collect(Collectors.toList())
        );
        this.oldSettings = externalCapeSettings;
    }

    private final List<ExternalCapeSetting> oldSettings;
    private final State<List<Component>> servers;

    @Override
    protected Component[] buildScreen() {
        return new Component[] {
                new CapeServerList(this.servers),
                new MenuEndSelection()
        };
    }

    @Override
    public void unmount() {
        // check for changed settings
        boolean isModified = false;
        List<Component> newSettings = this.servers.peek();

        for (int i = 0; i < this.oldSettings.size(); i++) {
            ExternalCapeSetting setting = this.oldSettings.get(i);
            CapeSetting component = (CapeSetting) newSettings.get(i);

            // Either order is different or toggles are different
            if (component.setting.getService() != setting.getService() ||
                    component.enabled.peek() != setting.isEnabled()) {
                isModified = true;
                break;
            }
        }

        // Update settings
        if (isModified) {
            Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Updating external cape settings");

            UpdateSettingsDto dto = new UpdateSettingsDto();
            List<UpdateExternalCapeSettingDto> newExternalCapes = new ArrayList<>();
            for (Component component : newSettings) {
                CapeSetting capeSetting = (CapeSetting) component;
                UpdateExternalCapeSettingDto dto1 = new UpdateExternalCapeSettingDto();
                dto1.setEnabled(capeSetting.enabled.peek());
                dto1.setReplace(capeSetting.setting.isReplace());
                dto1.setService(capeSetting.setting.getService().getValue());
            }

            dto.setExternalCapes(newExternalCapes);
            CosmeticaAPI.settings().requestAsync(api -> api.setCloud(dto))
                    .thenAccept(user -> Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Updated external cape settings"))
                    .exceptionally(e -> {
                        Logging.getInstance().error("Error updating external cape settings: ", e);
                        return null;
                    });
        }
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(CapeServerList.class, Style.create()
                        .set(AbstractScrollContainer.SCROLLBAR_POSITION, AbstractScrollContainer.ScrollbarPosition.OUTSIDE)
                        .set(MARGINS, fixed(new Margins(30, 0, 0, 0)))
                        .set(HEIGHT, screen(0, 72)))
                .tag("cape-server", Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)
                        .set(Div.ALIGN_ITEMS, Align.STRETCH_CENTRE)
                        .set(PADDING, fixed(new Margins(0, 6)))
                        .set(BACKGROUND_COLOUR, OptionalInt.of(GuiUtils.NORMAL_COLOUR))
                        .set(BORDER, GuiUtils.POPOUT_BORDER)
                        .set(HEIGHT, fixedSize(40))
                        .set(WIDTH, screen(60, 0)))
                .tag("inner-wrapper", Style.create().set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                .tag("cape-server-button", Style.create()
                        .set(WIDTH, fixedSize(100)))
                .tag("padding-right", Style.create()
                        .set(MARGINS, fixed(new Margins(0, 6, 0, 0))))
                .component(Image.class, Style.create()
                        .set(HEIGHT, fixedSize(24)));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "cape_server_settings");

    private static class CapeSetting extends Div {
        CapeSetting(ExternalCapeSetting capeServerSetting) {
            this.tag("cape-server");
            this.setting = capeServerSetting;
            this.enabled = new State<>(setting.isEnabled());
        }

        private final ExternalCapeSetting setting;
        private final State<Boolean> enabled;

        @Override
        public List<Component> build() {
            boolean enabled = this.enabled.acquire(this);

            return ImmutableList.of(
                    new Div(
//                        new Image(
//                                "official".equals(capeServerSetting.getService().getValue()) ?
//                                new ResourceKey("minecraft", "textures/block/grass_block_side.png") :
//                                new ResourceKey("cosmetica", "textures/capeservers/" + capeServerSetting.getService().getValue() + ".png")
//                        ).setTransparent(1).tag("padding-right"),
                            new Label(Text.literal(setting.getName()))
                    ).tag("inner-wrapper"),
                    new Div(
                            new Button(enabled ?
                                    (useMinecraftText ? Text.GUI_YES : Text.translatable("button.cosmetica.enabled"))
                                    : (useMinecraftText ? Text.GUI_NO : Text.translatable("button.cosmetica.disabled")),
                                    () -> this.enabled.set(!enabled)).tag("cape-server-button", "padding-right"),
                            new Image(new ResourceKey("cosmetica", "textures/grabbable.png")).setTransparent(1)
                    ).tag("inner-wrapper")
            );
        }

        boolean useMinecraftText = !Minecraft.getInstance().getLanguageManager().getSelected().getCode().toLowerCase(Locale.ROOT).startsWith("en")
                && "Enabled".equals(I18n.get("button.cosmetica.enabled"));
    }

    // This iteration uses generic component children. Could squeeze more performance by hardcoding
    // child paints so that we don't have to do a resize on drag.
    // Only allows moving items vertically.
    private static class CapeServerList extends AbstractScrollContainer {
        CapeServerList(State<List<Component>> children) {
            this.children = children;
            this.ghost = new Div().withStyle(Style.create()
                    .set(BACKGROUND_COLOUR, OptionalInt.of(0x363636))
                    .set(BORDER, Border.create(Border.BorderConfig.split(1, 0x606060, 0x232323))));
        }

        private State<List<Component>> children;
        private final Component ghost;
        private @Nullable Component dragging = null;
        // rootY updated in paint()
        // clickY updated on click
        // draggingOffset updated in both
        private int draggingOffset, clickY, rootY;
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
                // ignore ghost
                if (element.getComponent() == this.ghost) continue;

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
            return this.overflow;
        }

        @Override
        public List<Component> build() {
            List<Component> result = new ArrayList<>(this.children.acquire(this));
            result.add(this.ghost);
            return result;
        }

        @Override
        public void resize(Region contentRegion, SizedElement sizedElement, List<? extends ResizableElement> children, Context context) {
            // determine child region height (must be same)
            int elementHeight = 0;
            ResizableElement ghostElement = null;

            for (ResizableElement element : children) {
                // Ignore ghost
                if (element.getComponent() == ghost) {
                    ghostElement = element;
                    continue;
                }

                Dimensions dimensions = element.getPreferredSize();

                if (dimensions.getHeight() > elementHeight) {
                    elementHeight = dimensions.getHeight();
                }
            }

            if (ghostElement == null) {
                throw new IllegalArgumentException("Ghost element cannot be null for cape server list");
            }

            this.elementHeight = elementHeight;

            // default ghost position: not visible
            ghostElement.setRenderRegion(new Region(0,0,0,0));

            // lay out stuff
            int y = contentRegion.getY();
            for (ResizableElement element : children) {
                // skip ghost (set elsewhere)
                if (element.getComponent() == ghost) continue;

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
                    // ghost at normal position
                    ghostElement.setRenderRegion(new Region(contentRegion.getX(), y, contentRegion.getWidth(), height));
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

        @Override
        public void paintDecorations(Canvas canvas, Region region, int mouseX, int mouseY) {
            super.paintDecorations(canvas, region, mouseX, mouseY);

            // fixes a rendering bug
            RenderSystem.color4f(1, 1, 1, 1);
        }

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
                        this.dragging.withStyle(Style.create().set(Z_INDEX, 10));
                        // need to rebuild
                        this.children.set(this.children.peek());
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
                // clear dragging. children have already been rearranged
                this.dragging.withStyle(Style.create());
                this.dragging = null;
                StateManagerImpl.scheduleResize();
            }
        }
    }
}
