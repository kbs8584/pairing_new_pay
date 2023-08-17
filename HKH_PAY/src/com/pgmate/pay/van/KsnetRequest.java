package com.pgmate.pay.van;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class KsnetRequest {

	
	public String spec = ""; 	//0010 : MPI 가입확인 , //1110 : 승인 , 1120 : 인증후승인, 1130 : ISP-MPI,1210 :취소,1310 거래확인, 1320 : BIN조회, 1510 : TEST Call
	public String entry= "K"; 	//K:Key-In,S:Swipe
	public String card = ""; 	//VAN-TR 또는 카드번호=유효기간
	public int quota   = 00; 	//할부기간
	public String currency 	= "";//통화구분 //USD,KRW 
	public String amount = "";
	public String amountService = "";
	public String amountVat = "";
	
	//취소시 
	public String originAuthCode	= "";
	public String originTransactionDay = "";
	
	public String pwType	= "AA";	//AA:비밀번호 미사용, BB:암호화되지 않은 비밀번호, "01~99":암호화된 비번
	public String pinNo		= "";	//비밀번호
	
	public String productCd = "";	//상품코드
	public String identiNo	= ""; 	//주민번호 또는 사업자 번호 
	
	public String ecSecurity = "";	//전자상거래 보안등급
	public String ecDomain	= "";	//전자상거래 도메인
	public String ecIp		= "";	//전자상거래 서버 IP
	public String ecIdentiNo= "";	//전자상거래 몰 사업자 번호
	
	public String requestCard= "1";	//카드 정보 전송 구분 
	
	public String thirdType	 = "";	//I : ISP. M:MPI 
	public String mpiLoc	 = "";	//K: KSNET, R: Rmote , C: 제 3의 장소
	public String cavvRetry	 = "N";	//Y:재사용,  N:재사용아님
	
	public String ispKey	= "";
	public String ispData	= "";
	
	public String cavv		= "";
	public String xid		= "";
	public String eci		= "";
	
	
	//ISP : SESSIONKEYLEN(4)+SESSIONKEY+ENCRYPTED DATALEN(4)+ENCRYPOTED DATA
	//MPI : CAVV *40 +XID(40)+ECI (2)
	

	public KsnetRequest() {
	}
	
	public String getTransaction(){
		StringBuilder sb = new StringBuilder();
		sb.append(spec);
		sb.append(entry);
		sb.append(CommonUtil.byteFiller(card,37));
		sb.append(CommonUtil.zerofill(quota, 2));
		if(currency.equals("KRW")){
			sb.append("1");
			sb.append("0");	//소숫점 구분
			sb.append(CommonUtil.zerofill(amount, 12));
			sb.append(CommonUtil.zerofill(amountService, 12));
			sb.append(CommonUtil.zerofill(amountVat, 12));
		}else if(currency.equals("USD")){
			sb.append("2");
			sb.append("2");	//소숫점 구분
			sb.append(getRoundAmount(amount));
			sb.append(getRoundAmount(amountService));
			sb.append(getRoundAmount(amountVat));
		}else if(currency.equals("JPY")){
			sb.append("3");
			sb.append("0");	//소숫점 구분
			sb.append(CommonUtil.zerofill(amount, 12));
			sb.append(CommonUtil.zerofill(amountService, 12));
			sb.append(CommonUtil.zerofill(amountVat, 12));
		}else if(currency.equals("EUR")){
			sb.append("4");
			sb.append("2");	//소숫점 구분
			sb.append(getRoundAmount(amount));
			sb.append(getRoundAmount(amountService));
			sb.append(getRoundAmount(amountVat));
		}else if(currency.equals("CNY")){
			sb.append("5");
			sb.append("2");	//소숫점 구분
			sb.append(getRoundAmount(amount));
			sb.append(getRoundAmount(amountService));
			sb.append(getRoundAmount(amountVat));
		}else{
			sb.append(" ");
			sb.append("0");
			sb.append(CommonUtil.zerofill(amount, 12));
			sb.append(CommonUtil.zerofill(amountService, 12));
			sb.append(CommonUtil.zerofill(amountVat, 12));
		}
		
		sb.append(CommonUtil.byteFiller(originAuthCode, 12));
		sb.append(CommonUtil.byteFiller(originTransactionDay,6));
		sb.append(pwType);
		sb.append(CommonUtil.byteFiller(pinNo,16));
		sb.append(CommonUtil.byteFiller(productCd,6));
		sb.append(CommonUtil.byteFiller(identiNo,10));
		
		sb.append(CommonUtil.byteFiller(ecSecurity,1));
		sb.append(CommonUtil.byteFiller(ecDomain,40));
		sb.append(CommonUtil.byteFiller(ecIp,20));
		sb.append(CommonUtil.byteFiller(ecIdentiNo,10));
		
		sb.append(CommonUtil.byteFiller(requestCard,1));
		//가맹점 사용 ID, 사용 FIELD
		sb.append(CommonUtil.setFiller(32));
		//수표관련
		sb.append(CommonUtil.zerofill("", 40));
		//예비영역
		sb.append(CommonUtil.setFiller(30));
		//공인인증
		
		sb.append(CommonUtil.byteFiller(thirdType, 1));
		sb.append(CommonUtil.byteFiller(mpiLoc, 1));
		if(thirdType.equals("M")){
			sb.append(CommonUtil.byteFiller(cavvRetry, 1));
		}else{
			sb.append("N");
		}
		
		//ISP : SESSIONKEYLEN(4)+SESSIONKEY+ENCRYPTED DATALEN(4)+ENCRYPOTED DATA
		//MPI : CAVV *40 +XID(40)+ECI (2)
		if(thirdType.equals("I")){
			sb.append(CommonUtil.zerofill(ispKey.getBytes().length,4));
			sb.append(ispKey);
			sb.append(CommonUtil.zerofill(ispData.getBytes().length,4));
			sb.append(ispData);
		}else if(thirdType.equals("M")){
			sb.append(CommonUtil.byteFiller(cavv,40));
			sb.append(CommonUtil.byteFiller(xid,40));
			sb.append(CommonUtil.byteFiller(eci,40));
		}
		
		return sb.toString();
		
	}
	
	
	
	private String getRoundAmount(String amount){
		double amt = CommonUtil.parseDouble(amount)*100;
		return CommonUtil.zerofill(new Double(amt).longValue(),12);
	}

}

