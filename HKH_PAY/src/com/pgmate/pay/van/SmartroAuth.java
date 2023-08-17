package com.pgmate.pay.van;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;

public class SmartroAuth implements Van {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.SmartroAuth.class);

	public SmartroAuth() {
		
	}

	public SmartroAuth(SharedMap<String, Object> tmnVanMap) {

	}
	
	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		logger.info("#### SmartroAuth TRANSACTION ####");
		response.result = ResultUtil.getResult("0000", "정상", "정상승인");
		sharedMap.put("vanId",sharedMap.get("vanId"));
		sharedMap.put("vanTrxId", sharedMap.get("vanTrxId"));
		sharedMap.put("vanResultCd", "0000");
		sharedMap.put("vanResultMsg", "정상승인");
		sharedMap.put("vanDate", "20" + sharedMap.get("vanDate"));
		sharedMap.put("authCd", sharedMap.get("authCd"));
		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		logger.info("#### SmartroAuth TRANSACTION ####");
		logger.info("sharedMap : " + sharedMap.toString());
		logger.info("payMap : " + payMap.toString());
		logger.info("response : " + response.toString());
		
		response.refund.authCd = "10"+CommonUtil.getCurrentDate("HHmmss");
		response.result 	= ResultUtil.getResult("0000","정상","승인취소");
		sharedMap.put("van", "SMARTRO");
		sharedMap.put("vanTrxId", sharedMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd","0000");
		sharedMap.put("vanResultMsg","정상취소메시지");
		sharedMap.put("vanDate", "20" + response.pay.metadata.get("AuthDate"));
		return sharedMap;
	}

}