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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.LoginResult;
import cc.cosmetica.cosmetica.Authentication;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.CosmeticEntry;
import cc.cosmetica.cosmetica.gui.widget.CosmeticsList;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class HomeScreen extends AbstractHomeScreen {
	public HomeScreen() {
		super(ID);
	}

	private final State<Boolean> dismissedError = new State<>(false);
	private static @Nullable LoginResult dismissed = null;
	private static final LoginResult GENERIC = new LoginResult(false, LoginResult.Code.SUCCESS, "", null);

	@Override
	protected @NotNull Component createRightMenu(Cosmetics cosmetics, boolean authenticated) {
		// only rebuild an intermediate 'right menu' component rather than whole screen for error dismiss.
		return new Component() {
			@Override
			public List<Component> build() {
				boolean dismissedError = HomeScreen.this.dismissedError.acquire(this);
				LoginResult result = Authentication.LOGIN_RESULT.acquire(this).orElse(GENERIC);

				if (!authenticated && (!dismissedError || dismissed != result)) {
					return ImmutableList.of(NotLoggedIn(result));
				}

				List<CosmeticEntry> entries = new ArrayList<>();
				CosmeticEntry.populateEntryList(entries, cosmetics, CosmeticEntry.Type.removable(authenticated));

				return ImmutableList.of(
						new CosmeticsList(entries, !authenticated ? CosmeticsList.ListType.OFFLINE : cosmetics == null ? CosmeticsList.ListType.DISABLED : CosmeticsList.ListType.EDITABLE)
				);
			}
		};
	}

	private Component NotLoggedIn(final LoginResult error) {
		return new Div(
			new Label(Text.translatable("label.cosmetica.offline")).tag("not-logged-in-title"),
			// See authentication: representing no internet internally as success code/no success (core does not have a specific code)
			new Label(error == GENERIC ? Text.translatable("label.cosmetica.offline.logging_in")
					: (error.getCode() == LoginResult.Code.SUCCESS && !error.isSuccess()) ? Text.translatable("label.cosmetica.offline.no_internet")
					: Text.translatable("label.cosmetica.offline." + error.getCode().toString().toLowerCase(Locale.ROOT))),
			new Label(Text.literal(error.getMessage())).tag("not-logged-in-description"),
			new Button(Text.translatable("button.cosmetica.dismiss"), () -> {
				dismissed = error;
				this.dismissedError.set(true);
			})
		).tag("not-logged-in");
	}

	@Override
	public @NotNull Stylesheet getStylesheet() {
		return super.getStylesheet()
				.tag("not-logged-in", Style.create()
						.set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
						.set(PADDING, fixed(new Margins(4))))
				.tag("not-logged-in-title", Style.create()
						.set(Label.ALIGN_TEXT, Align.CENTRE)
						.set(MARGINS, fixed(new Margins(20, 0, 4, 0))))
				.tag("not-logged-in-description", Style.create()
						.set(HEIGHT, fixedSize(12 * 5))
						.set(Label.TEXT_COLOUR, 0xa0a0a0));
	}

	public static final ResourceKey ID = new ResourceKey("cosmetica", "home");
}
