package com.pgmate.pay.proc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.conf.ConfigLoader;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.util.APICall;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcBillUser extends Proc {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcBillUser.class );
	public static String AUTH_KEY  = "pk_893e-219f75-6f2-9bf03";		// 정기과금 인증처리 tmnid_payKey
	public static String PAY_URL   = ""; 
	public static String REFUND_URL   = ""; 
	public static long AMOUNT		= 1000;
	private SharedMap<String,Object> mchtMngBillMap = null;
	private SharedMap<String,Object> billBoxMap = null;
	private SharedMap<String,Object> billUserMap = null;
	
	public ProcBillUser() {
		if(CommonUtil.isNullOrSpace(ProcBillUser.PAY_URL)) {
			
			VertXConfigBean conf = ConfigLoader.getConfig().vertx;
			ProcBillUser.PAY_URL = "http://"+conf.getHost()+":"+conf.getPort()+PAYUNIT.API_PAY;
			ProcBillUser.REFUND_URL = "http://"+conf.getHost()+":"+conf.getPort()+PAYUNIT.API_REFUND;
			
		}
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		
		if(response.result != null){
			setResponse();
			return;
		}
		
		executeAuth();
		
		trxDAO.insertBillUser(sharedMap, request.widget);
		
		SharedMap<String,Object> resWidget = new SharedMap<String,Object>();
		
		resWidget.put("billId",request.widget.getString("billId"));
		resWidget.put("mchtId",sharedMap.getString(PAYUNIT.MCHTID));
		resWidget.put("trackId",request.widget.getString("trackId"));
		resWidget.put("payerId",request.widget.getString("payerId"));
		resWidget.put("expireAt",request.widget.getString("expireAt"));
		resWidget.put("udf1",request.widget.getString("udf1"));
		resWidget.put("udf2",request.widget.getString("udf2"));
		resWidget.put("regDate",sharedMap.getString(PAYUNIT.REG_DATE));
		resWidget.put("brand",request.widget.getString("brand"));
	
		
		if(!mchtMngBillMap.isNullOrSpace("hookAddr")){
			new ThreadWebHook(mchtMngBillMap.getString("hookAddr"),response).start();
		}
	
		setResponse();
		
		return;
	}


	@Override
	public void valid() {
	
		if(request.widget == null ){
			response.result = ResultUtil.getResult("9999", "필수값없음","과금 등록정보가 없습니다.");return;
		}
		
		SharedMap<String,Object> widget = request.widget;
		
		if(widget.isNullOrSpace("number")) {
			response.result = ResultUtil.getResult("9999", "필수값없음","카드정보가 입력되지 않았습니다.");return;
		}else {
			int cardLength =  widget.getString("number").length();
			if(cardLength < 14 || 16 < cardLength){
				response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.","카드번호는 14~16자리만 허용합니다.");return;
			}
		}
		String expiry = widget.getString("expiry");
		if(expiry.length() == 4) {
			if(CommonUtil.parseInt(expiry.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
				response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");return;
			}
			if(CommonUtil.parseInt(expiry.substring(2,4)) > 12){
				response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");return;
			}
		}else {
			response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");return;
		}
		
		
		if(widget.isNullOrSpace("trackId")) {
			response.result = ResultUtil.getResult("9999", "필수값 없음","trackId 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
		}else if(widget.isNullOrSpace("payerName")) {
			response.result = ResultUtil.getResult("9999", "필수값 없음","payerName 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
		}else if(widget.isNullOrSpace("payerTel")) {
			response.result = ResultUtil.getResult("9999", "필수값 없음","payerTel 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
		}else if(widget.isNullOrSpace("payerId")) {
			response.result = ResultUtil.getResult("9999", "필수값 없음","payerId 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
		}else {
			
		}
		
		mchtMngBillMap = trxDAO.getMchtMngBillMap(mchtTmnMap.getString("mchtId"), mchtTmnMap.getString("tmnId"));
		
		if(!mchtMngBillMap.isEquals("status","사용")) {
			response.result = ResultUtil.getResult("9999", "유효성오류","정기과금 서비스가 시작되지 않았습니다. 관리자에게 문의하여 주시기 바랍니다.");return;
		}
		
		//위젯 콜로 연결해야 함.
		if(trxDAO.isDuplicatedBillTrackId(mchtTmnMap.getString("mchtId"), widget.getString("trackId"))) {
			response.result = ResultUtil.getResult("9999", "중복오류","이미 사용한 주문번호 입니다. ");return;
		}
		if(trxDAO.isDuplicatedBillPayerId(mchtTmnMap.getString("mchtId"), widget.getString("payerId"))) {
			response.result = ResultUtil.getResult("9999", "유효성오류","이미 사용한 가맹점 고객 아이디입니다. ");return;
		}
		

		widget.put("billId","B"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 6));
		widget.put("expireAt",CommonUtil.getOpDate(Calendar.DATE, mchtMngBillMap.getInt("expireSet"), sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8)));
		widget.put("brand",trxDAO.getDBIssuer(widget.getString("number").substring(0,6)).getString("brand"));
		
		
		
	}
	
	
	public void executeAuth() {
		Request req = new Request();
		Pay pay = new Pay();
		pay.trxType		= "ONTR";								//고정값
		pay.trackId		= request.widget.getString("billId");	//주문번호
		pay.amount		= AMOUNT;				//거래금액
		pay.udf1		= request.widget.getString("payerId");		//가맹점 정의영역1
		pay.udf2		= request.widget.getString("udf1");				//가맹점 정의영역2
		pay.payerName	= request.widget.getString("payerName");	//결제자 이름
		pay.payerEmail	= request.widget.getString("payerEmail");	//결제자 이메일
		pay.payerTel	= request.widget.getString("payerTel");	//결제자 전화번호
		
		pay.card		= new Card();					
		pay.card.number = request.widget.getString("number");	
		pay.card.expiry = request.widget.getString("expiry");				//유효기간 YYMM
		pay.card.installment = 0;	//할부기간 
		
		//상품 수량 또는 종류에 따라 다중 입력 단. 1개의 상품은 반드시 입력 바람.
		pay.products 	= new ArrayList<Product>();
		Product product = new Product();
		product.name= request.widget.getString("billId");									//결제 상품명
		product.qty = 1;										//결제 상품 수량
		product.price = AMOUNT;									//결제 상품 가격
		product.desc = "정기인증 ";
		pay.products.add(product);
		
		req.pay = pay;
		
		Response res =  new APICall().comm(req,ProcBillUser.AUTH_KEY,PAY_URL);
		
		
		if(res.result.resultCd.equals("0000") && !CommonUtil.isNullOrSpace(res.pay.authCd)) {	//결제 성공시
			request.widget.put("authTrxId",res.pay.trxId);
			request.widget.put("authCd",res.pay.authCd);
			request.widget.put("status","사용");
			
			new ProcBillRefund(res.pay.trxId,res.pay.trackId+"R").start();
			response.widget = request.widget;
			response.result = ResultUtil.getResult("0000", "성공","인증완료");return;
			
		}else {
			request.widget.put("authTrxId",res.pay.trxId);
			request.widget.put("resultMsg",res.result.advanceMsg);
			request.widget.put("status","실패");
			response.widget = request.widget;
			response.result = ResultUtil.getResult("9999", "카드번호 인증실패","인증 실패 : "+res.result.advanceMsg);return;
		}
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	
	
	
	
	

}
