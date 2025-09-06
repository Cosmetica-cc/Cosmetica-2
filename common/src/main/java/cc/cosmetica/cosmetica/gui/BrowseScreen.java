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
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.CosmeticsList;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.TextBox;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.SearchCosmeticsDto;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Screen for browsing and applying new cosmetics.
 * Should be seamless with {@link cc.cosmetica.cosmetica.gui.CosmeticaHomeScreen}.
 */
public class BrowseScreen extends AbstractHomeScreen {
    public BrowseScreen() {
        super(ID);
        super.lockActions = true;
    }

    @Override
    protected @NotNull Component createRightMenu(Cosmetics cosmetics, boolean authenticated) {
        return new CosmeticsBrowser(Collections.emptyList());
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "browse");

    /**
     * Browser layout.
     */
    private static final class CosmeticsBrowser extends CosmeticsList {
        public CosmeticsBrowser(Collection<CosmeticEntry> entries) {
            super(entries, false);
        }

        private final State<String> searchQuery = new State<>("");

        @Override
        public List<Component> build() {
            return Arrays.asList(
                    new TextBox(
                            Text.translatable("label.browse.search"), // todo better format for translation strings?
                            this.searchQuery,
                            true,
                            32),
                    new Results()
            );
        }

        /**
         * Browser results. Automatically updates.
         */
        private class Results extends Component {
            private final State<List<Component>> pageResults = new State<>(Collections.emptyList());
            private final State<@Nullable CosmeticEntry> selected = new State<>(null);
            private volatile int state = 0;

            @Override
            public List<Component> build() {
                // Acquire states
                String query = CosmeticsBrowser.this.searchQuery.acquire(this);

                // Build Search
                final int nextState = this.state + 1;
                this.state = nextState;

                SearchCosmeticsDto dto = new SearchCosmeticsDto();
                dto.setName(query);

                // Send Search
                CosmeticaAPI.performAsync(api -> api.searchControllerSearchCosmetics(dto))
                        .thenAcceptAsync(cosmetics -> {
                            ArrayList next = new ArrayList();
//                            CosmeticEntry.populateEntryList(next, cosmetics, 0);
                            this.pageResults.set(next);
                            this.selected.set(null);
                        }, Minecraft.getInstance())
                        .exceptionally(ex -> {
                            Logging.getInstance().error("Error performing search for " + query, ex);
                            // TODO show error visually
                            return null;
                        });

                // Layout
                return ImmutableList.of(
                        new EntryList.DynamicDiv(this.pageResults, this.selected::acquire)
                );
            }
        }
    }
}
