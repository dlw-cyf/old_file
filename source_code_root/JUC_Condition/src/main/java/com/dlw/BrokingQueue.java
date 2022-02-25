package com.dlw;

public interface BrokingQueue<T> {

	/**
	 * 插入数据的接口
	 */
	void put(T element);

	/**
	 * 获取数据的接口
	 */
	T take();
}
