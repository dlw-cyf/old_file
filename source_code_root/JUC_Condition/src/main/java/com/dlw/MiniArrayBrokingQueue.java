package com.dlw;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MiniArrayBrokingQueue implements BrokingQueue {

	/**线程并发控制*/
	private Lock lock = new ReentrantLock();

	/**
	 * 当生成者线程生产数据时，它会先检查当前queues是否已经满了。
	 * 如果已经满了，需要将当前生产者下沉调用notFull.await(),
	 * 进入到notFull条件队列挂起，等待消费者线程消费数据时唤醒。
	 * */
	private Condition notFull = lock.newCondition();

	/**
	 * 当消费者消费数据时，它会先检查当前queues中是否有数据，
	 * 如果没有数据，需要将当前消费者线程调用notEmpty.await()
	 * 进入notEmpty条件队列挂起，等待生产者生产数据时唤醒。
	 */
	private Condition notEmpty = lock.newCondition();

	/**底层存储元素的数组队列*/
	private Object[] queues;

	/**数组长度*/
	private int size;

	/**
	 * count:当前队列中可以被消费的数据量
	 * putptr：记录生产者存放数据的下一次位置。每个生成者生产完一个数据后，会将 putptr++
	 * takeptr：记录消费者消费数据的下一次位置。每个消费者消费完一个数据后，会将 takeptr++
	 */
	private int count,putptr,takeptr;

	public MiniArrayBrokingQueue(int size){
		this.size = size;
		this.queues = new Object[size];
	}

	@Override
	public void put(Object element) {
		lock.lock();
		try {
			/**
			 * 1.判断当前queues是否已经满了
			 * */
			if(count == this.size){
				notFull.await();
			}

			/**
			 * 执行到这里说明当前队列中还可以存放数据
			 */
			this.queues[putptr] = element;

			putptr = ++putptr == this.size ? 0 :putptr;
			count ++;

			/**
			 * 当向队列中放入一个元素之后，需要做什么呢？
			 * 需要给notEmpty一个唤醒信号
			 */
			notEmpty.signal();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Object take() {
		lock.lock();
		try{
			/**1.判断一下当前队列是否有数据可以被消费*/
			if(count == 0){
				notEmpty.await();
			}

			/**
			 * 执行到这里，说明当前队列有数据可以被消费了
			 */
			Object element = this.queues[takeptr];

			takeptr = ++takeptr == size ? 0 : takeptr;
			/**消费了一个数据*/
			count--;

			/**
			 * 向队列中take(消费)一个数据之后，需要做什么呢？
			 * 需要给notFull一个唤醒信号
			 */
			notFull.signal();
			return element;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return null;
	}

	public static void main(String[] args) {
		BrokingQueue<Integer> queue = new MiniArrayBrokingQueue(10);

		Thread producer = new Thread(() -> {
			int i = 0;
			while (true){
				i++;
				i = i == 10 ? 0 : i;

				System.out.println("生产数据：" + i);
				queue.put(Integer.valueOf(i));
				try {
					TimeUnit.MILLISECONDS.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		producer.start();

		Thread consumer = new Thread(()->{
			while (true){
				Integer result = queue.take();
				System.out.println("消费者消费数据：" + result);
//				try {
//					TimeUnit.MILLISECONDS.sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
			}
		});
		consumer.start();
		Condition


	}
}
