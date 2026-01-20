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

package cc.cosmetica.cosmetica.neoforge;

import cc.cosmetica.com.fasterxml.jackson.databind.MapperFeature;
import cc.cosmetica.com.fasterxml.jackson.databind.ObjectMapper;
import cc.cosmetica.com.fasterxml.jackson.databind.json.JsonMapper;
import cc.cosmetica.cosmetica.CacheCosmeticManager;
import cc.cosmetica.cosmetica.Cosmetica;
import gg.cloaks.javaclient.model.CosmeticaUser;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Mod("cosmetica")
public class CosmeticaNeoforged implements CacheCosmeticManager.UserIO {
	public CosmeticaNeoforged(IEventBus bus) {
		bus.addListener(this::onClientSetup);
		this.mapper = JsonMapper.builder()
				.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
				.build();
		this.mapper.findAndRegisterModules();
	}

	private final ObjectMapper mapper;

	private void onClientSetup(FMLClientSetupEvent event) {
		Cosmetica.init(this);
	}

	@Override
	public CosmeticaUser read(InputStream is) throws IOException {
		return this.mapper.readValue(is, CosmeticaUser.class);
	}

	@Override
	public void write(CosmeticaUser user, OutputStream os) throws IOException {
		this.mapper.writeValue(os, user);
	}
}
