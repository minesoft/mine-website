package cn.iocoder.yudao.module.system.service.sms;

import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.log.SmsLogExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.log.SmsLogPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsLogDO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.sms.SmsLogMapper;
import cn.iocoder.yudao.module.system.enums.sms.SmsReceiveStatusEnum;
import cn.iocoder.yudao.module.system.enums.sms.SmsSendStatusEnum;
import cn.iocoder.yudao.module.system.enums.sms.SmsTemplateTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomBoolean;
import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(SmsLogServiceImpl.class)
public class SmsLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SmsLogServiceImpl smsLogService;

    @Resource
    private SmsLogMapper smsLogMapper;

    @Test
    public void testGetSmsLogPage() {
       // mock ??????
       SmsLogDO dbSmsLog = randomSmsLogDO(o -> { // ???????????????
           o.setChannelId(1L);
           o.setTemplateId(10L);
           o.setMobile("15601691300");
           o.setSendStatus(SmsSendStatusEnum.INIT.getStatus());
           o.setSendTime(buildTime(2020, 11, 11));
           o.setReceiveStatus(SmsReceiveStatusEnum.INIT.getStatus());
           o.setReceiveTime(buildTime(2021, 11, 11));
       });
       smsLogMapper.insert(dbSmsLog);
       // ?????? channelId ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setChannelId(2L)));
       // ?????? templateId ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setTemplateId(20L)));
       // ?????? mobile ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setMobile("18818260999")));
       // ?????? sendStatus ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setSendStatus(SmsSendStatusEnum.IGNORE.getStatus())));
       // ?????? sendTime ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setSendTime(buildTime(2020, 12, 12))));
       // ?????? receiveStatus ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setReceiveStatus(SmsReceiveStatusEnum.SUCCESS.getStatus())));
       // ?????? receiveTime ?????????
       smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setReceiveTime(buildTime(2021, 12, 12))));
       // ????????????
       SmsLogPageReqVO reqVO = new SmsLogPageReqVO();
       reqVO.setChannelId(1L);
       reqVO.setTemplateId(10L);
       reqVO.setMobile("156");
       reqVO.setSendStatus(SmsSendStatusEnum.INIT.getStatus());
       reqVO.setSendTime(buildBetweenTime(2020, 11, 1, 2020, 11, 30));
       reqVO.setReceiveStatus(SmsReceiveStatusEnum.INIT.getStatus());
       reqVO.setReceiveTime(buildBetweenTime(2021, 11, 1, 2021, 11, 30));

       // ??????
       PageResult<SmsLogDO> pageResult = smsLogService.getSmsLogPage(reqVO);
       // ??????
       assertEquals(1, pageResult.getTotal());
       assertEquals(1, pageResult.getList().size());
       assertPojoEquals(dbSmsLog, pageResult.getList().get(0));
    }

    @Test
    public void testGetSmsLogList() {
        // mock ??????
        SmsLogDO dbSmsLog = randomSmsLogDO(o -> { // ???????????????
            o.setChannelId(1L);
            o.setTemplateId(10L);
            o.setMobile("15601691300");
            o.setSendStatus(SmsSendStatusEnum.INIT.getStatus());
            o.setSendTime(buildTime(2020, 11, 11));
            o.setReceiveStatus(SmsReceiveStatusEnum.INIT.getStatus());
            o.setReceiveTime(buildTime(2021, 11, 11));
        });
        smsLogMapper.insert(dbSmsLog);
        // ?????? channelId ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setChannelId(2L)));
        // ?????? templateId ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setTemplateId(20L)));
        // ?????? mobile ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setMobile("18818260999")));
        // ?????? sendStatus ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setSendStatus(SmsSendStatusEnum.IGNORE.getStatus())));
        // ?????? sendTime ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setSendTime(buildTime(2020, 12, 12))));
        // ?????? receiveStatus ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setReceiveStatus(SmsReceiveStatusEnum.SUCCESS.getStatus())));
        // ?????? receiveTime ?????????
        smsLogMapper.insert(cloneIgnoreId(dbSmsLog, o -> o.setReceiveTime(buildTime(2021, 12, 12))));
        // ????????????
        SmsLogExportReqVO reqVO = new SmsLogExportReqVO();
        reqVO.setChannelId(1L);
        reqVO.setTemplateId(10L);
        reqVO.setMobile("156");
        reqVO.setSendStatus(SmsSendStatusEnum.INIT.getStatus());
        reqVO.setSendTime(buildBetweenTime(2020, 11, 1, 2020, 11, 30));
        reqVO.setReceiveStatus(SmsReceiveStatusEnum.INIT.getStatus());
        reqVO.setReceiveTime(buildBetweenTime(2021, 11, 1, 2021, 11, 30));

       // ??????
       List<SmsLogDO> list = smsLogService.getSmsLogList(reqVO);
       // ??????
       assertEquals(1, list.size());
       assertPojoEquals(dbSmsLog, list.get(0));
    }

    @Test
    public void testCreateSmsLog() {
        // ????????????
        String mobile = randomString();
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        Boolean isSend = randomBoolean();
        SmsTemplateDO templateDO = randomPojo(SmsTemplateDO.class,
                o -> o.setType(randomEle(SmsTemplateTypeEnum.values()).getType()));
        String templateContent = randomString();
        Map<String, Object> templateParams = randomTemplateParams();
        // mock ??????

        // ??????
        Long logId = smsLogService.createSmsLog(mobile, userId, userType, isSend,
                templateDO, templateContent, templateParams);
        // ??????
        SmsLogDO logDO = smsLogMapper.selectById(logId);
        assertEquals(isSend ? SmsSendStatusEnum.INIT.getStatus() : SmsSendStatusEnum.IGNORE.getStatus(),
                logDO.getSendStatus());
        assertEquals(mobile, logDO.getMobile());
        assertEquals(userType, logDO.getUserType());
        assertEquals(userId, logDO.getUserId());
        assertEquals(templateDO.getId(), logDO.getTemplateId());
        assertEquals(templateDO.getCode(), logDO.getTemplateCode());
        assertEquals(templateDO.getType(), logDO.getTemplateType());
        assertEquals(templateDO.getChannelId(), logDO.getChannelId());
        assertEquals(templateDO.getChannelCode(), logDO.getChannelCode());
        assertEquals(templateContent, logDO.getTemplateContent());
        assertEquals(templateParams, logDO.getTemplateParams());
        assertEquals(SmsReceiveStatusEnum.INIT.getStatus(), logDO.getReceiveStatus());
    }

    @Test
    public void testUpdateSmsSendResult() {
        // mock ??????
        SmsLogDO dbSmsLog = randomSmsLogDO(
                o -> o.setSendStatus(SmsSendStatusEnum.IGNORE.getStatus()));
        smsLogMapper.insert(dbSmsLog);
        // ????????????
        Long id = dbSmsLog.getId();
        Integer sendCode = randomInteger();
        String sendMsg = randomString();
        String apiSendCode = randomString();
        String apiSendMsg = randomString();
        String apiRequestId = randomString();
        String apiSerialNo = randomString();

        // ??????
        smsLogService.updateSmsSendResult(id, sendCode, sendMsg,
                apiSendCode, apiSendMsg, apiRequestId, apiSerialNo);
        // ??????
        dbSmsLog = smsLogMapper.selectById(id);
        assertEquals(CommonResult.isSuccess(sendCode) ? SmsSendStatusEnum.SUCCESS.getStatus()
                : SmsSendStatusEnum.FAILURE.getStatus(), dbSmsLog.getSendStatus());
        assertNotNull(dbSmsLog.getSendTime());
        assertEquals(sendMsg, dbSmsLog.getSendMsg());
        assertEquals(apiSendCode, dbSmsLog.getApiSendCode());
        assertEquals(apiSendMsg, dbSmsLog.getApiSendMsg());
        assertEquals(apiRequestId, dbSmsLog.getApiRequestId());
        assertEquals(apiSerialNo, dbSmsLog.getApiSerialNo());
    }

    @Test
    public void testUpdateSmsReceiveResult() {
        // mock ??????
        SmsLogDO dbSmsLog = randomSmsLogDO(
                o -> o.setReceiveStatus(SmsReceiveStatusEnum.INIT.getStatus()));
        smsLogMapper.insert(dbSmsLog);
        // ????????????
        Long id = dbSmsLog.getId();
        Boolean success = randomBoolean();
        LocalDateTime receiveTime = randomLocalDateTime();
        String apiReceiveCode = randomString();
        String apiReceiveMsg = randomString();

        // ??????
        smsLogService.updateSmsReceiveResult(id, success, receiveTime, apiReceiveCode, apiReceiveMsg);
        // ??????
        dbSmsLog = smsLogMapper.selectById(id);
        assertEquals(success ? SmsReceiveStatusEnum.SUCCESS.getStatus()
                : SmsReceiveStatusEnum.FAILURE.getStatus(), dbSmsLog.getReceiveStatus());
        assertEquals(receiveTime, dbSmsLog.getReceiveTime());
        assertEquals(apiReceiveCode, dbSmsLog.getApiReceiveCode());
        assertEquals(apiReceiveMsg, dbSmsLog.getApiReceiveMsg());
    }

    // ========== ???????????? ==========

    @SafeVarargs
    private static SmsLogDO randomSmsLogDO(Consumer<SmsLogDO>... consumers) {
        Consumer<SmsLogDO> consumer = (o) -> {
            o.setTemplateParams(randomTemplateParams());
            o.setTemplateType(randomEle(SmsTemplateTypeEnum.values()).getType()); // ?????? templateType ?????????
            o.setUserType(randomEle(UserTypeEnum.values()).getValue()); // ?????? userType ?????????
            o.setSendStatus(randomEle(SmsSendStatusEnum.values()).getStatus()); // ?????? sendStatus ?????????
            o.setReceiveStatus(randomEle(SmsReceiveStatusEnum.values()).getStatus()); // ?????? receiveStatus ?????????
        };
        return randomPojo(SmsLogDO.class, ArrayUtils.append(consumer, consumers));
    }

    private static Map<String, Object> randomTemplateParams() {
        return MapUtil.<String, Object>builder().put(randomString(), randomString())
                .put(randomString(), randomString()).build();
    }
}
