/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.repair;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.apache.cassandra.concurrent.ExecutorPlus;
import org.apache.cassandra.locator.InetAddressAndPort;
import org.apache.cassandra.repair.consistent.CoordinatorSession;
import org.apache.cassandra.utils.concurrent.Future;

public class IncrementalRepairTask extends AbstractRepairTask
{
    protected IncrementalRepairTask(RepairCoordinator coordinator)
    {
        super(coordinator);
    }

    @Override
    public String name()
    {
        return "Repair";
    }

    @Override
    public Future<CoordinatedRepairResult> performUnsafe(ExecutorPlus executor, Scheduler validationScheduler) throws Exception
    {
        // the local node also needs to be included in the set of participants, since coordinator sessions aren't persisted
        Set<InetAddressAndPort> allParticipants = ImmutableSet.<InetAddressAndPort>builder()
                                                  .addAll(coordinator.neighborsAndRanges.participants)
                                                  .add(broadcastAddressAndPort)
                                                  .build();

        // Not necessary to include self for filtering. The common ranges only contains neighbhor node endpoints.
        List<CommonRange> allRanges = coordinator.neighborsAndRanges.filterCommonRanges(keyspace, coordinator.getColumnFamilyNames());

        CoordinatorSession coordinatorSession = coordinator.ctx.repair().consistent.coordinated.registerSession(coordinator.state.id, allParticipants, coordinator.neighborsAndRanges.excludedDeadParticipants);

        return coordinatorSession.execute(() -> runRepair(coordinator.state.id, true, executor, validationScheduler, allRanges, coordinator.neighborsAndRanges.excludedDeadParticipants, coordinator.getColumnFamilyNames().toArray(new String[0])));

    }
}
