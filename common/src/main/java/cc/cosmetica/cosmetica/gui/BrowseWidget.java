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

import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.TextBox;
import com.google.common.collect.ImmutableList;

import java.util.List;

// Requirements
// - browse cosmetica
// - filter by type
// - adjust position (accessories)
// Flow
// - browse and select item / cancel
// - expand item and adjust (select arm, adjust offsets) / cancel
// - add
public class BrowseWidget extends Div {
    private final State<String> searchValue = new State<>("");

    @Override
    public List<Component> build() {
        return ImmutableList.of(
                new TextBox(Text.translatable("label.browse.search"), this.searchValue, true, 32),
                new EntryList.Div(
                        // elements
                )
        );
    }
}
