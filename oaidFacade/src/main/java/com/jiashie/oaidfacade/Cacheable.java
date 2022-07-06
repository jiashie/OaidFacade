package com.jiashie.oaidfacade;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存数据，避免重复请求
 * @param <E>
 */
public abstract class Cacheable<E> {

    public static final String TAG = "Cacheable";

    public interface DataCallback<E> {
        boolean mainThread();

        boolean oneTime();

        void onNext(@Nullable E e);

        void onError(Throwable t);
    }

    public interface Dispatcher {
        void dispatch(Runnable r);
    }

    public static abstract class MainDataCallback<T> implements DataCallback<T> {
        private final boolean oneTime;

        public MainDataCallback() {
            this(true);
        }
        public MainDataCallback(boolean oneTime) {
            this.oneTime = oneTime;
        }
        @Override
        public boolean mainThread() {
            return true;
        }

        @Override
        public boolean oneTime() {
            return oneTime;
        }
    }
    private volatile E data;
    private final Set<DataCallback<E>> callbacks = Collections.synchronizedSet(new LinkedHashSet<>());
    //newCachedThreadPool
    private final ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            new DefaultThreadFactory(TAG));

    private final AtomicBoolean status = new AtomicBoolean(false);

    private final Dispatcher mainThreadDispatcher = new MainThreadDispatcher();
    /**
     * 在调用者线程执行fetchData
     */
    protected boolean fetchOnCallerThread() {
        return false;
    }

    private Type dataType;
    public Cacheable() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        if (genericSuperclass != null) {
            try {
                Type[] actualTypeArguments = genericSuperclass.getActualTypeArguments();
                dataType = actualTypeArguments[0];
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    public E get() {
        return data;
    }

    public void invalidate() {
        data = null;
    }
    public void fetch(@Nullable DataCallback<E> callback) {
        E tmp = get();
        if (tmp != null) {
            if (callback != null) {
                callback.onNext(tmp);
                if (!callback.oneTime()) {
                    callbacks.add(callback);
                }
            }
            Log.d(TAG, "callback with cached value to " + callback);
            return;
        }
        if (callback != null) {
            callbacks.add(callback);
        }
        if (status.compareAndSet(false, true)) {
            Log.d(TAG, dataType + ", doFetch " + callback);
            doFetch();
        } else {
            Log.d(TAG, dataType + ", fetching... wait for callback " + callback);
        }
    }

    private void doFetch() {
        if (fetchOnCallerThread()) {
            try {
                data = fetchData();
                status.set(false);
                dispatchResult();
            } catch (Throwable e) {
                e.printStackTrace();
                status.set(false);
                dispatchError(e);
            }
        } else {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        data = fetchData();
                        status.set(false);
                        dispatchResult();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        status.set(false);
                        dispatchError(e);
                    }
                }
            });
        }
    }

    private void dispatchError(Throwable t) {
        Iterator<DataCallback<E>> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            DataCallback<E> next = iterator.next();
            if (next.mainThread()) {
                mainThreadDispatcher.dispatch(() -> next.onError(t));
            } else {
                next.onError(t);
            }
            if (next.oneTime()) {
                iterator.remove();
            }
        }
    }

    private void dispatchResult() {
        Iterator<DataCallback<E>> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            DataCallback<E> next = iterator.next();
            Log.d(TAG, dataType + ", onNext " + next);
            if (next.mainThread()) {
                mainThreadDispatcher.dispatch(() -> next.onNext(data));
            } else {
                next.onNext(data);
            }
            if (next.oneTime()) {
                iterator.remove();
            }
        }
    }

    /**
     * 子类实现具体的获取数据逻辑。运行在子线程
     * @return
     */
    @WorkerThread
    abstract protected E fetchData();

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = prefix + "-" +
                    poolNumber.getAndIncrement() +
                    "-t-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    static class MainThreadDispatcher implements Dispatcher {

        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        @Override
        public void dispatch(Runnable r) {
            mainHandler.post(r);
        }
    }
}

