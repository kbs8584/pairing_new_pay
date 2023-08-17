package com.pgmate.pay.bean;

/**
 * @author Administrator
 *
 */
public class CompRefund {

	public String mchtKey			= null;
	public String trxId				= null;
	public String compNo			= null;
	public String compMember	= null;
	public String amount 			= "";
	
	// 2022-08-01 - salesId 영업사원 ID 추가
	public String salesId 			= "";
	
	public CompRefund() {
		// TODO Auto-generated constructor stub
	}

}
