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

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.kupe.api.Context;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.SizedElement;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Vec3;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public class DataForwarder extends Component {
    protected DataForwarder() {
    }

    @Override
    public List<Component> build() {
        return ImmutableList.of();
    }

    @Override
    public Dimensions intrinsicSize(List<? extends SizedElement> children, Margins padding, Context context) {
        return Dimensions.NONE;
    }

    @Override
    public Dimensions minimumSize(List<? extends SizedElement> children, Margins padding, int vw, int vh) {
        return Dimensions.NONE;
    }

    public static <U, V> DataForwarder merge(State<Triple<Vec3, U, V>> o,
                                                State<Float> x, State<Float> y, State<Float> z, State<? extends U> u, State<? extends V> v) {
        final class DataForwarder3 extends DataForwarder {
            @Override
            public List<Component> build() {
                float xValue = x.acquire(this);
                float yValue = y.acquire(this);
                float zValue = z.acquire(this);
                U uValue = u.acquire(this);
                V vValue = v.acquire(this);
                o.set(Triple.of(Vec3.of(xValue, yValue, zValue), uValue, vValue));
                return super.build();
            }
        }
        return new DataForwarder3();
    }
}
