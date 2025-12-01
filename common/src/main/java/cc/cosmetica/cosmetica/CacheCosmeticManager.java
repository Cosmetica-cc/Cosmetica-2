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

import cc.cosmetica.core.api.*;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.ImageCacheManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.gui.player.AccessoriesAttachment;
import cc.cosmetica.cosmetica.util.SelfCosmeticsReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.cloaks.javaclient.model.CosmeticaUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheCosmeticManager implements CosmeticManager {
    public CacheCosmeticManager(Path directory) {
        this.directory = directory;
        this.cacheFile = directory.resolve("cache.json");
        this.load();
    }

    private final Path directory, cacheFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("Cache Cosmetic Manager");
        return t;
    });
    private Cosmetics cosmetics;

    @Override
    public boolean canManage(LivingEntity entity) {
        return entity instanceof LocalPlayer && cosmetics != null;
    }

    @Override
    public Cosmetics getCosmetics(LivingEntity entity) {
        return cosmetics;
    }

    private void load() {
        this.executor.submit(() -> {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(this.cacheFile))) {
                ObjectMapper mapper = new ObjectMapper();
                CosmeticaUser user = mapper.readValue(is, CosmeticaUser.class);

                Minecraft.getInstance().execute(() -> {
                    this.cosmetics = PlayerCosmetics.fromUser(user);
                });
            } catch (NoSuchFileException e) {
                // expected on first launch
            } catch (IOException e) {
                Logging.getInstance().error("Failed to read cached player cosmetics", e);
            }
        });
    }

    public void save(CosmeticaUser response) {
        // assume SelfCosmeticManager publishes the event
        Cosmetics loaded = SelfCosmeticsReader.getCosmetics();

        this.executor.submit(() -> {
            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(this.cacheFile))) {
                CosmeticaUser user = new CosmeticaUser();
                // only extract relevant settings
                user.setActiveSettings(response.getActiveSettings()); // may be useful to have a known copy of settings
                user.setExternalCape(response.getExternalCape()); // not used currently
                user.setIcon(response.getIcon());
                user.setLore(response.getLore());
                user.setSkin(response.getSkin()); // not used currently
                user.setUuid(response.getUuid());
                user.setOutfit(response.getOutfit());

                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(os, mapper);
                Logging.getInstance().debug("Cached player cosmetics");
            } catch (IOException e) {
                Logging.getInstance().error("Failed to cache player cosmetics", e);
            }

            // keep images loaded in cache
            List<ResourceLocation> cachedImages = new ArrayList<>();
            loaded.getCloak().ifPresent(ic -> {
                cachedImages.add(ic.getImage().location);
                cachedImages.add(BlockModelManager.getLocation("thumbs-c/" + ic.getId()));
            });
            loaded.getElytra().ifPresent(ic -> {
                cachedImages.add(ic.getImage().location);
                cachedImages.add(BlockModelManager.getLocation("thumbs-c/" + ic.getId()));
            });
            loaded.getLore().ifPresent(ic -> {
                if (ic.getIcon() != NametagConfig.NO_ICON) {
                    cachedImages.add(ic.getIcon().getImage().location);
                    cachedImages.add(BlockModelManager.getLocation("thumbs-c/" + ic.getIcon().getId()));
                }
            });
            for (Accessory accessory : loaded.getAccessories()) {
                cachedImages.add(BlockModelManager.getLocation("accessory/" + accessory.getId()));
                cachedImages.add(BlockModelManager.getLocation("thumbs-c/" + accessory.getId()));
            }
            BlockModelManager.preserveImages(cachedImages);

            // TODO cache models
        });
    }
}
