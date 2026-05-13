package br.com.terreiroreisebastiao.config;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Profile("local")
public class LocalInMemoryLockConfig {

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(RedissonClient.class)
	public RedissonClient redissonClient() {
		ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "getLock" -> createLockProxy(
					(String) args[0],
					locks.computeIfAbsent((String) args[0], ignored -> new ReentrantLock())
			);
			case "shutdown" -> null;
			case "isShutdown", "isShuttingDown" -> false;
			case "toString" -> "InMemoryRedissonClient(local)";
			case "hashCode" -> System.identityHashCode(proxy);
			case "equals" -> proxy == args[0];
			default -> throw unsupported("RedissonClient", method.getName());
		};

		return (RedissonClient) Proxy.newProxyInstance(
				RedissonClient.class.getClassLoader(),
				new Class<?>[] { RedissonClient.class },
				handler
		);
	}

	private RLock createLockProxy(String name, ReentrantLock delegate) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "getName" -> name;
			case "tryLock" -> tryLock(delegate, args);
			case "lock" -> {
				delegate.lock();
				yield null;
			}
			case "lockInterruptibly" -> {
				delegate.lockInterruptibly();
				yield null;
			}
			case "unlock" -> {
				delegate.unlock();
				yield null;
			}
			case "isHeldByCurrentThread" -> delegate.isHeldByCurrentThread();
			case "isLocked" -> delegate.isLocked();
			case "forceUnlock", "delete" -> false;
			case "remainTimeToLive" -> -1L;
			case "toString" -> "InMemoryRLock(" + name + ")";
			case "hashCode" -> System.identityHashCode(proxy);
			case "equals" -> proxy == args[0];
			default -> throw unsupported("RLock", method.getName());
		};

		return (RLock) Proxy.newProxyInstance(
				RLock.class.getClassLoader(),
				new Class<?>[] { RLock.class },
				handler
		);
	}

	private Object tryLock(ReentrantLock delegate, Object[] args) throws InterruptedException {
		if (args == null || args.length == 0) {
			return delegate.tryLock();
		}

		if (args.length == 2 && args[0] instanceof Long waitTime && args[1] instanceof TimeUnit unit) {
			return delegate.tryLock(waitTime, unit);
		}

		if (args.length == 3 && args[0] instanceof Long waitTime && args[2] instanceof TimeUnit unit) {
			return delegate.tryLock(waitTime, unit);
		}

		throw unsupported("RLock", "tryLock/" + args.length);
	}

	private UnsupportedOperationException unsupported(String type, String method) {
		return new UnsupportedOperationException(type + "." + method + " nao e suportado no profile local.");
	}
}
