package com.pgmate.pay.bean;

import java.util.List;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class PayLinkNine extends Base{

	public String trxId								= null;
	public String vanTrxId							= null;
	public String authCd							= null;
	public String trxDate							= null;
	
	public String cardCode						= null;
	public String cardNumber					= null;
	public String installment						= null;
	public String amount							= null;
	
	public String payerName						= null;
	
	public String prdName						= null;
	
	public String compNo							= null;
	
	public PayLinkNine() {
		// TODO Auto-generated constructor stub
	}
}
