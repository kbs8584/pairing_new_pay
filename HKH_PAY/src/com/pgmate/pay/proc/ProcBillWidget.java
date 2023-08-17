package com.pgmate.pay.proc;

import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.APIPath;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

/**
 * @author Administrator
 *
 */
public class ProcBillWidget {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcBillWidget.class );
	private static HandlebarsTemplateEngine engine 	= null;
	private static String directory 				= null;
	private static String htmlLocation				= "/form/bill/index";
	private static String ERROR = "<html><head><title>PAYMENT API</title></head><body>resulMsg : RESULT, advanceMsg : ADVANCE</body></html>";
	
	
	
	private TrxDAO trxDAO 	= null;
	private Result result = null;
	private RoutingContext rc = null;
	private SharedMap<String,Object> sharedMap = new SharedMap<String,Object>();
	private SharedMap<String,Object> billWidgetMap = new SharedMap<String,Object>();
	private SharedMap<String,Object> mchtBillMap = new SharedMap<String,Object>();
	private SharedMap<String,Object> mchtTmnMap = new SharedMap<String,Object>();
	
	
	
	public ProcBillWidget() {
		trxDAO = new TrxDAO();
		if(engine == null){
			engine = HandlebarsTemplateEngine.create();
			engine.setMaxCacheSize(PAYUNIT.HANDLER_STATUC_CACHE);
			engine.setExtension("html");
		}
		if(directory == null){
			directory =PropertyUtil.getJavaProperty("user.dir").replaceAll("bin", "war");
		}
	}


	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		System.out.println();
		System.out.println("##### ##### 3#### ##### ProcBillWidget exec");
		System.out.println();
		
		logger.info("BLL WIDGET CALL : "+sharedMap.getString(PAYUNIT.REMOTEIP));
		valid();
		
		if(result != null) {
			logger.info("bill request : {}",GsonUtil.toJson(billWidgetMap, true, ""));
			logger.info("resultMsg : {}, advanceMsg : {}",result.resultMsg,result.advanceMsg);
			VertXMessage.setTemplate(rc, ERROR.replaceAll("RESULT",result.resultMsg ).replaceAll("ADVANCE",result.advanceMsg));
		}else {
			for( String key : billWidgetMap.keySet() ){
				rc.put(key, billWidgetMap.get(key));
				System.out.println( String.format("%s, 값 : %s", key, billWidgetMap.get(key)) );
			}
			APIPath.setPath(rc);

			engine.render(rc, directory+htmlLocation,  res -> {
			    if (res.succeeded()) {
			    	rc.response().setStatusCode(200).end(res.result());
			    }else {
			    	logger.info("api uri : {}, method : {}, ip : {},{}",CommonUtil.nToB(rc.request().uri()),VertXUtil.getMethod(rc),VertXUtil.getRemoteIp(rc),directory);
			        rc.fail(res.cause());
			    }
		    });
		}
	}


	
	public void valid() {
		
		if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){
			
			
			
			String[] st = sharedMap.getString(PAYUNIT.PAYLOAD).split("&");
			for (int i = 0; i < st.length; i++) {
				int index = st[i].indexOf('=');
				if (index > 0){
					String key = st[i].substring(0, index);
					try {
					billWidgetMap.put(key, URLDecoder.decode(st[i].substring(index + 1),"utf-8"));
					}catch(Exception e) {}
				}
			}
			/*수취정보 : 
			
			payKey 	=  결제에 필요한 KEY
			trackId	=  주문번호
			payerNAme	= 
			payerEmail
			payerTel 
			payerId = ;
			udf1 
			udf2
			summary 
			redirectUrl = 
			*/
			
			if(billWidgetMap.isNullOrSpace("payKey")) {
				result = ResultUtil.getResult("9999", "필수값 없음","payKey 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
			}else if(billWidgetMap.isNullOrSpace("trackId")) {
				result = ResultUtil.getResult("9999", "필수값 없음","trackId 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
			}else if(billWidgetMap.isNullOrSpace("payerName")) {
				result = ResultUtil.getResult("9999", "필수값 없음","payerName 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
			}else if(billWidgetMap.isNullOrSpace("payerTel")) {
				result = ResultUtil.getResult("9999", "필수값 없음","payerTel 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
			}else if(billWidgetMap.isNullOrSpace("payerId")) {
				result = ResultUtil.getResult("9999", "필수값 없음","payerId 정보가 없습니다. 관리자에 문의하여 주시기 바랍니다.");return;
			}else if(billWidgetMap.isNullOrSpace("redirectUrl")) {
				result = ResultUtil.getResult("9999", "필수값 없음","결제 전달 redirectUrl 이 없습니다.");return;
			}else {
				mchtTmnMap = trxDAO.getMchtTmnByPayKey(billWidgetMap.getString("payKey"));
				
				if(mchtTmnMap == null || mchtTmnMap.isNullOrSpace("mchtId")) {
					result = ResultUtil.getResult("9999", "유효성오류","payKey 정보가 일치하지 않습니다. 관리자에게 문의하여 주시기 바랍니다.");return;
				}
				
				if(!mchtTmnMap.isEquals("status","사용")) {
					result = ResultUtil.getResult("9999", "유효성오류","요청하신 터미널이 활성화 되지 않았습니다. 관리자에게 문의하여 주시기 바랍니다.");return;
				}
				
				mchtBillMap = trxDAO.getMchtMngBillMap(mchtTmnMap.getString("mchtId"),mchtTmnMap.getString("tmnId"));
				if(!mchtTmnMap.isEquals("tmnId",mchtBillMap.getString("tmnId"))) {
					result = ResultUtil.getResult("9999", "유효성오류","정기과금 서비스에 등록되지 않았습니다. 관리자에게 문의하여 주시기 바랍니다.");return;
				}
				
				if(!mchtBillMap.isEquals("status","사용")) {
					result = ResultUtil.getResult("9999", "유효성오류","정기과금 서비스가 시작되지 않았습니다. 관리자에게 문의하여 주시기 바랍니다.");return;
				}
				billWidgetMap.put("title", mchtBillMap.getString("title"));
				billWidgetMap.put("terms", mchtBillMap.getString("terms"));
				
				SharedMap<String, Object> mchtMap =  trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
				billWidgetMap.put("mchtName", mchtMap.getString("name"));
			}
			
		}else{
			result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
		}
				
	}
	


}


