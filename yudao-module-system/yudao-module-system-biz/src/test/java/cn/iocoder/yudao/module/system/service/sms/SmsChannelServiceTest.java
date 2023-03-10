package cn.iocoder.yudao.module.system.service.sms;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.sms.core.client.SmsClientFactory;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.channel.SmsChannelCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.channel.SmsChannelPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.channel.SmsChannelUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsChannelDO;
import cn.iocoder.yudao.module.system.dal.mysql.sms.SmsChannelMapper;
import cn.iocoder.yudao.module.system.mq.producer.sms.SmsProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.*;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SMS_CHANNEL_HAS_CHILDREN;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SMS_CHANNEL_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(SmsChannelServiceImpl.class)
public class SmsChannelServiceTest extends BaseDbUnitTest {

    @Resource
    private SmsChannelServiceImpl smsChannelService;

    @Resource
    private SmsChannelMapper smsChannelMapper;

    @MockBean
    private SmsClientFactory smsClientFactory;
    @MockBean
    private SmsTemplateService smsTemplateService;
    @MockBean
    private SmsProducer smsProducer;

    @Test
    public void testInitLocalCache_success() {
        // mock ??????
        SmsChannelDO smsChannelDO01 = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(smsChannelDO01);
        SmsChannelDO smsChannelDO02 = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(smsChannelDO02);

        // ??????
        smsChannelService.initLocalCache();
        // ????????????
        verify(smsClientFactory, times(1)).createOrUpdateSmsClient(
                argThat(properties -> isPojoEquals(smsChannelDO01, properties)));
        verify(smsClientFactory, times(1)).createOrUpdateSmsClient(
                argThat(properties -> isPojoEquals(smsChannelDO02, properties)));
    }

    @Test
    public void testCreateSmsChannel_success() {
        // ????????????
        SmsChannelCreateReqVO reqVO = randomPojo(SmsChannelCreateReqVO.class, o -> o.setStatus(randomCommonStatus()));

        // ??????
        Long smsChannelId = smsChannelService.createSmsChannel(reqVO);
        // ??????
        assertNotNull(smsChannelId);
        // ?????????????????????????????????
        SmsChannelDO smsChannel = smsChannelMapper.selectById(smsChannelId);
        assertPojoEquals(reqVO, smsChannel);
        // ????????????
        verify(smsProducer, times(1)).sendSmsChannelRefreshMessage();
    }

    @Test
    public void testUpdateSmsChannel_success() {
        // mock ??????
        SmsChannelDO dbSmsChannel = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel);// @Sql: ?????????????????????????????????
        // ????????????
        SmsChannelUpdateReqVO reqVO = randomPojo(SmsChannelUpdateReqVO.class, o -> {
            o.setId(dbSmsChannel.getId()); // ??????????????? ID
            o.setStatus(randomCommonStatus());
            o.setCallbackUrl(randomString());
        });

        // ??????
        smsChannelService.updateSmsChannel(reqVO);
        // ????????????????????????
        SmsChannelDO smsChannel = smsChannelMapper.selectById(reqVO.getId()); // ???????????????
        assertPojoEquals(reqVO, smsChannel);
        // ????????????
        verify(smsProducer, times(1)).sendSmsChannelRefreshMessage();
    }

    @Test
    public void testUpdateSmsChannel_notExists() {
        // ????????????
        SmsChannelUpdateReqVO reqVO = randomPojo(SmsChannelUpdateReqVO.class);

        // ??????, ???????????????
        assertServiceException(() -> smsChannelService.updateSmsChannel(reqVO), SMS_CHANNEL_NOT_EXISTS);
    }

    @Test
    public void testDeleteSmsChannel_success() {
        // mock ??????
        SmsChannelDO dbSmsChannel = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel);// @Sql: ?????????????????????????????????
        // ????????????
        Long id = dbSmsChannel.getId();

        // ??????
        smsChannelService.deleteSmsChannel(id);
       // ????????????????????????
       assertNull(smsChannelMapper.selectById(id));
        // ????????????
        verify(smsProducer, times(1)).sendSmsChannelRefreshMessage();
    }

    @Test
    public void testDeleteSmsChannel_notExists() {
        // ????????????
        Long id = randomLongId();

        // ??????, ???????????????
        assertServiceException(() -> smsChannelService.deleteSmsChannel(id), SMS_CHANNEL_NOT_EXISTS);
    }

    @Test
    public void testDeleteSmsChannel_hasChildren() {
        // mock ??????
        SmsChannelDO dbSmsChannel = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel);// @Sql: ?????????????????????????????????
        // ????????????
        Long id = dbSmsChannel.getId();
        // mock ??????
        when(smsTemplateService.countByChannelId(eq(id))).thenReturn(10L);

        // ??????, ???????????????
        assertServiceException(() -> smsChannelService.deleteSmsChannel(id), SMS_CHANNEL_HAS_CHILDREN);
    }

    @Test
    public void testGetSmsChannel() {
        // mock ??????
        SmsChannelDO dbSmsChannel = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel); // @Sql: ?????????????????????????????????
        // ????????????
        Long id = dbSmsChannel.getId();

        // ??????????????????
        assertPojoEquals(dbSmsChannel, smsChannelService.getSmsChannel(id));
    }

    @Test
    public void testGetSmsChannelList() {
        // mock ??????
        SmsChannelDO dbSmsChannel01 = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel01);
        SmsChannelDO dbSmsChannel02 = randomPojo(SmsChannelDO.class);
        smsChannelMapper.insert(dbSmsChannel02);
        // ????????????

        // ??????
        List<SmsChannelDO> list = smsChannelService.getSmsChannelList();
        // ??????
        assertEquals(2, list.size());
        assertPojoEquals(dbSmsChannel01, list.get(0));
        assertPojoEquals(dbSmsChannel02, list.get(1));
    }

    @Test
    public void testGetSmsChannelPage() {
       // mock ??????
       SmsChannelDO dbSmsChannel = randomPojo(SmsChannelDO.class, o -> { // ???????????????
           o.setSignature("????????????");
           o.setStatus(CommonStatusEnum.ENABLE.getStatus());
           o.setCreateTime(buildTime(2020, 12, 12));
       });
       smsChannelMapper.insert(dbSmsChannel);
       // ?????? signature ?????????
       smsChannelMapper.insert(cloneIgnoreId(dbSmsChannel, o -> o.setSignature("??????")));
       // ?????? status ?????????
       smsChannelMapper.insert(cloneIgnoreId(dbSmsChannel, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
       // ?????? createTime ?????????
       smsChannelMapper.insert(cloneIgnoreId(dbSmsChannel, o -> o.setCreateTime(buildTime(2020, 11, 11))));
       // ????????????
       SmsChannelPageReqVO reqVO = new SmsChannelPageReqVO();
       reqVO.setSignature("??????");
       reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
       reqVO.setCreateTime(buildBetweenTime(2020, 12, 1, 2020, 12, 24));

       // ??????
       PageResult<SmsChannelDO> pageResult = smsChannelService.getSmsChannelPage(reqVO);
       // ??????
       assertEquals(1, pageResult.getTotal());
       assertEquals(1, pageResult.getList().size());
       assertPojoEquals(dbSmsChannel, pageResult.getList().get(0));
    }

}
