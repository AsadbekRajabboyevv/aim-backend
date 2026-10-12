package com.onlinemenu.billing.usage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class UsageStore {

  private final JdbcTemplate jdbcTemplate;

  public long used(UUID userId, String key, LocalDate periodStart) {
    List<Long> rows = jdbcTemplate.queryForList("""
        select used from billing_usage
        where user_id = ? and metric_key = ? and period_start = ?
        """, Long.class, userId, key, periodStart);
    return rows.isEmpty() ? 0 : rows.get(0);
  }

  public Map<String, Long> usedByKey(UUID userId, Map<String, LocalDate> periodStarts) {
    if (periodStarts.isEmpty()) {
      return Map.of();
    }
    return jdbcTemplate.queryForList("""
            select metric_key, period_start, used from billing_usage where user_id = ?
            """, userId).stream()
        .filter(row -> {
          LocalDate start = periodStarts.get((String) row.get("metric_key"));
          return start != null && start.equals(((java.sql.Date) row.get("period_start")).toLocalDate());
        })
        .collect(Collectors.toMap(row -> (String) row.get("metric_key"),
            row -> ((Number) row.get("used")).longValue()));
  }

  public boolean tryIncrement(UUID userId, String key, LocalDate periodStart, long limit) {
    if (limit == 0) {
      return false;
    }
    long max = limit < 0 ? Long.MAX_VALUE : limit;
    int updated = jdbcTemplate.update("""
        insert into billing_usage (user_id, metric_key, period_start, used) values (?, ?, ?, 1)
        on conflict (user_id, metric_key, period_start)
        do update set used = billing_usage.used + 1 where billing_usage.used < ?
        """, userId, key, periodStart, max);
    return updated > 0;
  }

  public void increment(UUID userId, String key, LocalDate periodStart) {
    tryIncrement(userId, key, periodStart, -1);
  }
}
