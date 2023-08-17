package com.pgmate.pay.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Auth;
import com.pgmate.pay.bean.Cash;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.bean.VactHookBean;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class TrxDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.dao.TrxDAO.class);

	private static int glb_processcnt = 0;
	private static int glb_prevday = 0;
	private static String glb_lasttrxid = "";
	
	private static String glb_trxidFirstInitial = "";
	
	public TrxDAO() {
		//super.setDebug(false);
		super.setDebug(true);
	}

	public static String getFunction(String function, String value) {
		String returnVal = "";
		String query = "SELECT " + function + "(?) as val";

		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;

		try {

			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, value);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				returnVal = rset.getString(1);
			}
			conn.commit();
		} catch (Exception t) {
			logger.debug("sql error : {}, query : {}", t.getMessage(), query);
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}

	public synchronized static String getTrxId() {
		String trxId = "";
		/*
		int limit = 3;
		for (int i = 0; i < limit; i++) {
			trxId = "T" + getFunction("FN_NEXTVAL2", "TRN");
			if (trxId.length() > 3) {
				break;
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				
			}
		}

		if (trxId.equals("T")) {
			return "T" + CommonUtil.getCurrentDate("yyMMdd") + UUID.randomUUID().toString().substring(0, 6);
		}
		*/
		//LoadProcessCntFromDB();
		
		// 2022-04-04 : 주문번호 중복 오류로 인한 수정
		//trxId = "T";
		trxId = glb_trxidFirstInitial;	// 2022-06-22 PG_COMPANY 테이블참조하여 서버정보기반으로 설정 - 운영 첫글자 T / 개발 첫글자 D
		if (glb_trxidFirstInitial == null || "".equals(glb_trxidFirstInitial)) {
			trxId = getServerMode();	// 2022-06-22 PG_COMPANY 테이블참조하여 서버정보기반으로 설정 - 운영 첫글자 T / 개발 첫글자 D
		}
		
		trxId += GetCurrentFromCalendar();
		//logger.info("========== getTrxId trxId : " + trxId);
		if(glb_lasttrxid == null) glb_lasttrxid = "19700101";
		
		if (glb_lasttrxid.equals(trxId) == true) {
			glb_processcnt++;
			logger.info("========== getTrxId trxId duplicated 페어링주문번호 발급 : 기존 주문번호와 동일하여 주문번호 값을 증가합니다. : " + trxId + " => " + glb_processcnt);
		}
		glb_lasttrxid = trxId;
		//logger.info("========== getTrxId FINAL trxId : " + trxId);
		//logger.info("========== trxId :: " + trxId);
		return trxId;
	}
	public synchronized static String getServerMode() {
	//	logger.info("========== getServerMode() ");
		String mode = "";
		DAO dao = new DAO();
        dao.setDebug(true);
        dao.setTable("PG_COMPANY");
        dao.setColumns("*");
        SharedMap<String, Object> seqMap = dao.search().getRowFirst();
        if (seqMap.get("apiHost") != null) {
        	if ("175.207.29.23".equals(seqMap.get("apiHost"))) {
        		mode = "T";
        	} else if ("222.237.78.137".equals(seqMap.get("apiHost"))) {
        		mode = "T";
        	} else if ("220.86.113.220".equals(seqMap.get("apiHost"))) {
        		mode = "D";
        	} else {
        		mode = "D";
        	}
        }
     //   logger.info("========== apiHost : " + seqMap.get("apiHost"));
    //    logger.info("========== trxId Initial mode : " + mode);
        if (seqMap.get("apiDomain") != null) {
        	if ("http://apis-dev.pairingpayments.net".equals(seqMap.get("apiDomain"))) {
        		mode = "D";
        	} else if ("http://api.pairingpayments.net".equals(seqMap.get("apiDomain"))) {
        		mode = "T";
        	}
        }
    //    logger.info("========== apiDomain : " + seqMap.get("apiDomain"));
    //    logger.info("========== trxId Initial mode : " + mode);
        glb_trxidFirstInitial = mode;
    //    logger.info("========== trxId Initial mode glb_trxidFirstInitial : " + glb_trxidFirstInitial);
        return mode;
	}
	// 2022-04-04 페어링 주문번호 시퀀스 생성
	public synchronized static String GetCurrentFromCalendar() {
		String timeStamp = new SimpleDateFormat("yyMMdd").format(Calendar.getInstance().getTime());
        //현재의 년/월/일을 int로 변환하여 가져온다.
		int curday = GetCurrentFromCalendarDay();
		// int로 변환된 현재일이 이전에 작업된 시간과 다르다면 glb_processcnt를 0으로 만든다.
		if (curday != glb_prevday) glb_processcnt = 0;
		// 현재 작업일을 이전 작업일에 넣는다.
		glb_prevday = curday;

		// 1000000 건이 넘지 않도록 한다. 숫자 6자리
		if (glb_processcnt > 999999) glb_processcnt = 0;

		String substring = String.format("%06d", glb_processcnt);

		// 자 여기에 DB에 glb_processcnt를 저장하는 코드가 들어가야 한다.
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setTable("PG_SEQUENCE");
		dao.setRecord("processcnt", glb_processcnt);
		dao.setRecord("prevday", glb_prevday);
		// 2022-07-07 idx 추가
		dao.setWhere("idx = 1");
		if (dao.update()) {
			logger.info("========== GetCurrentFromCalendar - 페어링 주문번호 DB 저장 OK : glb_processcnt = " + glb_processcnt + " |  glb_prevday = "+ glb_prevday );
		} else {
			logger.info("========== GetCurrentFromCalendar - 페어링 주문번호 ERROR : PG_SEQUENCE DB ERROR 저장없이 진행합니다.");
		}
		// update glb_processcnt
		glb_processcnt++;
		//logger.info("========== GetCurrentFromCalendar - 페어링 주문번호 NEXT : " + glb_processcnt);
        return timeStamp + substring;
    }
	// 2022-04-04 페어링 주문번호 시퀀스 일자
	private static int GetCurrentFromCalendarDay() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
        return Integer.parseInt(timeStamp);
    }
	
	// 2022-04-04 페어링 주문번호 시퀀스 DB 로드 추가
    public synchronized static int LoadProcessCntFromDB() {
        int pcnt = 0;
        //logger.info("========== LoadProcessCntFromDB() - 페어링 주문번호 시퀀스 DB 로드");
        //여기에 DB로 부터 저장된 glb_processcnt 카운트를 읽어와 넣는다.
        DAO dao = new DAO();
        dao.setDebug(true);
        dao.setTable("PG_SEQUENCE");
        dao.setColumns("*");
        // 2022-07-07 idx 추가
        dao.setWhere("idx = 1");
        SharedMap<String, Object> seqMap = dao.search().getRowFirst();
        if (seqMap.get("processcnt") != null) {
        	glb_processcnt = seqMap.getInt("processcnt");
        	glb_prevday = seqMap.getInt("prevday");
        }
        glb_processcnt++;     //그리고 이미 해당 glb_processcnt 카운트는 사용이 되었으니 1을 더해준다.
        //logger.info("========== LoadProcessCntFromDB() - 페어링 주문번호 시퀀스 DB 로드 : glb_processcnt : " + glb_processcnt);
        return pcnt;
    }
	
	public synchronized static String getTrxId(String trxDay) {
		return "T" + trxDay+getFunction("FN_NEXTVAL", "TRN");
	}

	public synchronized static String getCapId() {
		return "C" + getFunction("FN_NEXTVAL2", "CAP");
	}

	public synchronized static String getSettleId() {
		return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
	}

	public synchronized static String getBillId() {
		return "B" + getFunction("FN_NEXTVAL2", "BILL");
	}
	
	public synchronized static String getCashId() {
		return "H" + getFunction("FN_NEXTVAL2", "CASH");
	}
	
	
	public synchronized static String getVactIssueId() {
		return "VI" + getFunction("FN_NEXTVAL2", "VACT_ISSUE");
	}
	
	//WH
	public synchronized SharedMap<String, Object> getMchtTmnByTmnId(String tmnId) {
		logger.info("========== PAYUNIT.TRX_ID : " + PAYUNIT.TRX_ID + " ========== getMchtTmnByTmnId - tmnId : " + tmnId);
		setDebug(true);
		super.setTable("PG_MCHT_TMN");
		super.setColumns("*");
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	// 2021-12-28 가맹점명으로 찾기
	public synchronized SharedMap<String, Object> getMchtTmnByMchtId(String mchtId) {
		logger.info("========== getMchtTmnByMchtId() - mchtId : " + mchtId);
		setDebug(true);
		super.setTable("PG_MCHT_TMN A JOIN PG_MCHT B ON A.mchtId = B.mchtId");
		super.setColumns("A.*");
		super.addWhere("B.mchtId", mchtId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getMchtTmnByPayKey(String payKey) {
		logger.info("========== getMchtTmnByPayKey() - payKey : " + payKey);
		setDebug(true);
		super.setTable("PG_MCHT_TMN");
		super.setColumns("*");
		super.addWhere("payKey", payKey, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
		
	}
	
	public synchronized SharedMap<String, Object> getMchtMngBillMap(String mchtId,String tmnId) {
		String key = "PG_MCHT_MNG_BILL_" + mchtId +tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG_BILL");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			super.addWhere("tmnId", tmnId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	

	public synchronized SharedMap<String, Object> getMchtByMchtId(String mchtId) {
		logger.info("========== getMchtByMchtId() - mchtId : " + mchtId);
		String key = "PG_MCHT_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("PG_MCHT");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	public synchronized SharedMap<String, Object> getMchtByMchtId2(String mchtId) {
		logger.info("========== getMchtByMchtId2() - mchtId : " + mchtId);
		String key = "PG_MCHT_" + mchtId;
		super.setDebug(true);
		super.setTable("PG_MCHT");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return PAYUNIT.cacheMap.put(key, rset.getRow(0));
	}

	public synchronized SharedMap<String, Object> getMchtMngByMchtId(String mchtId) {
		logger.info("========== getMchtMngByMchtId() - mchtId : " + mchtId);
		String key = "PG_MCHT_MNG_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("PG_MCHT_MNG");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	public synchronized SharedMap<String, Object> getMchtMngByMchtId2(String mchtId) {
		logger.info("========== getMchtMngByMchtId2() - mchtId : " + mchtId);
		String key = "PG_MCHT_MNG_" + mchtId;
		super.setDebug(true);
		super.setTable("PG_MCHT_MNG");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return PAYUNIT.cacheMap.put(key, rset.getRow(0));
	}
	
	
	public synchronized SharedMap<String, Object> getMchtMngByMchtIdImmediate(String mchtId) {
		super.setTable("PG_MCHT_MNG");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getAgencyMngById(String agencyId) {
		String key = "PG_MAM_AGENCY_MNG_" + agencyId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_AGENCY_MNG");
			super.setColumns("*");
			super.addWhere("agencyId", agencyId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public synchronized SharedMap<String, Object> getDistMngById(String distId) {
		String key = "PG_MAM_DIST_MNG_" + distId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_DIST_MNG");
			super.setColumns("*");
			super.addWhere("distId", distId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public synchronized SharedMap<String, Object> getSalesMngById(String salesId) {
		String key = "PG_MAM_SALES_MNG_" + salesId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_SALES_MNG");
			super.setColumns("*");
			super.addWhere("salesId", salesId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized SharedMap<String, Object> getLoanMngById(String salesId) {
		String key = "PG_LOAN_MNG_" + salesId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_LOAN_MNG");
			super.setColumns("*");
			super.addWhere("loanId", salesId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized SharedMap<String, Object> getOrgFee(String van) {
		String key = "PG_ORG_FEE_" + van;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_ORG_FEE");
			super.setColumns("*");
			super.addWhere("van", van, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public synchronized SharedMap<String, Object> getTrxSum(SharedMap<String, Object> mchtMap) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as mchtDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as mchtMonthlySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and substr(regDay,1,4) =DATE_FORMAT(now(),'%Y') ) as mchtYearSum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE agencyId ='" + mchtMap.getString("agencyId") + "' and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as agencyDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE agencyId ='" + mchtMap.getString("agencyId") + "' and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as agencyMonthlySum, ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE distId   ='" + mchtMap.getString("distId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as distDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE distId   = '" + mchtMap.getString("distId") + "'  and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as distMonthlySum ");
		sb.append(" FROM DUAL");

		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	public synchronized SharedMap<String, Object> getTrxSum2(SharedMap<String, Object> mchtMap) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT A.mchtDailySum, A.mchtDailySum+A.mchtMonthlySum as mchtMonthlySum FROM ( ");
		sb.append(" SELECT ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM PG_TRX_CAP WHERE regDay =DATE_FORMAT(now(),'%Y%m%d') AND mchtId   = '" + mchtMap.getString("mchtId") + "' ) as mchtDailySum , ");
		sb.append(" ( SELECT ifnull(sum(saleAmount+rfdAmount),0) FROM PG_TOT_CAP WHERE capMonth =DATE_FORMAT(now(),'%Y%m') AND mchtId   ='" + mchtMap.getString("mchtId") + "'  ) as mchtMonthlySum  ");
		sb.append(" FROM DUAL");
		sb.append(") A");

		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	
	//20201123 터미널 일 합계금액
	public synchronized SharedMap<String, Object> getTrxTmnDaySum(SharedMap<String, Object> tmnMap) {
		logger.info("========== getTrxTmnDaySum() ");
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT IFNULL(SUM(amount),0) as tmnDailySum FROM PG_TRX_CAP ");
		sb.append(" WHERE DATE_FORMAT(regDay,'%Y%m%d') = DATE_FORMAT(now(),'%Y%m%d') AND tmnId = '"+tmnMap.getString("tmnId")+"'");

		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	//20201123 터미널 월 합계금액
	public synchronized SharedMap<String, Object> getTrxTmnMonthSum(SharedMap<String, Object> tmnMap) {
		logger.info("========== getTrxTmnMonthSum() ");
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT IFNULL(SUM(amount),0) as tmnMonthSum FROM PG_TRX_CAP ");
		sb.append(" WHERE DATE_FORMAT(regDay,'%Y%m') = DATE_FORMAT(now(),'%Y%m') AND tmnId = '"+tmnMap.getString("tmnId")+"' ");

		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	//20201123 터미널 년 합계금액
	public synchronized SharedMap<String, Object> getTrxTmnYearSum(SharedMap<String, Object> tmnMap) {
		logger.info("========== getTrxTmnYearSum() ");
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT IFNULL(SUM(amount),0) as tmnYearSum FROM PG_TRX_CAP ");
		sb.append(" WHERE DATE_FORMAT(regDay,'%Y') = DATE_FORMAT(now(),'%Y') AND tmnId = '"+tmnMap.getString("tmnId")+"' ");

		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized boolean isDuplicatedTrackId(String mchtId, String trackId) {
		logger.info("========== isDuplicatedTrackId() - 중복거래번호 확인 - mchtId : " + mchtId + " - trackId : " + trackId);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	
	public synchronized boolean isDuplicatedVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RES");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public synchronized boolean isDuplicatedRFDVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public synchronized void insertProduct(String prodId, List<Product> products, String regDate) {
		logger.info("========== insertProduct() - prodId : {}", prodId + " - size : " + products.size());
		super.initRecord();

		int i = 1;
		super.setDebug(true);
		super.setTable("PG_TRX_PRD");
	//	for (int j = 0; j < products.size(); j++) {
	//		logger.info("========== products : j = " + i + " ::: name + " + products.get(i).name + " qty : " + products.get(i).qty + ", price : " + products.get(i).price);
	//	}
		for (Product product : products) {
			super.setRecord("prodId", prodId);//1개
			super.setRecord("description", CommonUtil.nToB(product.desc));
			super.setRecord("name", CommonUtil.nToB(product.name));
			super.setRecord("price", product.price);
			super.setRecord("qty", product.qty);
			super.setRecord("regDate", regDate);
			// 2021-08-18 로그확인후 추가
			super.setRecord("mchtId", product.mchtId);
			logger.info("========== insertProduct() - PG_TRX_PRD insert() : {}, {}", i++, super.insert());
			super.initRecord();
		}
	}

	public static String getIssuer(String issuer) {
		if (issuer.startsWith("KB") || issuer.indexOf("국민") > -1 || issuer.indexOf("신세계한미") > -1) {
			return "국민";
		} else if (issuer.startsWith("NH") || issuer.indexOf("농협") > -1) {
			return "농협";
		} else if (issuer.indexOf("롯데") > -1) {
			return "롯데";
		} else if (issuer.indexOf("삼성") > -1) {
			return "삼성";
		} else if (issuer.indexOf("신한") > -1) {
			return "신한";
		} else if (issuer.indexOf("비씨") > -1 || issuer.indexOf("BC") > -1 || issuer.indexOf("신세계한미") > -1) {
			return "비씨";
		} else if (issuer.indexOf("현대") > -1) {
			return "현대";
		} else if (issuer.indexOf("하나") > -1 || issuer.indexOf("외환") > -1) {
			return "하나";
		} else {
			if (issuer.indexOf("광주") > -1 || issuer.indexOf("제주") > -1 || issuer.indexOf("강원") > -1 || issuer.indexOf("조흥") > -1 || issuer.indexOf("신한") > -1) {
				return "신한";
			} else if (issuer.indexOf("우리") > -1 || issuer.indexOf("전북") > -1 || issuer.indexOf("수협") > -1 || issuer.indexOf("씨티") > -1 || issuer.indexOf("산업") > -1 || issuer.indexOf("기업") > -1 || issuer.indexOf("시티") > -1
					|| issuer.indexOf("우체국") > -1 || issuer.indexOf("신협") > -1 || issuer.indexOf("새마을") > -1) {
				return "비씨";
			} else {
				logger.info("UNKNOWN ISSUER : {}", issuer);
				return "";
			}
		}
	}
	
	public synchronized SharedMap<String,Object> getDBIssuer(String bin){
		logger.info("========== getDBIssuer() - bin : " + bin);
		SharedMap<String,Object> issuerMap = new SharedMap<String,Object>();
		if (CommonUtil.isNullOrSpace(bin)) {
			return issuerMap;
		}
		
		String key = "PG_CODE_BIN_" + bin;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("PG_CODE_BIN");
			super.setColumns("*");
			super.addWhere("bin", bin, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if (rset.size() == 0) {
				issuerMap.put("bin", bin);
				issuerMap.put("issuer", "기타");
				issuerMap.put("type", "신용");
				return issuerMap;
			} else {
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}
		}
	}

	public synchronized void insertCard(String cardId, String value) {
		logger.info("========== insertCard() - cardId : " + cardId + " - value : " + value);
		super.setTable("PG_TRX_BOX");

		super.setRecord("cardId", cardId);//1개
		super.setRecord("value", value);
		logger.info("========== insertCard() - insertCard() - insert() : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized SharedMap<String,Object> getByCardId(String cardId) {
		RecordSet rset = super.query("SELECT `value` FROM PG_TRX_BOX WHERE cardId ='"+cardId+"'");
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String,Object> getByKsnetCardId(String cardId,String mchtId) {
		logger.info("========== getByKsnetCardId() - cardId : " + cardId + " - mchtId : " + mchtId);
		super.setDebug(true);
		RecordSet rset = super.query("SELECT * FROM PG_TRX_AUTH WHERE cardId ='"+cardId+"' AND mchtId ='"+mchtId+"' AND resultCd ='0000'");
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void insertKsnetCard(SharedMap<String, Object> sharedMap, Auth auth) {
		super.setTable("PG_TRX_AUTH");
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));//1개
		super.setRecord("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("trxType", auth.trxType);//1개
		super.setRecord("trackId", auth.trackId);
		super.setRecord("authKey", sharedMap.getString("authKey"));
		super.setRecord("unit",auth.card.number);
		
		super.setRecord("payerName",auth.payerName);
		super.setRecord("payerEmail",auth.payerEmail);
		super.setRecord("payerTel",auth.payerTel);
		super.setRecord("udf1", CommonUtil.cut(auth.udf1, 100));
		super.setRecord("udf2", CommonUtil.cut(auth.udf2, 100));

		super.setRecord("expiry",auth.card.expiry);
		super.setRecord("issuer",auth.card.issuer);
		super.setRecord("cardType",auth.card.cardType);
		super.setRecord("acquirer",auth.card.acquirer);
		super.setRecord("van",sharedMap.getString("van"));
		super.setRecord("vanTrxId",sharedMap.getString("vanTrxId"));
		super.setRecord("resultCd",sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg",sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		
		logger.info("insertKsnetCard insert() : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized void insertKsnetCard(SharedMap<String,Object> sharedMap, Pay pay) {
		logger.info("========== insertKsnetCard() - pay.card.acquirer : " + pay.card.acquirer);
		super.setDebug(true);
		super.setTable("PG_TRX_AUTH");
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));//1개
		super.setRecord("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("trxType", pay.trxType);//1개
		super.setRecord("trackId", pay.trackId);
		super.setRecord("authKey", sharedMap.getString("authKey"));
		super.setRecord("unit",pay.card.number);
		
		super.setRecord("payerName",pay.payerName);
		super.setRecord("payerEmail",pay.payerEmail);
		super.setRecord("payerTel",pay.payerTel);
		super.setRecord("udf1", CommonUtil.cut(pay.udf1, 100));
		super.setRecord("udf2", CommonUtil.cut(pay.udf2, 100));

		super.setRecord("expiry",pay.card.expiry);
		super.setRecord("issuer",pay.card.issuer);
		super.setRecord("cardType",pay.card.cardType);
		super.setRecord("acquirer",pay.card.acquirer);
		super.setRecord("van",sharedMap.getString("van"));
		super.setRecord("vanTrxId",sharedMap.getString("vanTrxId"));
		super.setRecord("resultCd",sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg",sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		
		logger.info("========== insertKsnetCard() - insert() : {}", super.insert());
		super.initRecord();
	}

	public synchronized void insertTrxREQ(SharedMap<String, Object> sharedMap, Response response) {
		logger.info("========== insertTrxREQ() ");
		super.setDebug(true);
		super.setTable("PG_TRX_REQ");
/*
logger.debug("========== trxId : " + response.pay.trxId);
logger.debug("========== trxType : " + response.pay.trxType);
//logger.debug("========== mchtId " + sharedMap.getString(PAYUNIT.MCHTID));
logger.debug("========== mchtId : " + response.pay.mchtId);
logger.debug("========== tmnId : " + response.pay.tmnId);
logger.debug("========== trackId : " + response.pay.trackId);
logger.debug("========== payerName : " + response.pay.payerName);
logger.debug("========== payerEmail : " + response.pay.payerEmail);
logger.debug("========== payerTel : " + response.pay.payerTel);
logger.debug("========== amount : " + response.pay.amount);
logger.debug("========== cardId : " + response.pay.card.cardId);

logger.debug("========== acquirer : " + response.pay.card.acquirer);
logger.debug("========== issuer : " + response.pay.card.issuer);
logger.debug("========== prodId : " + response.pay.products.get(0).prodId);
logger.debug("========== regDate : " + sharedMap.getString(PAYUNIT.REG_DATE));
logger.debug("========== bin : " + response.pay.card.bin);
logger.debug("========== last4 : " + response.pay.card.last4);
*/
		super.setRecord("trxId", response.pay.trxId);
		super.setRecord("trxType", response.pay.trxType);
		
		//super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		// 2022-05-16 - 전역변수 제거
		super.setRecord("mchtId", response.pay.mchtId);
		
		super.setRecord("tmnId", response.pay.tmnId);
		super.setRecord("trackId", response.pay.trackId);
		super.setRecord("payerName", response.pay.payerName);
		super.setRecord("payerEmail", response.pay.payerEmail);
		super.setRecord("payerTel", response.pay.payerTel);
		super.setRecord("amount", response.pay.amount);
		
		// super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
		// 2022-05-16 - 전역변수 제거
		super.setRecord("cardId", response.pay.card.cardId);
		if (response.pay.card != null) {
			super.setRecord("issuer", response.pay.card.issuer);
			super.setRecord("last4", response.pay.card.last4);
			super.setRecord("cardType", response.pay.card.cardType); 
			super.setRecord("bin", response.pay.card.bin);
			super.setRecord("installment", CommonUtil.zerofill(response.pay.card.installment,2));
			super.setRecord("acquirer", response.pay.card.acquirer);
		}
		//super.setRecord("prodId", sharedMap.getString(PAYUNIT.KEY_PROD));
		// 2022-05-16 - 전역변수 제거
		super.setRecord("prodId", response.pay.products.get(0).prodId);
		
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		
		if (response.pay.compNo != null) {
			super.setRecord("compNo", response.pay.compNo);
		} else {
			super.setRecord("compNo", "");
		}
		if (response.pay.compMember != null) {
			super.setRecord("compMember", response.pay.compMember);
		} else {
			super.setRecord("compMember", "");
		}
		
		// 2022-08-12 - 영업사원 아이디
		if (response.pay.salesId != null) {
			super.setRecord("salesId", response.pay.salesId);
		}
		
		logger.info("========== insertTrxREQ() - insert() : {}, {}", super.insert(),response.pay.trxId);
		super.initRecord();

	}

	public synchronized void insertTrxREQ(SharedMap<String, Object> sharedMap) {
		logger.info("========== insertTrxREQ()");
		super.setDebug(true);

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("payerName", sharedMap.getString("payerName"));
		super.setRecord("payerEmail", sharedMap.getString("payerEmail"));
		super.setRecord("payerTel", sharedMap.getString("payerTel"));
		super.setRecord("amount", sharedMap.getString("amount"));
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
		if (!CommonUtil.isNullOrSpace(sharedMap.getString(PAYUNIT.KEY_CARD))) {
			super.setRecord("issuer", sharedMap.getString("issuer"));
			super.setRecord("last4", sharedMap.getString("last4"));
			super.setRecord("cardType", sharedMap.getString("cardType"));
			super.setRecord("bin", sharedMap.getString("bin"));
			super.setRecord("installment", sharedMap.getString("installment"));
			super.setRecord("acquirer", sharedMap.getString("acquirer"));
		}
		super.setRecord("prodId", sharedMap.getString(PAYUNIT.KEY_PROD));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("insertTrxREQ() insert() : {}", super.insert());
		super.initRecord();

	}
	
	
	public synchronized void updateTrxREQACQ(String trxId, String acquirer) {
		logger.info("========== updateTrxREQACQ() - acquirer : " + acquirer);
		super.setDebug(true);
		super.setTable("PG_TRX_REQ");
		super.setRecord("acquirer", acquirer);
		super.addWhere("trxId", trxId);
		logger.info("updateTrxREQACQ() update() : {}", super.update());
		super.initRecord();
	}

	public synchronized void insertTrxRES(SharedMap<String, Object> sharedMap, Response response) {
		logger.info("========== insertTrxRES() ");
		super.setDebug(true);

		super.setTable("PG_TRX_RES");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		if (sharedMap.getString("vanDate").length() == 14){
			curDate = sharedMap.getString("vanDate");
		}

		super.setRecord("trxId", response.pay.trxId);
		super.setRecord("authCd", CommonUtil.nToB(response.pay.authCd));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("pairingVtid", sharedMap.getString("pairingVtid"));
		super.setRecord("pairingRouteVan", sharedMap.getString("pairingRouteVan"));
		super.setRecord("pairingCid", sharedMap.getString("pairingCid"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);

		if (response.pay.compNo != null) {
			super.setRecord("compNo", response.pay.compNo);
		} else {
			super.setRecord("compNo", "");
		}
		if (response.pay.compMember != null) {
			super.setRecord("compMember", response.pay.compMember);
		} else {
			super.setRecord("compMember", "");
		}
		// 2022-08-12 - 영업사원 아이디
		if (response.pay.salesId != null) {
			super.setRecord("salesId", response.pay.salesId);
		}
		logger.info("========== insertTrxRES() - van : " + sharedMap.getString("van"));
		logger.info("========== insertTrxRES() - vanId : " + sharedMap.getString("vanId"));
		logger.info("========== insertTrxRES() - salesId : " + response.pay.salesId);
		
		// 2022-08-12 - 영업사원 아이디
		if (response.pay.salesId != null) {
			super.setRecord("salesId", response.pay.salesId);
		}
		
		logger.info("========== insertTrxRES() - insert() : {}", super.insert());

		if (response.result.resultCd.equals("0000")) {
			insertTrxPAY(response.pay.trxId);
		} else {
			insertTrxERR(response.pay.trxId);
		}

		super.initRecord();

	}

	public synchronized void insertTrxRES(SharedMap<String, Object> sharedMap) {
		super.setDebug(true);

		super.setTable("PG_TRX_RES");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("authCd", sharedMap.getString("authCd"));
		super.setRecord("resultCd", sharedMap.getString("resultCd"));
		super.setRecord("resultMsg", sharedMap.getString("resultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("pairingVtid", sharedMap.getString("pairingVtid"));
		super.setRecord("pairingRouteVan", sharedMap.getString("pairingRouteVan"));
		super.setRecord("pairingCid", sharedMap.getString("pairingCid"));
		
		//super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		//super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		//super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		// 2021-11-29 서버시간 -> KSNET 시간으로 변경
		logger.info("========== vanDate regDay ::: " + sharedMap.getString("vanDate").substring(0, 8));
		logger.info("========== vanDate regTime ::: " + sharedMap.getString("vanDate").substring(8));
		logger.info("========== vanDate regDate ::: " + sharedMap.getString("vanDate"));
		super.setRecord("regDay", sharedMap.getString("vanDate").substring(0, 8));
		super.setRecord("regTime", sharedMap.getString("vanDate").substring(8));
		super.setRecord("regDate", sharedMap.getString("vanDate"));

		logger.info("insertTrxRES() - insert() : {}", super.insert());

		if (sharedMap.getString("resultCd").equals("0000")) {
			insertTrxPAY(sharedMap.getString("trxId"));
		} else {
			insertTrxERR(sharedMap.getString("trxId"));
		}

		super.initRecord();
	}

	public synchronized void insertTrxERR(String trxId) {
		logger.info("========== insertTrxERR() ");
		super.setDebug(true);

		String q = "INSERT INTO PG_TRX_ERR \n" + 
		"(trxId, trxType, mchtId, tmnId, trackId, \n" + 
		"payerName, payerEmail, payerTel, amount, installment, \n" + 
		"cardId, cardType, BIN, last4, issuer, \n" + 
		"acquirer, prodId, reqDay, reqTime, resultCd, \n" + 
		"resultMsg, van, vanId, vanTrxId, vanResultCd, \n" + 
		"vanResultMsg, cyrexVtid, cyrexRouteVan, cyrexCid, regDay, \n" + 
		"regTime, regDate, reqDate, compNo, compMember, \n" + 
		"semiAuth, salesId) \n" + 
		" SELECT A.trxId, trxType, mchtId, tmnId, trackId, \n" + 
		"payerName, payerEmail, payerTel, amount, installment, \n" + 
		"cardId, cardType, bin, last4, issuer, \n" + 
		"acquirer, prodId, " +
				//+ " A.regDay, A.regTime, resultCd, resultMsg, van, vanId, vanTrxId, vanResultCd, vanResultMsg, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, B.regDate " + " FROM PG_TRX_REQ A,  PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		// 2021-08-27 승인실패 페이지 - 결제시도일자 컬럼 추가
//				+ " A.regDay, A.regTime, resultCd, resultMsg, van, vanId, vanTrxId, vanResultCd, vanResultMsg, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, B.regDate, A.regDate,  A.compNo,  A.compMember,  A.semiAuth " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		// 2022-08-12 - 영업사원 아이디 추가
		"A.regDay, A.regTime, resultCd, \n" + 
		"resultMsg, van, vanId, vanTrxId, vanResultCd, \n" + 
		"vanResultMsg, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, \n" + 
		"B.regTime, B.regDate, A.regDate, A.compNo, A.compMember, \n" + 
		"A.semiAuth, A.salesId \n" + 
		"FROM PG_TRX_REQ A, PG_TRX_RES B \n" + 
		"WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		
		logger.info(q);
		logger.info("========== insertTrxERR() - update() : {}", super.update(q));
		super.initRecord();

	}

	public synchronized void insertTrxPAY(String trxId) {
		logger.info("========== insertTrxPAY() ");
		super.setDebug(true);

		String q = "INSERT INTO PG_TRX_PAY \n" + 
		"(trxId, mchtId, tmnId, trackId, payerName, \n" + 
		"payerEmail, payerTel, amount, installment, cardId, \n" + 
		"cardType, BIN, last4, status, prodId, \n" + 
		"issuer, acquirer, reqDay, reqTime, authCd, \n" + 
		"resultCd, resultMsg, van, vanId, vanTrxId, \n" + 
		"cyrexVtid, cyrexRouteVan, cyrexCid, regDay, regTime, \n" + 
		"regDate, reqDate, compNo, compMember, semiAuth, \n" + 
		"salesId, cancelType ) \n" +
		
		"SELECT A.trxId, mchtId, tmnId, trackId, payerName, \n" +
		"payerEmail, payerTel, amount, installment, cardId, \n" +
		"cardType, bin, last4, '승인', prodId, \n" +
		"issuer, acquirer, " + 
		//		+ " A.regDay, A.regTime, authCd, resultCd, resultMsg, van, vanId, vanTrxId, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		// 2021-08-27 승인실패 페이지 - 결제시도일자 컬럼 추가
		//+ " A.regDay, A.regTime, authCd, resultCd, resultMsg, van, vanId, vanTrxId, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, B.regDate, A.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		// 2022-03-02 업체번호,  업체회원 추가
		//+ " A.regDay, A.regTime, authCd, resultCd, resultMsg, van, vanId, vanTrxId, B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, B.regDate, A.regDate,  A.compNo,  A.compMember,  A.semiAuth " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		// 2022-08-12 - 영업사원 아이디 추가
		" A.regDay, A.regTime, authCd, \n" +
		"resultCd, resultMsg, van, vanId, vanTrxId, \n" +
		"B.pairingVtid, B.pairingRouteVan, B.pairingCid, B.regDay, B.regTime, \n" +
		"B.regDate, A.regDate,  A.compNo,  A.compMember,  A.semiAuth,  \n" +
		"A.salesId, '' \n" + 
		"FROM PG_TRX_REQ A, PG_TRX_RES B \n" + 
		"WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'; ";
		
		logger.info(q);
		logger.info("========== insertTrxPAY - update() : {}", super.update(q));
		super.initRecord();
	}


/*
	public void insertTrxCAP(Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		long vat = response.pay.amount - new Double(Math.floor((response.pay.amount / 1.1) + 0.001)).longValue();
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + response.settle.capId + "',trxId,mchtId,tmnId,trackId,'매입','','',amount," + vat + ",cardId,cardType,bin,issuer,last4,installment,authCd,regDay," + " '" + curDate.substring(0, 8) + "','"
				+ curDate.substring(8) + "','" + curDate + "' FROM PG_TRX_PAY " + " WHERE trxId = '" + response.pay.trxId + "'";
		logger.info("set TRX_CAP : {}", super.update(q));
		super.initRecord();
		if (response.settle.detail != null) {
			insertTrxCAPDetail(response.settle);
		}
	}

	public boolean insertTrxCAP(SharedMap<String, Object> sharedMap, Settle settle) {
		long vat = sharedMap.getLong("amount") - new Double(Math.floor((sharedMap.getLong("amount") / 1.1) + 0.001)).longValue();
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + settle.capId + "',trxId,mchtId,tmnId,trackId,'매입','','',amount," + vat + ",cardId,cardType,bin,issuer,last4,installment,authCd,regDay,'" + sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8)
				+ "','" + sharedMap.getString(PAYUNIT.REG_DATE).substring(8) + "','" + sharedMap.getString(PAYUNIT.REG_DATE) + "' FROM PG_TRX_PAY " + " WHERE trxId = '" + sharedMap.getString("trxId") + "'";
		//logger.debug("QUERY {}", q);
		boolean res = super.update(q);
		logger.info("set TRX_CAP : {}", res);
		super.initRecord();
		if (res && settle.detail != null) {
			res = insertTrxCAPDetail(settle);
		}
		return res;
	}

	public boolean insertTrxCAPDetail(Settle settle) {
		SharedMap<String, Object> details = settle.detail;

		super.setTable("PG_TRX_CAP_DTL");
		super.setRecord("capId", settle.capId);
		super.setRecord("stlAmount", settle.stlAmount);
		super.setRecord("stlRate", settle.rate);
		super.setRecord("stlFee", settle.fee);
		super.setRecord("stlFeeVat", settle.vat);
		super.setRecord("stlType", details.getString("stlType"));
		super.setRecord("stlDay", settle.settleDay);
		super.setRecord("stlId", details.getString("stlId"));
		super.setRecord("stlDistFee", details.getLong("stlDistFee"));
		super.setRecord("stlDistRate", details.getDouble("stlDistRate"));
		super.setRecord("stlDistDay", details.getString("stlDistDay"));
		super.setRecord("stlDistId", "");
		super.setRecord("stlAgencyFee", details.getLong("stlAgencyFee"));
		super.setRecord("stlAgencyRate", details.getDouble("stlAgencyRate"));
		super.setRecord("stlAgencyDay", details.getString("stlAgencyDay"));
		super.setRecord("stlAgencyId", "");
		super.setRecord("stlSalesFee", details.getLong("stlSalesFee"));
		super.setRecord("stlSalesRate", details.getDouble("stlSalesRate"));
		super.setRecord("stlSalesDay", details.getString("stlSalesDay"));
		super.setRecord("stlSalesId", details.getString(""));
		
		super.setRecord("stlLoanFee", details.getLong("stlLoanFee"));
		super.setRecord("stlLoanRate", details.getDouble("stlLoanRate"));
		super.setRecord("stlLoanDay", details.getString("stlLoanDay"));
		super.setRecord("stlLoanId", details.getString(""));
		
		super.setRecord("van", details.getString("van"));
		super.setRecord("stlVanFee", details.getLong("stlVanFee"));
		super.setRecord("stlVanRate", details.getDouble("stlVanRate"));
		super.setRecord("stlVanDay", details.getString("stlVanDay"));
		
		super.setRecord("benefit", details.getLong("benefit"));
		super.setRecord("taxId", details.getString("taxId"));
		
		boolean res = super.insert();
		logger.info("set TRX_CAP_DTL : {}", res);
		super.initRecord();
		
		return res;
	}
*/
	
	public synchronized SharedMap<String, Object> getTrxPayByTrackId(String tmnId, String trackId, String trxDay, long amount) {
		logger.info("========== getTrxPayByTrackId() - tmnId : " + tmnId + " / trackId " + trackId + " / trxDay : " + trxDay + " / amount : " + amount);
		super.setDebug(true);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("regDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	public synchronized SharedMap<String, Object> getTrxPayByTrxId(String tmnId, String trxId) {
		logger.info("========== getTrxPayByTrxId() - tmnId : " + tmnId + " / trxId " + trxId);
		super.setDebug(true);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	public synchronized SharedMap<String, Object> getTrxCapByTrackId(String tmnId, String trackId, String trxDay, long amount) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("capType", "매입", eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized SharedMap<String, Object> getTrxCapByTrxId(String tmnId, String trxId) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("capType", "매입", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized SharedMap<String,Object> getTrxRefundSumByTrxId(String trxId) {

		super.setTable("PG_TRX_CAP A LEFT JOIN PG_TRX_CAP B ON A.capId = B.rootTrxId");
		super.setColumns(" SUM(B.amount) as AMT,count(1) as CNT ");
		super.addWhere("A.trxId", trxId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.setColumns("*");
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	

	public synchronized SharedMap<String, Object> getTrxRfdByTrxId(String trxId) {

		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("rootTrxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getTrxCapDtlByCapId(String capId) {

		super.setTable("PG_TRX_CAP_DTL");
		super.setColumns("*");
		super.addWhere("capId", capId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized SharedMap<String, Object> getTrxPayByTrxId(String trxId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized SharedMap<String, Object> getTrxByVanTrxId(String van,String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van", van, eq);
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized SharedMap<String, Object> getCapByTrxId(String trxId) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("capType", "매입", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public synchronized void insertTrxRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		logger.info("========== response.refund.rootTrxId : " + response.refund.rootTrxId + " ========== exec() - insertTrxRFD()");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		super.setDebug(true);
		
		super.setTable("PG_TRX_RFD");
		long vat = 0;
		if(sharedMap.isEquals("taxType", "") || sharedMap.isEquals("taxType", "과세")) {
			vat = new Double(response.refund.amount *10 /110).longValue();
		}
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("mchtId", payMap.getString("mchtId"));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("status", "접수");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", -response.refund.amount);
		super.setRecord("rfdVat", -vat);
		super.setRecord("cardId", payMap.getString("cardId"));
		super.setRecord("bin", payMap.getString("bin"));
		super.setRecord("issuer", payMap.getString("issuer"));
		super.setRecord("acquirer", payMap.getString("acquirer"));
		super.setRecord("last4", payMap.getString("last4"));
		super.setRecord("rootTrnDay", payMap.getString("regDay"));
		super.setRecord("rootTrxId", payMap.getString("trxId"));
		super.setRecord("rootTrackId", payMap.getString("trackId"));
		super.setRecord("rootAmount", payMap.getLong("amount"));
		super.setRecord("rootVat", payMap.getLong("vat"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("authCd", payMap.getString("authCd"));

		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		
		// 2021-08-27 승인실패 페이지 - 결제시도일자 컬럼 추가
		//System.out.println("PAYUNIT.REG_DATE ::: " + PAYUNIT.REG_DATE);
		super.setRecord("reqDate", sharedMap.getString(PAYUNIT.REG_DATE));
		
		// 2022-08-12 - 영업사원 아이디 추가
		super.setRecord("salesId", payMap.getString("salesId"));
		
		logger.info("========== response.refund.rootTrxId : " + response.refund.rootTrxId + " ========== exec() - insertTrxRFD() - insert() : {}", super.insert());
		super.initRecord();
	}

	//WH
	public synchronized void insertTrxRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> trxMap) {
		super.setDebug(true);
		super.setTable("PG_TRX_RFD");
		long vat = 0;
		if(sharedMap.isEquals("taxType", "") || sharedMap.isEquals("taxType", "과세")) {
			vat = new Double(sharedMap.getLong("amount") *10 /110).longValue();
		}
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		if(sharedMap.isNullOrSpace("trackId")){
			super.setRecord("trackId", trxMap.getString("trackId"));
		}else{
			super.setRecord("trackId", sharedMap.getString("trackId"));
		}
		super.setRecord("status", "접수");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", -sharedMap.getLong("amount"));
		super.setRecord("rfdVat", -vat);
		super.setRecord("cardId", trxMap.getString("cardId"));
		super.setRecord("bin", trxMap.getString("bin"));
		super.setRecord("issuer", trxMap.getString("issuer"));
		super.setRecord("acquirer", trxMap.getString("acquirer"));
		super.setRecord("last4", trxMap.getString("last4"));
		super.setRecord("rootTrnDay", trxMap.getString("regDay"));
		super.setRecord("rootTrxId", trxMap.getString("trxId"));
		super.setRecord("rootTrackId", trxMap.getString("trackId"));
		super.setRecord("rootAmount", trxMap.getLong("amount"));
		super.setRecord("rootVat", trxMap.getLong("vat"));
		super.setRecord("authCd", trxMap.getString("authCd"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
 
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		
		// 2021-08-27 승인실패 페이지 - 결제시도일자 컬럼 추가
		System.out.println("PAYUNIT.REG_DATE ::: " + PAYUNIT.REG_DATE);
		super.setRecord("reqDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("insertTrxRFDinsertTrxRFD() - insert() : {}", super.insert());
		super.initRecord();
	}

	public synchronized void updateTrxRFD(SharedMap<String, Object> sharedMap, Response response) {
		super.setDebug(true);
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		if (!sharedMap.isNullOrSpace("vanRegDate")) {
			curDate = sharedMap.getString("vanRegDate");
		}
		if (!sharedMap.isNullOrSpace("vanDate")) {
			curDate = sharedMap.getString("vanDate");
		}
		super.setTable("PG_TRX_RFD");
		if (response.result.resultCd.equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		logger.info("updateTrxRFD van : " + sharedMap.getString("van"));
		logger.info("updateTrxRFD vanId : " + sharedMap.getString("vanId"));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		
		logger.info("updateTrxRFD() - update() : {}", super.update());
		super.initRecord();
	}
	public synchronized void updateTrxRFDnotify(SharedMap<String, Object> sharedMap, Response response) {
		logger.info("========== updateTrxRFDnotify() 노티취소건 날짜,시간 업데이트");
		super.setDebug(true);
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		if (!sharedMap.isNullOrSpace("vanRegDate")) {
			curDate = sharedMap.getString("vanRegDate");
		}
		if (!sharedMap.isNullOrSpace("vanDate")) {
			curDate = sharedMap.getString("vanDate");
		}
		super.setTable("PG_TRX_RFD");
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		
		logger.info("updateTrxRFDnotify() - update() : {}", super.update());
		super.initRecord();
	}

	//WH
	public synchronized void updateTrxRFD(SharedMap<String, Object> sharedMap) {
		super.setDebug(true);
		super.setTable("PG_TRX_RFD");
		if (sharedMap.getString("vanResultCd").equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("updateTrxRFD() - update() : {}", super.update());
		super.initRecord();
	}
	/**
	public boolean insertTrxRefundCAP(String trxId, Settle settle) {
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + settle.capId + "',A.trxId,A.mchtId,A.tmnId,A.trackId,A.rfdType,A.rfdAll,A.rootTrxId,A.rfdAmount,A.rfdVat,A.cardId,B.cardType,A.bin,A.issuer,A.last4,B.installment,A.authCd,A.reqDay,A.regDay,A.regTime,A.regDate FROM PG_TRX_RFD A, PG_TRX_CAP B " 
				+ " WHERE A.rootTrxId = B.trxId AND A.trxId = '" + trxId + "'";
		
		boolean res = super.update(q);
		logger.info("set TRX_CAP : {}", res);
		super.initRecord();
		if (res && settle.detail != null) {
			res = insertTrxCAPDetail(settle);
		}
		return res;
	}**/

	public synchronized void updateTrxPay(String trxId) {
		super.setDebug(true);

		super.setTable("PG_TRX_PAY");

		super.setRecord("status", "승인취소");
		super.addWhere("trxId", trxId);
		logger.info("updateTrxPay() - update() : {}", super.update());
		super.initRecord();
	}

	public synchronized void insertTrxWH(SharedMap<String, Object> sharedMap) {
		super.setDebug(true);

		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("reqData", sharedMap.getString("reqData"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("orgData", sharedMap.getString("orgData"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));

		logger.info("insertTrxWH() - insert() : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized void updateTrxWH(SharedMap<String, Object> sharedMap) {
		super.setDebug(true);
		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("retry", "Y");
		super.addWhere("vanTrxId", sharedMap.getString("vanTrxId"), eq);
		super.addWhere("trxType", sharedMap.getString("trxType"), eq);
		logger.info("updateTrxWH() - update() : {}", super.update());
		super.initRecord();
	}

	public synchronized SharedMap<String, Object> getMchtTaxByTaxId(String taxId) {
		String key = "PG_MCHT_TAX_" + taxId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TAX");
			super.setColumns("*,FN_AES_DEC(identity) iden");
			super.addWhere("taxId", taxId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized SharedMap<String, Object> getMchtReadyTaxByMchtId(String mchtId) {
		super.setTable("PG_MCHT_TAX");
		super.setColumns("*,FN_AES_DEC(identity) iden");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("taxStatus", "'예정','사용'", in);
		super.setOrderBy("regDay desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized long getTaxUsedLimit(String taxId) {
		super.setTable("PG_TRX_CAP_DTL A, PG_TRX_CAP B");
		super.setColumns("IFNULL(SUM(A.stlAmount), 0) as amt");
		super.setWhere("A.capId = B.capId");
		super.addWhere(" SUBSTR(A.stlDay, 1, 4) = SUBSTR(CURDATE(), 1, 4) ");
		super.addWhere("taxId", taxId, eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		super.setColumns("*");
		SharedMap<String, Object> result = rset.getRow(0);
		if(result != null && !result.isEmpty() && result.getLong("amt") > 0) {
			return result.getLong("amt");
		} else {
			return 0;
		}
		
	}

	public synchronized void updateTaxStatus(String taxId, String taxStatus) {
		super.setDebug(true);
		super.setTable("PG_MCHT_TAX");
		super.setRecord("taxStatus", taxStatus);
		super.addWhere("taxId", taxId, eq);
		logger.info("updateTaxStatus() - update() : {}", super.update());
		super.initRecord();
	}
	
	public synchronized void updateMchtTmnTaxId(String tmnId, String taxId) {
		super.setDebug(true);
		super.setTable("PG_MCHT_TMN");
		super.setRecord("taxId", taxId);
		super.addWhere("tmnId", tmnId, eq);
		logger.info("updateMchtTmnTaxId() - update() : {}", super.update());
		super.initRecord();
	}
	
	public synchronized void deleteMchtTmnByTmnId(String tmnId) {
		super.setDebug(true);
		String key = "PG_MCHT_TMN_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			PAYUNIT.cacheMap.delete(key);
		}
	}
	
	public synchronized void deleteMchtTmnByPayKey(String payKey) {
		super.setDebug(true);
		String key = "PG_MCHT_TMN_" + payKey;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			PAYUNIT.cacheMap.delete(key);
		}
	}
	
	public synchronized SharedMap<String, Object> getAdminRfdByVanTrxId(String vanTrxId) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setColumns("idx");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void updateAdminRfd(String idx, String trxId, String resultCd) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setRecord("trxId", trxId);
		super.setRecord("resultCd", resultCd);
		super.addWhere("idx", idx, eq);
		logger.info("updateAdminRfd() - update() : {}", super.update());
		super.initRecord();
	}
	
	//NICE
	public synchronized SharedMap<String, Object> getMchtTmnByVanId(String vanId) {
		String key = "VW_MCHT_TMN_" + vanId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("VW_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("vanId", vanId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized SharedMap<String, Object> getMchtTmnByVanIdx(long idx, String mchtId) {
		logger.info("========== getMchtTmnByVanIdx() - vanIdx : " + idx + " mchtId : " + mchtId);
		super.initRecord();
		String key = "VW_MCHT_TMN_IDX_"+mchtId+"_" + idx;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("VW_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("vanIdx", idx, eq);
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized SharedMap<String, Object> getMchtTmnByVanIdx2(long idx, String mchtId) {
		logger.info("========== getMchtTmnByVanIdx2() - vanIdx : " + idx + " mchtId : " + mchtId);
		super.initRecord();
		super.setDebug(true);
		super.setTable("VW_MCHT_TMN");
		super.setColumns("*");
		super.addWhere("vanIdx", idx, eq);
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	public synchronized SharedMap<String, Object> getMchtTmnDtlByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_DTL_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN_DTL");
			super.setColumns("*");
			super.addWhere("tmnId", tmnId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized String getSettleDay(String today,int term) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days > '"+today+"' AND status ='no' limit "+(term-1)+",1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
	
	public synchronized String getSettleDay(String today) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days >= '"+today+"' AND status ='no' limit 1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
	
	
	
	public synchronized void insertTrxCAPSub(String capId,SharedMap<String,Object> map) {
		
		String q = "INSERT INTO PG_TRX_CAP_SUB  " + " SELECT capId,trxId,mchtId,tmnId,capType,rfdType,rootTrxId,amount,"
				+   map.getLong("stlAmount")+","+map.getDouble("stlRate")+","+map.getLong("stlFee")+","+map.getLong("stlFeeVat")+",'D+1','"+map.getString("stlDay")+"','',"+map.getLong("benefit")+",trxDay,regDay,regTime,regDate  "
				+ " FROM PG_TRX_CAP " + " WHERE capId = '" + capId + "'";
		logger.info("insertTrxCAPSub() - insert() : {}", super.update(q));
		super.initRecord();
		
	}
	
	
	public synchronized void insertTrxRefundSub(String capId,SharedMap<String,Object> map) {
		
		String q = "INSERT INTO PG_TRX_CAP_SUB  " + " SELECT '"+capId+"',A.trxId,A.mchtId,A.tmnId,B.capType,A.rfdType,A.rootTrxId,A.rfdAmount,"
				+   map.getLong("stlAmount")+","+map.getDouble("stlRate")+","+map.getLong("stlFee")+","+map.getLong("stlFeeVat")+",'D+1','"+map.getString("stlDay")+"','',"+map.getLong("benefit")+",A.reqDay,A.regDay,A.regTime,A.regDate  "
				+ " FROM PG_TRX_RFD A, PG_TRX_CAP B " + " WHERE A.rootTrxId = B.trxId AND A.capId = '" + capId + "'";
		logger.info("insertTrxRefundSub() - insert() : {}", super.update(q));
		super.initRecord();
		
	}
	
	
	public synchronized void insertTrxIO(SharedMap<String, Object> sharedMap, Pay pay) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", pay.trxType);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", pay.trackId);
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", sharedMap.getString(PAYUNIT.PAYLOAD));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}

	public synchronized boolean insertTrxIOchk(SharedMap<String, Object> sharedMap, Pay pay) {
		logger.info("========== insertTrxIOchk() - 중복거래번호 확인 - mchtId : " + pay.mchtId);
		super.setTable("PG_TRX_IO");
		
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", pay.trxType);
		//super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		// 2022-05-16 : 전역변수 제거
		super.setRecord("mchtId", pay.mchtId);
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", pay.trackId);
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", sharedMap.getString(PAYUNIT.PAYLOAD));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		//logger.info("set TRX_IO : {}", super.insert());
		boolean chk = super.insert();
		super.initRecord();
		logger.info("========== insertTrxIOchk() - set PG_TRX_IO : {}", chk);
		return chk;
	}
	
	public synchronized void insertTrxIO(SharedMap<String, Object> sharedMap,Auth auth) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", auth.trxType);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", auth.trackId);
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", Base64.encodeToString(SeedKisa.encrypt(sharedMap.getString(PAYUNIT.PAYLOAD), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}
	
	
	public synchronized void insertTrxIO(SharedMap<String, Object> sharedMap,Vact vact) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", "VACT");
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", CommonUtil.nToB(vact.trackId));
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", sharedMap.getString(PAYUNIT.PAYLOAD));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}
	
	public synchronized void updateTrxIO(SharedMap<String, Object> sharedMap, String payLoad) {
		logger.info("========== updateTrxIO()");
		super.setDebug(true);
		
		super.setTable("PG_TRX_IO");
		super.setRecord("status", "응답");
		super.setRecord("resDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		super.setRecord("resTime",CommonUtil.getCurrentDate("HHmmss"));
		super.setRecord("resData", payLoad);
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("========== set TRX_IO : {}", super.update());
		super.initRecord();

	}
	
	public synchronized String getTrxIO(String tmnId, String search) {
		logger.info("========== getTrxIO()");
		super.setDebug(true);
		super.setTable("PG_TRX_IO");
		super.setColumns("resData");
		super.addWhere("tmnId", tmnId, eq);
		if(search.startsWith("T") && search.length() == 13){
			super.addWhere("trxId",search);
		}else{
			super.addWhere("trackId",search);
		}
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0){
			return "";
		}else{
			return rset.getRow(0).getString("resData");
		}
		
	
	}
	
	
	public synchronized SharedMap<String, Object> getVanByVanId(String van, String vanId) {
		logger.info("========== getVanByVanId() - van : " + van + " / vanId : " + vanId);
		String key = "PG_VAN_" + van + vanId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.initRecord();
			super.setTable("PG_VAN");
			super.setColumns("*");
			if (!CommonUtil.isNullOrSpace(van)) {
				super.addWhere("van", van, eq);
			}
			super.addWhere("vanId", vanId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized SharedMap<String, Object> getVanByVanId2(String van, String vanId) {
		logger.info("========== getVanByVanId2() - van : " + van + " / vanId : " + vanId);
		super.setDebug(true);
		super.initRecord();
		super.setTable("PG_VAN");
		super.setColumns("*");
		if(!CommonUtil.isNullOrSpace(van)) {
			super.addWhere("van", van, eq);
		}
		super.addWhere("vanId", vanId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void insertTrxNTS(SharedMap<String, Object> sharedMap) {
		super.setTable("PG_TRX_NTS");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("webHookUrl", sharedMap.getString("webHookUrl"));
		super.setRecord("retry", sharedMap.getInt("retry"));
		super.setRecord("status", sharedMap.getString("status"));
		super.setRecord("code", sharedMap.getInt("code"));
		super.setRecord("payLoad", sharedMap.getString("payLoad"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("sentDate", sharedMap.getTimestamp("sentDate"));
		super.setRecord("regDay", sharedMap.getString("regDay"));
		super.setRecord("regTime", sharedMap.getString("regTime"));
		logger.info("set TRX_NTS : {}", super.insert());
		super.initRecord();
	}
	
	
	
	public synchronized void insertTrxCash(SharedMap<String, Object> sharedMap,Cash cash,String issuer) {
		super.setTable("PG_TRX_CASH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", cash.trackId);
		super.setRecord("status", "승인");
		super.setRecord("`usage`", cash.usage);
		super.setRecord("taxType", cash.taxType);
		super.setRecord("supplyAmt", cash.supplyAmt);
		super.setRecord("vatAmt", cash.vatAmt);
		super.setRecord("serviceAmt", cash.serviceAmt);
		super.setRecord("amount", cash.amount);
		if (cash.identity.length() == 13) {
			super.setRecord("identity", cash.identity.substring(0, 6) + "*******");
		} else if (cash.identity.length() > 15) {
			super.setRecord("identity", cash.identity.substring(0, 6) + "******" + cash.identity.substring(12));
		} else {
			super.setRecord("identity", cash.identity);
		}
		super.setRecord("`issuer`", issuer);
		super.setRecord("custName", cash.custName);
		super.setRecord("custTel", cash.custTel);
		super.setRecord("custEmail", cash.custEmail);
		super.setRecord("pdtName", cash.pdtName);
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_CASH : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized void updateTrxCash(String trxId,String vanTrxId,String authCd,String issueDay,String status) {
		super.setTable("PG_TRX_CASH");
		if (!CommonUtil.isNullOrSpace(vanTrxId)) {
			super.setRecord("vanTrxId", vanTrxId);
		}
		if (!CommonUtil.isNullOrSpace(authCd)) {
			super.setRecord("authCd", authCd);
		}
		if (!CommonUtil.isNullOrSpace(status)) {
			super.setRecord("status", status);
		}
		if (!CommonUtil.isNullOrSpace(issueDay)) {
			super.setRecord("issueDay", issueDay);
		}
		super.addWhere("trxId", trxId);
		logger.info("set TRX_CASH : {}", super.update());
		super.initRecord();
	}
	
	public synchronized void updateTrxCashRevoke(String trxId,String status,String canTrxId) {
		super.setTable("PG_TRX_CASH");
		super.setRecord("canTrxId", canTrxId);
		super.setRecord("status", status);
		super.addWhere("trxId", trxId);
		logger.info("set TRX_CASH : {}", super.update());
		super.initRecord();
	}
	
	
	public synchronized void insertTrxCashDtl(String trxId,String progress,String resultCd,String resultMsg,String ntsCd,String ntsMsg,String upDate) {
		super.setTable("PG_TRX_CASH_DTL");
		super.setRecord("trxId", trxId);
		super.setRecord("progress", progress);
		super.setRecord("isNew", "N");
		super.setRecord("resultCd", CommonUtil.toString(resultCd));
		super.setRecord("resultMsg", resultMsg);
		if (ntsCd != null) {
			super.setRecord("ntsCd", ntsCd);
		}
		if (ntsMsg != null) {
			super.setRecord("ntsMsg", ntsMsg);
		}
		super.setRecord("`modDate`", upDate);
		logger.info("set TRX_CASH : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized void updateTrxCashDtl(String trxId) {
		super.setTable("PG_TRX_CASH_DTL");
		super.setRecord("isNew", "");
		super.addWhere("trxId", trxId);
		super.addWhere("isNew", "N");
		logger.info("set TRX_CASH : {}", super.update());
		super.initRecord();
	}
	
	public synchronized SharedMap<String, Object> getTrxCash(String trxId) {
		super.setTable("PG_TRX_CASH");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String, Object> getTrxCashDtl(String trxId) {
		super.setTable("PG_TRX_CASH_DTL");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.setOrderBy("idx desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String, Object> getTrxCash2(String trxId) {
		super.setTable("PG_TRX_CASH2");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("trxType", "발급", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String, Object> getTrxCash2(String tmnId,String trackId,String trxDay) {
		super.setTable("PG_TRX_CASH2");
		super.setColumns("*");
		super.addWhere("regDay", trxDay, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trxType", "발급", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized long sumByCard(String mchtId,String bin,String last4){
		super.setTable("PG_TRX_CAP");
		super.setColumns("IFNULL(sum(amount),0) as amount");
		super.addWhere("trxDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		super.addWhere("tmnId", mchtId);
		super.addWhere("bin",bin);
		super.addWhere("last4",last4);
		super.setOrderBy("capId desc");
		RecordSet rset = super.search();
		super.initRecord();
		SharedMap<String, Object> map = rset.getRow(0);
		if (map == null) {
			return 0;
		} else {
			return map.getLong("amount");
		}
	}

	public synchronized SharedMap<String, Object> getVanByVanIdx(String vanIdx) {
		String key = "PG_VAN_" + vanIdx;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VAN");
			super.addWhere("idx", vanIdx, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	public synchronized SharedMap<String, Object> getVanByVanIdx2(String vanIdx) {
		logger.info("========== getVanByVanIdx2() - vanIdx : " + vanIdx);
		super.setDebug(true);
		super.setTable("PG_VAN");
		super.addWhere("idx", vanIdx, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getTrxPayByTrackId(String trackId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("trackId", trackId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void insertTrxNTSPG(SharedMap<String, Object> ntsMap) {
		super.setTable("PG_TRX_NTS_PG");
		super.setRecord("trxId", ntsMap.getString("trxId"));
		super.setRecord("trxType", ntsMap.getString("trxType"));
		super.setRecord("trackId", ntsMap.getString("trackId"));
		super.setRecord("vanId", ntsMap.getString("vanId"));
		super.setRecord("vanTrxId", ntsMap.getString("vanTrxId"));
		super.setRecord("amount", ntsMap.getLong("amount"));
		super.setRecord("authCd", ntsMap.getString("authCd"));
		super.setRecord("trxDay", ntsMap.getString("trxDay"));
		super.setRecord("webHookUrl", ntsMap.getString("webHookUrl"));
		super.setRecord("retry", ntsMap.getInt("retry"));
		super.setRecord("status", ntsMap.getString("status"));
		super.setRecord("code", ntsMap.getInt("code"));
		super.setRecord("payLoad", ntsMap.getString("payLoad"));
		super.setRecord("resData", ntsMap.getString("resData"));
		super.setRecord("sentDate", ntsMap.getTimestamp("sentDate"));
		super.setRecord("regDay", ntsMap.getString("regDay"));
		super.setRecord("regTime", ntsMap.getString("regTime"));
		logger.info("set TRX_NTS_PG : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized List<String> getBanks(){
		String key = "PG_VACT_BANK";
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.vactCacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VACT");
			super.setColumns("distinct(bankCd) bankCd");
			RecordSet rset = super.search();
			super.initRecord();
			List<String> list = new ArrayList<String>();
			for(SharedMap<String,Object> map : rset.getRows()){
				list.add(map.getString("bankCd"));
			}
			return PAYUNIT.vactCacheMap.put(key, list);
		}
	}
	
	public synchronized SharedMap<String,Object> getNotIssueAccount(String bankCd,List<String> accounts){
		String account = "";
		if (account != null && accounts.size() > 0) {
			for (String s : accounts) {
				account += "'" + s + "',";
			}
			account = account.substring(0, account.length() - 1);
		}
		super.setTable("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account ");
		super.setColumns(" A.bankCd,MAX(A.account) account,A.issuerBank");
		super.setWhere("B.account is null");
		super.addWhere("A.bankCd", bankCd, eq);
		if (!account.equals("")) {
			super.addWhere("A.account", account, ni);
		}
		super.setOrderBy("A.regDate DESC");

		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			rset = getUsingAccount(bankCd, account);
		}
		return rset.getRow(0);
	}
	
	private RecordSet getUsingAccount(String bankCd,String account){
		super.setTable("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account ");
		super.setColumns(" A.bankCd,MAX(A.account) account ,A.issuerBank");
		super.setWhere("B.account is null");
		super.addWhere("A.bankCd", bankCd, eq);
		if (!account.equals("")) {
			super.addWhere("A.account", account, ni);
		}
		super.setOrderBy("A.regDate DESC");

		RecordSet rset = super.search();
		super.initRecord();
		return rset;
	}
	
	public synchronized SharedMap<String,Object> getMchtMngVact(String mchtId){
		String key = "PG_MCHT_MNG_VACT_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG_VACT");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized String isDuplicatedVactTrackId(String mchtId, String trackId) {
		String issueId = "";
		super.setTable("PG_VACT_DTL");
		super.setColumns("issueId");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() != 0) {
			return rset.getRow(0).getString("issueId");
		} else {
			return "";
		}
	}
	
	public synchronized SharedMap<String,Object> getReadyVactDtl(String account,String mchtId) {
		super.setTable("PG_VACT A LEFT OUTER JOIN PG_VACT_DTL B ON A.account = B.account ");
		super.setColumns("A.issuerBank,A.bankCd,B.*");
		super.addWhere("B.account", account);
		super.addWhere("B.mchtId", mchtId);
		super.addWhere("B.vactType", "영구");
		super.addWhere("B.status", "대기");
		super.setOrderBy("B.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String,Object> getNotIssueVactDtl(String account,String mchtId) {
		super.setTable("PG_VACT A LEFT OUTER JOIN PG_VACT_DTL B ON A.account = B.account ");
		super.setColumns("A.issuerBank,A.bankCd,B.*");
		super.addWhere("B.account", account);
		super.addWhere("B.mchtId", mchtId);
		super.addWhere("B.vactType", "영구");
		super.addWhere("B.status", "발행");
		super.setOrderBy("B.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized boolean insertVactDtl(SharedMap<String,Object> vact){
		super.setTable("PG_VACT_DTL");
		super.setRecord("issueId", 	vact.getString("issueId"));
		super.setRecord("account", 	vact.getString("account"));
		super.setRecord("vactType", vact.getString("vactType"));
		super.setRecord("`status`", vact.getString("status"));
		super.setRecord("tmnId", 	vact.getString("tmnId")); //20190920 추가
		super.setRecord("mchtId", 	vact.getString("mchtId"));
		super.setRecord("holderName", vact.getString("holderName"));
		super.setRecord("amount", 	CommonUtil.parseLong(vact.getString("amount")));
		super.setRecord("oper", 	vact.getString("oper"));
		super.setRecord("trackId", 	vact.getString("trackId"));
		super.setRecord("expireAt", vact.getString("expireAt"));
		super.setRecord("payerEmail", vact.getString("payerEmail"));
		super.setRecord("payerTel", vact.getString("payerTel"));
		super.setRecord("payerName", vact.getString("payerName"));
		super.setRecord("cash", vact.getString("cash"));
		super.setRecord("prodId", CommonUtil.nToB(vact.getString("prodId")));
		super.setRecord("udf1",		vact.getString("udf1"));
		super.setRecord("udf2", 	vact.getString("udf2"));
		super.setRecord("regId", 	"SYSTEM");
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		boolean insert = super.insert();
		logger.info("set PG_VACT_DTL insert : {}",insert );
		super.initRecord();
		return insert;
	}
	
	public synchronized boolean updateVactDtl(SharedMap<String,Object> vact){
		super.setTable("PG_VACT_DTL");
		//issueId,account,vactType,status,mchtId,holderName,amount,oper,trackId,expireAt,expireDate,udf1,udf2,reason,regId,regDay
		super.setRecord("`status`", vact.getString("status"));
		super.setRecord("holderName", vact.getString("holderName"));
		super.setRecord("amount", 	CommonUtil.parseLong(vact.getString("amount")));
		super.setRecord("oper", 	vact.getString("oper"));
		super.setRecord("trackId", 	vact.getString("trackId"));
		super.setRecord("expireAt", vact.getString("expireAt"));
		super.setRecord("udf1",		vact.getString("udf1"));
		super.setRecord("udf2", 	vact.getString("udf2"));
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		super.addWhere("issueId", 	vact.getString("issueId"));
		boolean update = super.update();
		logger.info("set PG_VACT_DTL update : {}",update );
		super.initRecord();
		return update;
	}
	
	public synchronized SharedMap<String,Object> getVactDtl(String mchtId, String issueId,String trackId) {
		super.setTable("PG_VACT_DTL A, PG_VACT B");
		super.setColumns("A.issueId,A.account,B.bankCd,A.oper,A.amount,A.holderName,A.trackId,A.udf1,A.udf2,A.expireAt,A.`status`");
		super.setWhere("A.account = B.account ");
		if (!CommonUtil.isNullOrSpace(issueId)) {
			super.addWhere("issueId", issueId);
		}
		if (!CommonUtil.isNullOrSpace(trackId)) {
			super.addWhere("trackId", trackId);
		}
		super.addWhere("mchtId", mchtId);
		super.setOrderBy("A.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized boolean updateVactDtlClose(SharedMap<String, Object> vact) {
		super.setTable("PG_VACT_DTL");
		super.setRecord("`status`", "사용자만료");
		super.setRecord("expireAt", CommonUtil.getCurrentDate("yyyyMMddHH"));
		super.setRecord("expireDate", CommonUtil.getCurrentTimestamp());
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		super.addWhere("issueId", 	vact.getString("issueId"));
		boolean update = super.update();
		logger.info("set PG_VACT_DTL close : {}",update );
		super.initRecord();
		return update;
	}
	
	public synchronized List<VactHookBean> getVactHistory(String issueId){
		List<VactHookBean> result 	= null;
		String query = " SELECT vactId,hookRetry,tmnId,mchtId,issueId,bankCd,account,sender,amount,trxType,rootVactId,trxDay,trxTime,trackId,udf1,udf2,stlDay,stlAmount,stlFee,stlFeeVat FROM PG_VACT_TRX WHERE issueId = ? ORDER BY vactId asc";
		
		DBManager db 					= null;
		PreparedStatement pstmt	= null;
		Connection conn				= null;
		ResultSet rset					= null;
		
		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, issueId);

			rset 	= pstmt.executeQuery();

			while (rset.next()) {
				if (result == null) {
					result = new ArrayList<VactHookBean>();
				}
				VactHookBean bean = new VactHookBean();
				bean.vactId 	= CommonUtil.nToB(rset.getString("vactId"));
				bean.retry 		= rset.getInt("hookRetry");
				bean.tmnId 		= CommonUtil.nToB(rset.getString("tmnId"));
				bean.mchtId 	= CommonUtil.nToB(rset.getString("mchtId"));
				bean.issueId 	= CommonUtil.nToB(rset.getString("issueId"));
				bean.bankCd 	= CommonUtil.nToB(rset.getString("bankCd"));
				bean.account 	= CommonUtil.nToB(rset.getString("account"));
				bean.sender 	= CommonUtil.nToB(rset.getString("sender"));
				bean.amount 	= rset.getLong("amount");
				bean.trxType 	= CommonUtil.nToB(rset.getString("trxType"));
				if (bean.trxType.equals("입금")) {
					bean.trxType = "deposit";
				} else {
					bean.trxType = "depositback";
				}
				bean.rootVactId = CommonUtil.nToB(rset.getString("rootVactId"));
				bean.trxDay 	= CommonUtil.nToB(rset.getString("trxDay"));
				bean.trxTime 	= CommonUtil.nToB(rset.getString("trxTime"));
				bean.trackId 	= CommonUtil.nToB(rset.getString("trackId"));
				bean.udf1 		= CommonUtil.nToB(rset.getString("udf1"));
				bean.udf2 		= CommonUtil.nToB(rset.getString("udf2"));
				bean.stlDay 	= CommonUtil.nToB(rset.getString("stlDay"));
				bean.stlAmount 	= rset.getLong("stlAmount");
				bean.stlFee 	= rset.getLong("stlFee");
				bean.stlFeeVat 	= rset.getLong("stlFeeVat");
				
				result.add(bean);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			db.close(conn, pstmt, rset);
		}
		return result;
	}
	
	public synchronized boolean patchVactDtl(SharedMap<String,Object> patchMap,String issueId){
		super.setTable("PG_VACT_DTL");
		for (String key : patchMap.keySet()) {
			if (key.equals("amount")) {
				super.setRecord("amount", patchMap.getLong("amount"));
			} else {
				super.setRecord(key, patchMap.getString(key));
			}
		}
		super.addWhere("issueId", issueId);
		boolean update = super.update();
		logger.info("set PG_VACT_DTL upadte : {}", update);
		super.initRecord();
		return update;
	}
	
	public synchronized SharedMap<String, Object> getMchtSvc(String mchtId) {
		logger.info("========== getMchtSvc() - mchtId : " + mchtId);
		String key = "PG_MCHT_SVC_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("PG_MCHT_SVC");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized void insertTrxIO3D(SharedMap<String, Object> ioMap) {
		super.setTable("PG_TRX_IO_3D");
		super.setRecord("trxId"		, ioMap.getString("trxId"));
		super.setRecord("widgetKey"	, ioMap.getString("widgetKey"));
		super.setRecord("mchtId"	, ioMap.getString("mchtId"));
		super.setRecord("tmnId"		, ioMap.getString("tmnId"));
		super.setRecord("trackId"	, ioMap.getString("trackId"));
		super.setRecord("device"	, ioMap.getString("device"));
		super.setRecord("van"		, ioMap.getString("van"));
		super.setRecord("vanId"		, ioMap.getString("vanId"));
		super.setRecord("reqJson"	, ioMap.getString("reqJson"));
		super.setRecord("resJson"	, ioMap.getString("resJson"));
		super.setRecord("resultCd"	, ioMap.getString("resultCd"));
		super.setRecord("resultMsg"	, ioMap.getString("resultMsg"));
		super.setRecord("regDay"	, ioMap.getString("regDay"));
		super.setRecord("regTime"	, ioMap.getString("regTime"));
		logger.info("set PG_TRX_IO_3D : {}", super.insert());
		super.initRecord();
	}
	
	public synchronized SharedMap<String,Object> getTrxIO3DByTrxId(String trxId){
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String,Object> getTrxIO3DByWidgetKey(String widgetKey){
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("*");
		super.addWhere("widgetKey", widgetKey, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void updateTrxIO3D(SharedMap<String,Object> ioMap){
		super.setTable("PG_TRX_IO_3D");
		super.setRecord("vanTrxId", 	ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", 	ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("vanResultDate", ioMap.getString("vanResultDate"));
		
		super.addWhere("trxId", 		ioMap.getString("trxId"));
		boolean update = super.update();
		logger.info("set PG_TRX_IO_3D update : {}",update );

		super.initRecord();
	}
	
	
	
	public synchronized void insertTrx3D(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", "3DTR");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", widgetMap.getString("payerName"));
		super.setRecord("payerEmail", widgetMap.getString("payerEmail"));
		super.setRecord("payerTel", widgetMap.getString("payerTel"));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("cardId", ioMap.getString("cardId"));	
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();
		
		
		super.setTable("PG_TRX_RES");
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		logger.info("set TRX_RES : {}", super.insert());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertTrxPAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}

		super.initRecord();
		

	}
	
	
	public synchronized long notSettleSum(String mchtId){
		
		RecordSet rset = super.query("SELECT IFNULL(sum(amount),0) as amount FROM PG_TRX_CAP A, PG_TRX_CAP_DTL B WHERE A.capId = B.capId AND A.mchtId = '" +mchtId+"' AND B.stlStatus ='정산대기' order by A.capId desc");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return 0;
		}else{
			return map.getLong("amount");
		}
	}
	
	
	public synchronized long notSettleSumTmnId(String tmnId){
		RecordSet rset = super.query("SELECT IFNULL(sum(amount),0) as amount FROM PG_TRX_CAP A, PG_TRX_CAP_DTL B WHERE A.capId = B.capId AND A.tmnId = '" +tmnId+"' AND B.stlStatus ='정산대기' order by A.capId desc");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return 0;
		}else{
			return map.getLong("amount");
		}
	}
	
	public synchronized String getForceRefund(String trxId){
		super.setTable("PG_TRX_RFD_FORCE");
		super.setColumns("forceType");
		super.addWhere("trxId", trxId);
		super.addWhere("`status`", "요청");
		RecordSet rset = super.search();
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return "";
		}else{
			return map.getString("forceType");
		}
	}
	
	
	public synchronized void updateForceRefund(String trxId){
		super.setDebug(true);
		
		super.setTable("PG_TRX_RFD_FORCE");
		super.setRecord("rfdDate", 	CommonUtil.getCurrentTimestamp());
		super.setRecord("`status`", "완료");
		super.addWhere("trxId", 	trxId);
		super.update();
		super.initRecord();
	}
	
	
	
	public synchronized void insertRefundIQR(String trxId,String summary) {
		logger.info("========== insertRefundIQR() - trxId : " + trxId + " | summary : " + summary);
		String capId = getCapId(trxId);
		super.setTable("PG_TRX_IQR");
		super.setRecord("capId", capId);
		super.setRecord("iqrType", "취소반려");
		
		super.setRecord("telNo", "");
		super.setRecord("summary", summary);
		super.setRecord("comRoute", "API취소");
		super.setRecord("content", "취소반려");
		super.setRecord("`type`", "취소");
		super.setRecord("result", "완료");
		super.setRecord("regId", "SYSTEM");
		super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		
		super.insert();
		super.initRecord();
	}
	
	
	
	public synchronized String getCapId(String trxId) {
		super.setTable("PG_TRX_CAP");
		super.setColumns("capId");
		super.addWhere("trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return "";
		}else{
			return map.getString("capId");
		}
	}

	
	
	public synchronized SharedMap<String, Object> getMchtMngBillByMchtId(String mchtId) {
		String key = "PG_MCHT_MNG_BILL_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG_BILL");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized SharedMap<String, Object> getBillUser(String billId) {
		super.setTable("PG_BILL_USER");
		super.setColumns("*");
		super.addWhere("billId", billId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String, Object> getBillBox(String billId) {
		
		super.setTable("PG_BILL_BOX");
		super.setColumns("*");
		super.addWhere("billId", billId, eq);
		super.addWhere("boxStatus", "사용", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
		
	}
	
	
	public synchronized boolean isDuplicatedBillTrackId(String mchtId, String trackId) {
		super.setTable("PG_BILL_USER");
		super.setColumns("*");
		super.addWhere("trackId", trackId, eq);
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("status", "사용", eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return true;
		}else {
			return false;
		}
	}
	
	public synchronized boolean isDuplicatedBillPayerId(String mchtId, String payerId) {
		super.setTable("PG_BILL_USER");
		super.setColumns("*");
		super.addWhere("trackId", payerId, eq);
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("status", "사용", eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return true;
		}else {
			return false;
		}
	}
	
	
	public synchronized void insertBillUser(SharedMap<String, Object> sharedMap,SharedMap<String, Object> widget) {

		super.setTable("PG_BILL_USER");
		super.setRecord("billId", widget.getString("billId"));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("trackId", widget.getString("trackId"));
		super.setRecord("status", widget.getString("status"));
		super.setRecord("payerName", widget.getString("payerName"));
		super.setRecord("payerEmail", widget.getString("payerEmail"));
		super.setRecord("payerId", widget.getString("payerId"));
		super.setRecord("expireAt", widget.getString("expireAt"));
		super.setRecord("udf1", widget.getString("udf1"));
		super.setRecord("udf2", widget.getString("udf2"));
		super.setRecord("ipAddr", sharedMap.getString(PAYUNIT.REMOTEIP));
		super.setRecord("summary", widget.getString("summary"));
		super.setRecord("authTrxId", widget.getString("authTrxId"));
		super.setRecord("resultMsg", widget.getString("resultMsg"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));

		logger.info("set INSERT BILL_USER : {}", super.insert());
		super.initRecord();
	}
	
	/*
	public String getTrxCapDtlByTrxId(String trxId) {
		
		RecordSet rset = super.query("SELECT stlDay FROM PG_TRX_CAP A , PG_TRX_CAP_DTL B WHERE A.capId = B.capId AND A.trxId ='"+trxId+"' LIMIT 1");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return "";
		}else{
			return map.getString("stlDay");
		}

	}*/
	
	
	public synchronized SharedMap<String,Object> getTrxCapDtlByTrxId(String trxId) {
		logger.info("========== getTrxCapDtlByTrxId() - trxId : " + trxId);
		super.setDebug(true);
		RecordSet rset = super.query(
				"SELECT stlDay, stlStatus, stlVanDay, trxDay FROM PG_TRX_CAP A , PG_TRX_CAP_DTL B WHERE A.capId = B.capId AND A.trxId ='" + trxId + "' LIMIT 1");
		super.initRecord();
		SharedMap<String, Object> map = rset.getRow(0);
		if (map == null) {
			return new SharedMap<String, Object>();
		} else {
			return map;
		}
	}
	
	public synchronized boolean insertTrxNtsEmail(SharedMap<String, Object> email) {
		logger.info("========== insertTrxNtsEmail() ");
		super.setDebug(true);
		super.setTable("PG_TRX_NTS_EMAIL");
		super.setRecord("trxId", 	email.getString("trxId"));
		super.setRecord("tmnId", 	email.getString("tmnId"));
		super.setRecord("mchtId", email.getString("mchtId"));
		super.setRecord("trackId", email.getString("trackId"));
		super.setRecord("trxType", email.getString("trxType"));
		super.setRecord("payerEmail", email.getString("payerEmail"));
		super.setRecord("`status`", "대기");
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		
		boolean insert = super.insert();
		logger.info("========== insertTrxNtsEmail() - insert : {}", insert );

		super.initRecord();
		return insert;
	}
	
	
	public synchronized SharedMap<String, Object> getAcsRes(String acsId) {
		logger.info("========== getAcsRes() - acsId : " + acsId);
		super.setDebug(true);
		super.setTable("PG_ACS_RES");
		super.setColumns("*");
		super.addWhere("acsId", acsId, eq);
		super.setOrderBy("regDate desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getAcsReq(String acsId) {
		super.setTable("PG_ACS_REQ");
		super.setColumns("*");
		super.addWhere("acsId", acsId, eq);
		super.setOrderBy("regDate desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	/* TEST CODE */
	public synchronized String getAcqIdByIssuerId(String issuer) {
		logger.info("========== getAcqIdByIssuerId() - issuer : " + issuer);
		super.setDebug(true);
		super.setTable("PG_CODE_ISSUER");
		super.setColumns("*");
		super.addWhere("id", issuer, eq);
		super.setOrderBy("regDate desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0).getString("acqId");
	}

	public synchronized SharedMap<String, Object> getDirectVTID(String vanId, String promoType, String acqId) {
		logger.info("========== getDirectVTID() - vanId : " + vanId + " - promoType : " + promoType + " - acqId : " + acqId);
		super.setDebug(true);
		if(promoType.equals("일반")) {
			promoType = "NM";
		} else if(promoType.equals("중소1")) {
			promoType = "SM1";
		} else if(promoType.equals("중소2")) {
			promoType = "SM2";
		} else if(promoType.equals("중소3")) {
			promoType = "SM3";
		} else if(promoType.equals("영세")) {
			promoType = "SM";
		}

		super.setTable("PG_SYS_VAN A JOIN PG_SYS_VTID B ON A.vtid = B.vtid LEFT JOIN PG_SYS_CID C ON A.vtid = C.vtid");
		super.setColumns("B.*, C.cid");
		super.addWhere("A.vanIdx", vanId);
		super.addWhere("B.payType", "3D_" + promoType);
		super.addWhere("C.acqId", acqId);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public synchronized SharedMap<String, Object> getDirectVtidPromo(SharedMap<String, Object> sharedMap,
			SharedMap<String, Object> mchtTmnMap, SharedMap<String, Object> acsResMap, long amount) {
		logger.info("========== getDirectVtidPromo() - amount : " + amount);
		super.setDebug(true);
		String regDay = sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8);

		super.setTable("PG_SYS_3D_PROMO A JOIN PG_SYS_VTID B ON A.vtid = B.vtid LEFT JOIN PG_SYS_CID C ON A.vtid = C.vtid");
		super.setColumns("B.*, C.cid");
		super.addWhere("A.status", "사용");
		super.addWhere("startDay", regDay, DAO.le);
		super.addWhere("endDay", regDay, DAO.gt);

		super.addWhere("(A.acqId IS NULL OR A.acqId = '" + acsResMap.getString("acqId") + "')");
		if (!acsResMap.isEquals("issuerId", "")) {
			// super.addWhere("A.issuerId IS NOT NULL AND A.issuerId = '" +
			// acsResMap.getString("issuerId") + "'");
		}
		if (!acsResMap.isEquals("bin", "")) {
			super.addWhere("(A.bin IS NULL OR A.bin = '" + acsResMap.getString("bin") + "')");
		}
		super.addWhere("(A.mchtId IS NULL OR A.mchtId LIKE '%:" + mchtTmnMap.getString("mchtId") + ":%' )");

		super.setOrderBy("priority");
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized void updateLast4(String trxId, String last4) {
		super.update("UPDATE PG_TRX_REQ set last4 = '"+last4+"' WHERE trxId ='"+trxId+"'");
	}
	
	
	public synchronized void update3DDirect(String payId, String acsId, String trxId) {
		logger.info("========== update3DDirect() - payId : " + payId + " - acsId : " + acsId + " - trxId : " + trxId);
		super.setDebug(true);

		super.update("UPDATE PG_WEBPAY_REQ SET trxId = '" + trxId + "' WHERE payId ='" + payId + "'");
		super.update("UPDATE PG_ACS_RES SET trxId = '" + trxId + "' WHERE acsId ='" + acsId + "'");
	}
	
	public synchronized SharedMap<String,Object> getAcsResByTrxId(String trxId){
		RecordSet rset = super.query("SELECT * FROM PG_ACS_RES WHERE trxId ='"+trxId+"' LIMIT 1");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return new SharedMap<String,Object>();
		}else {
			return map;
		}
	}
	
	public synchronized SharedMap<String,Object> getPayTypeByVTID(String vtid) {
		RecordSet rset = super.query("SELECT * FROM PG_SYS_VTID WHERE vtid ='"+vtid+"' LIMIT 1");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return new SharedMap<String,Object>();
		}else {
			return map;
		}
	}
	
	public synchronized String getCode(String alias,String code) {
		RecordSet rset = super.query("SELECT codeName FROM PG_CODE WHERE `alias`='"+alias+"' and code ='"+code+"' LIMIT 1");
		super.initRecord();
		SharedMap<String,Object> map = rset.getRow(0);
		if(map == null){
			return "";
		}else {
			return map.getString("codeName");
		}
	}

	/* 20191106 yhbae 추가 */
	public synchronized SharedMap<String, Object> getTrxRes(String trxId) {
		super.setTable("PG_TRX_RES");
		super.setColumns("van, vanId, vanTrxId, pairingVtid, pairingRouteVan, pairingCid");
		super.addWhere("trxId", trxId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	/* 20200102 yhbae 추가 */
	public synchronized boolean isCaptured3D(String trxId) {
		super.setTable("PG_TRX_CAP A LEFT JOIN PG_TRX_CAP_3D B ON A.capId = B.capId");
		super.setColumns("capStatus");
		super.addWhere("A.trxId", trxId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		SharedMap<String, Object> resMap = rset.getRow(0);
		super.initRecord();
		String capStatus = resMap.getString("capStatus");
		if(capStatus.equals("매입") || capStatus.equals("요청") || capStatus.equals("대기") || capStatus.equals("반송")) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public synchronized List<SharedMap<String,Object>> searchFDSQ1(String tmnId,String resultCd){
		RecordSet rset = super.query("SELECT trxId FROM PG_TRX_ERR WHERE regDate > DATE_ADD(now(), INTERVAL -11 MINUTE) AND tmnId ='"+tmnId+"' AND resultCd in ("+resultCd+") order by regDate desc ");
		super.initRecord();
		return rset.getRows();
	}
	
	
	public synchronized void insertFDSQ(String trxId,String fdsType) {
		String q = "INSERT IGNORE INTO PG_TRX_FDS  " + " SELECT NULL,'Q','"+fdsType+"',FN_CODE_NAME('FDS_TYPE','"+fdsType+"'),'Y','ERR',NULL,trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,prodId,issuer,acquirer,reqDay,reqTime,'' authCd,resultCd,resultMsg,regDay,regTime,regDate " + 
				" FROM PG_TRX_ERR WHERE trxId = '" + trxId + "'";
		logger.info("set TRX_FDS : {},{}", super.insert(q),fdsType);
		super.initRecord();
	}
	
	
	
	
	public synchronized void insertTrxBL(SharedMap<String,Object> fdsMap,String regDay) {

		
		super.setTable("PG_TRX_BL");
		super.setRecord("unit", "CARD");
		super.setRecord("status", "block");
		super.setRecord("value", fdsMap.getString("hash"));
		super.setRecord("masked", fdsMap.getString("bin")+"******"+fdsMap.getString("last4"));
		super.setRecord("trxId", fdsMap.getString("trxId"));
		super.setRecord("block", 0);
		super.setRecord("activeDay", CommonUtil.getOpDate(GregorianCalendar.DATE, 180, regDay));
		super.setRecord("summary", "[AUTOSET] "+fdsMap.getString("vanResultMsg"));
		super.setRecord("regId", "SYSTEM");
		super.setRecord("regDay", CommonUtil.parseLong(regDay));
		logger.info("set trx_b/l : {},{}", super.insert(), fdsMap.getString("trxId"));
		super.initRecord();

	}
	
	public synchronized SharedMap<String,Object> selectTrxBl(String hash,String regDay){
		super.setTable("PG_TRX_BL");
		super.setColumns("idx,masked,trxId,activeDay");
		super.addWhere("activeDay", CommonUtil.parseLong(regDay),ge);
		super.addWhere("unit", "CARD",eq);
		super.addWhere("status", "block",eq);
		super.addWhere("value", hash,eq);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		}else {
			SharedMap<String,Object> map = rset.getRow(0);
			hitTrxBl(map.getLong("idx"));
			return map;
		}
	}
	
	
	public synchronized void hitTrxBl(long idx) {

		String q = "UPDATE PG_TRX_BL set block = block+1 WHERE idx ="+idx;
		logger.info("set bl hit count up : {}", super.update(q));
		super.initRecord();

	}
	
	
	public synchronized long getHighRiskTmn(String tmnId, String acquirer) {
		logger.info("========== getHighRiskTmn() - tmnId : " + tmnId + " - acquirer : " + acquirer);
		super.setDebug(true);
		super.setTable("PG_MCHT_TMN_HIGH_RISK");
		super.setColumns("amount");
		super.addWhere("tmnId", tmnId);
		super.addWhere("acquirer", acquirer,lk);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return 0;
		}else {
			SharedMap<String,Object> map = rset.getRow(0);
			return map.getLong("amount");
		}
	}

	public synchronized SharedMap<String, Object> getMchtMng3D(String mchtId) {
		logger.info("========== getMchtMng3D() - mchtId : " + mchtId);
		String key = "PG_MCHT_MNG_3D_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setDebug(true);
			super.setTable("PG_MCHT_MNG_3D");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized SharedMap<String, Object> getVanByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_TMN_ID_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN");
			
			super.addWhere("tmnId", tmnId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public synchronized SharedMap<String, Object> getVanByVanId(String mchtId) {
		String key = "PG_VAN_VAN_ID+" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VAN");
			
			super.addWhere("vanId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public synchronized SharedMap<String, Object> getMchtInstall(String mchtId) {
		logger.info("========== getMchtInstall() 가맹점 할부기간정보 조회 - mchtId : " + mchtId);
		super.setDebug(true);
		super.setColumns("*");
		super.setTable("PG_MCHT_MNG");
		super.addWhere("mchtId", mchtId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getVanInstall(String mchtId, int vanIdx) {
		logger.info("========== getVanInstall() PG사 할부기간정보 조회 - mchtId : " + mchtId + " | vanIdx : " + mchtId);
		super.setDebug(true);
		super.setColumns("*");
		super.setTable("VW_MCHT_TMN");
		super.addWhere("mchtId", mchtId);
		super.addWhere("webPay", "사용");
		super.addWhere("vanIdx", vanIdx);
		super.setOrderBy("regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	// 2022-03-25 - semiAuth 비인증, 구인증 로그 수정
	public synchronized void updateTrxReq(SharedMap<String, Object> sharedMap) {
		logger.info("========== setResponse2() - updateTrxReq() ");
		super.setDebug(true);
		super.setTable("PG_TRX_REQ");
		super.setRecord("semiAuth", sharedMap.getString("semiAuth"));
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("========== set PG_TRX_REQ : {}", super.update());
		super.initRecord();
	}
	public synchronized void updateTrxPay(SharedMap<String, Object> sharedMap) {
		logger.info("========== setResponse2() - updateTrxPay() ");
		super.setDebug(true);
		super.setTable("PG_TRX_PAY");
		super.setRecord("semiAuth", sharedMap.getString("semiAuth"));
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("========== set PG_TRX_PAY : {}", super.update());
		super.initRecord();
	}
	
	public synchronized Boolean getTrxIOData(String trxId) {
		super.setDebug(true);
		super.setTable("PG_TRX_IO");
		super.setColumns("*");
		super.addWhere("trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public SharedMap<String,SharedMap> selectCardCodeList(){
		SharedMap<String,SharedMap> map = new SharedMap<String,SharedMap>();
		String query = "SELECT * FROM tbl_cardcode_info; ";
		System.out.println("selectCardCodeList ::: " + query);
		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;
		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				SharedMap<String,Object> data = new SharedMap<String,Object>();
				data.put("idx", rset.getString(1));
				data.put("code", rset.getString(2));
				data.put("cname", rset.getString(3));
				data.put("calias", rset.getString(4));
				map.put(data.getString("idx"), data);
			}
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("sql error : {}, query : {}", e.getMessage(), query);
		} finally {
			db.close(conn, pstmt, rset);
		}
		
		return map;
	}
	
	public synchronized boolean insertRisk(String capId, String summary, String regId) {
		super.setTable("PG_TRX_IQR");
		super.setRecord("capId", capId);
		super.setRecord("iqrType", "정산보류");
		super.setRecord("telNo", "");
		super.setRecord("summary", summary);
		super.setRecord("regId", regId);
		super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		super.setRecord("regDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		boolean inserted = super.insert();
		super.initRecord();
		return inserted;
	}
	
	public synchronized Boolean insertCardCodeLog(String issuer, String acquirer) {
		logger.info("========== insertCardCodeLog() ");
		Boolean returnChk = false;
		super.setDebug(true);

		super.setTable("tbl_cardcode_log");

		super.setRecord("issuer", issuer);
		super.setRecord("acquirer", acquirer);
		
		returnChk = super.insert(); 
		logger.info("========== insertCardCodeLog() insert() : {}", returnChk);
		super.initRecord();
		
		return returnChk;
	}
	
	public synchronized Boolean getRootRiskChkToUpdate(String trxId) {
		logger.info("========== getRootRiskChk() : 정산보류건 취소시, 정산보류 해제 조회 체크 trxId : " + trxId);
		String query = "SELECT * FROM PG_TRX_CAP A JOIN PG_TRX_CAP_DTL B ON A.capId = B.capId WHERE B.risk != '' AND B.stlStatus = '정산대기' AND A.trxId = '" + trxId +"' ";
		logger.info("========== getRootRiskChk(0 query : " + query);
		
		try {
			RecordSet rset = super.query(query);
			super.initRecord();
			SharedMap<String,Object> map = rset.getRow(0);
			if (map == null) {
				logger.info("========== getRootRiskChk() : map : " + map);
				logger.info("========== 원거래건 정산보류 가 아닙니다. ");
				return false;
			} else {
				logger.info("========== getRootRiskChk() : 원거래건 정산보류 건 확인 : " + map.toString());
				logger.info("========== getRootRiskChk() : mchtId : " + map.get("mchtId"));
				logger.info("========== getRootRiskChk() : trxId : " + map.get("trxId"));
				logger.info("========== getRootRiskChk() : capType : " + map.get("capType"));
				logger.info("========== getRootRiskChk() : risk : " + map.get("risk"));
				
				String query_update = "UPDATE PG_TRX_CAP_DTL A JOIN PG_TRX_CAP B ON A.capId = B.capId SET A.risk = '' WHERE A.risk = '정산보류' AND A.stlStatus = '정산대기' AND B.trxId = '" + trxId +"' ";
				logger.info("========== getRootRiskChk(0 query_update : " + query_update);
				Boolean updChk = super.update(query_update);
				logger.info("========== getRootRiskChk(0 update : " + updChk);
				if (updChk) {
					logger.info("========== getRootRiskChk(0 : 전취소 원거래 승인건 정산보류 해제 했습니다.");
					
					String query_update_log = "UPDATE PG_TRX_IQR SET result = '완료' WHERE capId = '" + map.getString("capId") +"' ";
					logger.info("========== getRootRiskChk(0 query_update_log : " + query_update_log);
					Boolean updChkLog = super.update(query_update_log);
					logger.info("========== getRootRiskChk(0 query_update_log : " + updChkLog);
					if (updChkLog) {
						logger.info("========== getRootRiskChk(0 : 전취소 원거래 승인건 정산보류 해제 로그 업데이트 실패했습니다.");
					} else {
						logger.info("========== getRootRiskChk(0 : 전취소 원거래 승인건 정산보류 해제 로그 업데이트 실패했습니다.");
					}
					return updChkLog;
				} else {
					logger.info("========== getRootRiskChk(0 : 전취소 원거래 승인건 정산보류 해제 실패했습니다.");
					return updChk;
				}
				
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logger.info("========== getRootRiskChk() - ERROR : " + e);
			return false;
		}
	}
	
	public synchronized Boolean updateTransactionError(String order_no, String hash_value, int status, int type) {
		logger.info("========== updateTransactionError()");
		try {
			super.setDebug(true);
			super.setTable("PG_TRX_HKH_PAY");
			super.setRecord("order_no", order_no);
			super.setRecord("hash_value", hash_value);
			super.setRecord("status", status);
			super.setRecord("type", type);
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			super.setRecord("regDate", CommonUtil.getCurrentDate("yyyy-MM-dd HH:mm:ss"));
			super.insert();
			super.initRecord();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logger.info("========== updateTransactionError() - Exception : " + e.getMessage());
		}
		return true;
	}

	public synchronized SharedMap<String, Object> getSalesByIdChk(String salesId) {
		logger.info("========== getSalesByIdChk() - salesId : " + salesId);
		super.setDebug(true);
		super.setTable("PG_MAM_SALES");
		super.setColumns("*");
		super.addWhere("salesId", salesId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getSalesMngByIdChk(String salesId) {
		logger.info("========== getSalesMngByIdChk() - salesId : " + salesId);
		super.setDebug(true);
		super.setTable("PG_MAM_SALES_MNG");
		super.setColumns("*");
		super.addWhere("salesId", salesId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getPgCompany() {
		logger.info("========== getPgCompany() ");
		super.setDebug(true);
		super.setTable("PG_COMPANY");
		super.setColumns("*");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public synchronized SharedMap<String, Object> getCardFilterInfo(String mchtId) {
		super.setDebug(true);
		super.setTable("tbl_cardfilter_info");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getBinlockList(String mchtId, String bin) {
		super.setDebug(true);
		super.setTable("tbl_binlock_list");
		super.setColumns("*");
		super.addWhere("used", 1);
		super.addWhere("d_tm IS NULL");
		super.addWhere("name", mchtId, eq);
		super.addWhere("bin", bin, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized int getBinlockListCount(String name, String bin) {
		logger.info("========== getBinlockListCount() - name : " + name + " - bin : " + bin);
		super.setDebug(true);
		super.setTable("tbl_binlock_list");
		super.setColumns("COUNT(*) AS cnt");
		super.addWhere("used", 1);
		super.addWhere("d_tm IS NULL");
		super.addWhere("name", name, eq);
		super.addWhere("bin", bin, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getInt("cnt");
	}
	public synchronized int getBinlockListCount(String mchtId, String vanId, String bin) {
		logger.info("========== getBinlockListCount() - mchtId : " + mchtId + " - vanId : " + vanId + " - bin : " + bin);
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT COUNT(*) AS cnt FROM tbl_binlock_list ");
		sb.append(" WHERE used = 1 AND d_tm IS NULL AND (name = '" + mchtId + "' OR name = '" + vanId + "') AND bin = '" + bin + "' ;");
		logger.info("========== getBinlockListCount - query : " + sb.toString());
		
		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRowFirst().getInt("cnt");
	}
	
	public synchronized SharedMap<String, Object> getWelcomeNotify(String trxId) {
		logger.info("========== getWelcomeNotify() - trxId : " + trxId);
		super.setDebug(true);
		super.setTable("tbl_wellcome_notify");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}
	
	// 2022-12-14
	public synchronized SharedMap<String, Object> getVanByBrand(String brand) {
		logger.info("========== getVanByBrand() - brand : " + brand);
		super.setDebug(true);
		super.setTable("PG_VAN");
		super.addWhere("brand", brand, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getVanByBin(String bin) {
		logger.info("========== getVanByBrand() - bin : " + bin);
		super.setDebug(true);
		super.setColumns("A.*");
		super.setTable("PG_VAN A JOIN (SELECT * FROM PG_CODE_BIN WHERE bin = " + bin + " GROUP BY brand) B ON A.brand = B.brand WHERE A.status = '사용' ");
		super.setOrderBy("");
		//super.addWhere("bin", bin, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	public synchronized SharedMap<String, Object> getVanByBinNSemiAuth(String bin, String semiAuth) {
		logger.info("========== getVanByBrand() - bin : " + bin + " | semiAuth : " + semiAuth);
		super.setDebug(true);
		super.setColumns("A.*");
		super.setTable("PG_VAN A JOIN (SELECT * FROM PG_CODE_BIN WHERE bin = " + bin + " GROUP BY brand) B ON A.brand = B.brand WHERE A.status = '사용' AND A.semiAuth = '" + semiAuth + "' ");
		super.setOrderBy("");
		//super.addWhere("bin", bin, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
}
