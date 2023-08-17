package com.pgmate.pay.bean;

import com.pgmate.lib.util.gson.UserExclude;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Auth {

	public String trxId		= "";
	public String trxType	= "";		//card, account, phone , 
	public String tmnId		= "";
	public String trackId	= "";
	@UserExclude public String payerName	= "";
	@UserExclude public String payerEmail	= "";
	@UserExclude public String payerTel	= "";
	@UserExclude public String webhookUrl = null;
	public String udf1		= "";
	public String udf2		= "";

	public Card card		= null;
	@UserExclude public boolean recurring = false;
	public SharedMap<String,String> metadata = null;
	
	
	
	public Auth() {
		// TODO Auto-generated constructor stub
	}

}
