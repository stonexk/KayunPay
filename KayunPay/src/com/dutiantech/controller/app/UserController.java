package com.dutiantech.controller.app;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import com.alibaba.fastjson.JSONObject;
import com.dutian.SMSClient;
import com.dutiantech.Message;
import com.dutiantech.anno.AuthNum;
import com.dutiantech.controller.BaseController;
import com.dutiantech.controller.FuiouController;
import com.dutiantech.controller.JXappController;
import com.dutiantech.controller.portal.validator.LoginValidate;
import com.dutiantech.controller.portal.validator.RegisterValidator;
import com.dutiantech.interceptor.AppInterceptor;
import com.dutiantech.interceptor.PkMsgInterceptor;
import com.dutiantech.model.AuthLog;
import com.dutiantech.model.BizLog.BIZ_TYPE;
import com.dutiantech.model.FanLiTouUserInfo;
import com.dutiantech.model.Funds;
import com.dutiantech.model.QuestionnaireRecords;
import com.dutiantech.model.RecommendInfo;
import com.dutiantech.model.SMSLog;
import com.dutiantech.model.User;
import com.dutiantech.model.UserInfo;
import com.dutiantech.plugins.Memcached;
import com.dutiantech.service.AuthenticationService;
import com.dutiantech.service.FundsServiceV2;
import com.dutiantech.service.SMSLogService;
import com.dutiantech.service.TicketsService;
import com.dutiantech.service.UserInfoService;
import com.dutiantech.service.UserService;
import com.dutiantech.service.UserTermsAuthService;
import com.dutiantech.util.CommonUtil;
import com.dutiantech.util.DESUtil;
import com.dutiantech.util.DateUtil;
import com.dutiantech.util.IdCardUtils;
import com.dutiantech.util.MD5Code;
import com.dutiantech.util.Number;
import com.dutiantech.util.StringUtil;
import com.dutiantech.util.SysEnum;
import com.dutiantech.util.SysEnum.AppSource;
import com.dutiantech.util.UIDUtil;
import com.dutiantech.util.UserUtil;
import com.dutiantech.vo.VipV2;
import com.fuiou.data.ModifyMobileReqData;
import com.fuiou.service.FuiouService;
import com.jfinal.aop.Before;
import com.jfinal.core.ActionKey;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Db;

public class UserController extends BaseController {
	
	private UserService userService = getService(UserService.class);
	private UserInfoService userInfoService = getService(UserInfoService.class);
	private AuthenticationService authenticationService = getService(AuthenticationService.class);
	private TicketsService ticketService = getService(TicketsService.class);
	private SMSLogService smsLogService = getService(SMSLogService.class);
	private FundsServiceV2 fundsServiceV2 = getService(FundsServiceV2.class);
	private UserTermsAuthService userTermsAuthService = getService(UserTermsAuthService.class);
	
	/**
	 * 获取APP来源，判断是Android还是IOS
	 * @return
	 */
	private AppSource getFrom() {
		String httpInfo = getRequest().getHeader("User-Agent");
		httpInfo = httpInfo.toUpperCase();
		if (httpInfo.indexOf("IPHONE") != -1 && httpInfo.indexOf("IOS") != -1) {
			return SysEnum.AppSource.IOS;
		} else if (httpInfo.indexOf("ANDROID") != -1) {
			return SysEnum.AppSource.Android;
		} else {
			return SysEnum.AppSource.Other;
		}
	}
	
	/**
	 * 手机号登录
	 */
	@ActionKey("/app_mobileLogin")
	@Before(LoginValidate.class)
	public void mobileLogin() {
		// TODO 手机号登录
		Map<String , Object> resultMap = new HashMap<String , Object>() ;
		Message msg = null;
		
		//获取参数
		String userMobile = getPara("loginName");
		String userPwd = getPara("loginPwd");
		
		if(StringUtil.isBlank(userMobile)){
			msg = error("01", "手机号为空",resultMap);
			renderJson(msg);
			return ;
		}
		
		if(StringUtil.isBlank(userPwd)){
			msg = error("01", "密码为空", resultMap);
			renderJson(msg);
			return ;
		}
		
		//获取用户登录错误次数
		long errorCount = Memcached.incr("LOGIN_PWDERROR_" + userMobile, 1);
		//密码错误3次   加验证码 并验证
		if(errorCount >= 4){
			msg = checkCapTicket("cac_z02_v1");
			if( msg != null ){
				renderJson(msg);
				return ;
			}
		}
		
		//密码加密
		String loginAuth = "" ;
		User user ;
		try {
			//原始加密算法
			String oldPwd = CommonUtil.getSourcePwd(userPwd);
			loginAuth = MD5Code.md5( userMobile + oldPwd ) ;
			user = userService.find4AuthCode(loginAuth);
			
			//判断错误返回
			if(null == user){
//				//记录日志
//				BIZ_LOG_WARN( userMobile , BIZ_TYPE.LOGIN  ,  "登录失败，用户名或密码错误 ");
				if(errorCount > 3){
					msg = error("05", "用户名或密码错误,加验证码",resultMap);
				}else{
					//Memcached.storeCounter("LOGIN_PWDERROR_" + userMobile, 1);
					msg = error("02", "用户名或密码错误", resultMap);
				}
			}else{
				if (user.isBorrower()) {	// 借款人用户不允许登录
					msg = error("EU001", "用户名或密码错误", resultMap);
				} else if (user.isFrozen()) {	// 冻结用户不允许登录
					msg = error("EU002", "用户被冻结 " + user.getStr("userState"), null);
				} else {
					String userCode = user.getStr("userCode");

					// 同步江西银行存管信息
					if (!StringUtil.isBlank(user.getStr("jxAccountId"))) {
						JXappController jxAppController = new JXappController();
						// 同步银行卡号
						jxAppController.appSynBankCard(user);
						
						// 同步存管授权信息
						userTermsAuthService.syncUserTermsAuth(user);
					}

					// 更新含"x"的身份证号为大写
					userInfoService.update4CardIdUpperCase(userCode);
					
					// 缓存用户信息
					String userName = user.getStr("userName");
					resultMap.put("userCode" , user.getStr("userCode") ) ;
					resultMap.put("userName" , userName ) ;
					resultMap.put("userMobile" , user.getStr("userMobile") ) ;
					resultMap.put("userEmail", user.getStr("userEmail") ) ;
					resultMap.put("lastLoginDateTime", user.getStr("lastLoginDateTime")) ;
					resultMap.put("lastLoginIp", user.getStr("lastLoginIp") ) ;
					resultMap.put("loginCount", user.getInt("loginCount") ) ;
					resultMap.put("vipLevel", user.getInt("vipLevel") ) ;
					resultMap.put("vipLevelName", user.getStr("vipLevelName") );
					resultMap.put("vipInterestRate", user.getInt("vipInterestRate") ) ;
					resultMap.put("vipRiskRate", user.getInt("vipRiskRate") ) ;
					
					//session 缓存
					msg = succ("ok", resultMap );
					String token = UserUtil.UserEnCode(userCode, getRequestIP(), null) ;
					msg.setToken(token);
					setCookieByHttpOnly( AppInterceptor.COOKIE_NAME , token , AppInterceptor.APPINVALIDTIME );
					setCookie("userCode", userCode, AppInterceptor.APPINVALIDTIME);
					setCookie("userName", URLEncoder.encode(userName, "utf-8"), AppInterceptor.APPINVALIDTIME);
					//cached
					Memcached.set("PORTAL_USER_" + userCode , user ) ;
					
					//修改用户登录相关字段
					userService.updateUser4Login(userCode, getRequestIP());
					
					//清除登录错误次数
					Memcached.delete("LOGIN_PWDERROR_" + userMobile);
					//清除短密码登录错误次数
					Memcached.delete("SHORT_PWDERROR_" + userCode);
					
					//绑定返利投
					String source = getPara("source");
					String fltuid = getPara("fltuid");
					if( StrKit.isBlank(source) == false ){
						if( "fanlitou".equals(source.toLowerCase()) == true ){
							//bind 
							Object obj = Memcached.get(source + fltuid );
							if( obj != null ){
								FanLiTouUserInfo.fanlitouDao.saveUser(userCode, userName, userMobile, 
										source , fltuid , 1 );
								Memcached.delete(source + fltuid);
							}
						}
						
					}
					
					// 判断来源
					String bizContent = "用户使用移动端登录";
					if (SysEnum.AppSource.Android.equals(getFrom())) {
						bizContent = "用户使用ANDROID登录";
					} else if (SysEnum.AppSource.IOS.equals(getFrom())) {
						bizContent = "用户使用IOS登录";
					}
					// 记录日志
					BIZ_LOG_INFO( userCode , BIZ_TYPE.LOGIN , bizContent);
				}
			}
		} catch (Exception e) {
			//记录日志
			e.printStackTrace();
			BIZ_LOG_ERROR( userMobile , BIZ_TYPE.LOGIN , "登录异常：" , e);
			msg = error("AX", "系统错误",null);
			renderJson(msg);
		}
		//成功返回
		renderJson(msg);
	}

	private Integer getSMSCount4Day(String mobile){
		String date = DateUtil.getNowDate();
		Object objectDay = Memcached.get("msgExpires_day_" + date + mobile);
		int smsCount4Day = 0;
		if(objectDay != null){
			smsCount4Day = Integer.parseInt(objectDay.toString());
		}

		return smsCount4Day;
	}
	
	/**
	 * 发送短信验证码(无需输入手机号)
	 * @return
	 */
	@ActionKey("/app_sendMsgMac")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void sendMsgMac(){
		Message msg = null;
		String code = getPara("code", "");
		String verifiId = getPara("verifiId", "");
		int type = getParaToInt("type");
		
		if (!CommonUtil.validateCode("verifi_code_" + verifiId, code)) {
			msg = error("03", "图形验证码错误", "");
			renderJson(msg);
			return;
		}
		
		//获取用户标识
		String userCode = getUserCode();
			
		User user = User.userDao.findById(userCode);
		int msgResult = -4;
		String mobile = "";
		String msgMac = CommonUtil.getMathNumber(6);
		String memcachedKey = "";
		String msgContent = "";
		String smsType = "4";
		String smsTypeName = "银行卡绑定";
		switch (type) {
			case 4:
				msgContent = CommonUtil.SMS_MSG_BINDCARD.replace("[code]", msgMac);
				memcachedKey = "SMS_MSG_BINDCARD_";
				smsType = "4";
				smsTypeName = "银行卡绑定";
				break;
			case 1:
				msgContent = CommonUtil.SMS_MSG_PAYPWD.replace("[code]", msgMac);
				memcachedKey = "SMS_MSG_PAYPWD_";
				smsType = "5";
				smsTypeName = "修改支付密码";
				break;
			case 2:
				msgContent = CommonUtil.SMS_MSG_WITHDRAW.replace("[code]", msgMac);
				memcachedKey = "SMS_MSG_WITHDRAW_";
				smsType = "6";
				smsTypeName = "申请提现";
				break;
			case 3:
				msgContent = CommonUtil.SMS_MSG_TRANSFER.replace("[code]", msgMac);
				memcachedKey = "SMS_MSG_TRANSFER_";
				smsType = "7";
				smsTypeName = "手机承接债权";
				break;
			default:
				break;
		}
		
		int smsCount4Day = 0;
		try {
			mobile = DESUtil.decode4string(user.getStr("userMobile"), CommonUtil.DESKEY);
			smsCount4Day = getSMSCount4Day(mobile);
			
			//限制发送频率
			Object object = Memcached.get("msgExpires_" + mobile);
			if(object != null){
				msg = error("02", "相同手机号每分钟只能发送一条短信,请稍后再操作！", "");
				renderJson(msg);
				return;
			}else{
				if(smsCount4Day >= 50){
					msg = error("02", "相同手机号每天只能发送50条短信,请明天再试！", "");
					renderJson(msg);
					return;
				}else{
					msgResult = SMSClient.sendSms(mobile, msgContent);
					//记录日志
					smsLogService.save(setSMSLog(mobile, msgContent, smsType,smsTypeName, msgResult));
				}
			}
		} catch (Exception e) {
			msgResult = -3;
		}
		
		if(msgResult != 0){
			msg = error("01", "发送失败", msgResult);
			renderJson(msg);
			return;
		}else{
			Memcached.set(memcachedKey + userCode , msgMac, 10*60*1000);
			Memcached.set("msgExpires_" + mobile , mobile, 60*1000);
			setSMSCount4Day(mobile, smsCount4Day++);
			msg = succ("发送成功", mobile);
			
			
		}
		renderJson(msg);
	}
	
	
	/**
	 * 找回密码
	 * @param userMobile
	 * @param newPwd
	 * @param smsmsg
	 * @return
	 */
	@ActionKey("/app_findPwd4user")
	public void findPwd4user(){
		
		Message msg = null;
		
		String userMobile = getPara("userMobile");
		String newPwd = getPara("newPwd");
		String smsmsg = getPara("smsmsg");
		
		//数据验证
		if(StringUtil.isBlank(userMobile) || StringUtil.isBlank(newPwd) || StringUtil.isBlank(smsmsg)){
			msg = error("01", "请检查是否输入正确!", "");
		}else{
			//验证短信验证码
			if(CommonUtil.validateSMS("SMS_MSG_FINDPWD_" + userMobile, smsmsg) == false){
				msg = error("03", "短信验证码不正确", "");
			}else{
				//获取用户信息
				User user = userService.find4mobile(userMobile);
				if(null == user){
					msg = error("02", "用户不存在", "");
				}else{
					//修改密码
					try {
						newPwd = CommonUtil.getSourcePwd(newPwd);
						String authcode = MD5Code.md5( userMobile + newPwd ) ;
						user.set("loginAuthCode", authcode);
						user.set("loginPasswd", MD5Code.md5(newPwd));
						user.update();
						//记录日志
						BIZ_LOG_INFO(user.getStr("userCode"), BIZ_TYPE.FINDPWD, "用户通过手机验证找回密码成功");
						msg = succ("修改密码成功", "");
					} catch (Exception e) {
						//记录日志
						BIZ_LOG_ERROR(user.getStr("userCode"), BIZ_TYPE.FINDPWD, "用户通过手机验证找回密码失败", e);
						msg = error("04", "修改密码异常", e.getMessage());
					}
				}
			}
		}
		renderJson(msg); 
	}
	/**
	 * 手机站获取短信验证码(需要手机号)
	 * @param type  0 注册用户  1 找回密码 2 绑定邮箱 3 其他
	 * @return
	 */
	@ActionKey("/app_sendMsgMobile")
	@AuthNum(value=999)
	public void sendMsgMobil(){
		
		Message msg = null;
		
		String mobile = getPara("mobile");
		Integer type = getParaToInt("type");
		String code = getPara("code", "");
		String verifiId = getPara("verifiId", "");
		
		if(StringUtil.isBlank(mobile)){
			msg = error("01", "手机号为空", "");
			renderJson(msg);
			return;
		}
		if(null == type){
			msg = error("02", "发送类型为空", "");
			renderJson(msg);
			return;
		}
		if (!CommonUtil.validateCode("verifi_code_" + verifiId, code)) {
			msg = error("03", "图形验证码错误", "");
			renderJson(msg);
			return;
		}
		
		//限制发送频率
		Object object = Memcached.get("msgExpires_" + mobile);
		int smsCount4Day = getSMSCount4Day(mobile);

		if(object != null){
			msg = error("02", "您操作太频繁,请稍后再操作!", "");
		}else{
			int msgResult = -9;
			boolean b = true;
			String msgMac = CommonUtil.getMathNumber(6);
			String memcachedKey = "";
			String msgContent = "";
			String smsType = "0";
			String smsTypeName = "用户注册";
			switch (type) {
				case 0:
					//注册时 验证手机号是否被注册
					User user = userService.find4mobile(mobile);
					if(null != user){
						b = false;
						break;
					}
					memcachedKey = "SMS_MSG_REGISTER_";
					smsType = "0";
					smsTypeName = "用户注册";
					msgContent = CommonUtil.SMS_MSG_REGISTER.replace("[code]", msgMac);
					break;
				case 1:
					memcachedKey = "SMS_MSG_FINDPWD_";
					smsType = "1";
					smsTypeName = "找回密码";
					msgContent = CommonUtil.SMS_MSG_FINDPWD.replace("[code]", msgMac);
					break;
				case 2:
					memcachedKey = "SMS_MSG_BINDEMAIL_";
					smsType = "2";
					smsTypeName = "绑定邮箱";
					msgContent = CommonUtil.SMS_MSG_BINDEMAIL.replace("[code]", msgMac);
					break;
				case 3:
					User newUser = userService.find4mobile(mobile);
					if (null != newUser) {
						msg= error("04", "新手机号已存在", "");
						renderJson(msg);
						return;
					}
					memcachedKey="SMS_MSG_PHONE_";
					smsType="3";
					smsTypeName="更换手机号";
					msgContent=CommonUtil.SMS_MSG_PHONE.replace("[code]",msgMac);
					break;
//				case 4:
//					memcachedKey = "SMS_MSG_TEST_";
//					smsType = "4";
//					smsTypeName = "现金券测试";
//					msgContent = CommonUtil.SMS_MSG_LOGIN.replace("[code]", msgMac);
//					break;
			}
			try {
				if(b){
					if(smsCount4Day >= 50){
						msg = error("03", "相同手机号每天只能发送50条短信,请明天再试！", "");
					}else{
						msgResult = SMSClient.sendSms(mobile, msgContent);
						if(msgResult != 0){
							msg = error("01", "发送失败", msgResult);
						}else{
							Memcached.set(memcachedKey + mobile , msgMac, 10*60*1000);
							Memcached.set("msgExpires_" + mobile , mobile, 60*1000);
							setSMSCount4Day(mobile, smsCount4Day++);
							msg = succ("发送成功", mobile);
						}
						//记录日志
						smsLogService.save(setSMSLog(mobile, msgContent, smsType,smsTypeName, msgResult));
					}
				}else{
					msg = error("04", "该手机号已经被注册", null);
					//msg = error("04", "紧急维护，临时关闭注册至2月12日！望谅解！", null);
				}
			} catch (Exception e) {
				msg = error("05", "发送失败", null);
			}
		}
		renderJson(msg);
	}
	/**
	 * 记录短信条数
	 * */
	private void setSMSCount4Day(String mobile,Integer value){
		String date = DateUtil.getNowDate();
		Memcached.set("msgExpires_day_" + date + mobile , value, 24*60*60*1000);
	}
	/**
	 * 记录短信日志
	 * */
	private SMSLog setSMSLog(String mobile,String content,String type,String typeName,int status){
		SMSLog smsLog = new SMSLog();
		smsLog.set("mobile", mobile);
		smsLog.set("content", content);
		smsLog.set("type", type);
		smsLog.set("typeName", typeName);
		smsLog.set("status", status);
		return smsLog;
	}
	
	/**
	 * app通过手机号注册
	 * @return
	 */
	@ActionKey("/app_registermobile")
	@Before(RegisterValidator.class)
	public void register(){
		Message msg = null;
		if (true) {
			msg = error("99", "注册通道暂时关闭", "");
			renderJson(msg);
			return;
		}
		
		//获取参数并验证
		String userMobile = getPara("userMobile");
		String loginPasswd = getPara("loginPasswd");
		String userName =getPara("userName");
		String uCheckCode = getPara("uCheckCode");//验证码
		String fMobile = getPara("fMobile"); //推荐人手机
		String source = getCookie("platform");
		
		if(StringUtil.isNumeric(userName)){
			msg = error("02", "用户昵称不能为纯数字", "");
			renderJson(msg);
			return;
		}
		
		//验证短信验证码
		if(CommonUtil.validateSMS("SMS_MSG_REGISTER_" + userMobile, uCheckCode) == false){
			msg = error("05", "短信验证码不正确", "");
		}else{
			//验证用户名是否被注册
			List<User> listUser = userService.find4userName(userName);
			if(null != listUser && listUser.size() > 0){
				msg = error("06", "用户昵称已经被注册", "");
				renderJson(msg);
				return;
			}
			
			//验证是否已经存在此手机号
			User user = userService.find4mobile(userMobile);
			if(null != user){
				msg = error("06", "手机号已经被注册", null);
			}else{
				String regUserCode = UIDUtil.generate();
				try {
					//添加
					String sysDesc = "用户自助注册"+DateUtil.getNowDateTime();
					if ("p2peye".equals(source)) {
						sysDesc = "[p2peye][" + DateUtil.getNowDateTime() + "]";
					}
					System.out.println(sysDesc);
					boolean b = userService.save(regUserCode,userMobile, "", loginPasswd,userName, getRequestIP(), sysDesc);
					if(b == false){
						//记录日志
						BIZ_LOG_ERROR(userMobile, BIZ_TYPE.REGISTER, "注册失败",null);
						msg = error("07", "注册失败", "");
					}else{
						// TODO 新用户注册奖励_APP
						// 注册成功送518现金券
						ticketService.toReward4newUser(regUserCode);
						// 注册成功送可用积分
//						fundsServiceV2.doPoints(regUserCode, 0 , 1000, "注册送积分") ;
						
						//检查是否有推荐人
						String fUserCode = getCookie("fc","");
						User fuser = null;
						if( StringUtil.isBlank(fUserCode) == false ){
							fuser = userService.findById(fUserCode) ;
							
						}else if(StringUtil.isBlank(fMobile) == false){
							if(StringUtil.isNumeric(fMobile.trim()) && (fMobile.trim().length() == 11)){//手机号
								fuser = userService.find4mobile(fMobile);
							}else{//邀请码
								fuser = userService.findByInviteCode(fMobile);
							}
						}
						
						if( fuser != null ){
							//添加邀请记录
							RecommendInfo rmdInfo = new RecommendInfo();
							rmdInfo.set("aUserCode", fuser.getStr("userCode"));
							rmdInfo.set("aUserName", fuser.getStr("userName"));
							rmdInfo.set("bUserCode", regUserCode);
							rmdInfo.set("bUserName", userName);
							rmdInfo.set("bRegDate", DateUtil.getNowDate());
							rmdInfo.set("bRegTime", DateUtil.getNowTime());
							rmdInfo.set("rmdType", "");
							rmdInfo.set("rmdRemark", "好友推荐注册");
							rmdInfo.save();
							removeCookie("fc");
						}
						//记录用户注册日期
						String encryptUserMobile = CommonUtil.encryptUserMobile(userMobile);
						BIZ_LOG_INFO(encryptUserMobile, BIZ_TYPE.REGISTER, getFrom() + "注册用户成功");
					}
				} catch (Exception e) {
					//记录日志
					BIZ_LOG_ERROR(userMobile, BIZ_TYPE.REGISTER, getFrom() + "注册失败",e);
					msg = error("08", "注册失败", "");
				}
				
				msg = succ("恭喜您,注册成功!", "");
				//注册完直接登录
				String oldPwd = CommonUtil.getSourcePwd(loginPasswd);
				String loginAuth="";
				try {
					loginAuth = MD5Code.md5( userMobile + oldPwd );
				} catch (Exception e) {
					e.printStackTrace();
				}
				User newUser = userService.find4AuthCode(loginAuth);
				if(null!=newUser){
				Map<String , Object> resultMap = new HashMap<String , Object>() ;
				resultMap.put("userCode" , regUserCode ) ;
				String userName2 = newUser.getStr("userName");
				resultMap.put("userName" , userName2 ) ;
				resultMap.put("userMobile" , newUser.getStr("userMobile") ) ;
				resultMap.put("userEmail", newUser.getStr("userEmail") ) ;
				resultMap.put("lastLoginDateTime", newUser.getStr("lastLoginDateTime")) ;
				resultMap.put("lastLoginIp", newUser.getStr("lastLoginIp") ) ;
				resultMap.put("loginCount", newUser.getInt("loginCount") ) ;
				resultMap.put("vipLevel", newUser.getInt("vipLevel") ) ;
				resultMap.put("vipLevelName", newUser.getStr("vipLevelName") );
				resultMap.put("vipInterestRate", newUser.getInt("vipInterestRate") ) ;
				resultMap.put("vipRiskRate", newUser.getInt("vipRiskRate") ) ;
				
				//session 缓存
				msg = succ("ok", resultMap );
				String token = UserUtil.UserEnCode(regUserCode, getRequestIP(), null) ;
				msg.setToken(token);
				setCookieByHttpOnly( AppInterceptor.COOKIE_NAME , token , AppInterceptor.APPINVALIDTIME );	
				//cached
				Memcached.set("PORTAL_USER_" + regUserCode , newUser ) ;
				
				//修改用户登录相关字段
				userService.updateUser4Login(regUserCode, getRequestIP());
				
				//绑定返利投
				String source2 = getPara("source");
				String fltuid = getPara("fltuid");
				if( StrKit.isBlank(source2) == false ){
					if( "fanlitou".equals(source2.toLowerCase()) == true ){
						//bind 
						Object obj = Memcached.get(source2 + fltuid );
						if( obj != null ){
							FanLiTouUserInfo.fanlitouDao.saveUser(regUserCode, userName2, userMobile, 
									source2 , fltuid , 1 );
							Memcached.delete(source2 + fltuid);
						}
					}
					
				}
				
				// 判断来源
				String bizContent = "用户使用移动端登录";
				if (SysEnum.AppSource.Android.equals(getFrom())) {
					bizContent = "用户使用ANDROID登录";
				} else if (SysEnum.AppSource.IOS.equals(getFrom())) {
					bizContent = "用户使用IOS登录";
				}
				//记录日志
				BIZ_LOG_INFO( regUserCode, BIZ_TYPE.LOGIN, bizContent);
				}
			}
		}
		renderJson(msg);
	}
	
	/**
	 * 实名认证
	 * */
	@ActionKey("/app_certificationAuto")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public Message certificationAuto(){
		//获取参数
				String trueName = getPara("trueName");
				String cardId = getPara("cardId");
				
				//获取用户标识
				String userCode = getUserCode();
				//验证
				if(StringUtil.isBlank(trueName)){
					return error("01", "姓名为空!", "");
				}
				if(!IdCardUtils.validateCard(cardId)){
					return error("01", "身份证不正确!", "");
				}
				
				String md5CardId = "";
				try{
					md5CardId = CommonUtil.encryptUserCardId(cardId);
				}catch(Exception e){
					return error("02", "身份证加密错误", "");
				}
				
				//验证身份证是否已经被认证
				UserInfo userInfo = UserInfo.userInfoDao.findFirst("select isAuthed from t_user_info where userCardId = ?" , md5CardId);
				if(userInfo != null && "2".equals(userInfo.getStr("isAuthed"))){
					return error("06", "身份证已被认证", "");
				}
				
				//次数限制
				Long count = Db.queryLong("select count(1) from t_auth_log where userCode = ?" ,userCode);
				if(count > 3){
					return error("09", "认证次数超限制", "");
				}
				
				//调用自动认证
				int ret = authenticationService.autoAuth(trueName, cardId);
				if(0 != ret){
					saveAuthLog(userCode);
					return error("04", "请输入正确的姓名和身份证号", "");
				}
				
				//保存用户信息扩展表
//				UserInfo updateUserInfo = new UserInfo();
//				updateUserInfo.set("userCode", userCode);
//				updateUserInfo.set("userCardName", trueName);
//				updateUserInfo.set("userCardId", md5CardId);
//				updateUserInfo.set("isAuthed", "2");
//				boolean update = updateUserInfo.update();
//				if(!update){
//					BIZ_LOG_ERROR(userCode, BIZ_TYPE.USERINFO, "自动认证异常", null);
//					return error("05", "自动认证异常!", "");
//				}
				
				boolean update = userInfoService.userAuth(userCode,trueName, md5CardId, "","2");
				if(!update){
					BIZ_LOG_ERROR(userCode, BIZ_TYPE.USERINFO, "自动认证异常或重复认证", null);
					return error("05", "已经认证,请勿重复提交!", "");
				}
				
				//记录日志
				BIZ_LOG_INFO(userCode, BIZ_TYPE.USERINFO, "用户自动认证成功");
				
				// 身份认证成功赠送可用积分	add by stonexk at 20170601
//				fundsServiceV2.doPoints(userCode, 0 , 2000, "注册送积分");
				// end add
				
				// 身份认证后，邀请人添加30元现金抵用券  5月活动,9月继续，常态
				try{
					User user = userService.findById(userCode);
					RecommendInfo rmd = RecommendInfo.rmdInfoDao.findFirst("select * from t_recommend_info where bUserCode = ?",user.getStr("userCode"));
					if(rmd != null){
						User shareUser = userService.findById(rmd.getStr("aUserCode"));
						if(shareUser!=null){
							//实名认证送券
//							boolean aa = ticketService.save(shareUser.getStr("userCode"), shareUser.getStr("userName"),CommonUtil.decryptUserMobile(shareUser.getStr("userMobile")) , "", 
//									"30元现金券【好友实名认证奖励】", DateUtil.addDay(DateUtil.getNowDate(), 15), "F", null, SysEnum.makeSource.A);
							boolean aa = ticketService.saveADV(shareUser.getStr("userCode"), "50元现金券【好友实名认证奖励】", DateUtil.addDay(DateUtil.getNowDate(), 30), 5000, 1000000);
							if(aa){
								String mobile = userService.getMobile(shareUser.getStr("userCode"));
								String content = CommonUtil.SMS_MSG_TICKET.replace("[huoDongName]", "推荐好友实名认证").replace("[ticketAmount]", "50");
								SMSLog smsLog = new SMSLog();
								smsLog.set("mobile", mobile);
								smsLog.set("content", content);
								smsLog.set("userCode", shareUser.getStr("userCode"));
								smsLog.set("userName", shareUser.getStr("userName"));
								smsLog.set("type", "15");smsLog.set("typeName", "送现金券活动");
								smsLog.set("status", 9);
								smsLog.set("sendDate", DateUtil.getNowDate());
								smsLog.set("sendDateTime", DateUtil.getNowDateTime());
								smsLog.set("break", "");
								smsLogService.save(smsLog);
							}
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				return succ("认证成功", "");
		
	}
	/**
	 * 保存自动认证次数
	 * @param userCode
	 */
	private void saveAuthLog(String userCode){
		AuthLog authLog = new AuthLog();
		authLog.set("userCode", userCode);
		authLog.set("dateTime", DateUtil.getNowDateTime());
		authLog.save();
	}
	/**
	 * 判断是否登录
	 * */
	@ActionKey("/app_isLogin")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void appIsLogin(){
		Message msg=null;
		String userCode=getUserCode();
		if(null!=userCode&&!"".equals(userCode)){
			msg=succ("登录成功", "");
		}
		renderJson(msg);
	}
	/**
	 * 判断是否实名
	 * */
	@ActionKey("/app_isreal")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void appIsReal(){
		String userCode=getUserCode();
		Message msg=null;
		UserInfo userInfo = userInfoService.findById(userCode);
		String cardid=userInfo.getStr("userCardId");
		try {
			cardid=CommonUtil.decryptUserCardId(cardid);
		} catch (Exception e) {
			// TODO: handle exception
		}
		if("".equals(cardid)){
			msg=error("01", "未实名认证", "");
		}else{
			msg=succ("ok", "");
		}
		renderJson(msg);
	}
	
	/**
	 * 退出登录
	 * */
	@ActionKey("/app_saybey")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void appSayBey(){
		Cookie[] cookies = getCookieObjects();
		for (Cookie cookie : cookies) {
			removeCookie(cookie.getName());
		}
		renderJson(succ("deleted", ""));
	}
	
	
	/**
	 * app修改登录密码
	 * @return
	 */
	@ActionKey("/app_updateLoginPwd")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void updateLoginPwd() {
		
		String oldPwd = getPara("oldPwd");
		String newPwd = getPara("newPwd");
		Message message=null;
		//数据验证
		
		//获取用户标识
		String userCode = getUserCode();

		//获取用户信息
		User user = User.userDao.findById(userCode);
		
		try {
			oldPwd = MD5Code.md5(CommonUtil.getSourcePwd(oldPwd));
			if(!oldPwd.equals(user.getStr("loginPasswd"))){
				//记录日志
				BIZ_LOG_INFO(userCode, BIZ_TYPE.USER, "修改用户登录密码失败，旧密码不正确");
				message=error("03", "旧密码不正确", "");
			}
			newPwd = CommonUtil.getSourcePwd(newPwd);
			String authcode = MD5Code.md5(CommonUtil.decryptUserMobile(user.getStr("userMobile")) + newPwd ) ;
			user.set("loginAuthCode", authcode);
			user.set("loginPasswd",MD5Code.md5( newPwd ));
			user.update();
		} catch (Exception e) {
			BIZ_LOG_ERROR(userCode, BIZ_TYPE.USER, "修改用户登录密码失败04", e);
			message=error("04", "修改密码异常", e.getMessage());
		}
		
		//记录日志
		BIZ_LOG_INFO(userCode, BIZ_TYPE.USER, "修改用户登录密码成功");
		message=succ("修改密码成功", "");
		renderJson(message);
	}
	
	/**
	 * app修改支付密码
	 * @return
	 */
	@ActionKey("/app_updatePayPwd")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void updatePayPwd() {
		
		String payPwd = getPara("payPwd");
		String msgPayMac = getPara("msgPayMac");
		Message message=null;
		//数据验证
		if(StringUtil.isBlank(payPwd)){
			message= error("01", "支付密码不能为空!", "");
			renderJson(message);
			return;
		}
		if(StringUtil.isBlank(msgPayMac)){
			message= error("01", "短信验证码不能为空!", "");
			renderJson(message);
			return;
		}
		
		String userCode = getUserCode();

		//获取用户信息
		User user = User.userDao.findById(userCode);
		if(null == user){
			message= error("02", "用户查询错误", "");
			renderJson(message);
			return;
		}
		
		//验证短信验证码
		if(CommonUtil.validateSMS("SMS_MSG_PAYPWD_" + userCode, msgPayMac) == false){
			//记录日志
			BIZ_LOG_INFO(userCode, BIZ_TYPE.USER, "修改支付密码失败，短信验证码不正确");
			message= error("05", "短信验证码不正确", "");
			renderJson(message);
			return;
		}
		
		try {
			payPwd = CommonUtil.encryptPasswd(payPwd);
			user.set("payPasswd", payPwd);
			user.update();
		} catch (Exception e) {
			//记录日志
			BIZ_LOG_ERROR(userCode, BIZ_TYPE.USER, "修改支付密码异常", e);
			message= error("05", "修改支付密码异常", e.getMessage());
			renderJson(message);
			return;
		}
		
		//记录日志
		BIZ_LOG_INFO(userCode, BIZ_TYPE.USER, "修改支付密码成功");
		
		message= succ("修改支付密码成功", "");
		renderJson(message);
	}
	//判断是否激活存管
	@ActionKey("/app_isFuiouCount")
	@AuthNum(value=999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void appIsFuiouCount(){
		String userCode=getUserCode();
		Message msg=null;
		User user=userService.findById(userCode);
		if("".equals(user.getStr("loginId"))||null==user.getStr("loginId")){
			msg=error("01", "未激活存管账户", "");
		}else{
			msg=succ("已激活", "");
		}
		renderJson(msg);
	}

	// app更换手机号 2017.10.24 rain
	@ActionKey("app_updateYrMobile")
	@AuthNum(value = 999)
	@Before({ AppInterceptor.class, PkMsgInterceptor.class })
	public void app_updateYrMobile() {
		// 获取用户标识
		String userCode = getUserCode();
		Message msg = null;
		// 获取用户信息
		User user = User.userDao.findById(userCode);

		String oldMobile = "";
		try {
			oldMobile = CommonUtil.decryptUserMobile(user.getStr("userMobile"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String newMobile = getPara("newMobile", "");
		String msgMac = getPara("msgMac");
		String loginPwd = getPara("loginPwd", "");

		if (StringUtil.isBlank(newMobile)) {
			msg = error("01", "手机号不能为空", "");
			renderJson(msg);
			return;
		}
		if (StringUtil.isBlank(loginPwd)) {
			msg = error("02", "密码不能为空", "");
			renderJson(msg);
			return;
		}

		if (CommonUtil.isMobile(newMobile) == false) {
			msg = error("03", "手机号格式不正确", "");
			renderJson(msg);
			return;
		}
		User newUser = userService.find4mobile(newMobile);
		if (null != newUser) {
			msg = error("04", "新手机号已存在", "");
			renderJson(msg);
			return;
		}

		if (CommonUtil.validateSMS("SMS_MSG_PHONE_" + newMobile, msgMac) == false) {
			msg = error("05", "短信验证码不正确", "");
			renderJson(msg);
			return;
		}
		String oldPwd = "";
		try {
			oldPwd = MD5Code.md5(CommonUtil.getSourcePwd(loginPwd));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!oldPwd.equals(user.getStr("loginPasswd"))) {
			// 记录日志
			BIZ_LOG_INFO(user.getStr("userCode"), BIZ_TYPE.USER,
					"修改用户手机号失败，登录密码不正确");
			msg = error("06", "登录密码不正确", "");
			renderJson(msg);
			return;
		}
		boolean b = userService.updateUserMobile(user.getStr("userCode"),
				newMobile, loginPwd);
		if (b == false) {
			msg = error("07", "修改异常", "");
			renderJson(msg);
			return;
		} else {
			BIZ_LOG_INFO(user.getStr("userCode"), BIZ_TYPE.USER,
					"修改平台用户手机号：平台用户手机号【" + oldMobile + "】修改为 【" + newMobile
							+ "】");
			msg = succ("平台手机号修改成功,请重新登录!", "");
			renderJson(msg);

		}
	}
	/**
	 * app修改存管用户手机号 2017.10.27 rain
	 */
	@ActionKey("/app_updateHfMobile")
	@AuthNum(value = 999)
	@Before({ AppInterceptor.class, PkMsgInterceptor.class })
	public void app_updateHfMobile() {
		String userCode = getUserCode();
		User user = User.userDao.findById(userCode);
		String loginId = user.getStr("loginId");
		
		if (!StringUtil.isBlank(loginId) ) {
			try {
				loginId = CommonUtil.decryptUserMobile(loginId);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ModifyMobileReqData modifyMobileReqData = new ModifyMobileReqData();
			modifyMobileReqData.setLogin_id(loginId);
			modifyMobileReqData.setMchnt_cd(FuiouController.MCHNT_CD);
			modifyMobileReqData.setMchnt_txn_ssn(CommonUtil.genMchntSsn());
			modifyMobileReqData.setPage_notify_url(CommonUtil.ADDRESS
					+ "/app_changeUserMobileNotice");
			try {
				FuiouService.app400101(modifyMobileReqData, getResponse());
			} catch (Exception e) {
				e.printStackTrace();
			}
			renderNull();
		}

	}
	// 更换手机号通知接口
		@ActionKey("/app_changeUserMobileNotice")
		@AuthNum(value = 999)
		public void app_changeUserMobileNotice() {
			String resp_code = getPara("resp_code");
			redirect("/appModifyHfMobileNotice?code="+resp_code, true);
		}
		
	/**
	 * 推荐注册专用--手机站
	 */
	@ActionKey("/mRecommendRegister")
	@Before(RegisterValidator.class)
	public void recommendRegister(){
		Message msg = null;
		
		//获取参数并验证
		String userMobile = getPara("userMobile");
		String loginPasswd = getPara("loginPasswd");
		String userName = getPara("userName");
		String uCheckCode = getPara("uCheckCode");//验证码
		String inputStyle = getPara("inviteCode"); //推荐人手机或邀请码
		
		if(StringUtil.isNumeric(userName)){
			msg = error("02", "用户昵称不能为纯数字", "");
			renderJson(msg);
			return;
		}
		
		//验证短信验证码
		if(CommonUtil.validateSMS("SMS_MSG_REGISTER_" + userMobile, uCheckCode) == false){
			msg = error("05", "短信验证码不正确", "");
		}else{
			//验证用户名是否被注册
			List<User> listUser = userService.find4userName(userName);
			if(null != listUser && listUser.size() > 0){
				msg = error("06", "用户昵称已经被注册", "");
				renderJson(msg);
				return;
			}
			
			//验证是否已经存在此手机号
			User user = userService.find4mobile(userMobile);
			if(null != user){
				msg = error("06", "手机号已经被注册", null);
			}else{
				String regUserCode = UIDUtil.generate();
				try {
					//添加
					String sysDesc = "用户自助注册"+DateUtil.getNowDateTime();
					boolean b = userService.save(regUserCode,userMobile, "", loginPasswd,userName, getRequestIP(), sysDesc);
					if(b == false){
						//记录日志
						BIZ_LOG_ERROR(userMobile, BIZ_TYPE.REGISTER, "注册失败",null);
						msg = error("07", "注册失败", "");
					}else{
						// 注册成功送518现金券
						ticketService.toReward4newUser(regUserCode);
						// 注册成功送可用积分
//						fundsServiceV2.doPoints(regUserCode, 0 , 1000, "注册送积分") ;
						
						//检查是否有推荐人
						User fuser = null;
						if(StringUtil.isBlank(inputStyle.trim()) == false){
							if(StringUtil.isNumeric(inputStyle.trim()) && (inputStyle.trim().length() == 11)){//手机号
								fuser = userService.find4mobile(inputStyle);
							}else{//邀请码
								fuser = userService.findByInviteCode(inputStyle);
							}
						}
						
						if( fuser != null ){
							//添加邀请记录
							RecommendInfo rmdInfo = new RecommendInfo();
							rmdInfo.set("aUserCode", fuser.getStr("userCode"));
							rmdInfo.set("aUserName", fuser.getStr("userName"));
							rmdInfo.set("bUserCode", regUserCode);
							rmdInfo.set("bUserName", userName);
							rmdInfo.set("bRegDate", DateUtil.getNowDate());
							rmdInfo.set("bRegTime", DateUtil.getNowTime());
							rmdInfo.set("rmdType", "");
							rmdInfo.set("rmdRemark", "好友推荐注册");
							rmdInfo.save();
						}
						//记录用户注册日期
						String encryptUserMobile = CommonUtil.encryptUserMobile(userMobile);
						BIZ_LOG_INFO(encryptUserMobile, BIZ_TYPE.REGISTER, getFrom() + "注册用户成功");
					}
				} catch (Exception e) {
					//记录日志
					BIZ_LOG_ERROR(userMobile, BIZ_TYPE.REGISTER, getFrom() + "注册失败",e);
					msg = error("08", "注册失败", "");
				}
				
				msg = succ("恭喜您,注册成功!", "");
			}
		}
		renderJson(msg);
	}
	
	
	
	/*--start--
	 * 风险评测    20180125   
	 */
	@ActionKey("/app_exposureRating")
	@AuthNum(value = 999)
	@Before({ AppInterceptor.class,PkMsgInterceptor.class })
	public void app_exposureRating(){
		String userCode = getUserCode();
		Message message = null;
		
		String age = getPara("age","");//年龄
		String familyIncome = getPara("familyIncome","");//家庭年收入
		String education = getPara("education","");//最高学历
		String investratio = getPara("investratio","");//投资比例
		String investexperience = getPara("investexperience","");//投资经验
		String investproduct = getPara("investproduct","");//投过的产品
		String investaim = getPara("investaim","");//投资目标
		String investlimit = getPara("investlimit","");//投资期限
		String compare = getPara("compare","");//投资比较
		String moodswing = getPara("moodswing","");//什么情况产生焦虑
		int sum = 2;//默认分数最低
			
		//测评分数
		Map<String,Integer> map = new HashMap<String,Integer>();
		   map.put("A",1);
		   map.put("B",2);
		   map.put("C",3);
		   map.put("D",4);
		   map.put("E",5);

		User user = userService.findById(userCode);

		String surveyResult = null;//评测结果 ABCD

		Map<String,String> result = new HashMap<String,String>();

		if(!compare.matches("[ABCD]")||!moodswing.matches("[ABCDE]")){
			message = error("01","请认证填写评测表！",null);
			renderJson(message);
			return;
		}
			
		sum = map.get(compare)+map.get(moodswing);//评测总分
			
			//评测结果 ：A为保守型   B为稳健型  C为进取型  D为冒险型
			if(sum<=3){
				surveyResult = "A";
			}else if(sum>3&&sum<=5){
				surveyResult = "B";
			}else if(sum>5&&sum<=7){
				surveyResult = "C";
			}else{
				surveyResult = "D";
			}

			//将评测答案 以json字符串保存
			JSONObject recordObj = new JSONObject();
			recordObj.put("age",age);
			recordObj.put("familyIncome",familyIncome);
			recordObj.put("education",education);
			recordObj.put("investratio",investratio);
			recordObj.put("investexperience",investexperience);
			recordObj.put("investproduct",investproduct);
			recordObj.put("investaim",investaim);
			recordObj.put("investlimit",investlimit);
			recordObj.put("compare",compare);
			recordObj.put("moodswing",moodswing);
			
			//保存评测表
			QuestionnaireRecords questionnaireRecords = new QuestionnaireRecords();
			questionnaireRecords.set("userCode",userCode);
			questionnaireRecords.set("userName",user.getStr("userName"));
			questionnaireRecords.set("surveyRecord", recordObj.toJSONString());
			questionnaireRecords.set("surveyResult",surveyResult);
			questionnaireRecords.set("addDateTime", DateUtil.getNowDateTime());

			if(questionnaireRecords.save()){
					boolean updateSurvey = userService.updateSurvey(userCode, surveyResult);//更新user评测结果
					if(!updateSurvey){
						message = error("01", "评测结果更新失败", null);
						renderJson(message);
						return;
					}
				
					result.put("result", user.getEvaluateResult());
			}else{
				message = error("01", "评测结果更新失败", null);
				renderJson(message);
				return;
			}
			
		message = succ("评测成功", result);
		renderJson(message);
		return;
	}
	
	/*
	 * 查询评测结果
	 */
	@ActionKey("/app_searchEvaluateResult")
	@AuthNum(value = 999)
	@Before({ AppInterceptor.class,PkMsgInterceptor.class })
	public void app_searchEvaluateResult(){
		
		String userCode = getUserCode();
		Message message = null;
		User user = userService.findById(userCode);

		String evaluation = user.getEvaluateResult();
		message = succ("查询成功", evaluation);
		renderJson(message);
		return;
	}
	
	//--end--
	@ActionKey("/app_VIPCenterPage")
	@AuthNum(value = 999)
	@Before({ AppInterceptor.class,PkMsgInterceptor.class })
	public Message app_VIPCenterPage(){
		String userCode = getUserCode();
		if( userCode == null){
			return error("01", "用户未登录", false);
		}
		User user = userService.findById(userCode);
		if(user == null){
			return error("02", "未查找到用户信息", false);
		}
		Map<String, Object> resultMap = new HashMap<String, Object>();
		//当前会员名
		String vipLevelName = user.getStr("vipLevelName");
		int vipLevel = user.getInt("vipLevel");
		if(vipLevelName == null ){
			return error("03", "未查找到会员信息", null);
		}
		//实时待收
		long nowBeRecyAmount = 0l;
		//实时等级待收上限
		long limitAmount = 0l;
		//与一下等级的差距
		long leftAmount = 0l;
		Funds funds = null;

		try {
			funds = fundsServiceV2.findById(userCode);
			nowBeRecyAmount = funds.getLong("beRecyPrincipal") + funds.getLong("beRecyInterest");
			//nowBeRecyAmount = loanTraceService.queryNowBeRecyByUserCode(userCode);
			//实时会员等级
			VipV2 vipV2 = VipV2.getVipBybeRecyAmount(nowBeRecyAmount);
			if(vipV2.getVipLevel() != VipV2.VIPS.size()){//非至尊级别
				limitAmount = vipV2.getVipMaxAmount();
				leftAmount = limitAmount - nowBeRecyAmount;
			}
		} catch (Exception e) {
			return error("04", "实时待收加载中", null);
		}
		resultMap.put("vipLevelName", vipLevelName);
		resultMap.put("vipLevel", vipLevel);
		resultMap.put("nowBeRecyAmount", Number.longToString(nowBeRecyAmount));
		resultMap.put("leftAmount", Number.longToString(leftAmount));
		
		return succ("查询成功", resultMap);
	}
	
	/**
	 * 查询存管账户信息
	 */
	@ActionKey("/app_queryDepositAccount")
	@AuthNum(value = 999)
	@Before({AppInterceptor.class,PkMsgInterceptor.class})
	public void queryDepositAccount(){
		Message msg = new Message();
		String userCode = getUserCode();
		User user = userService.findById(userCode);
		if(user == null){
			msg = error("01", "用户未登录", "");
			renderJson(msg);
			return ;
		}
		String accountId = user.getStr("jxAccountId");
		if(StringUtil.isBlank(accountId)){
			msg = error("02", "未开通存管账户", "");
			renderJson(msg);
			return ;
		}
		UserInfo userInfo = userInfoService.findById(userCode);
		String trueName = userInfo.getStr("userCardName");
		if(StringUtil.isBlank(trueName)){
			msg = error("03", "实名信息不全", "");
			renderJson(msg);
			return ;
		}
		String cardId = userInfo.getStr("userCardId");
		try {
			cardId = CommonUtil.decryptUserCardId(cardId);
		} catch (Exception e) {
			msg = error("04", "身份信息解析异常", "");
			renderJson(msg);
			return ;
		}
		cardId = cardId.substring(0, 6) + "******" + cardId.substring(cardId.length()-4, cardId.length());
		Map<String, String> map = new HashMap<>();
		map.put("accountId", accountId);
		map.put("trueName", trueName);
		map.put("cardId", cardId);
		msg = succ("查询成功", map);
		renderJson(msg);
	}
}
