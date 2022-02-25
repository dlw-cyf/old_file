/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /** 线程获取ThreadLocal.get()时,如果是第一个在某个theadLocal对象上get时，会给当前线程分配一个value
     * 这个value 和 当前theadLocal对象 被包装成一个 entry 其中key是threadLocal对象，value是threadLocal对象给当前线程生成的value
     * 这个entry存放到当前线程的 threadLocals 这个map的哪个桶位？
     *      与当前threadLocalHashCode字段有关系。threadLocalHashCode & (table.length-1)得到的位置就是当前entry需要啊存放的位置。
     * */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * 创建ThreadLocal对象时，会使用到。
     * 每创建一个theadLocal对象，就会使用nextHashCode 分配一个hash值给这个对象。
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * 每创建一个TheadLocal对象：
     *  ThreadLocal.nextHashCode这个值就会增长 0x61c88647
     *      这个值很特殊，它是斐波拉契也叫黄金分割数。hash增量为这个数字，带来的好处就是hash分布非常均匀。
     *
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * 创建新的ThreadLocal对象时，会给当前对象分配一个hash值。
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 默认返回null，一般情况下都是需要重写的
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * 返回当前线程与当前ThreadLocal对象相关联的局部变量，这个变量只有当前线程能访问。
     * 如果当前线程没有分配，则使用(initialValue方法)给当前线程去分配
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        /**获取当前线程*/
        Thread t = Thread.currentThread();

        /**获取到当前线程的 threadLocals map引用*/
        ThreadLocalMap map = getMap(t);

        /**条件成立：说明当前线程已经拥有自己的ThreadLocalMap对象了*/
        if (map != null) {
            /**
             * key：当前ThreadLocal对象
             * 根据ThreadLocal对象获取与当前线程相关联的entry。
             * */
            ThreadLocalMap.Entry e = map.getEntry(this);
            /**条件成立：说明当前线程初始化过与当前TheadLocal对象相关联的线程局部变量。*/
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                /**返回对应的局部变量*/
                return result;
            }
        }
        /**
         * 执行到这里有几种情况？
         * 1.当前线程对应的threadLocalMap(threadLocals)是空。
         * 2.当前线程与当前ThreadLocal对象没有生成过相关联的线程局部变量。
         *
         * setInitialValue：初始化当前线程与当前ThreadLocal对象相关联的value(线程局部变量)
         * 且当前线程如果没有ThreadLocalMap的话，还会初始化创建这个Map
         */
        return setInitialValue();
    }

    /**
     * setInitialValue：初始化当前线程与当前ThreadLocal对象相关联的value(线程局部变量)
     * 且当前线程如果没有ThreadLocalMap的话，还会初始化创建这个Map
     * @return the initial value
     */
    private T setInitialValue() {
        /**调用的是当前ThreadLocal对象的initialValue方法，这个方法大部分情况下我们都会重写。
         * value：就是当前ThreadLocal对象与当前线程相关联的 局部变量
         * */
        T value = initialValue();
        Thread t = Thread.currentThread();
        /**获取当前线程内部的ThreadLocals:ThreadLocalMap对象*/
        ThreadLocalMap map = getMap(t);

        /**条件成立：说明当前线程内部已经初始化过 ThreadLocalMap对象了。(线程的ThreadLocals只会初始化一次)*/
        if (map != null)
            /**
             * 保存当前ThreadLocal与当前线程相关联的线程局部变量
             * Key：当前ThreadLocal对象。
             * value：线程与当前ThreadLocal相关联的局部变量。
             * */
            map.set(this, value);
        else
            /**
             * 执行到这里：说明当前线程内部的ThreadLocals(ThreadLocalMap)还未进行初始化。
             * 这里调用createMap为当前线程创建ThreadLocals(ThreadLocalMap)。
             * t:当前线程
             * value：线程与ThreadLocal对象相关联的局部变量。
             * */
            createMap(t, value);

        return value;
    }

    /**
     * 修改当前线程与当前ThreadLocal对象相关联的线程局部变量
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        /**获取当前线程的ThreadLocalMap对象*/
        ThreadLocalMap map = getMap(t);

        /**
         * 条件成立：说明当前线程的ThreadLocalMap已经初始化了。
         */
        if (map != null)
            /**调用ThreadLocalMap的set方法进行 重写 或者 添加*/
            map.set(this, value);
        else
            /**为当前线程创建ThreadLocalMap对象*/
            createMap(t, value);
    }

    /**
     * 移除当前线程与当前threadLocal对象相关联的局部变量。
     *
     * @since 1.5
     */
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        /**
         * 为当前线程t的threadLocals字段进行初始化：创建ThreadLocalMap对象
         *  key：当前线程。
         *  value：当前线程与当前ThreadLocal对象相关联的局部变量。
         * */
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap是一个定制的自定义hashMap哈希表，只适用于维护线程对应ThreadLocal的值。
     * 此类的方法没有在ThreadLocal类外部暴露，此类是私有的，允许在Thread类中以字段的形式声明。
     * 以助于处理存储量打，生命周期长的的使用用途。
     * 此类定制的哈希表实体键值对使用弱引用WeakReferences作为key，
     * 但是一旦引用不能再被使用，只有当哈希表中的空间被耗尽时，对应不再使用的键值对实体才回确保被移除回收。
     */
    static class ThreadLocalMap {

        /**
         * 什么是弱引用呢？
         * A a = new A();强引用
         * WeakReference weakA = new WeakReference(a);弱引用
         *
         * 当 a=null 时，下一次GC时，对象a就被回收了，别管有没有弱引用是否在关联这个对象。
         *
         * key使用的弱引用，key保存的是ThreadLocal对象。
         * value使用的强引用，value保存的是ThreadLocal对象与当前线程相关联的线程局部变量。
         *
         * Entry#key 这样设计有什么好处呢？
         * 当ThreadLocal对象失去强引用且对象GC回收后，散列表中的threadLocal对象相关联的entry#key再次去key.get()时，拿到的是null。
         * 站在map角度，就可以区分哪些entry是过期的，哪些entry是非过期的。
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * 初始化当前map内部散列表数组的长度。
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 当前ThreadLocalMap内部散列表数组引用，数组的长度必须是2的次方数。
         */
        private Entry[] table;

        /**
         * 当前散列表数组占用情况，存放多少个Entry
         */
        private int size = 0;

        /**
         * 扩容触发阈值，初始值为：len * 2 / 3
         * 触发后调用 rehash() 方法。
         * rehash() 方法先做一次全量检查全局过期数据，把散列表中所有过期的entry移除，
         * 如果移除之后，散列表中的entry个数仍然达到 threshold - threshold / 4 就进行扩容
         */
        private int threshold; // Default to 0

        /**
         * 当阈值设置为当前数组长度的2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * 向后轮循查找访问
         * @param i 当前下标
         * @param len 当前散列表数组长度
         * @return
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 向前轮循查找访问
         * @param i 当前下标
         * @param len 当前散列表数组长度
         * @return
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 因为Thread.threadLocals字段是懒加载的，只有线程第一次存储threadLocal--value时，才会创建threadLocalMap对象
         * @param firstKey ThreadLocal对象
         * @param firstValue 当前线程与ThreadLocal对象关联的value。
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            /**创建Entry数组长度为16，表示threadLocalMap内部的散列表*/
            table = new Entry[INITIAL_CAPACITY];
            /**
             * 寻址算法：key.threadLocalHashCode & (table.length -1)
             * table数组的长度一定是2的次方数。2的次方数-1 转化为二进制都是1 (16=> 1000 - 1 => 1111)
             * 1111与任何数值进行&运算一定是是小于等于111
             * */
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);

            /**创建Entry对象存放到指定的slot中。*/
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            /**设置扩容阈值 (INITIAL_CAPACITY的三分之二)*/
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    expungeStaleEntry(i);
                else
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
