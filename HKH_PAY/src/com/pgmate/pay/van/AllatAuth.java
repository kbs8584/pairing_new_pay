package com.pgmate.pay.van;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.main.Api;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.AllatAuthUtil;




/**
 * @author Administrator
 *
 */
public class AllatAuth implements Van {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.AllatAuth.class);

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
	
	public AllatAuth() {
		
	}

	public AllatAuth(SharedMap<String, Object> vanMap) {
		SHOPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
		VAN	= vanMap.getString("van").trim();
		
		logger.info("========== ========== ========== ========== ========== ========== ========== ");
		logger.info("========== AllatAuth SHOPID vanId : " + SHOPID);
		logger.info("========== AllatAuth CRYPTOKEY : " + CRYPTOKEY);
		logger.info("========== AllatAuth VAN : " + VAN);
		logger.info("========== ========== ========== ========== ========== ========== ========== ");
	}
	
	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		logger.info("========== ========== ========== ========== AllatAuth sales()");
		logger.info("#### AllatAuth TRANSACTION ####");
		String strReq = "";
		strReq  ="allat_shop_id=" + SHOPID;
		strReq +="&allat_amt="			+ CommonUtil.toString(response.pay.amount);
		strReq +="&allat_enc_data="		+ CommonUtil.toString(response.pay.encData);
		strReq +="&allat_cross_key="	+ CRYPTOKEY;
		logger.info("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - strReq :: " + strReq);
		
		////////////////////////////////////
		// 올앳 결제 서버와 통신  : AllatUtil.approvalReq->통신함수, HashMap->결과값
		AllatAuthUtil util = new AllatAuthUtil();
		HashMap hm     = null;
		hm = util.approvalReq(strReq, "NOSSL");
		//hm = util.approvalReq(strReq, "SSL");
		// 결제 결과 값 확인
		//------------------
		String sReplyCd     = (String)hm.get("reply_cd");
		String sReplyMsg    = (String)hm.get("reply_msg");
		/* 결과값 처리
		  --------------------------------------------------------------------------
		     결과 값이 '0000'이면 정상임. 단, allat_test_yn=Y 일경우 '0001'이 정상임.
		     실제 결제   : allat_test_yn=N 일 경우 reply_cd=0000 이면 정상
		     테스트 결제 : allat_test_yn=Y 일 경우 reply_cd=0001 이면 정상
		  --------------------------------------------------------------------------*/
		logger.info("========== ========== ========== sales() - hm : " + hm.toString());
		if (sReplyCd.equals("0000")) {
			// reply_cd "0000" 일때만 성공
			String sOrderNo        = (String)hm.get("order_no");
			String sAmt            = (String)hm.get("amt");
			String sPayType        = (String)hm.get("pay_type");
			String sApprovalYmdHms = (String)hm.get("approval_ymdhms");
			String sSeqNo          = (String)hm.get("seq_no");
			String sApprovalNo     = (String)hm.get("approval_no");
			String sCardId         = (String)hm.get("card_id");
			String sCardNm         = (String)hm.get("card_nm");
			String sSellMm         = (String)hm.get("sell_mm");
			String sZerofeeYn      = (String)hm.get("zerofee_yn");
			String sCertYn         = (String)hm.get("cert_yn");
			String sContractYn     = (String)hm.get("contract_yn");
			String sSaveAmt        = (String)hm.get("save_amt");
			String sBankId         = (String)hm.get("bank_id");
			String sBankNm         = (String)hm.get("bank_nm");
			String sCashBillNo     = (String)hm.get("cash_bill_no");
			String sCashApprovalNo = (String)hm.get("cash_approval_no");
			String sEscrowYn       = (String)hm.get("escrow_yn");
			String sAccountNo      = (String)hm.get("account_no");
			String sAccountNm      = (String)hm.get("account_nm");
			String sIncomeAccNm    = (String)hm.get("income_account_nm");
			String sIncomeLimitYmd = (String)hm.get("income_limit_ymd");
			String sIncomeExpectYmd= (String)hm.get("income_expect_ymd");
			String sCashYn         = (String)hm.get("cash_yn");
			String sHpId           = (String)hm.get("hp_id");
			String sTicketId       = (String)hm.get("ticket_id");
			String sTicketPayType  = (String)hm.get("ticket_pay_type");
			String sTicketNm       = (String)hm.get("ticket_nm");
			String sPointAmt       = (String)hm.get("point_amt");

			logger.info("========== 결과코드               : " + sReplyCd          + "<br>");
			logger.info("========== 결과메세지             : " + sReplyMsg         + "<br>");
			logger.info("========== 주문번호               : " + sOrderNo          + "<br>");
			logger.info("========== 승인금액               : " + sAmt              + "<br>");
			logger.info("========== 지불수단               : " + sPayType          + "<br>");
			logger.info("========== 승인일시               : " + sApprovalYmdHms   + "<br>");
			logger.info("========== 거래일련번호           : " + sSeqNo            + "<br>");
			logger.info("========== 에스크로 적용 여부     : " + sEscrowYn         + "<br>");
			logger.info("========== ==================== 신용 카드 ===================<br>");
			logger.info("========== 승인번호               : " + sApprovalNo       + "<br>");
			logger.info("========== 카드ID                 : " + sCardId           + "<br>");
			logger.info("========== 카드명                 : " + sCardNm           + "<br>");
			logger.info("========== 할부개월               : " + sSellMm           + "<br>");
			logger.info("========== 무이자여부             : " + sZerofeeYn        + "<br>");   //무이자(Y),일시불(N)
			logger.info("========== 인증여부               : " + sCertYn           + "<br>");   //인증(Y),미인증(N)
			logger.info("========== 직가맹여부             : " + sContractYn       + "<br>");   //3자가맹점(Y),대표가맹점(N)
			logger.info("========== 세이브 결제 금액       : " + sSaveAmt          + "<br>");
			logger.info("========== 포인트 결제 금액       : " + sPointAmt         + "<br>");
			logger.info("========== =============== 계좌 이체 / 가상계좌 =============<br>");
			logger.info("========== 은행ID                 : " + sBankId           + "<br>");
			logger.info("========== 은행명                 : " + sBankNm           + "<br>");
			logger.info("========== 현금영수증 일련 번호   : " + sCashBillNo       + "<br>");
			logger.info("========== 현금영수증 승인 번호   : " + sCashApprovalNo   + "<br>");
			logger.info("========== ===================== 가상계좌 ===================<br>");
			logger.info("========== 계좌번호               : " + sAccountNo        + "<br>");
			logger.info("========== 입금 계좌명            : " + sIncomeAccNm      + "<br>");
			logger.info("========== 입금자명               : " + sAccountNm        + "<br>");
			logger.info("========== 입금기한일             : " + sIncomeLimitYmd   + "<br>");
			logger.info("========== 입금예정일             : " + sIncomeExpectYmd  + "<br>");
			logger.info("========== 현금영수증신청 여부    : " + sCashYn           + "<br>");
			logger.info("========== ===================== 휴대폰 결제 ================<br>");
			logger.info("========== 이동통신사구분         : " + sHpId             + "<br>");
			logger.info("========== ===================== 상품권 결제 ================<br>");
			logger.info("========== 상품권ID               :" + sTicketId          + "<br>");
			logger.info("========== 상품권 이름            :" + sTicketPayType     + "<br>");
			logger.info("========== 결제구분               :" + sTicketNm          + "<br>");
			
			// 배포본에서는 제외 시킬것 //////////////////////////////////////////
			String sPartcancelYn  = (String)hm.get("partcancel_yn");
			String sBCCertNo      = (String)hm.get("bc_cert_no");
			String sCardNo        = (String)hm.get("card_no");
			String sIspFullCardCd = (String)hm.get("isp_full_card_cd");
			String sCardType      = (String)hm.get("card_type");
			String sBankAccountNm = (String)hm.get("bank_account_nm");
			logger.info("========== ===================== 배포본제외 ================<br>");
			logger.info("========== 신용카드 부분취소가능여부 : " + sPartcancelYn  + "<br>"); 
			logger.info("========== BC인증번호                : " + sBCCertNo      + "<br>");
			logger.info("========== 카드번호 Return           : " + sCardNo        + "<br>");
			logger.info("========== ISP 전체 카드코드         : " + sIspFullCardCd + "<br>");
			logger.info("========== 카드구분                  : " + sCardType      + "<br>");
			logger.info("========== 계좌이체 예금주명         : " + sBankAccountNm + "<br>");
			//////////////////////////////////////////////////////////////////////
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
			response.pay.authCd = sApprovalNo;
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", SHOPID);
			sharedMap.put("vanTrxId", sSeqNo);
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상승인");
			sharedMap.put("vanDate", sApprovalYmdHms);
			sharedMap.put("authCd", sApprovalNo);
			logger.info("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - 정상 : 정상승인");
		} else if (sReplyCd.equals("0001")) {
			logger.info("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - 실패 : 테스트성공");
			logger.debug("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - 테스트성공 : sReplyCd : " + sReplyCd + " | sReplyMsg : " + sReplyMsg);
			response.result = ResultUtil.getResult(sReplyCd, "실패", sReplyMsg);
	//		sharedMap.put("code", sReplyCd);
	//		sharedMap.put("status", sReplyMsg);
	//		sharedMap.put("msg", sReplyMsg);
			
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", SHOPID);
			//sharedMap.put("vanTrxId", sharedMap.get("vanTrxId"));
			sharedMap.put("vanResultCd", sReplyCd);
			sharedMap.put("vanResultMsg", sReplyMsg);
			sharedMap.put("vanDate", CommonUtil.getCurrentDate("yyyyMMdd"));
			//sharedMap.put("authCd", sharedMap.get("authCd"));
		} else {
			logger.info("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - 실패 : 승인실패");
			logger.debug("========== response.pay.trxId : " + response.pay.trxId + " ========== sales() - 승인실패 : sReplyCd : " + sReplyCd + " | sReplyMsg : " + sReplyMsg);
			response.result = ResultUtil.getResult(sReplyCd, "실패", sReplyMsg);
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", SHOPID);
			//sharedMap.put("vanTrxId", sharedMap.get("vanTrxId"));
			sharedMap.put("vanResultCd", sReplyCd);
			sharedMap.put("vanResultMsg", sReplyMsg);
			sharedMap.put("vanDate", CommonUtil.getCurrentDate("yyyyMMdd"));
			//sharedMap.put("authCd", sharedMap.get("authCd"));
		}

		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		logger.info("#### AllatAuth TRANSACTION ####");
		logger.info("sharedMap : " + sharedMap.toString());
		logger.info("payMap : " + payMap.toString());
		logger.info("response : " + response.toString());
		
		response.refund.authCd = "10"+CommonUtil.getCurrentDate("HHmmss");
		response.result 	= ResultUtil.getResult("0000","정상","승인취소");
		sharedMap.put("van", "ALLAT");
		sharedMap.put("vanTrxId", sharedMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd","0000");
		sharedMap.put("vanResultMsg","정상취소메시지");
		sharedMap.put("vanDate", "20" + response.pay.metadata.get("AuthDate"));
		return sharedMap;
	}
	  
}
