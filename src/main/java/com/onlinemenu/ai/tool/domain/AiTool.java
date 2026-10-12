package com.onlinemenu.ai.tool.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.common.entity.BaseEntity;
import com.onlinemenu.user.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ai_tool")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTool extends BaseEntity {

  @Column(name = "name", nullable = false, unique = true, length = 64)
  private String name;

  @Column(name = "title", nullable = false, length = 150)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "http_method", nullable = false, length = 10)
  private AiToolHttpMethod httpMethod;

  @Column(name = "path_template", nullable = false, length = 300)
  private String pathTemplate;

  @Convert(converter = JsonNodeTextConverter.class)
  @Column(name = "input_schema", nullable = false, columnDefinition = "text")
  private JsonNode inputSchema;

  @Column(name = "requires_confirmation", nullable = false)
  private boolean requiresConfirmation;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_roles", nullable = false, columnDefinition = "jsonb")
  private List<Role> allowedRoles = new ArrayList<>();

  @Column(name = "active", nullable = false)
  private boolean active;
}
