package com.pgmate.pay.main;

import java.util.Iterator;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedCacheMap;
import com.pgmate.pay.proc.Proc;

/**
 * @author Administrator
 *
 */
public class TrxIdScheduler extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.TrxIdScheduler.class);

	@Override
	public void run() {
		try {
			Iterator<String> keys = Proc.trxIdPayMap.keySet().iterator();
			if (Proc.trxIdPayMap.isEmpty() == false) {
				logger.info("TrxIdScheduler : trxIdPayMap : " + Proc.trxIdPayMap.toString());
		        while( keys.hasNext() ){
		            String key = keys.next();
		            if (Proc.trxIdPayMap.containsKey(key) == true) {
		            	int cnt = (int) Proc.trxIdPayMap.get(key);
		            	if (cnt > 0) {
		            		cnt--;
		            		Proc.trxIdPayMap.replace(key, cnt);
		            		logger.info("TrxIdScheduler : trxIdPayMap : " + key + " : " + cnt);
		            	} else {
		            		//Proc.trxIdPayMap.remove(key);
		            		keys.remove();
		            	}
		            }
		        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
