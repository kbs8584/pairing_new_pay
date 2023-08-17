package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class SmartroWH {

	public String PayMethod = null;
	public String MID = null;
	public String MallUserID = null;
	public String mallUserID = null;
	public String Amt = null;
	public String GoodsName = null;
	public String OID = null;
	public String TID = null;
	public String AuthDate = null;
	public String AuthCode = null;
	public String FnCd = null;
	public String fn_cd = null;
	public String FnName = null;
	public String fn_name = null;
	public String StateCd = null;
	public String state_cd = null;
	public String MallReserved = null;
	public String name = null;
	public String Name = null;
	public String CardQuota = null;
	public String ReceiptType = null;
	public String CardUsePoint = null;
	public String OTID = null;
	public String AppDt = null;
	public String CcDt = null;
	public String TransStatus = null;
	public String SvcCd = null;
	public String SvcPrdtCd = null;
	public String pinNo = null;
	public String BuyerEmail = null;
	public String BuyerAuthNum = null;
	public String BuyerTel = null;
	public String ResultCode = null;
	public String ResultMsg = null;
	public String SignValue = null;

	public SmartroWH() {
		// TODO Auto-generated constructor stub
	}

	public SharedMap<String, Object> toJson() {
		SharedMap<String, Object> map = new SharedMap<String, Object>();
		map.put("PayMethod", this.PayMethod);
		map.put("MID", this.MID);
		map.put("MallUserID", this.MallUserID);
		map.put("mallUserID", this.mallUserID);
		map.put("Amt", this.Amt);
		map.put("GoodsName", this.GoodsName);
		map.put("OID", this.OID);
		map.put("TID", this.TID);
		map.put("AuthDate", this.AuthDate);
		map.put("AuthCode", this.AuthCode);
		map.put("FnCd", this.FnCd);
		map.put("fn_cd", this.fn_cd);
		map.put("FnName", this.FnName);
		map.put("fn_name", this.fn_name);
		map.put("StateCd", this.StateCd);
		map.put("state_cd", this.state_cd);
		map.put("MallReserved", this.MallReserved);
		map.put("name", this.name);
		map.put("Name", this.Name);
		map.put("CardQuota", this.CardQuota);
		map.put("ReceiptType", this.ReceiptType);
		map.put("CardUsePoint", this.CardUsePoint);
		map.put("OTID", this.OTID);
		map.put("AppDt", this.AppDt);
		map.put("CcDt", this.CcDt);
		map.put("TransStatus", this.TransStatus);
		map.put("SvcCd", this.SvcCd);
		map.put("SvcPrdtCd", this.SvcPrdtCd);
		map.put("pinNo", this.pinNo);
		map.put("BuyerEmail", this.BuyerEmail);
		map.put("BuyerAuthNum", this.BuyerAuthNum);
		map.put("BuyerTel", this.BuyerTel);
		map.put("ResultCode", this.ResultCode);
		map.put("ResultMsg", this.ResultMsg);
		map.put("SignValue", this.SignValue);
		return map;
	} 

}
