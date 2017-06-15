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
package org.apache.qpid.server.queue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.server.model.AlternateBinding;
import org.apache.qpid.server.model.ConfiguredObject;
import org.apache.qpid.server.model.OverflowPolicy;
import org.apache.qpid.server.model.Queue;
import org.apache.qpid.server.util.ConnectionScopedRuntimeException;

public class QueueArgumentsConverter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueArgumentsConverter.class);

    public static final String X_QPID_FLOW_RESUME_CAPACITY = "x-qpid-flow-resume-capacity";
    public static final String X_QPID_CAPACITY = "x-qpid-capacity";
    public static final String X_QPID_MINIMUM_ALERT_REPEAT_GAP = "x-qpid-minimum-alert-repeat-gap";
    public static final String X_QPID_MAXIMUM_MESSAGE_COUNT = "x-qpid-maximum-message-count";
    public static final String X_QPID_MAXIMUM_MESSAGE_SIZE = "x-qpid-maximum-message-size";
    public static final String X_QPID_MAXIMUM_MESSAGE_AGE = "x-qpid-maximum-message-age";
    public static final String X_QPID_MAXIMUM_QUEUE_DEPTH = "x-qpid-maximum-queue-depth";

    public static final String QPID_ALERT_COUNT = "qpid.alert_count";
    public static final String QPID_ALERT_SIZE = "qpid.alert_size";
    public static final String QPID_ALERT_REPEAT_GAP = "qpid.alert_repeat_gap";

    public static final String X_QPID_PRIORITIES = "x-qpid-priorities";

    public static final String X_QPID_DESCRIPTION = "x-qpid-description";

    public static final String QPID_LAST_VALUE_QUEUE_KEY = "qpid.last_value_queue_key";

    public static final String QPID_QUEUE_SORT_KEY = "qpid.queue_sort_key";
    public static final String X_QPID_DLQ_ENABLED = "x-qpid-dlq-enabled";
    public static final String X_QPID_MAXIMUM_DELIVERY_COUNT = "x-qpid-maximum-delivery-count";
    public static final String QPID_GROUP_HEADER_KEY = "qpid.group_header_key";
    public static final String QPID_SHARED_MSG_GROUP = "qpid.shared_msg_group";
    public static final String QPID_DEFAULT_MESSAGE_GROUP_ARG = "qpid.default-message-group";

    public static final String QPID_MESSAGE_DURABILITY = "qpid.message_durability";

    public static final String QPID_LAST_VALUE_QUEUE = "qpid.last_value_queue";

    public static final String QPID_DEFAULT_FILTERS = "qpid.default_filters";

    public static final String QPID_ENSURE_NONDESTRUCTIVE_CONSUMERS = "qpid.ensure_nondestructive_consumers";

    public static final String QPID_EXCLUSIVITY_POLICY = "qpid.exclusivity_policy";
    public static final String QPID_LIFETIME_POLICY = "qpid.lifetime_policy";

    public static final String QPID_POLICY_TYPE = "qpid.policy_type";
    public static final String QPID_MAX_COUNT = "qpid.max_count";
    public static final String QPID_MAX_SIZE = "qpid.max_size";

    /**
     * No-local queue argument is used to support the no-local feature of Durable Subscribers.
     */
    public static final String QPID_NO_LOCAL = "no-local";

    static final Map<String, String> ATTRIBUTE_MAPPINGS = new LinkedHashMap<String, String>();

    private static final String ALTERNATE_EXCHANGE = "alternateExchange";
    private static final String DEFAULT_DLQ_NAME_SUFFIX = "_DLQ";
    private static String PROPERTY_DEAD_LETTER_QUEUE_SUFFIX = "qpid.broker_dead_letter_queue_suffix";

    static
    {
        ATTRIBUTE_MAPPINGS.put(X_QPID_MINIMUM_ALERT_REPEAT_GAP, Queue.ALERT_REPEAT_GAP);
        ATTRIBUTE_MAPPINGS.put(X_QPID_MAXIMUM_MESSAGE_AGE, Queue.ALERT_THRESHOLD_MESSAGE_AGE);
        ATTRIBUTE_MAPPINGS.put(X_QPID_MAXIMUM_MESSAGE_SIZE, Queue.ALERT_THRESHOLD_MESSAGE_SIZE);

        ATTRIBUTE_MAPPINGS.put(X_QPID_MAXIMUM_MESSAGE_COUNT, Queue.ALERT_THRESHOLD_QUEUE_DEPTH_MESSAGES);
        ATTRIBUTE_MAPPINGS.put(X_QPID_MAXIMUM_QUEUE_DEPTH, Queue.ALERT_THRESHOLD_QUEUE_DEPTH_BYTES);
        ATTRIBUTE_MAPPINGS.put(QPID_ALERT_COUNT, Queue.ALERT_THRESHOLD_QUEUE_DEPTH_MESSAGES);
        ATTRIBUTE_MAPPINGS.put(QPID_ALERT_SIZE, Queue.ALERT_THRESHOLD_QUEUE_DEPTH_BYTES);
        ATTRIBUTE_MAPPINGS.put(QPID_ALERT_REPEAT_GAP, Queue.ALERT_REPEAT_GAP);

        ATTRIBUTE_MAPPINGS.put(X_QPID_MAXIMUM_DELIVERY_COUNT, Queue.MAXIMUM_DELIVERY_ATTEMPTS);

        ATTRIBUTE_MAPPINGS.put(X_QPID_CAPACITY, Queue.MAXIMUM_QUEUE_DEPTH_BYTES);

        ATTRIBUTE_MAPPINGS.put(QPID_QUEUE_SORT_KEY, SortedQueue.SORT_KEY);
        ATTRIBUTE_MAPPINGS.put(QPID_LAST_VALUE_QUEUE_KEY, LastValueQueue.LVQ_KEY);
        ATTRIBUTE_MAPPINGS.put(X_QPID_PRIORITIES, PriorityQueue.PRIORITIES);

        ATTRIBUTE_MAPPINGS.put(X_QPID_DESCRIPTION, Queue.DESCRIPTION);

        ATTRIBUTE_MAPPINGS.put(QPID_GROUP_HEADER_KEY, Queue.MESSAGE_GROUP_KEY);
        ATTRIBUTE_MAPPINGS.put(QPID_DEFAULT_MESSAGE_GROUP_ARG, Queue.MESSAGE_GROUP_DEFAULT_GROUP);

        ATTRIBUTE_MAPPINGS.put(QPID_NO_LOCAL, Queue.NO_LOCAL);
        ATTRIBUTE_MAPPINGS.put(QPID_MESSAGE_DURABILITY, Queue.MESSAGE_DURABILITY);
        ATTRIBUTE_MAPPINGS.put(QPID_DEFAULT_FILTERS, Queue.DEFAULT_FILTERS);
        ATTRIBUTE_MAPPINGS.put(QPID_ENSURE_NONDESTRUCTIVE_CONSUMERS, Queue.ENSURE_NONDESTRUCTIVE_CONSUMERS);

        ATTRIBUTE_MAPPINGS.put(QPID_EXCLUSIVITY_POLICY, Queue.EXCLUSIVE);
        ATTRIBUTE_MAPPINGS.put(QPID_LIFETIME_POLICY, Queue.LIFETIME_POLICY);

        ATTRIBUTE_MAPPINGS.put(QPID_POLICY_TYPE, Queue.OVERFLOW_POLICY);
        ATTRIBUTE_MAPPINGS.put(QPID_MAX_COUNT, Queue.MAXIMUM_QUEUE_DEPTH_MESSAGES);
        ATTRIBUTE_MAPPINGS.put(QPID_MAX_SIZE, Queue.MAXIMUM_QUEUE_DEPTH_BYTES);
    }


    public static Map<String,Object> convertWireArgsToModel(final String queueName,
                                                            Map<String, Object> wireArguments)
    {
        Map<String,Object> modelArguments = new HashMap<String, Object>();
        if(wireArguments != null)
        {
            for(Map.Entry<String,String> entry : ATTRIBUTE_MAPPINGS.entrySet())
            {
                if(wireArguments.containsKey(entry.getKey()))
                {
                    modelArguments.put(entry.getValue(), wireArguments.get(entry.getKey()));
                }
            }
            if(wireArguments.containsKey(QPID_LAST_VALUE_QUEUE) && !wireArguments.containsKey(QPID_LAST_VALUE_QUEUE_KEY))
            {
                modelArguments.put(LastValueQueue.LVQ_KEY, LastValueQueue.DEFAULT_LVQ_KEY);
            }
            if(wireArguments.containsKey(QPID_POLICY_TYPE))
            {
                modelArguments.put(Queue.OVERFLOW_POLICY, OverflowPolicy.valueOf(String.valueOf(wireArguments.get(QPID_POLICY_TYPE)).toUpperCase()));
            }

            if(wireArguments.containsKey(QPID_SHARED_MSG_GROUP))
            {
                modelArguments.put(Queue.MESSAGE_GROUP_SHARED_GROUPS,
                                   AbstractQueue.SHARED_MSG_GROUP_ARG_VALUE.equals(String.valueOf(wireArguments.get(QPID_SHARED_MSG_GROUP))));
            }


            if(wireArguments.get(QPID_NO_LOCAL) != null)
            {
                modelArguments.put(Queue.NO_LOCAL, Boolean.parseBoolean(wireArguments.get(QPID_NO_LOCAL).toString()));
            }

            if (wireArguments.get(X_QPID_FLOW_RESUME_CAPACITY) != null && wireArguments.get(X_QPID_CAPACITY) != null)
            {
                double resumeCapacity = Integer.parseInt(wireArguments.get(X_QPID_FLOW_RESUME_CAPACITY).toString());
                double maximumCapacity = Integer.parseInt(wireArguments.get(X_QPID_CAPACITY).toString());
                if (resumeCapacity > maximumCapacity)
                {
                    throw new ConnectionScopedRuntimeException(
                            "Flow resume size can't be greater than flow control size");
                }
                Map<String, String> context = (Map<String, String>) modelArguments.get(Queue.CONTEXT);
                if (context == null)
                {
                    context = new HashMap<>();
                    modelArguments.put(Queue.CONTEXT, context);
                }
                double ratio = resumeCapacity / maximumCapacity;
                context.put(Queue.QUEUE_FLOW_RESUME_LIMIT, String.format("%.2f", ratio * 100.0));
                modelArguments.put(Queue.OVERFLOW_POLICY, OverflowPolicy.PRODUCER_FLOW_CONTROL);
            }

            if (wireArguments.get(ALTERNATE_EXCHANGE) != null)
            {
                modelArguments.put(Queue.ALTERNATE_BINDING,
                                   Collections.singletonMap(AlternateBinding.DESTINATION,
                                                            wireArguments.get(ALTERNATE_EXCHANGE)));
            }
            else if (wireArguments.containsKey(X_QPID_DLQ_ENABLED))
            {
                Object argument = wireArguments.get(X_QPID_DLQ_ENABLED);
                if ((argument instanceof Boolean && ((Boolean) argument).booleanValue())
                    || (argument instanceof String && Boolean.parseBoolean((String)argument)))
                {
                    modelArguments.put(Queue.ALTERNATE_BINDING,
                                       Collections.singletonMap(AlternateBinding.DESTINATION,
                                                                getDeadLetterQueueName(queueName)));
                }
            }
        }
        return modelArguments;
    }


    public static Map<String,Object> convertModelArgsToWire(Map<String,Object> modelArguments)
    {
        Map<String,Object> wireArguments = new HashMap<String, Object>();
        for(Map.Entry<String,String> entry : ATTRIBUTE_MAPPINGS.entrySet())
        {
            if(modelArguments.containsKey(entry.getValue()))
            {
                Object value = modelArguments.get(entry.getValue());
                if(value instanceof Enum)
                {
                    value = ((Enum) value).name();
                }
                else if(value instanceof ConfiguredObject)
                {
                    value = ((ConfiguredObject)value).getName();
                }
                wireArguments.put(entry.getKey(), value);
            }
        }

        if(Boolean.TRUE.equals(modelArguments.get(Queue.MESSAGE_GROUP_SHARED_GROUPS)))
        {
            wireArguments.put(QPID_SHARED_MSG_GROUP, AbstractQueue.SHARED_MSG_GROUP_ARG_VALUE);
        }

        return wireArguments;
    }

    private static String getDeadLetterQueueName(String name)
    {
        return name + System.getProperty(PROPERTY_DEAD_LETTER_QUEUE_SUFFIX, DEFAULT_DLQ_NAME_SUFFIX);
    }
}
