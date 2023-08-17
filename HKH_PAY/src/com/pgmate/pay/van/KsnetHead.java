package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class KsnetHead {

	public boolean encrypt 	= true;
	public String version 	= "";	
	public String tid		= "";	
	public String companyCd	= "11111";
	public String trackId	= "";
	public int timeout		= 15;
	public String admin		= "LimJuseop";
	public String telNo		= "02-1522-2742";
	public String phoneNo	= "010-2742-3730";
	
	public KsnetRequest request = null;
	public KsnetResponse response = null;
	
	

	public KsnetHead() {
	}
	
	public byte[] getTransaction(){
		StringBuilder sb = new StringBuilder();
		if(encrypt){
			sb.append("2");
		}else{
			sb.append("0");
		}
		
		sb.append(version);
		sb.append(CommonUtil.byteFiller(tid,10));
		sb.append(CommonUtil.byteFiller(companyCd,5));
		sb.append(CommonUtil.byteFiller(trackId,12));
		sb.append(CommonUtil.zerofill(timeout, 2));
		sb.append(CommonUtil.byteFiller(admin,20));
		sb.append(CommonUtil.byteFiller(telNo,13));
		sb.append(CommonUtil.byteFiller(phoneNo,13));
		sb.append(CommonUtil.setFiller(43));
		sb.append(request.getTransaction());
		
		String tr = sb.toString();
		
		tr = CommonUtil.zerofill(tr.getBytes().length, 4)+tr;
		byte[] transaction = null;
		try{
			transaction = tr.getBytes("KSC5601");
		}catch(Exception e){
			
		}
		return transaction;
	}
	
	
	public void setTransaction(byte[] data){
		
		version 	= CommonUtil.toString(data, 1,4);
		tid			= CommonUtil.toString(data, 5,10).trim();
		companyCd	= CommonUtil.toString(data,15,5).trim();
		trackId		= CommonUtil.toString(data,20,12).trim();
		
		
		response 		= new KsnetResponse();
		
		response.spec   = CommonUtil.toString(data,123,4).trim();
		response.vanTr  = CommonUtil.toString(data,127,12).trim();
		response.status = CommonUtil.toString(data,139,1).trim();
		response.transactionDate  = CommonUtil.toString(data,140,12).trim();
		response.card   = CommonUtil.toString(data,152,20).trim();
		response.expiry = CommonUtil.toString(data,172,4).trim();
		response.quota  = CommonUtil.parseInt(CommonUtil.toString(data,176,2));
		response.amount = CommonUtil.toString(data,178,12).trim();
		response.message1= convert(data,190,16).trim();
		response.message2= convert(data,206,16).trim();
		response.authCode= convert(data,254,12).trim();
		response.cardName= convert(data,266,16).trim();
		response.issueCd = convert(data,282,2).trim();
		response.acqCd   = convert(data,284,2).trim();
		response.merchantId = convert(data,286,15).trim();
		response.sendYn  = convert(data,301,2).trim();
		response.notice  = convert(data,303,20).trim();
		response.occurePoint = convert(data,323,12).trim();
		response.usablePoint = convert(data,335,12).trim();
		response.addedPoint  = convert(data,347,12).trim();
		response.pointMessage= convert(data,359,40).trim();
	}
	
	public String convert(byte[] str,int start,int end){
		String s = "";
		  ByteArrayOutputStream os = new ByteArrayOutputStream();
		  try{
		  os.write(str,start,end);
		  s = os.toString("ksc5601");
		  }catch(Exception e){}
		  return s;
	}
	
	
	
	
	
	
	

}
