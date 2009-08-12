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
package org.apache.qpid.server.logging.actors;

import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.qpid.server.configuration.ServerConfiguration;
import org.apache.qpid.server.logging.LogActor;
import org.apache.qpid.server.logging.LogMessage;
import org.apache.qpid.server.logging.LogSubject;
import org.apache.qpid.server.logging.RootMessageLogger;
import org.apache.qpid.server.logging.RootMessageLoggerImpl;
import org.apache.qpid.server.logging.rawloggers.UnitTestMessageLogger;

import java.util.List;

/**
 * Test : AMQPConnectionActorTest
 * Validate the AMQPConnectionActor class.
 *
 * The test creates a new AMQPActor and then logs a message using it.
 *
 * The test then verifies that the logged message was the only one created and
 * that the message contains the required message.
 */
public class ManagementActorTest extends TestCase
{

    LogActor _amqpActor;
    UnitTestMessageLogger _rawLogger;
    private static final String IP = "127.0.0.1";
    private static final String CONNECTION_ID = "1";
    private String _threadName;

    public void setUp() throws ConfigurationException
    {
        Configuration config = new PropertiesConfiguration();
        ServerConfiguration serverConfig = new ServerConfiguration(config);

        _rawLogger = new UnitTestMessageLogger();
        RootMessageLogger rootLogger =
                new RootMessageLoggerImpl(serverConfig, _rawLogger);

        _amqpActor = new ManagementActor(rootLogger);

        // Set the thread name to be the same as a RMI JMX Connection would use
        _threadName = Thread.currentThread().getName();
        Thread.currentThread().setName("RMI TCP Connection(" + CONNECTION_ID + ")-" + IP);
    }

    public void tearDown()
    {
        Thread.currentThread().setName(_threadName);
        _rawLogger.clearLogMessages();
    }

    /**
     * Test the AMQPActor logging as a Connection level.
     *
     * The test sends a message then verifies that it entered the logs.
     *
     * The log message should be fully repalaced (no '{n}' values) and should
     * not contain any channel identification.
     */
    public void testConnection()
    {
        final String message = "test logging";

        _amqpActor.message(new LogSubject()
        {
            public String toString()
            {
                return "[AMQPActorTest]";
            }

        }, new LogMessage()
        {
            public String toString()
            {
                return message;
            }
        });

        List<Object> logs = _rawLogger.getLogMessages();

        assertEquals("Message log size not as expected.", 1, logs.size());

        // Verify that the logged message is present in the output
        assertTrue("Message was not found in log message",
                   logs.get(0).toString().contains(message));

        // Verify that all the values were presented to the MessageFormatter
        // so we will not end up with '{n}' entries in the log.
        assertFalse("Verify that the string does not contain any '{'.",
                    logs.get(0).toString().contains("{"));

        // Verify that the message has the correct type
        assertTrue("Message contains the [mng: prefix",
                   logs.get(0).toString().contains("[mng:"));

        // Verify that the logged message does not contains the 'ch:' marker
        assertFalse("Message was logged with a channel identifier." + logs.get(0),
                    logs.get(0).toString().contains("/ch:"));

        // Verify that the message has the right values
        assertTrue("Message contains the [mng: prefix",
                   logs.get(0).toString().contains("[mng:" + CONNECTION_ID + "(" + IP + ")"));

    }

}
