package com.pgmate.pay.main;

import java.util.Date;
import java.util.Iterator;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.Proc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * @author Administrator
 *
 */
public class C3Runner extends AbstractVerticle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.main.C3Runner.class );
	private static String verticle = "com.pgmate.pay.main.C3Verticle";
	
	@Override
	public void start(Future<Void> future) {
		Future<String> future1 = Future.future();
		
		DeploymentOptions options = new DeploymentOptions()
		.setWorker(true)
		.setInstances(30)
		.setWorkerPoolName("API")
		.setWorkerPoolSize(40);
		
		vertx.deployVerticle(verticle, options, future1.completer());
		
		future1.setHandler(ar -> {
			if (ar.succeeded()) {
				//logger.info(verticle + " deployed  ");
		
		    } else {
		        String cause = ar.cause() == null ? "" : ar.cause().getMessage();
		        //logger.info(verticle + " deployment failed {}  "+cause);
		        if (!future.failed())
		            future.fail(cause);
		    }
		});
	}
	

	public static SharedMap<String,SharedMap> cardcodeList = new SharedMap<String,SharedMap>();
	
	public static void main(String[] args) {
		TrxDAO trxDao = new TrxDAO();
		trxDao.LoadProcessCntFromDB();
		trxDao.getServerMode();
		
	    // 카드사 코드,이름 리스트 - API 전달용 메모리 저장
	    System.out.println("");
	    System.out.println("cardcodeList : " + cardcodeList);
	    System.out.println("cardcodeList : " + cardcodeList.size());
	    System.out.println("");
	    if (cardcodeList.size() == 0l) {
	    	cardcodeList = trxDao.selectCardCodeList();
	    	System.out.println("cardcodeList size : " + cardcodeList.size());
	    }
	 // 카드사 코드,이름 리스트 - API 전달용 메모리 저장
	    
	    // 결제내역 - 즉시취소를 위한 데이터 정리 호출
		Timer t = new Timer();
		Date date = new Date();
		TrxIdScheduler trxIdSchduler = new TrxIdScheduler();
		t.scheduleAtFixedRate(trxIdSchduler, 1000 * 1, 1000 * 60 * 5);
	    
	    Vertx vertx = Vertx.vertx();
	    vertx.deployVerticle(new C3Runner());
	}
	
}
