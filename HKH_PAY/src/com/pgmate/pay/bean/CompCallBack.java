package com.pgmate.pay.bean;

/**
 * @author Administrator
 *
 */
public class CompCallBack {

	public String compNo			= null;
	public String compMember	= null;
	public String mchtKey			= null;
	public Long amount			= null;
	public String payerName		= null;
	public String payerTel			= null;
	public String payerEmail		= null;
	public String number			= null;
	public String expiry			= null;
	public String installment		= null;
	public String name				= null;
	public Long price				= null;
	public Integer qty				= null;
	
	public String cardAuth		= null;
	public String authPw			= null;
	public String authDob			= null;
	
	// 2022-08-01 - 영업사원 ID 추가
	public String salesId			= null;
	// 2022-08-05 - 인증결제 암호화데이터 추가
	public String encData 			= null;

	public CompCallBack() {
		// TODO Auto-generated constructor stub
	}

}
