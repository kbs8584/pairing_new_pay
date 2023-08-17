package com.pgmate.pay.proc;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.conf.ConfigLoader;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.util.APICall;
import com.pgmate.pay.util.KisaSeedCBC;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcBillPay extends Proc {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcBillPay.class );
	private static String PAY_URL   = ""; 
	private SharedMap<String,Object> mchtMngBillMap = null;
	private SharedMap<String,Object> billBoxMap = null;
	private SharedMap<String,Object> billUserMap = null;
	

	public ProcBillPay() {
		if(ProcBillPay.PAY_URL == null) {
			
			VertXConfigBean conf = ConfigLoader.getConfig().vertx;
			ProcBillPay.PAY_URL = "http://"+conf.getHost()+":"+conf.getPort()+PAYUNIT.API_PAY;
		}
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		response.billPay = request.billPay;
		if(response.result != null){
			
			setResponse();
			return;
		}
		
		System.out.println();
		System.out.println("##### ##### 3#### ##### ProcBillPay exec");
		System.out.println();
		
		Request req = new Request();
		Pay pay = new Pay();
		pay.trxType		= "ONTR";								//고정값
		pay.tmnId		= response.billPay.tmnId;				//필수값아님. 터미널아이디 : PAIRINGSOLUTION 에서 지정한 터미널 아이디
		pay.trackId		= response.billPay.trackId;				//주문번호
		pay.amount		= response.billPay.amount;				//거래금액
		pay.udf1		= response.billPay.udf1;				//가맹점 정의영역1
		pay.udf2		= response.billPay.udf2;				//가맹점 정의영역2
		pay.payerName	= billUserMap.getString("payerName");	//결제자 이름
		pay.payerEmail	= billUserMap.getString("payerEmail");	//결제자 이메일
		pay.payerTel	= billUserMap.getString("payerTel");	//결제자 전화번호
		
		pay.card		= new Card();					
		pay.card.number = billBoxMap.getString("number");	
		pay.card.expiry = billBoxMap.getString("expiry").substring(2);				//유효기간 YYMM
		pay.card.installment = response.billPay.installment;	//할부기간 
		
		//상품 수량 또는 종류에 따라 다중 입력 단. 1개의 상품은 반드시 입력 바람.
		pay.products 	= new ArrayList<Product>();
		Product product = new Product();
		product.name= response.billPay.billId;										//결제 상품명
		product.qty = 1;										//결제 상품 수량
		product.price = response.billPay.amount;									//결제 상품 가격
		product.desc = "정기과금 "+response.billPay.summary;
		pay.products.add(product);
		
		req.pay = pay;
		
		Response res =  new APICall().comm(req,mchtTmnMap.getString("payKey"),ProcBillPay.PAY_URL);
		
		response.result = res.result;
		if(res.pay != null) {//trxId 를 결제 거래번호로 변경한다.
			response.billPay.trxId = res.pay.trxId;
			response.billPay.authCd = res.pay.authCd;
			response.billPay.trxDate = res.pay.trxDate;
			response.billPay.summary = "";
		}
		
		
		if(!mchtMngBillMap.isNullOrSpace("hookAddr")){
			new ThreadWebHook(mchtMngBillMap.getString("hookAddr"),response).start();
		}
	
		setResponse();
		
		return;
	}


	@Override
	public void valid() {
	
		if(request.billPay == null ){
			response.result = ResultUtil.getResult("9999", "필수값없음","결제정보 및 카드 정보가 없습니다.");return;
		}
		
		request.billPay.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		request.billPay.trxDate = sharedMap.getString(PAYUNIT.REG_DATE);
		
		if(CommonUtil.isNullOrSpace(request.pay.tmnId)){
			request.billPay.tmnId = sharedMap.getString("tmnId");
		}else{
			request.billPay.tmnId = request.billPay.tmnId.trim();
		}
		
		mchtMngBillMap = trxDAO.getMchtMngBillByMchtId(mchtMap.getString("mchtId"));
		if(mchtMngBillMap == null || !mchtMngBillMap.isEquals("status", "사용")) {
			response.result = ResultUtil.getResult("9999","미등록가맹점","정기과금이 등록되지 않았습니다.");
		}
		
		if(!mchtMngBillMap.isEquals("tmnId", request.billPay.tmnId)) {
			response.result = ResultUtil.getResult("9999","설정오류","정기과금 등록된 터미널 아이디가 아닙니다.");
		}
		
		if(CommonUtil.isNullOrSpace(request.billPay.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.billPay.billId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","과금번호가 입력되지 않았습니다.");return;
		}
		
		if(request.billPay.amount < 1000){
			response.result = ResultUtil.getResult("9999", "결제 최소 금액 오류","1000원 미만은 결제를 허용하지 않습니다.");return;
		}
		
		billUserMap = trxDAO.getBillUser(request.billPay.billId);
		if(billUserMap == null) {
			response.result = ResultUtil.getResult("9999","과금사용자오류","정기과금 서비스에 등록된 사용자가 아닙니다.");
		}
		
		if(!billUserMap.isEquals("mchtId", mchtMap.getString("mchtId"))) {
			response.result = ResultUtil.getResult("9999","과금번호오류","과금번호가 잘못되었습니다. 과금번호를 확인하시기 바랍니다.");
		}
		
		if(!billUserMap.isEquals("status", "사용")) {
			response.result = ResultUtil.getResult("9999","과금사용자오류","정기과금  서비스가 "+billUserMap.getString("status")+" 인 회원입니다.");
		}

		
		
		if(billUserMap.getLong("expireAt") < CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"))) {
			response.result = ResultUtil.getResult("9999","과금유효기간경과","과금유효기간이 경과된 사용자입니다. 갱신 또는 다시 등록하여 주시기 바랍니다.");
		}
		
		
		billBoxMap =  trxDAO.getBillBox(request.billPay.billId);
		
		if(billBoxMap == null) {
			response.result = ResultUtil.getResult("9999","과금정보오류","정기과금에 등록된 결제수단 정보가 없습니다.");
		}
		
		
		if(!billBoxMap.isEquals("boxStatus", "사용")) {
			response.result = ResultUtil.getResult("9999","과금정보오류","정기과금 결제수단이  "+billBoxMap.getString("status")+"입니다.");
		}

		
		
		if(billBoxMap.getLong("expiry") < CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMM"))) {
			response.result = ResultUtil.getResult("9999","과금정보오류","등록된 카드가 만료되었습니다. 갱신 또는 신규 등록하여 주시기 바랍니다.");
		}
		
		String number  = "";
		try {
			byte[] dec = billBoxMap.getString("box").getBytes();
			number = new String(KisaSeedCBC.SEED_CBC_Decrypt(CommonUtil.byteFiller(mchtMngBillMap.getString("cryptoKey"),16).getBytes(), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16), dec, 0, dec.length)).trim();
		}catch(Exception e) {
			
		}
		if(CommonUtil.isNullOrSpace(number)) {
			response.result = ResultUtil.getResult("9999","과금정보오류","등록된 카드가 복호화 오류가 발생하였습니다. 관리자에 문의하여 주시기 바랍니다.");
		}else {
			billBoxMap.put("number",number);
			request.billPay.brand = billBoxMap.getString("brand");
			
		}
		
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	

}
