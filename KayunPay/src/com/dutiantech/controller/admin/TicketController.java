package com.dutiantech.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dutiantech.Message;
import com.dutiantech.anno.AuthNum;
import com.dutiantech.controller.BaseController;
import com.dutiantech.interceptor.AuthInterceptor;
import com.dutiantech.interceptor.PkMsgInterceptor;
import com.dutiantech.model.FundsTrace;
import com.dutiantech.model.LoanOverdue;
import com.dutiantech.model.LoanTrace;
import com.dutiantech.model.LoanTransfer;
import com.dutiantech.model.Tickets;
import com.dutiantech.model.User;
import com.dutiantech.model.UserInfo;
import com.dutiantech.service.FundsServiceV2;
import com.dutiantech.service.FundsTraceService;
import com.dutiantech.service.LoanOverdueService;
import com.dutiantech.service.LoanTraceService;
import com.dutiantech.service.LoanTransferService;
import com.dutiantech.service.TicketsService;
import com.dutiantech.service.UserInfoService;
import com.dutiantech.service.UserService;
import com.dutiantech.util.CommonUtil;
import com.dutiantech.util.DateUtil;
import com.dutiantech.util.LoggerUtil;
import com.dutiantech.util.StringUtil;
import com.dutiantech.util.SysEnum;
import com.jfinal.aop.Before;
import com.jfinal.core.ActionKey;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.tx.Tx;

public class TicketController extends BaseController {
	
	private TicketsService ticketService = getService(TicketsService.class);
	private FundsServiceV2 fundsServiceV2 = getService(FundsServiceV2.class);
	private FundsTraceService fundsTraceService=getService(FundsTraceService.class);
	private UserService userService=getService(UserService.class);
	private UserInfoService userInfoService=getService(UserInfoService.class);
	private LoanTraceService loanTraceService = getService(LoanTraceService.class);
	private LoanTransferService loanTransferService = getService(LoanTransferService.class);
	private LoanOverdueService loanOverdueService = getService(LoanOverdueService.class);
	
	private static final Logger scanTicketLogger = Logger.getLogger("scanTicketLogger");
	
	static{
		LoggerUtil.initLogger("scanTicket", scanTicketLogger);;
	}
	
	@ActionKey("/saveTickets")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
	public Message saveTickets() {

		String settingsType = getPara("settingsType");
		String opUserCode = getUserCode();
		String userMobile = getPara("userMobile");
		String userName = getPara("userName");
		String userTrueName = getPara("userTrueName");
		String userCode = getPara("userCode");
		String tname = getPara("tname");
		String expDate = getPara("expDate");
		String loanMonth=getPara("loanMonth");
		String isDel=getPara("isDel");
		
		boolean xyz = ticketService.save(userCode, userName, userMobile, userTrueName, tname, expDate, settingsType, opUserCode, SysEnum.makeSource.D,loanMonth,isDel);
		if(xyz){
			return succ("操作完成", true);
		}
		return error("01", "操作失败", false);
	}
	
	@ActionKey("/saveRewardRateTickets")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
	public Message saveRewardRateTickets() {

		String opUserCode = getUserCode();
		String gotrate=getPara("rate");//加息量
		int rate=(int)(Float.parseFloat(gotrate)*100);
		int examount=getParaToInt("examount",0)*100;//限额，默认无限额
		String userMobile = getPara("userMobile");
		String userName = getPara("userName");
		String userTrueName = getPara("userTrueName");
		String userCode = getPara("userCode");
		String tname = getPara("tname");//加息券名称
		String expDate = getPara("expDate");//过期日期
		String loanMonth=getPara("loanMonth","0");//期数限定
		String isDel=getPara("isDel","Y");//是否抵扣
		
		boolean xyz = ticketService.saveRate(userCode, userName, userMobile, userTrueName, tname, expDate, rate, opUserCode, SysEnum.makeSource.D,examount,loanMonth,isDel);
		if(xyz){
			return succ("操作完成", true);
		}
		return error("01", "操作失败", false);
	}
//	@ActionKey("/editTickets")
//	@AuthNum(value=999)
//	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
//	public Message editTickets() {
//		Tickets tickets = getModel(Tickets.class);
//		if(tickets.update()){
//			return succ("操作完成", true);
//		}
//		return error("01", "操作失败", false);
//	}
	
	@ActionKey("/delTickets")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
	public Message delTickets() {
		int tid = getParaToInt("tid");
		if(ticketService.del(tid)){
			return succ("操作完成", true);
		}
		return error("01", "操作失败", false);
	}
	
//	@ActionKey("/updateStates")
//	@AuthNum(value=999)
//	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
//	public Message updateStates() {
//		int tid = getParaToInt("tid");
//		String newState = getPara("newState");
//		String oldState = ticketService.findById4Fields(tid, "tstate").getStr("tstate");
//		if(ticketService.updateTState(tid, oldState, newState)){
//			return succ("操作完成", true);
//		}
//		return error("01", "操作失败", false);
//	}
	
	
	@ActionKey("/getTicketsByPage")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,PkMsgInterceptor.class})
	public Message getTicketsByPage() {
		
		String userCode = getPara("userCode");
		
		Integer pageNumber = getParaToInt("pageNumber");
		Integer pageSize = getParaToInt("pageSize");
		
		String ttype = getPara("ttype");
		
		String tstate = getPara("tstate");
		
		String makeSource = getPara("makeSource");
		
		String beginDate_make = getPara("beginDate_make");
		if(!StringUtil.isBlank(beginDate_make)){
			beginDate_make = beginDate_make + "000000";
		}
		
		String endDate_make = getPara("endDate_make");
		if(!StringUtil.isBlank(endDate_make)){
			endDate_make = endDate_make + "235959";
		}
		
		String beginDate_used = getPara("beginDate_used");
		
		if(!StringUtil.isBlank(beginDate_used)){
			beginDate_used = beginDate_used + "000000";
		}
		
		String endDate_used = getPara("endDate_used");
		
		if(!StringUtil.isBlank(endDate_used)){
			endDate_used = endDate_used + "235959";
		}
		
		String beginDate_expDate = getPara("beginDate_expDate");
		
		String endDate_expDate = getPara("endDate_expDate");
		
		
		Map<String,Object> result =  ticketService.findByPage(pageNumber, pageSize, ttype, tstate,makeSource, userCode, beginDate_make, endDate_make, beginDate_used, endDate_used, beginDate_expDate, endDate_expDate,null);
		
		return succ("查询完成", result);
		
	}
	
	@SuppressWarnings("unchecked")
	@ActionKey("/scanTicket")
	@Before({Tx.class,PkMsgInterceptor.class})
	public Message scanTicket(){
		String key = getPara("key", "");
		if(!key.equals("3.14159265358")){
			return error("01","密匙错误", false );
		}
		try {
			long initTotal = Db.queryLong("select count(tid) from t_tickets where tstate = 'A' and expDate<?",DateUtil.getNowDate());
			long leftTotal = initTotal;
			int doCount = 1 ;
			scanTicketLogger.log(Level.INFO,"[定时任务:扫描过期的奖券]扫描中......共计已过期奖券 "+leftTotal+"条 待处理...");
			while( leftTotal > 0 ){
				List<Object[]> tickets = getTicketList(0,100) ;//每次处理100条
				for(Object[] ticket : tickets){
					String tCode = (String) ticket[0];//奖券tCode
					String expDate = (String) ticket[1];//奖券过期日期
					String userName = (String) ticket[2];//用户名
					String userTrueName = (String) ticket[3];//姓名
					String makeDateTime = (String) ticket[4];//奖券生成日期时间
					try {
						if(StringUtil.isBlank(expDate)){
							expDate = DateUtil.getNowDate();
						}
						int result = DateUtil.compareDateByStr("yyyyMMdd",DateUtil.getNowDate(), expDate );
						if(result == 1){
							Db.update("update t_tickets set tstate = 'D',usedDateTime='00000000000000' where tCode = ? and tstate = 'A' ",tCode);
							scanTicketLogger.log(Level.INFO,"[定时任务:扫描过期的奖券]处理过期的奖券..."+userName+"("+userTrueName+")tCode:["+tCode+"]["+makeDateTime+"]   处理第"+doCount+"条奖券已过期...");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					doCount ++ ;
				}
				leftTotal = Db.queryLong("select count(tid) from t_tickets where tstate = 'A' and expDate<?",DateUtil.getNowDate());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			scanTicketLogger.log(Level.SEVERE,"扫描过期的奖券时发生异常:"+e.getMessage());
			return error("02", "扫描过期的债权时发生异常:"+e.getMessage(), false);
		}
		scanTicketLogger.log(Level.INFO,"扫描过期奖券的任务完成");
		return succ("扫描过期奖券的任务完成", true ) ;
	}
	
	
	/**
	 * 	获取进行中的债权转让信息
	 * @param index
	 * @param size
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private List getTicketList(int index , int size ){
		index = index * size;
		String querySql = "select tCode,expDate,userName,userTrueName,makeDateTime from t_tickets where tstate = 'A' and expDate<? limit ?,?";
		//querySql = querySql.replace("${index}", index+"" ) ;
		//querySql = querySql.replace("${size}", size+"" ) ;
		List ticket =  Db.query(querySql,DateUtil.getNowDate() , index , size ) ;
		return ticket ;
	}
	
	@ActionKey("/subTicket")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,Tx.class,PkMsgInterceptor.class})
	public Message subTicket(){
		String traceCode = getPara("traceCode","");
		String mobile = getPara("mobile");
		String tCode = getPara("tCode");
		String m_mobile = "1";
		try {
			if(CommonUtil.isMobile(mobile) == false){
				return error("00", "输入合法手机号", false);
			}
			m_mobile = CommonUtil.encryptUserMobile(mobile);
		} catch (Exception e) {
			m_mobile = "1";
		}
		long x = Db.queryLong("select count(userCode) from t_user where userMobile = ?",m_mobile);
		if(x < 1)
			return error("01", "用户不存在", false);
		long y = Db.queryLong("select count(traceCode) from t_loan_trace where loanState = 'N' and traceCode = ?",traceCode);
		if(y < 1)
			return error("02", "traceCode不存在，或者该traceCode不是还款中", false);
		String userCode = Db.queryStr("select userCode from t_user where userMobile = ?",m_mobile);
		long z = Db.queryLong("select count(tCode) from t_tickets where tCode = ? and tstate = 'A' and userCode = ?",tCode,userCode);
		if(z < 1)
			return error("03", "该用户的tCode不存在，或者该tCode不是可用的现金券", false);
		if(StringUtil.isBlank(userCode) == false){
			int amount = Db.queryInt("select amount from t_tickets where tCode = ? and tstate = 'A' and userCode = ?" ,tCode,userCode);
			if(amount > 0){
				String loanCode = Db.queryStr("select loanCode from t_loan_trace where traceCode = ?",traceCode);
				if(StringUtil.isBlank(loanCode) == false){
					int i = Db.update("update t_tickets set loanCode = ?,tstate = 'E',usedDateTime = ? where tstate = 'A' and tCode = ? and userCode = ?",loanCode,DateUtil.getNowDateTime(),tCode,userCode);
					if(i > 0){
						fundsServiceV2.recharge(userCode, amount, 0, "补偿现金券抵扣金额", "D");
						return succ("搞完了，券用了，用户余额人工充值补偿"+amount+"分", "搞完了，券已用了，用户余额被人工充值补偿"+amount+"分");
					}
				}
			}
		}
		return error("99", "操作未生效", false);
	}

	 /**
	  * 12月 让神券飞 嗨赚感恩节
	  * 发放加息券
	  * @return
	  */
	 @ActionKey("/grantPrize")
	 @AuthNum(value=999)
	 @Before({Tx.class,PkMsgInterceptor.class})
	 public Message grantPrize(){
		 String activityBeginDate = "20171118";	// 活动开始日期
		 String activityEndDate = "20171217";	// 活动结束日期
		 String nowDate=DateUtil.getStrFromNowDate("yyyyMMdd");//当前日期
		 nowDate=DateUtil.delDay(nowDate, 1);//发放时间为第二天00:01
		 // 判断发奖时间是否属于活动时间范围内
		 if (DateUtil.compareDateByStr("yyyyMMdd", nowDate, activityBeginDate) == -1) {	
			 return error("01", "活动未开始", "活动未开始");
		 }
		 if (DateUtil.compareDateByStr("yyyyMMdd", nowDate, activityEndDate) == 1) {	
			 return error("01", "活动已结束", "活动已结束");
		 }
		 
		 List<FundsTrace> fundsTraces = fundsTraceService.queryPayAmountRanking(nowDate,nowDate, 10);//查询单日投资排名
		 
		 if(fundsTraces==null||fundsTraces.size()==0){
			 return succ("中奖用户为空", null);
		 }
		 
		 //发放加息券
		 String saveError="";//发放错误信息
		 for (int i = 0; i < fundsTraces.size(); i++) {
			 String userCode = fundsTraces.get(i).getStr("userCode");//用户编号
			 String userName = fundsTraces.get(i).getStr("userName");//用户名
			 User user = userService.findById(userCode);
			 String userMobile = "";
			 try {
				 userMobile = CommonUtil.decryptUserMobile(user.getStr("userMobile"));//明文手机号
			 } catch (Exception e) {
			 }
			 UserInfo userInfo = userInfoService.findById(userCode);
			 String userTrueName = userInfo.getStr("userCardName");//身份证号姓名
			 String tname="";//奖券名
			 int rate=0;//加息量
			 if(i<3){
				 tname="1%加息券【让神券飞 嗨赚感恩节】";
				 rate=100;
			 }else if(i<6){
				 tname="0.8%加息券【让神券飞 嗨赚感恩节】";
				 rate=80;
			 }else if(i<10){
				 tname="0.5%加息券【让神券飞 嗨赚感恩节】";
				 rate=50;
			 }
			boolean saveRate = ticketService.saveRate(userCode, userName, userMobile, userTrueName, tname, DateUtil.addDay(nowDate, 30), rate, null, SysEnum.makeSource.B, 2000000, "0", "Y");
			if(!saveRate){//发放错误
				saveError+=" "+userCode;
			}
		 }
		 if("".equals(saveError)){
			 return succ("", "发放成功");
		 }else {
			return error("01", "发放错误", saveError);
		}
	 }
	 
	/**
	 * 计算每期给YIFEI加息返利金额
	 * ws 20171106
	 * pass  带此参数为查看这期反给YF的佣金   不带为查看加息券使用情况
	 * enddate 带此为查询到次天YF加息券使用情况   不带默认为上个月底
	 * */
	@ActionKey("/CYFHMSG")
	@AuthNum(value=999)
	@Before({AuthInterceptor.class,Tx.class,PkMsgInterceptor.class})
	public void checkYFhowMoneyShouldGive(){
		//默认20171118到上个月底
		String pass = getPara("pass");
		int n = -1;
		if("pass".equals(pass)){
			n = -2;
		}
		String date=DateUtil.addMonth(DateUtil.getNowDate(), n).substring(0, 6);
		int days=DateUtil.getDaysByYearMonth(date);
		String begindate=getPara("begindate","20171118");
		String enddate=getPara("enddate",date+days);
		List<LoanTrace> loanTraces = loanTraceService.queryYFloantrace(begindate, enddate);
		String filename = "逸飞加息券.xls";
		String output_html = "";
		HttpServletResponse response = getResponse();
		String output_extType = "application/vnd.ms-excel";
		output_html = "<table border='1'>";
		output_html+="<tr><td colspan='9' style='text-align:center;'><b>逸飞加息券使用详情</b></td></tr>";
		output_html+="<tr><td><b>用户昵称</b></td><td><b>满标时间</b></td><td><b>标编号</b></td><td><b>标的期限</b></td><td><b>投标金额(分)</b></td><td><b>待收本金(分)</b></td><td><b>本期期数</b></td><td><b>佣金(分)</b></td><td><b>剩余佣金(分)</b></td></tr>";
		for(LoanTrace loantrace:loanTraces){
			//若逾期 不计算
			String loanCode = loantrace.getStr("loanCode");
			List<LoanOverdue> loanOverdues = loanOverdueService.findByLoanCode(loanCode, "n", null);
			if(loanTraces != null && loanOverdues.size()>0){
				continue;
			}
			String traceCode=loantrace.getStr("traceCode");
			String loanTicket=loantrace.getStr("loanTicket");
			//分析券类型
			if(!StringUtil.isBlank(loanTicket)){
				JSONArray ja = JSONArray.parseArray(loanTicket);
				String type="";
				String tCode="";
				for (int i = 0; i < ja.size(); i++) {
					JSONObject jsonObj = ja.getJSONObject(i);
					type=jsonObj.getString("type");
					tCode=jsonObj.getString("code");
				}
				if("C".equals(type)&&!StringUtil.isBlank(tCode)){
					//若使用一飞的加息券
					List<Tickets> tickets = ticketService.findYFByCode(tCode);
					if(null!=tickets&&tickets.size()>0){
						int backDate = Integer.parseInt(loantrace.getStr("backDate"));
						//获取此标的债转记录
						List<LoanTransfer> loanTransfers=null;
						int size=0;
						//验证是否在上月回款后债转  若是则算上期
						if("A".equals(loantrace.getStr("isTransfer"))||"B".equals(loantrace.getStr("isTransfer"))){
							loanTransfers=loanTransferService.queryLoanTransferByTraceCode(traceCode, "B");
							size=loanTransfers.size();
							if(size>0){
								String gotDate = loanTraces.get(0).getStr("gotDate");
								if(!StringUtil.isBlank(gotDate)){
									if(Integer.parseInt(gotDate)>=backDate){
										size=0;
									}
								}
							}
						}
						//若未债转过
						if(null==loanTransfers||size==0){
							long payAmount=loantrace.getLong("payAmount");
							int loanTimeLimit=loantrace.getInt("loanTimeLimit");
							String refundType=loantrace.getStr("refundType");
							//若已结清  是上一期就算
							if("pass".equals(pass)){
							if("O".equals(loantrace.getStr("loanState"))||"P".equals(loantrace.getStr("loanState"))){
								String date2=DateUtil.addMonth(DateUtil.getNowDate(), -1).substring(0, 6);
								if(Integer.parseInt(date2+"01")>backDate){
									continue;
								}
							}}
							//计算0.5%加息总利息
							output_html+="<tr>";
							output_html+="<td>"+loantrace.getStr("payUserName")+"</td>";
							output_html+="<td>"+DateUtil.chenageDay(loantrace.getStr("effectDate"))+"</td>";
							output_html+="<td>"+loantrace.getStr("loanNo")+"</td>";
							output_html+="<td>"+loanTimeLimit+"</td>";
							output_html+="<td>"+loantrace.getLong("payAmount")+"</td>";
							output_html+="<td>"+loantrace.getLong("leftAmount")+"</td>";
							int reciedCount=loantrace.getInt("reciedCount");//已还期数
							if("pass".equals(pass)){
								String effectDate = loantrace.getStr("effectDate");
								effectDate=DateUtil.addMonth(effectDate, reciedCount);
								if(Integer.parseInt(effectDate)>Integer.parseInt(enddate)){
									reciedCount=(reciedCount-1<0?0:(reciedCount-1));
								}
							}
							output_html+="<td>"+reciedCount+"</td>";
							long reciedAmount=CommonUtil.f_005(payAmount, 50, loanTimeLimit, reciedCount, refundType)[1];
							long amount1=0;
							if(reciedCount>0){
								amount1=CommonUtil.f_000(payAmount, loanTimeLimit, 50, reciedCount, refundType)[1];
							}
							output_html+="<td>"+amount1+"</td>";
							output_html+="<td>"+reciedAmount+"</td>";
							output_html+="</tr>";
							}
					}
				}
			}
		}
		try {
			filename= new String(filename.getBytes("utf-8"), "ISO_8859_1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.setHeader("Content-Disposition", "attachment;filename="+filename+"-"+DateUtil.getNowDateTime()+".xls");
		renderText(output_html, output_extType);
	}
}