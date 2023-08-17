package com.pgmate.pay.bean;

import java.util.List;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class Pay extends Base{

	@UserExclude public String payerName	= "";
	@UserExclude public String payerEmail	= "";
	@UserExclude public String payerTel		= "";
	public String authCd							= null;
	public Card card								= null;
	public String trxDate							= null;
	public String webhookUrl 					= null;
	public List<Product> products 				= null;
	
	public String mchtId 							= null;
	
	public String compNo							= null;
	public String compMember					= null;
	
	public String salesId							= null;
	
	public String encData							= null;
	
	public Pay() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "Pay [payerName=" + payerName + ", payerEmail=" + payerEmail + ", payerTel=" + payerTel + ", authCd="
				+ authCd + ", card=" + card + ", trxDate=" + trxDate + ", webhookUrl=" + webhookUrl + ", products="
				+ products + ", mchtId=" + mchtId + ", compNo=" + compNo + ", compMember=" + compMember + ", salesId="
				+ salesId + ", encData=" + encData + "]";
	}

	public String getPayerName() {
		return payerName;
	}

	public void setPayerName(String payerName) {
		this.payerName = payerName;
	}

	public String getPayerEmail() {
		return payerEmail;
	}

	public void setPayerEmail(String payerEmail) {
		this.payerEmail = payerEmail;
	}

	public String getPayerTel() {
		return payerTel;
	}

	public void setPayerTel(String payerTel) {
		this.payerTel = payerTel;
	}

	public String getAuthCd() {
		return authCd;
	}

	public void setAuthCd(String authCd) {
		this.authCd = authCd;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public String getTrxDate() {
		return trxDate;
	}

	public void setTrxDate(String trxDate) {
		this.trxDate = trxDate;
	}

	public String getWebhookUrl() {
		return webhookUrl;
	}

	public void setWebhookUrl(String webhookUrl) {
		this.webhookUrl = webhookUrl;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

	public String getMchtId() {
		return mchtId;
	}

	public void setMchtId(String mchtId) {
		this.mchtId = mchtId;
	}

	public String getCompNo() {
		return compNo;
	}

	public void setCompNo(String compNo) {
		this.compNo = compNo;
	}

	public String getCompMember() {
		return compMember;
	}

	public void setCompMember(String compMember) {
		this.compMember = compMember;
	}

	public String getSalesId() {
		return salesId;
	}

	public void setSalesId(String salesId) {
		this.salesId = salesId;
	}

	public String getEncData() {
		return encData;
	}

	public void setEncData(String encData) {
		this.encData = encData;
	}

	
}
