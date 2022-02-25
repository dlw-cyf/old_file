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

package java.util.concurrent;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A synchronization aid that allows one or more threads to wait until
 * a set of operations being performed in other threads completes.
 *
 * <p>A {@code CountDownLatch} is initialized with a given <em>count</em>.
 * The {@link #await await} methods block until the current count reaches
 * zero due to invocations of the {@link #countDown} method, after which
 * all waiting threads are released and any subsequent invocations of
 * {@link #await await} return immediately.  This is a one-shot phenomenon
 * -- the count cannot be reset.  If you need a version that resets the
 * count, consider using a {@link CyclicBarrier}.
 *
 * <p>A {@code CountDownLatch} is a versatile synchronization tool
 * and can be used for a number of purposes.  A
 * {@code CountDownLatch} initialized with a count of one serves as a
 * simple on/off latch, or gate: all threads invoking {@link #await await}
 * wait at the gate until it is opened by a thread invoking {@link
 * #countDown}.  A {@code CountDownLatch} initialized to <em>N</em>
 * can be used to make one thread wait until <em>N</em> threads have
 * completed some action, or some action has been completed N times.
 *
 * <p>A useful property of a {@code CountDownLatch} is that it
 * doesn't require that threads calling {@code countDown} wait for
 * the count to reach zero before proceeding, it simply prevents any
 * thread from proceeding past an {@link #await await} until all
 * threads could pass.
 *
 * <p><b>Sample usage:</b> Here is a pair of classes in which a group
 * of worker threads use two countdown latches:
 * <ul>
 * <li>The first is a start signal that prevents any worker from proceeding
 * until the driver is ready for them to proceed;
 * <li>The second is a completion signal that allows the driver to wait
 * until all workers have completed.
 * </ul>
 *
 *  <pre> {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>Another typical usage would be to divide a problem into N parts,
 * describe each part with a Runnable that executes that portion and
 * counts down on the latch, and queue all the Runnables to an
 * Executor.  When all sub-parts are complete, the coordinating thread
 * will be able to pass through await. (When threads must repeatedly
 * count down in this way, instead use a {@link CyclicBarrier}.)
 *
 *  <pre> {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>Memory consistency effects: Until the count reaches
 * zero, actions in a thread prior to calling
 * {@code countDown()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful return from a corresponding
 * {@code await()} in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {
    /**
     * Synchronization control For CountDownLatch.
     * Uses AQS state to represent count.
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }



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

        /**
         * 返回-1：state状态是大于0的
         * @param acquires
         * @return
         */
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

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

    }

    private final Sync sync;

    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the current count is zero then this method returns immediately.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * <p>If the current count is zero then this method returns immediately
     * with the value {@code true}.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the count reaches zero then the method returns with the
     * value {@code true}.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Decrements the count of the latch, releasing all waiting threads if
     * the count reaches zero.
     *
     * <p>If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     *
     * <p>If the current count equals zero then nothing happens.
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * Returns the current count.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * Returns a string identifying this latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
