package com.dlw;

import java.util.Scanner;

public class RBTreeTest {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		RBTree<String,Object> rbt = new RBTree<String, Object>();
		while (true){
			System.out.print("请输入key：");
			String key = scanner.next();
			System.out.println();
			rbt.insert(key,null);

			TreeOperation.show(rbt.getRoot());
		}
	}
}
