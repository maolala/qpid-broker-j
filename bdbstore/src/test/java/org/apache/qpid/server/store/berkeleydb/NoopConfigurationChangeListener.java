/*
 *
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
package org.apache.qpid.server.store.berkeleydb;

import org.apache.qpid.server.model.ConfigurationChangeListener;
import org.apache.qpid.server.model.ConfiguredObject;
import org.apache.qpid.server.model.State;

public class NoopConfigurationChangeListener implements ConfigurationChangeListener
{

    public NoopConfigurationChangeListener() {
    }

    @Override
    public void stateChanged(ConfiguredObject<?> object, State oldState, State newState)
    {
    }

    @Override
    public void childAdded(ConfiguredObject<?> object, ConfiguredObject<?> child)
    {
    }

    @Override
    public void childRemoved(ConfiguredObject<?> object, ConfiguredObject<?> child)
    {
    }

    @Override
    public void attributeSet(ConfiguredObject<?> object, String attributeName, Object oldAttributeValue,
                             Object newAttributeValue)
    {
    }

    @Override
    public void bulkChangeStart(final ConfiguredObject<?> object)
    {

    }

    @Override
    public void bulkChangeEnd(final ConfiguredObject<?> object)
    {

    }
}