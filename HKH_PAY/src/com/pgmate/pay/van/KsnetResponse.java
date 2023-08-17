package com.pgmate.pay.van;

/**
 * @author Administrator
 *
 */
public class KsnetResponse {

	
	public String spec = ""; 	//0010 : MPI 가입확인 , //1110 : 승인 , 1120 : 인증후승인, 1130 : ISP-MPI,1210 :취소,1310 거래확인, 1320 : BIN조회, 1510 : TEST Call
	public String vanTr= ""; 	//van  거래번호 
	public String status= "";	//O:정상,P:자동이체정상 X:거절
	public String transactionDate = "";
	public String card	= "";
	public String expiry= ""; 
	public int quota	= 0;
	public String amount= "";
	public String message1 = "";	// OK:승인번호
	public String message2 = "";	// OK:승인번호
	public String authCode = "";
	public String cardName = "";
	public String issueCd  = "";
	public String acqCd	   = "";
	public String merchantId="";
	public String sendYn   = "";
	public String notice   = "";
	public String occurePoint = "";
	public String usablePoint = "";
	public String addedPoint  = "";
	public String pointMessage = "";
	
	

	public KsnetResponse() {
	}
	
	
	
}

