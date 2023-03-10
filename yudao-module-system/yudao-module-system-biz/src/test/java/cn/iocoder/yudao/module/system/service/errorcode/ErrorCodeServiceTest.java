package cn.iocoder.yudao.module.system.service.errorcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.api.errorcode.dto.ErrorCodeAutoGenerateReqDTO;
import cn.iocoder.yudao.module.system.api.errorcode.dto.ErrorCodeRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.errorcode.vo.ErrorCodeCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.errorcode.vo.ErrorCodeExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.errorcode.vo.ErrorCodePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.errorcode.vo.ErrorCodeUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.errorcode.ErrorCodeDO;
import cn.iocoder.yudao.module.system.dal.mysql.errorcode.ErrorCodeMapper;
import cn.iocoder.yudao.module.system.enums.errorcode.ErrorCodeTypeEnum;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ERROR_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ERROR_CODE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

@Import(ErrorCodeServiceImpl.class)
public class ErrorCodeServiceTest extends BaseDbUnitTest {

    @Resource
    private ErrorCodeServiceImpl errorCodeService;

    @Resource
    private ErrorCodeMapper errorCodeMapper;

    @Test
    public void testCreateErrorCode_success() {
        // ????????????
        ErrorCodeCreateReqVO reqVO = randomPojo(ErrorCodeCreateReqVO.class);

        // ??????
        Long errorCodeId = errorCodeService.createErrorCode(reqVO);
        // ??????
        assertNotNull(errorCodeId);
        // ?????????????????????????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(errorCodeId);
        assertPojoEquals(reqVO, errorCode);
        assertEquals(ErrorCodeTypeEnum.MANUAL_OPERATION.getType(), errorCode.getType());
    }

    @Test
    public void testUpdateErrorCode_success() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO();
        errorCodeMapper.insert(dbErrorCode);// @Sql: ?????????????????????????????????
        // ????????????
        ErrorCodeUpdateReqVO reqVO = randomPojo(ErrorCodeUpdateReqVO.class, o -> {
            o.setId(dbErrorCode.getId()); // ??????????????? ID
        });

        // ??????
        errorCodeService.updateErrorCode(reqVO);
        // ????????????????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(reqVO.getId()); // ???????????????
        assertPojoEquals(reqVO, errorCode);
        assertEquals(ErrorCodeTypeEnum.MANUAL_OPERATION.getType(), errorCode.getType());
    }

    @Test
    public void testDeleteErrorCode_success() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO();
        errorCodeMapper.insert(dbErrorCode);// @Sql: ?????????????????????????????????
        // ????????????
        Long id = dbErrorCode.getId();

        // ??????
        errorCodeService.deleteErrorCode(id);
       // ????????????????????????
       assertNull(errorCodeMapper.selectById(id));
    }

    @Test
    public void testGetErrorCodePage() {
       // mock ??????
       ErrorCodeDO dbErrorCode = initGetErrorCodePage();
       // ????????????
       ErrorCodePageReqVO reqVO = new ErrorCodePageReqVO();
       reqVO.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType());
       reqVO.setApplicationName("tu");
       reqVO.setCode(1);
       reqVO.setMessage("ma");
       reqVO.setCreateTime(buildBetweenTime(2020, 11, 1, 2020, 11, 30));

       // ??????
       PageResult<ErrorCodeDO> pageResult = errorCodeService.getErrorCodePage(reqVO);
       // ??????
       assertEquals(1, pageResult.getTotal());
       assertEquals(1, pageResult.getList().size());
       assertPojoEquals(dbErrorCode, pageResult.getList().get(0));
    }

    /**
     * ????????? getErrorCodePage ?????????????????????
     */
    private ErrorCodeDO initGetErrorCodePage() {
        ErrorCodeDO dbErrorCode = randomErrorCodeDO(o -> { // ???????????????
            o.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType());
            o.setApplicationName("tudou");
            o.setCode(1);
            o.setMessage("yuanma");
            o.setCreateTime(buildTime(2020, 11, 11));
        });
        errorCodeMapper.insert(dbErrorCode);
        // ?????? type ?????????
        errorCodeMapper.insert(cloneIgnoreId(dbErrorCode, o -> o.setType(ErrorCodeTypeEnum.MANUAL_OPERATION.getType())));
        // ?????? applicationName ?????????
        errorCodeMapper.insert(cloneIgnoreId(dbErrorCode, o -> o.setApplicationName("yuan")));
        // ?????? code ?????????
        errorCodeMapper.insert(cloneIgnoreId(dbErrorCode, o -> o.setCode(2)));
        // ?????? message ?????????
        errorCodeMapper.insert(cloneIgnoreId(dbErrorCode, o -> o.setMessage("nai")));
        // ?????? createTime ?????????
        errorCodeMapper.insert(cloneIgnoreId(dbErrorCode, o -> o.setCreateTime(buildTime(2020, 12, 12))));
        return dbErrorCode;
    }

    @Test
    public void testGetErrorCodeList_export() {
        // mock ??????
        ErrorCodeDO dbErrorCode = initGetErrorCodePage();
        // ????????????
        ErrorCodeExportReqVO reqVO = new ErrorCodeExportReqVO();
        reqVO.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType());
        reqVO.setApplicationName("tu");
        reqVO.setCode(1);
        reqVO.setMessage("ma");
        reqVO.setCreateTime(buildBetweenTime(2020, 11, 1, 2020, 11, 30));

        // ??????
        List<ErrorCodeDO> list = errorCodeService.getErrorCodeList(reqVO);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbErrorCode, list.get(0));
    }

    @Test
    public void testValidateCodeDuplicate_codeDuplicateForCreate() {
        // ????????????
        Integer code = randomInteger();
        // mock ??????
        errorCodeMapper.insert(randomErrorCodeDO(o -> o.setCode(code)));

        // ?????????????????????
        assertServiceException(() -> errorCodeService.validateCodeDuplicate(code, null),
                ERROR_CODE_DUPLICATE);
    }

    @Test
    public void testValidateCodeDuplicate_codeDuplicateForUpdate() {
        // ????????????
        Long id = randomLongId();
        Integer code = randomInteger();
        // mock ??????
        errorCodeMapper.insert(randomErrorCodeDO(o -> o.setCode(code)));

        // ?????????????????????
        assertServiceException(() -> errorCodeService.validateCodeDuplicate(code, id),
                ERROR_CODE_DUPLICATE);
    }

    @Test
    public void testValidateErrorCodeExists_notExists() {
        assertServiceException(() -> errorCodeService.validateErrorCodeExists(null),
                ERROR_CODE_NOT_EXISTS);
    }

    /**
     * ?????? 1??????????????????????????????
     */
    @Test
    public void testAutoGenerateErrorCodes_01() {
        // ????????????
        ErrorCodeAutoGenerateReqDTO generateReqDTO = randomPojo(ErrorCodeAutoGenerateReqDTO.class);
        // mock ??????

        // ??????
        errorCodeService.autoGenerateErrorCodes(Lists.newArrayList(generateReqDTO));
        // ??????
        ErrorCodeDO errorCode = errorCodeMapper.selectOne(null);
        assertPojoEquals(generateReqDTO, errorCode);
        assertEquals(ErrorCodeTypeEnum.AUTO_GENERATION.getType(), errorCode.getType());
    }

    /**
     * ?????? 2.1?????????????????????????????? ErrorCodeTypeEnum.MANUAL_OPERATION ??????
     */
    @Test
    public void testAutoGenerateErrorCodes_021() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO(o -> o.setType(ErrorCodeTypeEnum.MANUAL_OPERATION.getType()));
        errorCodeMapper.insert(dbErrorCode);
        // ????????????
        ErrorCodeAutoGenerateReqDTO generateReqDTO = randomPojo(ErrorCodeAutoGenerateReqDTO.class,
                o -> o.setCode(dbErrorCode.getCode()));
        // mock ??????

        // ??????
        errorCodeService.autoGenerateErrorCodes(Lists.newArrayList(generateReqDTO));
        // ????????????????????????????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(dbErrorCode.getId());
        assertPojoEquals(dbErrorCode, errorCode);
    }

    /**
     * ?????? 2.2?????????????????????????????? applicationName ?????????
     */
    @Test
    public void testAutoGenerateErrorCodes_022() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO(o -> o.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType()));
        errorCodeMapper.insert(dbErrorCode);
        // ????????????
        ErrorCodeAutoGenerateReqDTO generateReqDTO = randomPojo(ErrorCodeAutoGenerateReqDTO.class,
                o -> o.setCode(dbErrorCode.getCode()).setApplicationName(randomString()));
        // mock ??????

        // ??????
        errorCodeService.autoGenerateErrorCodes(Lists.newArrayList(generateReqDTO));
        // ????????????????????????????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(dbErrorCode.getId());
        assertPojoEquals(dbErrorCode, errorCode);
    }

    /**
     * ?????? 2.3?????????????????????????????? message ??????
     */
    @Test
    public void testAutoGenerateErrorCodes_023() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO(o -> o.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType()));
        errorCodeMapper.insert(dbErrorCode);
        // ????????????
        ErrorCodeAutoGenerateReqDTO generateReqDTO = randomPojo(ErrorCodeAutoGenerateReqDTO.class,
                o -> o.setCode(dbErrorCode.getCode()).setApplicationName(dbErrorCode.getApplicationName())
                    .setMessage(dbErrorCode.getMessage()));
        // mock ??????

        // ??????
        errorCodeService.autoGenerateErrorCodes(Lists.newArrayList(generateReqDTO));
        // ????????????????????????????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(dbErrorCode.getId());
        assertPojoEquals(dbErrorCode, errorCode);
    }

    /**
     * ?????? 2.3?????????????????????????????? message ????????????????????????
     */
    @Test
    public void testAutoGenerateErrorCodes_024() {
        // mock ??????
        ErrorCodeDO dbErrorCode = randomErrorCodeDO(o -> o.setType(ErrorCodeTypeEnum.AUTO_GENERATION.getType()));
        errorCodeMapper.insert(dbErrorCode);
        // ????????????
        ErrorCodeAutoGenerateReqDTO generateReqDTO = randomPojo(ErrorCodeAutoGenerateReqDTO.class,
                o -> o.setCode(dbErrorCode.getCode()).setApplicationName(dbErrorCode.getApplicationName()));
        // mock ??????

        // ??????
        errorCodeService.autoGenerateErrorCodes(Lists.newArrayList(generateReqDTO));
        // ???????????????
        ErrorCodeDO errorCode = errorCodeMapper.selectById(dbErrorCode.getId());
        assertPojoEquals(generateReqDTO, errorCode);
    }

    @Test
    public void testGetErrorCode() {
        // ????????????
        ErrorCodeDO errorCodeDO = randomErrorCodeDO();
        errorCodeMapper.insert(errorCodeDO);
        // mock ??????
        Long id = errorCodeDO.getId();

        // ??????
        ErrorCodeDO dbErrorCode = errorCodeService.getErrorCode(id);
        // ??????
        assertPojoEquals(errorCodeDO, dbErrorCode);
    }

    @Test
    public void testGetErrorCodeList() {
        // ????????????
        ErrorCodeDO errorCodeDO01 = randomErrorCodeDO(
                o -> o.setApplicationName("yunai_server").setUpdateTime(buildTime(2022, 1, 10)));
        errorCodeMapper.insert(errorCodeDO01);
        ErrorCodeDO errorCodeDO02 = randomErrorCodeDO(
                o -> o.setApplicationName("yunai_server").setUpdateTime(buildTime(2022, 1, 12)));
        errorCodeMapper.insert(errorCodeDO02);
        // mock ??????
        String applicationName = "yunai_server";
        LocalDateTime minUpdateTime = buildTime(2022, 1, 11);

        // ??????
        List<ErrorCodeRespDTO> errorCodeList = errorCodeService.getErrorCodeList(applicationName, minUpdateTime);
        // ??????
        assertEquals(1, errorCodeList.size());
        assertPojoEquals(errorCodeDO02, errorCodeList.get(0));
    }

    // ========== ???????????? ==========

    @SafeVarargs
    private static ErrorCodeDO randomErrorCodeDO(Consumer<ErrorCodeDO>... consumers) {
        Consumer<ErrorCodeDO> consumer = (o) -> {
            o.setType(randomEle(ErrorCodeTypeEnum.values()).getType()); // ?????? key ?????????
        };
        return randomPojo(ErrorCodeDO.class, ArrayUtils.append(consumer, consumers));
    }

}
