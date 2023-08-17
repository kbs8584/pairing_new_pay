package com.pgmate.pay.bean;

import java.util.List;

/**
 * @author Administrator
 *
 */
public class DanalBean {
	public String mchtId		= "";
	/**************************************************
	 * 결제 정보
	 **************************************************/
	public String accntHolder= "";	// 예금주
	public String amount		= "";		// 금액
	public String prdName 	= ""; 	// 상품이름
	
	public String bankCd		= "";	// 은행코드
	public String byPassValue= "";	// 추가필드 값
	public String isCashReceiptUi = ""; // 결제창 현금영수증 UI 표시 설정 유무
	/**************************************************
	 * 구매자 정보
	 **************************************************/
	public String payerEmail	= "";	// 이메일 가상계좌발급통지 또는 입금 통지 
	public String payerTel	= "";			// 구매자 연락처
	public String payerName = ""; 		// 구매자 성명
	public String userId		= "";	// 구매자 아이디
	public String userAgent	= "";	// 사용자환경 - "PC/MW/MA/MI" 중 하나
	/**************************************************
	 * 연동 정보
	 **************************************************/
	public String returnUrl	= "";	// 은행코드
	public String notiUrl		= "";	// 은행코드
	public String cancelUrl	= "";	// 계좌번호
	
	public String txType		= "";	// 고정값
	public String serviceType	= "";	// 고정값 
	
	public String expireDate = "";
	
	public String RETURNPARAMS = "";	// 결제용 암호화 데이터
	
	public DanalBean() {
		
	}

}
