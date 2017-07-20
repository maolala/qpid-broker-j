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

package org.apache.qpid.server.protocol.v1_0.store.bdb;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;

import com.google.common.io.Files;
import com.sleepycat.je.CacheMode;

import org.apache.qpid.server.protocol.v1_0.store.LinkStore;
import org.apache.qpid.server.protocol.v1_0.store.LinkStoreTestCase;
import org.apache.qpid.server.store.berkeleydb.BDBEnvironmentContainer;
import org.apache.qpid.server.store.berkeleydb.StandardEnvironmentConfiguration;
import org.apache.qpid.server.store.berkeleydb.StandardEnvironmentFacade;
import org.apache.qpid.server.util.FileUtils;

public class BDBLinkStoreTest extends LinkStoreTestCase
{
    private StandardEnvironmentFacade _facade;
    private File _storeFolder;

    @Override
    protected LinkStore createLinkStore()
    {
        _storeFolder = Files.createTempDir();
        StandardEnvironmentConfiguration configuration = mock(StandardEnvironmentConfiguration.class);
        when(configuration.getName()).thenReturn("test");
        when(configuration.getStorePath()).thenReturn(_storeFolder.getAbsolutePath());
        when(configuration.getCacheMode()).thenReturn(CacheMode.DEFAULT);
        when(configuration.getParameters()).thenReturn(Collections.emptyMap());
       _facade = new StandardEnvironmentFacade(configuration);

        BDBEnvironmentContainer environmentContainer = mock(BDBEnvironmentContainer.class);
        when(environmentContainer.getEnvironmentFacade()).thenReturn(_facade);
        return new BDBLinkStore(environmentContainer);
    }

    @Override
    protected void deleteLinkStore()
    {
        _facade.close();
        FileUtils.delete(_storeFolder, true);
    }
}