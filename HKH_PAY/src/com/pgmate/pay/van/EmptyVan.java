package com.pgmate.pay.van;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;

/**
 * @author Administrator
 *
 */
public class EmptyVan implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.EmptyVan.class ); 

	public EmptyVan() {
		
	}

	public EmptyVan(SharedMap<String, Object> tmnVanMap) {
		
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		response.pay.authCd = "";
		response.result 	= ResultUtil.getResult("9999","카드사미등록","카드사 정보 미설정 관리자에게 문의하시기 바랍니다.");
		sharedMap.put("vanId","");
		sharedMap.put("vanTrxId","EMT_"+CommonUtil.getCurrentDate("HHmmssSSS"));
		sharedMap.put("vanResultCd","9999");
		sharedMap.put("vanResultMsg","VAN 설정이 되지 않았습니다.");
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		response.refund.authCd = "";
		response.result 	= ResultUtil.getResult("9999","카드사미등록","카드사 정보 미설정 관리자에게 문의하시기 바랍니다.");
		sharedMap.put("vanId","");
		sharedMap.put("vanTrxId","EMT_"+CommonUtil.getCurrentDate("HHmmssSSS"));
		sharedMap.put("vanResultCd","9999");
		sharedMap.put("vanResultMsg","VAN 설정이 되지 않았습니다.");
		return sharedMap;
	}

}
