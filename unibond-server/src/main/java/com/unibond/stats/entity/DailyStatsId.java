package com.unibond.stats.entity;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
public class DailyStatsId implements Serializable {
    private Long coupleId;
    private LocalDate statDate;
    public DailyStatsId() {}
    public DailyStatsId(Long coupleId, LocalDate statDate) {
        this.coupleId = coupleId;
        this.statDate = statDate;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyStatsId that)) return false;
        return Objects.equals(coupleId, that.coupleId) && Objects.equals(statDate, that.statDate);
    }
    @Override public int hashCode() { return Objects.hash(coupleId, statDate); }
}
