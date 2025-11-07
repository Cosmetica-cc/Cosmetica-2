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

public final class EquipUtil {
    private EquipUtil() {}

    public static CreateOutfitAccessoryDto dtoFromAccessory(Accessory accessory) {
        CreateOutfitAccessoryDto caod = new CreateOutfitAccessoryDto();
        caod.id(accessory.getId());
        caod.mirrored(accessory.isMirrored());
        caod.setOffset(Arrays.asList(
                BigDecimal.valueOf(accessory.getOffset().x),
                BigDecimal.valueOf(accessory.getOffset().y),
                BigDecimal.valueOf(accessory.getOffset().z)
        ));
        return caod;
    }
}
