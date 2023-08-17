package com.pgmate.pay.util;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class TemplateUtil {
  public static void popupToParent3D(RoutingContext rc,String resData) {
    String sb = String.join("\n", 
    "<html><head>",
    " <meta charset=\"UTF-8\">",
    " <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
    " <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
    " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
    " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
    "<head><body>",
    " <form action=\"javascript:void(0);\" name=\"frm\">",
    "   <textarea name=\"data\">"+resData+"</textarea>",
    " </form>",
    " <div id=\"c3-loading\" style=\"display: block;\">",
    "   <div class=\"spinner\">",
    "     <div class=\"rect1\"></div>",
    "     <div class=\"rect2\"></div>",
    "     <div class=\"rect3\"></div>",
    "     <div class=\"rect4\"></div>",
    "   </div>",
    "   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
    " </div>",
    "<script src=\"/static/js/kspay.js\"></script>",
    "<script>setTimeout(function() { kspayToParent(); },200);</script>",
    "</body></html>");

    
    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "PAYMENT")
		 .write(sb.toString()).end();
  }

  public static void redirect3D(RoutingContext rc, String url,String resData) {
    String sb = String.join("\n", 
    "<html><head>",
    " <meta charset=\"UTF-8\">",
    " <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
    " <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
    " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
    " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
    "<head><body>",
    " <form action=\""+url+"\" name=\"frm\" method=\"POST\" target=\"_self\">",
    "   <textarea name=\"data\">"+resData+"</textarea>",
    " </form>",
    " <div id=\"c3-loading\" style=\"display: block;\">",
    "   <div class=\"spinner\">",
    "     <div class=\"rect1\"></div>",
    "     <div class=\"rect2\"></div>",
    "     <div class=\"rect3\"></div>",
    "     <div class=\"rect4\"></div>",
    "   </div>",
    "   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
    " </div>",
    "<script src=\"/static/js/kspay.js\"></script>",
    "<script>setTimeout(function() { document.forms.frm.submit(); },200);</script>",
    "</body></html>");
    
    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "PAYMENT")
		 .write(sb.toString()).end();
  }
  
	public static void redirectPayDanal(RoutingContext rc, String url, String encData) {
	  String sb = String.join("\n", 
			  "<html><head>",
			  " <meta charset=\"UTF-8\">",
			  " <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
			  " <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
			  " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
			  " <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
			  "<head><body>",
			  " <form action=\""+url+"\" name=\"frm\" method=\"POST\" target=\"_self\">",
			  "   <textarea class=\"data\" name=\"data\" id=\"data\">" + encData + "</textarea>",
			  " </form>",
			  " <div id=\"c3-loading\" style=\"display: block;\">",
			  "   <div class=\"spinner\">",
			  "     <div class=\"rect1\"></div>",
			  "     <div class=\"rect2\"></div>",
			  "     <div class=\"rect3\"></div>",
			  "     <div class=\"rect4\"></div>",
			  "   </div>",
			  "   <div class=\"de-msg loading-tag\">업체 결제가 진행중입니다. </div>",
			  " </div>",
			  "<script src=\"/static/js/kspay.js\"></script>",
			  "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>",
			  "<script src=\"/static/js/bootbox.min.js\"></script>",
			  "<script>",
			  "setTimeout(function() { document.forms.frm.submit(); },200);",
			  "</script>",
			  /*
			  "<script>",
			  "	var encData = 'RETURNPARAMS=" + encData + "';" +
			  "	console.log('encData : ' + encData);" +
			  "	var Obj = {\r\n" + 
			  "			encData 		: encData" +  
			  "	}\r\n" +
			  "	\r\n" + 
			  "	$.ajax({\r\n" + 
			  "		// 다날 인증\r\n" + 
			  "		url : 'https://apis-dev.pairingpayments.net/api/pay/danal/return',\r\n" + 
			  "		type : 'POST',\r\n" + 
			  //"		data : JSON.stringify(Obj),\r\n" + 
			  "		data : encData,\r\n" +
			  "		type : 'POST',\r\n" + 
			  "		beforeSend: function (xhr) {\r\n" + 
			  "		   // xhr.setRequestHeader (\"Authorization\", $('#mchtKey').val());\r\n" + 
			  "		},\r\n" + 
			  "		success : function(json, textStatus) {\r\n" + 
			  "			console.log(\"json : \" + json);\r\n" + 
			  "			console.log(\"code : \" + json.code);\r\n" + 
			  "			console.log(\"statusMessage : \" + json.statusMessage);\r\n" + 
			  "			console.log(\"message : \" + json.message);\r\n" + 
			  "			console.log(\"startUrl : \" + json.startUrl);\r\n" + 
			  "			console.log(\"startParams : \" + json.startParams);\r\n" + 
			  "			\r\n" + 
			  "			if (json.code == \"0000\") {\r\n" + 
			  "				setTimeout(function() { document.forms.frm.submit(); },2000);",
			  //"				setTimeout(function() { \r\n" + 
			  //"					alert(json.code + \" : \" + json.statusMessage + \" -  \" + json.message);\r\n" +
			  //"				}, 200);" +
			  //"					location.href = \"http://10.10.11.20:8090/HH/pay/sugipay/form\"; \r\n" +
			  "			} else {\r\n" + 
			  "				alert(json.code + \" : \" + json.statusMessage + \" -  \" + json.message);\r\n" + 
			  "			}\r\n" + 
			  "		},\r\n" + 
			  "		error : function(xhr, status, error) {\r\n" + 
			  "			alert(\"업체 결제에 실패헸습니다.\");\r\n" + 
			  "		},\r\n" + 
			  "		complete : function(data) {\r\n" + 
			  "			//$('#writeFrm').remove();\r\n" + 
			  "			//searchForList();\r\n" + 
			  "		}\r\n" + 
			  "	});",
			  "</script>",
			  */
			  "</body></html>");
	  
	  rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
	  .putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
	  .putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
	  .putHeader(HttpHeaders.EXPIRES, "-1")
	  .putHeader(HttpHeaders.CONNECTION, "close")
	  .putHeader(HttpHeaders.SERVER, "PAYMENT")
	  .write(sb.toString()).end();
  }
}