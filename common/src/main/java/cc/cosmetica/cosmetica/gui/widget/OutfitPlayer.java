/*
 * Copyright 2024 Cosmetica
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

import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.FakePlayer;
import cc.cosmetica.kupe.api.gui.Label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OutfitPlayer extends Component {
	public OutfitPlayer(UUID player) {
		this.player = player;
	}

	private final UUID player;

	@Override
	public List<Component> build() {
		return Arrays.asList(
				new FakePlayer(player, true),
				new Label(Text.literal("Outfit 1")),
				new Button(Text.translatable("button.cosmetica.changeOutfit"), () -> {})
		);
	}
}
