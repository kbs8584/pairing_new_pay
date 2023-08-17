package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcGet extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcGet.class );

	public ProcGet() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_GET+"/", "");
		logger.info("GET : [{},{}]",sharedMap.getString("tmnId"),search);
		
		String res = trxDAO.getTrxIO(sharedMap.getString("tmnId"), search).trim();
		if(CommonUtil.isNullOrSpace(res)){
			logger.info("검색된 거래가 없습니다.");
			response.result = ResultUtil.getResult("9999", "결제조회실패","결제정보를 확인할 수 없습니다. 검색어 : ["+search+"]");
		}else{
			logger.info("검색된 거래가 있습니다.");
			response = (Response)GsonUtil.fromJson(res, Response.class);
		}
		
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}

	

	

}
