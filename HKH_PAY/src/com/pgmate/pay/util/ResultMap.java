package com.pgmate.pay.util;

import java.util.List;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.map.SharedCacheMap;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Result;

/**
 * @author Administrator
 *
 */
public class ResultMap {

	private static SharedCacheMap cacheMap			= new SharedCacheMap(60*24);
	
	public ResultMap() {
	}
	
	
	public static Result convert(Result result , String lang) {
		
		if(lang.equals("") || lang.equals("ko")) {
			return result;
		}
		
		if(ResultMap.cacheMap.containsKey(result.resultCd)) {
			
		}else {
			DAO dao = new DAO();
			dao.setTable("PG_CODE_PAY");
			dao.setColumns("*");
			RecordSet rset = dao.search();
			dao.initRecord();
			
			List<SharedMap<String,Object>> list = rset.getRows();
			for(SharedMap<String,Object> data : list) {
				ResultMap.cacheMap.put(data.getString("id"),data);
			}
		}
		
		SharedMap<String,Object> map = ResultMap.cacheMap.getUnchecked(result.resultCd);
		if(map == null || map.isEmpty()) {
			
		}else {
			if(lang.equals("en")) {
				result.resultMsg = map.getString("en");
				result.advanceMsg = map.getString("enDesc");
			}else if(lang.equals("ja")) {
				result.resultMsg = map.getString("ja");
				result.advanceMsg = map.getString("jaDesc");
			}else if(lang.equals("zh")) {
				result.resultMsg = map.getString("zh");
				result.advanceMsg = map.getString("zhDesc");
			}else {
				result.resultMsg = map.getString("ko");
				result.advanceMsg = map.getString("koDesc");
			}
		}
		return result;

	}


}
