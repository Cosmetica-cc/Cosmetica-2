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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pojo for lore.
 */
public class Lore {
    public Lore(String text, UpdateLoreDto.ColorEnum colour, @NotNull CachedImage icon, String service) {
        this(text, text, colour, icon, service);
    }

    public Lore(String value, String display, UpdateLoreDto.ColorEnum colour, @NotNull CachedImage icon, String service) {
        Objects.requireNonNull(icon, "Icon cannot be null! Use NO_TEXTURE.");
        this.value = value;
        this.displayText = display;
        this.colour = colour;
        this.icon = icon;
        this.service = service;
    }

    public Lore old; // used to store previous Lore when pre-emptively showing a new lore
    /**
     * The lore value. Used when setting the lore.
     */
    public final String value;
    /**
     * The lore display text. Purely visual for preview.
     */
    public final String displayText;
    public final UpdateLoreDto.ColorEnum colour;
    public final CachedImage icon;
    public final String service;

    public boolean isNoLore() {
        return this.value.isEmpty();
    }

    public String formatted() {
        String c;
        switch (this.colour) {
            case BLACK: c = "0"; break;
            case DARK_BLUE: c = "1"; break;
            case DARK_GREEN: c = "2"; break;
            case DARK_AQUA: c = "3"; break;
            case DARK_RED: c = "4"; break;
            case DARK_PURPLE: c = "5"; break;
            case GOLD: c = "6"; break;
            case GRAY: c = "7"; break;
            case DARK_GRAY: c = "8"; break;
            case BLUE: c = "9"; break;
            case GREEN: c = "a"; break;
            case AQUA: c = "b"; break;
            case RED: c = "c"; break;
            case LIGHT_PURPLE: c = "d"; break;
            case YELLOW: c = "e"; break;
            case WHITE:
            default:
                c = "f"; break;
        }
        return "§" + c + this.displayText;
    }

    public UpdateLoreDto.TypeEnum getType() {
        if (service.isEmpty()) return UpdateLoreDto.TypeEnum.TITLE;
        return PRONOUN_SERVICE.equals(service) ? UpdateLoreDto.TypeEnum.PRONOUNS : UpdateLoreDto.TypeEnum.CONNECTION;
    }

    public static final String PRONOUN_SERVICE = "pronoun";

    /**
     * Create a new 'no lore' lore.
     * @return a new instance of an empty lore.
     */
    public static Lore none(UpdateLoreDto.ColorEnum colour) {
        return new Lore("", UpdateLoreDto.ColorEnum.WHITE, CachedImage.NO_TEXTURE, "");
    }
}
