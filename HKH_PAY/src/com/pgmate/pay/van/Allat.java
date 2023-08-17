package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;




/**
 * @author Administrator
 *
 */
public class Allat implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Allat.class ); 
	static final String ALLAT_CREDIT_URL 			= "https://tx.allatpay.com/servlet/AllatPay/pay/approval.jsp";
	static final String ALLAT_CANCEL_URL 		= "https://tx.allatpay.com/servlet/AllatPay/pay/cancel.jsp";
	static final int ALLAT_CONNECT_TIMEOUT 	= 5000;
	static final int ALLAT_TIMEOUT 				= 30000;
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 				= "Network Error";
	static final String CHARSET 						= "UTF-8";
	
	 
	private String SHOPID 							= "";
	private String CRYPTOKEY						= "";
	private String VAN									= "";
	
	
	public Allat(){
		
	}
	
	public Allat(SharedMap<String, Object> vanMap) {
		SHOPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
		VAN	= vanMap.getString("van").trim();

		logger.info("========== ========== ========== ========== ========== ========== ========== ");
		logger.info("========== Allat SHOPID : " + SHOPID);
		logger.info("========== Allat CRYPTOKEY : " + CRYPTOKEY);
		logger.info("========== Allat VAN : " + VAN);
		logger.info("========== ========== ========== ========== ========== ========== ========== ");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		HashMap<String,String> req = new HashMap<String,String>();
		
		req.put("allat_shop_id"      , SHOPID);									//상점ID
		req.put("allat_order_no"     , response.pay.trxId);						//주문번호
		req.put("allat_amt"          , CommonUtil.toString(response.pay.amount));//거래금액
		req.put("allat_card_no"      , response.pay.card.number);				//카드번호
		req.put("allat_cardvalid_ym" , response.pay.card.expiry);				//유효기간
		req.put("allat_sell_mm"      , CommonUtil.zerofill(response.pay.card.installment, 2));		//할부기간
		
		req.put("allat_cardcert_yn"      , "N");								//카드인증 안함.
		
		if(response.pay.metadata != null){
			if(response.pay.metadata.isTrue("cardAuth")){						//인증결제사용시
				req.put("allat_cardcert_yn"      , "Y");						//인증결제
				req.put("allat_passwd_no"      , response.pay.metadata.getString("authPw"));		//패스워드 2자리
				if(response.pay.metadata.getString("authDob").length() == 6){	//개인의 경우 
					req.put("allat_business_type"      , "0");					//개인
					req.put("allat_registry_no"        , response.pay.metadata.getString("authDob"));	//개인
				}else{
					req.put("allat_business_type"      , "1");					//법인
					req.put("allat_biz_no"             , response.pay.metadata.getString("authDob"));	//법인
				}
			}
			response.pay.metadata = null;
		}
		//String memberId = response.pay.card.cardId.replaceAll("card_", "").replaceAll("-", "");
		req.put("allat_shop_member_id"	, response.pay.trxId);		//회원아이디
		req.put("allat_product_cd"		, item);							//상품코드
		req.put("allat_product_nm"      , item);							//상품명
		req.put("allat_zerofee_yn"      , "N");								//일반/무이자여부
		req.put("allat_buyer_nm"        , CommonUtil.nToB(response.pay.payerName,"구매자"));		//구매자이름
		req.put("allat_recp_name"       , CommonUtil.nToB(response.pay.payerName,"구매자"));		//수취인이름
		req.put("allat_recp_addr"       , CommonUtil.nToB(response.pay.payerName,"구매자"));		//수취인주소
		req.put("allat_user_ip"         , CommonUtil.nToB(sharedMap.getString(PAYUNIT.REMOTEIP),"Unknown"));		//구매자아이피
		req.put("allat_email_addr"      , CommonUtil.nToB(response.pay.payerEmail,"support@pairingpayments.net"));		//이메일
		req.put("allat_bonus_yn"        , "N");		//보너스포인트사용여부
		req.put("allat_gender"          , "");		//성별 M,F
		req.put("allat_birth_ymd"       , "");		//생년월일   YYYYMMDD
		req.put("allat_cost_amt"        , CommonUtil.toString(new Double(response.pay.amount *10 /110).longValue()));		//공급가액
		req.put("allat_pay_type"        , "NOR");		//결제방식
		req.put("allat_test_yn"         , "N");			//테스트여부 
		req.put("allat_opt_pin"         , "NOUSE");		//참조필드
		req.put("allat_opt_mod"         , "APP");		//참조필드

		
		String szAllatEncData = setValue(req);
		String szReqMsg = "allat_shop_id=" + SHOPID + "&allat_amt="+ CommonUtil.toString(response.pay.amount)+ "&allat_enc_data=" + szAllatEncData+ "&allat_cross_key="+ CRYPTOKEY
				+ "&allat_opt_lang=JSP&allat_opt_ver=1.0.7.1&allat_apply_ymdhms="+sharedMap.getString(PAYUNIT.REG_DATE);
		
		
		HashMap<String,String> resD = connect(ALLAT_CREDIT_URL,szReqMsg);
		
		response.pay.authCd = CommonUtil.toString(resD.get("approval_no"));
		if(CommonUtil.toString(resD.get("reply_cd")).equals("0000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("reply_cd")),"승인실패",CommonUtil.cut(resD.get("reply_msg"),100));
		}
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",SHOPID);
		sharedMap.put("vanTrxId",CommonUtil.toString(resD.get("seq_no")));
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("reply_cd")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("reply_msg")));
		sharedMap.put("vanDate",CommonUtil.toString(resD.get("approval_ymdhms")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		logger.debug("cardName : {},{}",CommonUtil.toString(resD.get("card_nm")),CommonUtil.toString(resD.get("card_id")));
		logger.debug("cardType : {}",CommonUtil.toString(resD.get("card_type")),CommonUtil.toString(resD.get("card_id")));
		
		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		HashMap<String,String> req = new HashMap<String,String>();
		
		req.put("allat_shop_id"      , SHOPID);									//상점ID
		//req.put("allat_order_no"     , response.refund.trxId);						//주문번호
		String trxId = response.refund.trxId;
		String rootTrxId = response.refund.rootTrxId;
		logger.info("========== trxId : " + trxId + " | rootTrxId : " + rootTrxId);
		SharedMap<String, Object> notifyMap = new SharedMap<String, Object>();
		if (rootTrxId.substring(0, 1).equals("N")) {
			notifyMap = trxDAO.getWelcomeNotify(rootTrxId);
			if (notifyMap != null) {
				logger.info("========== order_no -> : allat_order_no " + notifyMap.getString("order_no"));
				logger.info("========== org_pg_seq_no -> : allat_seq_no " + notifyMap.getString("org_pg_seq_no"));
				//req.put("allat_order_no", trxId);						//주문번호
				req.put("allat_order_no", notifyMap.getString("order_no"));		//웰컴주문번호
				req.put("allat_seq_no", notifyMap.getString("org_pg_seq_no"));// 상위주문번호
			} else {
				logger.info("========== notifyMap IS NULL");
			}
		} else {
			logger.info("========== response.refund.trxId : " + response.refund.trxId);
			req.put("allat_order_no", response.refund.trxId);							//주문번호
			req.put("allat_seq_no"       , payMap.getString("vanTrxId"));			//거래번호
		}
		//req.put("allat_seq_no"       , payMap.getString("vanTrxId"));			//원거래번호
		
		
		req.put("allat_amt"          , CommonUtil.toString(response.refund.amount));//거래금액
		req.put("allat_pay_type"        , "CARD");		//결제방식
		req.put("allat_test_yn"         , "N");			//테스트여부 
		req.put("allat_opt_pin"         , "NOUSE");		//참조필드
		req.put("allat_opt_mod"         , "APP");		//참조필드
		
		logger.info("");
		logger.info("========== Allat 암호화 전 req : " + req.toString());
		logger.info("");
		
		String szAllatEncData = setValue(req);
		
		logger.info("");
		logger.info("========== Allat 암호화 후 szAllatEncData : " + szAllatEncData);
		logger.info("");
		
		String szReqMsg = 
				"allat_shop_id=" + SHOPID + 
				"&allat_amt="+ CommonUtil.toString(response.refund.amount) + 
				"&allat_enc_data=" + szAllatEncData + 
				"&allat_cross_key="+ CRYPTOKEY + 
				"&allat_opt_lang=JSP&allat_opt_ver=1.0.7.1&allat_apply_ymdhms=" + 
				sharedMap.getString(PAYUNIT.REG_DATE);
		
		HashMap<String,String> resD = connect(ALLAT_CANCEL_URL,szReqMsg);
		
		response.refund.authCd = "";
		if(CommonUtil.toString(resD.get("reply_cd")).equals("0000") || CommonUtil.toString(resD.get("reply_cd")).equals("0505") || CommonUtil.toString(resD.get("reply_cd")).equals("0510")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		} else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("reply_cd")),"취소실패",CommonUtil.cut(resD.get("reply_msg"),100));
		}
		sharedMap.put("van",payMap.getString("van"));
		sharedMap.put("vanId",SHOPID);
		sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("reply_cd")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("reply_msg")));
		sharedMap.put("vanDate",CommonUtil.toString(resD.get("cancel_ymdhms")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;
	}
	
	
	public HashMap<String,String> connect(String targetUrl,String szReqMsg){

		HashMap<String,String> resData= new HashMap<String,String>();
		logger.info("send : [{}]",szReqMsg);
		/*
		if(!checkEnc(szReqMsg)){
			resData.put("reply_cd", "XXXX");
			resData.put("reply_msg", "데이터 암호화 오류");
			return resData;
		}*/
		
		long time = System.currentTimeMillis();
		
		String result = "";
		URL url = null;
		HttpURLConnection conn = null;
		
		try {
			logger.info("allat send : [{}]",szReqMsg);
			url = new URL(targetUrl);
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(ALLAT_CONNECT_TIMEOUT);
			conn.setReadTimeout(ALLAT_TIMEOUT);
			
			byte[] reqbuf = szReqMsg.getBytes(Charset.forName("euc-kr"));
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-length", CommonUtil.toString(reqbuf.length));
			
			OutputStream os = conn.getOutputStream();
			os.write(reqbuf);
			os.flush();
			os.close();
			
			InputStream inputStream = conn.getInputStream();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
	        int bcount = 0;
	        byte[] buf = new byte[2048];
	        int read_retry_count = 0;
	        while(true) {
				int n = inputStream.read(buf);
	            if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
	            else if (n == -1) break;
	            else  { // n == 0
	                if (++read_retry_count >= 5)
	                  throw new IOException("inputstream-read-retry-count(5) exceed !");
	            }
	            if(inputStream.available() == 0){ break; }
	        }
	        bout.flush();
	        byte[] res = bout.toByteArray();
	        bout.close();
			
	        result = convert(res,"euc-kr");
	        resData = getValue(result);

		} catch(Exception e) {
			result = "NOTCONNECTED "+e.getMessage();
			resData.put("reply_cd", "XXXX");
			resData.put("reply_msg", result);
			logger.debug("allat,error : {}",e.getMessage());
		}finally{
			logger.debug("allat Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			logger.debug("allat, res = [{}]",resData);
			conn.disconnect();
		}
		
		
		return resData;
	}
	
	private HashMap<String, String> getValue(String sText) {
		HashMap<String, String> retHm = new HashMap<String, String>();
		String sArg1 = null;
		String sArg2 = null;

		StringTokenizer fstTk = new StringTokenizer(sText, "\n");
		while (fstTk != null && fstTk.hasMoreTokens()) {
			String tmpTk = fstTk.nextToken();
			StringTokenizer secTk = new StringTokenizer(tmpTk, "=");
			for (int i = 0; i < 2; i++) {
				if (i == 0) {
					if (secTk.hasMoreTokens())
						sArg1 = secTk.nextToken().trim();
					else
						sArg1 = "";
				} else {
					if (secTk.hasMoreTokens())
						sArg2 = secTk.nextToken().trim();
					else
						sArg2 = "";
				}
			}
			retHm.put(sArg1, sArg2);
		}
		if (retHm.get("reply_cd") == null) {
			retHm.put("reply_cd", "0299");
			retHm.put("reply_msg", sText);
		}
		return retHm;
	}

	private boolean checkEnc(String srcStr) {
		int ckIdx;

		ckIdx = srcStr.indexOf("allat_enc_data=");

		if (ckIdx == -1) {
			logger.info("DDDDDD");
			return false;
		} else {
			ckIdx += "allat_enc_data=".length() + 5;
		}
		if ((srcStr.substring(ckIdx, ckIdx + 1)).equals("1")) {
			return true;
		} else {
			logger.info("XXXXXX [{}]", srcStr.substring(ckIdx, ckIdx + 1));
			return false;
		}
	}

	public String setValue(HashMap<String, String> hm) {
		String formData = "";
		int i = 0;
		boolean bFirst = false;
		if (hm == null) {
			formData = null;
			return formData;
		}
		
		String str_hexa1 = "";
		String str_hexa2 = "";
		String hexa1 = "";
		String hexa2 = "";
		
		//hexa1 = stringToHex(str_hexa1);
		//hexa2 = stringToHex(str_hexa2);
		//logger.info("========== str_hexa1 : " + str_hexa1);
		//logger.info("========== hexa1 : " + hexa1.trim());
		//logger.info("========== str_hexa2 : " + str_hexa2);
		//logger.info("========== hexa2 : " + hexa2.trim());
		// stringToHex - X
		// stringToHex0x - X
		//hexa1 = "&#3;";
		//hexa2 = "&#24;";
		//logger.info("========== hexa1 : " + hexa1.trim());
		//logger.info("========== hexa2 : " + hexa2.trim());
		// html 엔티티변환 - X
		
		hexa1 = StringEscapeUtils.escapeHtml(str_hexa1);
		hexa2 = StringEscapeUtils.escapeHtml(str_hexa2);
		logger.info("========== hexa1 - StringEscapeUtils.escapeHtml : " + hexa1.trim());
		logger.info("========== hexa2 - StringEscapeUtils.escapeHtml : " + hexa2.trim());
		
		Iterator<String> ir = hm.keySet().iterator();
		while (ir.hasNext()) {
			String sKey = (String) ir.next();
			String sValue = (String) hm.get(sKey);
			if (bFirst) {
				//formData += sKey + "" + encode(sValue) + "";
				// 2022-08-19 - 빈칸으로 대체 처리
				//formData += sKey + " " + encode(sValue) + " ";
				// 2022-08-19 - hexa 값으로 대체 처리 
				formData += sKey + hexa1 + encode(sValue) + hexa2;
			} else {
				//formData += "00000010" + sKey + "" + encode(sValue) + "";
				// 2022-08-19 - 빈칸으로 대체 처리
				//formData += "00000010" + sKey + " " + encode(sValue) + " ";
				// 2022-08-19 - hexa 값으로 대체 처리 
				formData += "00000010" + sKey + hexa1 + encode(sValue) + hexa2;
				bFirst = true;
			}
			
		}
		return formData;
	}

	// 문자열을 헥사 스트링으로 변환하는 메서드
	public static String stringToHex(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i++) {
			result += String.format("%02X ", (int) s.charAt(i));
		}
		return result;
	}

	// 헥사 접두사 "0x" 붙이는 버전
	public static String stringToHex0x(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i++) {
			result += String.format("0x%02X ", (int) s.charAt(i));
		}
		return result;
	}
	  
	private String convert(byte[] str, String encoding) {
		String s = "";
		ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
		try {
			requestOutputStream.write(str);
			s = requestOutputStream.toString(encoding);
		} catch (Exception e) {
		}
		return s;
	}
	  
	private String encode(String s) {
		try {
			s = URLEncoder.encode(s, "euc-kr");
		} catch (Exception e) {
		}
		return s;
	}

}
