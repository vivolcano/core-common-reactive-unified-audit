package com.sbt.audit.core.model.v2.event;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Заглушка модели события аудита клиента SBT.
 */
@Getter
@ToString
@Builder
public class Event {

  private final String name;
  private final String metamodelVersion;
  private final String module;
  private final String nodeId;
  private final String userNode;
  private final String session;
  private final String sourceSystem;
  private final List<String> tags;
  private final String userLogin;
  private final String userName;
  private final String requestId;
  private final EventParams params;
  private final boolean isSuccess;
}
