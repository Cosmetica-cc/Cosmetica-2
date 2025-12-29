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

package cc.cosmetica.cosmetica.mixin.textures;

import cc.cosmetica.core.api.texture.FrameMetaData;
import cc.cosmetica.core.api.texture.FrameMetadataHolder;
import cc.cosmetica.core.impl.Logging;
import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(targets = "cc.cosmetica.include.twelvemonkeys.imageio.plugins.webp.WebPImageReader", remap = false)
@Pseudo
public class WebpMetadataProviderProdMixin implements FrameMetadataHolder {
    @Override
    public List<FrameMetaData> getFrameMetadata() {
        try {
            Field f = this.getClass().getDeclaredField("frames");
            f.setAccessible(true);
            List<Object> l = (List<Object>) f.get(this);
            return l.stream()
                    .map(obj -> (AnimationFrameProdAccessor)obj)
                    .map(meta -> new FrameMetaData(
                            meta.getBounds(),
                            meta.getBlend(),
                            meta.getDispose()
                    ))
                    .collect(Collectors.toList());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logging.getInstance().error("Error reading webp frame metadata (Prod)", e);
            return ImmutableList.of();
        }
    }

    @Override
    public Optional<int[]> getCanvasDimensions() {
        return Optional.empty();
    }
}
