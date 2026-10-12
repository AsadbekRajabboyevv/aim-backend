package com.onlinemenu.systemmessage.domain;

import com.onlinemenu.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "system_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMessage extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 150)
  private String code;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> translations = new HashMap<>();
}
