package com.dlw;

import java.util.concurrent.CyclicBarrier;

public class Test {

	public static void main(String[] args) {
		try {

			try {
				int i = 1/0;
			}finally {
				System.out.println("inner finally");
			}
			System.out.println("mid finally");
		}finally {
			System.out.println("outer finally");
		}
	}
}
