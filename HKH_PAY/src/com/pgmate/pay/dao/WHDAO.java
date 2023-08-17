package com.pgmate.pay.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author juseop
 *
 */
public class WHDAO extends DAO {
	private static Logger logger = LoggerFactory.getLogger(WHDAO.class);
	/**
	 * 
	 */
	public WHDAO() {
		// TODO Auto-generated constructor stub
	}
	
	
	public SharedMap<String, Object> getVanByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_TMN_ID_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN A LEFT OUTER JOIN PG_VAN B ON A.vanIdx = B.idx ");
			super.addWhere("A.tmnId", tmnId, eq);
			super.setColumns("A.*,B.vanId,B.secondKey");
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.getRow(0) == null){
				return null;
			}else{
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}
		}
	}
	
	
	public SharedMap<String, Object> getVanByVanId(String mchtId) {
		String key = "PG_VAN_VAN_ID_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VAN");
			super.setColumns("*");
			super.addWhere("vanId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.getRow(0) == null){
				return null;
			}else{
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}
		}
	}
	
	public boolean isDuplicatedVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RES");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public SharedMap<String, Object> getRfdByVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
		
	}
	
	
	public SharedMap<String, Object> getTrxPayByVanTrxId(String regDay , String van,String vanTrxId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("regDay",CommonUtil.parseLong(regDay), eq);
		super.addWhere("van", van, eq);
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	public SharedMap<String, Object> getTrxPayByTrackId(String van,String trackId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van", van, eq);
		super.addWhere("trackId", trackId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	public SharedMap<String, Object> getTrxRfdByTrxId(String trxId) {

		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("rootTrxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String,Object> getTrxRefundSumByTrxId(String trxId) {

		super.setTable("PG_TRX_CAP A LEFT JOIN PG_TRX_CAP B ON A.capId = B.rootTrxId");
		super.setColumns(" SUM(B.amount) as AMT,count(1) as CNT ");
		super.addWhere("A.trxId", trxId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.setColumns("*");
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	public void insertCard(String cardId, String value) {
		super.setTable("PG_TRX_BOX");
		super.setRecord("cardId", cardId);//1개
		super.setRecord("value", value);
		logger.info("set card : {}", super.insert());
		super.initRecord();
	}
	
	
	public SharedMap<String, Object> getAdminRfdByVanTrxId(String vanTrxId) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setColumns("idx");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public void updateAdminRfd(String idx, String trxId, String resultCd) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setRecord("trxId", trxId);
		super.setRecord("resultCd", resultCd);
		super.addWhere("idx", idx, eq);
		logger.info("set PG_TRX_ADMIN_RFD : {}", super.update());
		super.initRecord();
	}
	
	public void insertTrxRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> trxMap) {
		super.setTable("PG_TRX_RFD");
		long vat = 0;
		if(sharedMap.isEquals("taxType", "") || sharedMap.isEquals("taxType", "과세")) {
			vat = new Double(sharedMap.getLong("amount") *10 /110).longValue();
		}
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		if(sharedMap.isNullOrSpace("trackId")){
			super.setRecord("trackId", trxMap.getString("trackId"));
		}else{
			super.setRecord("trackId", sharedMap.getString("trackId"));
		}
		super.setRecord("status", "완료");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", sharedMap.getLong("amount"));
		super.setRecord("rfdVat", vat);
		super.setRecord("cardId", trxMap.getString("cardId"));
		super.setRecord("bin", trxMap.getString("bin"));
		super.setRecord("issuer", trxMap.getString("issuer"));
		super.setRecord("acquirer", trxMap.getString("acquirer"));
		super.setRecord("last4", trxMap.getString("last4"));
		super.setRecord("rootTrnDay", trxMap.getString("regDay"));
		super.setRecord("rootTrxId", trxMap.getString("trxId"));
		super.setRecord("rootTrackId", trxMap.getString("trackId"));
		super.setRecord("rootAmount", trxMap.getLong("amount"));
		super.setRecord("rootVat", trxMap.getLong("vat"));
		super.setRecord("authCd", trxMap.getString("authCd"));
		super.setRecord("reqDay", sharedMap.getString("regDate").substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString("regDate").substring(8));
		super.setRecord("resultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", sharedMap.getString("regDate").substring(0, 8));
		super.setRecord("regTime", sharedMap.getString("regDate").substring(8));
		super.setRecord("regDate", sharedMap.getString("regDate"));
		logger.info("set TRX_RFD : {}", super.insert());
		super.initRecord();
	}
	

	
	public void updateTrxPay(String trxId) {

		super.setTable("PG_TRX_PAY");
		super.setRecord("status", "승인취소");
		super.addWhere("trxId", trxId);
		logger.info("update TRX_PAY : {}", super.update());
		super.initRecord();
	}
	
	
	
	public void insertTrxREQ(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("mchtId", sharedMap.getString("mchtId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("payerName", sharedMap.getString("payerName"));
		super.setRecord("payerEmail", sharedMap.getString("payerEmail"));
		super.setRecord("payerTel", sharedMap.getString("payerTel"));
		super.setRecord("amount", sharedMap.getString("amount"));
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
		if (!CommonUtil.isNullOrSpace(sharedMap.getString(PAYUNIT.KEY_CARD))) {
			super.setRecord("issuer", sharedMap.getString("issuer"));
			super.setRecord("last4", sharedMap.getString("last4"));
			super.setRecord("cardType", sharedMap.getString("cardType"));
			super.setRecord("bin", sharedMap.getString("bin"));
			super.setRecord("installment", sharedMap.getString("installment"));
			super.setRecord("acquirer", sharedMap.getString("acquirer"));
		}
		super.setRecord("prodId", sharedMap.getString(PAYUNIT.KEY_PROD));
		super.setRecord("regDay", sharedMap.getString("regDate").substring(0, 8));
		super.setRecord("regTime", sharedMap.getString("regDate").substring(8));
		super.setRecord("regDate", sharedMap.getString("regDate"));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();

	}
	
	
	public void insertTrxRES(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_RES");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("authCd", sharedMap.getString("authCd"));
		super.setRecord("resultCd", sharedMap.getString("resultCd"));
		super.setRecord("resultMsg", sharedMap.getString("resultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("pairingVtid", "");
		super.setRecord("pairingRouteVan", "");
		super.setRecord("pairingCid", "");
		super.setRecord("regDay", sharedMap.getString("regDate").substring(0, 8));
		super.setRecord("regTime", sharedMap.getString("regDate").substring(8));
		super.setRecord("regDate", sharedMap.getString("regDate"));

		logger.info("set TRX_RES : {}", super.insert());

		if (sharedMap.getString("resultCd").equals("0000")) {
			insertTrxPAY(sharedMap.getString("trxId"));
		} else {
			insertTrxERR(sharedMap.getString("trxId"));
		}

		super.initRecord();
	}

	public void insertTrxERR(String trxId) {

		String q = "INSERT INTO PG_TRX_ERR  " + " SELECT A.trxId,trxType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,issuer,acquirer,prodId,"
				+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,B.pairingVtid,B.pairingRouteVan,B.pairingCid,B.regDay,B.regTime,B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set TRX_ERR : {}", super.update(q));
		super.initRecord();

	}

	public void insertTrxPAY(String trxId) {

		String q = "INSERT INTO PG_TRX_PAY  " + " SELECT A.trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,'승인',prodId,issuer,acquirer,"
				+ " A.regDay,A.regTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,B.pairingVtid,B.pairingRouteVan,B.pairingCid,B.regDay,B.regTime,B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("insertTrxPAY update() : {}", super.update(q));
		super.initRecord();

	}
	
	public void insertTrxWH(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		
		if(sharedMap.like("resData", "미등록터미널") || sharedMap.like("resData", "취소원거래")){
			super.setRecord("terminal", "X");
		}else{
			super.setRecord("terminal", "O");
		}
		
		super.setRecord("reqData", sharedMap.getString("reqData"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("orgData", sharedMap.getString("orgData"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));

		logger.info("set TRX_WH : {}", super.insert());
		super.initRecord();
	}
	
	public void updateTrxWH(SharedMap<String, Object> sharedMap) {
		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("terminal", sharedMap.getString("terminal"));
		super.setRecord("retry", "Y");
		super.addWhere("vanTrxId", sharedMap.getString("vanTrxId"), eq);
		super.addWhere("trxType", sharedMap.getString("trxType"), eq);
		logger.info("UPDATE PG_TRX_WH : {}", super.update());
		super.initRecord();
	}
	
	public SharedMap<String,Object> getDBIssuer(String bin){
		SharedMap<String,Object> issuerMap = new SharedMap<String,Object>();
		if(CommonUtil.isNullOrSpace(bin)){
			return issuerMap;
		}
		
		String key = "PG_CODE_BIN_" + bin;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_CODE_BIN");
			super.setColumns("*");
			super.addWhere("bin", bin, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() == 0){
				issuerMap.put("bin", bin);
				issuerMap.put("issuer", "기타");
				issuerMap.put("acquirer", "기타");
				issuerMap.put("type", "신용");
				return issuerMap;
			}else{
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}
		}
		
	}
	
	public SharedMap<String, Object> getTmnIdByTrxId(String rootTrxId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("tmnId");
		super.addWhere("vanTrxId", rootTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		
		if(rset.getRow(0) == null){
			return null;
		}
		
		return rset.getRow(0);
		
	}
	
	public SharedMap<String, Object> getTmnId(String tmnId) {
		super.setTable("PG_MCHT_TMN");
		super.addWhere("tmnId", tmnId, eq);
		super.setColumns("tmnId");
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.getRow(0) == null){
			return null;
		}
		
		return rset.getRow(0);
		
	}


}
