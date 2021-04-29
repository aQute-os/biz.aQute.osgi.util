package biz.aQute.scheduler.basic.provider;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;

import org.osgi.util.function.Consumer;
import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import biz.aQute.scheduler.api.CancellablePromise;

public abstract class CancellablePromiseImpl<T> implements CancellablePromise<T>, Closeable {

	private final Promise<T> promise;


	public CancellablePromiseImpl(Promise<T> promise) {
		this.promise = promise;
	}


	@Override
	public void close() {
		cancel();
	}


	@Override
	public boolean isDone() {
		return promise.isDone();
	}


	@Override
	public T getValue() throws InvocationTargetException, InterruptedException {
		return promise.getValue();
	}


	@Override
	public Throwable getFailure() throws InterruptedException {
		return promise.getFailure();
	}


	@Override
	public Promise<T> onResolve(Runnable callback) {
		return promise.onResolve(callback);
	}


	@Override
	public Promise<T> onSuccess(Consumer<? super T> success) {
		return promise.onSuccess(success);
	}


	@Override
	public Promise<T> onFailure(Consumer<? super Throwable> failure) {
		return promise.onFailure(failure);
	}


	@Override
	public <R> Promise<R> then(Success<? super T, ? extends R> success, Failure failure) {
		return promise.then(success);
	}


	@Override
	public <R> Promise<R> then(Success<? super T, ? extends R> success) {
		return promise.then(success);
	}


	@Override
	public Promise<T> thenAccept(Consumer<? super T> consumer) {
		return promise.thenAccept(consumer);
	}


	@Override
	public Promise<T> filter(Predicate<? super T> predicate) {
		return promise.filter(predicate);
	}


	@Override
	public <R> Promise<R> map(Function<? super T, ? extends R> mapper) {
		return promise.map(mapper);
	}


	@Override
	public <R> Promise<R> flatMap(Function<? super T, Promise<? extends R>> mapper) {
		return promise.flatMap(mapper);
	}


	@Override
	public Promise<T> recover(Function<Promise<?>, ? extends T> recovery) {
		return promise.recover(recovery);
	}


	@Override
	public Promise<T> recoverWith(Function<Promise<?>, Promise<? extends T>> recovery) {
		return promise.recoverWith(recovery);
	}


	@Override
	public Promise<T> fallbackTo(Promise<? extends T> fallback) {
		return promise.fallbackTo(fallback);
	}


	@Override
	public Promise<T> timeout(long milliseconds) {
		return promise.timeout(milliseconds);
	}


	@Override
	public Promise<T> delay(long milliseconds) {
		return promise.delay(milliseconds);
	}

}
