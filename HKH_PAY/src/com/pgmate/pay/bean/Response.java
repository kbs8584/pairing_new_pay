package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.van.DanalVacct;

/**
 * @author Administrator
 *
 */
public class Response {
	public Result result	= null;
	public Pay  pay 		= null;
	public Refund refund	= null;
	public Cash cash		= null;
	public CashCC cashcc	= null;
	public Vact vact		= null;
	public BillPay billPay	= null;
	public SharedMap<String,Object> widget = null;
	public Auth auth		= null;
	
	public DanalBean danalBean			= null;
	
	public Response() {
		
	}
 
}
