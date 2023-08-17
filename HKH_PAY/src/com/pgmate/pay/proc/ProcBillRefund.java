package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.APICall;

/**
 * @author Administrator
 *
 */
public class ProcBillRefund extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcBillRefund.class );
	private String trxId 	= "";
	private String trackId = "";
	
	public ProcBillRefund(String trxId,String trackId) {
		this.trxId = trxId;
		this.trackId = trackId;
		
	}
	
	
	public void run(){
		
		Request reqRef = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= trackId;	//취소 주문번호
		refund.amount		= ProcBillUser.AMOUNT;
		refund.rootTrxId 	= trxId;
		
		reqRef.refund = refund;
		new APICall().comm(reqRef,ProcBillUser.AUTH_KEY,ProcBillUser.REFUND_URL);
	}
	
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
