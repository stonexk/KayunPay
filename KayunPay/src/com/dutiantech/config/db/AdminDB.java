package com.dutiantech.config.db;

import com.dutiantech.model.ContractTemplate;
import com.dutiantech.model.HistoryRecy;
import com.dutiantech.model.MenuV2;
import com.dutiantech.model.OPUserV2;
import com.dutiantech.model.PlatformCount;
import com.dutiantech.model.RepaymentCount;
import com.dutiantech.model.RoleV2;
import com.dutiantech.model.SettlementFunds;
import com.dutiantech.model.SettlementPlan;
import com.dutiantech.model.ShopInformation;
import com.dutiantech.model.TmpUserReg;
import com.dutiantech.model.TransferUser;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;

public class AdminDB {
	
	public static void addMapping( ActiveRecordPlugin arp ){
		arp.addMapping("t_op_user_v2", "op_code",OPUserV2.class);
		arp.addMapping("t_menu_v2", "menu_id", MenuV2.class);
		arp.addMapping("t_role_v2", "role_code",RoleV2.class);
		arp.addMapping("t_history_recy", "recyCode",HistoryRecy.class);
		arp.addMapping("t_settlement_plan","uid", SettlementPlan.class);
		arp.addMapping("t_platform_count","countDate", PlatformCount.class);
		arp.addMapping("t_settlement_funds", "seCode",SettlementFunds.class);
		arp.addMapping("t_tmp_userReg", "userCode",TmpUserReg.class);
		arp.addMapping("t_contract_template","templateCode", ContractTemplate.class);
		arp.addMapping("t_transfer_user", "transferUserNo", TransferUser.class);
		arp.addMapping("t_shop_information","shopId", ShopInformation.class);
		arp.addMapping("t_repayment_count", "uid", RepaymentCount.class);	//回款统计表
	}
	
}