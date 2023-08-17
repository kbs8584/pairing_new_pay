package com.pgmate.pay.bean;

/**
 * @author Administrator
 *
 */
public class Refund extends Base {

	public String rootTrxId			= "";
	public String rootTrackId			= "";
	public String rootTrxDay			= "";
	public String authCd				= "";
	public String trxDate				= null;
	public String webhookUrl 		= null;
	
	public Integer cancelReqStat		= null;
	
	public String mchtId 						= null;
	
	public String compNo						= null;
	public String compMember				= null;
	
	// 2022-08-01 - 영업사원 ID 추가
	public String salesId						= null;
	
	public Refund() {
		// TODO Auto-generated constructor stub
	}

}
