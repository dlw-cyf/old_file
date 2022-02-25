#### 1. 核心属性

```java
	/**
     * 缺省table大小
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * table最大长度
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 缺省负载因子大小
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 树化阈值
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 树降级成为链表的阈值
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     *树化的另一个参数，当哈希表中的所有元素超过64是才会允许树化。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;
	/**
     * 哈希表：什么时候初始化呢？
     */
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * 当前哈希表中元素个数。
     */
    transient int size;

    /**
     * 当前哈希表结构修改次数：添加或者删除一个元素
     */
    transient int modCount;

    /**
     * 扩容阈值，当你的哈希表中的元素超过这个阈值时，触发扩容
     * @serial
     */
    int threshold;

    /**
     *  负载因子
     *  threshold = capacity + loadFactor
     * @serial
     */
    final float loadFactor;
```

#### 2. 核心方法

##### 2.1 构造方法

```java
/**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public HashMap(int initialCapacity, float loadFactor) {
        //其实就是做了一个参数合理化的校验
        //initialCapacity，并且不能超过最大值MAXIMUM_CAPACITY:(table最大长度)
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        //loadFactor(负载因子):必须大于0
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        //返回一个大于等于当前cap的一个数字，并且这个数字一定是2的次方数。
        this.threshold = tableSizeFor(initialCapacity);
    }
```

##### 2.2 tableSizeFor()方法

* **第一次右移**：由于n不等于0，则n的二进制表示中总会有一bit为1，这时考虑最高位的1。通过无符号右移1位，则将最高位的1右移了1位，再做或操作，使得n的二进制表示中与最高位的1紧邻的右边一位也为1，如`000011xxxxxx`。
* **第二次右移**：注意，这个n已经经过了`n |= n >>> 1;` 操作。假设此时n为`000011xxxxxx` ，则n无符号右移两位，会将最高位两个连续的1右移两位，然后再与原来的n做或操作，这样n的二进制表示的高位中会有4个连续的1。如`00001111xxxxxx` 。 
* **第三次右移**：这次把已经有的高位中的连续的4个1，右移4位，再做或操作，这样n的二进制表示的高位中会有8个连续的1。如`00001111 1111xxxxxx` 。
* **以此类推**：注意，容量最大也就是32bit的正数，因此最后`n |= n >>> 16;` ，最多也就32个1，但是这时已经大于了`MAXIMUM_CAPACITY` ，所以取值到`MAXIMUM_CAPACITY` 。

```java
 /**
     * 返回一个大于等于当前cap的一个数字，并且这个数字一定是2的次方数。
     *
     * cap=10
     */
    static final int tableSizeFor(int cap) {
        //这是为了防止，cap已经是2的幂。如果cap已经是2的幂， 又没有执行这个减1操作，则执行完后面的几条无符号右移操作之后，返回的capacity将是这个cap的2倍
        int n = cap - 1;// n = 10 -1 = 9
        n |= n >>> 1;//0b1001 | 0b0100 ==> 0b1101
        n |= n >>> 2;//0b1101 | 0b0011 ==> 0b1111
        n |= n >>> 4;//0b1111 | 0b0000 ==> 0b1111
        n |= n >>> 8;//0b1111 | 0b0000 ==> 0b1111
        n |= n >>> 16;//0b1111 | 0b0000 ==> 0b1111
        //0b1111 = 15
        //return 15+1
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

##### 2.3 hash()方法

```java
/**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     *
     * 作用：让key的Hash值的高16位也参与路由运算(减少Hash碰撞)。
     * 异或：相同返回0，不同返回1
     *
     * h = 0b 0010 0101 1010 1100 0011 1111 0010 1110
     * 0b 0010 0101 1010 1100 0011 1111 0010 1110
     * ^
     * 0b 0000 0000 0000 0000 0010 0101 1010 1100
     * => 0010 0101 1010 1100 0001 1010 1000 0010
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
```

##### 2.4 putVal()插入元素

```java
/**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value：如果插入的key存在，就不插入
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        /**
         * tab：当前HashMap的散列表
         * p：当前散列表的元素
         * n：表示散列表数组的长度
         * i：表示路由寻址结果的下标
         */
        Node<K,V>[] tab; Node<K,V> p; int n, i;

        //延迟初始化：第一次putVal数据时会初始化hashMap对象中最耗费内存的散列表
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;

        //最简单的一种情况：寻址找到的桶位刚好是null,这时候,会创建一个Node节点设置key-value值赋值给找到的桶位。
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            /**
             * e：Node临时元素（不为null的话，找到了一个与当前插入的key-value hash一致的元素）
             * k：临时的一个key
             */
            Node<K,V> e; K k;

            /**
             * 表示桶位中的该元素，与你当前插入的key完全一致，表示后续需要进行替换操作。
             */
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            /**
             * 判断当前桶是否转换成红黑树结构
             */
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                //链表的情况，而且链表的头元素与我们插入的key不一致。
                for (int binCount = 0; ; ++binCount) {
                    //条件成立：说明迭代到最后一个元素，也没有找到一个与你要插入的key一致的Node
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        //条件成立：说明当前链表长度达到树化标准了，需要进行树化
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }

                    //条件成立：说明找到了相同key的node元素，需要进行替换操作。
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }

            //条件成立说明：找到了一个与你插入元素key完全一直的数据。
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        //表示散列表数据结构被修改的次数。替换Node元素的value不算。
        ++modCount;
        //插入新元素，size自增,如果自增后的值大于扩容阈值，触发扩容
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }
```

##### 2.5 resize()扩容方法

```java
/**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
	 * 为什么需要扩容？
	 * 为了解决哈希重入导致的链化影响查询效率的问题，扩容会缓解该问题。
	 *
     * @return the table
     */
    final Node<K,V>[] resize() {
    	//oldTab：扩容前的哈希表
        Node<K,V>[] oldTab = table;
        //oldCap：扩容前table数组的长度。
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //oldThr：表示扩容之前的扩容阈值，触发本次扩容的阈值。
        int oldThr = threshold;
		/**
		 * newCap：扩容之后table数组的大小。
		 * newThr：扩容之后，下次再次触发扩容的条件
		 */
		int newCap, newThr = 0;

		//条件成立说明：hashMap中的散列表已经初始化过了。这是一次正常的扩容情况。
        if (oldCap > 0) {

        	//扩容之前的table数组大小已经达到最大长度了，则不扩容，且设置扩容条件为int最大值
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
			/**
			 * (newCap = oldCap << 1) < MAXIMUM_CAPACITY: oldCap左移一位，相当于乘以2，并赋值给newCap，newCap小于数组最大长度限制
			 * oldCap >= DEFAULT_INITIAL_CAPACITY : 扩容之前的table数组长度 >= 默认(缺省)的数组长度(16):
			 * 也就是说数组还未占满16就不需要扩容，虽然此时总的键值对个数已经超过阈值(MIN_TREEIFY_CAPACITY)。
			 *
			 * 这种情况下，则下次扩容的阈值等于当前阈值 * 2:这里只是设置扩容的阈值，newCap在if中已经<<1(乘以2了)
			 */
			else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }

		/**
		 * 前提条件。oldCap == 0的情况:说明hashMap中的散列表是null
		 * oldThr > 0 条件成立：
		 * 1. new HashMap(initCap,loadFactor)
		 * 2. new HashMap(initCap)
		 * 3. new HashMap(map):并且这个map是有数据的
		 */
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;

		/**
		 * 前提条件：oldCap == 0 和oldThr == 0 的情况
		 * 使用new HashMap()构造方法创建的。
		 */
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }

        // newThr为零是，通过newCap和loadFactor计算出一个newThr
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
		//根据newCap创建一个新的数组
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;

        //说明，hashMap本次扩容之前，table不为null
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
            	//当前node节点
                Node<K,V> e;
                //说明当前node节点有数据
                if ((e = oldTab[j]) != null) {
					//方便JVM GC时回收内存。
                    oldTab[j] = null;

                    //说明当前node节点是单个元素。
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;

					//说明当前node节点是红黑树结构。
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);

					//说明当前node节点是链表结构。
                    else { // preserve order
						//低位链表：存放在扩容之后数组的下标位置，与当前数组的位置一致。
						Node<K,V> loHead = null, loTail = null;
						//高位链表：存放在扩容之后的数组下标位置为 当前数组下标位置 + 扩容之前的数组的长度。
                        Node<K,V> hiHead = null, hiTail = null;

                        Node<K,V> next;
                        do {
                            next = e.next;
							/**
							 * hash-> .... 1 1111
							 * hash-> .... 0 1111
							 *
							 * oldCap-> 10000
							 * 判断高位的元素是否为0
							 * 0 ：数据放在 低位链表 中
							 * 1 ：数据放在 高位链表 中
							 */
							if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                        	//相当于把扩容之前的链接关系给断掉：此时的loTail指向的是最后一个低位元素，而原来的这个低位元素.next可能指向一个高位元素
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                        	//相当于把扩容之前的链接关系给断掉，原因同上
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

##### 2.6 get()查询方法

```java
public V get(Object key) {
        Node<K,V> e;
    	//这里会对key进行一次hash，因为在put的时候也会对key进行一次hash，让高16位也参与运算
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }
    
    /**
     * Implements Map.get and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    final Node<K,V> getNode(int hash, Object key) {
		/**
		 * tab：引用当前HashMap的散列表
		 * first：桶位中的头元素
		 * e：临时Node元素
		 * n：table数组长度
		 */
		Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
				//找到对应key的位置
            (first = tab[(n - 1) & hash]) != null) {

        	//第一种情况：定位出来的桶位元素就是我们要get的数据
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;

            if ((e = first.next) != null) {
            	//第二种情况：如果对应的桶元素为红黑树结构
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);

                //第三种情况：对应的桶元素为链表结构。
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

##### 2.7 remove()删除方法

```java
public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * Implements Map.remove and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
		/**
		 * tab：引用当前hashMap中的散列表
		 * p：当前Node元素
		 * n：散列表数组的长度
		 * index：寻址结果
		 */
		Node<K,V>[] tab; Node<K,V> p; int n, index;

        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
			/**
			 * 进入if说明对应的桶位是有数据的，需要进行查找并删除
			 * node：查找到的结果
			 * e：当前Node的下一个元素
			 */
			Node<K,V> node = null, e; K k; V v;

			//第一种情况：当前桶位中的元素就是你要删除的元素
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;


            else if ((e = p.next) != null) {
            	//第二种情况：当前桶位是红黑树结构
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);

                //第三种情况：当前桶位时链表结构
                else {
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }

            //判断node不为空的话，说明按照key查找到了需要删除的数据了
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {

            	//第一种情况：红黑树结构
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);

                //第二种情况：需要删除的桶位元素是第一个元素
                else if (node == p)
                    tab[index] = node.next;

                //第三中情况：链表结构：此时 p.next = node
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }
```

##### 2.8 replace()方法

```java
 	@Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
            ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }
```

#### 3. 红黑树

* **情景1**：红黑树为空树
  * 将根节点染色为黑色
* **情景2**：插入节点的key已经存在
  * 在insert方法中已经进行了覆盖处理，这里不需要处理。
* **情景3**：插入节点的父节点为黑色
  * 因为插入的路径黑色节点没有变化，所以红黑树依然平衡。
* **情景4**：插入节点的父节点为红色（需要咱们去处理）
  * **情景4.1**：叔叔节点存在，并且为红色（**父-叔 双红**）
    * 将父节点和叔叔节点染色为黑色，将爷爷节点染色为红色，并且再以爷爷节点为当前节点，然后进行下一轮处理。
  * **情景4.2**：叔叔节点不存在，或者为黑色，父节点为爷爷节点的左子树
    * **情景4.2.1**：插入节点为其父节点的左子节点（LL情况）
      * 将父节点染色成黑色，将爷爷节点染色为红色，然后以爷爷节点进行右旋。
    * **情景4.2.2**：插入节点为其父节点的右子节点（LR情况）
      * 以父节点进行一次左旋，得到LL双红的情景(4.2.1),然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
  * **情景4.3**：叔叔节点不存在，或者为黑色，父节点为爷爷节点的右子树
    * **情景4.3.1**：插入节点为其父节点的右子节点（RR情况）
      * 将父节点染色为黑色，将爷爷节点染色为红色，然后以爷爷节点进行左旋。
    * **情景4.3.2**：插入节点为其父节点的左子节点（RL情况）
      * 以父节点进行一次右旋，得到RR双红的情景，然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。

```java
/**
 * 自己手写红黑二叉树
 * 1.创建RBTree类：定义颜色
 * 2.创建内部类RBNode
 * 3.辅助方法定义：parentOf(node),isRed(node),setRed(node),setBlack(node),inOrderPrint()
 * 4.左旋方法定义：leftRotate(node)
 * 5.右旋方法定义：rightRotate(node)
 * 6.公开插入接口方法定义：insert(K key,V value)
 * 7.内部插入接口方法定义：insert(RBNode node)
 * 8.修正插入导致红黑树失衡的方法定义：insertFIxUp(TBNode node);
 * 9.测试红黑树的正确性
 */

public class RBTree<K extends Comparable<K>,V>{

	private static final boolean RED = true;
	private static final boolean BLACK = false;

	//树根的引用
	private RBNode root;

	public RBNode getRoot() {
		return root;
	}

	/**
	 * 获取当前节点的父节点
	 * @param node
	 */
	private RBNode parentOf(RBNode node){
		if(node != null){
			return node.parent;
		}
		return null;
	}
	/**
	 * 判断节点是否为红色
	 * @param node
	 */
	private boolean isRed(RBNode node){
		return node != null && node.color == RED;
	}

	/**
	 * 判断节点是否为黑色
	 * @param node
	 */
	private boolean isBlack(RBNode node){
		return node != null && node.color == BLACK;
	}

	/**
	 * 设置节点为红色
	 * @param node
	 */
	private boolean setRed(RBNode node){
		if(node != null){
			node.color = RED;
			return true;
		}
		return false;
	}
	/**
	 * 设置节点为黑色
	 * @param node
	 */
	private boolean setBlack(RBNode node){
		if(node != null){
			node.color = BLACK;
			return true;
		}
		return false;
	}

	/**
	 * 中序打印二叉树
	 */
	public void inOrderPrint(){
		inOrderPrint(this.root);
	}
	private void inOrderPrint(RBNode node){
		if(node != null){
			inOrderPrint(node.left);
			System.out.println("key:"+node.key + ",value:" + node.value);
			inOrderPrint(node.right);
		}
	}

	/**
	 * 左旋方法
	 * 左旋示意图：左旋x节点
	 *    p                   p
	 *    |                   |
	 *    x                   y
	 *   / \         ---->   / \
	 *  lx  y               x   ry
	 *     / \             / \
	 *    ly  ry          lx  ly
	 *
	 * 左旋做了几件事？
	 * 1. 将x的右子节点指向y的左子节点：将y的左子节点的父节点指向x。
	 * 2.更新y的父节点为x的父节点，当x的父节点不为空时，将x的父结点的子节点设置为y
	 * 3.将x的父节点更新为y，将y的左子节点更新为x
	 */

	private void leftRotate(RBNode x){
		RBNode y = x.right;
		RBNode lx = x.left;
		//1. 将x的右子节点指向y的左子节点：将y的左子节点的父节点指向x。
		x.right = y.left;
		if(y.left != null){
			y.left.parent = x;
		}
		//2.更新y的父节点为x的父节点，当x的父节点不为空时，将x的父结点的子节点设置为y
		y.parent = x.parent;
		if(x.parent != null){
			if(x == x.parent.left){
				x.parent.left = y;
			}else {
				x.parent.right = y;
			}
		}else {
			//说明x是root节点，需要将y的父节点设置为null。
			this.root = y;
		}
		//3.将x的父节点更新为y，将y的左子节点更新为x
		x.parent = y;
		y.left = x;
	}

	/**
	 * 右旋方法
	 * 右旋示意图：右旋y节点
	 *
	 *    p                       p
	 *    |                       |
	 *    y                       x
	 *   / \          ---->      / \
	 *  x   ry                  lx  y
	 * / \                         / \
	 *lx  ly                      ly  ry
	 *
	 * 右旋都做了几件事？
	 * 1.将y的左子节点设置为x的右子节点，将x的右子节点的父节点指向y
	 * 2.将x的父节点设置为y的父节点，当y的父节点不为空时:将y的父节点的子节点设置为x
	 * 3.将y的父节点设置为x，将x的右子节点设置为y
	 */

	private void rightRotate(RBNode y){
		RBNode x = y.left;

		//1.将y的左子节点设置为x的右子节点，将x的右子节点的父节点指向y
		y.left = x.right;
		if(x.right != null){
			x.right.parent = y;
		}

		//2.将x的父节点设置为y的父节点，当y的父节点不为空时:将y的父节点的子节点设置为x
		x.parent = y.parent;
		if(y.parent != null){
			if(y.parent.right == y){
				y.parent.right = x;
			}else {
				y.parent.left = x;
			}
		}else {
			this.root = x;
		}

		//3.将y的父节点设置为x，将x的右子节点设置为y
		y.parent = x;
		x.right = y;
	}

	/**
	 * 公开的插入方法
	 * @param key
	 * @param value
	 */
	public void insert(K key ,V value){
		RBNode node = new RBNode();
		node.setKey(key);
		node.setValue(value);
		//新节点一定是红色
		node.setColor(RED);

		insert(node);
	}

	private void insert(RBNode node){
		//1.查找当前node的父节点
		RBNode parent = null;
		RBNode x = this.root;

		while (x != null){
			parent = x;
			/**
			 * tmp < 0 说明node.key 小于 parent.key 需要到parent的左子树查找
			 * tmp == 0 说明node.key 等于 parent.key 需要进行替换操作
			 * tmp > 0 说明node.key 大于 parent.key 需要到parent的右子树查找
			 */
			int tmp = node.key.compareTo(parent.key);
			if(tmp < 0){
				x = x.left;
			}else if(tmp == 0){
				parent.setValue(node.value);
				return;
			}else {
				x = x.right;
			}
		}

		node.parent = parent;

		//判断node和parent的key谁大：决定node在parent的左子树还是右子树
		if(parent != null){
			int cmp = node.key.compareTo(parent.key);
			if(cmp < 0){//当前node的key比parent的key小，需要把node放入parent的左子节点
				parent.left = node;
			}else {//当前node的key比parent的key大，需要把node放入parent的右子节点
				parent.right = node;
			}
		}else {
			this.root = node;
		}

		//需要调用修复红黑树平衡的方法
		insertFixUp(node);

	}

	/**
	 * 插入后修复红黑树平衡的方法
	 *     |---情景1：红黑树为空树：将根节点染色为黑色
	 *     |---情景2：插入节点的key已经存在：在insert方法中已经进行了覆盖处理
	 *     |---情景3：插入节点的父节点为黑色：因为插入的路径黑色节点没有变化，所以红黑树依然平衡。
	 *
	 *     情景4 需要咱们去处理
	 *     |---情景4：插入节点的父节点为红色
	 *          |---情景4.1：叔叔节点存在，并且为红色（父-叔 双红）：
	 *          		将父节点和叔叔节点染色为黑色，将爷爷节点染色为红色，并且再以爷爷节点为当前节点，然后进行下一轮处理。
	 *          |---情景4.2：叔叔节点不存在，或者为黑色，父节点为爷爷节点的左子树
	 *               |---情景4.2.1：插入节点为其父节点的左子节点（LL情况）
	 *               	将父节点染色成黑色，将爷爷节点染色为红色，然后以爷爷节点进行右旋。
	 *               |---情景4.2.2：插入节点为其父节点的右子节点（LR情况）
	 *               	以父节点进行一次左旋，得到LL双红的情景(4.2.1),然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
	 *          |---情景4.3：叔叔节点不存在，或者为黑色，父节点为爷爷节点的右子树
	 *               |---情景4.3.1：插入节点为其父节点的右子节点（RR情况）
	 *               	将父节点染色为黑色，将爷爷节点染色为红色，然后以爷爷节点进行左旋。
	 *               |---情景4.3.2：插入节点为其父节点的左子节点（RL情况）
	 *               	以父节点进行一次右旋，得到RR双红的情景，然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
	 */
	private void insertFixUp(RBNode node){
		this.root.setColor(BLACK);

		RBNode parent = parentOf(node);
		RBNode gparent = parentOf(parent);

		//情景4：插入的节点的父节点是红色
		if(parent != null && isRed(parent)){
			//如果父节点是红色，那么一定存在爷爷节点。因为节点不可能出现两个红色。
			RBNode uncle = null;

			if(parent == gparent.left){//父节点为爷爷节点的左节点
				uncle = gparent.right;

				/**
				 * 情景4.1：叔叔节点存在，并且叔叔节点也是红色
				 * 		//将父节点和叔叔节点染色为黑色，将爷爷节点染色为红色，并且再以爷爷节点为当前节点，然后进行下一轮处理。
				 */
				if(uncle != null && isRed(uncle)){
					parent.setColor(BLACK);
					uncle.setColor(BLACK);
					gparent.setColor(RED);
					insertFixUp(gparent);
					return;
				}
				//情景4.2：叔叔节点不存在，或者为黑色，父节点为爷爷节点的左子树
				if(uncle == null || isBlack(uncle)){
					/**
					 * 情景4.2.1：插入节点为其父节点的左子节点（LL情况）
					 * 		将父节点染色成黑色，将爷爷节点染色为红色，然后以爷爷节点进行右旋。
					 */
					if(node == parent.left){
						parent.setColor(BLACK);
						gparent.setColor(RED);
						rightRotate(gparent);
						return;
					}
					/**
					 * 情景4.2.2：插入节点为其父节点的右子节点（LR情况）
					 * 		以父节点进行一次左旋，得到LL双红的情景(4.2.1),然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
					 */
					if(node == parent.right){
						leftRotate(parent);
						insertFixUp(parent);
						return;
					}


				}

			}else {//父节点为爷爷节点的右节点
				//情景4.3：叔叔节点不存在，或者为黑色，父节点为爷爷节点的右子树
				uncle = gparent.left;
				/**
				 * 情景4.1：叔叔节点存在，并且叔叔节点也是红色
				 * 		//将父节点和叔叔节点染色为黑色，将爷爷节点染色为红色，并且再以爷爷节点为当前节点，然后进行下一轮处理。
				 */
				if(uncle != null && isRed(uncle)){
					parent.setColor(BLACK);
					uncle.setColor(BLACK);
					gparent.setColor(RED);
					insertFixUp(gparent);
					return;
				}

				//情景4.3：叔叔节点不存在，或者为黑色，父节点为爷爷节点的右子树
				if(uncle == null || isBlack(uncle)){
					/**
					 * 情景4.3.1：插入节点为其父节点的右子节点（RR情况）
					 * 		将父节点染色为黑色，将爷爷节点染色为红色，然后以爷爷节点进行左旋。
					 */
					if(parent.right == node){
						parent.setColor(BLACK);
						gparent.setColor(RED);
						leftRotate(gparent);
						return;
					}
					/**
					 * 情景4.3.2：插入节点为其父节点的左子节点（RL情况）
					 * 		以父节点进行一次右旋，得到RR双红的情景，然后指定父节点（此时的父节点为左旋之前的爷爷节点）为当前节点进行下一轮处理。
					 */
					if(parent.left == node){
						rightRotate(parent);
						insertFixUp(parent);
						return;
					}
				}



			}
		}
	}

	//内部类RBNode
	static class RBNode <K extends Comparable<K> ,V> {
		private RBNode parent;
		private RBNode left;
		private RBNode right;
		private boolean color;
		private K key;
		private V value;

		public RBNode(RBNode parent, RBNode left, RBNode right, boolean color, K key, V value) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.color = color;
			this.key = key;
			this.value = value;
		}

		public RBNode() {
		}

		public RBNode getParent() {
			return parent;
		}

		public void setParent(RBNode parent) {
			this.parent = parent;
		}

		public RBNode getLeft() {
			return left;
		}

		public void setLeft(RBNode left) {
			this.left = left;
		}

		public RBNode getRight() {
			return right;
		}

		public void setRight(RBNode right) {
			this.right = right;
		}

		public boolean isColor() {
			return color;
		}

		public void setColor(boolean color) {
			this.color = color;
		}

		public K getKey() {
			return key;
		}

		public void setKey(K key) {
			this.key = key;
		}

		public V getValue() {
			return value;
		}

		public void setValue(V value) {
			this.value = value;
		}
	}


}
```

