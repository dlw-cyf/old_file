#### 1. 核心属性

##### 1.1 静态属性

```java
	/**
     * 散列表数组的最大值限制
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 散列表的默认值(缺省)
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The largest possible (non-power of two) array size.
     * Needed by toArray and related methods.
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 并发级别。jdk1.7遗留下来的，1.8只有在初始化的时候用了用。
     * 不代表并发级别。
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 负载因子：在JDK1.8中ConcurrentHashMap 是固定值。
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 树化阈值，指定桶位链表长度达到8的话，有可能发生树化操作。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 红黑树转换为链表的阈值。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 联合TREEIFY_THRESHOLD控制桶位是否需要树化，只有当table数组长度达到64，且某个同为中的链表长度达到8，才会真正树化。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 线程迁移数据最小步长，控制线程迁移任务最小区间的一个值
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * 扩容相关：计算扩容时生成的一个 标识戳
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * 结果65535 标识并发扩容最多线程数
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * The bit shift for recording size stamp in sizeCtl.
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /**
     *  当node节点的hash值为-1时：表示当前节点是FWD节点。
     *  当node节点的hash值为-2时：表示当前节点已经树化了，且当前节点为TreeBin对象。TreeBin对象代理操作红黑树。
     */

    static final int MOVED     = -1; // hash for forwarding nodes
    static final int TREEBIN   = -2; // hash for roots of trees
    static final int RESERVED  = -3; // hash for transient reservations
    //0x7fffffff => 0111 1111 1111 1111 1111 1111 1111 1111 可以将一个负数通过位与运算后得到正数，但是不是取绝对值。
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /** Number of CPUS, to place bounds on some sizings */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** For serialization compatibility.
     * JDK1.8 序列化为了兼容JDK1.7的ConcurrentHashMap保存的。
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("segments", Segment[].class),
        new ObjectStreamField("segmentMask", Integer.TYPE),
        new ObjectStreamField("segmentShift", Integer.TYPE)
    };
```

##### 1.2 静态代码块

```java
	// Unsafe mechanics
    private static final sun.misc.Unsafe U;
    /**表示sizeCtl属性在ConcurrentHashMap中内存偏移地址*/
    private static final long SIZECTL;
    /**表示tansferIndex属性在ConcurrentHashMap中内存偏移地址*/
    private static final long TRANSFERINDEX;
    /**表示baseCount属性在ConcurrentHashMap中内存偏移地址*/
    private static final long BASECOUNT;
    /**表示cellsBusy属性在ConcurrentHashMap中内存偏移地址*/
    private static final long CELLSBUSY;
    /**表示cellValue属性在CounterCell中内存偏移地址*/
    private static final long CELLVALUE;
    /**表示数组第一个元素的偏移地址*/
    private static final long ABASE;
    private static final int ASHIFT;

	static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset
                (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                (k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset
                (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                (k.getDeclaredField("cellsBusy"));
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                (ck.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);

            //表示数组单元所占用空间大小，scale 表示Node[]数组总每一个单元所占用的空间大小（int：4个字节）。
            int scale = U.arrayIndexScale(ak);

            //1 0000 & 0 1111 = 0
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");

            /**
             * numberOfLeadingZeros()：返回单签数值转换为二进制后，从高位到低位开始统计，有多少个0连续在一块
             * numberOfLeadingZeros(4)：4 => 100 return 29
             * ASHIFT = 31 - numberOfLeadingZeros(4) = 2
             * 假如需要查找数组中的第五个元素
             * table[5]内存地址偏移量 = ABASE(首个元素地址偏移量) + 5 * 数组单元空间大小(scale) ：这种写法low
             * table[5]内存地址偏移量 = ABASE + (5 << ASHIFT)
             */
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
```

##### 1.3 成员属性

```java
	/**
     * 散列表，长度一定是2的次方数。
     */
    transient volatile Node<K,V>[] table;

    /**
     * 扩容过程中，会将扩容中的新table赋值给nextTable保持引用，扩容结束之后，这里会被设置为null
     */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * LongAdder中的baseCount：未发生竞争时，或者当前LongAdder处于加锁状态中，增量累加到baseCount中。
     */
    private transient volatile long baseCount;

    /**
     * sizeCtl < 0
     *      情况一：-1 表示当前table正在初始化(有线程再创建table数组)，当前线程需要自旋等待
     *      情况二：表示当前table数组正在进行扩容 高16位表示：扩容的标识戳 低16位表示：(1 + nThread) 当前参与并发扩容的线程数量
     * sizeCtl = 0 ：表示创建table数组时，使用DEFAULT_CAPACITY为大小。
     * sizeCtl > 0
     *      情况一：如果table未初始化，表示初始化大小
     *      情况二：如果table已经初始化，表示下次扩容时 触发条件（阈值）
     */
    private transient volatile int sizeCtl;

    /**
     * 扩容过程中，记录当前进度。所有线程都需要从transferIndex中分配区间任务，去执行自己的任务。
     */
    private transient volatile int transferIndex;

    /**
     * LongAdder中的cellsBusy 0：表示当前LongAdder对象无锁，1.表示当前LongAdder对象是加锁状态
     */
    private transient volatile int cellsBusy;

    /**
     * LongAdder中的cells数组，当baseCount发生竞争后，会创建cells数组
     * 线程会通过计算hash值，取到自己的cell，将增量累加到对应的cell中
     * 总数 = sum(cells) + baseCount
     */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K,V> keySet;
    private transient ValuesView<K,V> values;
    private transient EntrySetView<K,V> entrySet;
```

#### 2. 内部小方法

##### 2.1 spread()

让key的Hash值的高16位也参与路由运算(减少Hash碰撞)。并将符号位改为0，保证Hash值为正数。

```java
	/**
     * 传入的hash值：1100 0011 1010 0101 0001 1100 0001 1110
     * 右移十六位  ：0000 0000 0000 0000 1100 0011 1010 0101
     * ^操作的结果   1100 0011 1010 0101 1101 1111 1011 1011
     * 让key的Hash值的高16位也参与路由运算(减少Hash碰撞)。
     * -----------------------------------------------------
     * 1100 0011 1010 0101 1101 1111 1011 1011
     * 0111 1111 1111 1111 1111 1111 1111 1111（HASH_BITS）
     * 0100 0011 1010 0101 1101 1111 1011 1011
     * 其实就是将符号位改为0，得到一个正数
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }
```

##### 2.2 tableSizeFor()

返回>=c 的最小的2的次方

```java
	/**
     * 返回>=c 的最小的2的次方数和hashMap中的一致。
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

##### 2.3 CAS方法

```java
	//
	static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }
```

##### 2.4 resizeStamp()

获取扩容批次的唯一时间戳

```java
	/**
     * 16 扩容到 32
     *
     * numberOfLeadingZeros(16) = 27 => 0000 0000 0001 1011
     *
     * (1 << (RESIZE_STAMP_BITS - 1))==> 1000 0000 0000 0000 (1向左移动15位) => 32768
     * ----------------------------------------------------------
     * 0000 0000 0001 1011
     * 1000 0000 0000 0000（|运算）
     * 1000 0000 0001 1011
     *
     * 获取扩容批次的唯一时间戳
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }
```

#### 3. 核心方法

##### 3.1 putVal()

1. 判断key和value不能为空，否则抛出空指针异常。
2. 获取Hash值，让key的Hash值的高16位也参与路由运算(减少Hash碰撞)。
3. 进行自旋。
4. **CASE1：判断table数组是否还未进行初始化**：initTable()方法进行初始化。
5. **CASE2：判断寻址算法找到的桶位是否为空**，如果是直接通过CAS方法设值。
6. **CASE3：判断当前桶位的头节点是否 FWD节点**。通过允许多线程复制的功能，来减少数组的复制所带来的性能损失。
   * 条件成立：说明正在扩容，调用`helpTransfer(tab, f)`方法帮助扩容。
7. **CASE4：以上条件都不成立，说明当前桶位为链表、红黑树或者代理红黑树的TreeBin结构**
   1. 对头结点f进行加锁(Synchronized)：
   2. **再次判断**当前桶位的节点是否为`CASE2`中获取的头结点。
   3. 如果是链表，循环判断key是否相同，不相同则插入末尾。
   4. 如果是红黑树，调用`((TreeBin<K,V>)f).putTreeVal(hash, key,value)`方法插入。
   5. 判断是否在链表末尾追加了元素，是则判断链表长度是否达到树化阈值(8)，是则调用`treeifyBin(tab, i)`方法。
8. 插入的key没有冲突，最后调用`addCount(1L, binCount)`方法
   * 统计当前table一共有多少数据
   * 判断是否达到扩容阈值，触发扩容。

```java
	/**
     * @param onlyIfAbsent :false->如果有相同的key则吧value覆盖。true->不做任何操作
     */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        //控制key和value不能为空
        if (key == null || value == null) throw new NullPointerException();

        //让key的Hash值的高16位也参与路由运算(减少Hash碰撞)
        int hash = spread(key.hashCode());

        /**
         * binCount表示当前k-v 封装成node后插入到指定桶位后，在桶位中的所属链表的下标位置
         *  0：表示当前桶位为空，node可以直接放入
         *  2：表示当前啊桶位已经树化为红黑树
         */
        int binCount = 0;

        //自旋：tab：引用Map对象的table
        for (Node<K,V>[] tab = table;;) {
            /**
             * f：表示桶位的头结点
             * n：表示散列表数组的长度
             * i：表示key通过寻址计算后，得到的桶位下标
             * fh：桶位头结点的hash值
             */
            Node<K,V> f; int n, i, fh;
            /**CASE1：成立，表示table还没有进行初始化*/
            if (tab == null || (n = tab.length) == 0)
                //对map.table进行初始化。
                tab = initTable();
            /**CASE2：如果寻址算法找到的桶位为空时：通过CAS方法设置当前桶位为new Node<>
             * tabAt(table,index):获取当前数组对象下标为index的内存偏移量。
             */
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                /**
                 * 如果CAS成功，跳出循环
                 * 如果CAS失败，继续自旋
                 */
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            /**CASE3：前置条件：桶位的头结点一点不是null 条件成立说明当前桶位的头结点为 FWD节点：说明当前Map正在扩容过程中*/
            else if ((fh = f.hash) == MOVED)
                //看到FWD节点后，当前线程由义务帮助当前map完成迁移数据的工作。
                tab = helpTransfer(tab, f);

            /**CASE4：当前桶位 可能是链表，也可能是红黑树啊，也可能是红黑树代理节点TreeBin*/
            else {
                //当插入key存在时，会将旧值复制给oldVal，返回给put方法调用处。。
                V oldVal = null;

                //使用sync给 `头结点` 加锁 : 理论上是头节点(因为其他节点可能对头结点进行了修改，而这个f为之前的头结点对象。)。
                synchronized (f) {
                    /**
                     * 第一个问题：为什么又要对比一下，看看当前桶位的头结点是否为之前获取的头结点？
                     *      避免其他线程对头结点进行了修改，导致当前线程sync 加锁就有问题了，之后的所有操作都不用再做了。
                     * 第二个问题：为什么要先用synchronized再使用if判断？
                     *      如果先if判断，那么有可能在你判断的时候这个头结点还未被修改，
                     *      但是在你判断完之后其他线程对头结点进行了修改，这时候拿到锁继续执行时有问题的。
                     *
                     * 条件成立说明，咱们加锁的条件没有问题。
                     */
                    if (tabAt(tab, i) == f) {
                        /**
                         * 条件成立：说明当前同为是普通链表桶位。
                         */
                        if (fh >= 0) {
                            /**
                             * 1.当前插入key与链表中所有元素的key都不一致时，当前的插入操作追加到链表的末尾，
                             *      此时binCount表示链表长度。
                             * 2.当前插入key与链表当中的某个元素的key一致时，当前插入操作可能就是替换了。
                             *      此时binCount表示冲突位置(binCount - 1)
                             */
                            binCount = 1;

                            //迭代循环当前桶位的链表，e是每次循环处理的节点。
                            for (Node<K,V> e = f;; ++binCount) {
                                // ek:当前循环节点的key
                                K ek;
                                /**
                                 * 条件一：e.hash == hash
                                 *      表示当前循环元素的hash值与插入节点的hash值一致，需要进一步判断。
                                 * 条件二：((ek = e.key) == key || (ek != null && key.equals(ek)))
                                 *      表示：说明当前循环的节点和插入的key一致，发生了冲突，需要替换value值。
                                 */
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    //将当前循环元素的 旧值 赋值给oldVal
                                    oldVal = e.val;
                                    //根据传入的onlyIfAbsent参数 判断是否对当前key的value进行覆盖
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }

                                /**
                                 * 当前循环元素与插入的元素的key值不一致
                                 * 1.更新循环处理的节点为当前循环节点的下一个节点。
                                 * 2.判断下一个节点是否为null，
                                 *   如果为null说明当前链表已经到末尾了， 需要将key-value追加到末尾。
                                 */
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        /**
                         * 前置条件：该桶位一定不是链表
                         * 条件成立：表示当前桶位时红黑树代理节点 TreeBin
                         */
                        else if (f instanceof TreeBin) {
                            //p 表示红黑树中与你插入的key有冲突节点的话，则putTreeVal方法会返回冲突节点的引用
                            Node<K,V> p;
                            //强制设置binCount为2：因为binCount <= 1时，有其他含义，所以这里设置为2
                            binCount = 2;
                            /**
                             * 如果插入的key在红黑树有冲突节点的话，返回该冲突的节点。
                             */
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                /**
                 * binCount != 0 说明插入的key通过寻址算法找到的桶位，不为null，是一个链表或者红黑树。
                 */
                if (binCount != 0) {
                    /**如果binCount >= 8 表示处理的桶位一定是链表。*/
                    if (binCount >= TREEIFY_THRESHOLD)
                        //调用转换链表为红黑树的方法
                        treeifyBin(tab, i);

                    //产生了冲突，就会把之前key的value值，进行返回。
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }

        /**
         * 1. 统计当前table一共有多少数据
         * 2. 判断是否达到扩容阈值，触发扩容。
         */
        addCount(1L, binCount);
        return null;
    }
```

##### 3.2 addCount()

```java
private final void addCount(long x, int check) {
        /**
         * as：表示LongAdder的cells
         * b：表示LongAdder的base
         * s：表示当前map.table中元素的数量
         */
        CounterCell[] as; long b, s;
        /**
         * 条件一：(as = counterCells) != null
         *      true-->表示cells已经初始化了，当前线程应该使用hash寻址去找到合适的cell，累加数据。
         *      false-->表示需要将数据累加到base。
         * 条件二：!U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)
         *      true：表示写入base失败，需要创建cells进行累加数据。
         *      false：表示写入base成功，数据累加到base中了。
         */
        if ((as = counterCells) != null ||
            !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            /**
             * a：表示当前线程hash寻址命中的cell
             * v：表示当前线程写cell时的期望值
             * m：表示当前cells数组的长度
             * uncontended：true->未发生竞争，false->发生了竞争
             */
            CounterCell a; long v; int m;
            boolean uncontended = true;
            /**
             * 条件一：as == null || (m = as.length - 1) < 0
             *      表示当前线程是通过 上个if的条件二进入的if代码块，需要通过fullAddCount方法进行 [重试 | 扩容]
             * 条件二：(a = as[ThreadLocalRandom.getProbe() & m]) == null
             *      表示当前线程是通过上一个if的条件一进入的if代码块，需要进入fullAddCount方法对当前桶位的cell进行初始化
             * 条件三：!(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
             *      表示当前线程是通过上一个if的条件一进入的if代码块，对当前桶位的cell使用CAS方式累加
             *      true-->在CAS操作时，发生了竞争需要通过fullAddCount方法进行[重试|扩容]
             *      false-->CAS操作成功。
             */
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                !(uncontended =
                  U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, uncontended);
                //考虑到fullAddCount里面的事情比较累，就让当前线程不参与到扩容相关的逻辑了，直接返回调用点
                return;
            }
            /**
             * check <= 1 ：说明不需要扩容。
             */

            if (check <= 1)
                return;

            //获取当前散列表的元素个数：这是一个期望值(不一定准确)。
            s = sumCount();
        }
        /**表示一定是put操作调用的addCount*/
        if (check >= 0) {
            /**
             * tab：表示map.table
             * nt：表示map.nextTable
             * n：表示数组的长度
             * sc：表示sizeCtl的临时值
             */
            Node<K,V>[] tab, nt; int n, sc;

            /**
             * 自旋操作：
             *  条件一：s >= (long)(sc = sizeCtl)
             *      true：1.当前sizeCtl为一个负数，表示正在扩容中。。
             *            2.当前sizeCtl是一个正数，表示扩容阈值。
             *      false：表示当前table尚未达到扩容阈值。
             *  条件二：(tab = table) != null (恒成立)
             *  条件三：(n = tab.length) < MAXIMUM_CAPACITY
             *      true->当前table长度小于最大值限制，则可以扩容。
             */
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                   (n = tab.length) < MAXIMUM_CAPACITY) {

                //获取扩容批次的唯一时间戳。16->32的扩容标识为：1000 0000 0001 1011(32768)
                int rs = resizeStamp(n);

                /**
                 * 条件成立：表示当前table正在扩容
                 *      当前线程理论上应该协助table完成扩容。
                 */
                if (sc < 0) {
                    /**
                     * 条件一：(sc >>> RESIZE_STAMP_SHIFT) != rs
                     *      true：说明当前线程获取到的扩容唯一标识戳 不是 本次扩容
                     * 条件二：JDK1.8中有bug jira已经提出来了，其实想表达的是 sc == (rs << RESIZE_STAMP_SHIFT(16)) + 1
                     *      true：表示扩容完毕，当前线程不需要参与进来了。
                     * 条件三：JDK1.8中有bug jira已经提出来了，其实想表达的是sc == (rs << RESIZE_STAMP_SHIFT(16)) + MAX_RESIZERS
                     *      true：表示当前参与并发扩容的线程达到了最大值，65535-1
                     * 条件四：(nt = nextTable) == null
                     *      true：表示本次扩容结束。
                     * 条件五：transferIndex：扩容过程中，记录当前进度
                     *      true：表示当前已经扩容完毕，扩容时从后往前。
                     */
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    /**
                     * 前置条件：当前table正在执行扩容中。那么将 sc 加 1. 表示多了一个线程在帮助扩容。
                     * 条件成立：说明当前线程成功参与到扩容任务中。
                     * 条件失败：1.当前有多个线程都在次数尝试修改sizeCtl，产生了竞争。
                     *          2.transfer内部的线程对sizeCtl进行了修改。
                     */

                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        //协助扩容线程，持有nextTable参数
                        transfer(tab, nt);
                }

                /**
                 * 1000 0000 0001 1011 0000 0000 0000 0000 + 2 ==>
                 * 1000 0000 0001 1011 0000 0000 0000 0010
                 * 高十六位表示扩容批次的唯一时间戳
                 * 低十六位表示（1+N）：N表示当前扩容的线程数。
                 *
                 * 条件成立：说明当前线程是触发扩容的第一个线程，在transfer方法需要做一些扩容的准备工作。
                 */
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    //触发扩容条件的线程，不持有nextTable
                    transfer(tab, null);
                s = sumCount();
            }
        }
    }
```

##### 3.3 transfer()

```java
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        /**
         * n：表示扩容之前，table数组的长度
         * stride：表示给线程分配任务的步长。
         */
        int n = tab.length, stride;

        /**方便讲解源码，stride固定为16*/
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range

        /**条件成立：表示当前线程为触发扩容的线程。需要做一些扩容的准备工作*/
        if (nextTab == null) {            // initiating
            try {
                //创建一个比扩容之前大一倍的table
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            //复制给对象属性 nextTable，方便协助扩容线程拿到新表。
            nextTable = nextTab;
            //记录迁移数据整体进度(位置)的标记。
            transferIndex = n;
        }

        //nextn表示新数组的长度
        int nextn = nextTab.length;

        //FWD节点：当某个桶位的数据处理完毕后，将此同为设置为fwd节点，其他线程看到后，会进行不同的处理。
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);

        //推进标记
        boolean advance = true;
        //完成标记
        boolean finishing = false; // to ensure sweep before committing nextTab

        /**
         * i：表示分配给当前线程任务执行到桶位。
         * bound：表示分配给当前线程任务的下界限制。
         */
        for (int i = 0, bound = 0;;) {
            /**
             * f：桶位的头结点
             * fh：桶位的hash值。
             */
            Node<K,V> f; int fh;

            /**
             * 1.给当前线程任务分配任务区间
             * 2.维护当前线程进度(i 表示当前处理的桶位)。
             * 3.维护map对象全局范围内的进度。
             */
            while (advance) {
                /**
                 * nextIndex：分配任务的开始下标
                 * nextBound：分配任务结束的下标
                 */
                int nextIndex, nextBound;

                /**
                 * CASE1：
                 * 条件一：--i >= bound
                 *      true-->表示当前线程的任务尚未完成，还有响应区间的桶位需要处理，--i，处理下一个桶位。
                 *      false->表示当前线程任务已完成，或者未分配。
                 */
                if (--i >= bound || finishing)
                    advance = false;

                /**
                 * CASE2:
                 * 前置条件：当前任务已完成，或者未分配
                 * 条件一：(nextIndex = transferIndex) <= 0
                 *      true：表示任务已经分配完毕了，没有区间可分配了。设置当前线程的i为-1，跳出循环后，执行突出迁移任务相关的逻辑
                 *      false：表示全局范围内的桶位尚未分配完毕。
                 */
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }

                /**
                 * CASE3：
                 * 前置条件：1. 当前线程需要分配任务。  2.全局范围内还有桶位可分配任务。
                 * 条件成立：说明通过stride参数给当前线程分配任务成功
                 */
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    bound = nextBound;
                    //数组从0开始计数。transferIndex从1开始计数
                    i = nextIndex - 1;
                    advance = false;
                }
            }

            /**
             * CASE1：
             * 条件一：i < 0
             *      true：表示当前线程未分配到任务。
             * 条件二和条件三(永远不会成立)
             */
            if (i < 0 || i >= n || i + n >= nextn) {
                //保存sizeCtl的变量。
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    //设置sizeCtl为扩容后数组的75%的阈值。
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }

                /**
                 * 条件成立：说明设置sizeCtl 低16位 -1 成功。当前线程可以正常退出。
                 */
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {

                    /**
                     * 条件成立：说明当前线程不是最后一个退出协助扩容(transfer任务)的线程
                     */
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
             //CASE2 - CASE4 前置条件:当前线程任务尚未处理完，正在进行中。
             /**
             * CASE2：
             * 条件成立:说明单签桶位未存放数据，只需要将此处设置为FWD节点即可。
             */
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);

            /**
             * CASE3：说明当前桶位已经迁移过了，当前线程不用在处理了。直接在此更新当前线程任务索引，在此处理下一个桶位 或者 其他操作。
             */
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed

            /**
             * CASE4:
             * 前置条件：当前桶位有数据，而且Node节点不是FWD节点，说明这些数据还需要迁移。
             */
            else {
                /**给当前桶位的头结点加锁*/
                synchronized (f) {
                    /**避免在当前线程获取到头对象f期间，有其他线程对f进行了修改。再次修改就会数据不一致*/
                    if (tabAt(tab, i) == f) {
                        /**
                         * ln：表示低位链表引用
                         * hn：表示高位链表引用
                         */
                        Node<K,V> ln, hn;

                        /**条件成立：表示当前桶位时链表桶位。*/
                        if (fh >= 0) {

                            /**
                             * lastRun：获取当前链表末尾连续高位不变的node。
                             */
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }

                            /**如果末尾连续高位不变的node为0：表示此时lastRun引用的是低位链表*/
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }

                            /**迭代链表，跳出条件，当前循环节点不等于 lastRun*/
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                /**
                                 * 为什么要new Node？
                                 *  如果不创建新的Node对象，使用原来的链表，此时你又在遍历链表，就会产生问题。
                                 */
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }

                            //设置map.nextTable低位链表，就是原来所在的桶位。
                            setTabAt(nextTab, i, ln);
                            //设置map.nextTable高位链表：原来桶位+扩容前的数组长度
                            setTabAt(nextTab, i + n, hn);
                            //将旧数组的当前桶位设置为FWD节点
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        /**条件成立：表示当前桶位时红黑树代理节点：TreeBin*/
                        else if (f instanceof TreeBin) {
                            //转换头结点为TreeBin引用
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            /**
                             * lo：低位双向链表的头
                             * loTail：低位链表的尾巴
                             */
                            TreeNode<K,V> lo = null, loTail = null;
                            /**
                             * lo：高位双向链表的头
                             * loTail：高位链表的尾巴
                             */
                            TreeNode<K,V> hi = null, hiTail = null;

                            /**
                             * lc：低位链表元素数量
                             * hn：高位链表元素数量
                             */
                            int lc = 0, hc = 0;

                            /**迭代TreeBin中的双向链表，从头节点 至 尾节点*/
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                //使用当前节点构建出来 新的 TreeNode节点
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);

                                /**表示当前循环节点为低位链节点*/
                                if ((h & n) == 0) {
                                    /**条件成立：说明当前低位链表没有数据*/
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    /**说明低位链表已经有元素了，此时当前元素 追加 到低位链表的尾部*/
                                    else
                                        loTail.next = p;

                                    //将低位链的表的尾指针指向当前循环节点。
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    /**
                                     * 如果没有高位链表，直接使用原来的红黑树，
                                     * 否则需要重新构建一个红黑树节点。
                                     */
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;

                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }
```

##### 3.4 get()

```java
public V get(Object key) {
        /**
         * table：map.table
         * e：当前节点
         * p；目标节点
         * n：table数组长度
         * eh：当前元素的hash
         * ph：目标元素的hash
         */
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        int h = spread(key.hashCode());
        /**
         * 条件一：(tab = table) != null && (n = tab.length) > 0
         *      true：说明map中已经put过数据
         * 条件二：(e = tabAt(tab, (n - 1) & h)) != null
         *      true：通过key的hash值寻址到桶位，并且当前桶位中有值
         */
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                /**如果当前桶位的头结点就是我们想找的数据。*/
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            /**
             * hash值为负值表示正在扩容，这个时候查的是ForwardingNode的find方法来定位到nextTable来
             * eh=-1，说明该节点是一个ForwardingNode，正在迁移，此时调用ForwardingNode的find方法去nextTable里找。
             * eh=-2，说明该节点是一个TreeBin，此时调用TreeBin的find方法遍历红黑树，由于红黑树有可能正在旋转变色，所以find里会有读写锁。
             * eh>=0，说明该节点下挂的是一个链表，直接遍历该链表即可。
             */
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h &&
                    ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }
```

##### 3.5 FWD节点的find()

```java
static final class ForwardingNode<K,V> extends Node<K,V> {
        final Node<K,V>[] nextTable;
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer: for (Node<K,V>[] tab = nextTable;;) {
                /**
                 * e：表示为扩容而创建的新表 使用寻址算法得到的桶位头结点。
                 * n：表示为扩容创建新表的长度
                 */
                Node<K,V> e; int n;

                if (k == null || tab == null || (n = tab.length) == 0 ||
                    (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;

                /**
                 * 前置条件：扩容后的表对应的同为一定不是null，e为桶位的头结点
                 * e可能为哪些类型呢：
                 *      1.Node类型
                 *      2.TreeBin类型
                 *      3.FWD类型
                 */
                for (;;) {
                    int eh; K ek;

                    //条件成立：说明新扩容的表中命中的桶位的头结点就是我们需要查询的数据
                    if ((eh = e.hash) == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;


                    /**
                     * hash不同值的情况
                     *      正数：spread()方法会将key的hash值的高十六位参与运算并且会将符号位设置为0
                     *      -1：表示ForwardingNode结点，
                     *      -2：表示TreeBin结点。
                     */
                    if (eh < 0) {
                        /**
                         * 如果在 outer: for (Node<K,V>[] tab = nextTable;;) 获取到nextTable引用后，map又进行了一次扩容，
                         * 那么此时的tab对象还是原来的table引用，就会出现FWD节点。
                         */
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            //说明桶位为TreeBin节点，调用红黑树的.find方法查询节点。
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }
```

##### 3.6 replaceNode()

* key：需要替换的key
* value：需要替换的value
* cv：期望的value值

```java
final V replaceNode(Object key, V value, Object cv) {
        //通过spread方法让高16位参与运算 避免hash碰撞
        int hash = spread(key.hashCode());

        for (Node<K,V>[] tab = table;;) {
            /**
             * f：表示桶位头节点
             * n：表示当前table的长度
             * i：表示hash命中的桶位下标
             * fn：表示同为头节点的hash
             */
            Node<K,V> f; int n, i, fh;

            /**
             * 当前table数组还没初始化或者命中的桶为空，直接跳出循环，返回null
             */
            if (tab == null || (n = tab.length) == 0 ||
                (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            /**如果当前table正在扩容中，当前是写操作，所以当前线程需要协助table进行扩容*/
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            /**表示当前桶位为：链表或者红黑树结构*/
            else {
                /**
                 * oldVal：保留替换之前的数据引用。
                 * validated：校验标记。
                 */
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    /**典型的双重检查：避免在获取到f后，其他线程将f进行了修改*/
                    if (tabAt(tab, i) == f) {
                        /**
                         * 当前桶位节点是链表
                         */
                        if (fh >= 0) {
                            validated = true;
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;
                                /**当前Node的key就是我们要找的key*/
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    /**
                                     * cv：表示需要替换value的期望值
                                     * 条件一：cv == null
                                     *      true：替换value的期望值为null 那么就是一个删除操作
                                     * 条件二：cv == ev || (ev != null && cv.equals(ev))
                                     *      true：期望值等于真实的value值，那么就是一个替换操作
                                     */
                                    if (cv == null || cv == ev ||
                                        (ev != null && cv.equals(ev))) {
                                        oldVal = ev;

                                        /**条件成立说明当前是一个替换操作*/
                                        if (value != null)
                                            e.val = value;
                                        /**
                                         * 前提条件：说明当前是个删除操作
                                         * 条件成立：说明当前节点不是头结点，
                                         *          直接将前一个节点.next指向当前节点的next就能完成删除操作
                                         */
                                        else if (pred != null)
                                            pred.next = e.next;

                                        /**条件成立说明当前节点是头结点，所以需要将当前桶位的头节点设置为自己的下一个节点即可*/
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                /**继续遍历下一个节点*/
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        /**当前节点是TreeBin节点：红黑树*/
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;

                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;

                                /**
                                 * cv：表示需要替换value的期望值
                                 * 条件一：cv == null
                                 *      true：替换value的期望值为null 那么就是一个删除操作
                                 * 条件二：cv == ev || (ev != null && cv.equals(ev))
                                 *      true：期望值等于真实的value值，那么就是一个替换操作
                                 */
                                if (cv == null || cv == pv ||
                                    (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        //这里没做判断，直接将红黑树转换成链表
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                //当其他线程修改过桶位头结点时，当前线程 synchronized(f)锁错对象，validated为false，继续自旋
                if (validated) {
                    if (oldVal != null) {
                        /**如果传入的替换的值为null，说明当前是删除操作，需要更新当前元素个数计数器*/
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }

        }
        return null;
    }
```

#### 4. TreeBin内部类

##### 4.1 核心属性

```java
		//红黑树 根节点
        TreeNode<K,V> root;

        //链表的头结点
        volatile TreeNode<K,V> first;

        //等待的线程,(当前lockState是读锁状态)
        volatile Thread waiter;
        /**
         * 1.写锁状态：写是独占状态，以散列表来看，真正进入到TreeBin的写线程 同一时刻只有一个线程。
         * 2.读锁状态：读锁是共享状态，同一时刻可以有多个线程，同时进入到TreeBin对象中获取数据，每一个线程都会给 lockState + 4
         * 3.等待状态：一定是一个写线程再等待，当TreeBin中有读线程正在读取数据时，写线程无法修改数据，那么就将lockState最后两位设置为0b 10
         */
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock
```

##### 4.2 构造方法

```java
TreeBin(TreeNode<K,V> b) {
            //设置节点的hash为-2，表示此节点是TreeBin节点
            super(TREEBIN, null, null, null);

            //使用first引用TreeNode双向链表
            this.first = b;

            //红黑树的根节点引用
            TreeNode<K,V> r = null;

            for (TreeNode<K,V> x = b, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;

                /**强制设置当前插入节点的左右子树为null。*/
                x.left = x.right = null;

                /**条件成立：说明当前红黑树是一个空数，设置插入元素为根节点*/
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                }
                /**非第一次循环都会来到else分组，此时根节点已经有数据了。*/
                else {
                    /**
                     * k：插入节点的key
                     * h：插入节点的hash
                     * kc：插入节点的class类型
                     */
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = r
                    ) {
                        /**
                         * p：表示插入节点的父节点的一个临时节点
                         * dir：(-1,1)
                         *      -1：表示插入节点的hash值大于当前p节点的hash
                         *      1：表示插入节点的hash值小于当前p节点的hash
                         */
                        int dir, ph;
                        K pk = p.key;

                        /**
                         * 插入节点的hash值小于当前临时父节点
                         *      插入节点可能需要插入到当前临时父节点的左子节点 或者 继续在左子树上查找
                         */
                        if ((ph = p.hash) > h)
                            dir = -1;
                        /**
                         * 插入节点的hash值大于当前临时父节点
                         *      插入节点可能需要插入到当前临时父节点的右子节点 或者 继续在右子树上查找
                         */
                        else if (ph < h)
                            dir = 1;

                        /**
                         * 执行到 CASE3：说明当前插入节点的hash 与 当前临时父节点的hash一致，会在CASE3做出最终排序
                         * 最终拿到的dir 一定不是0 (-1,1)
                         */
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);
                        /**xp 想要表示的是插入节点的临时父节点*/
                        TreeNode<K,V> xp = p;

                        /**
                         * 条件成立：说明单昂前p节点 即为插入节点的父节点
                         * 条件不成立：说明当前p节点 底下还有层次，需要将p指向左子节点或者右子节点，表示继续向下搜索
                         */
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            //设置插入节点的父节点 为当前临时父节点
                            x.parent = xp;
                            /**dir = -1 需要插入到当前临时父节点的左子节点*/
                            if (dir <= 0)
                                xp.left = x;
                            /**dir = 1 需要插入到当前临时父节点的右子节点*/
                            else
                                xp.right = x;

                            /**插入节点后，红黑树的性质可能被破坏，所以需要调用平衡方法*/
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            //将r赋值给TreeBin对象的 root引用
            this.root = r;
            /**通过断言检查红黑树结构是否有问题*/
            assert checkInvariants(root);
        }
```

##### 4.3 核心方法

###### 4.3.1 lockRoot()

```java
	private final void lockRoot() {
        /**条件成立：说明此时有其他的读线程再TreeBin红黑树中读取数据*/
        if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
            contendedLock(); // offload to separate method
    }

	private final void contendedLock() {
            boolean waiting = false;
            for (int s;;) {
                /**
                 * ~WAITER = 1111 .... 1101  == 0：条件成立说明没有任何的读线程再访问红黑树
                 */
                if (((s = lockState) & ~WAITER) == 0) {
                    /**条件成立：说明写线程抢占锁成功。*/
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            //设置TreeBin对象waiter引用为null。
                            waiter = null;
                        return;
                    }
                }
                /**
                 * (lock & 0000 .... 0010) == 0 ：
                 *      条件成立：说明lock中的waiter标志位为0，此时当前线程可以设置为1了，然后将当前线程挂起
                 */
                else if ((s & WAITER) == 0) {
                    /**把倒数第二为设置为1*/
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                }
                /**条件成立：说明当前线程再CASE2中已经将lockState中表示等待着标志位的地方设置为1 同时将TreeBin.waiter 设置为了当前线程*/
                else if (waiting)
                    //将当前线程挂起
                    LockSupport.park(this);
            }
        }
```

###### 4.3.2 find()

```java
	final Node<K,V> find(int h, Object k) {
            if (k != null) {
                for (Node<K,V> e = first; e != null; ) {
                    /**
                     * s：保存的是lock临时状态
                     * ek：链表当前节点的key
                     */
                    int s; K ek;
                    /**
                     * (WAITER | WRITER) ==> (0010 | 0001 ) ==> 0011
                     * (lockState & 0011) != 0
                     * 条件成立：说明当前TreeBin 有等待这线程 或者有写操作正在加锁
                     *          此时通过双向链表进行查询
                     */
                    if (((s = lockState) & (WAITER|WRITER)) != 0) {
                        if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    }
                    /**
                     * 前置条件：当前TreeBin中 等待着线程或者写线程都没有
                     * 条件成立：说明添加读锁成功
                     */
                    else if (U.compareAndSwapInt(this, LOCKSTATE, s,
                                                 s + READER)) {
                        TreeNode<K,V> r, p;
                        try {
                            p = ((r = root) == null ? null :
                                 r.findTreeNode(h, k, null));
                        } finally {
                            //w表示等待者线程
                            Thread w;
                            /**
                             * 1.U.getAndAddInt(this, LOCKSTATE, -READER)：读操作完成，将本次读锁释放(减去[READER|4])
                             *  并将减去READER之前的值返回。
                             * 2.如果释放当前读锁后等于(READER|WAITER) ==> 0110 ==>表示当前读线程为最后一个读线程，且有一个线程再等待
                             * 3.(w = waiter) != null 条件成立：说明有一个写线程再在等待
                             */
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                                (READER|WAITER) && (w = waiter) != null)
                                //使用unpark让写线程恢复运行状态。
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }
```

###### 4.3.3 putTreeVal()

```java
	final TreeNode<K,V> putTreeVal(int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if (p == null) {
                    first = root = new TreeNode<K,V>(h, k, v, null, null);
                    break;
                }
                else if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.findTreeNode(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                /**
                 * 当前循环的节点就是x节点的父节点
                 */
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    /**
                     * x：表示插入节点
                     * f：表示修改链表之前的 链表的头结点
                     */
                    TreeNode<K,V> x, f = first;
                    first = x = new TreeNode<K,V>(h, k, v, f, xp);

                    /**条件成立，说明链表中有数据*/
                    if (f != null)
                        f.prev = x;

                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;


                    if (!xp.red)
                        x.red = true;
                    else {
                        //表示当前新插入的节点后，破坏了红黑树的节点
                        lockRoot();
                        try {
                            //平衡红黑树，使其再次符合规范
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }
```

###### 4.3.4 balanceInsertion()

```java
	static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                /**条件成立：说明当前红黑树为空树，将插入的x节点设置为root节点*/
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                /**条件成立：插入x节点并没有影响红黑树的结构*/
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                /**
                 * 前提条件：插入节点的父节点不为空，并且为红色
                 * 条件成立：说明插入节点的父节点为爷爷节点的左子树
                 */
                if (xp == (xppl = xpp.left)) {
                    /**条件成立：说明是 情景4.1：叔叔节点存在，并且为红色（父-叔 双红）
                     *  将父节点和叔叔节点染色为黑色，将爷爷节点染色为红色，并且再以爷爷节点为当前节点，然后进行下一轮处理。
                     */
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    /**
                     * 前置条件：此时的叔叔节点不为红色，可能为null或者为黑色
                     */
                    else {
                        /**
                         * 情景4.2.2：插入节点为其父节点的右子节点（LR情况）
                         *      1. 以父节点进行一次左旋，得到LL双红的情景(4.2.1),
                         *      2. 然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
                         */
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        /**
                         * 通过上面那个if，此时的节点一定是LL情况
                         * 情景4.2.1：插入节点为其父节点的左子节点（LL情况）
                         * 将父节点染色成黑色，将爷爷节点染色为红色，然后以爷爷节点进行右旋。
                         */
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }
                /**条件成立：说明插入节点的父节点为爷爷节点的右子树*/
                else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
```





#### 5. 小结

##### 5.1 key的hash值

* 正数：所有key会通过spread()方法会将key的hash值的**高十六位参与运算**(减少hash碰撞)并且会通过和Integer.MAX_VALUE进行位运算将**符号位设置为0**。
* -1：表示当前节点为`ForwardingNode`结点（数组正在扩容）。
* -2：表示`TreeBin`结点（红黑树的代理操作节点）。

##### 5.2 sizeCtl属性

* 0：用来控制table的初始化和扩容操作
* -1：表示table正在初始化
* -N：表示table正在扩容。
  * 高16位：表示扩容批次的唯一时间戳，`resizeStamp()`方法会将获取的扩容唯一时间戳的最高位设置为1
  * 低16位：表示（1+N）：N表示当前扩容的线程数。

* 其余情况：
  * 如果table未初始化，表示table需要初始化的大小。
  * 如果table初始化完成，表示table的扩容阈值，默认是table大小的0.75倍。