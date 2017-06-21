/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.qpid.server.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CachingUUIDFactory
{
    private final Map<UUID, UUID> _uuids = new HashMap<>();

    public UUID createUuidFromString(final String name)
    {
        UUID candidate = UUID.fromString(name);
        return cacheIfNecessary(candidate);
    }

    public UUID createUuidFromBits(final long mostSigBits, final long leastSigBits)
    {
        UUID candidate = new UUID(mostSigBits, leastSigBits);
        return cacheIfNecessary(candidate);
    }

    private UUID cacheIfNecessary(final UUID candidate)
    {
        UUID existing = _uuids.get(candidate);
        if (existing != null)
        {
            return existing;
        }
        else
        {
            _uuids.put(candidate, candidate);
            return candidate;
        }
    }
}