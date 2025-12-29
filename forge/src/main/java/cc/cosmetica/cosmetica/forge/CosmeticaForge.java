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

package cc.cosmetica.cosmetica.forge;

import cc.cosmetica.com.fasterxml.jackson.databind.ObjectMapper;
import cc.cosmetica.cosmetica.CacheCosmeticManager;
import cc.cosmetica.cosmetica.Cosmetica;
import gg.cloaks.javaclient.model.CosmeticaUser;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Mod("cosmetica")
public class CosmeticaForge implements CacheCosmeticManager.UserIO {
	public CosmeticaForge() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
	}

	private void onClientSetup(FMLClientSetupEvent event) {
		Cosmetica.init(this);
	}

	@Override
	public CosmeticaUser read(InputStream is) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(is, CosmeticaUser.class);
	}

	@Override
	public void write(CosmeticaUser user, OutputStream os) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(os, user);
	}
}
