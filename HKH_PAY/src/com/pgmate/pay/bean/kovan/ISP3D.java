package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class ISP3D {
	private String type				= "1100";	// 전문 번호 / 4 / "1100"
	private String bitmap			= "322000012AC10108";	// Primary bit map / 16 / "322000012AC10108"
	private String sBitmap			= "0000000000000000"; // Secondary bit map / 16 / "0000000000000000"
	private String trxType			= "011000"; // Processing Code / 6 / "011000" 
	public String amount			= ""; // 승인 요청 금액 / 12 / 
	public String currency			= "";	// 통화코드 / 3 / WON "410" , USD "840"
	public String reqDate			= ""; // 전문 전송 일시 / 10 / MMDDhhmmss
	public String trackId			= "";	// 전문 추적 번호 / 6 / PG에서 전문에 대한 일련번호 생성
	public String installment		= ""; // 할부기간 / 2 / 03~12
	public String companyCd			= "";	// 취급기관코드 / 5 / KVP에서 부여한 기관 코드 PGID 와 동일 예)이니시스 "19038"
	public String vpTrxId			= "";	// 거래고유번호 / 12 / 공란(KVP와 카드사간 사용, 응답코드에 들어갑니다.)
	public String pairingTmnId		= "";	// Terminal-ID / 16 / Left Just
	public String mchtId			= "";	// 가맹점 번호 / 15
	public String trxId				= "";	// TID / 40 / 상점 거래 ID(PG에서 취급하는 거래고유번호)
	public String cardCode			= "";	// 카드코드 / 22 / KVP 카드 코드 앞2자리 길이
	public String sessionKey		= "";	// SessionKey / 20 / 길이(4)+URLEncoding된SessionKey
	public String encData			= "";	// Encrypted Data / 10 / 길이(4)+URLEncoding된EncryptedData
	public String checkDigit		= "";	// Check_digit / 3 / ABC : A(1:체크, 0:체크안함), B(0:일반거래, 1:무이자), C(0:가맹점번호 체크 안함, 1:가맹점)
	public String mchtBRN			= "";	// 몰사업자번호 / 10 / 몰사업자번호(국민필요)
	public String mchtUrl			= ""; // 상점도메인 / 40 / 
	public String mchtIp			= "";	// 유저IP / 20 /
	public String product			= "";	// 상품명 / 80 / (조회시 사용)
	public String mchtTrackId		= "";	// 상점정보 / 40 / 상점명 또는 PG에서 취급하는 상점고유번호(조회시 사용) 
	
	
	
	public ISP3D() {
	}


	
	public String get() {
		StringBuilder trx = new StringBuilder();
		trx.append(type);
		trx.append(bitmap);
		trx.append(sBitmap);
		trx.append(trxType);
		trx.append(CommonUtil.zerofill(amount, 12));
		trx.append(CommonUtil.byteFiller(currency, 3));
		trx.append(CommonUtil.zerofill(reqDate, 10));
		trx.append(CommonUtil.byteFiller(trackId, 6));
		trx.append(CommonUtil.zerofill(installment, 2));
		trx.append(CommonUtil.byteFiller(companyCd, 5));
		trx.append(CommonUtil.byteFiller(vpTrxId, 12));
		trx.append(CommonUtil.byteFiller(pairingTmnId, 16));
		trx.append(CommonUtil.byteFiller(mchtId, 15));
		trx.append(CommonUtil.byteFiller(trxId, 40));
		trx.append(CommonUtil.byteFiller(cardCode, 22));
		trx.append(CommonUtil.zerofill(sessionKey.getBytes().length, 4)+sessionKey);
		trx.append(CommonUtil.zerofill(encData.getBytes().length, 4)+encData);
		trx.append(CommonUtil.zerofill(checkDigit, 3)); 
		trx.append(CommonUtil.zerofill(mchtBRN, 10));  
		trx.append(CommonUtil.byteFiller(mchtUrl, 40));
		trx.append(CommonUtil.byteFiller(mchtIp, 20));
		trx.append(CommonUtil.byteFiller(product, 80));
		trx.append(CommonUtil.byteFiller(mchtTrackId, 40));
		
		String trxPlain = trx.toString();
		return CommonUtil.zerofill(trxPlain.getBytes().length, 4)+trxPlain;
	}
   
}
