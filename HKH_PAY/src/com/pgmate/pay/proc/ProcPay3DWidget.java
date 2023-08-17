package com.pgmate.pay.proc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPay3DWidget extends Proc {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay3DWidget.class );
	private String widgetKey 		= "";			
	
	public ProcPay3DWidget() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		
		if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){
			
			if(request.widget == null){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.Widget  오류");return;
			}else{
				
				widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
				
				SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
				ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
				ioMap.put("widgetKey", widgetKey);
				ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
				ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
				ioMap.put("trackId", request.widget.getString("trackId"));
				ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
				ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
				detectDevice();
				ioMap.put("device", request.widget.getString("device"));
				ioMap.put("reqJson", GsonUtil.toJson(request.widget));
				
				SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));
				
				if(mchtTmnMap.isEquals("webPay", "사용") && mchtSvcMap.isEquals("card3D", "사용")){
					
					
					SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
					if(vanMap == null || !vanMap.isEquals("status", "사용")){
						response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. route 등록 오류");
					}else{
						ioMap.put("van", vanMap.getString("van"));
						ioMap.put("vanId", vanMap.getString("vanId"));
						
						if(response.widget == null){response.widget = new SharedMap<String,Object>();}
						
						response.widget.put("device", ioMap.getString("device"));
						
						
						if(vanMap.startsWith("van", "KSPAY")){
							response.widget.put("target", "KSPAY");
							response.widget.put("routeUrl", "/form/payment/kspay/index.html?token=" + widgetKey);
							setKspay(vanMap);
							ioMap.put("reqJson", GsonUtil.toJson(request.widget));
							response.result = ResultUtil.getResult("0000", "정상","정상완료");
						}else if(vanMap.startsWith("van", "DAOU")){
							response.widget.put("target", "DAOU");
							response.widget.put("routeUrl", "/form/payment/daou/index.html?token=" + widgetKey);
							setDaou(vanMap);
							ioMap.put("reqJson", GsonUtil.toJson(request.widget));
							response.result = ResultUtil.getResult("0000", "정상","정상완료");
						}else{
							response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. 서비스 준비중인 카드사입니다.");
						}
						
					}
				}else{
					
					response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제를 사용하지 않거나 3D Secure가 등록되지 않은 가맹점입니다..관리자에 문의바랍니다.");
					
				}
				ioMap.put("resJson", GsonUtil.toJson(response.widget) );
				ioMap.put("resultCd", response.result.resultCd);
				ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
				trxDAO.insertTrxIO3D(ioMap);
			}
		}else if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")){
			
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_3D_WIDGET+"/", "");
			logger.info("widget key : {}",key);
			if(!key.startsWith("key_")){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","Widget 정보 요청 실패 Invalid Key");return;
			}else{
				if(PAYUNIT.cacheMap.containsKey(key)){
					response.result = ResultUtil.getResult("0000", "정상","정상완료");
					response.widget = PAYUNIT.cacheMap.get(key); return;
				}else{
					response.result = ResultUtil.getResult("9999", "거래시간이 초과하였습니다.");return;
				}
				//20분 이상 처리를 위하여 이 부분을 DB에서 조회하여 결과를 회신할 수 도 있다.
			}
			
		}else{
			response.result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
		}
				
	}
	
	
	/**
	 * 보내는 값 
	 * products 	: 반드시 보내야 함.
	 * amount 		: 결제 금액
	 * productType	: digital, good 
	 * servicePeriod: digital 의 경우 제공 기간 YYYY/MM/DD ~ YYYY/MM/DD
	 * 3d			: option 3d 결제중 ISP/MPI 여부 
	 * 3dIssuer		: MPI : 현대/신한/삼성/롯데/하나/외환/농협/시티 , 그외에는 ISP
	 * 무이자는 다음에 하자.......
	 * @param vanMap
	 */
	public void setKspay(SharedMap<String,Object> vanMap){
		
		
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));
		
		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");
		if(request.widget.isEquals("device", "mobile")){
			request.widget.put("targetUrl", "https://kspay.ksnet.to/store/mb2/KSPayPWeb_utf8.jsp");
		}else{
			request.widget.put("targetUrl", "https://kspay.ksnet.to/store/KSPayFlashV1.3/KSPayPWeb.jsp?sndCharSet=utf-8");
		}
		request.widget.put("width", 500);
		if(request.widget.isEquals("device", "MSIE")){
			request.widget.put("height", 568);
		}else{
			request.widget.put("height", 518);
		}
		SharedMap<String,Object> form = new SharedMap<String,Object>();
		form.put("sndPaymethod", "1000000000");			//신용카드
		form.put("sndStoreid", vanMap.getString("vanId"));
		form.put("sndStoreName", mchtMap.getString("nick"));//상점명
		form.put("sndStoreDomain", "");//도메인 , REFFERE
		form.put("sndOrdernumber", sharedMap.getString(PAYUNIT.TRX_ID));
		
		form.put("sndGoodname", getProduct(request.widget.get("products")));
		form.put("sndAmount", request.widget.getString("amount"));
		form.put("sndOrdername", request.widget.getString("payerName"));
		form.put("sndEmail", request.widget.getString("payerEmail"));
		form.put("sndMobile", request.widget.getString("payerTel").replaceAll("[-]", ""));
		form.put("sndServicePeriod", request.widget.getString("servicePeriod")); //서비스 제공기간 YYYY/MM/DD ~ YYYY/MM/DD 컨텐츠의 경우 표기
		
		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
			form.put("sndReply", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_3D_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}else{
			form.put("sndReply", String.format("http://%s%s/%s/%s","pairingpayments.net:8080",PAYUNIT.API_3D_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}
		if(request.widget.isEquals("3d", "ISP")){
			form.put("sndShowcard", "I");		//카드사별 인증방식 설정 구분없으면 I,M 아니면 I,M,N,A,W
		}else if(request.widget.isEquals("3d", "MPI")){
			form.put("sndShowcard", "M");		//카드사별 인증방식 설정 구분없으면 I,M 아니면 I,M,N,A,W
		}else{
			form.put("sndShowcard", "I,M");		//카드사별 인증방식 설정 구분없으면 I,M 아니면 I,M,N,A,W
		}
		if(request.widget.isEquals("productType","digital")){
			form.put("sndGoodType", "2");		//실물 1, 컨텐츠 2
		}else{
			form.put("sndGoodType", "1");		//실물 1, 컨텐츠 2
		}
		
		form.put("sndCurrencytype", "WON");
		//할부 가능기간 설정 
		String installment = "ALL(";
		for(int i=0;i<mchtTmnMap.getInt("apiMaxInstall");i++){
			if(i != 1){
				installment+=CommonUtil.toString(i)+":";
			}
		}
		installment += mchtTmnMap.getInt("apiMaxInstall")+")";
		form.put("sndInstallmenttype", installment);
		
		
		form.put("sndInteresttype", "NONE");	//무이자 적용여부 !나중에 하자
		
		form.put("reWHCid", "");				//승인 후 수취 필드
		form.put("reWHCtype", "");				//승인 후 수취 필드
		form.put("reWHHash", "");				//승인 후 수취 필드
		
		
		
		//REDIRECT FIELD	거래번호하고 위젯 키 
		
		form.put("a", sharedMap.getString(PAYUNIT.TRX_ID));
		form.put("b",response.widget.getString("key"));
		form.put("c", mchtTmnMap.getString("tmnId"));
		
		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		
		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);
		
		
		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}
	
	
	
	public void setDaou(SharedMap<String,Object> vanMap){
		
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));
		
		request.widget.put("target", "DAOU");
		request.widget.put("targetMethod", "POPUP");
		if(request.widget.isEquals("device", "mobile")){
			request.widget.put("targetUrl", "https://ssltest2.kiwoompay.co.kr/m/creditCard/DaouCreditCardMng.jsp");
		}else{
			request.widget.put("targetUrl", "https://ssltest.kiwoompay.co.kr/creditCard/DaouCreditCardMng.jsp");
		}
		request.widget.put("width", 469);
		if(request.widget.isEquals("device", "MSIE")){
			request.widget.put("height", 507);
		}else{
			request.widget.put("height", 520);
		}
		SharedMap<String,Object> form = new SharedMap<String,Object>();
		form.put("CPID", vanMap.getString("vanId"));
		form.put("ORDERNO", sharedMap.getString(PAYUNIT.TRX_ID));
		if(request.widget.isEquals("productType","digital")){
			form.put("PRODUCTTYPE", "1");		//컨텐츠 1
		}else{
			form.put("PRODUCTTYPE", "2");		//실물 2
		}
		form.put("BILLTYPE", "1");				//일반
		form.put("TAXFREECD", "00");			//과세00 비과세 01
		form.put("AMOUNT", request.widget.getString("amount"));
		form.put("quotaopt", CommonUtil.zerofill(mchtTmnMap.getInt("apiMaxInstall"),2));
		form.put("PRODUCTNAME", getProduct(request.widget.get("products")));
		
		
		
		
		form.put("USERNAME", request.widget.getString("payerName"));
		form.put("EMAIL", request.widget.getString("payerEmail"));
		form.put("USERID", request.widget.getString("payerTel"));
		form.put("PRODUCTCODE", "");
		
		//REDIRECT FIELD	거래번호하고 위젯 키 
		form.put("RESERVEDINDEX1", sharedMap.getString(PAYUNIT.TRX_ID));
		form.put("RESERVEDINDEX2",response.widget.getString("key"));
		form.put("RESERVEDSTRING", mchtTmnMap.getString("tmnId"));
		
		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
			form.put("RETURNURL", String.format("https://%s/%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_3D_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}else{
			form.put("RETURNURL", String.format("http://%s/%s/%s/%s","pairingpayments.net:8080",PAYUNIT.API_3D_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}
		
		
		
		form.put("HOMEURL", "");
		form.put("DIRECTRESULTFLAG", "");	//Y 입력시 키움페이 결제완료 창 없이 HOMEURL 로 이동한다.
		form.put("used_card_YN", "");		//Y 일경우 카드사 노출 여부 
		form.put("used_card", "");			// used_card_YN 이 Y 일경우 해당 : 구분으로 카드사 전송
		form.put("not_used_card", "");		// 카드사 표현하고 싶지 않은 것만
		form.put("eng_flag", "");			// Y 일 경우 영문
		form.put("kcp_site_logo", request.widget.getString("widgetLogoUrl"));
		
		
		
		
		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		
		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);
		
		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}
	
	
	
	public void detectDevice(){
		String ua = sharedMap.getString(PAYUNIT.USERAGENT).toLowerCase();
		if(ua.matches("(?i).*((android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino).*")||ua.substring(0,4).matches("(?i)1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-")) {
			request.widget.put("device","mobile");
		}else{
			if(ua.indexOf("trident") > -1 || ua.indexOf("msie") > -1){
				request.widget.put("device","MSIE");
			}else if(ua.indexOf("chrome") > -1){
				request.widget.put("device","Chrome");
			}else if(ua.indexOf("opera") > -1){
				request.widget.put("device","Opera");
			}else if(ua.indexOf("safari") > -1){
				request.widget.put("device","Safari");
			}else if(ua.indexOf("firefox") > -1){
				request.widget.put("device","Firefox");
			}else{
				request.widget.put("device","pc");
			}
		}
		
	}
	
	
	public String getProduct(Object json){
		List<Product> prodList = null;
		String prodName = "상품명";
		if(json instanceof String) {
			logger.debug("product type: String");
			logger.info("products : {}",json);
			try{
				prodList = (List<Product>)GsonUtil.fromJson((String)json, new TypeToken<List<Product>>(){}.getType());
			}catch(Exception e){
				logger.info("product error : {}",e.getMessage());
			}
		} else {
			logger.debug("product type: Object");
			prodList = new GsonBuilder().create().fromJson(GsonUtil.toJson(json), new TypeToken<List<Product>>(){}.getType());
		}
		
		if(prodList.size() > 0){
			Product product = prodList.get(0);
			prodName = product.name;
			if(prodList.size() > 1){
				prodName += " 외 "+prodList.size();
			}
		}
		
		return prodName;
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}


}


