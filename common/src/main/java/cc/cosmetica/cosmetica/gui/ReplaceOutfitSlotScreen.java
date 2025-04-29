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
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.OutfitCosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.cosmetica.gui.widget.OutfitCount;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import gg.cloaks.javaclient.api.DefaultApi;
import gg.cloaks.javaclient.model.CopyOutfitDto;
import gg.cloaks.javaclient.model.Outfit;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static cc.cosmetica.cosmetica.Cosmetica.mainThreadCall;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * A confirmation screen for stealing someone's look when you don't already have the outfit.
 * Prompts the user which outfit slot to use, or whether to add a new item. Shows how many outfit slots
 * are used.
 * Unregistered. Pass ID as a second parameter when setting Kupe screen.
 */
public class ReplaceOutfitSlotScreen extends Component {
    protected ReplaceOutfitSlotScreen(Cosmetics initialCosmetics) {
        if (!initialCosmetics.getOutfitId().isPresent())
            throw new IllegalArgumentException("No outfit id for steal-their-look cosmetics?!");

        this.title = STEAL_THEIR_LOOK.translationKey("screens");
        this.newOutfit = UUID.fromString(initialCosmetics.getOutfitId().get());
        this.newOutfitCosmetics = new State<>(initialCosmetics);
        CosmeticaAPI.subscribe(CosmeticaAPI.SubscriptionEvent.OUTFIT, this.newOutfit, STEAL_THEIR_LOOK.toResourceLocation(), () -> {
            CosmeticaAPI.performAsync(api -> api.outfitsControllerGet(this.newOutfit.toString()))
                    .thenApply(OutfitCosmetics::new)
                    .thenAccept(mainThreadCall(this.newOutfitCosmetics::set))
                    .exceptionally(err->{
                        Logging.getInstance().error("Error updating outfit cosmetics", err);
                        return null;
                    });
        });
    }

    private final Text title;
    private final UUID newOutfit;
    private final State<Cosmetics> newOutfitCosmetics;
    private final State<Integer> outfitLimit = new State<>(-1);
    private final State<OutfitWheelScreen.OutfitOption> replacing = new State<>(null);

    @Override
    public List<Component> build() {
        Cosmetics outfit = this.newOutfitCosmetics.acquire(this);

        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.acquire(this);

        List<Component> components = options.stream()
                .map(ReplaceableOutfit::new)
                .collect(Collectors.toCollection(ArrayList::new));
        components.add(0, new );

        final UUID player = Minecraft.getInstance().getUser().getGameProfile().getId();

        return Arrays.asList(
                new Div(
                        new Label(this.title),
                        new OutfitCount(this.outfitLimit)
                ).tag("title"),
                new Div(
                        new Div(new FakePlayer(player, true)
                                .withStyle(Style.create().set(WIDTH, screen(12, 0)))),
                        new OutfitSlotGrid(this.outfitLimit, components)
                ).tag("body"),
                new Div(
                        new Button(Text.translatable("button.cosmetica.confirm"), () -> {}),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                ).tag("bottom-bar")
        );
    }

    @Override
    public void unmount() {
        CosmeticaAPI.unsubscribe(CosmeticaAPI.SubscriptionEvent.OUTFIT, this.newOutfit, STEAL_THEIR_LOOK.toResourceLocation());
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return new Stylesheet()
                .tag("body", BODY_DEFAULT_STYLE)
                .tag("title", TITLE_DEFAULT_STYLE)
                .tag("bottom-bar", Style.create()
                        .set(MARGINS, (vw, vh, pw, ph) -> new Margins(ph - 80, 0, 0, 0))
                );
    }

    public static final ResourceKey STEAL_THEIR_LOOK = new ResourceKey("cosmetica", "steal_their_look");

    // Stolen & modified from kupe Screen
    private static final Style BODY_DEFAULT_STYLE = Style.create()
                    .set(WIDTH, SCREEN_WIDTH)
                    .set(HEIGHT, SCREEN_HEIGHT)
                    .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)//modified
                    .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                    .set(Div.ALIGN_ITEMS, Align.CENTRE);

    private static final Style TITLE_DEFAULT_STYLE = Style.create()
                    .set(WIDTH, SCREEN_WIDTH)
                    .set(Label.ALIGN_TEXT, Align.CENTRE)
                    .set(MARGINS, fixed(new Margins(15, 0, 0, 0)));

    private static Component find(Component[] components, String id) {
        for (Component outfit : components) {
            if (outfit instanceof ReplaceableOutfit) {
                if (((ReplaceableOutfit)outfit).option.id.equals(id)) {
                    return outfit;
                }
            } else if (id.isEmpty()) {
                return outfit; // remaining item: the new option
            }
        }

        // none matched
        return null;
    }

    private static class OutfitSlotGrid extends Div {
        public OutfitSlotGrid(State<Integer> outfitLimit, ReplaceableOutfit[] components) {
            this.outfitLimit = outfitLimit;
            this.components = components;
        }

        private final State<Integer> outfitLimit;
        private final ReplaceableOutfit[] components;

        @Override
        public List<Component> build() {
            int limit = this.outfitLimit.acquire(this);

            return Collections.singletonList(
                    new EntryList.Grid(this.components/*, state*/)
            );
        }
    }
    /**
     * A selectable outfit item in the menu.
     */
    private static class ReplaceableOutfit extends Image {
        ReplaceableOutfit(OutfitWheelScreen.OutfitOption option) {
            super(option == null ? OutfitSelectScreen.NEW_OUTFIT_ICON : new ResourceKey(option.thumbnail.location));
            this.option = option;
            this.setTransparent(option.usable ? 1.0f : 0.5f);
        }

        private final OutfitWheelScreen.OutfitOption option;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (!this.option.usable) return;
            // play click sound
            GuiUtils.playClick();
        }
    }
}
