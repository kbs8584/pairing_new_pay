package com.pgmate.pay.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.validation.StringValidator;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.lib.vertx.main.RouteWorker;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXRoute;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Listener extends RouteWorker {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.Listener.class);

	@Override
	public void execute(VertXConfigBean vertxConfig, Router router) {
//		System.out.println();
//		logger.info("========== execute() - BEGIN");
		//1. CORS
		VertXRoute.setCorsHandler(router);

		//2. Body Handler
		VertXRoute.setBodyHandler(router);

		//3. "/" Handler  
		router.route(PAYUNIT.ROUTE_ROOT).handler(rc -> {
			VertXMessage.set200(rc, "Payment API [" + VertXUtil.getXRealIp(rc) + "]");
		});

		//4. "favicon.ico" Handler  
		//router.route().handler(FaviconHandler.create(vertxConfig.getWebroot() + "/favicon.ico"));

		//5. crossdomain.xml 
		router.route(PAYUNIT.ROUTE_CROSSDOMAIN).handler(rc -> {
			VertXMessage.setCrossDomain(rc);
		});

		//6. static handler 분기 처리 
		if (vertxConfig.getHost().indexOf("192.168.48") > -1 || vertxConfig.getHost().indexOf(PAYUNIT.PAY_HOST_LIVE) > -1) {
			logger.debug("========== execute() - API MODE ROUTE ENABLE CACHE");
			VertXRoute.setAPIStaticHander(router, "/static/*", vertxConfig.getWebroot() + "/static");
		} else {
			logger.debug("========== execute() - DEV MODE ROUTE DISABLE CACHE");
			VertXRoute.setStaticHander(router, "/static/*", vertxConfig.getWebroot() + "/static");
		}

		//8. "/api/webhooks/*" route
		router.route(PAYUNIT.ROUTE_API_WEBHOOKS).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", PAYUNIT.ROUTE_API_WEBHOOKS, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//9. "/api/redirect/*" route
		router.route(PAYUNIT.ROUTE_API_REDIRECT).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", PAYUNIT.ROUTE_API_REDIRECT, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//7. "/api/*" route
		router.route(PAYUNIT.ROUTE_API).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", PAYUNIT.ROUTE_API, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//10. HANDLER 
		router.route(PAYUNIT.ROUTE_FORM).handler(this::formHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", PAYUNIT.ROUTE_FORM, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//11. HANDLER 
		router.route(PAYUNIT.ROUTE_JS).handler(this::jsHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", PAYUNIT.ROUTE_JS, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		// TEST 
		router.route("/static/*").handler(this::jsHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("========== execute() - {} not found ", fc.request().uri());
			} else {
				logger.error("========== execute() - {} error : {},{}", "/static/*", fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//12. "/*" 그 외 모든 것은 404 not found 
		router.route(PAYUNIT.ROUTE_NOT_FOUND).handler(rc -> {
			log(rc);
			String uri = CommonUtil.nToB(rc.request().uri());

			if (StringValidator.isInclude(uri, PAYUNIT.ROUTE_IGNORE)) {
				VertXMessage.set404EmptryLog(rc);
			} else {
				logger.info("========== execute() - deny uri : {}, ip : {}", rc.request().uri(), VertXUtil.getRemoteIp(rc));
				MultiMap headers = rc.request().headers();
				for (String key : headers.names()) {
					logger.debug("========== execute() - Header {},{}", key, CommonUtil.nToB(headers.get(key)));
				}
				VertXMessage.set404EmptryLog(rc);
			}
		});
		
	}

	private void apiHandler(RoutingContext rc) {
		System.out.println();
		logger.info("========== apiHandler");
		log(rc);
		new Api().apiHandler(rc);
	}

	private void formHandler(RoutingContext rc) {
		System.out.println();
		logger.info("========== formHandler");
		log(rc);
		new Form().formHandler(rc);
	}

	private void jsHandler(RoutingContext rc) {
		System.out.println();
		logger.info("========== jsHandler");
		log(rc);
		new Js().jsHandler(rc);
	}

	private void log(RoutingContext rc) {
		logger.info("========== log() - {}", VertXUtil.getAccessLog(rc));
	}

}
