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

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * "Sniper, no sniping!"
 */
public class Sniper {
    private static LivingEntity target;
    private static final float MAX_SNIPE_DISTANCE = 64.0f;

    @Nullable
    public static LivingEntity getTarget() {
        return target;
    }

    public static void updateTargetPlayer(Minecraft minecraft, float yawProbably) {
        Entity camera = minecraft.getCameraEntity();
        target = null;

        if (camera != null) {
            if (minecraft.level != null) {
                Profiler.get().push("snipe");

                final double maxDist = MAX_SNIPE_DISTANCE;
                HitResult pickResult = camera.pick(maxDist, yawProbably, false);
                Vec3 eyePosition = camera.getEyePosition(yawProbably);

                double maxDistSqr = maxDist;
                maxDistSqr *= maxDistSqr;

                if (pickResult != null) {
                    maxDistSqr = pickResult.getLocation().distanceToSqr(eyePosition);
                }

                Vec3 view = camera.getViewVector(1.0F);
                Vec3 castTowards = eyePosition.add(view.x * maxDist, view.y * maxDist, view.z * maxDist);

                final float inflation = 1.0F;
                AABB selectionBoundingBox = camera.getBoundingBox().expandTowards(view.scale(maxDist)).inflate(inflation, inflation, inflation);
                EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(camera, eyePosition, castTowards, selectionBoundingBox, e -> !e.isSpectator() && e.isPickable(), maxDistSqr);

                if (entityHitResult != null) {
                    Entity hitEntity = entityHitResult.getEntity();
                    Vec3 resultLocation = entityHitResult.getLocation();
                    double distance = eyePosition.distanceToSqr(resultLocation);

                    if (distance < maxDistSqr || pickResult == null) {
                        if (hitEntity instanceof LivingEntity) { // vanilla crosshair pick: entity2 instanceof LivingEntity || entity2 instanceof ItemFrame
                            target = (LivingEntity) hitEntity;
                        }
                    }
                }

                Profiler.get().pop();
            }
        }
    }
}
