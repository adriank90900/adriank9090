import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TwoMaps<K, V> implements AutoCloseable {

    private final Map<K, V> values = new HashMap<>();
    private final Map<K, Long> expiresAtNanos = new HashMap<>();

    private final Thread cleaner;
    private final long cleanupSleepMillis;
    private volatile boolean running = true;

    public TwoMaps(long cleanupInterval, TimeUnit unit) {
        this.cleanupSleepMillis = Math.max(1L, unit.toMillis(cleanupInterval));

        cleaner = new Thread(() -> {
            while (running) {
                try {
                    cleanupExpired();
                    Thread.sleep(cleanupSleepMillis);
                } catch (InterruptedException ignored) {
                }
            }
        }, "ttl-cleaner");

        cleaner.setDaemon(true);
        cleaner.start();
    }

    public void put(K key, V value, long ttl, TimeUnit unit) {
        if (ttl <= 0) {
            remove(key);
            return;
        }
        long expires = System.nanoTime() + unit.toNanos(ttl);

        synchronized (this) {
            values.put(key, value);
            expiresAtNanos.put(key, expires);
        }
    }

    public V get(K key) {
        synchronized (this) {
            Long expires = expiresAtNanos.get(key);
            if (expires == null)
                return null;

            if (System.nanoTime() >= expires) {
                values.remove(key);
                expiresAtNanos.remove(key);
                return null;
            }
            return values.get(key);
        }
    }

    public void remove(K key) {
        synchronized (this) {
            values.remove(key);
            expiresAtNanos.remove(key);
        }
    }

    private void cleanupExpired() {
        long now = System.nanoTime();

        synchronized (this) {
            Iterator<Map.Entry<K, Long>> iterator = expiresAtNanos.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, Long> e = iterator.next();
                if (now >= e.getValue()) {
                    K key = e.getKey();
                    iterator.remove();
                    values.remove(key);
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        cleaner.interrupt();
        try {
            cleaner.join(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}