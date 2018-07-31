/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.broker.client.impl.workflow;

import io.zeebe.broker.client.api.commands.PublishMessageCommandStep1;
import io.zeebe.broker.client.api.commands.PublishMessageCommandStep1.PublishMessageCommandStep2;
import io.zeebe.broker.client.api.commands.PublishMessageCommandStep1.PublishMessageCommandStep3;
import io.zeebe.broker.client.api.events.MessageEvent;
import io.zeebe.broker.client.cmd.ClientException;
import io.zeebe.broker.client.impl.CommandImpl;
import io.zeebe.broker.client.impl.PartitionManager;
import io.zeebe.broker.client.impl.RequestManager;
import io.zeebe.broker.client.impl.command.MessageCommandImpl;
import io.zeebe.broker.client.impl.data.ZeebeObjectMapperImpl;
import io.zeebe.broker.client.impl.record.RecordImpl;
import io.zeebe.protocol.impl.SubscriptionUtil;
import io.zeebe.protocol.intent.MessageIntent;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class PublishMessageCommandImpl extends CommandImpl<MessageEvent>
    implements PublishMessageCommandStep1, PublishMessageCommandStep2, PublishMessageCommandStep3 {

  private final MessageCommandImpl command;
  private final PartitionManager partitionManager;
  private final String topicName;

  public PublishMessageCommandImpl(
      final RequestManager commandManager,
      ZeebeObjectMapperImpl objectMapper,
      String topicName,
      PartitionManager partitionManager) {
    super(commandManager);

    this.command = new MessageCommandImpl(objectMapper, MessageIntent.PUBLISH);
    this.topicName = topicName;
    this.partitionManager = partitionManager;
  }

  @Override
  public PublishMessageCommandStep3 payload(final InputStream payload) {
    this.command.setPayload(payload);
    return this;
  }

  @Override
  public PublishMessageCommandStep3 payload(final String payload) {
    this.command.setPayload(payload);
    return this;
  }

  @Override
  public PublishMessageCommandStep3 payload(Map<String, Object> payload) {
    this.command.setPayload(payload);
    return this;
  }

  @Override
  public PublishMessageCommandStep3 payload(Object payload) {
    this.command.setPayload(payload);
    return this;
  }

  @Override
  public PublishMessageCommandStep3 messageId(String messageId) {
    command.setMessageId(messageId);
    return this;
  }

  @Override
  public PublishMessageCommandStep3 correlationKey(String correlationKey) {
    command.setCorrelationKey(correlationKey);
    return this;
  }

  @Override
  public PublishMessageCommandStep2 messageName(String messageName) {
    command.setName(messageName);
    return this;
  }

  @Override
  public RecordImpl getCommand() {
    final List<Integer> partitionIds = partitionManager.getPartitionIds(topicName);
    if (partitionIds == null || partitionIds.isEmpty()) {
      throw new ClientException(String.format("No topic found with name '%s'", topicName));
    }

    final int hashCode = SubscriptionUtil.getSubscriptionHashCode(command.getCorrelationKey());
    final int index = Math.abs(hashCode % partitionIds.size());
    final Integer partitionId = partitionIds.get(index);
    command.setPartitionId(partitionId);

    return command;
  }
}
