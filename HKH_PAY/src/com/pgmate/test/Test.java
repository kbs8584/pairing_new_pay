package com.pgmate.test;

import java.util.UUID;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class Test {

	/**
	 * 
	 */
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args){
		System.out.println( "T"+CommonUtil.getCurrentDate("yyMMdd")+UUID.randomUUID().toString().substring(0,6));
		long amount = 200000;
		double rate = 0.0255;
		
		System.out.println(rate);
		double stlVanFee = new Double(amount*(rate*1000)).longValue()/1000;
		double vat = new Double(stlVanFee*PAYUNIT.VAT).longValue();
		System.out.println(stlVanFee);
		System.out.println(vat);
		System.out.println(stlVanFee+vat);
		
		//4124998.0
	}

}
