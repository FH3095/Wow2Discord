package eu._4fh.wow2discord.util;

import java.util.concurrent.atomic.AtomicReference;

public class Singleton<T> {

    private final String instanceClassName;
    private final AtomicReference<T> instance = new AtomicReference<>(null);

    public Singleton(Class<T> instanceClass) {
        this.instanceClassName = instanceClass.getName();
    }

    public T get() {
        T ret = instance.get();
        if (ret == null) {
            throw new IllegalStateException("Class " + instanceClassName + " not yet initialized");
        }
        return ret;
    }

    private void set(T oldInstance, T newInstance) {
        boolean setSuccessful = instance.compareAndSet(oldInstance, newInstance);
        if (!setSuccessful) {
            throw new IllegalStateException(
                    "Cant set instance to " + newInstance + " because instance currently set to " + oldInstance);
        }
    }

    public void set(T newInstance) {
        set(null, newInstance);
    }

    public void unset(T currentInstance) {
        set(currentInstance, null);
    }
}
