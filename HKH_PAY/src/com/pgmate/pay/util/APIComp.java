package com.pgmate.pay.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.CompCallBack;
import com.pgmate.pay.bean.CompPay;
import com.pgmate.pay.bean.CompPayAuth;
import com.pgmate.pay.bean.CompRefund;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.main.Api;
import com.pgmate.pay.main.C3Runner;
import com.pgmate.pay.proc.Proc;
import com.pgmate.pay.proc.ResultUtil;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class APIComp {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.util.APIComp.class ); 

	public APIComp() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized static boolean APICompCallBack(RoutingContext rc, String uri) {
		DAO dao = new DAO();
		if (uri.equals("/api/comp/callBack")) {
			CompCallBack compCallBack = null;
			compCallBack = (CompCallBack) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), CompCallBack.class);System.out.println();
			logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() BEGIN");
			logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() 가맹점 조회");
			logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() compCallBack.mchtKey : " + compCallBack.mchtKey);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, payKey AS mchtKey, van, tmnId, semiAuth ");
			dao.setTable("PG_MCHT_TMN");
			dao.addWhere("payKey", compCallBack.mchtKey);
			dao.setOrderBy("");
			SharedMap <String, Object> compData = dao.search().getRowFirst();
			if (compData.getString("mchtKey").equals(compCallBack.mchtKey)) {
				JSONObject body = new JSONObject();
				try {
					// 2022-08-01 - 영업사원 아이디 추가
					body.put("salesId", 		compCallBack.salesId);
					if (compCallBack.salesId != null) {
						logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증 결제 가맹정 : " + compData.get("mchtId") + " | salesId : " + compCallBack.salesId);
					} else {
						logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증 결제 가맹정 : " + compData.get("mchtId") + " | salesId IS NULL");
					}
					body.put("compNo", 		compCallBack.compNo);
					body.put("compMember", compCallBack.compMember);
					body.put("amount", 		compCallBack.amount);
					body.put("payerName", 	compCallBack.payerName);
					body.put("payerTel", 		compCallBack.payerTel);
					body.put("payerEmail", 	compCallBack.payerEmail);
					body.put("number", 		compCallBack.number);
					body.put("expiry", 			compCallBack.expiry);
					body.put("installment", 	compCallBack.installment);
					body.put("name", 			compCallBack.name);
					body.put("price", 			compCallBack.price);
					body.put("qty", 				compCallBack.qty);
					
					if ("Y".equals(compData.get("semiAuth"))) {
						logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증 결제 가맹정 : " + compData.get("mchtId") + " | mchtKey : " + compData.get("mchtKey")+ " | semiAuth : " + compData.get("semiAuth"));
						if (compCallBack.authPw == null && compCallBack.authDob == null) {
							logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증결제 필수값 없음 : authPw, authDob");
							Api.sendMsg(rc, "0000", "구인증결제", "구인증결제 필수값 없음 : authPw, authDob");
							logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
							return false;
						} else if (compCallBack.authPw == null && compCallBack.authDob != null) {
							logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증결제 필수값 없음 : authPw");
							Api.sendMsg(rc, "0000", "구인증결제", "구인증결제 필수값 없음 : authPw");
							logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
							return false;
						} else if (compCallBack.authPw != null && compCallBack.authDob == null) {
							logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 구인증결제 필수값 없음 : authDob");
							Api.sendMsg(rc, "0000", "구인증결제", "구인증결제 필수값 없음 : authDob");
							logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
							return false;
						} else {
							body.put("authPw", 				compCallBack.authPw);
							body.put("authDob", 				compCallBack.authDob);
						}
					} else {
						logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() : 비인증 결제 가맹정 : id : " + compData.get("mchtId") + " | mchtKey : " + compData.get("mchtKey")+ " | semiAuth : " + compData.get("semiAuth"));
					}
				} catch (Exception e) {
					// TODO: handle exception
					logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : 데이터를 변환 할수 없습니다. Exception : " + e);
					logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : 데이터를 변환 할수 없습니다. Exception : compCallBack : " + compCallBack);
					e.printStackTrace();
					Api.sendMsg2(rc, "400", "Bad Request", "데이터를 변환 할수 없습니다. ERROR : " + e);
					logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
					return false;
				}
				
				long trx_long = (long) (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
				body.put("trackId", "COMP" + trx_long);
				logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() trackId : " + body.get("trackId"));
				logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() body : \n" + body.toJSONString());
				String encData = null;
				try {
					//encData = AES256Cipher.AES_Encode(body.toString(), compCallBack.mchtKey);
					encData = AES256Cipher.AES_Encode(body.toString());
					logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() EncData : \n" + encData);
					Api.sendMsgCallBack2(rc, "0000", "승인", "데이터 암호화 성공", StringEscapeUtils.unescapeJava(encData));
					logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
					return false;
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
					// TODO Auto-generated catch block
					logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : 데이터를 암호화 할수 없습니다. : Exception : " + e);
					logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : 데이터를 암호화 할수 없습니다. : body : " + body);
					e.printStackTrace();
					Api.sendMsg2(rc, "400", "Bad Request", "데이터를 암호화 할수 없습니다.");
					logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
					return false;
				}
			} else {
				logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : 존재하지 않는 가맹점키입니다.");
				logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : mchtKey : " + compCallBack.mchtKey);
				logger.debug("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() ERROR : DB mchtKey : " + compData.getString("mchtKey"));
				Api.sendMsg2(rc, "401", "Unauthorized", "존재하지 않는 가맹점키입니다.");
				logger.info("========== compCallBack.compNo : " + compCallBack.compNo + " ========== APICompCallBack() END");
				return false;
			}
		}
		return false;
	}
	
	public synchronized static boolean APICompPay(RoutingContext rc, String uri) {
		System.out.println();
		DAO dao = new DAO();
		if (uri.equals("/api/comp/pay")) {
			CompPay compPay = 	(CompPay) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), CompPay.class);
			logger.info("========== APICompPay() BEGIN");
			logger.info("========== APICompPay() 가맹점 조회");
			logger.info("========== APICompPay() APICompPayAuth()mchtKeyy : " + compPay.mchtKey);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, payKey AS mchtKey, van, tmnId");
			dao.setTable("PG_MCHT_TMN");
			dao.addWhere("payKey", compPay.mchtKey);
			dao.setOrderBy("");
			SharedMap <String, Object> compData = dao.search().getRowFirst();
			if (compData.getString("mchtKey").equals(compPay.mchtKey)) {
				String decData = null;
				try {
					//decData = AES256Cipher.AES_Decode(compPay.EncData, compPay.mchtKey);
					decData = AES256Cipher.AES_Decode(StringEscapeUtils.unescapeJava(compPay.EncData));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e1) {
					// TODO Auto-generated catch block
					logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다. Exception : " + e1);
					logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다. compPay.EncData :: " + compPay.EncData);
					e1.printStackTrace();
					Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
					logger.info("========== APICompPay() END");
					return false;
				}
				System.out.println();
				logger.info("========== APICompPay() DEC : \n" + decData);
				
				JSONParser resParser = new JSONParser();
				Object obj = new Object();
				try {
					obj = resParser.parse(decData);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다. Exception : " + e);
					logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다. decData : " + decData);
					e.printStackTrace();
					Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
					logger.info("========== APICompPay() END");
					return false;
				}
				JSONObject jObj = (JSONObject) obj;
				
				// 중복결제 조회 
				logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() 중복결제 조회");
				dao.initRecord();
				dao.setDebug(true);
				dao.setColumns("*");
				dao.setTable("PG_TRX_PAY A JOIN PG_TRX_REQ B ON A.trackId = B.trackId");
				dao.addWhere("A.trackId", (String) jObj.get("trackId"));
				dao.setOrderBy("");
				SharedMap <String, Object> payData = dao.search().getRowFirst();
				if (payData.getString("trackId").equals("")) {
					Pay pay = new Pay();
					try {
						// 2022-05-16 mchtId 변수 추가
						pay.mchtId = compData.getString("mchtId");
						System.out.println();
						System.out.println("compData mchtId : " + compData.getString("mchtId"));
						System.out.println("pay.mchtId : " + pay.mchtId);
						System.out.println();
						//pay.trxId = "";
						pay.compNo = (String) jObj.get("compNo");
						pay.compMember = (String) jObj.get("compMember");
						// 2022-08-01 - 영업사원 아이디 추가
						pay.salesId = (String) jObj.get("salesId");
						
						pay.trxType = "COMPTR";
						pay.tmnId = compData.getString("tmnId");
						//long trx_long = (long) (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
						//pay.trackId = "COMP" + trx_long;
						pay.trackId = (String) jObj.get("trackId");
						pay.amount = (long) jObj.get("amount");
						pay.payerName = (String) jObj.get("payerName");
						pay.payerTel = (String) jObj.get("payerTel");
						pay.payerEmail = (String) jObj.get("payerEmail");
		
						pay.card		= new Card();
						pay.card.number = (String) jObj.get("number");
						pay.card.expiry = (String) jObj.get("expiry");
						pay.card.installment = Integer.parseInt((String) jObj.get("installment"));
					
						if (jObj.get("name") == null) {
							// 상품 미입력시 - NULL 처리
						} else if (jObj.get("name") != null) {
							// 상품 미입력시 - NULL 처리
							String prdName = (String) jObj.get("name");
							String prdNameChk = prdName.trim();
							if ("".equals(prdNameChk)) {
								
							} else {
								// 상품명은 있으나, 가격, 개수 미입력시 = prcie = amount / qty = 1
								pay.products 	= new ArrayList<Product>();
								Product product = new Product();
								product.desc = "WEBPAY";
								product.name = prdName;
								
								Object data_price = jObj.get("price");
								if (data_price == null) {
									product.price = (long) jObj.get("amount");
								} else {
									if (data_price instanceof java.lang.Long) {
										long price = (long) data_price;
										if (price > 0) {
											product.price = price;
										} else {
											product.price = (long) jObj.get("amount");
										}
									}
								}
								Object data_qty = jObj.get("qty");
								if (data_qty == null) {
									product.qty = 1;
								} else {
									if (data_qty instanceof java.lang.Long) {
										Integer qty = Integer.parseInt(String.valueOf(data_qty));
										if (qty > 0) {
											product.qty = qty;
										} else {
											product.qty = 1;
										}
									}
								}
								pay.products.add(product);
							}
						}
						pay.metadata = new SharedMap<String,String>();
						// 2022-05-04 구인증 추가
						Object data_authPw = jObj.get("authPw");
						Object data_authDob = jObj.get("authDob");
						System.out.println("authPw : " + data_authPw);
						System.out.println("data_authDob : " + data_authDob);
						System.out.println();
						if (data_authPw == null && data_authDob == null) {
							pay.metadata.put("cardAuth", "false");
						//	없으면 비인증결제
						//	Api.sendMsg(rc, "0000", "인증결제", "인증결제 필수값 없음 : authPw, authDob");
						//	return false;
						} else if (data_authPw == null && data_authDob != null) {
							logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 구인증결제 필수값 없음 : authPw : " + data_authPw);
							Api.sendMsg(rc, "0000", "구인증결제", "구인증결제 필수값 없음 : authPw");
							logger.info("========== APICompPay() END");
							return false;
						} else if (data_authPw != null && data_authDob == null) {
							logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 구인증결제 필수값 없음 : authDob : " + data_authDob);
							Api.sendMsg(rc, "0000", "구인증결제", "구인증결제 필수값 없음 : authDob");
							logger.info("========== APICompPay() END");
							return false;
						} else {
							if (data_authPw != null) {
								if (data_authPw instanceof java.lang.String) {
									pay.metadata.put("authPw", (String) data_authPw);
								}
							}
							if (data_authDob != null) {
								if (data_authDob instanceof java.lang.String) {
									pay.metadata.put("authDob", (String) data_authDob);
								}
							}
						}
					} catch (Exception e) {
						// TODO: handle exception
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 데이터입니다. 데이터를 변환 할수 없습니다. : Exception : " + e);
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 데이터입니다. 데이터를 변환 할수 없습니다. : jObj : " + jObj.toString());
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 데이터입니다. 데이터를 변환 할수 없습니다.");
						logger.info("========== APICompPay() END");
						return false;
					}
					
					Request request = new Request();
					request.pay = pay; 
					String reqJson = GsonUtil.toJson(request, true, "");
					//logger.info("========== APICompPay() reqJson : \n" + reqJson);
					String connStr = "";
					try {
						connStr = compConnect("pay", reqJson, compPay.mchtKey);
						//request = (Request) GsonUtil.fromJson(compConnect(reqJson, compPay.mchtKey), Request.class);
						//logger.info("========== APICompPay() compConnect : \n" + connStr);
						// executeRefund(response);
					} catch (Exception e) {
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : compConnect Excetion : " + e);
						e.printStackTrace();
						logger.info("========== APICompPay() END");
						//return false;
					}
					JSONParser connParser = new JSONParser();
					Object connObj = new Object();
					try {
						connObj = connParser.parse(connStr);
					} catch (ParseException e) {
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connStr : " + connStr);
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connObj : " + connObj);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== APICompPay() END");
						return false;
					}
					JSONObject connjObj = (JSONObject) connObj;
				//	logger.info("========== APICompPay() connjObj : " + connjObj);
					
					String connStr2 = connjObj.get("result").toString();
					JSONParser connParser2 = new JSONParser();
					Object connObj2 = new Object();
					try {
						connObj2 = connParser2.parse(connStr2);
					} catch (ParseException e) {
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connStr2 : " + connStr2);
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connObj2 : " + connObj2);
					}
					JSONObject connjObj2 = (JSONObject) connObj2;
					logger.info("========== APICompPay() connjObj2 : \n" + connjObj2);
					logger.info("========== APICompPay() connjObj2 resultCd : " + connjObj2.get("resultCd"));
					logger.info("========== APICompPay() connjObj2 resultMsg : " + connjObj2.get("resultMsg"));
					logger.info("========== APICompPay() connjObj2 advanceMsg : " + connjObj2.get("advanceMsg"));
					
					String connStr3 = connjObj.get("pay").toString();
					JSONParser connParser3 = new JSONParser();
					Object connObj3 = new Object();
					try {
						connObj3 = connParser3.parse(connStr3);
					} catch (ParseException e) {
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connStr3 : " + connStr3);
						logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== connObj3 : " + connObj3);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== APICompPay() END");
						return false;
					}
					JSONObject connjObj3 = (JSONObject) connObj3;
					logger.info("========== APICompPay() connjObj3 : " + connjObj3);
					logger.info("========== APICompPay() connjObj3 trxId : " + connjObj3.get("trxId"));
					//Api.sendMsg(rc, "200", "Success", "결제 성공");
					//Api.sendMsgPay2(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString(), connjObj3.get("trxId").toString());
					logger.info("========== APICompPay() 전달데이터 조회 trackId : " + jObj.get("trackId"));
					dao.initRecord();
					dao.setDebug(true);
					dao.setColumns("A.mchtId, A.trxId, C.vanTrxId, B.vanResultCd, B.vanResultMsg, C.authCd,\n" + 
							"IFNULL(C.regDay, D.regDay) AS regDay, \n" + 
							"IFNULL(C.regTime, D.regTime) AS regTime, \n" + 
							"C.issuer, C.acquirer, A.bin, A.last4, A.installment, \n" + 
							"A.amount, A.payerName, G.name AS prdName, A.compNo, \n" +
							"A.compMember, B.van, B.vanId \n"
					);
					dao.setTable("PG_TRX_REQ A \n" + 
							"JOIN PG_TRX_RES B ON A.trxId = B.trxId \n" + 
							"LEFT JOIN PG_TRX_PAY C ON A.trxId = C.trxId \n" + 
							"LEFT JOIN PG_TRX_ERR D ON A.trxId = D.trxId \n" + 
							"LEFT JOIN PG_CODE_BIN E ON A.bin = E.bin \n" + 
							"LEFT JOIN PG_CODE F ON E.acquirer = F.codeName \n" + 
							"LEFT JOIN PG_TRX_PRD G ON A.prodId = G.prodId \n");
					dao.addWhere("A.trackId", (String) jObj.get("trackId"));
					dao.setGroupBy("A.trxId");
					dao.setOrderBy("A.regDate DESC");
					SharedMap <String, Object> payDataMap = dao.search().getRowFirst();
					JSONObject body = new JSONObject();
					body.put("trxId", payDataMap.getString("trxId"));
					if (VertXUtil.getXRealIp(rc).equals("115.68.14.246")) {
						logger.info("========== APICompPay() 링크나인 전달 PG거래번호 vanTrxId : " + payDataMap.getString("vanTrxId"));
						body.put("vanTrxId", payDataMap.getString("vanTrxId"));
						// 2022-03-28 PG상점코드, PG상점ID 추가
						body.put("van", payDataMap.getString("van"));
						body.put("vanId", payDataMap.getString("vanId"));
					}
					body.put("authCd", payDataMap.getString("authCd"));
					body.put("vanDay", payDataMap.getString("regDay"));
					body.put("vanTime", payDataMap.getString("regTime"));
					
					//body.put("cardCode", payDataMap.getString("idx"));
					body.put("issuer", payDataMap.getString("issuer"));			// 카드사[카드명] - ex : 삼성카드 
					// 2022-05-13 업체요청으로 카드코드테이블 데이터로 전달 처리
					if (C3Runner.cardcodeList.size() == 0) { 
						TrxDAO trxDao = new TrxDAO();
						C3Runner.cardcodeList = trxDao.selectCardCodeList();
					}
					logger.info("========== issuer : " + payDataMap.getString("issuer"));
					logger.info("========== acquirer : " + payDataMap.getString("acquirer"));
					if (!"".equals(payDataMap.getString("acquirer"))) {
						if (C3Runner.cardcodeList.size() > 0) { 
							int acquirerChk = 0;
							//logger.info("========== C3Runner.cardcodeList : " + C3Runner.cardcodeList.toString());
							for (String key : C3Runner.cardcodeList.keySet()) {
							//	System.out.println("key : " + key);
								SharedMap<String, Object> data = C3Runner.cardcodeList.get(key);
								String code = data.getString("code");
								String cname = data.getString("cname");
								String calias = data.getString("calias");
								int compareChk = 0;
								compareChk = cname.compareTo(payDataMap.getString("acquirer"));
								if (compareChk > 1) {
									logger.info("========== ");
									logger.info("========== 비교결과 : " + compareChk);
									logger.info("========== 리턴 카드사명 : [" + payDataMap.getString("acquirer") + "]");
									logger.info("========== 디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
									
									body.put("acquirerCode", code);	// 카드사코드 - ex : 001, 002
									body.put("acquirer", calias);	// 카드사 - ex : BC, 삼성
								} else if (compareChk == 0) {
									logger.info("========== ");
									logger.info("========== 비교결과 : " + compareChk);
									logger.info("========== 리턴 카드사명 : [" + payDataMap.getString("acquirer") + "]");
									logger.info("========== 디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
									body.put("acquirerCode", code);	// 카드사코드 - ex : 001, 002
									body.put("acquirer", calias);	// 카드사 - ex : BC, 삼성
									break;
								} else if (compareChk < 0) {
								//	System.out.println("카드사명 결과 : " + compareChk);
									body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
									body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
								}
							}
						} else {
							body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
							body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
						}
					} else {
						body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
						body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
					}
					logger.info("========== 최종 발급사 : " + body.get("acquirer"));
					logger.info("========== ");
					
					body.put("cardNumber", payDataMap.getString("bin") + "******" + (String) payDataMap.getString("last4"));
					body.put("installment", payDataMap.getString("installment"));
					body.put("amount", payDataMap.getString("amount"));
					body.put("payerName", payDataMap.getString("payerName"));
					body.put("prdName", payDataMap.getString("prdName"));
					body.put("compNo", payDataMap.getString("compNo"));
					
					Api.sendMsgPayMap(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString(), body);
					
					//Api.sharedMap.put(PAYUNIT.PAYLOAD, pay);
					logger.debug("");
				} else {
					logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 주문번호 중복 등록. 중복 결제 할수 없습니다.");
					Api.sendMsg(rc, "400", "Bad Request", "주문번호 중복 등록. 중복 결제 할수 없습니다.");
				}
			} else {
				logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR : 존재하지 않는 가맹점키입니다.");
				logger.debug("========== compPay.mchtKey : " + compPay.mchtKey + " ========== APICompPay() ERROR mchtKey : " + compPay.mchtKey);
				logger.debug("========== APICompPay() ERROR DB mchtKey : " + compData.getString("mchtKey"));
				Api.sendMsg2(rc, "401", "Unauthorized", "존재하지 않는 가맹점키입니다.");
			}
		}
		logger.info("========== APICompPay() END");
		return false;
	}
	
	public synchronized static boolean APICompRefund(RoutingContext rc, String uri) {
		System.out.println();
		logger.info("========== APICompRefund BEGIN");
		DAO dao = new DAO();
		if (uri.equals("/api/comp/refund")) {
			CompRefund compRefund = 	(CompRefund) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), CompRefund.class);
			logger.info("========== APICompRefund mchtKey : " + compRefund.mchtKey);
			logger.info("========== APICompRefund trxId : " + compRefund.trxId);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, payKey AS mchtKey, van, tmnId");
			dao.setTable("PG_MCHT_TMN");
			dao.addWhere("payKey", compRefund.mchtKey);
			dao.setOrderBy("");
			SharedMap <String, Object> compData = dao.search().getRowFirst();
			if (compData.getString("mchtKey").equals(compRefund.mchtKey)) {
				Boolean dupChk = false;
				// 2022-05-16 즉시취소를 위한 결제데이터 관리 - 데이터 확인
				Iterator<String> keys = Proc.trxIdPayMap.keySet().iterator();
		        while( keys.hasNext() ){
		            String key = keys.next();
		            if (compRefund.trxId.contains((key)) == true) {
		            	System.out.println("");
		            	System.out.println("========== 취소 중복결제 - 즉시취소 데이터 체크");
		            	System.out.println("========== map key :" + key);
		            	System.out.println("========== compRefund.trxId :" + compRefund.trxId);
		            	System.out.println("");
		            	dupChk = true;
		            	break;
		            }
				}
		        if (dupChk == false) {
		        	System.out.println("========== 취소 중복결제 - 즉시취소 데이터 체크 : 메모리데이터 미존재, DB데이터 체크");
					// 중복결제 조회 
					dao.initRecord();
					dao.setDebug(true);
					dao.setColumns("trxId, amount, capId ");
					dao.setTable("VW_TRX_CAP");
					dao.addWhere("trxId", compRefund.trxId);
					dao.setOrderBy("");
					SharedMap <String, Object> payData = dao.search().getRowFirst();
					if (!payData.getString("capId").equals("")) {
						dupChk = true;
						System.out.println("========== 취소 중복결제 - 즉시취소 데이터 체크 : DB데이터 확인완료");
					}
		        } else {
		        	System.out.println("========== 취소 중복결제 - 즉시취소 데이터 체크 : 메모리데이터 확인완료");
		        }
				
				//if (!payData.getString("capId").equals("")) {
				if (dupChk == true) {
					Refund refund = new Refund();
					
					refund.trxType = "COMPTR";
					refund.trackId = GenKey.genKeys(CPKEY.REFUND, compData.getString("mchtId"));
					refund.cancelReqStat = 0;
				//	refund.rootTrxId = payData.getString("trxId");
					refund.rootTrxId = compRefund.trxId;
					
					refund.amount = Long.parseLong(compRefund.amount);
					
					refund.metadata = new SharedMap<String,String>();
					refund.metadata.put("grade", "가맹점");
					
					refund.compNo 		= compRefund.compNo;
					refund.compMember 	= compRefund.compMember;
					
					// 2022-08-01 - 영업사원 아이디 추가
					refund.salesId 	= compRefund.salesId;
					
					Request request = new Request();
					request.refund = refund; 
					String reqJson = GsonUtil.toJson(request, true, "");
					logger.info("========== APICompRefund reqJson : \n" + reqJson);
					//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
					String connStr = "";
					try {
						connStr = compConnect("refund", reqJson, compRefund.mchtKey);
						//request = (Request) GsonUtil.fromJson(compConnect(reqJson, compPay.mchtKey), Request.class);
						logger.info("========== APICompRefund compConnect : \n" + connStr);
					} catch (Exception e) {
						logger.debug("========== APICompRefund ERROR : compConnect : " + e.getMessage());
						e.printStackTrace();
						Api.sendMsg2(rc, "500", "Internal Server Error", "통신에러");
					}
					JSONParser connParser = new JSONParser();
					Object connObj = new Object();
					try {
						connObj = connParser.parse(connStr);
					} catch (ParseException e) {
						logger.debug("========== APICompRefund ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== connStr : " + connStr);
						logger.info("========== connObj : " + connObj);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
					}
					JSONObject connjObj = (JSONObject) connObj;
					logger.info("========== APICompRefund connjObj : \n" + connjObj);
					
					String connStr2 = connjObj.get("result").toString();
					JSONParser connParser2 = new JSONParser();
					Object connObj2 = new Object();
					try {
						connObj2 = connParser2.parse(connStr2);
					} catch (ParseException e) {
						logger.debug("========== APICompRefund ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== connStr2 : " + connStr2);
						logger.info("========== connObj2 : " + connObj2);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
					}
					JSONObject connjObj2 = (JSONObject) connObj2;
					logger.info("========== APICompRefund  : \n" + connjObj2);
					logger.info("========== APICompRefund connObj2 resultCd : " + connjObj2.get("resultCd"));
					logger.info("========== APICompRefund connObj2 resultMsg : " + connjObj2.get("resultMsg"));
					logger.info("========== APICompRefund connObj2 advanceMsg : " + connjObj2.get("advanceMsg"));
					//Api.sendMsg2(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString());
					
					String connStr3 = connjObj.get("refund").toString();
					JSONParser connParser3 = new JSONParser();
					Object connObj3 = new Object();
					try {
						connObj3 = connParser3.parse(connStr3);
					} catch (ParseException e) {
						logger.debug("========== APICompRefund ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
					}
					JSONObject connjObj3 = (JSONObject) connObj3;
					logger.info("========== APICompRefund connjObj3 : " + connjObj3);
					logger.info("========== APICompRefund connjObj3 trxId : " + connjObj3.get("trxId"));
					
					dao.initRecord();
					dao.setDebug(true);
					dao.setColumns("*");
					dao.setTable("PG_TRX_RFD \n");
					dao.addWhere("trackId", connjObj3.get("trackId"));
					dao.setGroupBy("trxId");
					dao.setOrderBy("regDate DESC");
					SharedMap <String, Object> payDataMap = dao.search().getRowFirst();
					JSONObject body = new JSONObject();
					body.put("trxId", payDataMap.getString("trxId"));
					if (VertXUtil.getXRealIp(rc).equals("115.68.14.246")) {
						logger.info("========== APICompRefund 링크나인 전달 PG거래번호 vanTrxId : " + payDataMap.getString("vanTrxId"));
						//body.put("vanTrxId", payDataMap.getString("vanTrxId"));
						// 2022-03-28 PG상점코드, PG상점ID 추가
						body.put("van", payDataMap.getString("van"));
						body.put("vanId", payDataMap.getString("vanId"));
					}
					//body.put("authCd", payDataMap.getString("authCd"));
					body.put("vanDay", payDataMap.getString("regDay"));
					body.put("vanTime", payDataMap.getString("regTime"));
					
					//body.put("cardCode", payDataMap.getString("idx"));
					//body.put("cardNumber", payDataMap.getString("bin") + "********" + (String) payDataMap.getString("last4"));
					//body.put("installment", payDataMap.getString("installment"));
					//body.put("amount", payDataMap.getString("amount"));
					//body.put("payerName", payDataMap.getString("payerName"));
					//body.put("prdName", payDataMap.getString("prdName"));
					//body.put("compNo", payDataMap.getString("compNo"));
					
					try {
						String q = "INSERT INTO PG_TRX_ADMIN_RFD SELECT NULL,rootTrxId,vanTrxId,vanId,vanResultCd,vanResultMsg,rfdAmount,resultCd,trxId, '업체 매입취소 : " 
							+ refund.amount + "', '" + refund.compMember + "', '" 
							+ CommonUtil.getCurrentDate("yyyyMMdd") 
							+ "',regDate FROM PG_TRX_RFD  WHERE trxId = '" 
							+ payDataMap.getString("trxId") + "'";
						logger.info("========== APICompRefund PG_TRX_ADMIN_RFD Query : " + q);
						new DAO().update(q);
					} catch (Exception e) {
						// TODO: handle exception
						logger.debug("========== APICompRefund ERROR : PG_TRX_ADMIN_RFD INSERT ERROR");
						e.printStackTrace();
					}
					
					Api.sendMsgPayMap(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString(), body);
				} else {
					logger.debug("========== APICompRefund ERROR : 존재하지 않는 거래번호입니다.");
					Api.sendMsg2(rc, "400", "Bad Request", "존재하지 않는 거래번호입니다.");
				}
			} else {
				logger.debug("========== APICompRefund ERROR : 존재하지 않는 가맹점키입니다.");
				logger.info("========== APICompRefund ERROR : mchtKey : " + compRefund.mchtKey);
				logger.info("========== APICompRefund ERROR : DB mchtKey : " + compData.getString("mchtKey"));
				Api.sendMsg2(rc, "401", "Unauthorized", "존재하지 않는 가맹점키입니다.");
			}
		}
		logger.info("========== APICompRefund END");
		return false;
	}
	
	public synchronized static String compConnect(String urlParam, String request, String payKey) {
		System.out.println();
		logger.info("========== compConnect BEGIN");
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setColumns("*");
		dao.setTable("PG_COMPANY");
		SharedMap<String, Object> rs = dao.search().getRowFirst();
		logger.info("========== compConnect PG URL : " + rs.getString("apiDomain"));
		//String paymentUrl = "https://apis-dev.pairingpayments.net/api/" + urlParam;		// 개발
		//String paymentUrl = "http://api.pairingpayments.net/api/" + urlParam;	// 운영
		String paymentUrl = rs.getString("apiDomain") + "/api/" +  urlParam;	// 운영
		logger.info("========== compConnect paymentUrl : " + paymentUrl);
		
		StringBuilder result = new StringBuilder();
		URL url = null;
		HttpURLConnection conn = null;

		System.setProperty("https.protocols", "TLSv1.2");

		long time = System.currentTimeMillis();
		try {
			System.out.println("LOCAL >> PAYMENT [" + request + "]");
			url = new URL(paymentUrl);

			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);

			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", payKey);
			conn.setRequestProperty("Connection", "close");

			OutputStream os = conn.getOutputStream();
			os.write(request.getBytes("utf-8"));
			os.flush();
			os.close();

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String line;
			while ((line = br.readLine()) != null)
				result.append(line + "\n");

			br.close();

		} catch (Exception e) {
			result.append("CONNECT ERROR [" + e.getMessage() + "] " + paymentUrl);
			logger.info("PAYMENT URL REQUEST ERROR =[" + e.getMessage() + "]");
		} finally {
			logger.info("ElapsedTime : " + (long) (System.currentTimeMillis() - time) + " msec");
			logger.info("LOCAL << PAYMENT [" + result.toString() + "]");
			conn.disconnect();
		}
		logger.info("========== compConnect END");
		return result.toString();
	}
	
	// 2022-08-05 - 외부업체 결제 API - 인증결제
	public synchronized static boolean APICompCallBackAuth(RoutingContext rc, String uri) {
		DAO dao = new DAO();
		if (uri.equals("/api/comp/callBackAuth")) {
			CompCallBack compCallBack = null;
			compCallBack = (CompCallBack) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), CompCallBack.class);System.out.println();
			logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== ========== ========== APICompCallBackAuth() BEGIN");
			logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() 가맹점 조회 compCallBack.mchtKey : " + compCallBack.mchtKey);
			
			/*	인증결제 구성 Flow 정리
			 * 인증결제 요청
- /api/comp/callBackAuth
{
	"mchtKey":"pk_8c33-f35761-3fb-7d507"
}
- return
{
	"code": "0000",
   	"status": "승인",
	"msg": "올앳 인증결제 승인",
	"vanId": "welcome246",
	"mchtId": "mchtId",
	"pairing_payauth_url": "/api/comp/payAuth"
}

페어링에서 제공해야할 데이터
allat_shop_id - PG상점아이디 vanId
shop_receive_url - 페어링 json 전달받고 결제할 url 주소
allat_pmember_id - 회원아이디 - 가맹점 아이디 mchtId
			*/
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, payKey AS mchtKey, van, vanIdx, tmnId, semiAuth ");
			dao.setTable("PG_MCHT_TMN");
			dao.addWhere("payKey", compCallBack.mchtKey);
			dao.setOrderBy("");
			SharedMap <String, Object> compData = dao.search().getRowFirst();
			if (compData.getString("mchtKey").equals(compCallBack.mchtKey)) {
				if (CommonUtil.isNullOrSpace(compData.getString("semiAuth"))) {
					logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() : 인증결제 가맹점이 아닙니다. isNullOrSpace semiAuth : " + compData.getString("semiAuth"));
					Api.sendMsg(rc, "9999", "인증결제", "인증결제 가맹점이 아닙니다.");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
					return false;
				} else {
					if ("A".equals(compData.get("semiAuth"))) {
						logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() : 인증결제 가맹점 확인 A equals semiAuth : " + compData.getString("semiAuth"));
					} else {
						logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() : 인증결제 가맹점이 아닙니다. A equals x semiAuth : " + compData.getString("semiAuth"));
						Api.sendMsg(rc, "9999", "인증결제", "인증결제 가맹점이 아닙니다.");
						logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
						return false;	
					} 
				}
				String trxId = TrxDAO.getTrxId();
				logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() - trxId : " + trxId);
				
				String mchtId = "";
				if (CommonUtil.isNullOrSpace(compData.getString("mchtId"))) {
					logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() : 등록된 가맹점이 없습니다. mchtId : " + compData.getString("mchtId"));
					Api.sendMsg(rc, "9999", "인증결제", "등록된 가맹점이 없습니다.");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
					return false;	
				} else {
					mchtId = compData.getString("mchtId");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() - mchtId : " + mchtId);
				}
				
				String vanId = "";
				TrxDAO trxDao = new TrxDAO();
				SharedMap <String, Object> vanData = trxDao.getVanByVanIdx2(compData.getString("vanIdx"));
				if (CommonUtil.isNullOrSpace(vanData.getString("vanId"))) {
					logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() : 등록된 PG상점ID가 없습니다. van : " + compData.getString("van") + " | vanIdx : " + compData.getString("vanIdx"));
					Api.sendMsg(rc, "9999", "인증결제", "등록된 PG상점ID가 없습니다.");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
					return false;	
				} else {
					vanId = vanData.getString("vanId");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() - vanId : " + vanId);
				}
				
				if (CommonUtil.isNullOrSpace(mchtId) && CommonUtil.isNullOrSpace(mchtId) && CommonUtil.isNullOrSpace(vanId)) {
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() - 인증결제 요청 승인 - 데이터가 없습니다. ");
					Api.sendMsg2(rc, "9999", "인증결제", "데이터가 없습니다.");
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
					return false;
				} else {
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() - 인증결제 요청 승인");
					sendMsgCompCallBackAuth(rc, "0000", "인증결제", "인증결제 요청 승인 성공", trxId, vanId, mchtId);
					logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
					return false;
				}
			} else {
				logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() ERROR : 존재하지 않는 가맹점키입니다.");
				logger.debug("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() ERROR : DB mchtKey : " + compData.getString("mchtKey"));
				Api.sendMsg2(rc, "9999", "인증결제", "데이터를 암호화 할수 없습니다.");
				logger.info("========== compCallBack.mchtKey : " + compCallBack.mchtKey + " ========== APICompCallBackAuth() END");
				return false;
			}
		}
		return false;
	}
	public static void sendMsgCompCallBackAuth(RoutingContext rc, Object object, Object object2, Object object3, Object object4, Object object5, Object object6) {
		logger.info("========== mchtId : " + object5 + " ========== ========== sendMsgCompCallBackAuth()");
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", object);
		responseMap.put("status", object2);
		responseMap.put("msg", object3);
		responseMap.put("trxId", object4);
		responseMap.put("vanId", object5);
		responseMap.put("mchtId", object6);

		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public synchronized static boolean APICompPayAuth(RoutingContext rc, String uri) {
		System.out.println();
		DAO dao = new DAO();
		if (uri.equals("/api/comp/payAuth")) {
			CompPayAuth compPayAuth = 	(CompPayAuth) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), CompPayAuth.class);
			logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== ========== ========== APICompPayAuth() BEGIN");
			logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + "========== APICompPayAuth() 가맹점 조회 compPayAuth.mchtKey : " + compPayAuth.mchtKey);
			
			/*	인증결제 구성 Flow 정리
			 * 인증결제 
- /api/comp/payAuth
{
    "compNo":"1234567890",
    "compMember":"1234",
    "mchtKey":"pk_8c33-f35761-3fb-7d507",
    "amount":"1004",
    "payerName":"a",
    "payerTel":"00000000",
    "payerEmail":"",
    "name":"천사",
    "price" : 1004,
    "qty": 1,
    "encData": encData
}

- PG사 결제데이터 생성
- PG사 결제요청
- PG사 응답 확인
- noti 테이블 카드번호 체크 - 인증결제 타입? sumiAuth, vanId 체크 후 카드번호 /pay 입력처리 로직 추가
- Pay 데이터 요청
- vanId, semiAuth 체크후 노 PG 데이터 생성 처리
- 
- return - 기존 결제모듈 리턴값으로 리턴 처리

			*/
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, payKey AS mchtKey, van, vanIdx, tmnId");
			dao.setTable("PG_MCHT_TMN");
			dao.addWhere("payKey", compPayAuth.mchtKey);
			dao.setOrderBy("");
			SharedMap <String, Object> compData = dao.search().getRowFirst();
			if (compData.getString("mchtKey").equals(compPayAuth.mchtKey)) {
				// 추가 - 올앳페이 모빌리언스 통신 PG결제 응답 확인

				String vanId = "";
				TrxDAO trxDao = new TrxDAO();
				SharedMap <String, Object> vanData = trxDao.getVanByVanIdx2(compData.getString("vanIdx"));
				if (CommonUtil.isNullOrSpace(vanData.getString("vanId"))) {
					logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() : 등록된 PG상점ID가 없습니다. van : " + compData.getString("van") + " | vanIdx : " + compData.getString("vanIdx"));
					Api.sendMsg2(rc, "9999", "인증결제", "등록된 PG상점ID가 없습니다.");
					logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() END");
					return false;	
				} else {
					vanId = vanData.getString("vanId");
					logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() - vanId : " + vanId);
				}
				String allat_shop_id = vanId;
				long allat_amt = compPayAuth.amount;
				String allat_enc_data = compPayAuth.encData;
				
				if (CommonUtil.isNullOrSpace(vanData.getString("cryptoKey"))) {
					logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() : API키가 없습니다. 관리자에게 문의하세요. cryptoKey : " + vanData.getString("cryptoKey"));
					Api.sendMsg2(rc, "9999", "인증결제", "API키가 없습니다. 관리자에게 문의하세요.");
					logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() END");
					return false;	
				} else {
				}
				String allat_cross_key = vanData.getString("cryptoKey");
				logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey +" ========== APICompPayAuth() - allat_shop_id :: " + allat_shop_id);
				logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey +" ========== APICompPayAuth() - allat_amt :: " + allat_amt);
				logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey +" ========== APICompPayAuth() - allat_enc_data :: " + allat_enc_data);
				logger.info("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey +" ========== APICompPayAuth() - allat_cross_key :: " + allat_cross_key);
/*				
				logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey +" ========== APICompPayAuth() 중복결제 조회");
				dao.initRecord();
				dao.setDebug(true);
				dao.setColumns("*");
				dao.setTable("tbl_wellcome_notify");
				dao.addWhere("status", 0);
				dao.addWhere("mid", vanId);
				dao.addWhere("pay_type", "CREDIT_CARD");
				dao.addWhere("org_pg_seq_no", (String) hm.get("seq_no"));	// vanTrxId
				dao.setOrderBy("");
				SharedMap <String, Object> payData = dao.search().getRowFirst();
*/				
				// tbl_wellcome_notify - PG 결제 후 3분 ~ 3분 30초 가량 후 데이터 입력들어옴.
				
				if (CommonUtil.isNullOrSpace(compPayAuth.trxType)) {
					logger.debug("========== compPayAuth.trxType : " + compPayAuth.trxType + " ========== APICompPayAuth() : trxType 파라메터가 없습니다. 관리자에게 문의하세요. cryptoKey : " + vanData.getString("cryptoKey"));
					Api.sendMsg2(rc, "9999", "인증결제", "trxType 파라메터가 없습니다.");
					logger.info("========== compPayAuth.trxType : " + compPayAuth.trxType + " ========== APICompPayAuth() END");
					return false;	
				}
				
				Pay pay = new Pay();
				//if (!CommonUtil.isNullOrSpace(payData.getString("vanTrxId"))) {	// 데이터 없을때
				//if (CommonUtil.isNullOrSpace(payData.getString("vanTrxId"))) {	// 데이터 있을때
					//logger.info("========== echo - payData : " + payData.getString("echo"));
					try {
						//pay.trxId = compPayAuth.trxId;	
						
						// 2022-10-20 - trxID 중복생성 이슈 수정
						pay.trxId = TrxDAO.getTrxId();
						
						pay.compNo = compPayAuth.compNo;
						pay.compMember = compPayAuth.compMember;
						pay.mchtId = compData.getString("mchtId");
						
						//pay.compMember = payData.getString("echo");
						//logger.info("========== echo - hm user_id: " + hm.get("echo"));
						//pay.salesId = compData.getString("salesId");	// 영업사원 아이디
						
						//pay.trxType = "COMPTR";
						pay.trxType = compPayAuth.trxType;
						pay.tmnId = compData.getString("tmnId");
						long trx_long = (long) (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
						pay.trackId = "AUTH" + trx_long;
						;//pay.trackId = (String) jObj.get("trackId");
						pay.amount = compPayAuth.amount;
						pay.payerName = compPayAuth.payerName;
						pay.payerTel = compPayAuth.payerTel;
						pay.payerEmail = compPayAuth.payerEmail;
		
						pay.card		= new Card();
						pay.card.number = "";
						pay.card.expiry = "";
						pay.card.installment = 0;
					
						if (CommonUtil.isNullOrSpace(compPayAuth.name)) {
							// 상품 미입력시 - NULL 처리
						} else {
							String prdName = compPayAuth.name;
							String prdNameChk = prdName.trim();
							if (CommonUtil.isNullOrSpace(prdNameChk)) {
								
							} else {
								// 상품명은 있으나, 가격, 개수 미입력시 = prcie = amount / qty = 1
								pay.products 	= new ArrayList<Product>();
								Product product = new Product();
								product.desc = "WEBPAY";
								product.name = prdName;
								
								Object data_price = compPayAuth.amount;
								if (data_price == null) {
									product.price = compPayAuth.amount;
								} else {
									if (data_price instanceof java.lang.Long) {
										long price = (long) data_price;
										if (price > 0) {
											product.price = price;
										} else {
											product.price = compPayAuth.amount;
										}
									}
								}
								Object data_qty = compPayAuth.qty;
								if (data_qty == null) {
									product.qty = 1;
								} else {
									if (data_qty instanceof java.lang.Long) {
										Integer qty = Integer.parseInt(String.valueOf(data_qty));
										if (qty > 0) {
											product.qty = qty;
										} else {
											product.qty = 1;
										}
									}
								}
								pay.products.add(product);
							}
						}
						pay.metadata = new SharedMap<String,String>();
						
						pay.encData = allat_enc_data;
						
					} catch (Exception e) {
						// TODO: handle exception
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : 잘못된 데이터입니다. 데이터를 변환 할수 없습니다. : Exception : " + e.getMessage());
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 데이터입니다. 데이터를 변환 할수 없습니다.");
						logger.info("========== APICompPayAuth() END");
						return false;
					}
					
					Request request = new Request();
					request.pay = pay; 
					String reqJson = GsonUtil.toJson(request, true, "");
					//logger.info("========== APICompPayAuth() reqJson : \n" + reqJson);
					String connStr = "";
					try {
						//connStr = compConnect("pay", reqJson, compPayAuth.mchtKey);
						connStr = compConnect("pay/auth/", reqJson, compPayAuth.mchtKey);
						//request = (Request) GsonUtil.fromJson(compConnect(reqJson, compPay.mchtKey), Request.class);
						//logger.info("========== APICompPayAuth() compConnect : \n" + connStr);
						// executeRefund(response);
					} catch (Exception e) {
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : compConnect Excetion : " + e);
						e.printStackTrace();
						logger.info("========== APICompPayAuth() END");
						//return false;
					}
					JSONParser connParser = new JSONParser();
					Object connObj = new Object();
					try {
						connObj = connParser.parse(connStr);
					} catch (ParseException e) {
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connStr : " + connStr);
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connObj : " + connObj);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== APICompPayAuth() END");
						return false;
					}
					JSONObject connjObj = (JSONObject) connObj;
				//	logger.info("========== APICompPayAuth() connjObj : " + connjObj);
					
					String connStr2 = connjObj.get("result").toString();
					JSONParser connParser2 = new JSONParser();
					Object connObj2 = new Object();
					try {
						connObj2 = connParser2.parse(connStr2);
					} catch (ParseException e) {
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connStr2 : " + connStr2);
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connObj2 : " + connObj2);
					}
					JSONObject connjObj2 = (JSONObject) connObj2;
					logger.info("========== APICompPayAuth() connjObj2 : \n" + connjObj2);
					logger.info("========== APICompPayAuth() connjObj2 resultCd : " + connjObj2.get("resultCd"));
					logger.info("========== APICompPayAuth() connjObj2 resultMsg : " + connjObj2.get("resultMsg"));
					logger.info("========== APICompPayAuth() connjObj2 advanceMsg : " + connjObj2.get("advanceMsg"));
					
					String connStr3 = connjObj.get("pay").toString();
					JSONParser connParser3 = new JSONParser();
					Object connObj3 = new Object();
					try {
						connObj3 = connParser3.parse(connStr3);
					} catch (ParseException e) {
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : 잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connStr3 : " + connStr3);
						logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== connObj3 : " + connObj3);
						e.printStackTrace();
						Api.sendMsg2(rc, "400", "Bad Request", "잘못된 암호데이터입니다. 데이터를 복호화 할수 없습니다.");
						logger.info("========== APICompPayAuth() END");
						return false;
					}
					JSONObject connjObj3 = (JSONObject) connObj3;
					logger.info("========== APICompPayAuth() connjObj3 : " + connjObj3);
					logger.info("========== APICompPayAuth() connjObj3 trxId : " + connjObj3.get("trxId"));
					//Api.sendMsg(rc, "200", "Success", "결제 성공");
					//Api.sendMsgPay2(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString(), connjObj3.get("trxId").toString());
					logger.info("========== APICompPayAuth() 전달데이터 조회 trackId : " + pay.trackId);
					dao.initRecord();
					dao.setDebug(true);
					dao.setColumns("A.mchtId, A.trxId, C.vanTrxId, B.vanResultCd, B.vanResultMsg, C.authCd,\n" + 
							"IFNULL(C.regDay, D.regDay) AS regDay, \n" + 
							"IFNULL(C.regTime, D.regTime) AS regTime, \n" + 
							"C.issuer, C.acquirer, A.bin, A.last4, A.installment, \n" + 
							"A.amount, A.payerName, G.name AS prdName, A.compNo, \n" +
							"A.compMember, B.van, B.vanId \n"
					);
					dao.setTable("PG_TRX_REQ A \n" + 
							"JOIN PG_TRX_RES B ON A.trxId = B.trxId \n" + 
							"LEFT JOIN PG_TRX_PAY C ON A.trxId = C.trxId \n" + 
							"LEFT JOIN PG_TRX_ERR D ON A.trxId = D.trxId \n" + 
							"LEFT JOIN PG_CODE_BIN E ON A.bin = E.bin \n" + 
							"LEFT JOIN PG_CODE F ON E.acquirer = F.codeName \n" + 
							"LEFT JOIN PG_TRX_PRD G ON A.prodId = G.prodId \n");
					dao.addWhere("A.trackId", pay.trackId);
					dao.setGroupBy("A.trxId");
					dao.setOrderBy("A.regDate DESC");
					SharedMap <String, Object> payDataMap = dao.search().getRowFirst();
					JSONObject body = new JSONObject();
					body.put("trxId", payDataMap.getString("trxId"));
					if (VertXUtil.getXRealIp(rc).equals("115.68.14.246")) {
						logger.info("========== APICompPayAuth() 링크나인 전달 PG거래번호 vanTrxId : " + payDataMap.getString("vanTrxId"));
						body.put("vanTrxId", payDataMap.getString("vanTrxId"));
						// 2022-03-28 PG상점코드, PG상점ID 추가
						body.put("van", payDataMap.getString("van"));
						body.put("vanId", payDataMap.getString("vanId"));
					}
					body.put("authCd", payDataMap.getString("authCd"));
					body.put("vanDay", payDataMap.getString("regDay"));
					body.put("vanTime", payDataMap.getString("regTime"));
					
					//body.put("cardCode", payDataMap.getString("idx"));
					body.put("issuer", payDataMap.getString("issuer"));			// 카드사[카드명] - ex : 삼성카드 
					// 2022-05-13 업체요청으로 카드코드테이블 데이터로 전달 처리
					if (C3Runner.cardcodeList.size() == 0) { 
						//TrxDAO trxDao = new TrxDAO();
						C3Runner.cardcodeList = trxDao.selectCardCodeList();
					}
					System.out.println("issuer : " + payDataMap.getString("issuer"));
					System.out.println("acquirer : " + payDataMap.getString("acquirer"));
					if (!"".equals(payDataMap.getString("acquirer"))) {
						if (C3Runner.cardcodeList.size() > 0) { 
							int acquirerChk = 0;
							//logger.info("========== C3Runner.cardcodeList : " + C3Runner.cardcodeList.toString());
							for (String key : C3Runner.cardcodeList.keySet()) {
							//	System.out.println("key : " + key);
								SharedMap<String, Object> data = C3Runner.cardcodeList.get(key);
								String code = data.getString("code");
								String cname = data.getString("cname");
								String calias = data.getString("calias");
								int compareChk = 0;
								compareChk = cname.compareTo(payDataMap.getString("acquirer"));
								if (compareChk > 1) {
									System.out.println();
									System.out.println("비교결과 : " + compareChk);
									System.out.println("리턴 카드사명 : [" + payDataMap.getString("acquirer") + "]");
									System.out.println("디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
									
									body.put("acquirerCode", code);	// 카드사코드 - ex : 001, 002
									body.put("acquirer", calias);	// 카드사 - ex : BC, 삼성
								} else if (compareChk == 0) {
									System.out.println();
									System.out.println("비교결과 : " + compareChk);
									System.out.println("리턴 카드사명 : [" + payDataMap.getString("acquirer") + "]");
									System.out.println("디비 카드사명 : code[" + code + "] cname[" + cname + "] calias[" + calias + "]");
									body.put("acquirerCode", code);	// 카드사코드 - ex : 001, 002
									body.put("acquirer", calias);	// 카드사 - ex : BC, 삼성
									break;
								} else if (compareChk < 0) {
								//	System.out.println("카드사명 결과 : " + compareChk);
									body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
									body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
								}
							}
						} else {
							body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
							body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
						}
					} else {
						body.put("acquirerCode", "");	// 카드사코드 - ex : 001, 002
						body.put("acquirer", payDataMap.getString("acquirer"));	// 발급사 - ex : BC, 삼성
					}
					System.out.println("최종 발급사 : " + body.get("acquirer"));
					System.out.println();
					
					if (CommonUtil.isNullOrSpace(payDataMap.getString("bin")) || CommonUtil.isNullOrSpace(payDataMap.getString("last4"))) {
						body.put("cardNumber", "");
					} else {
						body.put("cardNumber", payDataMap.getString("bin") + "******" + (String) payDataMap.getString("last4"));						
					}
					body.put("installment", payDataMap.getString("installment"));
					body.put("amount", payDataMap.getString("amount"));
					body.put("payerName", payDataMap.getString("payerName"));
					body.put("prdName", payDataMap.getString("prdName"));
					body.put("compNo", payDataMap.getString("compNo"));
					
					Api.sendMsgPayMap(rc, connjObj2.get("resultCd").toString(), connjObj2.get("resultMsg").toString(), connjObj2.get("advanceMsg").toString(), body);
					
					//Api.sharedMap.put(PAYUNIT.PAYLOAD, pay);
					logger.debug("");
				/*
				} else {
					logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : tbl_wellcome_notify 테이블에 데이터가 아직 미입력 되었습니다."");
					Api.sendMsg(rc, "400", "Bad Request", "tbl_wellcome_notify 테이블에 데이터가 아직 미입력 되었습니다."");
				}
				 */
				/*
				} else {
					logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : 주문번호 중복 등록. 중복 결제 할수 없습니다.");
					//Api.sendMsg(rc, "400", "Bad Request", "주문번호 중복 등록. 중복 결제 할수 없습니다.");
					Api.sendMsg(rc, "400", "Bad Request", "tbl_wellcome_notify 테이블에 데이터가 아직 미입력 되었습니다.");
				}
				*/
			} else {
				logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR : 존재하지 않는 가맹점키입니다.");
				logger.debug("========== compPayAuth.mchtKey : " + compPayAuth.mchtKey + " ========== APICompPayAuth() ERROR mchtKey : " + compPayAuth.mchtKey);
				logger.debug("========== APICompPayAuth() ERROR DB mchtKey : " + compData.getString("mchtKey"));
				Api.sendMsg2(rc, "401", "Unauthorized", "존재하지 않는 가맹점키입니다.");
			}
		}
		logger.info("========== APICompPayAuth() END");
		return false;
	}
}
