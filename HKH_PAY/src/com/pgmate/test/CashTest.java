package com.pgmate.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.popbill.api.PopbillException;
import com.popbill.api.Response;
import com.popbill.api.cashbill.Cashbill;
import com.popbill.api.cashbill.CashbillInfo;
import com.popbill.api.cashbill.CashbillServiceImp;

/**
 * @author Administrator
 *
 */
public class CashTest {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.test.CashTest.class);
	public long code = 0;
	public String message = "";
	public CashTest() {
		// TODO Auto-generated constructor stub
	}
	
	public void issue(CashbillServiceImp cbs ){
		String idenNum = "xxxxxxxxx";
		String trxId		=  "CR_"+CommonUtil.getCurrentDate("yyMMddHHmmss");
		
		
		
		Cashbill cb = new Cashbill();
		cb.setMgtKey(trxId);
		cb.setTradeType("승인거래");  	//승인거래,취소거래
		//cb.setOrgConfirmNum(""); //원승인번호 
		//cb.setOrgTradeDate("원승인일자");
		cb.setTradeUsage("소득공제용");//지출증빙용
		cb.setIdentityNum("0101112222");//주민등록번호/휴대폰/카드번호
		cb.setTaxationType("과세");//과세/비과세
		cb.setSupplyCost("10000");	//공급가액
		cb.setTax("1000");			//부가세
		cb.setServiceFee("0");		//봉사료
		cb.setTotalAmount("11000"); //합계금액 봉사료+공급가액+_세액

		
	
		cb.setFranchiseCorpNum(idenNum);
		cb.setFranchiseCorpName("발행자상호");
		cb.setFranchiseCEOName("발행자 대표자");
		cb.setFranchiseAddr("발행자 주소");
		cb.setFranchiseTEL("07043042991");
		cb.setSmssendYN(false);
		cb.setCustomerName("고객명");
		cb.setItemName("상품명");
		cb.setEmail("test@test.com");
		cb.setHp("010111222");
		cb.setFax("070111222");
		
		if(registIssue(cbs,idenNum,cb,"")){
			CashbillInfo cbi = get(cbs,idenNum,trxId);
			if(cbi == null){
				logger.info("info error : code : {},message : "+code,message);
			}else{
				System.out.println("거래번호 : "+cbi.getItemKey());
				System.out.println("관리번호 : "+cbi.getMgtKey());
				System.out.println("거래일자 : "+cbi.getTradeDate());
				System.out.println("발행일시 : "+cbi.getIssueDT());
				System.out.println("등록일시 : "+cbi.getRegDT());
				System.out.println("상태코드 : "+cbi.getStateCode());
				System.out.println("상태변경 : "+cbi.getStateDT());
				System.out.println("승인번호 : "+cbi.getConfirmNum());
				System.out.println("원거래일자 : "+cbi.getOrgTradeDate());
				System.out.println("원승인번호 : "+cbi.getOrgConfirmNum());
				System.out.println("국세청전송일자 : "+cbi.getNtssendDT());
				System.out.println("국세청결과수신 : "+cbi.getNtsresultDT());
				System.out.println("국세청결과코드 : "+cbi.getNtsresultCode());
				System.out.println("국세청메세지 : "+cbi.getNtsresult());
				

			}
			
			
			
			
		}else{
			logger.info("issue error : code : {},message : "+code,message);
		}
	}
	

	public boolean registIssue(CashbillServiceImp cbs,String idenNum,Cashbill cb,String memo){
		Response res = null;
		try {

			res = cbs.registIssue(idenNum, cb, "메모");
			code = res.getCode();
			message = res.getMessage();
		}catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		
		if(code == 1){
			return true;
		}else{
			return false;
		}
	}
	
	
	public CashbillInfo get(CashbillServiceImp cbs,String idenNum,String trxId){
		CashbillInfo cashbillInfo = null;
		try {
			cashbillInfo = cbs.getInfo(idenNum, trxId);
		} catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		return cashbillInfo;
	}
	
	
	
	public static void main(String[] args){
		CashTest c = new CashTest();
		CashbillServiceImp cbs = new CashbillServiceImp();
		cbs.setLinkID("PAIRINGSOLUTION");
		cbs.setSecretKey("ctf4uzjhR/4AvpdRpPA3EP/tQGDPvRW1pFX9KK2D3QI=");
		cbs.setTest(false);
		//c.issue(cbs);
		System.out.println("run");
		CashbillInfo cbi = c.get(cbs,"1208815955","T180413028257");
		System.out.println("get");
		if(cbi == null){
			System.out.println(c.code);
			System.out.println(c.message);
		}else{
			
			System.out.println("거래번호 : "+cbi.getItemKey());
			System.out.println("관리번호 : "+cbi.getMgtKey());
			System.out.println("거래일자 : "+cbi.getTradeDate());
			System.out.println("발행일시 : "+cbi.getIssueDT());
			System.out.println("등록일시 : "+cbi.getRegDT());
			System.out.println("상태코드 : "+cbi.getStateCode());
			System.out.println("상태변경 : "+cbi.getStateDT());
			System.out.println("승인번호 : "+cbi.getConfirmNum());
			System.out.println("원거래일자 : "+cbi.getOrgTradeDate());
			System.out.println("원승인번호 : "+cbi.getOrgConfirmNum());
			System.out.println("국세청전송일자 : "+cbi.getNtssendDT());
			System.out.println("국세청결과수신 : "+cbi.getNtsresultDT());
			System.out.println("국세청결과코드 : "+cbi.getNtsresultCode());
			System.out.println("국세청메세지 : "+cbi.getNtsresult());
			

		}
	}
	
	
	
	
	
	
	

}
