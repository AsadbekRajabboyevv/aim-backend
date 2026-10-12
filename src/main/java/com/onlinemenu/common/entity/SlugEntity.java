package com.onlinemenu.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class SlugEntity extends BaseEntity {

  @Column(name = "slug", nullable = false, unique = true, length = 180)
  private String slug;
}
