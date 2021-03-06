package com.dutiantech.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dutiantech.model.FundsTrace;
import com.dutiantech.model.User;
import com.dutiantech.util.DateUtil;
import com.dutiantech.util.Number;
import com.dutiantech.util.StringUtil;
import com.dutiantech.util.SysEnum;
import com.dutiantech.util.SysEnum.traceSynState;
import com.dutiantech.util.UIDUtil;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;

public class FundsTraceService extends BaseService {
	
	/**
	 * 根据资金流水编号查询资金流水详细数据
	 * @param traceCode		资金流水编号
	 * @return
	 */
	public FundsTrace findById(String traceCode){
		FundsTrace fundsTrace = FundsTrace.fundsTraceDao.findById(traceCode);
		return fundsTrace;
	}
	
	public long findTraceAmount(String userCode, String traceType){
		return Db.queryBigDecimal("select COALESCE(sum(traceAmount),0) from t_funds_trace where userCode = ? and traceType = ?",userCode, traceType).longValue();
	}

	/**
	 * 根据交易类型查询用户时间段内交易列表
	 * @param userCode
	 * @param traceType	交易类型
	 * @param beginDate	yyyyMMdd
	 * @param endDate	yyyyMMdd
	 * @return
	 */
	public List<FundsTrace> queryTraceByUserCodeDate(String userCode, String traceType,String beginDate,String endDate){
		return FundsTrace.fundsTraceDao.find("select traceAmount,traceRemark from t_funds_trace where userCode = ? and traceType = ? and traceDate between ? and ?", userCode,traceType,beginDate,endDate);
	}
	
	/**
	 * 分页查询资金流水对应的统计数据,返回Map,可用于生成excel
	 * @param pageNumber
	 * @param pageSize
	 * @param beginDate
	 * @param endDate
	 * @param traceType
	 * @param fundsType
	 * @param userMobile
	 * @param userCode
	 * @return
	 */
	public Map<String,Object> findByPage4NoobWithSum(String beginDate,String endDate, String traceType, String fundsType, String userCode){
		String sqlFrom = " from t_funds_trace ";
		StringBuffer buff = new StringBuffer("");
		List<Object> paras = new ArrayList<Object>();
		makeExp(buff, paras, "userCode", "=", userCode, "and");
		if(StringUtil.isBlank(traceType) == false){
			String[] traceTypes = traceType.split(",");
			makeExp4In(buff, paras, "traceType", traceTypes, "and");
		}
		makeExp(buff, paras, "traceDateTime", ">=", beginDate, "and");
		makeExp(buff, paras, "traceDateTime", "<=", endDate, "and");
		makeExp(buff, paras, "fundsType", "=", fundsType, "and");
		
		makeExp(buff, paras, "traceType", "!=", "Z", "and");
		makeExp(buff, paras, "traceType", "!=", "J", "and");
		
		Map<String,Object> result = new HashMap<String, Object>();
		
		long count_traceAmount = Db.queryBigDecimal("select COALESCE(sum(traceAmount),0) " + sqlFrom+(makeSql4Where(buff)).toString(),paras.toArray()).longValue();
		
		result.put("count_traceAmount", count_traceAmount);
		
		return result;
	}
	
	/**
	 * 分页查询资金流水,返回Map,可用于生成excel
	 * @param pageNumber	页码
	 * @param pageSize	条数
	 * @param beginDate	开始时间
	 * @param endDate	结束时间
	 * @param traceType	交易类型
	 * @param fundsType	资金类型
	 * @param userMobile	用户手机号
	 * @param userCode	用户编码
	 * @return
	 */
	public Map<String,Object> findByPage4Noob(Integer pageNumber, Integer pageSize, String beginDate,String endDate, String traceType, String fundsType, String userCode){
		String sqlSelect = "select t1.traceCode,t1.userName,t1.traceTypeName,t1.fundsType,t1.traceAmount,t1.traceBalance,t1.traceFrozeBalance,t1.traceFee,t1.traceDateTime,t1.traceDate,t1.traceTime,t1.traceRemark,t2.userCardName,t3.userMobile ";
		String sqlFrom = " from t_funds_trace t1 left join t_user_info t2  on t1.userCode=t2.userCode left join t_user t3 on t3.userCode=t1.userCode";
		StringBuffer buff = new StringBuffer("");
		String sqlOrder = " order by t1.traceDateTime desc, t1.uid desc";  
		List<Object> paras = new ArrayList<Object>();
		makeExp(buff, paras, "t1.userCode", "=", userCode, "and");
		if(StringUtil.isBlank(traceType) == false){
			String[] traceTypes = traceType.split(",");
			makeExp4In(buff, paras, "traceType", traceTypes, "and");
		}else{
			makeExp(buff, paras, "traceType", "!=", "J", "and");
			makeExp(buff, paras, "traceType", "!=", "Z", "and");
		}
		makeExp(buff, paras, "traceDateTime", ">=", beginDate, "and");
		makeExp(buff, paras, "traceDateTime", "<=", endDate, "and");
		makeExp(buff, paras, "fundsType", "=", fundsType, "and");
		
		Page<FundsTrace> pages =  FundsTrace.fundsTraceDao.paginate(pageNumber, pageSize, sqlSelect,sqlFrom+(makeSql4Where(buff)).toString()+sqlOrder,paras.toArray());
		
		Map<String,Object> result = new HashMap<String, Object>();
		
		result.put("firstPage", pages.isFirstPage());
		result.put("lastPage", pages.isLastPage());
		result.put("pageNumber", pages.getPageNumber());
		result.put("pageSize", pages.getPageSize());
		result.put("totalPage", pages.getTotalPage());
		result.put("totalRow", pages.getTotalRow());
		result.put("list", pages.getList());
		
		return result;
	}
	
	/**
	 * 分页查询资金明细，返回Map，用于生成excel
	 */
	public Map<String,Object> findByPage4NoobExcel(Integer pageNumber, Integer pageSize, String beginDate,String endDate, String traceType, String fundsType, String userCode){
		String sqlSelect = "select fundsType,traceAmount,traceBalance,traceFrozeBalance,traceFee,traceDateTime,traceRemark,traceType";
		String sqlFrom = " from t_funds_trace";
		StringBuffer buff = new StringBuffer("");
		String sqlOrder = " order by traceDateTime asc";
		List<Object> paras = new ArrayList<Object>();
		makeExp(buff, paras, "userCode", "=", userCode, "and");
		if(StringUtil.isBlank(traceType) == false){
			String[] traceTypes = traceType.split(",");
			makeExp4In(buff, paras, "traceType", traceTypes, "and");
		}
		makeExp(buff, paras, "traceDate", ">=", beginDate, "and");
		makeExp(buff, paras, "traceDate", "<=", endDate, "and");
		makeExp(buff, paras, "fundsType", "=", fundsType, "and");
		
		Page<FundsTrace> pages =  FundsTrace.fundsTraceDao.paginate(pageNumber, pageSize, sqlSelect,sqlFrom+(makeSql4Where(buff)).toString()+sqlOrder,paras.toArray());
		
		Map<String,Object> result = new HashMap<String, Object>();
		
		result.put("firstPage", pages.isFirstPage());
		result.put("lastPage", pages.isLastPage());
		result.put("pageNumber", pages.getPageNumber());
		result.put("pageSize", pages.getPageSize());
		result.put("totalPage", pages.getTotalPage());
		result.put("totalRow", pages.getTotalRow());
		result.put("list", pages.getList());
		
		return result;
	}
	
	/**
	 * 分页查询所有资金流水
	 * 
	 * @param pageNumber	第几页
	 * @param pageSize		每页多少条
	 * @param beginDate		开始日期
	 * @param endDate		结束日期
	 * @param traceType		交易类型
	 * @param fundsType		资金类型
	 * @param userMobile	电话号码(密文)
	 * @param userCode		用户编码
	 * @return
	 */
	public Map<String,Object> findByPage(Integer pageNumber, Integer pageSize, String beginDate, 
			String endDate, String traceType, String fundsType, String userCode){
		try {
			
			String sqlSelect = "select t1.traceCode,t1.userCode,t1.userName,t1.fundsType,t1.traceType,t1.traceAmount,t1.traceTypeName,"
					+ "t1.traceBalance,t1.traceFrozeBalance,t1.traceFee,t1.traceDate,t1.traceTime,t1.traceSynState,t1.traceRemark,t2.userCardName";
			String sqlFrom = " from t_funds_trace t1 left join t_user_info t2 on t1.userCode = t2.userCode";
			StringBuffer buff = new StringBuffer("");
			String sqlOrder = " order by t1.traceDateTime desc,t1.uid desc";  
			List<Object> paras = new ArrayList<Object>();
			makeExp(buff, paras, "t1.userCode", "=", userCode, "and");
			if(StringUtil.isBlank(traceType) == false){
				String[] traceTypes = traceType.split(",");
				makeExp4In(buff, paras, "t1.traceType", traceTypes, "and");
			}
			makeExp(buff, paras, "t1.traceDate", ">=", beginDate, "and");
			makeExp(buff, paras, "t1.traceDate", "<=", endDate, "and");
			makeExp(buff, paras, "t1.fundsType", "=", fundsType, "and");
			
			Page<FundsTrace> pages = FundsTrace.fundsTraceDao.paginate(pageNumber, pageSize, sqlSelect,  
					sqlFrom+(makeSql4Where(buff)).toString()+sqlOrder,paras.toArray());
			
			Map<String,Object> result = new HashMap<String, Object>();
			
			result.put("firstPage", pages.isFirstPage());
			result.put("lastPage", pages.isLastPage());
			result.put("pageNumber", pages.getPageNumber());
			result.put("pageSize", pages.getPageSize());
			result.put("totalPage", pages.getTotalPage());
			result.put("totalRow", pages.getTotalRow());
			result.put("list", pages.getList());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public long sumTraceAmount(String beginDate, 
			String endDate, String traceType, String fundsType, String userCode){
		
		String selectSql = "select COALESCE(sum(traceAmount),0) from t_funds_trace ";
		
		StringBuffer buff = new StringBuffer("");
		List<Object> paras = new ArrayList<Object>();
		makeExp(buff, paras, "userCode", "=", userCode, "and");
		if(StringUtil.isBlank(traceType) == false){
			String[] traceTypes = traceType.split(",");
			makeExp4In(buff, paras, "traceType", traceTypes, "and");
		}
		makeExp(buff, paras, "traceDate", ">=", beginDate, "and");
		makeExp(buff, paras, "traceDate", "<=", endDate, "and");
		makeExp(buff, paras, "fundsType", "=", fundsType, "and");
		return Db.queryBigDecimal(selectSql+(makeSql4Where(buff)),paras.toArray()).longValue();
		
		
	}
	
	/**
	 * 更新对账状态
	 * @param traceCode			流水编号
	 * @param traceSynState		对账状态
	 * @return
	 */
	public boolean updateTraceSynState(String traceCode, String traceSynState){
		FundsTrace fundsTrace = FundsTrace.fundsTraceDao.findByIdLoadColumns(traceCode,"traceCode,traceSynState");
		fundsTrace.set("traceSynState", traceSynState);
		return fundsTrace.update();
	}
	
	public boolean bidTrace(String userCode , long amount , long avBalance , long fzBalance , int traceFee ,long ticketAmount){
		String remark = "预投标，可用余额转冻结余额。";
//		if( ticketAmount > 0 ){
//			amount = amount - ticketAmount ;
//			//未考虑抵用券类型
//			remark += "现金券抵用：" + ticketAmount/10.0/10.0 + "元";
//		}
		
		FundsTrace fundsTrace = new FundsTrace();
		fundsTrace.set("traceCode", UIDUtil.generate());
		fundsTrace.set("userCode", userCode);
		fundsTrace.set("userName", User.userDao.findByIdLoadColumns(userCode, "userName").get("userName","") );
		fundsTrace.set("traceType", SysEnum.traceType.X.val());
		fundsTrace.set("traceTypeName", SysEnum.traceType.X.desc());
		fundsTrace.set("fundsType", SysEnum.fundsType.D.val());
		fundsTrace.set("traceAmount", amount);
		fundsTrace.set("traceBalance",avBalance );
		fundsTrace.set("traceFrozeBalance", fzBalance );
		fundsTrace.set("traceFee", traceFee);
		fundsTrace.set("traceSynState",traceSynState.N.val());
		fundsTrace.set("traceDateTime", DateUtil.getNowDateTime());
		fundsTrace.set("traceDate", DateUtil.getNowDate());
		fundsTrace.set("traceTime", DateUtil.getNowTime());
		fundsTrace.set("traceRemark", remark );
		return fundsTrace.save() ;
	}
	
	public boolean bidTrace4auto(String userCode , long amount , long avBalance , long fzBalance , int traceFee ,long ticketAmount){
		String remark = "自动预投标，可用余额转冻结余额。";
		if( ticketAmount > 0 ){
			//未考虑抵用券类型
			remark += "现金红包：" + ticketAmount/10.0/10.0 + "元";
		}
		
		FundsTrace fundsTrace = new FundsTrace();
		fundsTrace.set("traceCode", UIDUtil.generate());
		fundsTrace.set("userCode", userCode);
		fundsTrace.set("userName", User.userDao.findByIdLoadColumns(userCode, "userName").get("userName","") );
		fundsTrace.set("traceType", SysEnum.traceType.X.val());
		fundsTrace.set("traceTypeName", SysEnum.traceType.X.desc());
		fundsTrace.set("fundsType", SysEnum.fundsType.D.val());
		fundsTrace.set("traceAmount", amount);
		fundsTrace.set("traceBalance",avBalance );
		fundsTrace.set("traceFrozeBalance", fzBalance );
		fundsTrace.set("traceFee", traceFee);
		fundsTrace.set("traceSynState",traceSynState.N.val());
		fundsTrace.set("traceDateTime", DateUtil.getNowDateTime());
		fundsTrace.set("traceDate", DateUtil.getNowDate());
		fundsTrace.set("traceTime", DateUtil.getNowTime());
		fundsTrace.set("traceRemark", remark );
		return fundsTrace.save() ;
	}
	
	public boolean bidTrace(String userCode , long amount , long avBalance , long fzBalance , int traceFee ){
		return bidTrace(userCode, amount, avBalance, fzBalance, traceFee , 0 );
	}
	
	public boolean bidTrace4Share(String userCode , long amount , long avBalance , long fzBalance){
		String remark = "分享奖励";
		FundsTrace fundsTrace = new FundsTrace();
		fundsTrace.set("traceCode", UIDUtil.generate());
		fundsTrace.set("userCode", userCode);
		fundsTrace.set("userName", User.userDao.findByIdLoadColumns(userCode, "userName").get("userName","") );
//		fundsTrace.set("traceType", SysEnum.traceType.T.val());
//		fundsTrace.set("traceTypeName", SysEnum.traceType.T.desc());
		fundsTrace.set("traceType", SysEnum.traceType.W.val());
		fundsTrace.set("traceTypeName", SysEnum.traceType.W.desc());
		fundsTrace.set("fundsType", SysEnum.fundsType.J.val());
		fundsTrace.set("traceAmount", amount);
		fundsTrace.set("traceBalance",avBalance );
		fundsTrace.set("traceFrozeBalance", fzBalance );
		fundsTrace.set("traceFee", 0);
		fundsTrace.set("traceSynState",traceSynState.N.val());
		fundsTrace.set("traceDateTime", DateUtil.getNowDateTime());
		fundsTrace.set("traceDate", DateUtil.getNowDate());
		fundsTrace.set("traceTime", DateUtil.getNowTime());
		fundsTrace.set("traceRemark", remark );
		return fundsTrace.save() ;
	}
	
	
	/**
	 * 按时间区间查询统计用户投资排行(债权转让相应增减)
	 * @param beginDateTime
	 * @param endDateTime
	 * @param pageNumber
	 * @param pageSize
	 * @return
	 */
	public List<FundsTrace> countToubiao(String beginDateTime,String endDateTime,Integer pageNumber, Integer pageSize){
		pageSize = pageSize * pageNumber;
		pageNumber = (pageNumber - 1) * pageSize;
		List<FundsTrace> result = FundsTrace.fundsTraceDao.find("SELECT traceDateTime,userCode, userName,(SUM(traceAmount) - (SELECT COALESCE (sum(traceAmount), 0) FROM t_funds_trace WHERE userCode = t1.userCode AND traceType = 'B' AND traceDateTime >= ? AND traceDateTime <= ?) ) traceAmount FROM t_funds_trace t1 WHERE traceDateTime >= ? AND traceDateTime <= ? AND (traceType = 'P' OR traceType = 'A' ) GROUP BY userCode ORDER BY traceAmount DESC,traceDateTime asc limit ?,?",beginDateTime,endDateTime,beginDateTime,endDateTime,pageNumber,pageSize);
		return result;
	}
	
	/**
	 * 按时间区间查询统计某一用户用户投资排行(债权转让相应增减)  20170808  WCF
	 * @param beginDateTime
	 * @param endDateTime
	 * @param userCode
	 * @return
	 */
	public BigDecimal countToubiao4One(String beginDateTime,String endDateTime,String userCode){
		BigDecimal loanOutAmt;
		BigDecimal transferInAmt;
		BigDecimal total = new BigDecimal("0");
		List<FundsTrace> loanOut = FundsTrace.fundsTraceDao.find("SELECT SUM(traceAmount)amt FROM t_funds_trace WHERE traceType IN ('P','A') AND traceDateTime >=? AND traceDateTime <= ? AND userCode = ?", beginDateTime,endDateTime,userCode);
		List<FundsTrace> transferIn = FundsTrace.fundsTraceDao.find("SELECT SUM(traceAmount)amt FROM t_funds_trace WHERE traceType = 'B' AND traceDateTime >=? AND traceDateTime <= ? AND userCode = ?", beginDateTime,endDateTime,userCode);
		if (loanOut.get(0).getBigDecimal("amt") == null) {
			loanOutAmt = new BigDecimal("0");
		} else {
			loanOutAmt = loanOut.get(0).getBigDecimal("amt");
		}
		if (transferIn.get(0).getBigDecimal("amt") == null) {
			transferInAmt = new BigDecimal("0");
		} else {
			transferInAmt = transferIn.get(0).getBigDecimal("amt");
		}
		total = loanOutAmt.subtract(transferInAmt);
		return total;
	}
	
	/**
	 * 根据时间查询投标金额
	 * @param beginDateTime
	 * @param endDateTime
	 * @return
	 */
	public long countPayAmount(String beginDateTime, String endDateTime){
		return Db.queryBigDecimal("select COALESCE(sum(traceAmount),0) from t_funds_trace where traceDateTime >= ? and traceDateTime <= ? and traceType = 'P'",beginDateTime,endDateTime).longValue();
	}

	/**
	 * 查询用户第一次回款记录
	 * @param userCode
	 * @return
	 */
	public FundsTrace findFirstBack(String userCode) {
		String sql = "select * from t_funds_trace where userCode = ? and traceType in ('R','L') and fundsType = 'J' order by traceDateTime ";
		return FundsTrace.fundsTraceDao.findFirst(sql ,userCode);
	}
	
	/**
	 * app分页查询资金明细
	 * 
	 * @param pageNumber	第几页
	 * @param pageSize		每页多少条
	 * @param userMobile	电话号码(密文)
	 * @param userCode		用户编码
	 * @return
	 */
	public Map<String,Object> appQueryFundsTraceList(Integer pageNumber, Integer pageSize,  String userCode){
		try {
			
			String sqlSelect = "select t1.traceCode,t1.userCode,t1.userName,t1.fundsType,t1.traceType,t1.traceAmount,t1.traceTypeName,"
					+ "t1.traceDateTime,t1.traceBalance,t1.traceRemark,t1.traceFee";
			String sqlFrom = " from t_funds_trace t1 left join t_user_info t2 on t1.userCode = t2.userCode";
			StringBuffer buff = new StringBuffer("");
			String sqlOrder = " order by t1.uid desc";  
			List<Object> paras = new ArrayList<Object>();
			makeExp(buff, paras, "t1.userCode", "=", userCode, "and");
			makeExp(buff, paras, "traceType", "!=", "Z", "and");
			makeExp(buff, paras, "traceType", "!=", "J", "and");
			
			
			
			Page<FundsTrace> pages = FundsTrace.fundsTraceDao.paginate(pageNumber, pageSize, sqlSelect,  
					sqlFrom+(makeSql4Where(buff)).toString()+sqlOrder,paras.toArray());
			
			Map<String,Object> result = new HashMap<String, Object>();
			
			result.put("firstPage", pages.isFirstPage());
			result.put("lastPage", pages.isLastPage());
			result.put("pageNumber", pages.getPageNumber());
			result.put("pageSize", pages.getPageSize());
			result.put("totalPage", pages.getTotalPage());
			result.put("totalRow", pages.getTotalRow());
			result.put("list", pages.getList());
			
			 System.out.println(pages.getList().size());
			@SuppressWarnings("rawtypes")
			ArrayList list=(ArrayList) pages.getList();
			for (int i = 0; i <list.size(); i++) {
				FundsTrace fundsTrace=(FundsTrace)list.get(i);
				String traceDateTime=fundsTrace.getStr("traceDateTime");
				int traceFee=fundsTrace.getInt("traceFee");
				fundsTrace.put("traceFee",Number.longToString(traceFee));
				String traceAmount=Number.longToString(fundsTrace.getLong("traceAmount"));
				
				String fundsType=fundsTrace.getStr("fundsType");
				if(("D").equals(fundsType)){
					
					fundsTrace.put("fundsTypeName","支出");
					fundsTrace.put("traceAmount","-"+traceAmount);
				}
				else{
					
					fundsTrace.put("fundsTypeName","收入");
					fundsTrace.put("traceAmount","+"+traceAmount);
				}
				// String traceState= fundsTrace.getStr("traceState");
				 String traceBalance=Number.longToString(fundsTrace.getLong("traceBalance"));
				 
				String traceDateTimeFormat=DateUtil.parseDateTime(DateUtil.getDateFromString(traceDateTime, "yyyyMMddHHmmss"),
							"yyyy/MM/dd HH:mm:ss");
				 
				fundsTrace.put("traceDateTime", traceDateTimeFormat);
				fundsTrace.put("traceBalance",traceBalance);
				 result.put("list", pages.getList());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Map<String,Object> appQueryFundsTraceDetail(Integer pageNumber, Integer pageSize,String traceCode){
		try {
			
			String sqlSelect = "select * ";
			String sqlFrom = " from t_funds_trace ";
			StringBuffer buff = new StringBuffer("");
			String sqlOrder = " order by traceDateTime desc";  
			List<Object> paras = new ArrayList<Object>();
			makeExp(buff, paras, "traceCode", "=", traceCode, "and");
		
			
			
			
			Page<FundsTrace> pages = FundsTrace.fundsTraceDao.paginate(pageNumber, pageSize, sqlSelect,  
					sqlFrom+(makeSql4Where(buff)).toString()+sqlOrder,paras.toArray());
			
			Map<String,Object> result = new HashMap<String, Object>();
			
			result.put("firstPage", pages.isFirstPage());
			result.put("lastPage", pages.isLastPage());
			result.put("pageNumber", pages.getPageNumber());
			result.put("pageSize", pages.getPageSize());
			result.put("totalPage", pages.getTotalPage());
			result.put("totalRow", pages.getTotalRow());
			result.put("list", pages.getList());
			
			 System.out.println(pages.getList().size());
			@SuppressWarnings("rawtypes")
			ArrayList list=(ArrayList) pages.getList();
			for (int i = 0; i <list.size(); i++) {
				FundsTrace fundsTrace=(FundsTrace)list.get(i);
				String traceDateTime=fundsTrace.getStr("traceDateTime");
				String traceAmount=Number.longToString(fundsTrace.getLong("traceAmount"));
				int traceFee=fundsTrace.getInt("traceFee");
				fundsTrace.put("traceFee",Number.longToString(traceFee));
				String fundsType=fundsTrace.getStr("fundsType");
				if(("D").equals(fundsType)){
					
					fundsTrace.put("fundsTypeName","支出");
					fundsTrace.put("traceAmount","-"+traceAmount);
				}
				else{
					
					fundsTrace.put("fundsTypeName","收入");
					fundsTrace.put("traceAmount","+"+traceAmount);
				}
				// String traceState= fundsTrace.getStr("traceState");
				 String traceBalance=Number.longToString(fundsTrace.getLong("traceBalance"));
				 
				String traceDateTimeFormat=DateUtil.parseDateTime(DateUtil.getDateFromString(traceDateTime, "yyyyMMddHHmmss"),
							"yyyy/MM/dd HH:mm:ss");
				 
				fundsTrace.put("traceDateTime", traceDateTimeFormat);
				fundsTrace.put("traceBalance",traceBalance);
				 result.put("list", pages.getList());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 查询时间段内投资排名(不含债权转让，现金抵用券)
	 * @param beginDate 格式:yyyyMMdd
	 * @param endDate   格式:yyyyMMdd
	 * @param limit     数据个数
	 * @return
	 */
	public List<FundsTrace> queryPayAmountRanking(String beginDate,String endDate,Integer limit){
		String sql="select userCode,userName,COALESCE(sum(traceAmount),0) traceAmount from t_funds_trace where traceDate BETWEEN ? and ? and traceType='P' group by userCode order by traceAmount desc,traceTime limit 0,?";
		return FundsTrace.fundsTraceDao.find(sql, beginDate,endDate,limit);
	}
	
	
	
	/**
	 * 查询用户活动期间的投资金额
	 */
	public double sumAmountActivityByUser(String beginDate,String endDate,String traceType,String userCode){
		String sqlSelect = "select COALESCE(sum(traceAmount),0)";
		String sqlFrom = " from t_funds_trace";
		String sqlWhere = " where traceDate BETWEEN ? and ? and traceType= ? and userCode = ?";
		return Db.queryBigDecimal(sqlSelect + sqlFrom + sqlWhere,beginDate,endDate,traceType,userCode).doubleValue();
		
	}
	
	/**
	 * 查询首次投资金额
	 * @param userCode	用户编码
	 * @return
	 */
	public long queryFirstInvestAmount(String userCode) {
		List<FundsTrace> fundsTraces = FundsTrace.fundsTraceDao.find("SELECT traceAmount FROM t_funds_trace WHERE userCode = ? AND traceType = 'P' ORDER BY traceDateTime ASC LIMIT 0, 1", userCode);
		if (fundsTraces.isEmpty()) {
			return 0;
		} else {
			return fundsTraces.get(0).getLong("traceAmount");
		}
	}
	
	/**
	 * 查询被邀请用户投资总额
	 * @param userCode	邀请人
	 * @param startDate	统计开始日期 yyyyMMdd
	 * @param endDate	统计结束日期 yyyyMMdd
	 * @return
	 */
	public Long sumInvestAmount4BeRecommondByInvite(String userCode, String startDate, String endDate) {
		return Db.queryBigDecimal("SELECT COALESCE(SUM(traceAmount),0) FROM t_funds_trace WHERE userCode IN(SELECT bUserCode FROM t_recommend_info  WHERE bUserCode IN(SELECT userCode FROM t_user_info WHERE isAuthed='2') AND bRegDate BETWEEN ? AND ? and aUserCode=?) AND traceType='P'", startDate,endDate,userCode).longValue();
	}
	
	/**
	 * 查询被邀请人的投资列表 hw 20171128
	 * @param userCode  邀请人
	 * @param beginDate 统计开始时间yyyyMMdd
	 * @param endDate 统计结束时间yyyyMMdd
	 * 
	 */
	public List<FundsTrace> queryInviteListByUserCode(String userCode,String beginDate,String endDate){
		
		String sql = "SELECT t2.traceAmount,t2.userName,t2.traceDate FROM t_funds_trace t2 WHERE traceType='P' AND t2.userCode IN(SELECT t1.userCode FROM t_user_info t1 WHERE t1.userCode IN(SELECT bUserCode FROM t_recommend_info WHERE aUserCode=? AND bRegDate BETWEEN ? AND ?) AND t1.isAuthed='2')";
		return FundsTrace.fundsTraceDao.find(sql, userCode,beginDate,endDate);
		
	}
	
	/**
	 * 查询用户在指定时间内的回款和返佣奖励
	 * @param startDate
	 * @param endDate
	 * @param userCode
	 * @return
	 */
	public long queryRepaymentAmount(String startDate, String endDate, String userCode){
		String sqlSelect = "select COALESCE(SUM(traceAmount - traceFee),0) ";
		String sqlFrom = " from t_funds_trace ";
		String sqlWhere = " where (traceDate BETWEEN ? and ?) and userCode = ? and traceType in('R','L','W')";
		return Db.queryBigDecimal(sqlSelect + sqlFrom + sqlWhere, startDate, endDate, userCode).longValue();
	}
	
}




