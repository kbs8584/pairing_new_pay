package com.pgmate.pay.util;

import com.pgmate.lib.vertx.main.VertXUtil;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class APIPath {

	/**
	 * 
	 */
	public APIPath() {
		// TODO Auto-generated constructor stub
	}
	
	public static void setPath(RoutingContext rc){
		
		if (VertXUtil.getHost(rc).indexOf("api.pairingpayments.nett") > -1) {
			rc.put("API_HOST", "https://api.pairingpayments.nett");
		} else { // DEV,SANDBOX CONFIG
			rc.put("API_HOST", VertXUtil.getSchemeHost(rc));
		}
	}

}
