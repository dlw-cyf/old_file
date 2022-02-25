package com.dlw;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class MiniReentrantLock implements MiniLock {
	/**
	 * 锁的是什么？
	 * 资源 --> state
	 * 0:表示未加锁状态
	 * >0:加锁状态
	 */
	private volatile int state;

	/**
	 * 独占模式：同一时刻只有一个线程可以持有锁，其他线程再未获取到锁时，会被阻塞...
	 * 当前独占锁的线程(占用锁线程)
	 */
	private Thread exclusiveOwnerThread;

	/**
	 * 需要有两个引用去维护阻塞队列
	 * 1.Head 指向队列的头节点
	 * 		head节点对应的线程，就是当前占用锁的线程。
	 * 2.Tail 指向队列的尾节点
	 */
	private Node head;
	private Node tail;

	/**
	 * 阻塞的线程被封装成什么？
	 * Node节点，然后放入到FIFO队列
	 */
	static final class Node{
		//封装的线程
		Thread thread;

		//前置节点引用
		Node prev;
		//后置节点引用
		Node next;

		public Node() {
		}

		public Node(Thread thread) {
			this.thread = thread;
		}
	}

	/**
	 * 获取锁
	 * 假设当前锁被占用，则会被阻塞调用者线程，直到抢占到锁为止
	 * 模拟公平锁：讲究先来后到
	 *
	 * Lock的过程是怎样的？
	 * 情景1：当前state == 0，这时候很幸运，直接抢锁。
	 * 情景2：当前state >= 0，这时候需要将当前线程入队。
	 */
	@Override
	public void lock() {
		/**第一次获取到锁时，将state == 1
		 * 第n次获取到锁时，将state == n
		 * */
		acquire(1);
	}

	/**
	 * 当前线程入队的一个方法
	 * 返回当前线程对应的Node节点
	 * @return
	 */
	private Node addWaiter(){
		Node newNode = new Node(Thread.currentThread());

		/**
		 * 如何入队呢?
		 * 1.如果队列不为空：找到newNode的前置节点
		 * 2.更新newNode.prev == 前置节点
		 * 3.CAS更新tail为newNode
		 * 4.更新pred.next = newNode
		 */
		Node pred = tail;
		if(pred != null){
			newNode.prev = pred;
			/**
			 * 条件成立：说明当前线程成功入队。
			 */
			if(compareAndSetTail(pred,newNode)){
				pred.next = newNode;
			}
		}

		/**
		 * 执行到这里有几种情况？
		 * 1.tail == null队列是空队列
		 * 2.cas设置当前newNode 为tail时失败了..被其他线程抢先一步了....
		 */
		enq(newNode);
		return newNode;

	}

	/**
	 * 通过自旋方式入队，只有成功后返回。
	 * 1.tail == null队列是空队列
	 * 2.cas设置当前newNode 为tail时失败了..被其他线程抢先一步了....
	 */
	private void enq(Node node){
		for(;;){
			/**
			 * 第一种情况：tail == null队列是空队列
			 * ==>
			 * 	当前线程是第一个抢占锁失败的线程...当前持有锁的线程并没有设置为Node加入队列
			 * 	所以需要给持有锁的线程补充一个Node 作为head节点(head节点表示占有锁的线程)
			 */
			if(tail == null){
				/**
				 * 条件成立：说明当前线程给当前持有锁的线程补充head操作成功了。
				 */
				if(compareAndSetHead(new Node())){
					tail = head;
				}
			}else {
				/**
				 * 说明当前队列中已经有node了，这里是一个追加node的过程
				 * 如何入队呢?
				 * 1.如果队列不为空：找到newNode的前置节点
				 * 2.更新newNode.prev == 前置节点
				 * 3.CAS更新tail为newNode
				 * 4.更新pred.next = newNode
				 */
				Node pred = tail;
				if(pred != null){
					node.prev = pred;
					/**
					 * 条件成立：说明当前线程成功入队。
					 */
					if(compareAndSetTail(pred,node)){
						pred.next = node;
						//入队成功一定要return
						return;
					}
				}
			}
		}
	}


	/**
	 * 只有当前线程成拿到锁之后才会返回
	 * @param node
	 */
	private void acquireQueued(Node node,int arg){
		for(;;){
			/**
			 * 什么情况下，当前node被唤醒后可以尝试去获取锁呢？
			 * 1. 当前node试试head的next节点，也就是先来后到
			 */
			Node pred = node.prev;
			if(head == pred && tryAcquire(arg)){
				/**
				 * 这里面说明当前线程竞争锁成功了
				 * 这里需要做什么？
				 * 1.设置head为当前线程的Node
				 * 2.协助原始线程出队
				 */
				setHead(node);
				pred.next = null;//help GC
				return;
			}

			System.out.println("线程：" + Thread.currentThread().getName() + " 。 挂起!");
			//将当前线程挂起
			LockSupport.park();
			System.out.println("线程：" + Thread.currentThread().getName() + " 。 唤醒成功!");

		}
	}
	/**
	 * 竞争资源
	 * 1.尝试获取锁，成功则占用锁，然后返回
	 * 2.抢占锁失败，阻塞当前线程
	 * @param arg
	 */
	private void acquire(int arg){
		if(!tryAcquire(arg)){
			/**
			 * 抢占锁失败，需要做些什么呢？
			 * 1.需要将当前线程封装成 node，加入到阻塞队列
			 * 2.需要将当前线程park掉，使线程处于挂起状态。
			 *
			 * 唤醒后需要做什么呢？
			 * 1.检查当前node节点 是否为 head.next节点。
			 * 		(head.next节点拥有抢占权限的线程，其他的线程都没有)
			 * 2.抢占
			 * 		成功：将当前node设置为head，将老的head做出队操作，返回到业务层
			 * 		失败：继续park，等待被唤醒
			 * ===>
			 * 1.添加到阻塞队列的逻辑 addWaiter()
			 * 2.竞争资源的逻辑		acquireQueued()
			 * */
			//1.需要将当前线程封装成 node，加入到阻塞队列
			Node node = addWaiter();
			//2.需要将当前线程park掉，使线程处于挂起状态。
			acquireQueued(node,arg);
		}
	}

	/**
	 * 尝试获取锁，不会阻塞线程
	 * true -> 抢占成功
	 * false -> 抢占失败
	 * @param arg
	 * @return
	 */
	private boolean tryAcquire(int arg){
		if(state == 0){
			/**
			 * 当前state，是否可以直接抢锁呢。
			 * 不可以，因为模拟的是公平锁，先来后到...
			 *
			 * 条件一：!hasQueuedPredecessor()
			 * 		true：当前线程前面没有等待者线程
			 * 条件二：compareAndSetState(0,arg)
			 * 		true：通过CAS方式设置状态成功(抢锁成功)。
			 */
			if(!hasQueuedPredecessor() && compareAndSetState(0,arg)){
				/**
				 * 抢锁成功，需要做些什么？
				 * 1.将exclusiveOwnerThread 设置为当前进入if块中的线程
				 */
				this.exclusiveOwnerThread = Thread.currentThread();
				return true;
			}
		}else if(Thread.currentThread() == this.exclusiveOwnerThread){
			/**
			 * 说明当前线程就是持有锁的线程。(这里就是锁重入的流程)
			 * 1.需要个呢过下state的值
			 *
			 * 为什么不需要通过CAS操作？
			 * 因为当前线程已经获取过锁，不存在并发的问题
			 */
			int c = getState();
			c = c + arg;
			this.state = c;
			return true;
		}
		/**
		 * 什么时候会返回false
		 * 1.state == 0时，CAS获取锁失败。
		 * 2.state > 0时，当前线程不是占有者线程
		 */
		return false;
	}

	/**
	 * true：表示当前线程前面有等待着线程
	 * false：当前线程前面没有其他等待者线程。
	 *
	 * 调用链
	 * lock --> acquire --> tryAcquire ->hasQueuedPredecessor (ps：当state == 0 时，即当前lock未被线程持有状态...)
	 *
	 * 什么时候返回false
	 * 1.当前队列为空
	 * 2.当线程为head.next节点
	 * @return
	 */
	private boolean hasQueuedPredecessor(){
		Node h = head;
		Node t = tail;
		Node s;

		/**
		 * 条件一：h != t
		 * 		成立：说明当前队列已经有node了....
		 * 		不成立：
		 * 			1. h == t == null
		 * 			2. h == t == head 第一个获取锁失败的线程，会为当前持有锁的线程 补充创建一个head节点。
		 * 条件二：(s = h.next) == null || s.thread != Thread.currentThread()
		 * 排除几种情况：
		 * 		条件2.1：(s = h.next) == null
		 * 			极端情况：第一个获取锁失败的线程，会为持有锁的线程补充创建一个head节点，
		 * 				然后在自己入队，1.cas tail成功了，2.pred[head].next = node 还没执行的时候。
		 * 			其实想表达的就是：已经有head.next节点了，其他线程再来到这，需要返回true
		 * 		条件2.2：s.thread != Thread.currentThread()
		 * 			条件成立：说明当前线程不是head.next节点对应的线程
		 */

		return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
	}

	/**
	 * 释放锁
	 */
	@Override
	public void unlock() {
		release(1);
	}

	private void release(int arg){
		/**
		 * 条件成立：说明线程已经完全释放锁了
		 * 需要干点什么？
		 * 1.唤醒阻塞队列的线程，进行抢占锁。
		 * */
		if(tryRelease(arg)){
			Node th= this.head;
			/**需要判断阻塞队列中是否有等待着线程*/
			if(th.next != null){
				/**公平锁，就是唤醒head.next节点*/
				unparkSuccessor(head);
			}
		}
	}
	private void unparkSuccessor(Node node){
		Node s = node.next;
		Thread t;
		if(s != null && (t = s.thread) != null){
			LockSupport.unpark(t);
		}
	}

	/**
	 * 完全释放锁成功。则返回true
	 * 否则说明当前state > 0 返回false
	 * @param arg
	 * @return
	 */
	private boolean tryRelease(int arg){
		int c = getState() - arg;

		if(getExclusiveOwnerThread() != Thread.currentThread()){
			throw new RuntimeException("fuck you ! must getLock!");
		}

		/**如果执行到这里，不能存在并发 只有持有锁的线程会来到这里*/

		if(c == 0){
			/**
			 * 说明当前线程持有的锁已经完全释放了
			 * 1.ExclusiveOwnerThread == null
			 * 2.设置state == 0
			 * */
			this.exclusiveOwnerThread = null;
			this.state = c;
			return true;
		}
		this.state = c;
		return false;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public Thread getExclusiveOwnerThread() {
		return exclusiveOwnerThread;
	}

	public void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
		this.exclusiveOwnerThread = exclusiveOwnerThread;
	}

	public Node getHead() {
		return head;
	}

	public Node getTail() {
		return tail;
	}

	public void setTail(Node tail) {
		this.tail = tail;
	}

	private void setHead(Node node){
		this.head = node;
		/**
		 * 为什么要设置为null？
		 * 因为当前node已经是获取锁成功的线程了...
		 */
		node.thread = null;
		node.prev = null;
	}

	private static final Unsafe unsafe;
	private static final long stateOffset;
	private static final long headOffset;
	private static final long tailOffset;

	static {
		try{
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);

			stateOffset =  unsafe.objectFieldOffset
					(MiniReentrantLock.class.getDeclaredField("state"));
			headOffset =  unsafe.objectFieldOffset
					(MiniReentrantLock.class.getDeclaredField("head"));
			tailOffset =  unsafe.objectFieldOffset
					(MiniReentrantLock.class.getDeclaredField("tail"));
		}catch (Exception e){
			throw new Error(e);
		}
	}

	private final boolean compareAndSetHead(Node update){
		return unsafe.compareAndSwapObject(this,headOffset,null,update);
	}

	private final boolean compareAndSetTail(Node expect,Node update){
		return unsafe.compareAndSwapObject(this,tailOffset,expect,update);
	}

	private final boolean compareAndSetState(int expect,int update){
		return unsafe.compareAndSwapInt(this,stateOffset,expect,update);
	}
}
