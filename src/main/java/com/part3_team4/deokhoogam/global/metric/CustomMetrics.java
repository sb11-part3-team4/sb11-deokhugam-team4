package com.part3_team4.deokhoogam.global.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomMetrics {

  private final MeterRegistry meterRegistry;
  private final Map<String, AtomicLong> lastSuccessHolders = new ConcurrentHashMap<>();


  private final Map<String, AtomicLong> countHolders = new ConcurrentHashMap<>();

  // 최근 개수 기록
  public void recordCount(String job, String period, long count) {
    String key = job + ":" + (period == null ? "-" : period);
    AtomicLong holder = countHolders.computeIfAbsent(key, k -> {
      AtomicLong al = new AtomicLong();
      Gauge.Builder<AtomicLong> g = Gauge
          .builder("batch.processed.count", al, AtomicLong::get)
          .tag("job", job);
      if (period != null) g.tag("period", period);
      g.register(meterRegistry);
      return al;
    });
    holder.set(count);
  }
  //최근 성공 시간 기록
  public void recordLastSuccess(String job, String period) {
    String key = job + ":" + (period == null ? "-" : period);
    AtomicLong holder = lastSuccessHolders.computeIfAbsent(key, k -> {
      AtomicLong al = new AtomicLong();
      Gauge.Builder<AtomicLong> g = Gauge
          .builder("batch.last.success.time", al, AtomicLong::get)
          .tag("job", job);
      if (period != null) g.tag("period", period);
      g.register(meterRegistry);
      return al;
    });
    holder.set(System.currentTimeMillis());
  }
}
