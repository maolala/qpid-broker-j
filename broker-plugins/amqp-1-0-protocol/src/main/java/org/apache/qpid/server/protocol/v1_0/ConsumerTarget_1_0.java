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
package org.apache.qpid.server.protocol.v1_0;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.server.bytebuffer.QpidByteBuffer;
import org.apache.qpid.server.consumer.AbstractConsumerTarget;
import org.apache.qpid.server.logging.EventLogger;
import org.apache.qpid.server.logging.LogSubject;
import org.apache.qpid.server.logging.messages.ChannelMessages;
import org.apache.qpid.server.message.MessageInstance;
import org.apache.qpid.server.message.MessageInstanceConsumer;
import org.apache.qpid.server.message.ServerMessage;
import org.apache.qpid.server.model.Exchange;
import org.apache.qpid.server.model.Queue;
import org.apache.qpid.server.plugin.MessageConverter;
import org.apache.qpid.server.protocol.MessageConverterRegistry;
import org.apache.qpid.server.protocol.v1_0.messaging.SectionEncoder;
import org.apache.qpid.server.protocol.v1_0.messaging.SectionEncoderImpl;
import org.apache.qpid.server.protocol.v1_0.type.Binary;
import org.apache.qpid.server.protocol.v1_0.type.DeliveryState;
import org.apache.qpid.server.protocol.v1_0.type.Outcome;
import org.apache.qpid.server.protocol.v1_0.type.BaseTarget;
import org.apache.qpid.server.protocol.v1_0.type.UnsignedInteger;
import org.apache.qpid.server.protocol.v1_0.type.codec.AMQPDescribedTypeRegistry;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Accepted;
import org.apache.qpid.server.protocol.v1_0.type.messaging.EncodingRetainingSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Header;
import org.apache.qpid.server.protocol.v1_0.type.messaging.HeaderSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Modified;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Rejected;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Released;
import org.apache.qpid.server.protocol.v1_0.type.transaction.TransactionalState;
import org.apache.qpid.server.protocol.v1_0.type.transport.SenderSettleMode;
import org.apache.qpid.server.protocol.v1_0.type.transport.Transfer;
import org.apache.qpid.server.store.TransactionLogResource;
import org.apache.qpid.server.transport.AMQPConnection;
import org.apache.qpid.server.transport.ProtocolEngine;
import org.apache.qpid.server.txn.AutoCommitTransaction;
import org.apache.qpid.server.txn.ServerTransaction;
import org.apache.qpid.server.util.Action;

class ConsumerTarget_1_0 extends AbstractConsumerTarget<ConsumerTarget_1_0>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerTarget_1_0.class);
    private final boolean _acquires;

    private long _deliveryTag = 0L;

    private Binary _transactionId;
    private final AMQPDescribedTypeRegistry _typeRegistry;
    private SendingLinkEndpoint _linkEndpoint;
    private final SectionEncoder _sectionEncoder;
    private boolean _queueEmpty;

    public ConsumerTarget_1_0(final SendingLinkEndpoint linkEndpoint, boolean acquires)
    {
        super(false, linkEndpoint.getSession().getAMQPConnection());
        _typeRegistry = linkEndpoint.getSession().getConnection().getDescribedTypeRegistry();
        _linkEndpoint = linkEndpoint;
        _sectionEncoder = new SectionEncoderImpl(_typeRegistry);
        _acquires = acquires;
    }

    private SendingLinkEndpoint getEndpoint()
    {
        return _linkEndpoint;
    }

    @Override
    public void updateNotifyWorkDesired()
    {
        boolean state = false;
        Session_1_0 session = _linkEndpoint.getSession();
        if (session != null)
        {
            final AMQPConnection<?> amqpConnection = session.getAMQPConnection();

            state = !amqpConnection.isTransportBlockedForWriting()
                    && _linkEndpoint.isAttached()
                    && getEndpoint().hasCreditToSend();
        }
        setNotifyWorkDesired(state);

    }

    public void doSend(final MessageInstanceConsumer consumer, final MessageInstance entry, boolean batch)
    {
        ServerMessage serverMessage = entry.getMessage();
        Message_1_0 message;
        final MessageConverter<? super ServerMessage, Message_1_0> converter;
        if(serverMessage instanceof Message_1_0)
        {
            converter = null;
            message = (Message_1_0) serverMessage;
        }
        else
        {
            converter =
                    (MessageConverter<? super ServerMessage, Message_1_0>) MessageConverterRegistry.getConverter(serverMessage.getClass(), Message_1_0.class);
            message = converter.convert(serverMessage, _linkEndpoint.getAddressSpace());
        }

        Transfer transfer = new Transfer();
        try
        {
            Collection<QpidByteBuffer> bodyContent = message.getContent(0, (int) message.getSize());
            HeaderSection headerSection = message.getHeaderSection();

            if (entry.getDeliveryCount() != 0)
            {

                Header header = new Header();
                if (headerSection != null)
                {
                    final Header oldHeader = headerSection.getValue();
                    header.setDurable(oldHeader.getDurable());
                    header.setPriority(oldHeader.getPriority());
                    header.setTtl(oldHeader.getTtl());
                }
                header.setDeliveryCount(UnsignedInteger.valueOf(entry.getDeliveryCount()));

                QpidByteBuffer headerPayload = _sectionEncoder.encodeObject(header);

                headerSection = new HeaderSection(_typeRegistry);
                headerSection.setEncodedForm(Collections.singletonList(headerPayload));
            }
            List<QpidByteBuffer> payload = new ArrayList<>();
            if(headerSection != null)
            {
                payload.addAll(headerSection.getEncodedForm());
            }
            EncodingRetainingSection<?> section;
            if((section = message.getDeliveryAnnotationsSection()) != null)
            {
                payload.addAll(section.getEncodedForm());
            }

            if((section = message.getMessageAnnotationsSection()) != null)
            {
                payload.addAll(section.getEncodedForm());
            }

            if((section = message.getPropertiesSection()) != null)
            {
                payload.addAll(section.getEncodedForm());
            }

            if((section = message.getApplicationPropertiesSection()) != null)
            {
                payload.addAll(section.getEncodedForm());
            }

            payload.addAll(bodyContent);

            if((section = message.getFooterSection()) != null)
            {
                payload.addAll(section.getEncodedForm());
            }


            transfer.setPayload(payload);

            for(QpidByteBuffer buf : payload)
            {
                buf.dispose();
            }

            byte[] data = new byte[8];
            ByteBuffer.wrap(data).putLong(_deliveryTag++);
            final Binary tag = new Binary(data);

            transfer.setDeliveryTag(tag);

            if (_linkEndpoint.isAttached())
            {
                if (SenderSettleMode.SETTLED.equals(getEndpoint().getSendingSettlementMode()))
                {
                    transfer.setSettled(true);
                }
                else
                {
                    UnsettledAction action = _acquires
                            ? new DispositionAction(tag, entry, consumer)
                            : new DoNothingAction();

                    _linkEndpoint.addUnsettled(tag, action, entry);
                }

                if (_transactionId != null)
                {
                    TransactionalState state = new TransactionalState();
                    state.setTxnId(_transactionId);
                    transfer.setState(state);
                }
                // TODO - need to deal with failure here
                if (_acquires && _transactionId != null)
                {
                    ServerTransaction txn = _linkEndpoint.getTransaction(_transactionId);
                    if (txn != null)
                    {
                        txn.addPostTransactionAction(new ServerTransaction.Action()
                        {

                            public void postCommit()
                            {
                            }

                            public void onRollback()
                            {
                                entry.release(consumer);
                                _linkEndpoint.updateDisposition(tag, (DeliveryState) null, true);
                            }
                        });
                    }
                    else
                    {
                        // TODO - deal with the case of an invalid txn id
                    }

                }
                getSession().getAMQPConnection().registerMessageDelivered(message.getSize());
                getEndpoint().transfer(transfer, false);
            }
            else
            {
                entry.release(consumer);
            }

        }
        finally
        {
            transfer.dispose();
            if(converter != null)
            {
                converter.dispose(message);
            }
        }
    }

    public void flushBatched()
    {
        // TODO
    }

    /*
        Currently if a queue is deleted the consumer sits there withiout being closed, but
        obviously not receiving any new messages

    public void queueDeleted()
    {
        //TODO
        getEndpoint().setSource(null);
        getEndpoint().close();

        final LinkRegistryModel linkReg = getSession().getConnection()
                .getAddressSpace()
                .getLinkRegistry(getEndpoint().getSession().getConnection().getRemoteContainerId());
        linkReg.unregisterSendingLink(getEndpoint().getName());
    }
      */
    public boolean allocateCredit(final ServerMessage msg)
    {
        ProtocolEngine protocolEngine = getSession().getConnection();
        final boolean hasCredit = _linkEndpoint.isAttached() && getEndpoint().hasCreditToSend();

        updateNotifyWorkDesired();

        if (hasCredit)
        {
            _linkEndpoint.setLinkCredit(_linkEndpoint.getLinkCredit().subtract(UnsignedInteger.ONE));
        }

        return hasCredit;
    }


    public void restoreCredit(final ServerMessage message)
    {
        _linkEndpoint.setLinkCredit(_linkEndpoint.getLinkCredit().add(UnsignedInteger.ONE));
        updateNotifyWorkDesired();
    }

    public void queueEmpty()
    {
        if(_linkEndpoint.drained())
        {
            updateNotifyWorkDesired();
        }
    }

    public void flowStateChanged()
    {
        updateNotifyWorkDesired();

        if (isSuspended() && getEndpoint() != null)
        {
            _transactionId = _linkEndpoint.getTransactionId();
        }
    }

    @Override
    public Session_1_0 getSession()
    {
        return _linkEndpoint.getSession();
    }

    public void flush()
    {
        while(sendNextMessage());
    }

    private class DispositionAction implements UnsettledAction
    {

        private final MessageInstance _queueEntry;
        private final Binary _deliveryTag;
        private final MessageInstanceConsumer _consumer;

        public DispositionAction(Binary tag, MessageInstance queueEntry, final MessageInstanceConsumer consumer)
        {
            _deliveryTag = tag;
            _queueEntry = queueEntry;
            _consumer = consumer;
        }

        public MessageInstanceConsumer getConsumer()
        {
            return _consumer;
        }

        public boolean process(DeliveryState state, final Boolean settled)
        {

            Binary transactionId = null;
            final Outcome outcome;
            ServerTransaction txn;
            // If disposition is settled this overrides the txn?
            if(state instanceof TransactionalState)
            {
                transactionId = ((TransactionalState)state).getTxnId();
                outcome = ((TransactionalState)state).getOutcome();
                txn = _linkEndpoint.getTransaction(transactionId);
                if(txn == null)
                {
                    // TODO - invalid txn id supplied
                }
            }
            else if (state instanceof Outcome)
            {
                outcome = (Outcome) state;
                txn = new AutoCommitTransaction(getSession().getConnection().getAddressSpace().getMessageStore());
            }
            else
            {
                outcome = null;
                txn = null;
            }

            if(outcome instanceof Accepted)
            {
                if (_queueEntry.makeAcquisitionUnstealable(getConsumer()))
                {
                    txn.dequeue(_queueEntry.getEnqueueRecord(),
                                new ServerTransaction.Action()
                                {
                                    @Override
                                    public void postCommit()
                                    {
                                        if (_queueEntry.isAcquiredBy(getConsumer()))
                                        {
                                            _queueEntry.delete();
                                        }
                                    }

                                    @Override
                                    public void onRollback()
                                    {

                                    }
                                });
                }
                txn.addPostTransactionAction(new ServerTransaction.Action()
                    {
                        @Override
                        public void postCommit()
                        {
                            if(Boolean.TRUE.equals(settled))
                            {
                                _linkEndpoint.settle(_deliveryTag);
                            }
                            else
                            {
                                _linkEndpoint.updateDisposition(_deliveryTag, (DeliveryState) outcome, true);
                            }
                            _linkEndpoint.sendFlowConditional();
                        }

                        @Override
                        public void onRollback()
                        {
                            if(Boolean.TRUE.equals(settled))
                            {
                                // TODO: apply source's default outcome
                                applyModifiedOutcome();
                            }
                        }
                    });
            }
            else if(outcome instanceof Released)
            {
                txn.addPostTransactionAction(new ServerTransaction.Action()
                {
                    @Override
                    public void postCommit()
                    {

                        _queueEntry.release(getConsumer());
                        _linkEndpoint.settle(_deliveryTag);
                    }

                    @Override
                    public void onRollback()
                    {
                        _linkEndpoint.settle(_deliveryTag);

                        // TODO: apply source's default outcome if settled
                    }
                });
            }

            else if(outcome instanceof Modified)
            {
                txn.addPostTransactionAction(new ServerTransaction.Action()
                {
                    @Override
                    public void postCommit()
                    {
                        // TODO: add handling of undeliverable-here

                        if(Boolean.TRUE.equals(((Modified)outcome).getDeliveryFailed()))
                        {
                            incrementDeliveryCountOrRouteToAlternateOrDiscard();
                        }
                        else
                        {
                            _queueEntry.release(getConsumer());
                        }
                        _linkEndpoint.settle(_deliveryTag);
                    }

                    @Override
                    public void onRollback()
                    {
                        if(Boolean.TRUE.equals(settled))
                        {
                            // TODO: apply source's default outcome
                            applyModifiedOutcome();
                        }
                    }
                });
            }
            else if (outcome instanceof Rejected)
            {
                txn.addPostTransactionAction(new ServerTransaction.Action()
                {
                    @Override
                    public void postCommit()
                    {
                        _linkEndpoint.settle(_deliveryTag);
                        incrementDeliveryCountOrRouteToAlternateOrDiscard();
                        _linkEndpoint.sendFlowConditional();
                    }

                    @Override
                    public void onRollback()
                    {
                        if(Boolean.TRUE.equals(settled))
                        {
                            // TODO: apply source's default outcome
                            applyModifiedOutcome();
                        }
                    }
                });
            }

            return (transactionId == null && outcome != null);
        }

        private void applyModifiedOutcome()
        {
            final Modified modified = new Modified();
            modified.setDeliveryFailed(true);
            _linkEndpoint.updateDisposition(_deliveryTag, modified, true);
            _linkEndpoint.sendFlowConditional();
            incrementDeliveryCountOrRouteToAlternateOrDiscard();
        }

        private void incrementDeliveryCountOrRouteToAlternateOrDiscard()
        {
            _queueEntry.incrementDeliveryCount();
            if (_queueEntry.getMaximumDeliveryCount() > 0
                && _queueEntry.getDeliveryCount() >= _queueEntry.getMaximumDeliveryCount())
            {
                routeToAlternateOrDiscard();
            }
            else
            {
                _queueEntry.release(getConsumer());
            }
        }

        private void routeToAlternateOrDiscard()
        {
            final Session_1_0 session = _linkEndpoint.getSession();
            final ServerMessage message = _queueEntry.getMessage();
            final EventLogger eventLogger = session.getEventLogger();
            final LogSubject logSubject = session.getLogSubject();
            int requeues = 0;
            if (_queueEntry.makeAcquisitionUnstealable(getConsumer()))
            {
                requeues = _queueEntry.routeToAlternate(new Action<MessageInstance>()
                {
                    @Override
                    public void performAction(final MessageInstance requeueEntry)
                    {

                        eventLogger.message(logSubject,
                                            ChannelMessages.DEADLETTERMSG(message.getMessageNumber(),
                                                                          requeueEntry.getOwningResource().getName()));
                    }
                }, null);
            }

            if (requeues == 0)
            {
                final TransactionLogResource owningResource = _queueEntry.getOwningResource();
                if (owningResource instanceof Queue)
                {
                    final Queue<?> queue = (Queue<?>) owningResource;

                    final Exchange altExchange = queue.getAlternateExchange();

                    if (altExchange == null)
                    {
                        eventLogger.message(logSubject,
                                            ChannelMessages.DISCARDMSG_NOALTEXCH(message.getMessageNumber(),
                                                                                 queue.getName(),
                                                                                 message.getInitialRoutingAddress()));
                    }
                    else
                    {
                        eventLogger.message(logSubject,
                                            ChannelMessages.DISCARDMSG_NOROUTE(message.getMessageNumber(),
                                                                               altExchange.getName()));
                    }
                }
            }
        }
    }

    private class DoNothingAction implements UnsettledAction
    {
        public DoNothingAction()
        {
        }

        public boolean process(final DeliveryState state, final Boolean settled)
        {
            return true;
        }
    }

    @Override
    public void acquisitionRemoved(final MessageInstance node)
    {
    }

    @Override
    public String getTargetAddress()
    {
        BaseTarget target = _linkEndpoint.getTarget();

        return target instanceof org.apache.qpid.server.protocol.v1_0.type.messaging.Target ? ((org.apache.qpid.server.protocol.v1_0.type.messaging.Target) target).getAddress() : _linkEndpoint.getLinkName();
    }

    @Override
    public long getUnacknowledgedBytes()
    {
        // TODO
        return 0;
    }

    @Override
    public long getUnacknowledgedMessages()
    {
        // TODO
        return 0;
    }

    @Override
    public String toString()
    {
        return "ConsumerTarget_1_0[linkSession=" + _linkEndpoint.getSession().toLogString() + "]";
    }
}
