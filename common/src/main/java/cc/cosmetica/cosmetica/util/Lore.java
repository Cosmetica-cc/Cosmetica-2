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

package cc.cosmetica.cosmetica.util;

import cc.cosmetica.core.api.CachedImage;
import gg.cloaks.javaclient.model.UpdateLoreDto;

/**
 * Pojo for lore.
 */
public class Lore {
    public Lore(String text, UpdateLoreDto.ColorEnum colour, CachedImage icon, String service) {
        this.text = text;
        this.colour = colour;
        this.icon = icon;
        this.service = service;
    }

    public Lore old; // used to store previous Lore when pre-emptively showing a new lore
    public final String text;
    public final UpdateLoreDto.ColorEnum colour;
    public final CachedImage icon;
    public final String service;

    public UpdateLoreDto.TypeEnum getType() {
        if (service.isEmpty()) return UpdateLoreDto.TypeEnum.TITLE;
        return PRONOUN_SERVICE.equals(service) ? UpdateLoreDto.TypeEnum.PRONOUNS : UpdateLoreDto.TypeEnum.CONNECTION;
    }

    public static final String PRONOUN_SERVICE = "pronoun";
    public static final Lore NO_LORE = new Lore("", UpdateLoreDto.ColorEnum.WHITE, CachedImage.NO_TEXTURE, "");
}
