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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.SearchCosmeticsDto;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Screen for browsing and applying new cosmetics.
 * Should be seamless with {@link cc.cosmetica.cosmetica.gui.CosmeticaHomeScreen}.
 */
public class BrowseScreen extends AbstractHomeScreen {
    public BrowseScreen() {
        super(ID);
        super.lockActions = true;
    }

    private final State<String> searchQuery = new State<>("");
    // TODO use in outfit player for preview (or similar)
    private final State<@Nullable CosmeticEntry> selected = new State<>(null);

    @Override
    protected @NotNull Component createRightMenu(Cosmetics cosmetics, boolean authenticated) {
        /*
         * Browser layout.
         */
        return new Div(
                new Div(
                        new TextBox(
                                Text.translatable("label.browse.search"), // todo better format for translation strings?
                                this.searchQuery,
                                true,
                                32).tag("searchbar"),
                        new Button(Text.literal(" "), ()->{}).tag("btn-search-adjust"), // sort
                        new Button(Text.literal(" "), ()->{}).tag("btn-search-adjust")  // filter
                ).withStyle(Style.create()
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_BETWEEN)),
                new Results()
        ).withStyle(Style.create()
                .set(PADDING, fixed(new Margins(30, 10, 12, 10)))
                .set(Div.ALIGN_ITEMS, Align.STRETCH_START));
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("btn-search-adjust", Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(20))))
                .tag("searchbar", Style.create()
                        .set(WIDTH, (vw, vh, pw, ph) -> OptionalInt.of(pw - 22 * 2)));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "browse");


    /**
     * Browser results. Automatically updates.
     */
    private class Results extends Component {
        private final State<List<Component>> pageResults = new State<>(Collections.emptyList());
        private volatile int state = 0;

        @Override
        public List<Component> build() {
            // Acquire states
            String query = BrowseScreen.this.searchQuery.acquire(this);
            @Nullable Cosmetics outfit = Cosmetica.OWN_COSMETICS.acquire(this);

            // Build Search
            final int nextState = this.state + 1;
            this.state = nextState;

            SearchCosmeticsDto dto = new SearchCosmeticsDto();
            dto.setName(query);

            // Send Search
            CosmeticaAPI.performAsync(api -> api.searchControllerSearchCosmetics(dto))
                    .thenAcceptAsync(cosmetics -> {
                        ArrayList next = new ArrayList();
                        CosmeticEntry.populateBrowseList(next, cosmetics, outfit);
                        this.pageResults.set(next);
                        BrowseScreen.this.selected.set(null);
                    }, Minecraft.getInstance())
                    .exceptionally(ex -> {
                        Logging.getInstance().error("Error performing search for " + query, ex);
                        // TODO show error visually
                        return null;
                    });

            // Layout
            return ImmutableList.of(
                    new EntryList.DynamicDiv(this.pageResults, BrowseScreen.this.selected::acquire)
            );
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet()
                    .component(EntryList.DynamicDiv.class, Style.create()
                            .set(MARGINS, fixed(new Margins(10, 0, 0, 0)))
                            .set(HEIGHT, screen(0, 70)));
        }
    }
}
