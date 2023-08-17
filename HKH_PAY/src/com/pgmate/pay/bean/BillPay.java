package com.pgmate.pay.bean;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class BillPay extends Base{

	public String billId	= "";
	@UserExclude public String summary	= "";
	public String authCd	= null;
	public String trxDate	= null;
	@UserExclude public int installment	= 0; 
	public String brand	= null;
	
	public BillPay() {
		// TODO Auto-generated constructor stub
	}

}
