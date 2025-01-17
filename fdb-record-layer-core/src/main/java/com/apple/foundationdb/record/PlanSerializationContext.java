/*
 * PlanSerializationContext.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2023 Apple Inc. and the FoundationDB project authors
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

package com.apple.foundationdb.record;

import com.apple.foundationdb.annotation.API;
import com.apple.foundationdb.record.PlanHashable.PlanHashMode;
import com.apple.foundationdb.record.RecordQueryPlanProto.PPlanReference;
import com.apple.foundationdb.record.query.plan.cascades.IdentityBiMap;
import com.apple.foundationdb.record.query.plan.plans.RecordQueryPlan;
import com.apple.foundationdb.record.query.plan.serialization.DefaultPlanSerializationRegistry;
import com.apple.foundationdb.record.query.plan.serialization.PlanSerializationRegistry;
import com.google.common.base.Verify;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Context class defining the state of serialization/deserialization currently in-flight.
 */
@API(API.Status.INTERNAL)
public class PlanSerializationContext {

    @Nonnull
    private final PlanSerializationRegistry registry;
    @Nonnull
    private final PlanHashMode mode;

    @Nonnull
    private final IdentityBiMap<RecordQueryPlan, Integer> knownPlansMap;

    public PlanSerializationContext(@Nonnull final PlanSerializationRegistry registry,
                                    @Nonnull final PlanHashMode mode) {
        this(registry, mode, IdentityBiMap.create());
    }

    public PlanSerializationContext(@Nonnull final PlanSerializationRegistry registry,
                                    @Nonnull final PlanHashMode mode,
                                    @Nonnull final IdentityBiMap<RecordQueryPlan, Integer> knownPlansMap) {
        this.registry = registry;
        this.mode = mode;
        this.knownPlansMap = knownPlansMap;
    }

    @Nonnull
    public PlanSerializationRegistry getRegistry() {
        return registry;
    }

    @Nonnull
    public PlanHashMode getMode() {
        return mode;
    }

    @Nonnull
    public static PlanSerializationContext newForCurrentMode() {
        return newForCurrentMode(DefaultPlanSerializationRegistry.INSTANCE);
    }

    @Nonnull
    public static PlanSerializationContext newForCurrentMode(@Nonnull final PlanSerializationRegistry registry) {
        return new PlanSerializationContext(registry, PlanHashable.CURRENT_FOR_CONTINUATION);
    }

    @Nonnull
    public PPlanReference toPlanReferenceProto(@Nonnull final RecordQueryPlan recordQueryPlan) {
        if (knownPlansMap.containsKeyUnwrapped(recordQueryPlan)) {
            //
            // Plan has already been visited -- just set the reference.
            //
            int referenceId = Objects.requireNonNull(knownPlansMap.getUnwrapped(recordQueryPlan));
            return PPlanReference.newBuilder().setReferenceId(referenceId).build();
        }

        //
        // First time the plan is being visited, set the reference and the message.
        //
        final int referenceId = knownPlansMap.size();
        knownPlansMap.putUnwrapped(recordQueryPlan, referenceId);
        return PPlanReference.newBuilder()
                .setReferenceId(referenceId)
                .setRecordQueryPlan(recordQueryPlan.toRecordQueryPlanProto(this))
                .build();
    }

    @Nonnull
    public RecordQueryPlan fromPlanReferenceProto(@Nonnull final PPlanReference planReferenceProto) {
        final IdentityBiMap<Integer, RecordQueryPlan> inverse = knownPlansMap.inverse();
        Verify.verify(planReferenceProto.hasReferenceId());
        final int referenceId = planReferenceProto.getReferenceId();
        if (inverse.containsKeyUnwrapped(referenceId)) {
            return Objects.requireNonNull(inverse.getUnwrapped(referenceId));
        }
        final RecordQueryPlan recordQueryPlan = RecordQueryPlan.fromRecordQueryPlanProto(this, Objects.requireNonNull(planReferenceProto.getRecordQueryPlan()));
        Verify.verify(knownPlansMap.putUnwrapped(recordQueryPlan, referenceId) == null);
        return recordQueryPlan;
    }
}
