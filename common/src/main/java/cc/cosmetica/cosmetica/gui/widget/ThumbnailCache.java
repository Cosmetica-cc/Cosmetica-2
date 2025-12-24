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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.api.texture.CosmeticaTexture;

/**
 * Preserves the most recently accessed thumbnails in an array, so they won't have to be reloaded from disk/internet.
 */
public final class ThumbnailCache {
    private ThumbnailCache() {
    }

    public static CachedImage getOrCreateImage(CosmeticaTexture.Builder texture, boolean useBrowseCache) {
        CachedImage image = CosmeticaModel.getOrCreateCosmeticaImage(texture);

        if (useBrowseCache) {
            for (CachedImage i : browseCache) {
                if (i == image) {
                    return image;
                }
            }
            // add to cache
            browseCache[nextBrowse] = image;
            nextBrowse = (nextBrowse + 1) & 63;
            return image;
        } else {
            for (CachedImage i : cache) {
                if (i == image) {
                    return image;
                }
            }
            // add to cache
            cache[next] = image;
            next = (next + 1) & 0xF;
            return image;
        }
    }

    private static CachedImage[] cache = new CachedImage[16];
    private static CachedImage[] browseCache = new CachedImage[64];
    private static int next = 0;
    private static int nextBrowse = 0;
}
