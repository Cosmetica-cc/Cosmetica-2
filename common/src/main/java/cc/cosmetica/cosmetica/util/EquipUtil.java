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

import cc.cosmetica.core.api.Accessory;
import gg.cloaks.javaclient.model.CreateOutfitAccessoryDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

public final class EquipUtil {
    private EquipUtil() {}

    public static CreateOutfitAccessoryDto dtoFromAccessory(Accessory accessory) {
        CreateOutfitAccessoryDto caod = new CreateOutfitAccessoryDto();
        caod.setId(accessory.getId());
        caod.setMirrored(accessory.isMirrored());
        caod.setOffset(Arrays.asList(
                BigDecimal.valueOf(accessory.getOffset().x),
                BigDecimal.valueOf(accessory.getOffset().y),
                BigDecimal.valueOf(accessory.getOffset().z)
        ));
        // core uses identical collection if no custom override
        // TODO put other way to check in core
        caod.setFlags(accessory.getFlags() == accessory.getDefaultFlags() ? -1 : packFlags(accessory.getFlags()));
        return caod;
    }

    private static int packFlags(Collection<Accessory.Flag> flags) {
        int response = 0;
        for (Accessory.Flag flag : flags) {
            response |= maskOf(flag);
        }
        return response;
    }

    // TODO give access to mask in core
    private static int maskOf(Accessory.Flag flag) {
        switch (flag) {
        case HIDE_WITH_HELMET:
            return 0x1;
        case HIDE_WITH_CHESTPLATE:
            return 0x2;
        case HIDE_WITH_LEGGINGS:
            return 0x4;
        case HIDE_WITH_BOOTS:
            return 0x8;
        case HIDE_WITH_CLOAK:
            return 0x10;
        case HIDE_WITH_ELYTRA:
            return 0x20;
        case HIDE_WITH_PARROT:
            return 0x40;
        default:
            return 0;
        }
    }
}
