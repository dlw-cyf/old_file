#### 类加载器的加载顺序

![a](.\assets\类加载器的加载顺序.png)

#### 指令重排

CPU在进行读等待的同时执行指令，是CPU乱序的根源，这其实不是乱，而是提高效率。例如指令1去内存读数据，因为CPU与内存访问速度相差100倍，如果指令2的执行过程不需要依赖指令1，那么指令2可以先执行，乱序执行的本质是同时执行。

```java
public class CommandDisorder {

	private static int x = 0, y = 0;
	private static int a = 0, b = 0;

	public static void main(String[] args) throws InterruptedException {
		int i = 0;
		while (true){
			i++;
			x = 0; y = 0;
			a = 0; b = 0;
			Thread one = new Thread(new Runnable() {
				@Override
				public void run() {
					//由于线程one先启动，下面这句话是让它等一等线程two，
					shortWait(10000);
					a = 1;
					x = b;
				}
			});

			Thread two = new Thread(new Runnable() {
				@Override
				public void run() {
					b = 1;
					y = a;
				}
			});

			/**
			 * 分析：如果不发生指令重排，不管哪个线程先执行，a和b总有一个值为1。那么就不可能出现x=0并且y=0的情况。
			 * 如果出现了这种情况，说明出现了指令重排
			 */

			one.start();
			two.start();
			one.join();
			two.join();
			String result = "第" + i + "次 (" + x + "," + y + "）";
			if (x == 0 && y == 0) {
				System.err.println(result);
				break;
			} else {
				System.out.println(result);
			}
		}
	}

	public static void shortWait(long interval) {
		long start = System.nanoTime();
		long end;
		do {
			end = System.nanoTime();
		} while (start + interval >= end);
	}
}

```

#### 双重检查的单列模式

Java对象的创建过程不是一个原子操作，极有可能出现指令重排序，下面通过Java对象创建的汇编码讲解。

在new对象的时候会执行三条指令：`new(申请内存) ---> astore_1(变量关联)  ---> invokespecial(初始化对象)`

> 顺序的情况下，new指令申请了一块内存空间，`invokespecial`调用构造方法为对象进行初始化，`astore_1`将变量和新创建的对象关联起来。但是`invokespecial`和`astore_1`这两条指令没有关联性，所以`astore_1`有可能会跑到`invokespecial`前面执行。

```java
public class DoubleCheckSingle {

	//使用volatile：避免指令重排的情况。
	private static volatile DoubleCheckSingle doubleCheckSingle;

	//构造方法私有化，不允许new对象
	private DoubleCheckSingle(){
	}

	public static DoubleCheckSingle getDoubleCheckSingle() {
		if(doubleCheckSingle == null){
			//使用同步锁
			synchronized (DoubleCheckSingle.class){
				//双重检查：避免在加锁的过程中其他线程对该对象进行了初始化。
				if(doubleCheckSingle == null){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					doubleCheckSingle = new DoubleCheckSingle();
				}

			}
		}

		return doubleCheckSingle;
	}

}
```

#### 内存模型相关概念

​		大家都知道，计算机在执行程序时，每条指令都是在CPU中执行的，而执行指令过程中，势必涉及到数据的读取和写入。由于程序运行过程中的临时数据是存放在主存（物理内存）当中的，这时就存在一个问题，由于CPU执行速度很快，而从内存读取数据和向内存写入数据的过程跟CPU执行指令的速度比起来要慢的多，因此如果任何时候对数据的操作都要通过和内存的交互来进行，会大大降低指令执行的速度。因此在CPU里面就有了高速缓存。

​		也就是，当程序在运行过程中，会将运算需要的数据从主存复制一份到CPU的高速缓存当中，那么CPU进行计算时就可以直接从它的高速缓存读取数据和向其中写入数据，当运算结束之后，再将高速缓存中的数据刷新到主存当中。举个简单的例子，比如下面的这段代码：

```java
i = i + 1;
```

​		当线程执行这个语句时，会先从主存当中读取i的值，然后复制一份到高速缓存当中，然后CPU执行指令对i进行加1操作，然后将数据写入高速缓存，最后将高速缓存中i最新的值刷新到主存当中。

​		这个代码在单线程中运行是没有任何问题的，但是在多线程中运行就会有问题了。在多核CPU中，每条线程可能运行于不同的CPU中，因此每个线程运行时有自己的高速缓存（对单核CPU来说，其实也会出现这种问题，只不过是以线程调度的形式来分别执行的）。本文我们以多核CPU为例。

​		比如同时有2个线程执行这段代码，假如初始时i的值为0，那么我们希望两个线程执行完之后i的值变为2。但是事实会是这样吗？

​		可能存在下面一种情况：初始时，两个线程分别读取i的值存入各自所在的CPU的高速缓存当中，然后线程1进行加1操作，然后把i的最新值1写入到内存。此时线程2的高速缓存当中i的值还是0，进行加1操作之后，i的值为1，然后线程2把i的值写入内存。

​		最终结果i的值是1，而不是2。这就是著名的缓存一致性问题。通常称这种被多个线程访问的变量为共享变量。

​		也就是说，如果一个变量在多个CPU中都存在缓存（一般在多线程编程时才会出现），那么就可能存在缓存不一致的问题。

为了解决缓存不一致性问题，通常来说有以下2种解决方法：

* 通过在总线加LOCK#锁的方式
* 通过缓存一致性协议

这2种方式都是硬件层面上提供的方式。

​		在早期的CPU当中，是通过在总线上加LOCK#锁的形式来解决缓存不一致的问题。因为CPU和其他部件进行通信都是通过总线来进行的，如果对总线加LOCK#锁的话，也就是说阻塞了其他CPU对其他部件访问（如内存），从而使得只能有一个CPU能使用这个变量的内存。比如上面例子中 如果一个线程在执行 i = i +1，如果在执行这段代码的过程中，在总线上发出了LCOK#锁的信号，那么只有等待这段代码完全执行完毕之后，其他CPU才能从变量i所在的内存读取变量，然后进行相应的操作。这样就解决了缓存不一致的问题。

​		但是上面的方式会有一个问题，由于在锁住总线期间，其他CPU无法访问内存，导致效率低下。

​		所以就出现了缓存一致性协议。最出名的就是Intel 的MESI协议，MESI协议保证了每个缓存中使用的共享变量的副本是一致的。它核心的思想是：当CPU写数据时，如果发现操作的变量是共享变量，即在其他CPU中也存在该变量的副本，会发出信号通知其他CPU将该变量的缓存行置为无效状态，因此当其他CPU需要读取这个变量时，发现自己缓存中缓存该变量的缓存行是无效的，那么它就会从内存重新读取。

#### 硬件级别保证有序

###### 1. 内存屏障

CPU层面使用内存屏障禁止指令重排序，通过在指令1和指令2之间插入内存屏障来禁止指令重排序，Inter通过原语`lfence(load), sfence(save), mfence(mixed)`实现内存屏障，当然也可以使用总线锁来解决。

* sfence：在sfence指令前的写操作必须在sfence指令后的写操作前完成；
* lfence：在lfence指令前的读操作必须在lfence指令后的读操作前完成；
* mfence：在mfence指令前的读写操作必须在mfence指令后的读写操作前完成；

* lock：原子指令，如x86上的`lock...`指令是一个Full Barrier，执行时会锁住内存子系统来确保执行顺序，甚至跨越多个CPU，这是硬件层次.

###### 2.  java中实现有序

* volatile：locl addl 0x0(exp)，向exp寄存器中加0，主要是执行lock指令;
* sychronized：lock comxchg，通过自旋获得锁才能执行后面的操作;

#### MESI Cache一致性协议

###### 什么是cache

CPU在摩尔定律的指导下以每18个月翻一番的速度在发展，然而内存和硬盘的发展速度远远不及CPU。这就造成了高性能能的内存和硬盘价格及其昂贵。然而CPU的高度运算需要高速的数据。为了解决这个问题，CPU厂商在CPU中内置了少量的高速缓存以解决I\O速度和CPU运算速度之间的不匹配问题。

通过这个协议，让各个CPU的缓存保存一致性。

* Modified：当CPU对这个缓存数据进行了修改。
* Exclusive：当前CPU独享这个缓存数据。
* Shared：和其他CPU共享这个缓存数据。
* Invalid：其他的CPU对这个数据进行了更改。
  * 例如：如果我需要对这个数据进行修改时，发现此时状态已经变成了Invalid，则需要重新从内存获取这个数据。

#### cache line 缓存行

###### 1. 什么是缓存行

###### 2. 案例

当只有一个属性x时，两个线程操作的数据（X，Y）会放在同一个cache line里面，而关键字volatile又是线程可见，当其中一个线程改变了值，另一个线程会同步这个改变的值（保证缓存一致性），频繁的进行拉取数据，导致效率不高。

当有多个属性达到64字节的时候，两个线程操作的数据不会放在同一个cache line，不需要互相的同步。效率就很高了。

```java
public class CacheLineD {
    private static class T{
        // long的数据类型占有8个字节
        //private long x1,x2,x3,x4,x5,x6,x7;
        public volatile long x = 0;
    }
    public static T[] arr = new T[2];
 
    static {
        arr[0] = new T();
        arr[1] = new T();
    }
 
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for(long i = 0; i< 1000_0000L; i++) {
                arr[0].x = i;
            }
        });
 
        Thread t2 = new Thread(() -> {
            for(long i = 0; i< 1000_0000L; i++) {
                arr[1].x = i;
            }
        });
 
        long start = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(System.currentTimeMillis() -start);
 
    }

}
```

#### Write Combining 合并写技术

  Write Combining Buffer一般是4个字节，由于CPU速度太快，为了提高写效率，CPU在写入L1时，写入一个WC Buffer，当WC Buffer满了之后，直接用WC写入L2。

```java
public class WriteCombining {

	private static final int ITERATIONS = Integer.MAX_VALUE;
	private static final int ITEMS = 1 << 24;
	private static final int MASK = ITEMS -1;

	private static final byte[] arrayA = new byte[ITEMS];
	private static final byte[] arrayB = new byte[ITEMS];
	private static final byte[] arrayC = new byte[ITEMS];
	private static final byte[] arrayD = new byte[ITEMS];
	private static final byte[] arrayE = new byte[ITEMS];
	private static final byte[] arrayF = new byte[ITEMS];

	public static void main(final String[] args){
		System.out.println(" SingleLoop duration (ns) = " + runCaseOne());
		System.out.println(" SplitLoop duration (ns) = " + runCaseTwo());
	}

	public static long runCaseOne(){
		long start = System.nanoTime();
		int i = ITERATIONS;

		while (--i != 0){
			int slot = i & MASK;
			byte b = (byte) i;

			arrayA[slot] = b;
			arrayB[slot] = b;
			arrayC[slot] = b;
			arrayD[slot] = b;
			arrayE[slot] = b;
			arrayF[slot] = b;
		}

		return (System.nanoTime() - start)/100_000;
	}

	public static long runCaseTwo(){
		long start = System.nanoTime();
		int i = ITERATIONS;

		while (--i != 0){
			int slot = i & MASK;
			byte b = (byte) i;
			arrayA[slot] = b;
			arrayB[slot] = b;
			arrayC[slot] = b;
		}
		i = ITERATIONS;
		while (--i != 0){
			int slot = i & MASK;
			byte b = (byte) i;
			arrayD[slot] = b;
			arrayE[slot] = b;
			arrayF[slot] = b;
		}

		return (System.nanoTime() - start)/100_000;
	}
}
```

