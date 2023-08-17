package com.pgmate.pay.bean;

import java.util.List;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class vacct {
	/**************************************************
	 * 결제 정보
	 **************************************************/
	public String accntHolder= null;	// 예금주
	public long amount		= 0;	// 금액
	public List<Product> products = null; // 상품정보 리스트
	public String expireDate	= null;	// 입금만료일
	public String trackId		= null;	// 주문번호
	
	public String bankCd		= null;	// 은행코드
	public String byPassValue= null;	// 추가필드 값
	public String isCashReceiptUi = null; // 결제창 현금영수증 UI 표시 설정 유무
	/**************************************************
	 * 구매자 정보
	 **************************************************/
	public String payerEmail	= null;	// 이메일 가상계좌발급통지 또는 입금 통지 
	public String payerTel	= "";			// 구매자 연락처
	public String payerName = ""; 		// 구매자 성명
	public String userId		= null;	// 구매자 아이디
	public String userAgent	= null;	// 사용자환경 - "PC/MW/MA/MI" 중 하나
	/**************************************************
	 * 연동 정보
	 **************************************************/
	public String returnUrl	= null;	// 은행코드
	public String notiUrl		= null;	// 은행코드
	public String cancelUrl	= null;	// 계좌번호
	
	public String txType		= "AUTH";	// 고정값
	public String serviceType	= "DANALVACCOUNT";	// 고정값 
	
	// OUTPUT
	public String bankName	= null;	// 은행이름
	public String account		= null;	// 가상계좌
	public String expireTime	= null;	// 입금만료일시
	
	public String tmnId			= null; //yhbae 20190920 터미널 ID 추가

	public vacct() {
	}

}
