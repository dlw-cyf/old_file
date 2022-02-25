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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A synchronization aid that allows a set of threads to all wait for
 * each other to reach a common barrier point.  CyclicBarriers are
 * useful in programs involving a fixed sized party of threads that
 * must occasionally wait for each other. The barrier is called
 * <em>cyclic</em> because it can be re-used after the waiting threads
 * are released.
 *
 * <p>A {@code CyclicBarrier} supports an optional {@link Runnable} command
 * that is run once per barrier point, after the last thread in the party
 * arrives, but before any threads are released.
 * This <em>barrier action</em> is useful
 * for updating shared-state before any of the parties continue.
 *
 * <p><b>Sample usage:</b> Here is an example of using a barrier in a
 * parallel decomposition design:
 *
 *  <pre> {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}</pre>
 *
 * Here, each worker thread processes a row of the matrix then waits at the
 * barrier until all rows have been processed. When all rows are processed
 * the supplied {@link Runnable} barrier action is executed and merges the
 * rows. If the merger
 * determines that a solution has been found then {@code done()} will return
 * {@code true} and each worker will terminate.
 *
 * <p>If the barrier action does not rely on the parties being suspended when
 * it is executed, then any of the threads in the party could execute that
 * action when it is released. To facilitate this, each invocation of
 * {@link #await} returns the arrival index of that thread at the barrier.
 * You can then choose which thread should execute the barrier action, for
 * example:
 *  <pre> {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}</pre>
 *
 * <p>The {@code CyclicBarrier} uses an all-or-none breakage model
 * for failed synchronization attempts: If a thread leaves a barrier
 * point prematurely because of interruption, failure, or timeout, all
 * other threads waiting at that barrier point will also leave
 * abnormally via {@link BrokenBarrierException} (or
 * {@link InterruptedException} if they too were interrupted at about
 * the same time).
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * {@code await()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions that are part of the barrier action, which in turn
 * <i>happen-before</i> actions following a successful return from the
 * corresponding {@code await()} in other threads.
 *
 * @since 1.5
 * @see CountDownLatch
 *
 * @author Doug Lea
 */
public class CyclicBarrier {
    /**
     * 表示"代"这个概念
     */
    private static class Generation {
        /**
         * 表示当前 "代" 是否被打破，如果代被打破，那么再来到这一代的线程会执行抛出 BrokenException异常
         * 而且这一代挂起的线程都会被唤醒，然后抛出BrokenException
         * */
        boolean broken = false;
    }

    /** 因为barrier的实现是依赖于Condition条件队列的，condition条件队列必须依赖lock才能使用。 */
    private final ReentrantLock lock = new ReentrantLock();

    /** 线程挂起实现使用的condition队列。条件：当前代所有线程到位，这个条件队列内的线程才会被唤醒 */
    private final Condition trip = lock.newCondition();


    /** barrier需要参与进来的线程数量 */
    private final int parties;

    /**当前"代"最后一个到位的线程需要执行的操作*/
    private final Runnable barrierCommand;

    /** Barrier对象当前"代" */
    private Generation generation = new Generation();

    /**
     * 表示当前"代"还有多少个线程未到位
     */
    private int count;

    /**
     * 当这一代所有线程到位以后(barrierCommand不为空，最后一个线程执行完事件)，
     * 会调用nextGeneration()开启新的一代。
     */
    private void nextGeneration() {
        /**将trip条件队列内挂起的线程全部唤醒*/
        trip.signalAll();
        /**重置count为parties*/
        count = parties;
        /**
         * 开启新的一代：使用generation对象表示新的一代
         */
        generation = new Generation();
    }

    /**
     * 打破Barrier屏障，在屏障的线程都会抛出异常。
     */
    private void breakBarrier() {
        /**将"代"中的broken设置为true，表示这一代被打破了，再来到这一代的线程，直接抛出异常*/
        generation.broken = true;
        count = parties;
        /** 唤醒后的线程会检查当前代是否是被打破的，如果是打破的话，接下来的逻辑和开启下一代的唤醒逻辑不一样 */
        trip.signalAll();
    }

    /**
     *
     * @param timed 表示当前调用await方法的线程是否指定了超时时长，
     * @param nanos 线程等待超时时长 纳秒，
     * @return
     * @throws InterruptedException
     * @throws BrokenBarrierException
     * @throws TimeoutException
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        /**获取Barrier对象的全局锁*/
        final ReentrantLock lock = this.lock;
        /**
         * 为什么要加锁呢？
         * 因为barrier的挂起和唤醒依赖的组件是condition
         */
        lock.lock();
        try {
            /**获取barrier当前 "代"*/
            final Generation g = generation;

            /**如果当前代是被打破状态，则当前调用await方法的线程，直接抛出BrokenException*/
            if (g.broken)
                throw new BrokenBarrierException();

            /**如果当前线程的中断标志位为true：打破当前代，当前线程抛出中断异常。*/
            if (Thread.interrupted()) {
                /**1.设置当前代的状态为broken。2.唤醒在trip条件队列内的线程。*/
                breakBarrier();
                throw new InterruptedException();
            }

            /**假设parties = 5 那么index对应的值为[0,4]*/
            int index = --count;
            /**条件成立：说明当前线程是最后一个到达barrier的线程*/
            if (index == 0) {  // tripped
                /**
                 * true：最后一个线程执行，barrierCommand未抛出异常。
                 * false：表示抛出异常了。
                 * */
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;

                    /**
                     * 说明在创建Barrier对象时指定了Runnable接口了
                     * 这时最后一个到达的线程需要执行这个接口
                     * */
                    if (command != null)
                        command.run();
                    ranAction = true;
                    /**开启新的一代
                     * 1.唤醒trip条件队列内的挂起的线程，被唤醒的线程会依次获取lock，然后依次退出await方法。
                     * 2.重置count。
                     * 3.创建一个新的Generation对象，表示新的一代。
                     * */
                    nextGeneration();
                    return 0;
                } finally {
                    /**条件成立：command.run()执行发生异常的话，ranAction=false，*/
                    if (!ranAction)
                        /**打破当前代*/
                        breakBarrier();
                }
            }

            /**执行到这里说明当前线程并不是最后一个到达Barrier的线程，
             * 需要进行自旋 一直到条件满足，当前代被打破、当前线程被中断、等待超时
             * */
            for (;;) {
                try {
                    /**说明当前线程是不指定超时时间的*/
                    if (!timed)
                        /**当前线程会释放掉lock，然后进入到trip条件队列的尾部，然后挂起自己，等待被唤醒*/
                        trip.await();
                    else if (nanos > 0L)
                        /**说明当前线程调用await方法时，是指定了超时时间的*/
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    /**
                     * 什么时候会抛出InterruptedException异常呢？
                     *                      * 当前Node节点在条件队列内时收到中断信号时，会抛出中断异常。
                     *
                     * 条件一：g == generation
                     *      条件成立：说明当前代并没有变化。
                     * 条件二：! g.broken
                     *      条件成立：说明当前broken没有被打破。
                     */
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        /**
                         * 执行到else的几种情况
                         * 1.代发生了变化，这时候就不需要抛出中断异常，因为 代 已经更新，这里唤醒后就走正常逻辑了。
                         * 2.代没有发生变化，但是代被打破了，此时也不用返回中断异常，执行到下面的时候会 抛出BrokenBarrier异常
                         */
                        Thread.currentThread().interrupt();
                    }
                }

                /**
                 * 唤醒后执行到这里，有几种情况？
                 * 1.正常情况，当前barrier开启了新的一代：trip.signalAll()。
                 * 2.当前Generation被打破，此时也会唤醒所有在trip上挂起的线程。
                 * 3.当前线程trip中等待超时，然后主动转移到阻塞队列获取到锁，唤醒。
                 */

                /**这是第二种情况：当前代被打破*/
                if (g.broken)
                    throw new BrokenBarrierException();

                /**这里是第一种情况：正常情况，Barrier开启了新的一代*/
                if (g != generation)
                    return index;

                /**
                 * 这里是第三种情况：当前线程trip中等待超时了。
                 */
                if (timed && nanos <= 0L) {
                    /**打破barrier，抛出超时异常*/
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and which
     * will execute the given barrier action when the barrier is tripped,
     * performed by the last thread entering the barrier.
     *
     * @param parties Barrier(每次屏障)需要参与的线程数量，
     * @param barrierAction 当前"代"最后一个到位的线程需要执行的事件(可以为null)
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        /**小于等于0的 Barrier没有任何意义...*/
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        /**count的初始化就是parties，当前代每到位一个线程，count--*/
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and
     * does not perform a predefined action when the barrier is tripped.
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * Returns the number of parties required to trip this barrier.
     *
     * @return the number of parties required to trip this barrier
     */
    public int getParties() {
        return parties;
    }

    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while waiting,
     * then all other waiting threads will throw
     * {@link BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     */
    public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
               BrokenBarrierException,
               TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * Queries if this barrier is in a broken state.
     *
     * @return {@code true} if one or more parties broke out of this
     *         barrier due to interruption or timeout since
     *         construction or the last reset, or a barrier action
     *         failed due to an exception; {@code false} otherwise.
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the barrier to its initial state.  If any parties are
     * currently waiting at the barrier, they will return with a
     * {@link BrokenBarrierException}. Note that resets <em>after</em>
     * a breakage has occurred for other reasons can be complicated to
     * carry out; threads need to re-synchronize in some other way,
     * and choose one to perform the reset.  It may be preferable to
     * instead create a new barrier for subsequent use.
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of parties currently waiting at the barrier.
     * This method is primarily useful for debugging and assertions.
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
