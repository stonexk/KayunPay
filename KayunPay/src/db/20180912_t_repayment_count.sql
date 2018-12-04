CREATE TABLE `t_repayment_count` (
  `repaymentDate` char(8) NOT NULL COMMENT '原定结算日期',
  `repaymentYesDate` char(8) DEFAULT NULL COMMENT '实际结算日期',
  `uid` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增列',
  `pcdcyhzbj` bigint(20) DEFAULT '0' COMMENT '批次代偿应还总本金',
  `pcdcyhzlx` bigint(20) DEFAULT '0' COMMENT '批次代偿应还总利息',
  `hbyhzbj` bigint(20) DEFAULT '0' COMMENT '红包应还总本金',
  `hbyhzlx` bigint(20) DEFAULT '0' COMMENT '红包应还总利息',
  `yhzbj` bigint(20) DEFAULT '0' COMMENT '应还总本金',
  `yhzlx` bigint(20) DEFAULT '0' COMMENT '应还总利息',
  `sjzchkzbj` bigint(20) DEFAULT '0' COMMENT '实际正常还款总本金',
  `sjzchkzlx` bigint(20) DEFAULT '0' COMMENT '实际正常还款总利息',
  `yqhkzbj` bigint(20) DEFAULT '0' COMMENT '逾期还款总本金',
  `yqdfzlx` bigint(20) DEFAULT '0' COMMENT '逾期垫付总利息',
  `yhzchkbds` int(11) DEFAULT '0' COMMENT '应还正常还款标的数',
  `whzchkbds` int(11) DEFAULT '0' COMMENT '未还正常还款标的数',
  `yhtqhkbds` int(11) DEFAULT '0' COMMENT '应还提前还款标的数',
  `whtqhkbds` int(11) DEFAULT '0' COMMENT '未还提前还款标的数',
  `sjzchkbds` int(11) DEFAULT '0' COMMENT '实际正常还款标的数',
  `whsjzchkbds` int(11) DEFAULT '0' COMMENT '未还实际正常还款标的数',
  `sjtqhkbds` int(11) DEFAULT '0' COMMENT '实际提前还款标的数',
  `whsjtqhkbds` int(11) DEFAULT '0' COMMENT '未还实际提前还款标的数',
  `yqhbjbds` int(11) DEFAULT '0' COMMENT '逾期还本金标的数',
  `whyqbjbds` int(11) DEFAULT '0' COMMENT '未还逾期本金标的数',
  `yqdflxbds` int(11) DEFAULT '0' COMMENT '逾期垫付利息标的数',
  `whyqdflxbds` int(11) DEFAULT '0' COMMENT '未还逾期垫付利息标的数',
  `fjsrhkbds` int(11) DEFAULT '0' COMMENT '非结算日还款标的数',
  `sfhbsl` int(11) DEFAULT '0' COMMENT '实发红包数量',
  `sbhbsl` int(11) DEFAULT '0' COMMENT '失败红包数量',
  `yclsbhbsl` int(11) DEFAULT '0' COMMENT '已处理失败红包数量',
  `yfzchkpcs` int(11) DEFAULT '0' COMMENT '应发正常还款批次数(含提前还款)',
  `yfyqbjpcs` int(11) DEFAULT '0' COMMENT '应发逾期本金批次数',
  `yfyqdflxpcs` int(11) DEFAULT '0' COMMENT '应发逾期垫付利息批次数',
  `pcdcshzbj` bigint(20) DEFAULT '0' COMMENT '批次代偿实还总本金',
  `pcdcshzlx` bigint(20) DEFAULT '0' COMMENT '批次代偿实还总利息',
  `pcdcshzsxf` bigint(20) DEFAULT '0' COMMENT '批次代偿实收总手续费',
  `hbshzbj` bigint(20) DEFAULT '0' COMMENT '红包应还总本金',
  `hbshzlx` bigint(20) DEFAULT '0' COMMENT '红包实还总利息(利息-手续费)',
  `hbfysfze` bigint(20) DEFAULT '0' COMMENT '红包返佣实发总额',
  `wkhhkze` bigint(20) DEFAULT '0' COMMENT '未开户还款总额(本金+利息+佣金)',
  `qtyhzbj` bigint(20) DEFAULT '0' COMMENT '齐通应还总本金',
  `qtyhzlx` bigint(20) DEFAULT '0' COMMENT '齐通应还总利息',
  `qtsjzchkzbj` bigint(20) DEFAULT '0' COMMENT '齐通实际正常还款总本金',
  `qtsjzchkzlx` bigint(20) DEFAULT '0' COMMENT '齐通实际正常还款总利息',
  `qtyqdfzlx` bigint(20) DEFAULT '0' COMMENT '齐通逾期垫付总利息',
  `qtyhkbds` int(20) DEFAULT '0' COMMENT '齐通应还款标的数',
  `qtsjhkbds` int(20) DEFAULT '0' COMMENT '齐通实际还款标的数',
  `sfzchkpcs` int(11) DEFAULT '0' COMMENT '实发正常还款批次数(含提前还款)',
  `sfyqbjpcs` int(11) DEFAULT '0' COMMENT '实发逾期本金批次数',
  `sfyqdflxpcs` int(11) DEFAULT '0' COMMENT '实发逾期垫付利息批次数',
  `pczjwjdsl` int(11) DEFAULT '0' COMMENT '批次资金未解冻数量',
  `pczjyjdsl` int(11) DEFAULT '0' COMMENT '批次资金已解冻数量',
  `sbpcsl` int(11) DEFAULT '0' COMMENT '失败批次数量',
  `yclsbpcsl` int(11) DEFAULT '0' COMMENT '已处理失败批次数量',
  `batchRecord` text COMMENT '批次记录json:type(批次类型 1:正常还款,2:逾期本金还款,3:逾期垫付利息还款,4:补发),status(批次状态 0:未解冻,1:已解冻,2:全部失败,3:部分失败,4:失败已处理)',
  `addDate` char(8) DEFAULT NULL COMMENT '添加日期',
  `addTime` char(6) DEFAULT NULL COMMENT '添加时间',
  `updateDate` char(8) DEFAULT NULL COMMENT '更新日期',
  `updateTime` char(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`uid`),
  KEY `t_repayment_count_repaymentDate` (`repaymentDate`) USING BTREE,
  KEY `t_repayment_count_repaymentYesDate` (`repaymentYesDate`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
