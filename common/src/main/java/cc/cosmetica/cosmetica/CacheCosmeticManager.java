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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetic;
import cc.cosmetica.core.api.*;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import gg.cloaks.javaclient.model.Icon;
import gg.cloaks.javaclient.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cc.cosmetica.core.api.NametagConfig.NO_ICON;

public class CacheCosmeticManager implements CosmeticManager {
    public CacheCosmeticManager(Path directory, UserIO userIO) {
        this.directory = directory;
        this.outfitCache = directory.resolve("outfit.json");
        this.userIO = userIO;
        this.load();
    }

    private final Path directory, outfitCache;
    private final UserIO userIO;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("Cache Cosmetic Manager");
        return t;
    });
    private Cosmetics cosmetics;

    @Override
    public boolean canManage(Either entity) {
        return entity.entity instanceof LocalPlayer && cosmetics != null;
    }

    @Override
    public Cosmetics getCosmetics(Either entity) {
        return cosmetics;
    }

    private void load() {
        this.executor.submit(() -> {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(this.outfitCache))) {
                Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Reading offline cache outfit json");

                CosmeticaUser user = this.userIO.read(is);
                this.loadUser(user);
            } catch (NoSuchFileException e) {
                // expected on first launch
                Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "No cached player cosmetics for self yet.");
            } catch (IOException e) {
                Logging.getInstance().error("Failed to read cached player cosmetics", e);
            }
        });
    }

    private void loadUser(CosmeticaUser user) {
                Minecraft.getInstance().execute(() -> {
                    Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Transforming offline outfit json to outfit");
                    @Nullable Outfit outfit = user.getOutfit();
                    @Nullable Icon icon = user.getIcon();
                    @Nullable Lore lore = user.getLore();

                    // nametag and lore
                    ImageCosmetic iconImage = icon == null ? NO_ICON : ImageCosmetic.fromIcon(icon);
                    NametagConfig nametag = new NametagConfig(
                            user.getPrefix() == null ? "" : user.getPrefix(),
                            user.getSuffix() == null ? "" : user.getSuffix(),
                            iconImage,
                            false);
                    NametagConfig loreNametag = lore == null ? null : new NametagConfig(
                                lore.getFormatted().replaceAll("&", "§"), "",
                                lore.getIconUrl() == null ? NO_ICON : new ImageCosmetic(
                                        CosmeticaModel.getOrCreateCosmeticaImage(new CosmeticaTexture.Builder(lore.getIconUrl(), BlockModelManager.FALLBACK_TEXTURE)),
                                        lore.getService(),
                                        lore.getService(), // use service as id as well
                                        null,
                                        lore.getIconUrl(),
                                        0), false);

                    // outfit
                    List<Accessory> accessories = new ArrayList<>();
                    @Nullable String outfitName = null;
                    @Nullable String outfitId = null;
                    @Nullable ImageCosmetic cloak = null;
                    @Nullable ImageCosmetic elytra = null;

                    if (outfit != null) {
                        outfitName = outfit.getName();
                        outfitId = outfit.getId();

                        @Nullable AnimatedTextureCosmetic apiCloak = outfit.getCloak();
                        @Nullable AnimatedTextureCosmetic apiElytra = outfit.getElytra();
                        @Nullable ExternalCape externalCape = user.getExternalCape();

                        if (apiCloak != null) {
                            cloak = ImageCosmetic.fromAPI(apiCloak);
                        } else if (externalCape != null) {
                            cloak = ImageCosmetic.fromExternalCape(externalCape);
                        }
                        if (apiElytra != null) {
                            elytra = ImageCosmetic.fromAPI(apiElytra);
                        } else if (externalCape != null && externalCape.isHasElytra()) {
                            elytra = ImageCosmetic.fromExternalCape(externalCape);
                        }

                        // equip acessories
                        for (OutfitAccessory accessory : outfit.getAccessories()) {
                            // See: Accessory.fromOutfitAccessory
                            CosmeticaModel model = CosmeticaModel.getOrCreateModel(
                                    accessory.getAccessory().getId(),
                                    "textures",
                                    CosmeticaModel.textureId(accessory.getAccessory().getTexture()),
                                    () -> Files.newInputStream(this.directory.resolve(accessory.getAccessory().getId() + ".json")),
                                    accessory.getAccessory().getTexture(),
                                    accessory.getAccessory().getTicksPerFrame().intValue(),
                                    accessory.getAccessory().getFrames().intValue()
                            );

                            List<BigDecimal> offset = accessory.getOffset();

                            accessories.add(new Accessory(
                                    accessory.getAccessory(),
                                    Cosmetic.gameProfileOf(accessory.getAccessory().getCreator()),
                                    accessory.getFlags().intValue() == -1 ? OptionalInt.empty() : OptionalInt.of(accessory.getFlags().intValue()),
                                    accessory.isMirrored(),
                                    model,
                                    Accessory.attachmentTransform(
                                            accessory.getAccessory().getAttachment(),
                                            offset.get(0).doubleValue(),
                                            offset.get(1).doubleValue(),
                                            offset.get(2).doubleValue()
                                    )
                            ));
                        }
                    } else {
                        // this should probably never trigger
                        @Nullable ExternalCape externalCape = user.getExternalCape();

                        if (externalCape != null) {
                            cloak = ImageCosmetic.fromExternalCape(externalCape);
                        }
                        if (externalCape != null && externalCape.isHasElytra()) {
                            elytra = ImageCosmetic.fromExternalCape(externalCape);
                        }
                    }

                    Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Loaded offline cosmetics cache.");
                    this.cosmetics = new PlayerCosmetics(
                            cloak, elytra, accessories,
                            outfitName, outfitId,
                            nametag, loreNametag, user.isUpsideDown()
                    );
                });
    }

    public void save(CosmeticaUser response) {
        // assume SelfCosmeticManager publishes the event
        @NotNull Cosmetics loaded = SelfCosmeticManager.getCosmetics().orElse(NoneCosmetics.NONE);

        this.executor.submit(() -> {
            Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Caching player cosmetics for offline use");

            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(this.outfitCache))) {
                CosmeticaUser user = new CosmeticaUser();
                // only extract relevant settings
                user.setActiveSettings(response.getActiveSettings()); // may be useful to have a known copy of settings
                user.setExternalCape(response.getExternalCape());
                user.setIcon(response.getIcon());
                user.setLore(response.getLore());
                user.setSkin(response.getSkin()); // not used currently
                user.setUuid(response.getUuid());
                user.setOutfit(response.getOutfit());
                user.setUpsideDown(response.isUpsideDown());
                user.setModpackId(response.getModpackId());
                user.setPrefix(response.getPrefix());
                user.setSuffix(response.getSuffix());

                this.userIO.write(user, os);
                Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Cached player cosmetics");

                this.loadUser(user);
                Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Updated loaded cache cosmetics");
            } catch (IOException e) {
                Logging.getInstance().error("Failed to cache player cosmetics", e);
            }

            // keep images loaded in cache
            List<ResourceLocation> cachedImages = new ArrayList<>();
            loaded.getCloak().ifPresent(ic -> {
                cachedImages.add(ic.getImage().location);

                if (ic.getThumbnail().isPresent()) {
                    cachedImages.add(BlockModelManager.getLocation("textures/" + CosmeticaModel.textureId(ic.getThumbnail().get())));
                }
            });
            loaded.getElytra().ifPresent(ic -> {
                cachedImages.add(ic.getImage().location);

                if (ic.getThumbnail().isPresent()) {
                    cachedImages.add(BlockModelManager.getLocation("textures/" + CosmeticaModel.textureId(ic.getThumbnail().get())));
                }
            });
            loaded.getLore().ifPresent(ic -> {
                if (ic.getIcon() != NametagConfig.NO_ICON) {
                    cachedImages.add(ic.getIcon().getImage().location);

                    if (ic.getIcon().getThumbnail().isPresent()) {
                        cachedImages.add(BlockModelManager.getLocation("textures/" + CosmeticaModel.textureId(ic.getIcon().getThumbnail().get())));
                    }
                }
            });
            for (Accessory accessory : loaded.getAccessories()) {
                cachedImages.add(BlockModelManager.getLocation("textures/" + CosmeticaModel.textureId(accessory.getJsonObject().getTexture())));

                if (accessory.getThumbnail().isPresent()) {
                    cachedImages.add(BlockModelManager.getLocation("textures/" + CosmeticaModel.textureId(accessory.getThumbnail().get())));
                }
            }
            BlockModelManager.preserveImages(cachedImages);

            // cache models
            Outfit outfit = response.getOutfit();
            if (outfit != null) {
                for (OutfitAccessory oa : outfit.getAccessories()) {
                    final String modelURL = oa.getAccessory().getModel();
                    final Path output = this.directory.resolve(oa.getAccessory().getId() + ".json");

                    CosmeticaAPI.downloadAsync(modelURL).thenAcceptAsync(model -> {
                        try {
                            Files.write(output, model.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            Logging.getInstance().error("Failed to cache model from {}", e, modelURL);
                        }
                    }, executor);
                }
            }
            // clear old cached models
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.directory, "*.json")) {
                Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
                int count = 0;

                for (Path entry : stream) {
                    if (!Files.isRegularFile(entry)) continue;
                    // don't delete the outfit cache
                    if (Files.isSameFile(entry, this.outfitCache)) continue;;

                    BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class);

                    if (attributes.lastModifiedTime().toInstant().isBefore(oneHourAgo)) {
                        Files.delete(entry);
                        count++;
                    }
                }

                Logging.getInstance().debug(CosmeticaLogCategory.CACHE, "Deleted " + count + " old cached models");
            } catch (IOException e) {
                Logging.getInstance().error("Error clearing old cached models", e);
            }
        });
    }

    /**
     * Read/write cosmetic user to a stream.
     * Since dependencies are remapped in core's shadow, but only for fabric/forge,
     * we need this for ObjectMapper to work in dev.
     */
    public interface UserIO {
        CosmeticaUser read(InputStream is) throws IOException;
        void write(CosmeticaUser user, OutputStream os) throws IOException;
    }
}
