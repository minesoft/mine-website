package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.mq.producer.permission.RoleProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@Import(RoleServiceImpl.class)
public class RoleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private RoleServiceImpl roleService;

    @Resource
    private RoleMapper roleMapper;

    @MockBean
    private PermissionService permissionService;
    @MockBean
    private RoleProducer roleProducer;

    @Test
    public void testInitLocalCache() {
        RoleDO roleDO1 = randomPojo(RoleDO.class);
        roleMapper.insert(roleDO1);
        RoleDO roleDO2 = randomPojo(RoleDO.class);
        roleMapper.insert(roleDO2);

        // ??????
        roleService.initLocalCache();
        // ?????? roleCache ??????
        Map<Long, RoleDO> roleCache = roleService.getRoleCache();
        assertPojoEquals(roleDO1, roleCache.get(roleDO1.getId()));
        assertPojoEquals(roleDO2, roleCache.get(roleDO2.getId()));
    }

    @Test
    public void testCreateRole_success() {
        // ????????????
        RoleCreateReqVO reqVO = randomPojo(RoleCreateReqVO.class);

        // ??????
        Long roleId = roleService.createRole(reqVO, null);
        // ??????
        RoleDO roleDO = roleMapper.selectById(roleId);
        assertPojoEquals(reqVO, roleDO);
        assertEquals(RoleTypeEnum.CUSTOM.getType(), roleDO.getType());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), roleDO.getStatus());
        assertEquals(DataScopeEnum.ALL.getScope(), roleDO.getDataScope());
        // verify ??????????????????
        verify(roleProducer).sendRoleRefreshMessage();
    }

    @Test
    public void testUpdateRole_success() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setType(RoleTypeEnum.CUSTOM.getType()));
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();
        RoleUpdateReqVO reqVO = randomPojo(RoleUpdateReqVO.class, o -> o.setId(id));

        // ??????
        roleService.updateRole(reqVO);
        // ??????
        RoleDO newRoleDO = roleMapper.selectById(id);
        assertPojoEquals(reqVO, newRoleDO);
        // verify ??????????????????
        verify(roleProducer).sendRoleRefreshMessage();
    }

    @Test
    public void testUpdateRoleStatus_success() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setType(RoleTypeEnum.CUSTOM.getType()));
        roleMapper.insert(roleDO);

        // ????????????
        Long roleId = roleDO.getId();

        // ??????
        roleService.updateRoleStatus(roleId, CommonStatusEnum.DISABLE.getStatus());
        // ??????
        RoleDO dbRoleDO = roleMapper.selectById(roleId);
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), dbRoleDO.getStatus());
        // verify ??????????????????
        verify(roleProducer).sendRoleRefreshMessage();
    }

    @Test
    public void testUpdateRoleDataScope_success() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setType(RoleTypeEnum.CUSTOM.getType()));
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();
        Integer dataScope = randomEle(DataScopeEnum.values()).getScope();
        Set<Long> dataScopeRoleIds = randomSet(Long.class);

        // ??????
        roleService.updateRoleDataScope(id, dataScope, dataScopeRoleIds);
        // ??????
        RoleDO dbRoleDO = roleMapper.selectById(id);
        assertEquals(dataScope, dbRoleDO.getDataScope());
        assertEquals(dataScopeRoleIds, dbRoleDO.getDataScopeDeptIds());
        // verify ??????????????????
        verify(roleProducer).sendRoleRefreshMessage();
    }

    @Test
    public void testDeleteRole_success() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setType(RoleTypeEnum.CUSTOM.getType()));
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();

        // ??????
        roleService.deleteRole(id);
        // ??????
        assertNull(roleMapper.selectById(id));
        // verify ??????????????????
        verify(permissionService).processRoleDeleted(id);
        // verify ??????????????????
        verify(roleProducer).sendRoleRefreshMessage();
    }

    @Test
    public void testGetRoleFromCache() {
        // mock ??????????????????
        RoleDO roleDO = randomPojo(RoleDO.class);
        roleMapper.insert(roleDO);
        roleService.initLocalCache();
        // ????????????
        Long id = roleDO.getId();

        // ??????
        RoleDO dbRoleDO = roleService.getRoleFromCache(id);
        // ??????
        assertPojoEquals(roleDO, dbRoleDO);
    }

    @Test
    public void testGetRole() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class);
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();

        // ??????
        RoleDO dbRoleDO = roleService.getRole(id);
        // ??????
        assertPojoEquals(roleDO, dbRoleDO);
    }

    @Test
    public void testGetRoleListByStatus_statusNotEmpty() {
        // mock ??????
        RoleDO dbRole = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        roleMapper.insert(dbRole);
        // ?????? status ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));

        // ??????
        List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbRole, list.get(0));
    }

    @Test
    public void testGetRoleListByStatus_statusEmpty() {
        // mock ??????
        RoleDO dbRole01 = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        roleMapper.insert(dbRole01);
        RoleDO dbRole02 = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        roleMapper.insert(dbRole02);

        // ??????
        List<RoleDO> list = roleService.getRoleListByStatus(null);
        // ??????
        assertEquals(2, list.size());
        assertPojoEquals(dbRole01, list.get(0));
        assertPojoEquals(dbRole02, list.get(1));
    }

    @Test
    public void testGetRoleListFromCache() {
        // mock ??????
        RoleDO dbRole = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        roleMapper.insert(dbRole);
        // ?????? id ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> {}));
        roleService.initLocalCache();
        // ????????????
        Collection<Long> ids = singleton(dbRole.getId());

        // ??????
        List<RoleDO> list = roleService.getRoleListFromCache(ids);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbRole, list.get(0));
    }

    @Test
    public void testGetRoleList() {
        // mock ??????
        RoleDO dbRole = randomPojo(RoleDO.class, o -> { // ???????????????
            o.setName("??????");
            o.setCode("tudou");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2022, 2, 8));
        });
        roleMapper.insert(dbRole);
        // ?????? name ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setName("??????")));
        // ?????? code ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCode("hong")));
        // ?????? createTime ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCreateTime(buildTime(2022, 2, 16))));
        // ????????????
        RoleExportReqVO reqVO = new RoleExportReqVO();
        reqVO.setName("??????");
        reqVO.setCode("tu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2022, 2, 1, 2022, 2, 12));

        // ??????
        List<RoleDO> list = roleService.getRoleList(reqVO);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbRole, list.get(0));
    }

    @Test
    public void testGetRolePage() {
        // mock ??????
        RoleDO dbRole = randomPojo(RoleDO.class, o -> { // ???????????????
            o.setName("??????");
            o.setCode("tudou");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2022, 2, 8));
        });
        roleMapper.insert(dbRole);
        // ?????? name ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setName("??????")));
        // ?????? code ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCode("hong")));
        // ?????? createTime ?????????
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCreateTime(buildTime(2022, 2, 16))));
        // ????????????
        RolePageReqVO reqVO = new RolePageReqVO();
        reqVO.setName("??????");
        reqVO.setCode("tu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2022, 2, 1, 2022, 2, 12));

        // ??????
        PageResult<RoleDO> pageResult = roleService.getRolePage(reqVO);
        // ??????
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbRole, pageResult.getList().get(0));
    }

    @Test
    public void testHasAnySuperAdmin() {
        // ?????????
        assertTrue(roleService.hasAnySuperAdmin(singletonList(randomPojo(RoleDO.class,
                o -> o.setCode("super_admin")))));
        // ?????????
        assertFalse(roleService.hasAnySuperAdmin(singletonList(randomPojo(RoleDO.class,
                o -> o.setCode("tenant_admin")))));
    }

    @Test
    public void testValidateRoleDuplicate_success() {
        // ????????????????????????
        roleService.validateRoleDuplicate(randomString(), randomString(), null);
    }

    @Test
    public void testValidateRoleDuplicate_nameDuplicate() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setName("role_name"));
        roleMapper.insert(roleDO);
        // ????????????
        String name = "role_name";

        // ????????????????????????
        assertServiceException(() -> roleService.validateRoleDuplicate(name, randomString(), null),
                ROLE_NAME_DUPLICATE, name);
    }

    @Test
    public void testValidateRoleDuplicate_codeDuplicate() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setCode("code"));
        roleMapper.insert(roleDO);
        // ????????????
        String code = "code";

        // ????????????????????????
        assertServiceException(() -> roleService.validateRoleDuplicate(randomString(), code, null),
                ROLE_CODE_DUPLICATE, code);
    }

    @Test
    public void testValidateUpdateRole_success() {
        RoleDO roleDO = randomPojo(RoleDO.class);
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();

        // ??????????????????
        roleService.validateRoleForUpdate(id);
    }

    @Test
    public void testValidateUpdateRole_roleIdNotExist() {
        assertServiceException(() -> roleService.validateRoleForUpdate(randomLongId()), ROLE_NOT_EXISTS);
    }

    @Test
    public void testValidateUpdateRole_systemRoleCanNotBeUpdate() {
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setType(RoleTypeEnum.SYSTEM.getType()));
        roleMapper.insert(roleDO);
        // ????????????
        Long id = roleDO.getId();

        assertServiceException(() -> roleService.validateRoleForUpdate(id),
                ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
    }

    @Test
    public void testValidateRoleList_success() {
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        roleMapper.insert(roleDO);
        // ????????????
        List<Long> ids = singletonList(roleDO.getId());

        // ?????????????????????
        roleService.validateRoleList(ids);
    }

    @Test
    public void testValidateRoleList_notFound() {
        // ????????????
        List<Long> ids = singletonList(randomLongId());

        // ??????, ???????????????
        assertServiceException(() -> roleService.validateRoleList(ids), ROLE_NOT_EXISTS);
    }

    @Test
    public void testValidateRoleList_notEnable() {
        // mock ??????
        RoleDO RoleDO = randomPojo(RoleDO.class, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        roleMapper.insert(RoleDO);
        // ????????????
        List<Long> ids = singletonList(RoleDO.getId());

        // ??????, ???????????????
        assertServiceException(() -> roleService.validateRoleList(ids), ROLE_IS_DISABLE, RoleDO.getName());
    }
}
