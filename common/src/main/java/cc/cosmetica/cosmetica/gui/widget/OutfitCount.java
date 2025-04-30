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

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Div;
import cc.cosmetica.kupe.api.gui.Label;
import gg.cloaks.javaclient.api.DefaultApi;
import gg.cloaks.javaclient.model.PlanRestrictions;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cc.cosmetica.cosmetica.Cosmetica.mainThreadCall;

/**
 * Counter for used/remaining outfit count.
 */
public class OutfitCount extends Div {
    /**
     * Set a negative number for "Loading..."
     */
    public OutfitCount(State<Integer> outfitLimit) {
        this.outfitLimit = outfitLimit;
    }

    private final State<Integer> outfitLimit;

    @Override
    public List<Component> build() {
        int limit = this.outfitLimit.acquire(this);
        int count = Cosmetica.OWN_OUTFITS.extract(this, List::size);

        return Collections.singletonList(
                new Label(count<0 ? Text.translatable("label.cosmetica.loading") :
                        Text.translatable(
                                "label.cosmetica.outfitCount",
                                String.valueOf(count),
                                String.valueOf(limit)
                        ))
        );
    }
}
