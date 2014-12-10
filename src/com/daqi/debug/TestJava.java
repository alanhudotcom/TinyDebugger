package com.daqi.debug;

import java.util.Random;

public class TestJava {

	public static void main(String[] args) {

		testStand(0, 10);
	}
	
	
	private static int getGausData(int start, int end) {
		double N = 10.0;		//将标准正态分布拆分的粒度
		int parts = end - start;	//份数
		int size = parts + 1;		//切割的整数范围
		double[] gausData = new double[size];
		int[] resultData = new int[size];
		double gausValue = new Random().nextGaussian();
		
		for (int i = 0; i < size; i++) {
			resultData[i] = start + i;
			gausData[i] = -N / 2 + N / parts * i;
		}

		int result = 0;
		for (int i = 0; i < gausData.length - 1; i++) {
			if (gausValue <= gausData[0]) {
				result = resultData[0];
				break;
			} else if (gausData[i] < gausValue && gausValue <= gausData[i + 1]) {
				result = resultData[i];
				break;
			} else if (gausValue > gausData[gausData.length-1]) {
				result = resultData[gausData.length - 1];
				break;
			}
		}
		return result;

	}
	
	
	
	private static void testStand(int start, int end) {
		
		System.out.println("=====testStand");
		int sum = 1 * 30 * 10000;
		
		int parts = end - start;	//份数
		int size = parts + 1;		//切割的整数范围
		int[] resultData = new int[size];
		int[] staticsresultData = new int[size];
		do {
			int gausValue = getGausData(start, end);
			
			for (int i = 0; i < size; i++) {
				resultData[i] = start + i;
			}
			
			for (int i = 0; i < resultData.length - 1;i++) {
				if (resultData[i] == gausValue) {
					++staticsresultData[i];
					break;
				}
			}
			
		} while (--sum > 0);

		for (int i = 0; i < size-1; i++) {
			System.out.print("   ===[" + resultData[i] + ", " + resultData[i+1] + "]="+ staticsresultData[i]);
		}
	}
	
	
	private static double gause() {
		Random rand = new Random();
		return rand.nextGaussian();
	}
	
	private static double Norm_rand(double miu, double sigma2) {
		double N = 12;
		double x = 0, temp = N;
//		do {
			x = 0;
			for (int i = 0; i < N; i++)
				x = x + (Math.random());
			x = (x - temp / 2) / (Math.sqrt(temp / 12));
			x = miu + x * Math.sqrt(sigma2);
//		} while (x <= 0); // 在此我把小于0的数排除掉了
		return x;
	}
	
	class GausRandom extends Random {
		
		@Override
		public synchronized double nextGaussian() {
			Random random = new Random();
			double nextNextGaussian = 0;
			boolean haveNextNextGaussian = false;
			if (haveNextNextGaussian) {
				haveNextNextGaussian = false;
				return nextNextGaussian;
			} else {
				double v1, v2, s;
				do {
					v1 = 2 * random.nextDouble() - 1; // between -1.0 and 1.0
					v2 = 2 * random.nextDouble() - 1; // between -1.0 and 1.0
					s = v1 * v1 + v2 * v2;
				} while (s >= 1 || s == 0);
				double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
				nextNextGaussian = v2 * multiplier;
				haveNextNextGaussian = true;
				return v1 * multiplier;
			}
		}
		
	}
	
}
