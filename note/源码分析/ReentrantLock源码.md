#### 1. 核心属性

##### 1.1 AbstractQueuedSynchronizer的属性

```java
	/** 指向队列中的头结点：任何时候头结点对应的线程都是当前持锁线程。*/
    private transient volatile Node head;

    /** 指向阻塞队列中的尾节点 (阻塞队列不包含 头结点 head.next --> tail 认为是阻塞队列)*/
    private transient volatile Node tail;

    /**表示资源
     * 独占模式：0 表示未加锁状态 >1 表示已经加锁状态
     * */
    private volatile int state;
```



##### 1.2 内部类Node的属性

```java
		/** 枚举：共享模式 */
        static final Node SHARED = new Node();
        /** 枚举：独占模式 */
        static final Node EXCLUSIVE = null;

        /** 当前节点处于取消状态 */
        static final int CANCELLED =  1;
        /**  注释：表示当前节点需要唤醒他的后继节点。（SIGNAL 表示的其实是后继节点的状态，需要当前节点去喊他...）*/
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
        static final int PROPAGATE = -3;

        /**node状态：可选值:PROPAGATE(-3)-->CONDITION(-2)-->SIGNAL(-1)-->0-->CANCELLED(1)
         * waitStatus == 0 默认状态
         * waitState > 0 取消状态
         * waitState == -1 表示当前node如果是head节点时，释放锁之后，需要唤醒他的后继节点。
         * */
        volatile int waitStatus;

        /** Node需要构建成 fifo 队列，所以 prev 指向前继节点 */
        volatile Node prev;

        /** Node需要构建成 fifo 队列，所以 prev 指向后继节点 */
        volatile Node next;

        /**当前Node封装的线程*/
        volatile Thread thread;

        /**reentrantLock中未用到，暂时不说*/
        Node nextWaiter;
```

#### 2. 公平锁核心方法--FairSync

##### 2.1 Lock方法执行流程

###### 2.1.1 FairSync---lock()

```java
    final void lock() {
        //调用AQS中的acquire方法
        acquire(1);
    }
```

###### 2.1.2 AQS---acquire()

```java
/**AQS中的acquire方法*/
public final void acquire(int arg) {
    /**
     * 条件一：!tryAcquire(arg) 尝试获取锁
     *      true：获取锁成功
     *      false：获取锁失败
     * 条件二：acquireQueued(addWaiter(Node.EXCLUSIVE), arg)
     *      2.1：addWaiter() 将当前线程封装成Node入队。
     *      2.2：acquireQueued() 挂起当前线程，唤醒后相关逻辑...。
     *      true：表示挂起过程中被中断唤醒过
     *      false：表示未被中断过。
     */
    if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        /**再次设置中断标记位 true*/
        selfInterrupt();
}
```

###### 2.1.3 FairSync---tryAcquire()

```java
/**
     * 抢占成功(或进行了重入)：返回true
     * 抢占失败：返回false
     */
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        /**条件成立：表示当前AQS处于无锁状态*/
        if (c == 0) {
            /**
             * 因为当前为公平锁：任何时候都需要检查 当前线程之前是否还其他的Node等待。
             * 条件一：!hasQueuedPredecessors()
             *      true：表示当前线程前无等待者，可以尝试获取锁。
             *      false：当前线程前有等待者，当前线程需要入队等待
             * 条件二：compareAndSetState(0, acquires)
             *      true：表示当前线程抢占锁成功。
             *      false：表示存在竞争，当前线程竞争锁失败。
             */
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                /**抢占锁成功后：设置当前线程未独占者线程。*/
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        /**执行到这里有几种情况？
         *  1. c > 0 ：表示当前锁已经被线程占用。需要判断持有锁的线程是不是当前线程，
         *  因为ReenntrantLock是可重入锁。
         *
         *  条件成立：说明当前线程就是独占锁的线程。
         * */
        else if (current == getExclusiveOwnerThread()) {
            /**锁重入的逻辑*/
            int nextc = c + acquires;
            /**越界判断，当重入的深度很深时，会导致 nextc 小于0了*/
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            //更新操作
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

###### 2.1.3 AQS---addWaiter()

```java
/**AQS中的addWaiter()方法
 * Node.EXCLUSIVE:表示独占模式
 * */
private Node addWaiter(Node mode) {
    /**将当前线程封装成Node对象*/
    Node node = new Node(Thread.currentThread(), mode);

    /**快速入队*/
    Node pred = tail;
    /**条件成立：说明阻塞队列中已经有Node了*/
    if (pred != null) {
        /**1. 当前节点的prev指向尾节点元素。 */
        node.prev = pred;
        /**2. 尝试通过CAS方式设置尾节点为当前Node*/
        if (compareAndSetTail(pred, node)) {
            /**3. 将前置节点的next指向当前Node。完成双向绑定*/
            pred.next = node;
            return node;
        }
    }

    /**完整入队操作
     * 什么情况会执行到这里呢？
     * 1.当前队列是空队列 tail == null
     * 2.CAS入队竞争失败。
     * */
    enq(node);
    return node;
}
```

###### 2.1.4 AQS---enq()

```java
/**AQS的enq()方法*/
private Node enq(final Node node) {
    /**自旋入队，只有当前node入队成功后，才回跳出循环*/
    for (;;) {
        Node t = tail;
        /**
         * 条件成立：
         * 1.当前线程是空队列，当前线程可能是第一个获取锁失败的线程
         *  (当前时刻可能存在一批获取锁失败的线程...)
         * */
        if (t == null) { // Must initialize
            /**
             * 作为当前持锁线程的第一个后继线程，需要做什么事
             * 1.因为当前持锁的线程，获取锁时，直接tryAcquire成功了，没有向阻塞队列中添加任何node，
             * 所以作为后继，需要将持锁线程设置为head节点
             * 条件成立：说明通过CAS操作设置head节点成功，同时将tail节点也指向head。
             * 不管CAS操作有没有成功都会继续自旋。
             */
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            /**
             * 当前阻塞队列中一定有Node节点，
             * 1.将当前节点的prev设置为尾部节点
             */
            node.prev = t;
            /**2.通过CAS方式将tail指向当前节点*/
            if (compareAndSetTail(t, node)) {
                /**3. 将前置节点的next指向当前Node。完成双向绑定*/
                t.next = node;
                /**执行到这里。说的当前Node已经入队成功，需要跳出自旋，然后将当前Node节点返回*/
                return t;
            }
        }
    }
}
```

###### 2.1.5 AQS---acquireQueued()

```java
/**AQS中的acquireQueued()方法
 * 1.当前线程有没有被park?挂起？ 没有 ==> 挂起的操作。
 * 2.唤醒之后的逻辑在哪呢？ ==> 唤醒后的逻辑。
 * @param node ：就是当前线程分装成的Node对象，并且该Node已经入队成功了
 * @param arg ： 表示当前线程抢占资源成功后，用于更新state值
 * @return
 * */
final boolean acquireQueued(final Node node, int arg) {
    /**
     * failed:表示当前线程是否抢占锁成功。
     *  true：抢占锁成功。普通情况下【lock】 当前线程早晚会拿到锁
     *  false：抢占锁失败，需要执行出队的逻辑。
     */
    boolean failed = true;
    try {
        /**当前线程是否被中断*/
        boolean interrupted = false;
        for (;;) {
            /**获取当前Node节点的前一个节点。*/
            final Node p = node.predecessor();
            /**
             * 条件一：p == head
             *      true：说明当前Node为head.next节点，有抢占锁的资格
             * 条件二：tryAcquire(arg)
             *      true：表示抢占锁成功。
             */
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            /**
             * shouldParkAfterFailedAcquire():当前线程获取锁资源失败后，是否需要挂起呢？
             *      true：当前线程需要挂起
             *      false：不需要挂起
             * parkAndCheckInterrupt()
             *  挂起当前线程。并且唤醒后 返回当前线程的中断标记
             *  唤醒：1.正常唤醒，其他线程unpark()  2.其他线程给当前挂起的线程一个中断信号。
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

###### 2.1.6 AQS--shouldParkAfterFailedAcquire()

```java
/**
 * AQS中的shouldParkAfterFailedAcquire()方法
 * 总结：
 * 1.当前节点的前置节点是取消状态，第一次来到方法时会越过取消状态的节点，第二次会返回true，然后park当前线程。
 * 2.当前节点的前置节点状态是0，当前线程会设置前置节点的状态为-1，第二次自旋来到这个方法时，会返回true，然后park当前线程。
 *
 * @param pred 当前线程Node的前置节点
 * @param node 当前线程Node
 * @return true：当前线程需要挂起
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    /**
     * waitStatus:线程的状态
     * SIGNAL(-1)：表示当前节点释放锁之后会唤醒它的第一个后继节点
     * 默认状态(0):
     * CANCELED(1):表示当前线程为取消状态。
     */
    int ws = pred.waitStatus;

    /**条件成立：表示前置节点是可以唤醒当前节点*/
    if (ws == Node.SIGNAL)
        return true;

    /**条件成立：表示前置节点是CANCELED状态*/
    if (ws > 0) {
        /**
         * 循环向前找队列中的 waitStatus <= 0的节点
         * 其实就是将CANCELED状态的节点移除队列
         * */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /**
         * 当前node前置节点的状态就是 默认0 的状态
         * 将当前Node的前置节点状态设置为SIGNAL，表示前置节点释放锁之后，需要唤醒我。
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```

###### 2.1.7 AQS---parkAndCheckInterrupt()

```java
/**
 * AQS的方法
 * park当前线程，将当前线程挂起，唤醒后返回当前线程 是否为中断信号唤醒
 * @return
 */
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
}
```

##### 2.2 UnLock方法执行流程

###### 2.2.1 AQS---release()

```java
/**
 * AQS的release方法
 * ReentrantLock.unlock() --> sync.release() 【AQS提供的release方法】
 * @param arg
 * @return
 */
public final boolean release(int arg) {
    /**
     * tryRelease() ：尝试释放锁
     *      true：表示当前线程已经完全释放锁了
     *      false：表示当前线程尚未完全释放锁
     * */
    if (tryRelease(arg)) {
        Node h = head;

        /**
         * 条件一：h != null
         *      true：说明当前队列中的head节点已经初始化过了，ReentrantLock在使用期间发生过多线程竞争了。。。
         * 条件二：h.waitStatus != 0
         *      true：说明当前head后面一定插入过node节点。
         */
        if (h != null && h.waitStatus != 0)
            /**唤醒后继节点*/
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

###### 2.2.2 Sync---TryRelease()

```java
/**
 * 父类Sync的tryRelease()
 * @param releases
 * @return
 */
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;

    /**
     * 条件成立：说明当前线程并不是获得锁的线程，直接返回异常
     */
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();

    /**free：表示释放完全释放锁*/
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

###### 2.2.3 AQS---unparkSuccessor()

```java
/**
 * AQS中的unparkSuccessor()方法
 * @param node
 */
private void unparkSuccessor(Node node) {
    /**获取当前节点的状态*/
    int ws = node.waitStatus;
    if (ws < 0)//ws == -1
        compareAndSetWaitStatus(node, ws, 0);

    //s是当前节点的第一个后继节点。
    Node s = node.next;

    /**
     * 条件一：s == null
     *  s什么时候等于空呢？
     *  1.当前节点就是tail节点 s == null
     *  2.当新节点入未完成时，
     *      (1.设置新节点的prev指向 tail节点， 2.通过CAS方式设置tail为新节点 3.(这一步未完成)pred.next指向新节点)
     * 条件二：s.waitStatus > 0
     *  当前node节点的后继节点是取消状态...需要找一个合适的，可以被唤醒的节点...
     */
    if (s == null || s.waitStatus > 0) {
        /**查找可以被唤醒的节点... */
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                /**
                 * 注意：这里找到了并没有进行跳出循环，而是继续往前进行查找
                 * 说明是找到里当前Node最近的并且可以被唤醒的节点
                 * */
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

##### 2.3. 取消竞争方法

###### AQS---cancelAcquire()

```java
/**
 * AQS中的cancelAcquire()方法
 *  取消指定的Node参与竞争。
 * @param node
 */
private void cancelAcquire(Node node) {
    // Ignore if node doesn't exist
    if (node == null)
        return;

    /**需要取消排队了，所以当前Node内部关联的当前线程，设置为null*/
    node.thread = null;

    //获取当前节点的前驱节点
    Node pred = node.prev;
    /**waitStatus > 0 表示当前节点被取消*/
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    /**
     * 拿到不是取消状态的Node节点的后继节点next
     *  1.此时的Next就是当前节点
     *  2.此时的Next也有可能是取消状态的节点。
     * */
    Node predNext = pred.next;

    // 将当前Node状态设置为取消状态 waitStatus == 1
    node.waitStatus = Node.CANCELLED;

    /**
     * 当前取消排队的Node所在的队列的位置不同，执行的出队策略是不一样的。
     * 一共分为三种情况：
     *  1.当前Node是队尾节点 tail == node。
     *  2.当前node不是head.next节点，也不是tail。
     *  3.当前Node是head.next节点。
     *
     *  条件一：node == tail
     *      true：说明当前node就是队尾节点
     *  条件二：compareAndSetTail(node, pred)
     *      true：说明通过CAS操作修改tail成功
     */
    if (node == tail && compareAndSetTail(node, pred)) {
        //修改pred.next --> null，完成出队的操作
        compareAndSetNext(pred, predNext, null);
    } else {
        //保存节点的状态。
        int ws;
        /**
         * 第二种情况：当前Node不是head.next节点也不是tail节点
         * 条件一：pred != head
         *      true：说明当前node不是head.next节点
         * 条件二：((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL)))
         *      条件2.1：(ws = pred.waitStatus) == Node.SIGNAL
         *          true：说明node节点的前驱节点状态是Signal状态。
         *          false：说明前驱节点的状态可能是0
         *      条件2.2：(ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))
         *          true：前驱节点状态 <= 0 则设置前驱节点状态为Signal状态..表示要唤醒后继节点。
         */
        if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                        (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
            /**
             * 情况二：当前node不是head.newxt节点，也不是tail
             * 出队操作：pred.next --> node.next节点.
             * 当node.next节点被唤醒后 进行自旋，
             * 因为此时的node.next的前置节点指向的还不是head节点，所以会通过 shouldParkAfterFailedAcquire
             * 让node.next节点的前置越过取消状态的节点，指向头节点 ，就会完成真正的出队。然后下一次自旋就会获得锁
             */
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);
        } else {
            /**
             * 当前node节点是head.next节点
             * 因为当前node的状态已经设置为 CANCELLED
             * unparkSuccessor会唤醒一个后置节点，然后执行和情况二相同的逻辑
             */
            unparkSuccessor(node);
        }

        node.next = node; // help GC
    }
}
```

#### 3. 条件队列--Condition

##### 3.1 核心属性--ConditionObject

```java
/** 指向条件队列中的第一个Node节点*/
private transient Node firstWaiter;
/** 指向条件队列中的最后一个Node节点 */
private transient Node lastWaiter;
```

##### 3.2 await代码执行流程

###### 3.2.1 await()

```java
public final void await() throws InterruptedException {
    /**判断当前线程是否是中断状态，如果是直接返回中断异常*/
    if (Thread.interrupted())
        throw new InterruptedException();

    /**将调用await的线程包装成Node并且加入到条件队列中，并返回当前线程的Node*/
    Node node = addConditionWaiter();
    /**
     * 完全释放掉当前线程对应的锁(将state = 0)
     * 为什么要释放锁呢？
     *  加着锁挂起后，其他线程无法获取锁。
     *
     *  当被迁移到 `阻塞队列`后 被再次唤醒并获取到Lock锁时，需要将state设置为savedState
     * */
    int savedState = fullyRelease(node);

    /**
     * 0：在condition队列挂起期间未接收到中断信号。
     * -1：在condition队列挂起期间接收到中断信号。
     * 1：在condition队列挂起期间未接收到中断信号，但是迁移到 `阻塞队列` 之后 接收过中断信号。
     * */
    int interruptMode = 0;
    /**
     * isOnSyncQueue(node)
     *      true：表示当前线程对应的Node已经迁移到 `阻塞队列` 了
     *      false：表示当前线程对应的Node还在 `条件队列` 中，需要继续park了。
     */
    while (!isOnSyncQueue(node)) {
        /**
         * 挂起当前Node对应的线程
         * 什么时候会被唤醒？
         *  1. 外部线程调用了signal()方法，转移当前节点到阻塞队列中，并且当前节点在阻塞队列中获取到锁之后。
         *  2. 转移到阻塞队列之后，发现阻塞队列中的前驱节点是取消状态。
         *  3. 当前节点挂起期间，被外部线程使用中断唤醒。
         * */
        LockSupport.park(this);
        /**
         * checkInterruptWhileWaiting():
         *      就算在condition队列挂起期间，线程发生了中断，对应的node也会被迁移到 `阻塞队列`
         */
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }

    /**
     * 执行到这里：说明当前node已经迁移到 `阻塞队列`了
     *
     * 条件二：acquireQueued：竞争队列的逻辑
     *      true：说明在阻塞队列中被中断唤醒过
     * 条件二：interruptMode != THROW_IE
     *      true：说明当前node在条件队列中未发生中断
     */
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;

    /**
     * 条件什么时候成立呢？
     * 就是Node在条件队列时，如果被外部线程中断唤醒，会加入阻塞队列，
     * 但是并未设置node.nextWaiter == null
     */
    if (node.nextWaiter != null) // clean up if cancelled
        /**清理条件队列内取消状态的节点*/
        unlinkCancelledWaiters();
    /**条件成立：说明当前node发生过中断(1.条件队列内的挂起。2.条件队列我的挂起)*/
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

###### 3.2.2 addConditionWaiter()

```java
/**
 * 调用await方法的线程都是持锁状态的，也就是说addConditionWaiter方法中不存在并发。
 * @return its new wait node
 */
private Node addConditionWaiter() {
    /**获取当前条件队列的尾节点引用。*/
    Node t = lastWaiter;
    /**
     * 条件一：t ！= null
     *      true：说明当前条件队列中已经有node元素了。
     * 条件二：t.waitStatus != Node.CONDITION (node在条件队列中时，它的状态是CONDITION：-2)
     *      true：说明当前Node发生中断了。
     */
    if (t != null && t.waitStatus != Node.CONDITION) {
        /**清理条件队列中所有取消状态的节点*/
        unlinkCancelledWaiters();
        /**重新获取条件队列的尾结点引用*/
        t = lastWaiter;
    }
    /**为当前线程创建Node节点，设置状态为CONDITION：-2*/
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    /**条件成立：说明条件队列中没有任何元素，当前线程是第一个进入队列的元素*/
    if (t == null)
        firstWaiter = node;
    else
        t.nextWaiter = node;

    /**将当前队尾引用指向当前node节点*/
    lastWaiter = node;
    return node;
}
```

###### 3.2.3 fullyRelease(）

```java
final int fullyRelease(Node node) {
    /**完全释放锁是否成功：
     * 当failed失败时，说明当前线程是未持有锁调用await方法的线程...(错误写法)
     * 在finally代码块中，会将已经加入条件队列的当前线程对应的node节点状态设置为取消状态
     * 后继节点会将取消状态的节点给清理出去
     * */
    boolean failed = true;
    try {
        /**获取当前线程持有的state的值的总数*/
        int savedState = getState();

        /**
         * release(savedState)：释放当前线程持有的所有锁。
         */
        if (release(savedState)) {
            /**失败标记设置为false*/
            failed = false;
            /**
             * 返回当前线程释放的state值。
             * 当被迁移到 `阻塞队列`后 被再次唤醒并获取到Lock锁时，需要将state设置为savedState
             * */
            return savedState;
        } else {
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED;
    }
}
```

###### 3.2.4 isOnSyncQueue()

```java
final boolean isOnSyncQueue(Node node) {
    /**
     * 条件一：node.waitStatus == Node.CONDITION
     *      true：说明当前节点一定是在 `条件队列` 中，
     *            因为signal方法迁移节点到 `阻塞队列` 时，会将Node的状态设置为0。
     * 条件二：node.prev == null
     *  前置条件：node.waitStatus != Node.CONDITION
     *      1. node.waitStatus == 0 : 表示当前节点已经被signal了。
     *          因为signal方法是先修改状态，再迁移，可能当前node还未真的进行迁移操作。
     *      2. node.waitStatus == 1 ： 当前线程是未持有锁调用await方法...最终会将node的状态修改成取消状态。
     */
    if (node.waitStatus == Node.CONDITION || node.prev == null)
        return false;

    /**
     * 执行到这里，会是哪些情况？
     * node.waitStatus ！= Node.CONDITION && node.prev != null
     * ==> 可以排除掉 node.waitStatus == 1 取消状态。 为什么？
     *      因为signal方式是不会把取消状态的node迁移走的，设置prev引用的逻辑是迁移到阻塞队列逻辑中设置的(enq())。
     * 入队的逻辑：1.设置node.prev = tail 2.CAS设置当前node为tail 3.pred.next = node
     *
     * node.next != null：条件成立说明当前节点已经成功入队到阻塞队列了。
     */
    if (node.next != null) // If has successor, it must be on queue
        return true;
    /**
     * 执行到这里，说明当前节点的状态为 node.prev != null && node.waitStatus == 0
     *
     * findNodeFromTail：从阻塞队列的尾部开始向前遍历，查找node，如果查找到 返回true。
     * 当前node有可能正在signal迁移过程中，还未完成。
     */
    return findNodeFromTail(node);
}
```

###### 3.2.5 AQS---acquireQueued()

```java
/**AQS中的acquireQueued()方法
 * 1.当前线程有没有被park?挂起？ 没有 ==> 挂起的操作。
 * 2.唤醒之后的逻辑在哪呢？ ==> 唤醒后的逻辑。
 * @param node ：就是当前线程分装成的Node对象，并且该Node已经入队成功了
 * @param arg ： 表示当前线程抢占资源成功后，用于更新state值
 * @return
 * */
final boolean acquireQueued(final Node node, int arg) {
    /**
     * failed:表示当前线程是否抢占锁成功。
     *  true：抢占锁成功。普通情况下【lock】 当前线程早晚会拿到锁
     *  false：抢占锁失败，需要执行出队的逻辑。
     */
    boolean failed = true;
    try {
        /**当前线程是否被中断*/
        boolean interrupted = false;
        for (;;) {
            /**获取当前Node节点的前一个节点。*/
            final Node p = node.predecessor();
            /**
             * 条件一：p == head
             *      true：说明当前Node为head.next节点，有抢占锁的资格
             * 条件二：tryAcquire(arg)
             *      true：表示抢占锁成功。
             */
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            /**
             * shouldParkAfterFailedAcquire():当前线程获取锁资源失败后，是否需要挂起呢？
             *      true：当前线程需要挂起
             *      false：不需要挂起
             * parkAndCheckInterrupt()
             *  挂起当前线程。并且唤醒后 返回当前线程的中断标记
             *  唤醒：1.正常唤醒，其他线程unpark()  2.其他线程给当前挂起的线程一个中断信号。
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

###### 3.2.6 reportInterruptAfterWait()

```java
private void reportInterruptAfterWait(int interruptMode)
    throws InterruptedException {
    /**条件成立：说明在条件队列内发生过中断，此时await方法抛出中断异常*/
    if (interruptMode == THROW_IE)
        throw new InterruptedException();

    /**
     * 条件成立：说明在条件队列外发生的中断，此时设置当前线程的中断标记位为true
     * 中断处理交给你的业务处理，如果你不处理，什么都不会发生
     * */
    else if (interruptMode == REINTERRUPT)
        selfInterrupt();
}
```

#### 3.3 signal代码执行流程

###### 3.3.1 signal()

```java
public final void signal() {
    /**判断调用signal的线程是否是独占锁持有的线程*/
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();

    /**获取条件队列的第一个Node*/
    Node first = firstWaiter;

    if (first != null)
        /**第一个节点不为null，则将第一个节点迁移到队列的逻辑。*/
        doSignal(first);
}
```

###### 3.3.2 doSignal()

```java
private void doSignal(Node first) {
    do {
        /**
         * 因为当前firstWaiter马上要出条件队列了
         * 所以firstWaiter为当前节点的下一个节点。
         *
         * 吐过当前节点的下一个节点为null，说明当前条件队列只有一个节点了.将lastWaiter=null
         * */
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;

        /**firstWaiter出条件队列。*/
        first.nextWaiter = null;
        /**
         * transferForSignal(first):
         *      true：表示当前first节点迁移到阻塞队列成功，
         *      false：迁移失败。
         *
         * 如果first迁移失败，将first更新为first.next 继续尝试迁移。
         * 直到迁移某个节点成功 或者 条件队列为null
         */
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```

###### 3.3.3 transferForSignal()

```java
final boolean transferForSignal(Node node) {
    /**
     * CAS修改当前节点的状态，修改为0，因为当前节点马上要迁移到阻塞队列了
     *  true：当前节点在条件队列中状态正常
     *  false：1.取消状态 (线程await，未持有锁，最终线程对应的Node会设置为取消状态)
     *         2.取消状态 (node对应的线程挂起期间 被其他线程使用中断信号唤醒过，就会主动进入到阻塞队列，修改状态为0)
     */
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;

    /**
     * enq()最终会将当前Node入队到阻塞队列。
     * p：当前节点在阻塞队列的前驱节点。
     */
    Node p = enq(node);
    /**前驱节点的状态*/
    int ws = p.waitStatus;
    /**
     * 条件一：ws > 0
     *      true：说明前驱节点的状态在阻塞队列中是取消状态，唤醒当前节点
     * 条件二：!compareAndSetWaitStatus(p, ws, Node.SIGNAL)
     *      true：表示前驱节点修改失败。
     *          前驱节点是通过lockInterrupt入队的node时，是会响应中断的，
     *          外部线程给前驱线程中断信号之后，前驱node会将状态改为取消状态，并执行出队逻辑
     *      false：表示前驱节点修改成功。
     *
     * 前驱节点状态不是 (0和-1 或者 CAS失败：代表p节点无法来唤醒node节点)直接唤醒当前线程
     *
     * */
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        //唤醒当前node对应的线程
        LockSupport.unpark(node.thread);
    return true;
}
```

###### 3.3.4 enq()

```java
/**AQS的enq()方法*/
private Node enq(final Node node) {
    /**自旋入队，只有当前node入队成功后，才回跳出循环*/
    for (;;) {
        Node t = tail;
        /**
         * 条件成立：
         * 1.当前线程是空队列，当前线程可能是第一个获取锁失败的线程
         *  (当前时刻可能存在一批获取锁失败的线程...)
         * */
        if (t == null) { // Must initialize
            /**
             * 作为当前持锁线程的第一个后继线程，需要做什么事
             * 1.因为当前持锁的线程，获取锁时，直接tryAcquire成功了，没有向阻塞队列中添加任何node，
             * 所以作为后继，需要将持锁线程设置为head节点
             * 条件成立：说明通过CAS操作设置head节点成功，同时将tail节点也指向head。
             * 不管CAS操作有没有成功都会继续自旋。
             */
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            /**
             * 当前阻塞队列中一定有Node节点，
             * 1.将当前节点的prev设置为尾部节点
             */
            node.prev = t;
            /**2.通过CAS方式将tail指向当前节点*/
            if (compareAndSetTail(t, node)) {
                /**3. 将前置节点的next指向当前Node。完成双向绑定*/
                t.next = node;
                /**执行到这里。说的当前Node已经入队成功，需要跳出自旋，然后将当前Node节点的前置节点返回*/
                return t;
            }
        }
    }
}
```

#### 4. 共享模式-CountDownLatch

##### 4.1 await()执行流程

###### 4.1.1 await()

```java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

###### 4.1.2 acquireSharedInterruptibly()

```java
/**
 * AQS中的方法
 * @param arg
 * @throws InterruptedException
 */
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    /**条件成立：说明当前调用await方法的线程已经是中断状态，直接抛出异常*/
    if (Thread.interrupted())
        throw new InterruptedException();
    /**
     * 条件成立：说明当前AQS.state > 0,此时线程入队，然后等待唤醒。
     * 条件不成立：说明当前AQS.state ==0 ，此时就不会阻塞线程了。
     * */
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```

###### 4.1.3 tryAcquireShared()

```java
/**
 * 返回-1：state状态是大于0的
 * @param acquires
 * @return
 */
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```

###### 4.1.4 doAcquireSharedInterruptibly()

```java
/**
 * AQS的方法
 * @param arg
 * @throws InterruptedException
 */
private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {

    /**将调用lach.await()方法的线程，封装成Node加入AQS的阻塞队列中*/
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            /**获取当前节点的前驱节点*/
            final Node p = node.predecessor();
            /**条件成立：说明当前节点为head.next节点，就有权利获取共享锁*/
            if (p == head) {
                /**
                 * r==1 : state>0
                 * r==-1：state>0
                 * */
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }
            /**
             * shouldParkAfterFailedAcquire()
             *      会给当前节点找到一个唤醒自己的节点，并将找到的节点状态设置为 Signal(-1)。
             * parkAndCheckInterrupt()
             *      挂起当前节点
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

###### 4.1.5 setHeadAndPropagate()

```java
/**
 * AQS中的方法
 * 设置当前节点为head节点，并且向后传播(依次唤醒！)
 * */
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // Record old head for check below
    /**将当前节点设置为新的head结点*/
    setHead(node);

    /**
     * 条件一：propagate > 0
     *  进入这个方法时，propagate的值一定是1，恒成立
     */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
        /**当前节点的后继节点*/
        Node s = node.next;
        /***
         * 条件一：s == null
         *      什么时候成立？ 当前node节点已经是tail了，条件成立
         * 条件二：s.isShared()
         *      前置条件：s ！= null，要求后继节点的模式必须是共享模式
         */
        if (s == null || s.isShared())
            /**基本所有情况都会进入到if语句*/
            doReleaseShared();
    }
}
```

###### 4.1.6 doReleaseShared()

```java
/**
 * AQS中的方法
 * 都有哪几种情况会调用到doReleaseShared()方法呢？
 * 1.latch.countDown --> AQS.state == 0 --> doReleaseShared()唤醒当前阻塞队列内的 head.next 对应的线程。
 * 2.被唤醒的线程 --> doAcquireSharedInterruptibly()方法中被唤醒 --> setHeadAndPropagate() --> doReleaseShared()
 */
private void doReleaseShared() {
    for (;;) {
        /**获取当前AQS内的头节点*/
        Node h = head;
        /**
         * 条件一：h != null：
         *      true：说明阻塞队列不为空...
         *      false：说明latch创建出来之后，没有任何线程调用过await()方法之前，
         *          有线程调用countDown()操作且出发了唤醒阻塞节点的逻辑。
         * 条件二：h != tail
         *      true：说明当前阻塞队列内出来head节点外还有其他节点。
         *      false：说明 head == tail，什么时候会出现这种情况？
         *          1.正常情况：依次获取到共享锁，当前线程执行到这里时(这个线程就是tail节点。)
         *          2.第一个调用await()方法的 (线程A) 与 调用countDown()方法且触发唤醒阻塞节点的 (线程B) 出现了并发..
         *              线程A 是第一个调用await()的线程，此时阻塞队列中什么都没有，它需要创建一个Head节点，
         *              然后再次自旋将自己入队。
         *              在 线程A 创建Head成功，但是未入队之前，线程B调用countDown 触发了doReleaseShared()唤醒阻塞的节点
         */
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            /**当前head状态为signal 说明后继节点并没有被唤醒过*/
            if (ws == Node.SIGNAL) {
                /**
                 * 唤醒后继节点之前，将head节点的状态改为0
                 * 此时为什么要用CAS操作呢？
                 * 当doReleaseShared方法存在多个线程唤醒 head.next逻辑时，CAS可能会失败...
                 * 案例：t3在if(h == head) 返回false是，t3会继续自旋参与到唤醒下一个head.next的逻辑
                 *      当t3和t4都进入当前if(ws == Node.SIGNAL)，t3执行到当前CAS成功时，t4会失败。
                 * */
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                    !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        /**
         * 条件成立：
         * 1. 说明刚刚唤醒的后继节点还没有执行到setHeadAndPropagate()中的setHead方法，当前线程直接跳出去...结束了...
         * 2. latch创建出来之后，没有任何线程调用过await()方法之前，线程调用countDown()操作且出发了唤醒阻塞节点的逻辑。
         * 3. head == tail 说明当前线程是最后一个Node节点了
         * 条件不成立：
         *      被唤醒的节点非常积极，直接将自己设置为了新的head，此时 唤醒它的节点(前驱 )执行 h == head会不成立。。
         *      此时前驱节点不糊跳出doReleaseShared()方法，会继续唤醒新的head节点的后继节点。
         *
         * */
        if (h == head)                   // loop if head changed
            break;
    }
}
```

##### 4.2 countDown()执行流程

###### 4.2.1 countDown()

```java
public void countDown() {
    sync.releaseShared(1);
}
```

###### 4.2.2 releaseShared()

```java
/**
 * AQS中的方法
 * */
public final boolean releaseShared(int arg) {
    /**
     * 条件成立：说明当前调用lath.countDown()方法线程正好是 state-1 == 0的线程，
     * 需要触发唤醒await状态的线程
     * */
    if (tryReleaseShared(arg)) {
        /**只会有个线程会进入这个if条件中，此时的state == 0*/
        doReleaseShared();
        return true;
    }
    return false;
}
```

###### 4.2.3 tryReleaseShared()

```java
/**
 * 更新AQS的state值，没调用一次，state值-1
 * 当state-1 == 0时，会返回true。
 * @param releases
 * @return
 */
protected boolean tryReleaseShared(int releases) {
    for (;;) {
        int c = getState();
        /**条件成立：说明前面已经有线程触发唤醒操作了。*/
        if (c == 0)
            return false;

        /**state状态-1*/
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

###### 4.2.4 doReleaseShared()

```java
/**
 * AQS中的方法
 * 都有哪几种情况会调用到doReleaseShared()方法呢？
 * 1.latch.countDown --> AQS.state == 0 --> doReleaseShared()唤醒当前阻塞队列内的 head.next 对应的线程。
 * 2.被唤醒的线程 --> doAcquireSharedInterruptibly()方法中被唤醒 --> setHeadAndPropagate() --> doReleaseShared()
 */
private void doReleaseShared() {
    for (;;) {
        /**获取当前AQS内的头节点*/
        Node h = head;
        /**
         * 条件一：h != null：
         *      true：说明阻塞队列不为空...
         *      false：说明latch创建出来之后，没有任何线程调用过await()方法之前，
         *          有线程调用countDown()操作且出发了唤醒阻塞节点的逻辑。
         * 条件二：h != tail
         *      true：说明当前阻塞队列内出来head节点外还有其他节点。
         *      false：说明 head == tail，什么时候会出现这种情况？
         *          1.正常情况：依次获取到共享锁，当前线程执行到这里时(这个线程就是tail节点。)
         *          2.第一个调用await()方法的 (线程A) 与 调用countDown()方法且触发唤醒阻塞节点的 (线程B) 出现了并发..
         *              线程A 是第一个调用await()的线程，此时阻塞队列中什么都没有，它需要创建一个Head节点，
         *              然后再次自旋将自己入队。
         *              在 线程A 创建Head成功，但是未入队之前，线程B调用countDown 触发了doReleaseShared()唤醒阻塞的节点
         */
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            /**当前head状态为signal 说明后继节点并没有被唤醒过*/
            if (ws == Node.SIGNAL) {
                /**
                 * 唤醒后继节点之前，将head节点的状态改为0
                 * 此时为什么要用CAS操作呢？
                 * 当doReleaseShared方法存在多个线程唤醒 head.next逻辑时，CAS可能会失败...
                 * 案例：t3在if(h == head) 返回false是，t3会继续自旋参与到唤醒下一个head.next的逻辑
                 *      当t3和t4都进入当前if(ws == Node.SIGNAL)，t3执行到当前CAS成功时，t4会失败。
                 * */
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                    !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        /**
         * 条件成立：
         * 1. 说明刚刚唤醒的后继节点还没有执行到setHeadAndPropagate()中的setHead方法，当前线程直接跳出去...结束了...
         * 2. latch创建出来之后，没有任何线程调用过await()方法之前，线程调用countDown()操作且出发了唤醒阻塞节点的逻辑。
         * 3. head == tail 说明当前线程是最后一个Node节点了
         * 条件不成立：
         *      被唤醒的节点非常积极，直接将自己设置为了新的head，此时 唤醒它的节点(前驱 )执行 h == head会不成立。。
         *      此时前驱节点不糊跳出doReleaseShared()方法，会继续唤醒新的head节点的后继节点。
         *
         * */
        if (h == head)                   // loop if head changed
            break;
    }
}
```