package com.pgmate.pay.proc;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.main.C3Runner;
import com.pgmate.pay.util.CardUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Allat;
import com.pgmate.pay.van.AllatAuth;
import com.pgmate.pay.van.Danal;
import com.pgmate.pay.van.DanalAuth;
import com.pgmate.pay.van.Daou;
import com.pgmate.pay.van.DemoVan;
import com.pgmate.pay.van.EmptyVan;
import com.pgmate.pay.van.Fiserv;
import com.pgmate.pay.van.KovanDirect;
import com.pgmate.pay.van.Kspay;
import com.pgmate.pay.van.Nice;
import com.pgmate.pay.van.Pairing;
import com.pgmate.pay.van.Smartro;
import com.pgmate.pay.van.SmartroAuth;
import com.pgmate.pay.van.Spc;
import com.pgmate.pay.van.Van;
import com.pgmate.pay.van.Welcome;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPay extends Proc {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay.class );
	
	//private SharedMap<String,Object> agencyMngMap	= null;
	//private SharedMap<String,Object> distMngMap		= null;
	//private SharedMap<String,Object> vtidMap				= null;
	
	public ProcPay() {
		
	}

	@Override
	public synchronized void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
		logger.info("========== ========== ========== ========== ========== exec() - BEGIN ");
		//set(rc, request, sharedMap, sharedObject);
		// 2022-08-25 - set 로직 추가 - BEGIN
		logger.info("========== ========== ========== ========== ========== exec() - set2() - BEGIN ");
		SharedMap<String, Object> sales_mchtMap = sharedObject.get("mcht");
		SharedMap<String, Object> sales_mchtTmnMap = sharedObject.get("mchtTmn");
		SharedMap<String, Object> sales_mchtMngMap = sharedObject.get("mchtMng");

		//logger.info("sharedObject - sales_mchtMap : " + sales_mchtMap.toJson());
		//logger.info("sharedObject - sales_mchtTmnMap : " + sales_mchtTmnMap.toJson());
		//logger.info("sharedObject - sales_mchtMngMap : " + sales_mchtMngMap.toJson());
		
		this.response = new Response();
		this.trxDAO	= new TrxDAO();
		logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtMap mchtId : " + sales_mchtMap.getString("mchtId"));
		if (sales_mchtTmnMap.get("van").equals("SMARTRO")) {
			sharedMap.put("van", "SMARTRO");
		}
		BigDecimal rate = new BigDecimal(sales_mchtMngMap.getString("rate"));
		BigDecimal data = new BigDecimal("0");
		rate.stripTrailingZeros().toPlainString();		
		int compareRate = rate.compareTo(data); 
		if (compareRate < 0) {
			response.result = ResultUtil.getResult("9999", "설정오류", "가맹점 정산 정보 미설정 오류");
			return;
		} else if (compareRate == 0) {
		} else if (compareRate > 0) {
		}
		//2018.03.16 WIDGET 반영과 함께 적용 
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_GET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_WIDGET )
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_ECHO ) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_3D_WIDGET )
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_STATUS)
				|| sharedMap.getString(PAYUNIT.URI).startsWith("/api/comp/pay")){			
		} else {
			if (request == null) {
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.");
				return;
			}
		}
		if (!sales_mchtTmnMap.isEquals("status", "사용")) {
			if (!sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_REFUND)) {
				response.result = ResultUtil.getResult("9999", "설정오류", "사용가능한 터미널이 아닙니다.");
			}
		}
		
		if (sales_mchtTmnMap.getLong("vanIdx") == 0) {
			response.result = ResultUtil.getResult("9999", "설정오류", "라우팅을 찾을 수 없습니다.");
		}
		valid2(request, sharedMap, sales_mchtMap, sales_mchtTmnMap, sales_mchtMngMap);
		logger.info("========== ========== ========== ========== ========== exec() - set2() - END ");
		// 2022-08-25 - set 로직 추가 - END
		
		response.pay = request.pay;
	/*	
		if (response.result != null) {
			// 2022-04-06 중복결제 막기
			if (!response.result.resultCd.equals("0000")) {
				logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | 중복결제 막기 처리");
				String res = GsonUtil.toJsonExcludeStrategies(response,true);
				logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | res : " + res);
				logger.info("========== exec() - res : " + res);
				VertXMessage.set200(rc, res);
				return;
			}
		}
	*/	
		logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		trxDAO.insertTrxREQ(sharedMap, response);
		
		if (response.result != null) {
			trxDAO.insertTrxRES(sharedMap, response);
			setResponse2(rc, sharedMap);
			return;
		}
		
		//SharedMap<String, Object> tmnVanMap = trxDAO.getMchtTmnByVanIdx(mchtTmnMap.getLong("vanIdx"), mchtMap.getString("mchtId"));

		// 2022-12-14
		// SharedMap<String, Object> tmnVanMap = trxDAO.getMchtTmnByVanIdx(sales_mchtTmnMap.getLong("vanIdx"), sales_mchtMap.getString("mchtId"));
		logger.info("");
		SharedMap<String, Object> tmnVanMap = new SharedMap<String, Object>();
		logger.info("========== exec() - 카드별 PG상점ID관리 조회");
		//SharedMap<String, Object> vanBrandMap = new TrxDAO().getVanByBinNSemiAuth(response.pay.card.bin, sales_mchtTmnMap.getString("semiAuth"));
		SharedMap<String, Object> vanBrandMap = new TrxDAO().getVanByBin(response.pay.card.bin);
		if (vanBrandMap != null) {
			logger.info("========== exec() - vanBrandMap : " + vanBrandMap.toJson());
			tmnVanMap = vanBrandMap;
		} else {
			logger.info("========== valid2() - vanBrandMap NULL");
			tmnVanMap = trxDAO.getMchtTmnByVanIdx(sales_mchtTmnMap.getLong("vanIdx"), sales_mchtMap.getString("mchtId"));
		}
		logger.info("");
		if (tmnVanMap == null) {
			tmnVanMap = trxDAO.getMchtTmnByVanIdx2(sales_mchtTmnMap.getLong("vanIdx"), sales_mchtMap.getString("mchtId"));
		}
		
		if (request.pay.card.number.equals("4242424242424242")) {
			sales_mchtTmnMap.put("van", "DEMO");
		}
		Van van = null;
		//logger.info("VIA VAN : [{}]", mchtTmnMap.getString("van"));
		if (sales_mchtTmnMap.isEquals("van", "DEMO")) {
			van = new DemoVan(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "DANAL")) {
			van = new Danal(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "DANALAUTH")) {
			van = new DanalAuth(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "NICE")) {
			van = new Nice(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "DAOU")) {
			van = new Daou(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "KSPAY")) {
			van = new Kspay(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "PAIRING") || sales_mchtTmnMap.startsWith("van", "KWON")
				|| sales_mchtTmnMap.startsWith("van", "TPO") || sales_mchtTmnMap.startsWith("van", "E2U")
				|| sales_mchtTmnMap.startsWith("van", "PAYBOT") || sales_mchtTmnMap.startsWith("van", "UREX")
			 	|| sales_mchtTmnMap.startsWith("van", "UREX")) 
			{
			van = new Pairing(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "ALLAT")) {
			van = new Allat(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "FDIKPAY")) {
			van = new Fiserv(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "SMARTROAUTH")) {
			logger.info("========== exec() - van = new SMARTROAUTH ");
			sharedMap.put("vanId", request.pay.metadata.get("vanId"));
			sharedMap.put("vanTrxId", request.pay.metadata.get("vanTrxId"));
			sharedMap.put("authCd", request.pay.metadata.get("authCd"));
			sharedMap.put("vanDate", request.pay.metadata.get("vanDate"));
			van = new SmartroAuth(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "SMARTRO")) {
			
			if (sales_mchtTmnMap.startsWith("semiAuth", "A")) {
				logger.info("========== exec() - SMARTRO - 인증결제 O");
				sharedMap.put("vanId", request.pay.metadata.get("vanId"));
				sharedMap.put("vanTrxId", request.pay.metadata.get("vanTrxId"));
				sharedMap.put("authCd", request.pay.metadata.get("authCd"));
				sharedMap.put("vanDate", request.pay.metadata.get("vanDate"));
				van = new SmartroAuth(tmnVanMap);
			} else {
				logger.info("========== exec() - SMARTRO - 인증결제 X");
				van = new Smartro(tmnVanMap);	
			}
			//van = new Smartro(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "PAIRINGSOLUTION")) {
			if (sharedMap.isEquals("pairingRouteVan", "KOVAN")) {
				van = new KovanDirect(tmnVanMap);
			}
			if (sharedMap.isEquals("pairingRouteVan", "KSNET")) {
				// van = new kspayDirect(tmnVanMap);
			}
		} else if (sales_mchtTmnMap.startsWith("van", "SPC")) {
			van = new Spc(tmnVanMap);
		} else if (sales_mchtTmnMap.startsWith("van", "WELCOME")) {
			logger.info("========== exec() - van : " + sales_mchtTmnMap.get("van"));
			//van = new Welcome(tmnVanMap);
			if (!CommonUtil.isNullOrSpace(sales_mchtTmnMap.getString("semiAuth"))) {
				if (sales_mchtTmnMap.getString("semiAuth").equals("A")) {
					logger.info("========== exec() - 1 van : " + sales_mchtTmnMap.get("van") + " | check semiAuth : " + sales_mchtTmnMap.get("semiAuth") + " | vanId : " + tmnVanMap.getString("vanId"));
					logger.info("========== exec() - 2 WELCOME - 인증결제 O");
					sharedMap.put("van", tmnVanMap.getString("van"));
					sharedMap.put("vanId", tmnVanMap.getString("vanId").trim());
					sharedMap.put("cryptoKey", tmnVanMap.getString("cryptoKey").trim());
					sharedMap.put("secondKey", tmnVanMap.getString("secondKey").trim());
					van = new AllatAuth(tmnVanMap);
					/*
					logger.info("========== exec() - 1 van : " + sales_mchtTmnMap.get("van") + " | check semiAuth : " + sales_mchtTmnMap.get("semiAuth"));
					logger.info("========== exec() - 2 van -> vanId checking");
					if (!CommonUtil.isNullOrSpace(tmnVanMap.getString("vanId"))) {
						if (tmnVanMap.startsWith("vanId", "welcome")) {
							logger.info("========== exec() - 3 van : " + sales_mchtTmnMap.get("van") + " | check semiAuth : " + sales_mchtTmnMap.get("semiAuth") + " | vanId : " + tmnVanMap.getString("vanId"));
							logger.info("========== exec() - 4 WELCOME - 인증결제 O");
							van = new AllatAuth(tmnVanMap);
						} else {
							logger.info("========== exec() - 5 van -> vanId IS NULL");
							logger.info("========== exec() - 6 WELCOME - 인증결제 X");
							sharedMap.put("van", tmnVanMap.getString("van"));
							sharedMap.put("vanId", tmnVanMap.getString("vanId").trim());
							sharedMap.put("cryptoKey", tmnVanMap.getString("cryptoKey").trim());
							sharedMap.put("secondKey", tmnVanMap.getString("secondKey").trim());
							van = new Welcome(tmnVanMap);
						}
					} else {
						logger.info("========== exec() - 7 van -> vanId IS NULL");
						logger.info("========== exec() - 8 WELCOME - 인증결제 X");
						sharedMap.put("van", tmnVanMap.getString("van"));
						sharedMap.put("vanId", tmnVanMap.getString("vanId").trim());
						sharedMap.put("cryptoKey", tmnVanMap.getString("cryptoKey").trim());
						sharedMap.put("secondKey", tmnVanMap.getString("secondKey").trim());
						van = new Welcome(tmnVanMap);
					}
					*/
				} else {
					logger.info("========== exec() - 9 van : " + sales_mchtTmnMap.get("van") + " | check semiAuth : " + sales_mchtTmnMap.get("semiAuth"));
					logger.info("========== exec() - 10 WELCOME - 인증결제 X");
					sharedMap.put("van", tmnVanMap.getString("van"));
					sharedMap.put("vanId", tmnVanMap.getString("vanId").trim());
					sharedMap.put("cryptoKey", tmnVanMap.getString("cryptoKey").trim());
					sharedMap.put("secondKey", tmnVanMap.getString("secondKey").trim());
					van = new Welcome(tmnVanMap);
				}
			} else {
				logger.info("========== exec() - 11 van : " + sales_mchtTmnMap.get("van") + " | check semiAuth IS NULL");
				logger.info("========== exec() - 12 WELCOME - 인증결제 X");
				sharedMap.put("van", tmnVanMap.getString("van"));
				sharedMap.put("vanId", tmnVanMap.getString("vanId").trim());
				sharedMap.put("cryptoKey", tmnVanMap.getString("cryptoKey").trim());
				sharedMap.put("secondKey", tmnVanMap.getString("secondKey").trim());
				van = new Welcome(tmnVanMap);
			}
		} else {
			sales_mchtTmnMap.put("van", "");
			van = new EmptyVan(tmnVanMap);
			logger.info("========== exec() - van : [" + tmnVanMap.getString("van") + "]");
		}
		
		sharedMap = van.sales(trxDAO, sharedMap, response);
		
		//20181212 시간은 상위사 응답 시간으로 변경 
	//	logger.info("========== vanDate : " + sharedMap.getString("vanDate"));
		if (sharedMap.getString("vanDate").length() == 14) {
			response.pay.trxDate = sharedMap.getString("vanDate");
		}
		
		// 20190705 recurring set : 정기과금SET거래
		//logger.info("========== before sharedMap.isEquals(recurring, set) - response | issuer : " + response.pay.card.issuer + " acquirer : " + response.pay.card.acquirer + " || cardAcquirer : " + sharedMap.getString("cardAcquirer"));
		if (sharedMap.isEquals("recurring", "set")) {
			if (CommonUtil.isNullOrSpace(response.pay.card.number)) {
				response.pay.card.acquirer = sharedMap.getString("cardAcquirer");
				response.pay.card.number = cardMask(response.pay.card.number);
				trxDAO.insertKsnetCard(sharedMap,response.pay);
			}
		}
		
		// 2022-10-31 업체요청으로 카드코드테이블 데이터로 전달 처리 - 추가
		if (C3Runner.cardcodeList.size() == 0) { 
			TrxDAO trxDao = new TrxDAO();
			C3Runner.cardcodeList = trxDao.selectCardCodeList();
		}
		logger.info("========== sharedMap issuer : " + sharedMap.getString("issuer"));
		logger.info("========== sharedMap cardAcquirer : " + sharedMap.getString("cardAcquirer"));
		logger.info("========== response issuer : " + response.pay.card.issuer);
		logger.info("========== response acquirer : " + response.pay.card.acquirer);
		if (!"".equals(response.pay.card.acquirer)) {
			if (C3Runner.cardcodeList.size() > 0) { 
				int acquirerChk = 0;
				//logger.info("========== C3Runner.cardcodeList : " + C3Runner.cardcodeList.toString());
				for (String key : C3Runner.cardcodeList.keySet()) {
				//	System.out.println("key : " + key);
					SharedMap<String, Object> data_card = C3Runner.cardcodeList.get(key);
					String code = data_card.getString("code");
					String cname = data_card.getString("cname");
					String calias = data_card.getString("calias");
					int compareChk = 0;
					compareChk = cname.compareTo(response.pay.card.acquirer);
					if (compareChk > 1) {
						logger.info("========== ");
						logger.info("========== 비교결과 : " + compareChk);
						logger.info("========== 리턴 카드사명 : [" + response.pay.card.acquirer + "]");
						logger.info("========== 디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
						
						response.pay.card.issuer = calias + "카드";
						response.pay.card.acquirer = calias;
					} else if (compareChk == 0) {
						logger.info("========== ");
						logger.info("========== 비교결과 : " + compareChk);
						logger.info("========== 리턴 카드사명 : [" + response.pay.card.acquirer + "]");
						logger.info("========== 디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
						
						response.pay.card.issuer = calias + "카드";
						response.pay.card.acquirer = calias;
						break;
					} else if (compareChk < 0) {
					//	System.out.println("카드사명 결과 : " + compareChk);
						logger.info("========== 비교결과 : " + compareChk);
						logger.info("========== issuer : " + response.pay.card.issuer);
						logger.info("========== acquirer : " + response.pay.card.acquirer);
					}
				}
			} else {
				logger.info("========== 비교x : ");
				logger.info("========== issuer : " + response.pay.card.issuer);
				logger.info("========== acquirer : " + response.pay.card.acquirer);
			}
		} else {
			logger.info("========== 비교x : ");
			logger.info("========== issuer : " + response.pay.card.issuer);
			logger.info("========== acquirer : " + response.pay.card.acquirer);
		}
		
		//logger.info("========== after sharedMap.isEquals(recurring, set) - response | issuer : " + response.pay.card.issuer + " acquirer : " + response.pay.card.acquirer + " || cardAcquirer : " + sharedMap.getString("cardAcquirer"));
		//카드사 코드, 매입사 코드 
		//logger.info("========== before sharedMap.isNullOrSpace(cardAcquirer) -  response | issuer : " + response.pay.card.issuer + " acquirer : " + response.pay.card.acquirer + " || cardAcquirer : " + sharedMap.getString("cardAcquirer"));
		if (CommonUtil.isNullOrSpace(response.pay.card.number)) {
			if (!sharedMap.isNullOrSpace("cardAcquirer")) {
				response.pay.card.acquirer = sharedMap.getString("cardAcquirer");
				trxDAO.updateTrxREQACQ(response.pay.trxId, sharedMap.getString("cardAcquirer"));
			}
		}
		//logger.info("========== after sharedMap.isNullOrSpace(cardAcquirer) -  response | issuer : " + response.pay.card.issuer + " acquirer : " + response.pay.card.acquirer + " || cardAcquirer : " + sharedMap.getString("cardAcquirer"));
		
		//logger.info("========== BEFORE insertTrxRES() - van : " + sharedMap.getString("van") + " | vanId : " + sharedMap.getString("vanId"));
		trxDAO.insertTrxRES(sharedMap, response);
		
		if (response.result.resultCd.equals("0000")) {
			if (!CommonUtil.isNullOrSpace(request.pay.webhookUrl)) {
				new ThreadWebHook(request.pay.webhookUrl, response).start();
			}
			
			//20190827 이메일 전송 추가
			try {
				if (mchtMngMap.isEquals("notiEmail", "사용") && !CommonUtil.isNullOrSpace(response.pay.payerEmail)) {
					SharedMap<String, Object> emailMap = new SharedMap<String, Object>();
					emailMap.put("trxId", response.pay.trxId);
					emailMap.put("tmnId", mchtTmnMap.getString("tmnId"));
					emailMap.put("mchtId", sales_mchtMap.getString("mchtId"));
					emailMap.put("trackId", response.pay.trackId);
					emailMap.put("trxType", "CARDPAY");
					emailMap.put("payerEmail", response.pay.payerEmail);
					trxDAO.insertTrxNtsEmail(emailMap);
				}
			} catch (Exception e) {
				
			}
		} else {
			//20200206 FDS 실패 로직 추가
			SharedMap<String,Object> fdsMap = new SharedMap<String,Object>();
			fdsMap.put("trxId",sharedMap.getString("trxId"));
			fdsMap.put("tmnId",sharedMap.getString("tmnId"));
			fdsMap.put("van",sharedMap.getString("van"));
			fdsMap.put("vanResultCd",sharedMap.getString("vanResultCd"));
			fdsMap.put("vanResultMsg",sharedMap.getString("vanResultMsg"));
			fdsMap.put("hash",response.pay.card.hash);
			fdsMap.put("bin",response.pay.card.bin);
			fdsMap.put("last4",response.pay.card.last4);
			
			new ThreadFDS(trxDAO,fdsMap).start();
		}
		logger.info("========== LAST issuer : " + response.pay.card.issuer);
		logger.info("========== LAST acquirer : " + response.pay.card.acquirer);
		//setResponse();
		setResponse2(rc, sharedMap);
		logger.info("========== ========== ========== ========== ========== exec() - END ");
		return;
	}

	@Override
	public synchronized void valid() {
		logger.info("========== ========== ========== ========== ========== valid() - BEGIN ");
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - request.pay.mchtId : " + request.pay.mchtId + " | mchtTmnMap mchtId : " + mchtTmnMap.getString("mchtId"));
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - mchtTmnMap - vanIdx : " + mchtTmnMap.getString("vanIdx"));
		SharedMap<String, Object> vanMap = new TrxDAO().getVanByVanIdx2(mchtTmnMap.getString("vanIdx"));
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - vanMap - vanId  : " + vanMap.getString("vanId"));
		// 2022-05-16 mchtId 조회후 넣기
		if (request.pay.mchtId == null) {
			request.pay.mchtId = mchtTmnMap.getString("mchtId");
		}
		
		if (request.pay == null || request.pay.card == null) {
			response.result = ResultUtil.getResult("9999", "필수값없음", "결제정보 및 카드 정보가 없습니다.");
			return;
		}
		
		request.pay.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		request.pay.trxDate = sharedMap.getString(PAYUNIT.REG_DATE);

		if (CommonUtil.isNullOrSpace(request.pay.tmnId)) {
			request.pay.tmnId = sharedMap.getString("tmnId");
		} else {
			request.pay.tmnId = request.pay.tmnId.trim();
		}
		
		// 2022-08-23 - CARD BIN Number를 이용한 필터링
		String card_number = request.pay.card.number;
		String card_bin = card_number.substring(0, 6);
		String card_last4 = card_number.substring(card_number.length() - 4, card_number.length());
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - card_number : " + card_number);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - card_bin : " + card_bin);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - card_last4 : " + card_last4);
		/*
		int binBlockListCount_mchtId = trxDAO.getBinlockListCount(request.pay.mchtId, mchtTmnMap.getString("vanId"), card_bin);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - binBlockListCount_mchtId : " + binBlockListCount_mchtId);
		if (binBlockListCount_mchtId > 0) {
			response.result = ResultUtil.getResult("9999", "승인거절", "해당 카드는 가맹점 승인 제한된 카드입니다.");
			return;
		}
		*/
		
		int binBlockListCount_mchtId = trxDAO.getBinlockListCount(request.pay.mchtId, card_bin);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - binBlockListCount_mchtId : " + binBlockListCount_mchtId);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - request.pay.mchtId : " + request.pay.mchtId);
		if (binBlockListCount_mchtId > 0) {
			response.result = ResultUtil.getResult("9999", "승인거절", "해당 카드는 가맹점 승인 제한된 카드입니다.");
			return;
		}
		int binBlockListCount_vanId = trxDAO.getBinlockListCount(vanMap.getString("vanId"), card_bin);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - binBlockListCount_vanId : " + binBlockListCount_vanId);
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - vanMap vanId : " + vanMap.getString("vanId"));
		if (binBlockListCount_vanId > 0) {
			response.result = ResultUtil.getResult("9999", "승인거절", "해당 카드는 PG 승인 제한된 카드입니다.");
			return;
		}
		
		// 2022-08-01 - 영업사원 정보 조회
		SharedMap<String, Object> salesMap = new SharedMap<String, Object>();
		SharedMap<String, Object> salesMngMap = new SharedMap<String, Object>();
		if (CommonUtil.isNullOrSpace(request.pay.salesId)) {
			logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== salesId IS NULL");
		} else {
			logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - request.pay.salesId ::: " + request.pay.salesId);
			salesMap = trxDAO.getSalesByIdChk(request.pay.salesId);
			if (salesMap == null) {
				response.result = ResultUtil.getResult("9999", "영업사원결제", "영업사원 아이디가 존재하지 않습니다.");
				return;
			} else {
				if (CommonUtil.isNullOrSpace(salesMap.getString("salesId"))) {
					logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - salesMap salesId = empty ");
					logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() END");
				//	logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - 영업사원 아이디가 존재하지 않습니다. : salesId : " + request.pay.salesId);
					response.result = ResultUtil.getResult("9999", "영업사원결제", "영업사원 아이디가 존재하지 않습니다.");
					return;
				} else {
					if (CommonUtil.isNullOrSpace(salesMap.getString("parentId"))) {
						logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - salesMap parentId = empty ");
						logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() END");
					//	logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - 영업사원 아이디가 존재하지 않습니다. : salesId : " + request.pay.salesId);
						response.result = ResultUtil.getResult("9999", "영업사원결제", "가맹점 소속이 아닙니다.");
						return;
					} else {
						salesMngMap = trxDAO.getSalesByIdChk(request.pay.salesId);
						if (!request.pay.mchtId.equals(salesMap.getString("parentId"))) {
							logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - request.pay.mchtId : " + request.pay.mchtId + " | salesMap parentId : " + salesMap.getString("parentId"));
							logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() END");
						//	logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== valid() - 영업사원 아이디가 존재하지 않습니다. : salesId : " + request.pay.salesId);
							response.result = ResultUtil.getResult("9999", "영업사원결제", "가맹점과 영업사원의 가맹점이 다릅니다.");
							return;
						}
					}
				}
			}
		}
		
		// 2022-08-02 - 판매자(가맹점) 전화번호, 구매자 전화번호 동일시 차단처리
		logger.info("========== ========== ========== ========== ========== request.pay.payerTel : [" + request.pay.payerTel + "]");
		if (CommonUtil.isNullOrSpace(request.pay.payerTel)) {
			
		} else {
			SharedMap<String, Object> mchtMap2 = trxDAO.getMchtByMchtId2(mchtMap.getString("mchtId"));
			
			String payerTel = request.pay.payerTel;
			payerTel = payerTel.replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel);
			
			String tel1 = mchtMap2.getString("tel1").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | tel1 : " + tel1);
			if (payerTel.equals(tel1)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			String tel2 = mchtMap2.getString("tel2").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | tel2 : " + tel2);
			if (payerTel.equals(tel2)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			
			String ceoPhone = mchtMap2.getString("ceoPhone").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | ceoPhone : " + ceoPhone);			
			if (payerTel.equals(ceoPhone)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			String ceoTel = mchtMap2.getString("ceoTel").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | ceoTel : " + ceoTel);			
			if (payerTel.equals(ceoTel)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			/*
			String fax = mchtInstall.getString("fax").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | fax : " + fax);
			if (payerTel.equals(fax)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			String managerPhone = mchtInstall.getString("managerPhone").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | managerPhone : " + managerPhone);
			if (payerTel.equals(managerPhone)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			*/	
		}

		if (CommonUtil.isNullOrSpace(request.pay.trackId)) {
			response.result = ResultUtil.getResult("9999", "필수값없음", "가맹점 주문번호가 입력되지 않았습니다.");
			return;
		}
		//if (trxDAO.isDuplicatedTrackId(sharedMap.getString(PAYUNIT.MCHTID), request.pay.trackId)) {
		//2022-05-16 mchtId 전역변수 변경처리
		if (trxDAO.isDuplicatedTrackId(request.pay.mchtId, request.pay.trackId)) {
			response.result = ResultUtil.getResult("9999", "중복된 거래번호입니다.", "해당 거래번호로 승인/승인취소시는 재 사용할 수 없습니다.");
			return;
		}

		if (request.pay.amount < 1000) {
			response.result = ResultUtil.getResult("9999", "결제 최소 금액 오류", "1000원 미만은 결제를 허용하지 않습니다.");
			return;
		}

		//trxDAO.insertTrxIO(sharedMap, request.pay);
		// 2022-05-16 테스트
		if (!trxDAO.insertTrxIOchk(sharedMap, request.pay)) {
			response.result = ResultUtil.getResult("9999", "동시결제오류", "동시결제가 존재합니다. 결제를 다시 시도해주시기 바랍니다.");
			return;
		}
		
		//20190710 과세 유형에 따른 부가세 처리 
		sharedMap.put("taxType",mchtTmnMap.getString("taxType"));
		
		// recurring pay : 정기과금통한거래 20190603
		if (request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "pay")) {
			//SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
			//2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);

			if (!mchtSvcMap.isEquals("recurring", "사용")) {
				response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금서비스가 신청되지 않았습니다.");
				return;
			} else {
				// cardId 로 거래정보를 가져온다.
				//SharedMap<String, Object> cardKspayMap = trxDAO.getByKsnetCardId(request.pay.card.cardId, sharedMap.getString(PAYUNIT.MCHTID));
				//2022-05-16 mchtId 전역변수 변경처리
				SharedMap<String, Object> cardKspayMap = trxDAO.getByKsnetCardId(request.pay.card.cardId, request.pay.mchtId);
				if (cardKspayMap.isNullOrSpace("authKey")) {
					response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금 정보를 찾을 수 없습니다.");
					return;
				} else {
					sharedMap.put("authKey", cardKspayMap.getString("authKey"));
					sharedMap.put("recurring", "pay");
					logger.info("recurring pay, authKey : {}", cardKspayMap.getString("authKey"));
				}
				// 아래 검증을 통과하기위하여 임시로 SET
				request.pay.card.number = cardKspayMap.getString("unit");
				request.pay.card.expiry = cardKspayMap.getString("expiry");
			}
		}

		// 20190705 recurring set : 정기과금SET거래
		if (request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "set")) {
			//SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
			// 2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);
			if (!mchtSvcMap.isEquals("recurring", "사용")) {
				response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금서비스가 신청되지 않았습니다.");
				return;
			} else {
				if (request.pay.metadata.getString("authPw").length() != 2) {
					response.result = ResultUtil.getResult("9999", "필수값없음", "카드비밀번호 앞 2자리 필수 입력 : authPw");
					return;
				}
				if (request.pay.metadata.getString("authDob").length() == 6
						|| request.pay.metadata.getString("authDob").length() == 10) {
					// 생년월일 6자리 또는 사업자번호 10자리
				} else {
					response.result = ResultUtil.getResult("9999", "필수값없음", "생년월일 또는 사업자 번호 필수입니다. : authPw");
					return;
				}

				//recurring 관련 값 SET
				sharedMap.put("authPw",request.pay.metadata.getString("authPw"));
				sharedMap.put("authDob",request.pay.metadata.getString("authDob"));
				sharedMap.put("recurring","set");
			}
		}
		
		// 20190604 recurring 거래시 중복된 cardId 가 SET 되지 않도록 기존 아이디 사용
		String cardId = "";
		if (sharedMap.isEquals("recurring", "pay")) {
			//sharedMap.put(PAYUNIT.KEY_CARD, request.pay.card.cardId);
			cardId = request.pay.card.cardId;
		} else {
			//sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
			cardId = GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID));
		}

//		sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
		// 2022-05-16 상품아이디 전역변수 제거
		String prodId = GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID));
		
		if (request.pay.card.encTrackI.equals("")) {
			int cardLength = request.pay.card.number.length();
			if (cardLength < 14 || 16 < cardLength) {
				//response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.", "카드번호는 14~16자리만 허용합니다.");
				logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== 카드번호는 14~16자리만 허용합니다. - valid() END");
				//return;
			}
/*
			if (mchtTmnMap.getInt("apiMaxInstall") < request.pay.card.installment) {
				response.result = ResultUtil.getResult("9999", "할부제공기간을 초과하였습니다.",
						"최대 " + CommonUtil.zerofill(mchtTmnMap.getInt("apiMaxInstall"), 2) + "개월 가능합니다.");
				return;
			}
*/
			if (mchtMngMap.getInt("maxInstall")< request.pay.card.installment) {
				response.result = ResultUtil.getResult("9999", "할부제공기간을 초과하였습니다.",
						"최대 " + CommonUtil.zerofill(mchtMngMap.getInt("maxInstall"), 2) + "개월 가능합니다.");
				return;
			}

			// 20190228 삼성페이 적용을 위한 유효기간처리 변경
			if (request.pay.card.expiry.length() == 4 || request.pay.card.expiry.length() >= 18) {
				if (request.pay.card.expiry.length() >= 18) {
					logger.info("삼성페이거래");
				}
			} else {
				logger.info("========== ProcPay - sharedMap van : " + sharedMap.get("van") + " | mchtTmnMap van : " + mchtTmnMap.get("van"));
				if (sharedMap.get("van") != null) {
					if (sharedMap.get("van").equals("SMARTRO") || mchtTmnMap.get("van").equals("SMARTRO")) {
						
					} else if (sharedMap.get("van").equals("WELCOME") || mchtTmnMap.get("van").equals("WELCOME")) {
						
					} else {
						response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "YYMM 포맷이 아닙니다.");
						return;
					}
				}
			}
			if (request.pay.card.expiry.length() == 4) {
				if (CommonUtil.parseInt(request.pay.card.expiry.substring(0, 2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))) {
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "유효년수가 경과된 카드입니다.");
					return;
				}
				if (CommonUtil.parseInt(request.pay.card.expiry.substring(2, 4)) > 12) {
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "유효월 입력이 잘못되었습니다.");
					return;
				}
			}
			if (CommonUtil.isNullOrSpace(request.pay.card.number)) {
				
			} else {
				request.pay.card.last4 = request.pay.card.number.substring(cardLength-4, cardLength);
				request.pay.card.bin   = request.pay.card.number.substring(0,6);
			}

		} else if (request.pay.card.encTrackI.startsWith("acs") && request.pay.card.encTrackI.length() == 20) { // 20190805 인증결제
			
//			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
//			SharedMap<String, Object> mchtMng3DMap = trxDAO.getMchtMng3D(sharedMap.getString(PAYUNIT.MCHTID));
			// 2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);
			SharedMap<String, Object> mchtMng3DMap = trxDAO.getMchtMng3D(request.pay.mchtId);
//			if (!mchtSvcMap.isEquals("card3D", "사용") || !mchtMng3DMap.isEquals("mchtId", sharedMap.getString(PAYUNIT.MCHTID))) {
			if (!mchtSvcMap.isEquals("card3D", "사용") || !mchtMng3DMap.isEquals("mchtId", request.pay.mchtId)) {
				response.result = ResultUtil.getResult("9999", "인증결제오류", "인증결제서비스를 신청하지 않은 가맹점입니다.");
				return;
			}
			
			SharedMap<String, Object> acsResMap = trxDAO.getAcsRes(request.pay.card.encTrackI.trim());
			
			if (acsResMap == null || !acsResMap.isEquals("result", "0000")) {
				response.result = ResultUtil.getResult("9999", "인증결제정보없음", "ISP,MPI 인증 확인 정보가 없습니다.");
				return;
			}
			
			if (request.pay.metadata == null) {
				request.pay.metadata = new SharedMap<String, String>();
			}
			
			request.pay.metadata.put("acsType", acsResMap.getString("acsType"));
			request.pay.metadata.put("noInt", acsResMap.getString("noInt"));
			// 20191024 yhbae 변경
			String cardNo = "";
			if (!acsResMap.getString("cardNo").isEmpty()) {
				cardNo = SeedKisa.decryptAsString(Base64.decode(acsResMap.getString("cardNo")), ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16));
			}
			request.pay.card.number = cardNo;
			request.pay.card.bin = acsResMap.getString("bin");
			int cardLength = request.pay.card.number.length();
			if (cardLength > 0) {
				request.pay.card.last4 = request.pay.card.number.substring(cardLength-4, cardLength);
			}
			request.pay.card.installment = acsResMap.getInt("installment");
			logger.debug("INSTALLMENT: {}", request.pay.card.installment);
			// 20190917 ACS DEMO 분기 추가 
			if (acsResMap.isEquals("acsType", "DEMO")) {
				
			} else {
				if (acsResMap.isEquals("acsType", "KVP")) {
					request.pay.metadata.put("sessionKey", acsResMap.getString("sessionKey"));
					request.pay.metadata.put("encData", acsResMap.getString("encData"));
					request.pay.metadata.put("cardCode", acsResMap.getString("cardCode"));
				} else {
					request.pay.metadata.put("xid", acsResMap.getString("xid"));
					if (acsResMap.isEquals("acsType", "KMOTION")) {
						request.pay.metadata.put("cavv", acsResMap.getString("cardCode"));
						request.pay.metadata.put("eci", "  ");
					} else {
						request.pay.metadata.put("cavv", acsResMap.getString("cavv"));
						request.pay.metadata.put("eci", acsResMap.getString("eci"));
					}
				}
				/* Promotion Type을 포인트/무이자 사용에 따라 변경되도록 추가해야 한다. */
				if (acsResMap.isNullOrSpace("acqId")) {
					SharedMap<String, Object> acsReqMap = trxDAO.getAcsReq(request.pay.card.encTrackI.trim());
					String acqId = trxDAO.getAcqIdByIssuerId(acsReqMap.getString("issuer"));
					logger.debug("GET ACQ ID: {}", acqId);
					acsResMap.put("acqId", acqId);
				}
				logger.info("3D Grade: {}", mchtMng3DMap.getString("mchtGrade"));

				SharedMap<String,Object> vtidMap = new SharedMap<String,Object>(); 
				vtidMap = getDirectVTID(mchtMng3DMap, acsResMap);
				
				//vtidMap = trxDAO.getDirectVTID(mchtTmnMap.getString("vanIdx"), mchtMng3DMap.getString("mchtGrade"), acsResMap.getString("acqId"));
				if (vtidMap == null || vtidMap.isEquals("vtid", "")) {
					response.result = ResultUtil.getResult("9999", "가맹점 설정정보 없음","인증결제에 대한 설정정보가 없습니다. 관리자에 문의하시기 바랍니다.");
					return;
				}

				sharedMap.put("3ddirect", "true");
				sharedMap.put("vtidMap", vtidMap);
				sharedMap.put("pairingVtid", vtidMap.getString("vtid"));
				sharedMap.put("pairingRouteVan", vtidMap.getString("master"));
				sharedMap.put("pairingCid", vtidMap.getString("cid"));
				String mchtBRN = trxDAO.getAESDec(mchtMap.getString("identity"));
				sharedMap.put("mchtBRN", mchtBRN);
				sharedMap.put("shopDomain", mchtMngMap.getString("shopDomain"));
				sharedMap.put("mchtIp", ""); //TODO: 가맹점의 IP 설정
			}

			//3DDirect 의 경우 PG_WEBPAY_REQ, PG_ACS_RES 를 업데이트한다.
			trxDAO.update3DDirect(acsResMap.getString("payId"),acsResMap.getString("acsId"), sharedMap.getString(PAYUNIT.TRX_ID));
			
			logger.info("========== 3ddirect metadata =\n{}",GsonUtil.toJson(request.pay.metadata));
		}
		if (request.pay.card.bin.length() == 6) {
			SharedMap<String, Object> issuerMap = trxDAO.getDBIssuer(request.pay.card.bin);
			if (issuerMap != null) {
				request.pay.card.cardType = issuerMap.getString("type");
				request.pay.card.issuer = issuerMap.getString("issuer");
				request.pay.card.acquirer = issuerMap.getString("acquirer");
				request.pay.card.hash = CardUtil.hash(request.pay.card.number);
			}
		}
		
		//request.pay.card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);
		request.pay.card.cardId = cardId;
		
		String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(request.pay.card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
		
		if (!sharedMap.isEquals("recurring", "pay")) { // 20190604 추가 저장하지 않도록 수정
			//trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
			trxDAO.insertCard(cardId, encrypted);
		}
		sharedMap.put("CARD_INSERTED", true);//카드정보가 이미 등록되었는지 여부
		
		if (request.pay.products != null) {
			if (request.pay.products.size() > 0) {
				Product p = new Product();
				for (int i = 0; i < request.pay.products.size(); i++) {
					request.pay.products.get(0).prodId = prodId;
				}
			}
			
			// 다빈치 거래건에 대해서
			if (sharedMap.startsWith(PAYUNIT.MCHTID, "DVC") || sharedMap.startsWith(PAYUNIT.MCHTID, "onoff")) {
				if (!CommonUtil.isNullOrSpace(request.pay.udf1)) {
					Product p = new Product();
					p.name = request.pay.udf1;
					request.pay.products.add(p);
				}
			}
			//trxDAO.insertProduct(sharedMap.getString(PAYUNIT.KEY_PROD), request.pay.products, sharedMap.getString(PAYUNIT.REG_DATE));
			// 2022-05-16 전역변수 제거
			trxDAO.insertProduct(prodId, request.pay.products, sharedMap.getString(PAYUNIT.REG_DATE));
		}
		
		//semiAuth 즉 생년월일/카드비번2자리 꼭 사용하는 가맹점 2017-08-01 
		logger.info("========== semiAuth 체크 ::: " + mchtTmnMap.getString("semiAuth"));
		if (mchtTmnMap.getString("semiAuth").equals("Y")) {
			if (request.pay.metadata != null) {
				request.pay.metadata.put("cardAuth", "true");
				/* 법인 사용자의 경우 비밀번호가 없어도 된다.
				if(request.pay.metadata.getString("authPw").length() !=2){
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authPw");return;
				}*/
				if (request.pay.metadata.getString("authDob").length() == 6
						|| request.pay.metadata.getString("authDob").length() == 10) {
					//생년월일 6자리 또는 사업자번호 10자리 
				} else {
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authDob");
					return;
				}
				
			} else {
				response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : pay.metadata");
				return;
			}
		} else if (mchtTmnMap.getString("semiAuth").equals("A")) {
			logger.info("========== semiAuth 인증결제 가맹점 {}", mchtTmnMap.getString("semiAuth"));
		} else if (mchtTmnMap.getString("semiAuth").equals("A")) {
			logger.info("========== semiAuth 인증결제 가맹점 {}", mchtTmnMap.getString("semiAuth"));
		} else {
			logger.info("========== semiAuth 미사용 가맹점 {}", mchtTmnMap.getString("semiAuth"));
		}
		sharedMap.put("semiAuth", mchtTmnMap.getString("semiAuth"));
		
		
		//할부개월 적용 2018-04-10
		/*
		logger.info("mcht install : {}, pay installment : {}",mchtTmnMap.getInt("apiMaxInstall"),request.pay.card.installment);
		if (mchtTmnMap.getInt("apiMaxInstall") < request.pay.card.installment) {
			logger.debug("가맹점 할부개월 초과 ");
			String msg = "";
			if (mchtTmnMap.getInt("apiMaxInstall") == 0 || mchtTmnMap.getInt("apiMaxInstall") == 1) {
				msg ="일시불로만 ";
			} else {
				msg =mchtTmnMap.getInt("apiMaxInstall")+"개월 이하로 ";
			}
			response.result = ResultUtil.getResult("9999", "할부개월초과","할부기간은 "+msg+" 이용하여 주시기 바랍니다.");return;
		}
		*/
		for (String str : mchtTmnMap.keySet()) {
			logger.info("========== ========== mchtTmnMap str : " + str + "\t" + mchtTmnMap.get(str));
		}
		logger.info("");

		SharedMap<String, Object> mchtInstall = trxDAO.getMchtInstall(mchtMngMap.getString("mchtId"));
		SharedMap<String, Object> vanInstall = trxDAO.getVanInstall(mchtMngMap.getString("mchtId"), mchtTmnMap.getInt("vanIdx"));
		logger.info("========== ========== " + request.pay.card.acquirer + " 카드 할부: " + request.pay.card.installment);	
		
		// 2021-11-22 카드사별 할부기간 적용
		if (mchtInstall == null || vanInstall == null) {
			response.result = ResultUtil.getResult("9999", "가맹점 웹결제 미설정", "웹결제 미설정 가맹점입니다.");
			return;
		}
		
		if (request.pay.card.acquirer.equals("비씨")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall01"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall01"));	
			if (mchtInstall.getInt("cardInstall01") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall01") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall01") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall01") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall01") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall01") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall01") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall01") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("국민")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall02"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall02"));
			if (mchtInstall.getInt("cardInstall02") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall02") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall02") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall02") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall02") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall02") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall02") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall02") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("삼성")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall03"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall03"));
			if (mchtInstall.getInt("cardInstall03") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall03") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall03") == 1) {
					msg ="일시불로만 ";
				} else {
					msg =mchtInstall.getInt("cardInstall03")+"개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall03") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall03") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall03") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall03") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("외환")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall04"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall04"));
			if (mchtInstall.getInt("cardInstall04") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall04") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall04") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall04") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall04") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall04") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall04") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall04") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("신한")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall05"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall05"));
			if (mchtInstall.getInt("cardInstall05") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall05") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall05") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall05") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall05") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall05") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall05") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall05") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("농협")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall06"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall06"));
			if (mchtInstall.getInt("cardInstall06") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절","농협 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall06") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall06") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall06") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall06") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall06") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall06") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall06") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("롯데")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall07"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall07"));
			if (mchtInstall.getInt("cardInstall07") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall07") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall07") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall07") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall07") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall07") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall07") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall07") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("현대")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall08"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall08"));
			if (mchtInstall.getInt("cardInstall08") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall08") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall08") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall08") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall08") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall08") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall08") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall08") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		if (request.pay.card.acquirer.equals("하나")) {
			logger.info("========== ========== mcht maxInstall : \t" + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall09"));
			logger.info("========== ========== van maxInstall :\t" + vanInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall09"));
			if (mchtInstall.getInt("cardInstall09") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); return;
			}
			if (mchtInstall.getInt("cardInstall09") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall09") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall09") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
			
			if (vanInstall.getInt("cardInstall09") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다."); return;
			}
			if (vanInstall.getInt("cardInstall09") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (vanInstall.getInt("cardInstall09") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = vanInstall.getInt("cardInstall09") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다."); return;
			}
		}
		

		if ((request.pay.card.cardType.equals("체크") || request.pay.card.cardType.equals("선불")) && request.pay.card.installment > 0) {
			response.result = ResultUtil.getResult("9999", "할부불가카드","선불/체크카드는 할부거래가 불가합니다.");return;
		}

		//지불중지 가맹점
		if (mchtMngMap.isEquals("payStatus", "중지")) {
			logger.debug("결제 중지 가맹점 payStatus : {},{}", mchtMngMap.getString("payStatus"));
			response.result = ResultUtil.getResult("9999", "승인거절", "승인중지된 가맹점입니다.");
			return;
		}
		
		//사용중지 가맹점
		if (!mchtMap.isEquals("status", "사용")) {
			logger.debug("결제 중지 가맹점 status : {},{}", mchtMap.getString("status"));
			response.result = ResultUtil.getResult("9999", "승인거절", "승인중지된 가맹점입니다");
			return;
		}
		
		
		//단말기 한도 관리 단말기 한도가 있으면 단말기 한도 따라서 20200219  , 터미널 1회 한도 사용 가맹점은 가맹점 설정 정보 갱신 처리
		if(mchtTmnMap.getLong("limitOnce") > 0){
			if(mchtTmnMap.getDouble("limitOnce") < request.pay.amount ) {
				logger.debug("터미널 1회 한도초과 : {},{}",mchtTmnMap.getDouble("limitOnce"),request.pay.amount);
				// 하단 한도거래설정으로 인한 주석처리
				//response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과(터미널)");return;
			}else {
				//mchtMngMap = trxDAO.getMchtMngByMchtIdImmediate(sharedMap.getString(PAYUNIT.MCHTID));
				//2022-05-16 mchtId 전역변수 변경처리
				mchtMngMap = trxDAO.getMchtMngByMchtIdImmediate(request.pay.mchtId);
				logger.debug("가맹점 한도 설정 정보 재 갱신");
			}
		}else {
			//가맹점 한도 측정
			if(mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.pay.amount ){
				logger.debug("가맹점 1회 한도초과 : {},{}",mchtMngMap.getDouble("limitOnce"),request.pay.amount);
				// 하단 한도거래설정으로 인한 주석처리
				//response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
			}
		}
		
		//20201124 터미널 한도 처리
		SharedMap<String,Object> trxSumMap = trxDAO.getTrxTmnDaySum(mchtTmnMap);
		logger.debug("========== ========== tmnDailySum : {},{}", trxSumMap.getDouble("tmnDailySum"), trxSumMap.getLong("tmnDailySum"));
		
		if (mchtTmnMap.getLong("limitDay") > 0 && mchtTmnMap.getDouble("limitDay") < trxSumMap.getDouble("tmnDailySum") + request.pay.amount ) {
			logger.debug("터미널 일 한도초과 : {},{}",mchtTmnMap.getDouble("limitDay"),request.pay.amount);
			
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일 거래한도 초과(터미널)");return;
		}
		
		
		trxSumMap = trxDAO.getTrxTmnMonthSum(mchtTmnMap);
		logger.debug("limitMonth : {},{}", trxSumMap.getDouble("tmnMonthSum"), trxSumMap.getLong("tmnDailySum"));
		if(mchtTmnMap.getLong("limitMonth") > 0 && mchtTmnMap.getDouble("limitMonth") < trxSumMap.getDouble("tmnMonthSum") + request.pay.amount ) {
			logger.debug("터미널 월 한도초과 : {},{}",mchtTmnMap.getDouble("limitMonth"),request.pay.amount);
			
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과(터미널)");return;
		}
		
		trxSumMap = trxDAO.getTrxTmnYearSum(mchtTmnMap);
		if(mchtTmnMap.getLong("limitYear") > 0 && mchtTmnMap.getDouble("limitYear") < trxSumMap.getDouble("tmnYearSum") + request.pay.amount ) {
			logger.debug("터미널 년 한도초과 : {},{}",mchtTmnMap.getDouble("limitYear"), request.pay.amount);
			
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 년 거래한도 초과(터미널)");return;
		}
		
	
		//가맹점/지사/총판 한도조회 20190828 가맹점 한도만 가져도오록 수정 
		//trxSumMap = trxDAO.getTrxSum2(mchtMap);
		trxSumMap = trxDAO.getTrxSum(mchtMap);
		logger.info("========== ========== limitDay :  " + mchtMngMap.get("limitDay") + " SUM : " + trxSumMap.get("mchtDailySum"));
		logger.info("========== ========== limitMonth :  " + mchtMngMap.get("limitMonth") + " SUM : " + trxSumMap.get("mchtMonthlySum"));
		logger.info("========== ========== limitYear :  " + mchtMngMap.get("limitYear") + " SUM : " + trxSumMap.get("mchtYearSum"));
/*
		logger.info("mcht sum daily :{}, monthly: {}",trxSumMap.getDouble("mchtDailySum"),trxSumMap.getDouble("mchtMonthlySum"));
		//가맹점 한도 조회
		if (mchtMngMap.getDouble("limitDay") > 0
				&& mchtMngMap.getDouble("limitDay") < trxSumMap.getDouble("mchtDailySum") + request.pay.amount) {
			logger.debug("가맹점 일일 한도초과 : {},{}", mchtMngMap.getDouble("limitDay"),
					trxSumMap.getDouble("mchtDailySum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 일일 거래한도 초과");
			return;
		}
		if (mchtMngMap.getDouble("limitMonth") > 0
				&& mchtMngMap.getDouble("limitMonth") < trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount) {
			logger.debug("가맹점 월 한도초과 : {},{}", mchtMngMap.getDouble("limitMonth"),
					trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월 거래한도 초과");
			return;
		}
		if (mchtMngMap.getDouble("limitYear") > 0
				&& mchtMngMap.getDouble("limitYear") < trxSumMap.getDouble("mchtYearSum") + request.pay.amount) {
			logger.debug("가맹점 년 한도초과 : {},{}", mchtMngMap.getDouble("limitYear"),
					trxSumMap.getDouble("mchtlimitYearSum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 년 거래한도 초과");
			return;
		}
*/
		// 2021-11-24 적용여부에 따른 한도설정
		//가맹점 한도 조회
		logger.info("========== ========== 가맹점 건한도 적용여부 : " + mchtInstall.getString("limitOncePermit"));
		logger.info("========== ========== 가맹점 건한도 : " + mchtInstall.getDouble("limitOnce"));
		if (mchtInstall.getString("limitOncePermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitOncePermit").equals("Y")) {
			//한도초과
			if (mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.pay.amount) {
				logger.debug("가맹점 건 한도초과 : {},{}", mchtMngMap.getDouble("limitOnce"), request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점건한도초과");
				return;
			}
		} else if (mchtInstall.getString("limitOncePermit").equals("H")) {
			//정산보류
		}
		logger.info("========== ========== 가맹점 일한도 적용여부 : " + mchtInstall.getString("limitDayPermit"));
		logger.info("========== ========== 가맹점 일한도 : " + mchtInstall.getDouble("limitDay"));
		if (mchtInstall.getString("limitDayPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitDayPermit").equals("Y")) {
			//한도초과
			if (mchtMngMap.getDouble("limitDay") > 0 && mchtMngMap.getDouble("limitDay") < trxSumMap.getDouble("mchtDailySum") + request.pay.amount) {
				logger.debug("가맹점 일일 한도초과 : {},{}", mchtMngMap.getDouble("limitDay"), trxSumMap.getDouble("mchtDailySum") + request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점일한도초과");
				return;
			}
		} else if (mchtInstall.getString("limitDayPermit").equals("H")) {
			//정산보류
		}
		logger.info("========== ========== 가맹점 월한도 적용여부 : " + mchtInstall.getString("limitMonthPermit"));
		logger.info("========== ========== 가맹점 일한도 : " + mchtInstall.getDouble("limitMonth"));
		if (mchtInstall.getString("limitMonthPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitMonthPermit").equals("Y")) {
			//한도초과
			if (mchtMngMap.getDouble("limitMonth") > 0 && mchtMngMap.getDouble("limitMonth") < trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount) {
				logger.debug("가맹점 월 한도초과 : {},{}", mchtMngMap.getDouble("limitMonth"), trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점월한도초과");
				return;
			}
		} else if (mchtInstall.getString("limitMonthPermit").equals("H")) {
			//정산보류
		}
		logger.info("========== ========== 가맹점 년한도 적용여부 : " + mchtInstall.getString("limitYearPermit"));
		logger.info("========== ========== 가맹점 년한도 : " + mchtInstall.getDouble("limitYear"));
		if (mchtInstall.getString("limitYearPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitYearPermit").equals("Y")) {
			//한도초과
			if (mchtMngMap.getDouble("limitYear") > 0 && mchtMngMap.getDouble("limitYear") < trxSumMap.getDouble("mchtYearSum") + request.pay.amount) {
				logger.debug("가맹점 년 한도초과 : {},{}", mchtMngMap.getDouble("limitYear"), trxSumMap.getDouble("mchtYearSum") + request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점년한도초과");
				return;
			}
		} else if (mchtInstall.getString("limitYearPermit").equals("H")) {
			//정산보류
		}
		
		
		// 거래정지 카드 목록 SELECT 
/*
		SharedMap<String,Object> blMap = trxDAO.selectTrxBl(request.pay.card.hash, sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		if (blMap != null) {
			if (blMap.getLong("idx") > 0) {
				logger.info("B/L Block : {},{},{},{}",blMap.getLong("idx"),blMap.getString("masked"),blMap.getString("trxId"),blMap.getString("activeDay"));
				response.result = ResultUtil.getResult("9999", "서비스불가카드","서비스불가카드입니다. B/L 등록카드 관리자에 문의하시기 바랍니다.");return;
			}
		}
*/
	/*	
		//1일 카드 한도
		if(mchtMngMap.getLong("cardLimit") > 0 ){
			
			if(request.pay.card.bin.isEmpty() || request.pay.card.last4.isEmpty()) { // 20191024 yhbae 추가
				logger.debug("EMPTY BIN or Last4: {}", sharedMap.getString("trxId"));
			} else {
				long sumByCard = trxDAO.sumByCard(mchtMngMap.getString("mchtId"), request.pay.card.bin, request.pay.card.last4);
				if( mchtMngMap.getDouble("cardLimit") < request.pay.amount + sumByCard  ){
					logger.debug("한카드 1일거래 한도초과 : {},{}",mchtMngMap.getLong("cardLimit"),(sumByCard+request.pay.amount));
					response.result = ResultUtil.getResult("9999", "한도초과","한카드 1일 거래 한도 초과");return;
				}
			}
		}
	*/	
		
		//2020-01-22
		try {//
			if (mchtMap.isEquals("mchtId", "mono360") && request.pay.amount >= 1000000) {
				if (request.pay.card.acquirer.equals("롯데") && !request.pay.card.cardType.equals("체크")) {
					logger.debug("mono360 거래제한 요청 100만원이상 롯데 신용인경우 ");
					response.result = ResultUtil.getResult("9999", "고액롯데카드거래제한","고액 롯데 카드 거래제한");return;
				}
			}
		} catch (Exception e) {
		}
		
		
		//2020-03-23 특정 터미널 카드사별 한도 차단 
		try {
			if (CommonUtil.parseLong(sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8)) > 20200323 && !CommonUtil.isNullOrSpace(request.pay.card.acquirer)) {
			//	logger.info("========== tmnId : [{}], acquirer=[{}]",mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
				long riskAmount = trxDAO.getHighRiskTmn(mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
			//	logger.info("========== tmnId : [{}], riskAmount=[{}]",mchtTmnMap.getString("tmnId"),riskAmount);
				if (riskAmount != 0 && request.pay.amount > riskAmount) {
					logger.info("rejected tmnId : [{}], acquirer=[{}]",mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
					response.result = ResultUtil.getResult("9999", "승인거절","지정 카드 거래 한도 초과 ");return;
				}
			}
			
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		/*
		 * 2019-08-28 대리점,총판 한도 패쇄
		//대리점 한도 조회
		agencyMngMap = trxDAO.getAgencyMngById(mchtMap.getString("agencyId"));
		if(agencyMngMap != null){
			if(agencyMngMap.getDouble("limitOnce") > 0 && agencyMngMap.getDouble("limitOnce") < request.pay.amount ){
				logger.debug("대리점 1회 한도초과 : {},{}",agencyMngMap.getDouble("limitOnce"),request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 1회 거래한도 초과");return;
			}
			
			if(agencyMngMap.getDouble("limitDay") > 0 &&  agencyMngMap.getDouble("limitDay") < trxSumMap.getDouble("agencyDailySum") +request.pay.amount ){
				logger.debug("대리점 일일 한도초과 : {},{}",agencyMngMap.getDouble("limitDay"),trxSumMap.getDouble("agencyDailySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 일일 거래한도 초과");return;
			}
			
			if(agencyMngMap.getDouble("limitMonth") > 0 &&  agencyMngMap.getDouble("limitMonth") < trxSumMap.getDouble("agencyMonthlySum") +request.pay.amount ){
				logger.debug("대리점 월 한도초과 : {},{}",agencyMngMap.getDouble("limitMonth"),trxSumMap.getDouble("agencyMonthlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 월 거래한도 초과");return;
			}
		}
	
		
		
		//총판 한도 조회
		distMngMap = trxDAO.getDistMngById(mchtMap.getString("distId"));
		if(distMngMap != null){
			if(distMngMap.getDouble("limitOnce") > 0 && distMngMap.getDouble("limitOnce") < request.pay.amount ){
				logger.debug("총판 1회 한도초과 : {},{}",distMngMap.getDouble("limitOnce"),request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 1회 거래한도 초과");return;
			}
			
			if(distMngMap.getDouble("limitDay") > 0 &&  distMngMap.getDouble("limitDay") < trxSumMap.getDouble("distDailySum") +request.pay.amount ){
				logger.debug("총판 일일 한도초과 : {},{}",distMngMap.getDouble("limitDay"),trxSumMap.getDouble("distDailySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 일일 거래한도 초과");return;
			}
			
			if(distMngMap.getDouble("limitMonth") > 0 &&  distMngMap.getDouble("limitMonth") < trxSumMap.getDouble("distMonthlySum") +request.pay.amount ){
				logger.debug("총판 월 한도초과 : {},{}",distMngMap.getDouble("limitMonth"),trxSumMap.getDouble("distMonthlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 월 거래한도 초과");return;
			}
			
		}
		*/
		/*
		//TAX LIMIT
		SharedMap<String,Object> taxMap = trxDAO.getMchtTaxByTaxId(mchtTmnMap.getString("taxId"));
		long usedLimit = trxDAO.getTaxUsedLimit(mchtTmnMap.getString("taxId"));
		
		if(taxMap.getLong("taxLimit") == 0){
			
		}else if(taxMap.getLong("taxLimit") <= usedLimit+request.pay.amount){
			logger.info("Tax 한도초과 : LIMIT = {}, CAP AMT = {}", taxMap.getString("taxLimit"), (usedLimit+request.pay.amount) );
			
			SharedMap<String,Object> readyTaxMap = trxDAO.getMchtReadyTaxByMchtId( mchtMap.getString("mchtId"));
			
			if(readyTaxMap != null && readyTaxMap.size() > 0) {
				trxDAO.updateTaxStatus(taxMap.getString("taxId"), "만료");
				trxDAO.updateTaxStatus(readyTaxMap.getString("taxId"), "사용");
				trxDAO.updateMchtTmnTaxId(mchtTmnMap.getString("tmnId"), readyTaxMap.getString("taxId"));
				
				trxDAO.deleteMchtTmnByTmnId(mchtTmnMap.getString("tmnId"));
				trxDAO.deleteMchtTmnByPayKey(mchtTmnMap.getString("payKey"));
				
				
				mchtTmnMap.replace("taxId", readyTaxMap.getString("taxId"));
			}else {
				logger.info("Tax 한도가 초과. 예정 상태 Tax가 없어 Tax 변경 불가 - {}", taxMap.getString("taxId"));
			}
		}
		*/
		request.pay.tmnId = mchtTmnMap.getString("tmnId");
		
		logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== ========== ========== valid() - END");
	}

	private synchronized SharedMap<String, Object> getDirectVTID(SharedMap<String, Object> mng3DMap, SharedMap<String, Object> acsResMap) {
		logger.info("========== getDirectVTID() ");
		SharedMap<String, Object> map = null;
		
		logger.debug("========== getDirectVTID() - mchtId: {}", mng3DMap.getString("mchtId"));
		logger.debug("========== getDirectVTID() - amount: {}", request.pay.amount);
		logger.debug("========== getDirectVTID() - noInt: {}", acsResMap.getString("noInt"));
		logger.debug("========== getDirectVTID() - option1: {}", acsResMap.getString("option1"));
		logger.debug("========== getDirectVTID() - option2: {}", acsResMap.getString("option2"));
		logger.debug("========== getDirectVTID() - AcqId: {}", acsResMap.getString("acqId"));
		logger.debug("========== getDirectVTID() - IssuerId: {}", acsResMap.getString("issuer"));
		logger.debug("========== getDirectVTID() - Installment: {}", acsResMap.getInt("installment"));
		logger.debug("========== getDirectVTID() - BIN: {}", acsResMap.getString("bin"));

		map = trxDAO.getDirectVtidPromo(sharedMap, mchtTmnMap, acsResMap, request.pay.amount);
		if (map == null || map.isEquals("cid", "")) {
			map = trxDAO.getDirectVTID(mchtTmnMap.getString("vanIdx"), mng3DMap.getString("mchtGrade"), acsResMap.getString("acqId"));
		}
		return map;
	}
	  
	
	private synchronized String cardMask(String number) {
		String bin = number.substring(0,6);
		String last4 = number.substring(number.length()-3, number.length());
		if (number.length() == 14) {
			return bin + "*****" + last4;
		} else if (number.length() == 15) {
			return bin + "******" + last4;
		} else if (number.length() == 16) {
			return bin + "*******" + last4;
		} else {
			return bin + "*******" + last4;
		}
	}

	@Override
	public synchronized void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap, SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		logger.info("========== ========== ========== ========== ========== valid2() - BEGIN ");
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - sharedMap - mchtId : " + sharedMap.getString("mchtId"));
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - sales_mchtTmnMap - vanIdx : " + sales_mchtTmnMap.getString("vanIdx"));
		SharedMap<String, Object> vanMap = new TrxDAO().getVanByVanIdx2(sales_mchtTmnMap.getString("vanIdx"));
		SharedMap<String, Object> vanBrandMap = new SharedMap<String, Object>();
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - vanMap - vanId  : " + vanMap.getString("vanId"));
		// 2022-05-16 mchtId 조회후 넣기
		if (request.pay.mchtId == null) {
			request.pay.mchtId = sales_mchtTmnMap.getString("mchtId");
		}
		
		if (request.pay == null || request.pay.card == null) {
			response.result = ResultUtil.getResult("9999", "필수값없음", "결제정보 및 카드 정보가 없습니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		request.pay.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		request.pay.trxDate = sharedMap.getString(PAYUNIT.REG_DATE);

//		if (CommonUtil.isNullOrSpace(request.pay.tmnId)) {
		if (request.pay.tmnId == null) {
			request.pay.tmnId = sharedMap.getString("tmnId");
		} else {
			request.pay.tmnId = request.pay.tmnId.trim();
		}
		
		// 2022-08-23 - CARD BIN Number를 이용한 필터링
		String card_number = request.pay.card.number;
		if (CommonUtil.isNullOrSpace(card_number)) {
			
		} else {
			String card_bin = card_number.substring(0, 6);
			String card_last4 = card_number.substring(card_number.length() - 4, card_number.length());
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - card_number : " + card_number);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - card_bin : " + card_bin);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - card_last4 : " + card_last4);
			try {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - sharedMap mchtId : " + sharedMap.getString("mchtId"));
			} catch (Exception e) {
				// TODO: handle exception
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - Excetion : " + e.getMessage());
				e.printStackTrace();
			}
			try {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - card_bin : " + card_bin);
			} catch (Exception e) {
				// TODO: handle exception
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - Excetion : " + e.getMessage());
				e.printStackTrace();
			}
			//int binBlockListCount_mchtId = trxDAO.getBinlockListCount(request.pay.mchtId, card_bin);
			int binBlockListCount_mchtId = trxDAO.getBinlockListCount(sharedMap.getString("mchtId"), card_bin);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - binBlockListCount_mchtId : " + binBlockListCount_mchtId);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - request.pay.mchtId : " + request.pay.mchtId);
			if (binBlockListCount_mchtId > 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", "해당 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			int binBlockListCount_vanId = trxDAO.getBinlockListCount(vanMap.getString("vanId"), card_bin);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - binBlockListCount_vanId : " + binBlockListCount_vanId);
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - vanMap vanId : " + vanMap.getString("vanId"));
			if (binBlockListCount_vanId > 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", "해당 카드는 PG 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			// 2023-01-19 - 전용카드결제
			/*
			 *	삼성카드 결제시에만 특정 pg상점 아이디로 결제 요청 되게끔 요청을 하는데,
				작업 가능할까요
				추후에 또 수정요청 있을 것 같긴 합니다...
				
				=> 로직
				상섬카드 여부 체크
				PG상점ID 체크
				결제 처리
			 */
			logger.info("");
			logger.info("========== valid2() - 카드별 PG상점ID 조회");
		//	vanBrandMap = new TrxDAO().getVanByBinNSemiAuth(card_bin, sales_mchtTmnMap.getString("semiAuth"));
			vanBrandMap = new TrxDAO().getVanByBin(card_bin);
			if (vanBrandMap != null) {
				logger.info("========== valid2() - vanBrandMap : " + vanBrandMap.toJson());
				/*
SELECT * FROM PG_VAN A 
JOIN (SELECT * FROM PG_CODE_BIN GROUP BY brand) B ON A.brand = B.brand
WHERE B.bin = '379194';
				 */
				
			} else {
				logger.info("========== valid2() - vanBrandMap NULL");
			}
			logger.info("");
		}
		
		// 2022-08-01 - 영업사원 정보 조회
		SharedMap<String, Object> salesMap = new SharedMap<String, Object>();
		SharedMap<String, Object> salesMngMap = new SharedMap<String, Object>();
		if (CommonUtil.isNullOrSpace(request.pay.salesId)) {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - salesId IS NULL");
		} else {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - request.pay.salesId ::: " + request.pay.salesId);
			salesMap = trxDAO.getSalesByIdChk(request.pay.salesId);
			if (salesMap == null) {
				response.result = ResultUtil.getResult("9999", "영업사원결제", "영업사원 아이디가 존재하지 않습니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			} else {
				if (CommonUtil.isNullOrSpace(salesMap.getString("salesId"))) {
					response.result = ResultUtil.getResult("9999", "영업사원결제", "영업사원 아이디가 존재하지 않습니다.");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				} else {
					if (CommonUtil.isNullOrSpace(salesMap.getString("parentId"))) {
						response.result = ResultUtil.getResult("9999", "영업사원결제", "가맹점 소속이 아닙니다.");
						logger.info("========== ========== ========== ========== ========== valid2() END");
						return;
					} else {
						salesMngMap = trxDAO.getSalesByIdChk(request.pay.salesId);
						if (!request.pay.mchtId.equals(salesMap.getString("parentId"))) {
							response.result = ResultUtil.getResult("9999", "영업사원결제", "가맹점과 영업사원의 가맹점이 다릅니다.");
							logger.info("========== ========== ========== ========== ========== valid2() END");
							return;
						}
					}
				}
			}
		}
		// 2022-08-02 - 판매자(가맹점) 전화번호, 구매자 전화번호 동일시 차단처리
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | request.pay.payerTel : " + request.pay.payerTel);
		if (CommonUtil.isNullOrSpace(request.pay.payerTel)) {
		} else {
			SharedMap<String, Object> mchtMap2 = trxDAO.getMchtByMchtId2(sales_mchtMap.getString("mchtId"));
			
			String payerTel = request.pay.payerTel.replaceAll("-", "");
			String tel1 = mchtMap2.getString("tel1").replaceAll("-", "");
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | tel1 : " + tel1);
			if (payerTel.equals(tel1)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			String tel2 = mchtMap2.getString("tel2").replaceAll("-", "");
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | tel2 : " + tel2);
			if (payerTel.equals(tel2)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			String ceoPhone = mchtMap2.getString("ceoPhone").replaceAll("-", "");
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | ceoPhone : " + ceoPhone);
			if (payerTel.equals(ceoPhone)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			String ceoTel = mchtMap2.getString("ceoTel").replaceAll("-", "");
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | ceoTel : " + ceoTel);
			if (payerTel.equals(ceoTel)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			/*
			String fax = mchtInstall.getString("fax").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | fax : " + fax);
			if (payerTel.equals(fax)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			String managerPhone = mchtInstall.getString("managerPhone").replaceAll("-", "");
			logger.info("========== ========== 판매자, 구매자 전화번호 일치 체크 payerTel : " + payerTel + " | managerPhone : " + managerPhone);
			if (payerTel.equals(managerPhone)) {
				response.result = ResultUtil.getResult("9999", "승인거절", "주문자 휴대폰번호가 가맹점번호와 동일합니다.");
				return;
			}
			*/	
		}

		if (CommonUtil.isNullOrSpace(request.pay.trackId)) {
			response.result = ResultUtil.getResult("9999", "필수값없음", "가맹점 주문번호가 입력되지 않았습니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		//if (trxDAO.isDuplicatedTrackId(sharedMap.getString(PAYUNIT.MCHTID), request.pay.trackId)) {
		//2022-05-16 mchtId 전역변수 변경처리
		if (trxDAO.isDuplicatedTrackId(request.pay.mchtId, request.pay.trackId)) {
			response.result = ResultUtil.getResult("9999", "중복된 거래번호입니다.", "해당 거래번호로 승인/승인취소시는 재 사용할 수 없습니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}

		if (request.pay.amount < 1000) {
			response.result = ResultUtil.getResult("9999", "결제 최소 금액 오류", "1000원 미만은 결제를 허용하지 않습니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}

		//trxDAO.insertTrxIO(sharedMap, request.pay);
		// 2022-05-16 테스트
		if (!trxDAO.insertTrxIOchk(sharedMap, request.pay)) {
			response.result = ResultUtil.getResult("9999", "동시결제오류", "동시결제가 존재합니다. 결제를 다시 시도해주시기 바랍니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		//20190710 과세 유형에 따른 부가세 처리 
		sharedMap.put("taxType", sales_mchtTmnMap.getString("taxType"));
		
		// recurring pay : 정기과금통한거래 20190603
		if (request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "pay")) {
			//SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
			//2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);

			if (!mchtSvcMap.isEquals("recurring", "사용")) {
				response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금서비스가 신청되지 않았습니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			} else {
				// cardId 로 거래정보를 가져온다.
				//SharedMap<String, Object> cardKspayMap = trxDAO.getByKsnetCardId(request.pay.card.cardId, sharedMap.getString(PAYUNIT.MCHTID));
				//2022-05-16 mchtId 전역변수 변경처리
				SharedMap<String, Object> cardKspayMap = trxDAO.getByKsnetCardId(request.pay.card.cardId, request.pay.mchtId);
				if (cardKspayMap.isNullOrSpace("authKey")) {
					response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금 정보를 찾을 수 없습니다.");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				} else {
					sharedMap.put("authKey", cardKspayMap.getString("authKey"));
					sharedMap.put("recurring", "pay");
					logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | authKey : " + cardKspayMap.getString("authKey"));
				}
				// 아래 검증을 통과하기위하여 임시로 SET
				request.pay.card.number = cardKspayMap.getString("unit");
				request.pay.card.expiry = cardKspayMap.getString("expiry");
			}
		}

		// 20190705 recurring set : 정기과금SET거래
		if (request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "set")) {
			//SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
			// 2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);
			if (!mchtSvcMap.isEquals("recurring", "사용")) {
				response.result = ResultUtil.getResult("9999", "정기과금오류", "정기과금서비스가 신청되지 않았습니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			} else {
				if (request.pay.metadata.getString("authPw").length() != 2) {
					response.result = ResultUtil.getResult("9999", "필수값없음", "카드비밀번호 앞 2자리 필수 입력 : authPw");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}
				if (request.pay.metadata.getString("authDob").length() == 6 || request.pay.metadata.getString("authDob").length() == 10) {
					// 생년월일 6자리 또는 사업자번호 10자리
				} else {
					response.result = ResultUtil.getResult("9999", "필수값없음", "생년월일 또는 사업자 번호 필수입니다. : authPw");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}
				//recurring 관련 값 SET
				sharedMap.put("authPw",request.pay.metadata.getString("authPw"));
				sharedMap.put("authDob",request.pay.metadata.getString("authDob"));
				sharedMap.put("recurring","set");
			}
		}
		
		// 20190604 recurring 거래시 중복된 cardId 가 SET 되지 않도록 기존 아이디 사용
		String cardId = "";
		if (sharedMap.isEquals("recurring", "pay")) {
			//sharedMap.put(PAYUNIT.KEY_CARD, request.pay.card.cardId);
			cardId = request.pay.card.cardId;
		} else {
			//sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
			cardId = GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID));
		}

//		sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
		// 2022-05-16 상품아이디 전역변수 제거
		String prodId = GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID));
		
		if (request.pay.card.encTrackI.equals("")) {
			int cardLength = request.pay.card.number.length();
			if (cardLength < 14 || 16 < cardLength) {
				//response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.", "카드번호는 14~16자리만 허용합니다.");
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== 카드번호는 14~16자리만 허용합니다. - valid() END");
				//return;
			}
			
			if (sales_mchtMngMap.getInt("maxInstall")< request.pay.card.installment) {
				response.result = ResultUtil.getResult("9999", "할부결제", "할부제공기간을 초과하였습니다. 최대 " + CommonUtil.zerofill(mchtMngMap.getInt("maxInstall"), 2) + "개월 가능합니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}

			// 20190228 삼성페이 적용을 위한 유효기간처리 변경
			if (request.pay.card.expiry.length() == 4 || request.pay.card.expiry.length() >= 18) {
				if (request.pay.card.expiry.length() >= 18) {
					logger.info("========== 삼성페이거래");
				}
			} else {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | van : " + sharedMap.get("van"));
				if (sharedMap.get("van") != null) {
					if (sharedMap.get("van").equals("SMARTRO") || sales_mchtTmnMap.get("van").equals("SMARTRO")) {
						
					} else if (sharedMap.get("van").equals("WELCOME") || sales_mchtTmnMap.get("van").equals("WELCOME")) {
						
					} else {
						response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "YYMM 포맷이 아닙니다.");
						logger.info("========== ========== ========== ========== ========== valid2() END");
						return;
					}
				}
			}
			if (request.pay.card.expiry.length() == 4) {
				if (CommonUtil.parseInt(request.pay.card.expiry.substring(0, 2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))) {
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "유효년수가 경과된 카드입니다.");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}
				if (CommonUtil.parseInt(request.pay.card.expiry.substring(2, 4)) > 12) {
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.", "유효월 입력이 잘못되었습니다.");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}
			}
			if (CommonUtil.isNullOrSpace(request.pay.card.number)) {
				
			} else {
				request.pay.card.last4 = request.pay.card.number.substring(cardLength-4, cardLength);
				request.pay.card.bin   = request.pay.card.number.substring(0,6);
			}

		} else if (request.pay.card.encTrackI.startsWith("acs") && request.pay.card.encTrackI.length() == 20) { // 20190805 인증결제
			
//			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
//			SharedMap<String, Object> mchtMng3DMap = trxDAO.getMchtMng3D(sharedMap.getString(PAYUNIT.MCHTID));
			// 2022-05-16 mchtId 전역변수 변경처리
			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(request.pay.mchtId);
			SharedMap<String, Object> mchtMng3DMap = trxDAO.getMchtMng3D(request.pay.mchtId);
//			if (!mchtSvcMap.isEquals("card3D", "사용") || !mchtMng3DMap.isEquals("mchtId", sharedMap.getString(PAYUNIT.MCHTID))) {
			if (!mchtSvcMap.isEquals("card3D", "사용") || !mchtMng3DMap.isEquals("mchtId", request.pay.mchtId)) {
				response.result = ResultUtil.getResult("9999", "인증결제오류", "인증결제서비스를 신청하지 않은 가맹점입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			SharedMap<String, Object> acsResMap = trxDAO.getAcsRes(request.pay.card.encTrackI.trim());
			
			if (acsResMap == null || !acsResMap.isEquals("result", "0000")) {
				response.result = ResultUtil.getResult("9999", "인증결제정보없음", "ISP,MPI 인증 확인 정보가 없습니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (request.pay.metadata == null) {
				request.pay.metadata = new SharedMap<String, String>();
			}
			request.pay.metadata.put("acsType", acsResMap.getString("acsType"));
			request.pay.metadata.put("noInt", acsResMap.getString("noInt"));
			// 20191024 yhbae 변경
			String cardNo = "";
			if (!acsResMap.getString("cardNo").isEmpty()) {
				cardNo = SeedKisa.decryptAsString(Base64.decode(acsResMap.getString("cardNo")), ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16));
			}
			request.pay.card.number = cardNo;
			request.pay.card.bin = acsResMap.getString("bin");
			int cardLength = request.pay.card.number.length();
			if (cardLength > 0) {
				request.pay.card.last4 = request.pay.card.number.substring(cardLength-4, cardLength);
			}
			request.pay.card.installment = acsResMap.getInt("installment");

			// 20190917 ACS DEMO 분기 추가 
			if (acsResMap.isEquals("acsType", "DEMO")) {
				
			} else {
				if (acsResMap.isEquals("acsType", "KVP")) {
					request.pay.metadata.put("sessionKey", acsResMap.getString("sessionKey"));
					request.pay.metadata.put("encData", acsResMap.getString("encData"));
					request.pay.metadata.put("cardCode", acsResMap.getString("cardCode"));
				} else {
					request.pay.metadata.put("xid", acsResMap.getString("xid"));
					if (acsResMap.isEquals("acsType", "KMOTION")) {
						request.pay.metadata.put("cavv", acsResMap.getString("cardCode"));
						request.pay.metadata.put("eci", "  ");
					} else {
						request.pay.metadata.put("cavv", acsResMap.getString("cavv"));
						request.pay.metadata.put("eci", acsResMap.getString("eci"));
					}
				}
				/* Promotion Type을 포인트/무이자 사용에 따라 변경되도록 추가해야 한다. */
				if (acsResMap.isNullOrSpace("acqId")) {
					SharedMap<String, Object> acsReqMap = trxDAO.getAcsReq(request.pay.card.encTrackI.trim());
					String acqId = trxDAO.getAcqIdByIssuerId(acsReqMap.getString("issuer"));
					acsResMap.put("acqId", acqId);
				}
				logger.info("3D Grade: {}", mchtMng3DMap.getString("mchtGrade"));
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 3D Grade: {}", mchtMng3DMap.getString("mchtGrade"));

				SharedMap<String,Object> vtidMap = new SharedMap<String,Object>();
				vtidMap = getDirectVTID(mchtMng3DMap, acsResMap);
				
				//vtidMap = trxDAO.getDirectVTID(sales_mchtTmnMap.getString("vanIdx"), mchtMng3DMap.getString("mchtGrade"), acsResMap.getString("acqId"));
				if (vtidMap == null || vtidMap.isEquals("vtid", "")) {
					response.result = ResultUtil.getResult("9999", "가맹점 설정정보 없음","인증결제에 대한 설정정보가 없습니다. 관리자에 문의하시기 바랍니다.");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}

				sharedMap.put("3ddirect", "true");
				sharedMap.put("vtidMap", vtidMap);
				sharedMap.put("pairingVtid", vtidMap.getString("vtid"));
				sharedMap.put("pairingRouteVan", vtidMap.getString("master"));
				sharedMap.put("pairingCid", vtidMap.getString("cid"));
				String mchtBRN = trxDAO.getAESDec(sales_mchtMap.getString("identity"));
				sharedMap.put("mchtBRN", mchtBRN);
				sharedMap.put("shopDomain", mchtMngMap.getString("shopDomain"));
				sharedMap.put("mchtIp", ""); //TODO: 가맹점의 IP 설정
			}

			//3DDirect 의 경우 PG_WEBPAY_REQ, PG_ACS_RES 를 업데이트한다.
			trxDAO.update3DDirect(acsResMap.getString("payId"),acsResMap.getString("acsId"), sharedMap.getString(PAYUNIT.TRX_ID));
			
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 3dDirect metadata =\n{}",GsonUtil.toJson(request.pay.metadata));
		}
		if (request.pay.card.bin.length() >= 6) {
			SharedMap<String, Object> issuerMap = trxDAO.getDBIssuer(request.pay.card.bin);
			if (issuerMap != null) {
				request.pay.card.cardType = issuerMap.getString("type");
				request.pay.card.issuer = issuerMap.getString("issuer");
				request.pay.card.acquirer = issuerMap.getString("acquirer");
				request.pay.card.hash = CardUtil.hash(request.pay.card.number);
			}
		}
		
		//request.pay.card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);
		request.pay.card.cardId = cardId;
		
		String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(request.pay.card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
		
		if (!sharedMap.isEquals("recurring", "pay")) { // 20190604 추가 저장하지 않도록 수정
			//trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
			trxDAO.insertCard(cardId, encrypted);
		}
		sharedMap.put("CARD_INSERTED", true);//카드정보가 이미 등록되었는지 여부
		
		if (request.pay.products != null) {
			if (request.pay.products.size() > 0) {
				Product p = new Product();
				for (int i = 0; i < request.pay.products.size(); i++) {
					request.pay.products.get(0).prodId = prodId;
				}
			}
			
			// 다빈치 거래건에 대해서
			if (sharedMap.startsWith(PAYUNIT.MCHTID, "DVC") || sharedMap.startsWith(PAYUNIT.MCHTID, "onoff")) {
				if (!CommonUtil.isNullOrSpace(request.pay.udf1)) {
					Product p = new Product();
					p.name = request.pay.udf1;
					request.pay.products.add(p);
				}
			}
			//trxDAO.insertProduct(sharedMap.getString(PAYUNIT.KEY_PROD), request.pay.products, sharedMap.getString(PAYUNIT.REG_DATE));
			// 2022-05-16 전역변수 제거
			trxDAO.insertProduct(prodId, request.pay.products, sharedMap.getString(PAYUNIT.REG_DATE));
		}
		
		//semiAuth 즉 생년월일/카드비번2자리 꼭 사용하는 가맹점 2017-08-01 
		if (sales_mchtTmnMap.getString("semiAuth").equals("Y")) {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 구인증 가맹점 | semiAuth : " + sales_mchtTmnMap.getString("semiAuth"));
			if (request.pay.metadata != null) {
				request.pay.metadata.put("cardAuth", "true");
				/* 법인 사용자의 경우 비밀번호가 없어도 된다.
				if(request.pay.metadata.getString("authPw").length() !=2){
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authPw");return;
				}*/
				if (request.pay.metadata.getString("authDob").length() == 6 || request.pay.metadata.getString("authDob").length() == 10) {
					//생년월일 6자리 또는 사업자번호 10자리 
				} else {
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authDob");
					return;
				}
				
			} else {
				response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : pay.metadata");
				return;
			}
		} else if (sales_mchtTmnMap.getString("semiAuth").equals("N")) {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 비인증 가맹점 | semiAuth : " + sales_mchtTmnMap.getString("semiAuth"));
			logger.info("========== semiAuth 미사용 가맹점 {}", sales_mchtTmnMap.getString("semiAuth"));
		} else if (sales_mchtTmnMap.getString("semiAuth").equals("A")) {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 인증결제 가맹점 | semiAuth : " + sales_mchtTmnMap.getString("semiAuth"));
		} else {
			logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 결제??? 가맹점 | semiAuth : " + sales_mchtTmnMap.getString("semiAuth"));
		}
		sharedMap.put("semiAuth", sales_mchtTmnMap.getString("semiAuth"));
		
		//할부개월 적용 2018-04-10
		/*
		logger.info("mcht install : {}, pay installment : {}",sales_mchtTmnMap.getInt("apiMaxInstall"),request.pay.card.installment);
		if (sales_mchtTmnMap.getInt("apiMaxInstall") < request.pay.card.installment) {
			logger.debug("가맹점 할부개월 초과 ");
			String msg = "";
			if (sales_mchtTmnMap.getInt("apiMaxInstall") == 0 || sales_mchtTmnMap.getInt("apiMaxInstall") == 1) {
				msg ="일시불로만 ";
			} else {
				msg =sales_mchtTmnMap.getInt("apiMaxInstall")+"개월 이하로 ";
			}
			response.result = ResultUtil.getResult("9999", "할부개월초과","할부기간은 "+msg+" 이용하여 주시기 바랍니다.");return;
		}
		*/
		for (String str : sales_mchtTmnMap.keySet()) {
			//logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | 비인증 가맹점 ");
			//logger.info("========== ========== mchtTmnMap str : " + str + "\t" + sales_mchtTmnMap.get(str));
		}

		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - " + request.pay.card.acquirer + " - 할부 개월수 : " + request.pay.card.installment);
		SharedMap<String, Object> mchtInstall = trxDAO.getMchtInstall(sales_mchtMngMap.getString("mchtId"));
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - mcht maxInstall : " + mchtInstall.getInt("maxInstall") + " cardInstall : " + mchtInstall.getInt("cardInstall01"));
		SharedMap<String, Object> vanInstall = trxDAO.getVanInstall(sales_mchtMngMap.getString("mchtId"), sales_mchtTmnMap.getInt("vanIdx"));
		int van_cardInstall01 = 0;
		int van_cardInstall02 = 0;
		int van_cardInstall03 = 0;
		int van_cardInstall04 = 0;
		int van_cardInstall05 = 0;
		int van_cardInstall06 = 0;
		int van_cardInstall07 = 0;
		int van_cardInstall08 = 0;
		int van_cardInstall09 = 0;
		
		String installOnly = "";
		if (vanBrandMap != null) {
			logger.info("========== valid2() - 카드별 PG상점ID 처리");
			logger.info("========== valid2() - vanBrandMap : " + vanBrandMap.toJson());
			van_cardInstall01 = vanBrandMap.getInt("cardInstall01");
			van_cardInstall02 = vanBrandMap.getInt("cardInstall02");
			van_cardInstall03 = vanBrandMap.getInt("cardInstall03");
			van_cardInstall04 = vanBrandMap.getInt("cardInstall04");
			van_cardInstall05 = vanBrandMap.getInt("cardInstall05");
			van_cardInstall06 = vanBrandMap.getInt("cardInstall06");
			van_cardInstall07 = vanBrandMap.getInt("cardInstall07");
			van_cardInstall08 = vanBrandMap.getInt("cardInstall08");
			van_cardInstall09 = vanBrandMap.getInt("cardInstall09");
			
			installOnly = vanBrandMap.getString("installOnly"); 
		} else {
			logger.info("========== valid2() - 카드별 PG상점ID X");
			van_cardInstall01 = vanInstall.getInt("cardInstall01");
			van_cardInstall02 = vanInstall.getInt("cardInstall02");
			van_cardInstall03 = vanInstall.getInt("cardInstall03");
			van_cardInstall04 = vanInstall.getInt("cardInstall04");
			van_cardInstall05 = vanInstall.getInt("cardInstall05");
			van_cardInstall06 = vanInstall.getInt("cardInstall06");
			van_cardInstall07 = vanInstall.getInt("cardInstall07");
			van_cardInstall08 = vanInstall.getInt("cardInstall08");
			van_cardInstall09 = vanInstall.getInt("cardInstall09");
		}
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - van maxInstall : " + vanInstall.getInt("maxInstall"));
		logger.info("========== valid2() - van cardInstall01 : " + vanInstall.getInt("cardInstall01"));
		logger.info("========== valid2() - van cardInstall02 : " + vanInstall.getInt("cardInstall02"));
		logger.info("========== valid2() - van cardInstall03 : " + vanInstall.getInt("cardInstall03"));
		logger.info("========== valid2() - van cardInstall04 : " + vanInstall.getInt("cardInstall04"));
		logger.info("========== valid2() - van cardInstall05 : " + vanInstall.getInt("cardInstall05"));
		logger.info("========== valid2() - van cardInstall06 : " + vanInstall.getInt("cardInstall06"));
		logger.info("========== valid2() - van cardInstall07 : " + vanInstall.getInt("cardInstall07"));
		logger.info("========== valid2() - van cardInstall08 : " + vanInstall.getInt("cardInstall08"));
		logger.info("========== valid2() - van cardInstall09 : " + vanInstall.getInt("cardInstall09"));
		
		logger.info("========== valid2() - request.pay.card.installment : " + request.pay.card.installment);
		
		// 2021-11-22 카드사별 할부기간 적용
		if (mchtInstall == null || vanInstall == null) {
			response.result = ResultUtil.getResult("9999", "가맹점 웹결제 미설정", "웹결제 미설정 가맹점입니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		if (installOnly.equals("Y")) {
			logger.info("========== 할부결제만 가능");
			if (request.pay.card.installment == 0) {
				response.result = ResultUtil.getResult("9999", "할부결제", "해당 PG상점아이디는 할부결제만 가능합니다. 일시불결제를 할 수 없습니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		} else if (vanInstall.getString("installOnly").equals("N")) {
			logger.info("========== 일시불, 할부결제 가능");
		}
		
		if (request.pay.card.acquirer.equals("비씨")) {
			if (mchtInstall.getInt("cardInstall01") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다."); 
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall01") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall01") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall01") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall01 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall01 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall01 == 1) {
					msg ="일시불로만 ";
				} else {
					msg = van_cardInstall01 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("국민")) {
			if (mchtInstall.getInt("cardInstall02") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall02") < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall02") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall02") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall02 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall02 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall02 == 1) {
					msg ="일시불로만 ";
				} else {
					msg = van_cardInstall02 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("삼성")) {
			if (mchtInstall.getInt("cardInstall03") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall03") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall03") == 1) {
					msg ="일시불로만 ";
				} else {
					msg =mchtInstall.getInt("cardInstall03")+"개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall03 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall03 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall03 == 1) {
					msg ="일시불로만 ";
				} else {
					msg = van_cardInstall03 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("외환")) {
			if (mchtInstall.getInt("cardInstall04") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall04") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall04") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall04") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall04 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall04 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall04 == 1) {
					msg ="일시불로만 ";
				} else {
					msg = van_cardInstall04 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("신한")) {
			if (mchtInstall.getInt("cardInstall05") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall05") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall05") == 1) {
					msg ="일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall05") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall05 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall05 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall05 == 1) {
					msg = "일시불로만 ";
				} else {
					msg = van_cardInstall05 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("농협")) {
			if (mchtInstall.getInt("cardInstall06") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절","농협 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall06") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall06") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall06") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall06 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall06 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall06 == 1) {
					msg = "일시불로만 ";
				} else {
					msg = van_cardInstall06 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("롯데")) {
			if (mchtInstall.getInt("cardInstall07") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall07") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall07") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall07") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall07 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall07 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall07 == 1) {
					msg = "일시불로만 ";
				} else {
					msg = van_cardInstall07 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("현대")) {
			if (mchtInstall.getInt("cardInstall08") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall08") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall08") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall08") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall08 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall08 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall08 == 1) {
					msg = "일시불로만 ";
				} else {
					msg = van_cardInstall08 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		if (request.pay.card.acquirer.equals("하나")) {
			if (mchtInstall.getInt("cardInstall09") == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 가맹점 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (mchtInstall.getInt("cardInstall09") < request.pay.card.installment) {
				logger.info("가맹점 할부개월 초과 ");
				String msg = "";
				if (mchtInstall.getInt("cardInstall09") == 1) {
					msg = "일시불로만 ";
				} else {
					msg = mchtInstall.getInt("cardInstall09") + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			
			if (van_cardInstall09 == 0) {
				response.result = ResultUtil.getResult("9999", "승인거절", request.pay.card.acquirer + " 카드는 VAN사 승인 제한된 카드입니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
			if (van_cardInstall09 < request.pay.card.installment) {
				logger.info("VAN사 할부개월 초과 ");
				String msg = "";
				if (van_cardInstall09 == 1) {
					msg = "일시불로만 ";
				} else {
					msg = van_cardInstall09 + "개월 이하로 ";
				}
				response.result = ResultUtil.getResult("9999", "할부개월초과", request.pay.card.acquirer + " 할부기간은 "+msg+" 이용하여 주시기 바랍니다.");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		}
		
		if ((request.pay.card.cardType.equals("체크") || request.pay.card.cardType.equals("선불")) && request.pay.card.installment > 0) {
			response.result = ResultUtil.getResult("9999", "할부불가카드","선불/체크카드는 할부거래가 불가합니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}

		//지불중지 가맹점
		if (sales_mchtMngMap.isEquals("payStatus", "중지")) {
			response.result = ResultUtil.getResult("9999", "승인거절", "승인중지된 가맹점입니다.");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		//사용중지 가맹점
		if (!sales_mchtMap.isEquals("status", "사용")) {
			response.result = ResultUtil.getResult("9999", "승인거절", "승인중지된 가맹점입니다");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		//단말기 한도 관리 단말기 한도가 있으면 단말기 한도 따라서 20200219  , 터미널 1회 한도 사용 가맹점은 가맹점 설정 정보 갱신 처리
		if (sales_mchtTmnMap.getLong("limitOnce") > 0) {
			if (sales_mchtTmnMap.getDouble("limitOnce") < request.pay.amount) {
				logger.debug("터미널 1회 한도초과 : {},{}",sales_mchtTmnMap.getDouble("limitOnce"),request.pay.amount);
				// 하단 한도거래설정으로 인한 주석처리
				//response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과(터미널)");return;
			} else {
				//sales_mchtMngMap = trxDAO.getMchtMngByMchtIdImmediate(sharedMap.getString(PAYUNIT.MCHTID));
				//2022-05-16 mchtId 전역변수 변경처리
				sales_mchtMngMap = trxDAO.getMchtMngByMchtIdImmediate(request.pay.mchtId);
				logger.debug("가맹점 한도 설정 정보 재 갱신");
			}
		} else {
			//가맹점 한도 측정
			if (sales_mchtMngMap.getDouble("limitOnce") > 0 && sales_mchtMngMap.getDouble("limitOnce") < request.pay.amount) {
				logger.debug("가맹점 1회 한도초과 : {},{}",sales_mchtMngMap.getDouble("limitOnce"),request.pay.amount);
				// 하단 한도거래설정으로 인한 주석처리
				//response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
			}
		}
		
		//20201124 터미널 한도 처리
		SharedMap<String,Object> trxSumMap = trxDAO.getTrxTmnDaySum(sales_mchtTmnMap);
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | tmnDailySum : " + trxSumMap.getLong("tmnDailySum"));
		if (sales_mchtTmnMap.getLong("limitDay") > 0 && sales_mchtTmnMap.getDouble("limitDay") < trxSumMap.getDouble("tmnDailySum") + request.pay.amount ) {
			logger.debug("터미널 일 한도초과 : {},{}",sales_mchtTmnMap.getDouble("limitDay"),request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일 거래한도 초과(터미널)");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		
		trxSumMap = trxDAO.getTrxTmnMonthSum(sales_mchtTmnMap);
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | limitMonth : " + trxSumMap.getLong("tmnMonthSum"));
		if(sales_mchtTmnMap.getLong("limitMonth") > 0 && sales_mchtTmnMap.getDouble("limitMonth") < trxSumMap.getDouble("tmnMonthSum") + request.pay.amount ) {
			logger.debug("터미널 월 한도초과 : {},{}",sales_mchtTmnMap.getDouble("limitMonth"),request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과(터미널)");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		trxSumMap = trxDAO.getTrxTmnYearSum(sales_mchtTmnMap);
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | limitYear : " + trxSumMap.getLong("tmnYearSum"));
		if(sales_mchtTmnMap.getLong("limitYear") > 0 && sales_mchtTmnMap.getDouble("limitYear") < trxSumMap.getDouble("tmnYearSum") + request.pay.amount ) {
			logger.debug("터미널 년 한도초과 : {},{}",sales_mchtTmnMap.getDouble("limitYear"), request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 년 거래한도 초과(터미널)");
			logger.info("========== ========== ========== ========== ========== valid2() END");
			return;
		}
		
		//가맹점/지사/총판 한도조회 20190828 가맹점 한도만 가져도오록 수정 
		//trxSumMap = trxDAO.getTrxSum2(sales_mchtMap);
		trxSumMap = trxDAO.getTrxSum(sales_mchtMap);
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | limitDay :  " + sales_mchtMngMap.get("limitDay") + " SUM : " + trxSumMap.get("mchtDailySum"));
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | limitMonth :  " + sales_mchtMngMap.get("limitMonth") + " SUM : " + trxSumMap.get("mchtMonthlySum"));
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " | limitYear :  " + sales_mchtMngMap.get("limitYear") + " SUM : " + trxSumMap.get("mchtYearSum"));
/*
		logger.info("mcht sum daily :{}, monthly: {}",trxSumMap.getDouble("mchtDailySum"),trxSumMap.getDouble("mchtMonthlySum"));
		//가맹점 한도 조회
		if (sales_mchtMngMap.getDouble("limitDay") > 0
				&& sales_mchtMngMap.getDouble("limitDay") < trxSumMap.getDouble("mchtDailySum") + request.pay.amount) {
			logger.debug("가맹점 일일 한도초과 : {},{}", sales_mchtMngMap.getDouble("limitDay"),
					trxSumMap.getDouble("mchtDailySum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 일일 거래한도 초과");
			return;
		}
		if (sales_mchtMngMap.getDouble("limitMonth") > 0
				&& sales_mchtMngMap.getDouble("limitMonth") < trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount) {
			logger.debug("가맹점 월 한도초과 : {},{}", sales_mchtMngMap.getDouble("limitMonth"),
					trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월 거래한도 초과");
			return;
		}
		if (sales_mchtMngMap.getDouble("limitYear") > 0
				&& sales_mchtMngMap.getDouble("limitYear") < trxSumMap.getDouble("mchtYearSum") + request.pay.amount) {
			logger.debug("가맹점 년 한도초과 : {},{}", sales_mchtMngMap.getDouble("limitYear"),
					trxSumMap.getDouble("mchtlimitYearSum") + request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 년 거래한도 초과");
			return;
		}
*/
		// 2021-11-24 적용여부에 따른 한도설정
		//가맹점 한도 조회
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 건 한도 적용여부 limitOncePermit : " + mchtInstall.getString("limitOncePermit") + " - 건 한도 limitOnce : " + sales_mchtMngMap.getLong("limitOnce"));
		if (mchtInstall.getString("limitOncePermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitOncePermit").equals("Y")) {
			//한도초과
			if (sales_mchtMngMap.getDouble("limitOnce") > 0 && sales_mchtMngMap.getDouble("limitOnce") < request.pay.amount) {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 건 한도 초과 limitOnce : " + sales_mchtMngMap.getLong("limitOnce") + " - 거래금액 : " + request.pay.amount);
				logger.debug("가맹점 건 한도초과 : {},{}", sales_mchtMngMap.getDouble("limitOnce"), request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점건한도초과");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		} else if (mchtInstall.getString("limitOncePermit").equals("H")) {
			//정산보류
		}
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 일 한도 적용여부 limitDayPermit : " + mchtInstall.getString("limitDayPermit") + " - 일 한도 limitDay : " + sales_mchtMngMap.getLong("limitDay"));
		if (mchtInstall.getString("limitDayPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitDayPermit").equals("Y")) {
			//한도초과
			if (sales_mchtMngMap.getDouble("limitDay") > 0 && sales_mchtMngMap.getDouble("limitDay") < trxSumMap.getDouble("mchtDailySum") + request.pay.amount) {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 일 한도 초과 limitDay : " + sales_mchtMngMap.getDouble("limitDay") + " - 거래금액 : " + trxSumMap.getDouble("mchtDailySum") +  request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점일한도초과");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		} else if (mchtInstall.getString("limitDayPermit").equals("H")) {
			//정산보류
		}
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 월 한도 적용여부 limitMonthPermit : " + mchtInstall.getString("limitMonthPermit") + " - 월 한도 limitMonth : " + sales_mchtMngMap.getLong("limitMonth"));
		if (mchtInstall.getString("limitMonthPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitMonthPermit").equals("Y")) {
			//한도초과
			if (sales_mchtMngMap.getDouble("limitMonth") > 0 && sales_mchtMngMap.getDouble("limitMonth") < trxSumMap.getDouble("mchtMonthlySum") + request.pay.amount) {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 월 한도 초과 limitMonth : " + sales_mchtMngMap.getDouble("limitMonth") + " - 거래금액 : " + trxSumMap.getDouble("mchtMonthlySum") +  request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점월한도초과");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		} else if (mchtInstall.getString("limitMonthPermit").equals("H")) {
			//정산보류
		}
		logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 년 한도 적용여부 limitYearPermit : " + mchtInstall.getString("limitYearPermit") + " - 년 한도 limitYear : " + sales_mchtMngMap.getLong("limitYear"));
		if (mchtInstall.getString("limitYearPermit").equals("N")) {
			//적용안함
		} else if (mchtInstall.getString("limitYearPermit").equals("Y")) {
			//한도초과
			if (sales_mchtMngMap.getDouble("limitYear") > 0 && sales_mchtMngMap.getDouble("limitYear") < trxSumMap.getDouble("mchtYearSum") + request.pay.amount) {
				logger.info("========== valid2() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - 가맹점 년 한도 초과 limitYear : " + sales_mchtMngMap.getDouble("limitYear") + " - 거래금액 : " + trxSumMap.getDouble("mchtYearSum") +  request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과", "가맹점년한도초과");
				logger.info("========== ========== ========== ========== ========== valid2() END");
				return;
			}
		} else if (mchtInstall.getString("limitYearPermit").equals("H")) {
			//정산보류
		}
		
		
		// 거래정지 카드 목록 SELECT 
/*
		SharedMap<String,Object> blMap = trxDAO.selectTrxBl(request.pay.card.hash, sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		if (blMap != null) {
			if (blMap.getLong("idx") > 0) {
				logger.info("B/L Block : {},{},{},{}",blMap.getLong("idx"),blMap.getString("masked"),blMap.getString("trxId"),blMap.getString("activeDay"));
				response.result = ResultUtil.getResult("9999", "서비스불가카드","서비스불가카드입니다. B/L 등록카드 관리자에 문의하시기 바랍니다.");return;
			}
		}
*/
	/*	
		//1일 카드 한도
		if(sales_mchtMngMap.getLong("cardLimit") > 0 ){
			
			if(request.pay.card.bin.isEmpty() || request.pay.card.last4.isEmpty()) { // 20191024 yhbae 추가
				logger.debug("EMPTY BIN or Last4: {}", sharedMap.getString("trxId"));
			} else {
				long sumByCard = trxDAO.sumByCard(sales_mchtMngMap.getString("mchtId"), request.pay.card.bin, request.pay.card.last4);
				if( sales_mchtMngMap.getDouble("cardLimit") < request.pay.amount + sumByCard  ){
					logger.debug("한카드 1일거래 한도초과 : {},{}",sales_mchtMngMap.getLong("cardLimit"),(sumByCard+request.pay.amount));
					response.result = ResultUtil.getResult("9999", "한도초과","한카드 1일 거래 한도 초과");return;
				}
			}
		}
	*/	
		
		//2020-01-22
		try {//
			if (sales_mchtMap.isEquals("mchtId", "mono360") && request.pay.amount >= 1000000) {
				if (request.pay.card.acquirer.equals("롯데") && !request.pay.card.cardType.equals("체크")) {
					logger.debug("mono360 거래제한 요청 100만원이상 롯데 신용인경우 ");
					response.result = ResultUtil.getResult("9999", "고액롯데카드거래제한","고액 롯데 카드 거래제한");
					logger.info("========== ========== ========== ========== ========== valid2() END");return;
				}
			}
		} catch (Exception e) {
		}
		
		
		//2020-03-23 특정 터미널 카드사별 한도 차단 
		try {
			if (CommonUtil.parseLong(sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8)) > 20200323 && !CommonUtil.isNullOrSpace(request.pay.card.acquirer)) {
			//	logger.info("========== tmnId : [{}], acquirer=[{}]",sales_mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
				long riskAmount = trxDAO.getHighRiskTmn(sales_mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
			//	logger.info("========== tmnId : [{}], riskAmount=[{}]",sales_mchtTmnMap.getString("tmnId"),riskAmount);
				if (riskAmount != 0 && request.pay.amount > riskAmount) {
					logger.info("rejected tmnId : [{}], acquirer=[{}]",sales_mchtTmnMap.getString("tmnId"),request.pay.card.acquirer);
					response.result = ResultUtil.getResult("9999", "승인거절","지정 카드 거래 한도 초과 ");
					logger.info("========== ========== ========== ========== ========== valid2() END");
					return;
				}
			}
			
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		/*
		 * 2019-08-28 대리점,총판 한도 패쇄
		//대리점 한도 조회
		agencyMngMap = trxDAO.getAgencyMngById(sales_mchtMap.getString("agencyId"));
		if(agencyMngMap != null){
			if(agencyMngMap.getDouble("limitOnce") > 0 && agencyMngMap.getDouble("limitOnce") < request.pay.amount ){
				logger.debug("대리점 1회 한도초과 : {},{}",agencyMngMap.getDouble("limitOnce"),request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 1회 거래한도 초과");return;
			}
			
			if(agencyMngMap.getDouble("limitDay") > 0 &&  agencyMngMap.getDouble("limitDay") < trxSumMap.getDouble("agencyDailySum") +request.pay.amount ){
				logger.debug("대리점 일일 한도초과 : {},{}",agencyMngMap.getDouble("limitDay"),trxSumMap.getDouble("agencyDailySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 일일 거래한도 초과");return;
			}
			
			if(agencyMngMap.getDouble("limitMonth") > 0 &&  agencyMngMap.getDouble("limitMonth") < trxSumMap.getDouble("agencyMonthlySum") +request.pay.amount ){
				logger.debug("대리점 월 한도초과 : {},{}",agencyMngMap.getDouble("limitMonth"),trxSumMap.getDouble("agencyMonthlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","대리점 월 거래한도 초과");return;
			}
		}
	
		
		
		//총판 한도 조회
		distMngMap = trxDAO.getDistMngById(sales_mchtMap.getString("distId"));
		if(distMngMap != null){
			if(distMngMap.getDouble("limitOnce") > 0 && distMngMap.getDouble("limitOnce") < request.pay.amount ){
				logger.debug("총판 1회 한도초과 : {},{}",distMngMap.getDouble("limitOnce"),request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 1회 거래한도 초과");return;
			}
			
			if(distMngMap.getDouble("limitDay") > 0 &&  distMngMap.getDouble("limitDay") < trxSumMap.getDouble("distDailySum") +request.pay.amount ){
				logger.debug("총판 일일 한도초과 : {},{}",distMngMap.getDouble("limitDay"),trxSumMap.getDouble("distDailySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 일일 거래한도 초과");return;
			}
			
			if(distMngMap.getDouble("limitMonth") > 0 &&  distMngMap.getDouble("limitMonth") < trxSumMap.getDouble("distMonthlySum") +request.pay.amount ){
				logger.debug("총판 월 한도초과 : {},{}",distMngMap.getDouble("limitMonth"),trxSumMap.getDouble("distMonthlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","총판 월 거래한도 초과");return;
			}
			
		}
		*/
		/*
		//TAX LIMIT
		SharedMap<String,Object> taxMap = trxDAO.getMchtTaxByTaxId(sales_mchtTmnMap.getString("taxId"));
		long usedLimit = trxDAO.getTaxUsedLimit(sales_mchtTmnMap.getString("taxId"));
		
		if(taxMap.getLong("taxLimit") == 0){
			
		}else if(taxMap.getLong("taxLimit") <= usedLimit+request.pay.amount){
			logger.info("Tax 한도초과 : LIMIT = {}, CAP AMT = {}", taxMap.getString("taxLimit"), (usedLimit+request.pay.amount) );
			
			SharedMap<String,Object> readyTaxMap = trxDAO.getMchtReadyTaxByMchtId( sales_mchtMap.getString("mchtId"));
			
			if(readyTaxMap != null && readyTaxMap.size() > 0) {
				trxDAO.updateTaxStatus(taxMap.getString("taxId"), "만료");
				trxDAO.updateTaxStatus(readyTaxMap.getString("taxId"), "사용");
				trxDAO.updateMchtTmnTaxId(sales_mchtTmnMap.getString("tmnId"), readyTaxMap.getString("taxId"));
				
				trxDAO.deleteMchtTmnByTmnId(sales_mchtTmnMap.getString("tmnId"));
				trxDAO.deleteMchtTmnByPayKey(sales_mchtTmnMap.getString("payKey"));
				
				
				sales_mchtTmnMap.replace("taxId", readyTaxMap.getString("taxId"));
			}else {
				logger.info("Tax 한도가 초과. 예정 상태 Tax가 없어 Tax 변경 불가 - {}", taxMap.getString("taxId"));
			}
		}
		*/
		request.pay.tmnId = sales_mchtTmnMap.getString("tmnId");
		
		logger.info("========== ========== ========== ========== ========== valid2() - END");
	}

}
