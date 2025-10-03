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

package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.cosmetica.util.Thumbnail;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.cloaks.javaclient.model.Cosmetic;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Cosmetic.class)
public class JCACosmeticMixin implements Thumbnail {
    private static final String JSON_PROPERTY_THUMBNAIL = "thumbnail";
    private String thumbnail;

    @Override
    @JsonProperty(JSON_PROPERTY_THUMBNAIL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setThumbnail(String thumb) {
        thumbnail = thumb;
    }

    @Override
    @JsonProperty(JSON_PROPERTY_THUMBNAIL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getThumbnail() {
        return thumbnail;
    }
}
