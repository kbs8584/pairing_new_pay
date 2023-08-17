/*
 * Prodject Name	:   ktt_pgv
 * File Name		:	KSNETVoid.java
 * Date				:	오전 12:26:50
 * History			:	2007. 04. 18
 * Version			:	1.0
 * Author			:	(임주섭)ginaida@ginaida.net
 * Comment      	:	  				 
 */


package com.pgmate.pay.van;


import com.pgmate.lib.util.lang.CommonUtil;

public class KspayRefund {
	
	private String reqType			= "";	//승인구분
	private String voidType			= "";	//취소처리구분 0 :거래번호취소 , 1:주문번호취소
	private String ksnetTrnId		= "";	//KSNET 거래번호 취소구분이 1인경우 SPACE
	private String trnDate			= "";	//취소구분이 0 인경우 SPACE
	private String transactionId	= "";	//취소구분이 0 인 경우 SPACE
	private long amount				= 0;	//부분취소금액
	private int partRefundCnt		= 0;	//부분취소차수(01-99)
	private String extra			= "";	
	
	
	public KspayRefund(){
		
	}
	
	public byte[] getKSNETVoid(){
		StringBuffer transaction = new StringBuffer();
		transaction.append(CommonUtil.byteFiller(reqType		,4));
		transaction.append(CommonUtil.byteFiller(voidType		,1));
		transaction.append(CommonUtil.byteFiller(ksnetTrnId		,12));
		transaction.append(CommonUtil.byteFiller(trnDate		,8));
		transaction.append(CommonUtil.byteFiller(transactionId	,50));
		if(amount ==0) {	//전체취소,부분취소
			transaction.append(CommonUtil.byteFiller(extra			,75));
		}else {
			transaction.append(CommonUtil.zerofill(amount	,9));
			transaction.append(CommonUtil.zerofill(partRefundCnt	,2));
			transaction.append(CommonUtil.byteFiller(extra			,64));
		}
		
		return (transaction.toString()).getBytes();
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getKsnetTrnId() {
		return ksnetTrnId;
	}

	public void setKsnetTrnId(String ksnetTrnId) {
		this.ksnetTrnId = ksnetTrnId;
	}

	public String getReqType() {
		return reqType;
	}

	public void setReqType(String reqType) {
		this.reqType = reqType;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTrnDate() {
		return trnDate;
	}

	public void setTrnDate(String trnDate) {
		this.trnDate = trnDate;
	}

	public String getVoidType() {
		return voidType;
	}

	public void setVoidType(String voidType) {
		this.voidType = voidType;
	}

	/**
	 * @return the amount
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(long amount) {
		this.amount = amount;
	}

	/**
	 * @return the partRefundCnt
	 */
	public int getPartRefundCnt() {
		return partRefundCnt;
	}

	/**
	 * @param partRefundCnt the partRefundCnt to set
	 */
	public void setPartRefundCnt(int partRefundCnt) {
		this.partRefundCnt = partRefundCnt;
	}
	
	
	
	
	
	
	
}
