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
package org.apache.qpid.server.protocol.v0_10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.server.transport.ConnectionClose;
import org.apache.qpid.server.transport.ConnectionCloseOk;
import org.apache.qpid.server.transport.ConnectionException;
import org.apache.qpid.server.transport.ConnectionHeartbeat;
import org.apache.qpid.server.transport.Method;
import org.apache.qpid.server.transport.MethodDelegate;
import org.apache.qpid.server.transport.ProtocolDelegate;
import org.apache.qpid.server.transport.ProtocolError;
import org.apache.qpid.server.transport.SessionDetach;
import org.apache.qpid.server.transport.SessionDetachCode;
import org.apache.qpid.server.transport.SessionDetached;

/**
 * ConnectionDelegate
 *
 * @author Rafael H. Schloming
 */

/**
 * Currently only implemented client specific methods
 * the server specific methods are dummy impls for testing
 *
 * the connectionClose is kind of different for both sides
 */
public abstract class ConnectionDelegate
        extends MethodDelegate<Connection>
        implements ProtocolDelegate<Connection>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDelegate.class);

    public void control(Connection conn, Method method)
    {
        method.dispatch(conn, this);
    }

    public void command(Connection conn, Method method)
    {
        method.dispatch(conn, this);
    }

    public void error(Connection conn, ProtocolError error)
    {
        conn.exception(new ConnectionException(error.getMessage()));
    }

    public void handle(Connection conn, Method method)
    {
        conn.dispatch(method);
    }

    @Override public void connectionHeartbeat(Connection conn, ConnectionHeartbeat hearbeat)
    {
        // do nothing
    }

    @Override public void connectionClose(Connection conn, ConnectionClose close)
    {
        sendConnectionCloseOkAndCloseSender(conn);
        conn.closeCode(close);
        conn.setState(Connection.State.CLOSE_RCVD);
    }

    protected void sendConnectionCloseOkAndCloseSender(Connection conn)
    {
        conn.connectionCloseOk();
        conn.getSender().close();
    }

    @Override public void connectionCloseOk(Connection conn, ConnectionCloseOk ok)
    {
        conn.getSender().close();
    }

    @Override public void sessionDetach(Connection conn, SessionDetach dtc)
    {
        Session ssn = conn.getSession(dtc.getChannel());
        ssn.sessionDetached(dtc.getName(), ssn.getDetachCode() == null? SessionDetachCode.NORMAL: ssn.getDetachCode());
        conn.unmap(ssn);
        ssn.closed();
    }

    @Override public void sessionDetached(Connection conn, SessionDetached dtc)
    {
        Session ssn = conn.getSession(dtc.getChannel());
        if (ssn != null)
        {
            ssn.setDetachCode(dtc.getCode());
            conn.unmap(ssn);
            ssn.closed();
        }
    }

    public void writerIdle(final Connection connection)
    {
        connection.doHeartBeat();
    }
}