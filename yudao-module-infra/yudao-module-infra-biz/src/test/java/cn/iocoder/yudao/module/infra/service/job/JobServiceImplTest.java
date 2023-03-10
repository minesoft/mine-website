package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobExportReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobUpdateReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Import(JobServiceImpl.class)
public class JobServiceImplTest extends BaseDbUnitTest {

    @Resource
    private JobServiceImpl jobService;
    @Resource
    private JobMapper jobMapper;
    @MockBean
    private SchedulerManager schedulerManager;

    @Test
    public void testCreateJob_cronExpressionValid() {
        // ???????????????Cron ???????????? String ?????????????????????????????????
        JobCreateReqVO reqVO = randomPojo(JobCreateReqVO.class);

        // ????????????????????????
        assertServiceException(() -> jobService.createJob(reqVO), JOB_CRON_EXPRESSION_VALID);
    }

    @Test
    public void testCreateJob_jobHandlerExists() throws SchedulerException {
        // ???????????? ?????? Cron ?????????
        JobCreateReqVO reqVO = randomPojo(JobCreateReqVO.class, o -> o.setCronExpression("0 0/1 * * * ? *"));

        // ??????
        jobService.createJob(reqVO);
        // ????????????????????????
        assertServiceException(() -> jobService.createJob(reqVO), JOB_HANDLER_EXISTS);
    }

    @Test
    public void testCreateJob_success() throws SchedulerException {
        // ???????????? ?????? Cron ?????????
        JobCreateReqVO reqVO = randomPojo(JobCreateReqVO.class, o -> o.setCronExpression("0 0/1 * * * ? *"));

        // ??????
        Long jobId = jobService.createJob(reqVO);
        // ??????
        assertNotNull(jobId);
        // ?????????????????????????????????
        JobDO job = jobMapper.selectById(jobId);
        assertPojoEquals(reqVO, job);
        assertEquals(JobStatusEnum.NORMAL.getStatus(), job.getStatus());
        // ????????????
        verify(schedulerManager).addJob(eq(job.getId()), eq(job.getHandlerName()), eq(job.getHandlerParam()),
                eq(job.getCronExpression()), eq(reqVO.getRetryCount()), eq(reqVO.getRetryInterval()));
    }

    @Test
    public void testUpdateJob_jobNotExists(){
        // ????????????
        JobUpdateReqVO reqVO = randomPojo(JobUpdateReqVO.class, o -> o.setCronExpression("0 0/1 * * * ? *"));

        // ????????????????????????
        assertServiceException(() -> jobService.updateJob(reqVO), JOB_NOT_EXISTS);
    }

    @Test
    public void testUpdateJob_onlyNormalStatus(){
        // mock ??????
        JobDO job = randomPojo(JobDO.class, o -> o.setStatus(JobStatusEnum.INIT.getStatus()));
        jobMapper.insert(job);
        // ????????????
        JobUpdateReqVO updateReqVO = randomPojo(JobUpdateReqVO.class, o -> {
            o.setId(job.getId());
            o.setCronExpression("0 0/1 * * * ? *");
        });

        // ????????????????????????
        assertServiceException(() -> jobService.updateJob(updateReqVO),
                JOB_UPDATE_ONLY_NORMAL_STATUS);
    }

    @Test
    public void testUpdateJob_success() throws SchedulerException {
        // mock ??????
        JobDO job = randomPojo(JobDO.class, o -> o.setStatus(JobStatusEnum.NORMAL.getStatus()));
        jobMapper.insert(job);
        // ????????????
        JobUpdateReqVO updateReqVO = randomPojo(JobUpdateReqVO.class, o -> {
            o.setId(job.getId());
            o.setCronExpression("0 0/1 * * * ? *");
        });

        // ??????
        jobService.updateJob(updateReqVO);
        // ?????????????????????????????????
        JobDO updateJob = jobMapper.selectById(updateReqVO.getId());
        assertPojoEquals(updateReqVO, updateJob);
        // ????????????
        verify(schedulerManager).updateJob(eq(job.getHandlerName()), eq(updateReqVO.getHandlerParam()),
                eq(updateReqVO.getCronExpression()), eq(updateReqVO.getRetryCount()), eq(updateReqVO.getRetryInterval()));
    }

    @Test
    public void testUpdateJobStatus_changeStatusInvalid() {
        // ????????????????????????
        assertServiceException(() -> jobService.updateJobStatus(1L, JobStatusEnum.INIT.getStatus()),
                JOB_CHANGE_STATUS_INVALID);
    }

    @Test
    public void testUpdateJobStatus_changeStatusEquals() {
        // mock ??????
        JobDO job = randomPojo(JobDO.class, o -> o.setStatus(JobStatusEnum.NORMAL.getStatus()));
        jobMapper.insert(job);

        // ????????????????????????
        assertServiceException(() -> jobService.updateJobStatus(job.getId(), job.getStatus()),
                JOB_CHANGE_STATUS_EQUALS);
    }

    @Test
    public void testUpdateJobStatus_stopSuccess() throws SchedulerException {
        // mock ??????
        JobDO job = randomPojo(JobDO.class, o -> o.setStatus(JobStatusEnum.NORMAL.getStatus()));
        jobMapper.insert(job);

        // ??????
        jobService.updateJobStatus(job.getId(), JobStatusEnum.STOP.getStatus());
        // ?????????????????????????????????
        JobDO dbJob = jobMapper.selectById(job.getId());
        assertEquals(JobStatusEnum.STOP.getStatus(), dbJob.getStatus());
        // ????????????
        verify(schedulerManager).pauseJob(eq(job.getHandlerName()));
    }

    @Test
    public void testUpdateJobStatus_normalSuccess() throws SchedulerException {
        // mock ??????
        JobDO job = randomPojo(JobDO.class, o -> o.setStatus(JobStatusEnum.STOP.getStatus()));
        jobMapper.insert(job);

        // ??????
        jobService.updateJobStatus(job.getId(), JobStatusEnum.NORMAL.getStatus());
        // ?????????????????????????????????
        JobDO dbJob = jobMapper.selectById(job.getId());
        assertEquals(JobStatusEnum.NORMAL.getStatus(), dbJob.getStatus());
        // ????????????
        verify(schedulerManager).resumeJob(eq(job.getHandlerName()));
    }

    @Test
    public void testTriggerJob_success() throws SchedulerException {
        // mock ??????
        JobDO job = randomPojo(JobDO.class);
        jobMapper.insert(job);

        // ??????
        jobService.triggerJob(job.getId());
        // ????????????
        verify(schedulerManager).triggerJob(eq(job.getId()),
                eq(job.getHandlerName()), eq(job.getHandlerParam()));
    }

    @Test
    public void testDeleteJob_success() throws SchedulerException {
        // mock ??????
        JobDO job = randomPojo(JobDO.class);
        jobMapper.insert(job);

        // ??????
        jobService.deleteJob(job.getId());
        // ???????????????
        assertNull(jobMapper.selectById(job.getId()));
        // ????????????
        verify(schedulerManager).deleteJob(eq(job.getHandlerName()));
    }

    @Test
    public void testGetJobList() {
        // mock ??????
        JobDO dbJob = randomPojo(JobDO.class, o -> {
            o.setStatus(randomEle(JobStatusEnum.values()).getStatus()); // ?????? status ?????????
        });
        jobMapper.insert(dbJob);
        // ?????? id ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> {}));

        // ????????????
        Collection<Long> ids = singletonList(dbJob.getId());
        // ??????
        List<JobDO> list = jobService.getJobList(ids);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbJob, list.get(0));
    }

    @Test
    public void testGetJobPage() {
        // mock ??????
        JobDO dbJob = randomPojo(JobDO.class, o -> {
            o.setName("??????????????????");
            o.setHandlerName("handlerName ????????????");
            o.setStatus(JobStatusEnum.INIT.getStatus());
        });
        jobMapper.insert(dbJob);
        // ?????? name ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setName("??????")));
        // ?????? status ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setStatus(JobStatusEnum.NORMAL.getStatus())));
        // ?????? handlerName ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setHandlerName(randomString())));
        // ????????????
        JobPageReqVO reqVo = new JobPageReqVO();
        reqVo.setName("??????");
        reqVo.setStatus(JobStatusEnum.INIT.getStatus());
        reqVo.setHandlerName("??????");

        // ??????
        PageResult<JobDO> pageResult = jobService.getJobPage(reqVo);
        // ??????
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbJob, pageResult.getList().get(0));
    }

    @Test
    public void testGetJobList_export() {
        // mock ??????
        JobDO dbJob = randomPojo(JobDO.class, o -> {
            o.setName("??????????????????");
            o.setHandlerName("handlerName ????????????");
            o.setStatus(JobStatusEnum.INIT.getStatus());
        });
        jobMapper.insert(dbJob);
        // ?????? name ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setName("??????")));
        // ?????? status ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setStatus(JobStatusEnum.NORMAL.getStatus())));
        // ?????? handlerName ?????????
        jobMapper.insert(cloneIgnoreId(dbJob, o -> o.setHandlerName(randomString())));
        // ????????????
        JobExportReqVO reqVo = new JobExportReqVO();
        reqVo.setName("??????");
        reqVo.setStatus(JobStatusEnum.INIT.getStatus());
        reqVo.setHandlerName("??????");

        // ??????
        List<JobDO> list = jobService.getJobList(reqVo);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbJob, list.get(0));
    }

    @Test
    public void testGetJob() {
        // mock ??????
        JobDO dbJob = randomPojo(JobDO.class);
        jobMapper.insert(dbJob);
        // ??????
        JobDO job = jobService.getJob(dbJob.getId());
        // ??????
        assertPojoEquals(dbJob, job);
    }

}
