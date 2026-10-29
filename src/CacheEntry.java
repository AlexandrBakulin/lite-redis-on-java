import java.time.Instant;

public record CacheEntry(String value, Instant expireAt) {
    public boolean isExpired() {
        if(expireAt == null || expireAt.equals(Instant.EPOCH)) {
            return false;
        }
        return Instant.now().isAfter(expireAt);
    }
}
