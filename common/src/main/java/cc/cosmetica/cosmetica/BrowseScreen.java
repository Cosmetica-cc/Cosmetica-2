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

package cc.cosmetica.cosmetica;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.cosmetica.gui.AbstractHomeScreen;
import cc.cosmetica.cosmetica.gui.widget.CosmeticsList;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.TextBox;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        return new CosmeticsBrowser();
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "browse");

    private static final class CosmeticsBrowser extends Div {
        private final State<String> searchQuery = new State<>("");

        @Override
        public List<Component> build() {
            return Arrays.asList(
                    new TextBox(
                            Text.translatable("label.browse.search"), // todo better format for translation strings?
                            this.searchQuery,
                            true,
                            32),
                    new CosmeticsList(Collections.emptyList(), false)
            );
        }
    }
}
