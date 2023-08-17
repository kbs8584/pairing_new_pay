package com.pgmate.pay.van;

/**
 * @author Administrator
 *
 */
public class CashInter {

	
	public String trxType	= "";	//mcht, issue, revoke, statistic   9310 가맹점등록 / 9150 승인, 9160취소, 9201 ERR, 8120 집계  
	public String trxId		= "";	//승인,취소시만 전달
	public String trackId	= "";	//가맹점 주문번호
	public String tmnId		= "";	//터미널 아이디
	public String trxDate	= "";	//요청일자 및 응답 일자 
	public String resultCd	= null;	//응답코드 
	public String resultMsg = null;	//응답메세지 
	public String authCd	= null;
	public String rootTrxId	= null;	//취소시 원거래번호 
	public String cashType	= null;	//0:일반 1:도서공연비 소득공제
	public long amount		= 0;
	public long service		= 0;
	public long vat			 = 0;
	public long supply		=  0;
	public String usage		= null;  //0:소비자소득공제,1:사업자지출증빙
	public String identity	= null;	//현금영수증카드번호,휴대폰번호(소득공제), 사업자번호(지출증빙)
	
	public CashInter() {
		// TODO Auto-generated constructor stub
	}

}
