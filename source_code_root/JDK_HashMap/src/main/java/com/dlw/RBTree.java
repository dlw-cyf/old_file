package com.dlw;

import com.sun.org.apache.regexp.internal.RE;

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
