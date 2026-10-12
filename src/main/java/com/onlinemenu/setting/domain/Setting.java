package com.onlinemenu.setting.domain;

import com.onlinemenu.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_setting")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setting extends BaseEntity {

  @Column(name = "setting_key", nullable = false, unique = true, updatable = false, length = 150)
  private String key;

  @Column(name = "setting_value", nullable = false, length = 1000)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false, length = 20)
  private SettingType type;

  @Column(name = "description", length = 500)
  private String description;
}
