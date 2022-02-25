#### 1. LongAdder类

##### 1.1 add()方法

```java
/**
     * Adds the given value.
     *
     * @param x the value to add
     */
    public void add(long x) {
        //as 表示cells引用
        //b 表示获取的base值
        //v 表示期望值
        //m 表示cells数组的长度
        //a 表示当前线程命中的cell单元格
        Cell[] as; long b, v; int m; Cell a;

        /**
         * 条件一：(as = cells) != null
         *      ture--->表示cells表格已经初始化过了，当前线程应该将数据应该写入到对应的cell中
         *      false-->表示cells未初始化，当前所有线程数据应该写到base中。
         * 条件二：!casBase(b = base, b + x)
         *      ture--->CAS失败：表示发生了线程竞争，可能需要(重试 | 扩容)。
         *      false-->表示当前线程在base中CAS替换数据成功
         */

        if ((as = cells) != null || !casBase(b = base, b + x)) {
            /**
             * 什么时候会进入该if？
             *      1. 条件一为ture--->表示cells已经初始化过了，当前线程应该将数据应该写入到对应的cell中
             *      2. 条件二为ture--->CAS失败：表示发生了线程竞争，可能需要[重试 | 扩容]。
             */

			//true --> 未发生竞争  false --> 发生竞争
            boolean uncontended = true;

			/**
			 * 条件一：as == null || (m = as.length - 1) < 0
			 * 		true--->说明cells没有进行初始化，也就是通过条件二(CAS失败：多线程写base发生了竞争)。
			 * 		false-->说明cells已经初始化了，需要找到当前线程对应的cell写值
			 * 条件二：(a = as[getProbe() & m]) == null
			 * 		getProbe()：获取当前线程的hash值
			 * 		m表示cells长度-1：cells的长度一定是2的次方数(每次扩容都是2的倍数)
			 * 		true--->说明当前线程对应下标的cell为null，需要创建 longAccumulate 支持
             * 	    false-->说明当前线程对应下标的cell不为null，说明下一步应该将x的值添加到当前cell中。
             * 条件三：!(uncontended = a.cas(v = a.value, v + x))
             *      true--->表示case失败：说明当前线程对应下标的cell有竞争[重试 | 扩容]。
             *      false-->表示case成功。
			 */
			if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))

                /**
                 * 什么情况下会调用该方法：
                 *      1.条件一为true：说明cells没有进行初始化，也就是通过条件二(CAS失败：多线程写base发生了竞争)。
                 *      2.条件二为true：说明当前线程对应下标的cell为null，需要创建当前线程对应的cell对象
                 *      3.条件三为true：表示case失败：说明当前线程对应下标的cell有竞争[重试 | 扩容]。
                 */
                longAccumulate(x, null, uncontended);
        }
    }
```

#### 2. Striped64类(父类)

##### 2.1 cell内部类

```java
/**
     * Padded variant of AtomicLong supporting only raw accesses plus CAS.
     *
     * JVM intrinsics note: It would be possible to use a release-only
     * form of CAS here, if it were provided.
     */
    @sun.misc.Contended static final class Cell {
        volatile long value;
        Cell(long x) { value = x; }
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        // 表示拿到value属性基于当前对象内存地址的偏移量：能够通过Cell对象+valueOffset(内存偏移量)拿到value属性的值
        private static final long valueOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> ak = Cell.class;
                //获取value属性的内存偏移量。
                valueOffset = UNSAFE.objectFieldOffset
                    (ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```

##### 2.2 属性介绍

* NCPU：表示当前计算机CPU数量，有什么用？**控制cells数组长度**的一个关键条件。
* cells：**发生竞争**后，用于计算的数组对象。
* base：**没有发生过竞争或者当cells正在[初始化 | 扩容]**时，数据会累加到base上。
* cellsBusy：cells在`[初始化 | 扩容]`都需要获取锁：**0--表示无锁状态，1--表示锁已经被其他线程获取**。

```java
	/** Number of CPUS, to place bound on table size */
    // 表示当前计算机CPU数量，有什么用？控制cells数组长度的一个关键条件。
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * Table of cells. When non-null, size is a power of 2.
     */
    // 发生竞争后，用于计算的数组对象。
    transient volatile Cell[] cells;

    /**
     * Base value, used mainly when there is no contention, but also as
     * a fallback during table initialization races. Updated via CAS.
     */
    // 没有发生过竞争或者当cells正在[初始化 | 扩容]时，数据会累加到base上，
    transient volatile long base;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating Cells.
     */
    // cells在[初始化 | 扩容]都需要获取锁：0--表示无锁状态，1--表示锁已经被其他线程获取。
    transient volatile int cellsBusy;
```

##### 2.3 方法介绍

###### 2.3.1 casBase()

```java
/**
     * CASes the base field.
     * 通过CAS方式修改base值
     */
    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }
```

###### 2.3.2 casCellsBusy()

```java
/**
     * CASes the cellsBusy field from 0 to 1 to acquire lock.
     * 通过CAS方式获取锁。
     */
    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }
```

###### 2.3.3 getProbe()

```java
/**
     * Returns the probe value for the current thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     * 获取当前线程的Hash值。
     */
    static final int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

```

###### 2.3.4 advanceProbe()

```java
/**
     * Pseudo-randomly advances and records the given probe value for the
     * given thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     *
     * 重置当前线程的Hash值
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }
```

##### 3. 核心方法

###### 3.1 longAccumulate()

```java
/**
     * 什么情况下会调用该方法：
     *      1.条件一为true：说明cells没有进行初始化，也就是通过条件二(CAS失败：多线程写base发生了竞争)。
     *      2.条件二为true：说明当前线程对应下标的cell为null，需要创建当前线程对应的cell对象
     *      3.条件三为true：表示case失败：说明当前线程对应下标的cell有竞争[重试 | 扩容]。
     *
     *      wasUncontended：只有条件三的情况下为false。
     */
    final void longAccumulate(long x, LongBinaryOperator fn,
                              boolean wasUncontended) {
        // 表示当前线程的Hash值。
        int h;

        //条件成立说明：当前线程还未分配Hash值。
        if ((h = getProbe()) == 0) {
            //给当前线程分配Hash值。
            ThreadLocalRandom.current(); // force initialization
            //取出当前线程的Hash值，赋值给h
            h = getProbe();
            //为什么？因为默认情况下，当前线程的Hash值为0，肯定是写入到cells[0]的位置。不把它当做一次真正的竞争。
            wasUncontended = true;
        }

        //表示扩容意向：false-->一定不会扩容，true-->可能会扩容。
        boolean collide = false;                // True if last slot nonempty

        //自旋
        for (;;) {
            /**
             * as：表示cells引用
             * a：表示当前线程对应的cell
             * n：表示cells数组的长度
             * v：表示期望值。
             */
            Cell[] as; Cell a; int n; long v;

            //表示cells已经初始化了，当前线程应该将数据写入到对应的cell中。
            if ((as = cells) != null && (n = as.length) > 0) {
                /**
                 * 前置条件：
                 *      2.条件二为true：说明当前线程对应下标的cell为null，需要创建当前线程对应的cell对象。
                 *      3.条件三为true：表示case失败：说明当前线程对应下标的cell有竞争[重试 | 扩容]。
                 */
                //条件成立说明：当前线程对应下标的cell为null,需要创建当前线程对应的cell对象
                if ((a = as[(n - 1) & h]) == null) {

                    //条件成立说明：当前锁未被占用
                    if (cellsBusy == 0) {       // Try to attach new Cell

                        //那当前的x创建一个Cell对象。
                        Cell r = new Cell(x);   // Optimistically create

                        /**
                         * 条件一：cellsBusy == 0：判断cellsBusy锁是否被占用
                         * 条件二：casCellsBusy()
                         *      true-->表示当前线程获取锁成功。
                         */
                        if (cellsBusy == 0 && casCellsBusy()) {

                            //是否创建成功的标记。
                            boolean created = false;
                            try {               // Recheck under lock
                                /**
                                 * rs：表示当前cells引用
                                 * m：表示cells长度
                                 * j：表示当前线程命中的cells数组中的下标。
                                 */
                                Cell[] rs; int m, j;

                                /**
                                 * 条件一：(rs = cells) != null && (m = rs.length) > 0
                                 *      这两个条件恒成立。
                                 * 条件二：rs[j = (m - 1) & h] == null
                                 *      为什么要再次判断：避免其他线程对当前线程命中的cell进行了初始化，当前线程再次初始化导致丢失数据。原因和add方法的多次校验一致。
                                 */
                                if ((rs = cells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    //如果执行到当前行，代表cellsBusy=1，有线程正在更改cells数组，代表产生了冲突，将collide设置为false
                    //扩容意向强制改为false
                    collide = false;
                }

                //只有一种情况满足：在cells初始化之后，并且当前线程竞争失败(除去线程未分配Hash的情况)
                else if (!wasUncontended)       // CAS already known to fail
                    // 将wasUncontended设置为true之后，会执行 h = advanceProbe(h); 重置当前线程的Hash值。然后自旋重新进入if条件。
                    wasUncontended = true;      // Continue after rehash

                /**
                 * 前置条件：当前线程rehash后的Hash值，然后命中的cell不为空。
                 *  true-->通过CAS对新命中的cell写入成功。退出循环。
                 *  false->表示rehash之后命中的新的cell 也有竞争 (当前重试一次)
                 */
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                             fn.applyAsLong(v, x))))
                    break;
                /**
                 * 条件一：n >= NCPU
                 *      true：数组的长度大于CPU的个数：不进行扩容了。
                 *      false：表示还可以继续扩容。
                 * 条件二：cells != as
                 *      true：表示其他线程已经把cells扩容过了，当前线程继续rehash重试即可。
                 *      false：表示还可以继续扩容。
                 */
                else if (n >= NCPU || cells != as)
                    //扩容意向 改为false：表示不扩容了。
                    collide = false;            // At max size or stale

                //条件成立说明：设置扩容意向为true，但是不一定真的发生扩容。
                else if (!collide)
                    collide = true;

                /**
                 * 真正扩容的逻辑
                 * 条件一：cellsBusy == 0
                 *      true：表示无锁状态：表示可以去竞争这把锁
                 *      false：表示cellsBusy锁已经被其他线程获取，表示当前时刻其他线程正在进行扩容相关操作。
                 * 条件二：casCellsBusy()
                 *      true：表示当前时刻线程获取锁成功，可以执行扩容
                 *      false：表示当前时刻其他线程正在进行扩容相关操作。
                 */

                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        //再次判断，避免其他线程对cells数组扩容后，当前线程在进行了一次扩容。原因同上
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }

                // 重置当前线程的Hash值。
                h = advanceProbe(h);
            }
            /**
             * 前置条件：cells还未进行初始化。cells和as都为null
             * 条件一：cellsBusy == 0
             *      true：当前cells未加锁
             * 条件二：cells == as  (为什么要再次判断cells==null呢？因为其他线程可能会在你给as赋值后修改了cells)
             * 条件三：casCellsBusy()
             *      true：表示获取锁成功：将cellsBusy通过CAS方式改为 1
             *      false:表示其他线程正在持有这把锁。
             */
            else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {                           // Initialize table
                    /**
                     * 为什么要再次判断？
                     *  1. 线程一判断完 cellsBusy == 0 和 cells == as后，线程一让出CPU;
                     *  2. 然后线程二获取CPU，完成了cellsBusy == 0 && cells == as && casCellsBusy()整个条件校验，将cells初始化后释放了锁cellsBusy=0;
                     *  3. 这时候线程一继续判断 casCellsBusy()=true(拿到了cellsBusy锁) ，如果此时没有再次进行cells==as校验，就会对cells重新初始化，丢失之前的数据。
                     */

                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            /**
             * 前置条件：
             *      1. cells被其他线程初始化了：当前线程需要将
             初始化cells，所以当前线程将值累加到base
             */
            else if (casBase(v = base, ((fn == null) ? v + x :
                                        fn.applyAsLong(v, x))))
                break;                          // Fall back on using base
        }
    }

```





