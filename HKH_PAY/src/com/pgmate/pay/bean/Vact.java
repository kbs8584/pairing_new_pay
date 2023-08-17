package com.pgmate.pay.bean;

import java.util.List;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class Vact {
	
	//GET 으로 발급가능한 가상계좌 리스트 요청할때 null 이면 전체, 그외는 지정된 은행 
	public List<String> banks	= null;	 
	public List<VactBank> vacts = null;
	

	//OPEN 관련 사항
	public String trackId		= null;	//주문번호
	public String bankCd		= null;	//은행코드
	public String account		= null;	//계좌번호
	public String oper			= null;	//연산식 기본 eq
	public String amount		= null;	//금액
	public String holderName	= null;	//예금주
	public String payerEmail	= null;	//이메일 가상계좌발급통지 또는 입금 통지 
	public String payerTel	= "";	// 구매자 연락처
	public String payerName = ""; // 구매자 성명
	public String udf1			= null;	//가맹점 정의영역1
	public String udf2			= null;	//가맹점 정의영역2
	public String webhookUrl	= null;	//웹훅 정보
	public List<Product> products = null; // 상품정보 리스트
	
	//OPEN 응답 추가 영역
	public String issueId		= null;
	public String expireAt		= null;
	public String status		= null;	//계좌발급상태
	
	public String tmnId			= null; //yhbae 20190920 터미널 ID 추가

	//CLOSE 는 뒤에 trackId 또는 issueId 
	
	//STATUS issueId 에 따른 PG_VACT_TRX 리스트 
	public List<VactHookBean> datas		= null;
	
	
	public Vact() {
	}

}
