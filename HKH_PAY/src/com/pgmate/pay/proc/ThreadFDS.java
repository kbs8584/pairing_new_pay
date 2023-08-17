package com.pgmate.pay.proc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.dao.TrxDAO;

/**
 * VAN 거래코드에 따른 FDS 처리 
 *
 */
public class ThreadFDS extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ThreadFDS.class );
	private TrxDAO trxDAO 	= null;
	private SharedMap<String,Object> fdsMap = null;
	
	public ThreadFDS(TrxDAO trxDAO, SharedMap<String, Object> fdsMap) {
		this.trxDAO = new TrxDAO();
		this.fdsMap = fdsMap;

	}

	public void run() {
		String fdsType	= "";
		String resultCd = "";
		if (this.fdsMap.startsWith("van", "KSPAY")) {
			if (this.fdsMap.isEquals("vanResultCd", "8314") || this.fdsMap.isEquals("vanResultCd", "8418") || this.fdsMap.isEquals("vanResultCd", "8037")) {
				fdsType = "Q1";
				resultCd = "'8314','8418','8037'";
			} else if (this.fdsMap.isEquals("vanResultCd", "8350") || this.fdsMap.isEquals("vanResultCd", "8324")) {
				fdsType = "Q2";
				resultCd = "'8350','8324'";
			} else {
			}
		} else if (this.fdsMap.startsWith("van", "ALLAT")) {
			if (this.fdsMap.isEquals("vanResultCd", "6016") || this.fdsMap.isEquals("vanResultCd", "7216")) {
				fdsType = "Q1";
				resultCd = "'6016','7216'";
			} else if (this.fdsMap.isEquals("vanResultCd", "6002")) {
				fdsType = "Q2";
				resultCd = "6002";
			} else {
			}
		} else if (this.fdsMap.startsWith("van", "DANAL")) {
			if (this.fdsMap.isEquals("vanResultCd", "3119") || this.fdsMap.isEquals("vanResultCd", "3110")) {
				fdsType = "Q1";
				resultCd = "'3119','3110'";
			} else if (this.fdsMap.isEquals("vanResultCd", "3122")) {
				fdsType = "Q2";
				resultCd = "3122";
			} else {
			}
		} else {
			logger.info("FDS non_detected");
		}

		if (fdsType.equals("")) {
			logger.info("FDS non_detected");
		} else {
			logger.info("FDS detected : {}, {}", this.fdsMap.getString("trxId"), fdsType);
			if (fdsType.equals("Q1")) {
				List<SharedMap<String, Object>> list = this.trxDAO.searchFDSQ1(this.fdsMap.getString("tmnId"), resultCd);
				if (list.size() > 2) { // 검색 2개이상
					for (SharedMap<String, Object> map : list) {
						this.trxDAO.insertFDSQ(map.getString("trxId"), fdsType);
					}
				} else {
					logger.info("FDS Q1 : count : {},{}", this.fdsMap.getString("trxId"), list.size());
				}
			} else if (fdsType.equals("Q2")) {
				this.trxDAO.insertFDSQ(this.fdsMap.getString("trxId"), fdsType);
				this.trxDAO.insertTrxBL(fdsMap, CommonUtil.getCurrentDate("yyyyMMdd"));
			} else {
			}
		}
	}

}
