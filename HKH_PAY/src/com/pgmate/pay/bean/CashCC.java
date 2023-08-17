package com.pgmate.pay.bean;

/**
 * @author Administrator
 *
 */
public class CashCC extends Base{
	//웹캐쉬 전문 
	//public String trxType	= "";	//issue,revoke
	public String cashType	= null;	//0:일반 1:도서공연비 소득공제,
	public String trxDate	= "";	//요청일자 및 응답 일자 
	public String usage		= null;  //0:소비자소득공제,1:사업자지출증빙,2:자진발급
	public String identity	= null;	//현금영수증카드번호,휴대폰번호(소득공제), 사업자번호(지출증빙)
	
	public String authCd	= null;
	public String rootTrxId	= null;	//취소시 원거래번호 
	public String rootTrxDay= null; //취소시 거래일자
	public String rootTrackId= null;//취소시 주문번호
	
	
	public long supply		= 0;
	public long vat			= 0;
	public long service		= 0;
	
	

	public CashCC() {
		// TODO Auto-generated constructor stub
	}

}
