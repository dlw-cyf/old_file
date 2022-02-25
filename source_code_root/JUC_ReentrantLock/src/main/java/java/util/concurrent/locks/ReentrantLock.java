/*
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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 *
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 *
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * Sync object for non-fair locks
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * Sync object for fair locks
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

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

        /**公平锁的入口*/
        final void lock() {
            acquire(1);
        }

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

        /**
         * AQS的方法
         * park当前线程，将当前线程挂起，唤醒后返回当前线程 是否为中断信号唤醒
         * @return
         */
        private final boolean parkAndCheckInterrupt() {
            LockSupport.park(this);
            return Thread.interrupted();
        }

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

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * Acquires the lock.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until the lock has been acquired,
     * at which time the lock hold count is set to one.
     */
    public void lock() {
        sync.lock();
    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds this lock then the hold count
     * is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of two things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread.
     *
     * </ul>
     *
     * <p>If the lock is acquired by the current thread then the lock hold
     * count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock,
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * Acquires the lock only if it is not held by another thread at the time
     * of invocation.
     *
     * <p>Acquires the lock if it is not held by another thread and
     * returns immediately with the value {@code true}, setting the
     * lock hold count to one. Even when this lock has been set to use a
     * fair ordering policy, a call to {@code tryLock()} <em>will</em>
     * immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting for this lock, then use
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * <p>If the current thread already holds this lock then the hold
     * count is incremented by one and the method returns {@code true}.
     *
     * <p>If the lock is held by another thread then this method will return
     * immediately with the value {@code false}.
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * Acquires the lock if it is not held by another thread within the given
     * waiting time and the current thread has not been
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately with the value {@code true}, setting the lock hold count
     * to one. If this lock has been set to use a fair ordering policy then
     * an available lock <em>will not</em> be acquired if any other threads
     * are waiting for the lock. This is in contrast to the {@link #tryLock()}
     * method. If you want a timed {@code tryLock} that does permit barging on
     * a fair lock then combine the timed and un-timed forms together:
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * <p>If the current thread
     * already holds this lock then the hold count is incremented by one and
     * the method returns {@code true}.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses
     *
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned and
     * the lock hold count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while
     * acquiring the lock,
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock, and
     * over reporting the elapse of the waiting time.
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * Attempts to release this lock.
     *
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * Returns a {@link Condition} instance for use with this
     * {@link Lock} instance.
     *
     * <p>The returned {@link Condition} instance supports the same
     * usages as do the {@link Object} monitor methods ({@link
     * Object#wait() wait}, {@link Object#notify notify}, and {@link
     * Object#notifyAll notifyAll}) when used with the built-in
     * monitor lock.
     *
     * <ul>
     *
     * <li>If this lock is not held when any of the {@link Condition}
     * {@linkplain Condition#await() waiting} or {@linkplain
     * Condition#signal signalling} methods are called, then an {@link
     * IllegalMonitorStateException} is thrown.
     *
     * <li>When the condition {@linkplain Condition#await() waiting}
     * methods are called the lock is released and, before they
     * return, the lock is reacquired and the lock hold count restored
     * to what it was when the method was called.
     *
     * <li>If a thread is {@linkplain Thread#interrupt interrupted}
     * while waiting then the wait will terminate, an {@link
     * InterruptedException} will be thrown, and the thread's
     * interrupted status will be cleared.
     *
     * <li> Waiting threads are signalled in FIFO order.
     *
     * <li>The ordering of lock reacquisition for threads returning
     * from waiting methods is the same as for threads initially
     * acquiring the lock, which is in the default case not specified,
     * but for <em>fair</em> locks favors those threads that have been
     * waiting the longest.
     *
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * Queries the number of holds on this lock by the current thread.
     *
     * <p>A thread has a hold on a lock for each lock action that is not
     * matched by an unlock action.
     *
     * <p>The hold count information is typically only used for testing and
     * debugging purposes. For example, if a certain section of code should
     * not be entered with the lock already held then we can assert that
     * fact:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * Queries if this lock is held by the current thread.
     *
     * <p>Analogous to the {@link Thread#holdsLock(Object)} method for
     * built-in monitor locks, this method is typically used for
     * debugging and testing. For example, a method that should only be
     * called while a lock is held can assert that this is the case:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>It can also be used to ensure that a reentrant lock is used
     * in a non-reentrant manner, for example:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries if this lock is held by any thread. This method is
     * designed for use in monitoring of the system state,
     * not for synchronization control.
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns this lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries whether any threads are waiting to acquire this lock. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire this lock.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Queries whether the given thread is waiting to acquire this
     * lock. Note that because cancellations may occur at any time, a
     * {@code true} return does not guarantee that this thread
     * will ever acquire this lock.  This method is designed primarily for use
     * in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire this lock.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring of the system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire this lock.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
