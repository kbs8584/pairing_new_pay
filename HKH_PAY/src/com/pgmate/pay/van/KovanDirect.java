package com.pgmate.pay.van;

import java.net.URLEncoder;
import java.util.Base64.Encoder;

import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.comm.TcpSocket;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.kovan.ISP3D;
import com.pgmate.pay.bean.kovan.ISP3DRes;
import com.pgmate.pay.bean.kovan.ISPRefund;
import com.pgmate.pay.bean.kovan.KovanRefund;
import com.pgmate.pay.bean.kovan.KovanRegular;
import com.pgmate.pay.bean.kovan.KovanResponse;
import com.pgmate.pay.bean.kovan.MPI3D;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"all"})
public class KovanDirect implements Van {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.KovanDirect.class);
	private static String COMM_HOST = "203.231.12.200"; //"KOVANPAY";// 
	
	private static String KOVAN_ISP = "18304";
	private static String KOVAN_MPI = "18303";
	
	private static final int KOVAN_CONNECT_TIMEOUT 	= 5000;
	private static final int KOVAN_READ_TIMEOUT 	= 30000;
	
	private static final String ISP_COMPANYCD 	= "19046";
	
	private SharedMap<String,Object>  tmnVanMap = null;

	public KovanDirect(SharedMap<String, Object> tmnVanMap) {
		this.tmnVanMap = tmnVanMap;
		//this.COMM_HOST = System.getProperty("kovan.direct.comhost");
		//this.KOVAN_ISP = System.getProperty("kovan.direct.port.isp");
		//this.KOVAN_MPI = System.getProperty("kovan.direct.port.mpi");
	 }

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		sharedMap.put("van",tmnVanMap.getString("van"));
		sharedMap.put("vanId",tmnVanMap.getString("vanId"));
		
		if(sharedMap.isTrue("3ddirect")) {
			if(response.pay.metadata.isEquals("acsType", "KVP")) {
				return salesISP(trxDAO,sharedMap,response);
			}else {
				return salesMPI(trxDAO,sharedMap,response);
			}
		}else {
			return salesRegular(trxDAO,sharedMap,response);
		}
	}
	
	
	private SharedMap<String, Object> salesISP(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		SharedMap<String,Object> vtidMap = (SharedMap<String,Object>)sharedMap.get("vtidMap");
		ISP3D isp = new ISP3D();
		isp.amount 		= CommonUtil.toString(response.pay.amount);
		isp.currency 	= "410";
		isp.reqDate  	= sharedMap.getString(PAYUNIT.REG_DATE).substring(4);	//YYYYMMDDHHMMSS 중 YYMMMDDHHMMSS
		isp.trackId		= response.pay.trxId.substring(7);
		isp.installment = CommonUtil.zerofill(response.pay.card.installment,2);
		isp.companyCd 	= KovanDirect.ISP_COMPANYCD;
		isp.vpTrxId		= "";
		isp.pairingTmnId	= vtidMap.getString(vtidMap.getString("master"));
		//isp.mchtId 		= sharedMap.getString("pairingCid"); KOVAN 에서 넣지 말라함.
		isp.trxId   	= response.pay.trxId;
		//isp.cardCode	= response.pay.metadata.getString("cardCode");
		isp.cardCode	= response.pay.metadata.getString("cardCode").length() + "" + response.pay.metadata.getString("cardCode");
		logger.debug("CardCode: {}, {}", response.pay.metadata.getString("cardCode").length(), response.pay.metadata.getString("cardCode"));
		isp.sessionKey  = response.pay.metadata.getString("sessionKey");
		isp.encData		= response.pay.metadata.getString("encData");
		logger.debug("isp.sessionKey: {}", isp.sessionKey);
		logger.debug("isp.encData: {}", isp.encData);
		if(isp.sessionKey.length() > 15) {
			isp.sessionKey = URLEncoder.encode(isp.sessionKey);
			isp.encData = URLEncoder.encode(isp.encData);
		}
		logger.debug("isp.sessionKey: {}", isp.sessionKey);
		logger.debug("isp.encData: {}", isp.encData);
		isp.checkDigit	= "0";		//첫번재 부호
		if(response.pay.metadata.isEquals("noInt", "Y")) {	//무이자여부
			//isp.checkDigit	= isp.checkDigit+"1";		//무이자
			// 무이자 임시로 0 세팅
			logger.debug("무이자 거래: {}", response.pay.trxId);
			isp.checkDigit	= isp.checkDigit+"0";		//무이자
		}else {
			isp.checkDigit	= isp.checkDigit+"0";		//무이자
		}
		isp.checkDigit = isp.checkDigit+"0";			//가맹점 번호 체크 안함.
		isp.mchtBRN  	= sharedMap.getString("mchtBRN");
		isp.mchtUrl		= sharedMap.getString("shopDomain");
		isp.mchtIp		= sharedMap.getString("mchtIp");
		isp.mchtTrackId = response.pay.trxId;			// 상점정보 / 40 / 상점명 또는 PG에서 취급하는 상점고유번호(조회시 사용) 
		
		String item = "";
		try {
			if (response.pay.products != null && response.pay.products.size() > 0) {
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		} catch (Exception e) {
		}
		
		isp.product		= item;
		
		logger.info("PAIRING -> KOVAN {}: [{}]","ISP",isp.get());
		logger.debug("-->REQ DEBUG : \n{}",GsonUtil.toJson(isp, true, ""));
		
		TcpSocket tcp = new TcpSocket();
		tcp.setSocketProperty(KovanDirect.COMM_HOST, Integer.parseInt(KovanDirect.KOVAN_ISP), KovanDirect.KOVAN_CONNECT_TIMEOUT);
		byte[] res =null;
		ISP3DRes ispRes = null;
		try {
			res = tcp.sendRecv(isp.get().getBytes());
			logger.info("PAIRING <- KOVAN {}: [{}]","ISP",new String(res));
			ispRes = new ISP3DRes(res);
			logger.debug("<--RES DEBUG : \n{}",GsonUtil.toJson(ispRes, true, ""));
		}catch(Exception e) {
			ispRes = new ISP3DRes();
			ispRes.trxId = isp.trxId;
			ispRes.resultCd = "XXXX";
			ispRes.cardResultCd = "XXXX";
			logger.info("KOVAN ISP {} Connect Error : {}",sharedMap.getString(PAYUNIT.RUNTIME_ENV),e.getMessage());
		}
		
		if(ispRes.resultCd.equals("XXXX")) {
			response.result 	= ResultUtil.getResult("9999","승인실패",ispRes.resultCd+" VAN사 통신 오류");
			response.pay.authCd = "";
			sharedMap.put("vanTrxId",ispRes.vpTrxId);
			sharedMap.put("vanResultCd",ispRes.resultCd);
			sharedMap.put("vanResultMsg",ispRes.resultCd+" KOVAN 통신 오류");
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
		}else {
			if(ispRes.resultCd.equals("0000")) {
				response.result 	= ResultUtil.getResult("0000","정상","정상승인");
				response.pay.authCd = ispRes.authCd.substring(12);	// 카드사 승인번호만을 취해서 입력한다.
				sharedMap.put("vanTrxId",ispRes.authCd); // VAN 승인번호를 포함한 내역은 여기따로 저장한다. -> 승인 취소시 사용된다.
				sharedMap.put("vanResultCd","0000");
				sharedMap.put("vanResultMsg","정상승인");
				sharedMap.put("authCd",response.pay.authCd);
				sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
				//카드 LAST4 업데이트
				if(ispRes.cardNo.length() > 12) {
					trxDAO.updateLast4(response.pay.trxId,ispRes.cardNo.substring(12));
				}
			}else {
				String resultMsg = trxDAO.getCode("ISP",ispRes.cardResultCd);
				response.result 	= ResultUtil.getResult("9999","승인실패",ispRes.cardResultCd+":"+resultMsg);
				response.pay.authCd = "";
				sharedMap.put("vanTrxId",ispRes.vpTrxId);
				sharedMap.put("vanResultCd",ispRes.cardResultCd);
				sharedMap.put("vanResultMsg",resultMsg);
				sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
				
				//카드 LAST4 업데이트
				if(ispRes.cardNo.length() > 12) {
					trxDAO.updateLast4(response.pay.trxId,ispRes.cardNo.substring(12));
				}
				//카드코드에 따른 카드브랜드 또는 매입사 분류
				logger.info("VP CARDCODE : {}",ispRes.cardCd);
				logger.info("trxId : {},kovan code:{}, card code:{}",sharedMap.getString(PAYUNIT.TRX_ID),ispRes.resultCd,ispRes.cardResultCd);
			}
		}

		return sharedMap;
		
	}
	
	
	private SharedMap<String, Object> salesMPI(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		
		SharedMap<String,Object> vtidMap = (SharedMap<String,Object>)sharedMap.get("vtidMap");
		MPI3D mpi = new MPI3D();
		mpi.amount 			= CommonUtil.toString(response.pay.amount);
		mpi.currency 		= "410";
		mpi.reqDate  		= sharedMap.getString(PAYUNIT.REG_DATE).substring(4);	//YYYYMMDDHHMMSS 중 YYMMMDDHHMMSS
		mpi.trackId			= response.pay.trxId.substring(7);
		mpi.trxDay  		= sharedMap.getString(PAYUNIT.REG_DATE).substring(2,8);
		mpi.trxTime  		= sharedMap.getString(PAYUNIT.REG_DATE).substring(8);
		mpi.eci				= response.pay.metadata.getString("eci");
		mpi.companyCd 		= vtidMap.getString(vtidMap.getString("master")).substring(3, 8);
		if(response.pay.metadata.isEquals("acsType", "KMOTION")) {
			mpi.trackII 		= response.pay.card.number.substring(0,16) + "=8911" + CommonUtil.byteFiller("", 11) + response.pay.card.number.substring(16,21);
		} else {
			mpi.trackII 		= response.pay.card.number + "=" + "";
		}
		mpi.trxId   		= response.pay.trxId.substring(1);
		mpi.pairingTmnId		= vtidMap.getString(vtidMap.getString("master"));
		//mpi.pairingBRN 		= KovanDirect.PAIRING_BRN;	//"2138140742";

		mpi.mchtBRN  		= sharedMap.getString("mchtBRN");
		mpi.mchtUrl			= sharedMap.getString("shopDomain");
		mpi.mchtIp			= sharedMap.getString("mchtIp");
		mpi.installment		= CommonUtil.zerofill(response.pay.card.installment, 2);
		mpi.cavv			= response.pay.metadata.getString("cavv");
		mpi.xid				= response.pay.metadata.getString("xid");
		mpi.retry			= "00";
		logger.info("ECI : {}",response.pay.metadata.getString("eci"));  
		String req = "";
		if(response.pay.metadata.isTrue("cardAuth")){
			String authDob = response.pay.metadata.getString("authDob");
			if(authDob.length() == 7) {
				authDob = CommonUtil.zerofill("", 7)+authDob;
			}
			req = mpi.getPayWithAuth(authDob, response.pay.metadata.getString("authPw"), "");
		}else {
			req = mpi.getPay(true);
		}
		
		logger.info("PAIRING -> KOVAN {}: [{}]","MPI",req.substring(0,97) + "******" + req.substring(103, req.length()));
		
		TcpSocket tcp = new TcpSocket();
		tcp.setSocketProperty(KovanDirect.COMM_HOST, Integer.parseInt(KovanDirect.KOVAN_MPI), KovanDirect.KOVAN_CONNECT_TIMEOUT);

		byte[] res =null;
		KovanResponse mpiRes = null;
		try {
			res = tcp.sendRecv(req.getBytes());
			mpi.trackII = mpi.trackII.substring(0, 6) + mpi.trackII.substring(12, mpi.trackII.length());
			logger.info("-->REQ DEBUG : \n{}",GsonUtil.toJson(mpi, true, ""));

			String resLogging = new String(res);
			logger.info("PAIRING <- KOVAN {}: [{}]","MPI",resLogging.substring(0,92) + "******" + resLogging.substring(98, resLogging.length()));
			mpiRes = new KovanResponse(res);
			mpiRes.printFormat(resLogging);
		}catch(Exception e) {
			mpiRes = new KovanResponse();
			mpiRes.trxId = mpiRes.trxId;
			mpiRes.resultCd = "XXXX";
			mpiRes.cardResultCd = "XXXX";
			logger.info("KOVAN MPI {} Connect Error : {}",sharedMap.getString(PAYUNIT.RUNTIME_ENV),e.getMessage());
		}
		
		if(mpiRes.resultCd.equals("XXXX")){
			response.result 	= ResultUtil.getResult("XXXX","승인오류","VAN사 통신장애");
			sharedMap.put("vanTrxId","");
			sharedMap.put("vanResultCd","XXXX");
			sharedMap.put("vanResultMsg","KOVAN 통신 장애");	
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+mpi.reqDate);
		}else if(mpiRes.resultCd.equals("0000")) {
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			response.pay.authCd = mpiRes.authCd;
			if(response.pay.authCd != null && response.pay.authCd.length() > 8) {
				response.pay.authCd = response.pay.authCd.substring(0,8);
				logger.info("OVERFLOW AUTH CODE: {} -> {}", mpiRes.authCd, response.pay.authCd);
			} 
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd",response.pay.authCd);
			sharedMap.put("vanDate","20"+mpiRes.trxDay+mpiRes.trxTime);
			sharedMap.put("issuer",trxDAO.getCode("KV_ISSUER",mpiRes.issuerCd));
			logger.info("ISSUER: {},{}",mpiRes.issuerCd,sharedMap.getString("issuer"));
		}else {
			String resultMsg = trxDAO.getCode("KOVAN",mpiRes.resultCd);
			response.result 	= ResultUtil.getResult("9999","승인실패",mpiRes.resultCd+":"+resultMsg);
			response.pay.authCd = "";
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd",mpiRes.resultCd);
			sharedMap.put("vanResultMsg",resultMsg);
			sharedMap.put("vanDate","20"+mpiRes.trxDay+mpiRes.trxTime);
			logger.info("trxId : {},kovan code:{}, card code:{}",sharedMap.getString(PAYUNIT.TRX_ID),mpiRes.resultCd,mpiRes.cardResultCd);
		}
	
		return sharedMap;
		
	}
	
	
	private SharedMap<String, Object> salesRegular(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		
		SharedMap<String,Object> vtidMap = (SharedMap<String,Object>)sharedMap.get("vtidMap");
		KovanRegular kv = new KovanRegular();
		

		kv.amount 		= CommonUtil.toString(response.pay.amount);
		kv.reqDate  	= sharedMap.getString(PAYUNIT.REG_DATE).substring(4);	//YYYYMMDDHHMMSS 중 MMDDHHMMSS
		kv.trackId		= response.pay.trxId.substring(7);
		kv.trxDay  		= sharedMap.getString(PAYUNIT.REG_DATE).substring(2,8);
		kv.trxTime  	= sharedMap.getString(PAYUNIT.REG_DATE).substring(8);
		kv.eci			= "";
		kv.companyCd 	= vtidMap.getString(vtidMap.getString("master")).substring(3, 8);
		kv.trackII 		= response.pay.metadata.getString("number") + "=" + "";
		kv.trxId   		= response.pay.trxId.substring(1);
		kv.mchtId 		= "";
		kv.pairingTmnId	= vtidMap.getString(vtidMap.getString("master"));
		//kv.pairingBRN 	= KovanDirect.PAIRING_BRN;	//"2138140742";
		kv.currency 	= "410";
		kv.mchtBRN  	= sharedMap.getString("mchtBRN");
		kv.mchtUrl		= sharedMap.getString(PAYUNIT.HOST);
		kv.mchtIp		= sharedMap.getString(PAYUNIT.REMOTEIP);
		kv.installment	= CommonUtil.zerofill(response.pay.card.installment,2);
		if(response.pay.metadata.isTrue("cardAuth")){
			kv.authDob = response.pay.metadata.getString("authDob");
			kv.authPw  = response.pay.metadata.getString("authPw");
		}
		
		
		logger.info("PAIRING -> KOVAN {}: [{}]","REG",kv.getPayWithAuth());
		//logger.info("-->REQ DEBUG : \n{}",GsonUtil.toJson(kv, true, ""));
		
		TcpSocket tcp = new TcpSocket();
		tcp.setSocketProperty(KovanDirect.COMM_HOST, Integer.parseInt(KovanDirect.KOVAN_MPI), KovanDirect.KOVAN_CONNECT_TIMEOUT);
		byte[] res =null;
		KovanResponse mpiRes = null;
		try {
			res = tcp.sendRecv(kv.getPayWithAuth().getBytes());
			logger.info("PAIRING <- KOVAN {}: [{}]","REG",new String(res));
			mpiRes = new KovanResponse(res);
			logger.debug("<--RES DEBUG : \n{}",GsonUtil.toJson(mpiRes, true, ""));
		}catch(Exception e) {
			mpiRes = new KovanResponse();
			mpiRes.trxId = mpiRes.trxId;
			mpiRes.resultCd = "XXXX";
			mpiRes.cardResultCd = "XXXX";
			logger.info("KOVAN MPI {} Connect Error : {}",sharedMap.getString(PAYUNIT.RUNTIME_ENV),e.getMessage());
		}
		
		if(mpiRes.resultCd.equals("XXXX")){
			response.result 	= ResultUtil.getResult("XXXX","승인오류","VAN사 통신장애");
			sharedMap.put("vanTrxId","");
			sharedMap.put("vanResultCd","XXXX");
			sharedMap.put("vanResultMsg","KOVAN 통신 장애");	
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+kv.reqDate);
		}else if(mpiRes.resultCd.equals("0000")) {
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			response.pay.authCd = mpiRes.authCd;	//여기 부분 애매함.
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd",response.pay.authCd);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yy")+mpiRes.trxDay+mpiRes.trxTime);
			sharedMap.put("issuer",trxDAO.getCode("KV_ISSUER",mpiRes.issuerCd));
			logger.info("ISSUER: {},{}",mpiRes.issuerCd,sharedMap.getString("issuer"));
		}else {
			String resultMsg = trxDAO.getCode("KOVAN",mpiRes.resultCd);
			response.result 	= ResultUtil.getResult("9999","승인실패",mpiRes.resultCd+":"+resultMsg);
			response.pay.authCd = "";
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd",mpiRes.resultCd);
			sharedMap.put("vanResultMsg",resultMsg);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yy")+mpiRes.trxDay+mpiRes.trxTime);
			logger.info("trxId : {},kovan code:{}, card code:{}",sharedMap.getString(PAYUNIT.TRX_ID),mpiRes.resultCd,mpiRes.cardResultCd);
		}
		
		
		return sharedMap;
		
	}
	
	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> trxMap,Response response) {
		sharedMap.put("van",tmnVanMap.getString("van"));
		sharedMap.put("vanId",tmnVanMap.getString("vanId"));
		SharedMap<String, Object> resMap = new SharedMap<String, Object>();
		
		SharedMap<String,Object> vtidMap = trxDAO.getPayTypeByVTID(trxMap.getString("pairingVtid"));
		String acsType = trxDAO.getAcsResByTrxId(trxMap.getString("trxId")).getString("acsType");
		if(vtidMap.getString("payType").indexOf("3D") > -1 && acsType.equals("KVP")) {
			resMap = refundISP(trxDAO, sharedMap, trxMap, response, vtidMap.getString("KOVAN"));
			
		}else {
			resMap = refundMPI(trxDAO, sharedMap, trxMap, response,vtidMap.getString("KOVAN"));
		}
		
		checkFailCode(resMap, response, trxDAO, trxMap.getString("trxId"));
		return resMap;
	}
	
	
	private SharedMap<String, Object> refundISP(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> trxMap,Response response,String pairingTmnId) {
		ISPRefund isp = new ISPRefund();
		isp.amount		= CommonUtil.toString(response.refund.amount);
		isp.currency	= "410";
		isp.reqDate		= sharedMap.getString(PAYUNIT.REG_DATE).substring(4);
		isp.trackId		= response.refund.trxId.substring(7);
		isp.installment = trxMap.getString("installment");
		isp.netRefund  	= "1";
		isp.companyCd 	= KovanDirect.ISP_COMPANYCD;
		isp.rootTrxId	= trxMap.getString("trxId");
		isp.vpTrxId		= "";
		isp.rootTrxDay	= trxMap.getString("regDay").substring(2);
		
		isp.pairingTmnId	= pairingTmnId;
		//isp.mchtId 		= sharedMap.getString("pairingCid"); KOVAN 에서 체크하지 않음.
		isp.authCd  	= sharedMap.getString("vanTrxId");
		
		logger.info("PAIRING -> KOVAN {}: [{}]","ISP",isp.get());
		logger.debug("-->REQ DEBUG : \n{}",GsonUtil.toJson(isp, true, ""));
		isp.logSend(isp.get());
		TcpSocket tcp = new TcpSocket();
		tcp.setSocketProperty(KovanDirect.COMM_HOST, Integer.parseInt(KovanDirect.KOVAN_ISP), KovanDirect.KOVAN_CONNECT_TIMEOUT);
		
		byte[] res =null;
		ISPRefund ispRes = null;
		try {
			res = tcp.sendRecv(isp.get().getBytes());
			logger.info("PAIRING <- KOVAN {}: [{}]","ISP",new String(res));
			ispRes = new ISPRefund(res);
			logger.debug("<--RES DEBUG : \n{}",GsonUtil.toJson(ispRes, true, ""));
		}catch(Exception e) {
			ispRes = new ISPRefund();
			ispRes.rootTrxId = isp.rootTrxId;
			ispRes.resultCd = "XXXX";
			logger.info("KOVAN ISP Refund {} Connect Error : {}",sharedMap.getString(PAYUNIT.RUNTIME_ENV),e.getMessage());
		}
		
		
		if(ispRes.resultCd.equals("XXXX")) {
			response.result 	= ResultUtil.getResult("9999","취소실패",ispRes.resultCd+" VAN사 통신 오류");
			sharedMap.put("vanTrxId",ispRes.vpTrxId);
			sharedMap.put("vanResultCd","XXXX");
			sharedMap.put("vanResultMsg",ispRes.resultCd+" KOVAN 통신 오류");
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
		}else if(ispRes.resultCd.equals("0000")) {
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = ispRes.authCd.substring(12);	
			sharedMap.put("vanTrxId",ispRes.vpTrxId);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상취소");
			sharedMap.put("authCd",response.refund.authCd);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
		}else{
			String resultMsg = trxDAO.getCode("KOVAN",ispRes.resultCd);
			response.result 	= ResultUtil.getResult("9999","승인실패",ispRes.resultCd+":"+ resultMsg);
			sharedMap.put("vanTrxId",ispRes.vpTrxId);
			sharedMap.put("vanResultCd",ispRes.resultCd);
			sharedMap.put("vanResultMsg",resultMsg);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+ispRes.reqDate);
			logger.info("trxId : {},kovan code:{}",sharedMap.getString(PAYUNIT.TRX_ID),ispRes.resultCd);
		}
		
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		return sharedMap;
	}
	
	private SharedMap<String, Object> refundMPI(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> trxMap,Response response,String pairingTmnId) {
		KovanRefund kv = new KovanRefund();
		kv.amount 		= CommonUtil.toString(response.refund.amount);
		kv.reqDate  	= sharedMap.getString(PAYUNIT.REG_DATE).substring(4);	//YYYYMMDDHHMMSS 중 YYMMMDDHHMMSS
		kv.trackId		= response.refund.trxId.substring(7);
		kv.trxDay  		= sharedMap.getString(PAYUNIT.REG_DATE).substring(2,8);
		kv.trxTime  	= sharedMap.getString(PAYUNIT.REG_DATE).substring(8);

		kv.companyCd 	= pairingTmnId.substring(3, 8);
		
		String cardBox = trxDAO.getByCardId(trxMap.getString("cardId")).getString("value");
		cardBox = new String(SeedKisa.decrypt(Base64.decode(cardBox), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
		cardBox = CommonUtil.nToB(cardBox).trim();
		Card card = (Card)GsonUtil.fromJson(cardBox,new Card());
		
		kv.trackII 		= card.number + "=" + card.expiry;
		kv.trxId   	= response.refund.trxId.substring(1);
		kv.pairingTmnId	= pairingTmnId;
		kv.currency 	= "410";
		kv.installment	= trxMap.getString("installment");
		kv.authCd  	= trxMap.getString("authCd");
		//kv.mchtBRN	= sharedMap.getString("mchtBRN");

		kv.route = "01";

		String req = kv.getRefund();
		logger.info("PAIRING -> KOVAN {}: [{}]","MPI", req.substring(0,97) + "******" + req.substring(103, req.length()));

		TcpSocket tcp = new TcpSocket();
		tcp.setSocketProperty(KovanDirect.COMM_HOST, Integer.parseInt(KovanDirect.KOVAN_MPI), KovanDirect.KOVAN_CONNECT_TIMEOUT);

		byte[] res =null;
		KovanResponse mpiRes = null;
		try {
			res = tcp.sendRecv(kv.getRefund().getBytes());
			//logger.info("PAIRING <- KOVAN {}: [{}]","REG",new String(res));
			String resLogging = new String(res);
			logger.info("PAIRING <- KOVAN {}: [{}]","MPI",resLogging.substring(0,94) + "******" + resLogging.substring(100, resLogging.length()));

			mpiRes = new KovanResponse(res);
			logger.debug("<--RES DEBUG : \n{}",GsonUtil.toJson(mpiRes, true, ""));
		}catch(Exception e) {
			mpiRes = new KovanResponse();
			mpiRes.trxId = mpiRes.trxId;
			mpiRes.resultCd = "XXXX";
			mpiRes.cardResultCd = "XXXX";
			logger.info("KOVAN REG {} Connect Error : {}",sharedMap.getString(PAYUNIT.RUNTIME_ENV),e.getMessage());
		}
		
		
		if(mpiRes.resultCd.equals("XXXX")){
			response.result 	= ResultUtil.getResult("XXXX","실패","VAN사 통신장애");
			sharedMap.put("vanTrxId","");
			sharedMap.put("vanResultCd","XXXX");
			sharedMap.put("vanResultMsg","KOVAN 통신 장애");	
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yyyy")+kv.reqDate);
		}else if(mpiRes.resultCd.equals("0000")) {
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = mpiRes.authCd;
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상취소");
			sharedMap.put("authCd",response.refund.authCd);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yy")+mpiRes.trxDay+mpiRes.trxTime);
		}else {
			String resultMsg = trxDAO.getCode("KOVAN",mpiRes.resultCd);
			response.result 	= ResultUtil.getResult("9999","취소실패",mpiRes.resultCd+":"+resultMsg);
			response.refund.authCd = mpiRes.authCd;
			sharedMap.put("vanTrxId",mpiRes.trackId);
			sharedMap.put("vanResultCd",mpiRes.resultCd);
			sharedMap.put("vanResultMsg",resultMsg);
			sharedMap.put("vanDate",CommonUtil.getCurrentDate("yy")+mpiRes.trxDay+mpiRes.trxTime);
			logger.info("trxId : {},kovan code:{}, card code:{}",sharedMap.getString(PAYUNIT.TRX_ID),mpiRes.resultCd,mpiRes.cardResultCd);
		}
		
		return sharedMap;
	}
	
	private void checkFailCode(SharedMap<String, Object> resMap, Response response, TrxDAO trxDAO, String trxId) {
		if(resMap.isEquals("vanResultCd", "0000")) {
			return;
		}
		/* 매입취소 대상 거래 */
		if(resMap.isEquals("vanResultCd", "XXXX")) {
			logger.debug("통신장애로 인한 실패는 매입취소 요청 대상이 아님");
		} else if(resMap.isEquals("vanResultCd", "0153")) {
			response.result 	= ResultUtil.getResult("0000","정상","매입취소");
		} else if(resMap.isEquals("vanResultCd", "0022") || resMap.isEquals("vanResultCd", "9036")) {
			response.result 	= ResultUtil.getResult("0000","정상","매입취소(ISP)");
		} else if(trxDAO.isCaptured3D(trxId)) {
			response.result 	= ResultUtil.getResult("0000","정상","매입취소(기타)");
		}
	}
	
	

  
}
