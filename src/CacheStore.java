import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheStore {
//    используем потокобезопасную мапу и однопоточный очиститель
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public void removeExpiredKeys(){
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    public CacheStore() {
//        раз в секунду чистим старые ключи
        cleaner.scheduleAtFixedRate(this::removeExpiredKeys, 1, 1, TimeUnit.SECONDS);
    }

    public void put(String key, String value, long seconds) {
//        накладываем маркер вечного кэша
        Instant expireAt = seconds > 0 ? Instant.now().plusSeconds(seconds) : Instant.EPOCH;

        cache.put(key, new CacheEntry(value, expireAt));
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return null;
        }

//        защита от устаревших данных
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.value();
    }

    public void delete(String key) {
        cache.remove(key);
    }

    public void shutdown() {
        cleaner.shutdown();
    }

}
