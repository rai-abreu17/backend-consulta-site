package br.com.terreiroreisebastiao.shared.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockService {

    private static final Logger log = LoggerFactory.getLogger(LockService.class);
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public boolean executeWithLock(String lockKey, long waitSeconds, Runnable action) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            if (acquired) {
                action.run();
                return true;
            }
            log.warn("Falha ao adquirir lock. chave={}", lockKey);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrompida aguardando lock. chave={}", lockKey, e);
            return false;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
