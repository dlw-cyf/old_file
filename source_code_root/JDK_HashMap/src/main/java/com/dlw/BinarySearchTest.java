package com.dlw;

public class BinarySearchTest {

	public static void main(String[] args) {
		int [] arr = new int[50];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = i+1;
		}

		int index = binarySearch(arr, 100);
		System.out.println("index:"+index);
	}

	/**
	 * 二分查找算法
	 * @param arr 有序数组
	 * @param data 查找的数据
	 * @return index 下标，未查找到时返回-1
	 */
	public static int binarySearch(int [] arr , int data){
		int low = 0;
		int height = arr.length-1;

		while (low <= height){
			int mid = low + (height - low) / 2;

			int midData;
			if((midData = arr[mid]) == data){
				return mid;
			}else if(midData < data){
				low = mid +1;

			}else {
				height = mid -1;
			}
		}


		return -1;
	}
}
