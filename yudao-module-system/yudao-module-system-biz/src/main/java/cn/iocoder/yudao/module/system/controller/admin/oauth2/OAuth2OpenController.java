package cn.iocoder.yudao.module.system.controller.admin.oauth2;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.operatelog.core.annotations.OperateLog;
import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAccessTokenRespVO;
import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.open.OAuth2OpenAuthorizeInfoRespVO;
import cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.open.OAuth2OpenCheckTokenRespVO;
import cn.iocoder.yudao.module.system.convert.oauth2.OAuth2OpenConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.iocoder.yudao.module.system.enums.oauth2.OAuth2GrantTypeEnum;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2ApproveService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2ClientService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2GrantService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.util.oauth2.OAuth2Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * ?????????????????????????????????
 *
 * ?????????????????????????????? /system-api/* ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? RBAC ????????????????????????
 * ??????????????????????????????????????????????????? OpenAPI???????????????????????????????????? Controller ????????? open ???????????? /open-api/* ????????????????????? scope ???????????????
 * ????????????????????????????????????????????????????????? client_id ????????? access token ??????????????????????????????????????????????????????????????? API ????????????????????? client_id ?????????????????????
 *
 * ?????????????????????????????????????????????????????????????????????????????? access token ??????????????????????????????????????????????????? /system-api/* ????????????????????????????????? scope ?????????
 * scope ???????????????????????? {@link OAuth2UserController} ???
 *
 * @author ????????????
 */
@Tag(name = "???????????? - OAuth2.0 ??????")
@RestController
@RequestMapping("/system/oauth2")
@Validated
@Slf4j
public class OAuth2OpenController {

    @Resource
    private OAuth2GrantService oauth2GrantService;
    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private OAuth2ApproveService oauth2ApproveService;
    @Resource
    private OAuth2TokenService oauth2TokenService;

    /**
     * ?????? Spring Security OAuth ??? TokenEndpoint ?????? postAccessToken ??????
     *
     * ????????? authorization_code ????????????code + redirectUri + state ??????
     * ?????? password ????????????username + password + scope ??????
     * ?????? refresh_token ????????????refreshToken ??????
     * ????????? client_credentials ?????????scope ??????
     * ?????? implicit ?????????????????????
     *
     * ??????????????????????????? client_id + client_secret ??????
     */
    @PostMapping("/token")
    @PermitAll
    @Operation(summary = "??????????????????", description = "?????? code ???????????????????????? implicit ?????????????????? sso.vue ???????????????????????????????????????")
    @Parameters({
            @Parameter(name = "grant_type", required = true, description = "????????????", example = "code"),
            @Parameter(name = "code", description = "????????????", example = "userinfo.read"),
            @Parameter(name = "redirect_uri", description = "????????? URI", example = "https://www.iocoder.cn"),
            @Parameter(name = "state", description = "??????", example = "1"),
            @Parameter(name = "username", example = "tudou"),
            @Parameter(name = "password", example = "cai"), // ????????????????????????
            @Parameter(name = "scope", example = "user_info"),
            @Parameter(name = "refresh_token", example = "123424233"),
    })
    @OperateLog(enable = false) // ?????? Post ???????????????????????????
    public CommonResult<OAuth2OpenAccessTokenRespVO> postAccessToken(HttpServletRequest request,
                                                                     @RequestParam("grant_type") String grantType,
                                                                     @RequestParam(value = "code", required = false) String code, // ???????????????
                                                                     @RequestParam(value = "redirect_uri", required = false) String redirectUri, // ???????????????
                                                                     @RequestParam(value = "state", required = false) String state, // ???????????????
                                                                     @RequestParam(value = "username", required = false) String username, // ????????????
                                                                     @RequestParam(value = "password", required = false) String password, // ????????????
                                                                     @RequestParam(value = "scope", required = false) String scope, // ????????????
                                                                     @RequestParam(value = "refresh_token", required = false) String refreshToken) { // ????????????
        List<String> scopes = OAuth2Utils.buildScopes(scope);
        // 1.1 ??????????????????
        OAuth2GrantTypeEnum grantTypeEnum = OAuth2GrantTypeEnum.getByGranType(grantType);
        if (grantTypeEnum == null) {
            throw exception0(BAD_REQUEST.getCode(), StrUtil.format("??????????????????({})", grantType));
        }
        if (grantTypeEnum == OAuth2GrantTypeEnum.IMPLICIT) {
            throw exception0(BAD_REQUEST.getCode(), "Token ??????????????? implicit ????????????");
        }

        // 1.2 ???????????????
        String[] clientIdAndSecret = obtainBasicAuthorization(request);
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientIdAndSecret[0], clientIdAndSecret[1],
                grantType, scopes, redirectUri);

        // 2. ???????????????????????????????????????
        OAuth2AccessTokenDO accessTokenDO;
        switch (grantTypeEnum) {
            case AUTHORIZATION_CODE:
                accessTokenDO = oauth2GrantService.grantAuthorizationCodeForAccessToken(client.getClientId(), code, redirectUri, state);
                break;
            case PASSWORD:
                accessTokenDO = oauth2GrantService.grantPassword(username, password, client.getClientId(), scopes);
                break;
            case CLIENT_CREDENTIALS:
                accessTokenDO = oauth2GrantService.grantClientCredentials(client.getClientId(), scopes);
                break;
            case REFRESH_TOKEN:
                accessTokenDO = oauth2GrantService.grantRefreshToken(refreshToken, client.getClientId());
                break;
            default:
                throw new IllegalArgumentException("?????????????????????" + grantType);
        }
        Assert.notNull(accessTokenDO, "????????????????????????"); // ???????????????
        return success(OAuth2OpenConvert.INSTANCE.convert(accessTokenDO));
    }

    @DeleteMapping("/token")
    @PermitAll
    @Operation(summary = "??????????????????")
    @Parameter(name = "token", required = true, description = "????????????", example = "biu")
    @OperateLog(enable = false) // ?????? Post ???????????????????????????
    public CommonResult<Boolean> revokeToken(HttpServletRequest request,
                                             @RequestParam("token") String token) {
        // ???????????????
        String[] clientIdAndSecret = obtainBasicAuthorization(request);
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientIdAndSecret[0], clientIdAndSecret[1],
                null, null, null);

        // ??????????????????
        return success(oauth2GrantService.revokeToken(client.getClientId(), token));
    }

    /**
     * ?????? Spring Security OAuth ??? CheckTokenEndpoint ?????? checkToken ??????
     */
    @PostMapping("/check-token")
    @PermitAll
    @Operation(summary = "??????????????????")
    @Parameter(name = "token", required = true, description = "????????????", example = "biu")
    @OperateLog(enable = false) // ?????? Post ???????????????????????????
    public CommonResult<OAuth2OpenCheckTokenRespVO> checkToken(HttpServletRequest request,
                                                               @RequestParam("token") String token) {
        // ???????????????
        String[] clientIdAndSecret = obtainBasicAuthorization(request);
        oauth2ClientService.validOAuthClientFromCache(clientIdAndSecret[0], clientIdAndSecret[1],
                null, null, null);

        // ????????????
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.checkAccessToken(token);
        Assert.notNull(accessTokenDO, "????????????????????????"); // ???????????????
        return success(OAuth2OpenConvert.INSTANCE.convert2(accessTokenDO));
    }

    /**
     * ?????? Spring Security OAuth ??? AuthorizationEndpoint ?????? authorize ??????
     */
    @GetMapping("/authorize")
    @Operation(summary = "??????????????????", description = "?????? code ???????????????????????? implicit ?????????????????? sso.vue ???????????????????????????????????????")
    @Parameter(name = "clientId", required = true, description = "???????????????", example = "tudou")
    public CommonResult<OAuth2OpenAuthorizeInfoRespVO> authorize(@RequestParam("clientId") String clientId) {
        // 0. ????????????????????????????????? Spring Security ??????

        // 1. ?????? Client ??????????????????
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId);
        // 2. ?????????????????????????????????
        List<OAuth2ApproveDO> approves = oauth2ApproveService.getApproveList(getLoginUserId(), getUserType(), clientId);
        // ????????????
        return success(OAuth2OpenConvert.INSTANCE.convert(client, approves));
    }

    /**
     * ?????? Spring Security OAuth ??? AuthorizationEndpoint ?????? approveOrDeny ??????
     *
     * ??????????????????????????? autoApprove = true???
     *      ????????? sso.vue ??????????????????????????????????????????????????????????????????????????????????????? OAuth2Client ????????? scope ???????????????
     * ??????????????????????????? autoApprove = false???
     *      ??? sso.vue ???????????????????????? scope ?????????????????????????????????????????????????????????approved ??? true ?????? false
     *
     * ????????????????????????Axios ????????????????????? 302 ????????????????????? Spring Security OAuth ?????????????????????????????????????????? URL???????????????????????????
     */
    @PostMapping("/authorize")
    @Operation(summary = "????????????", description = "?????? code ???????????????????????? implicit ?????????????????? sso.vue ???????????????????????????????????????")
    @Parameters({
            @Parameter(name = "response_type", required = true, description = "????????????", example = "code"),
            @Parameter(name = "client_id", required = true, description = "???????????????", example = "tudou"),
            @Parameter(name = "scope", description = "????????????", example = "userinfo.read"), // ?????? Map<String, Boolean> ?????????Spring MVC ?????????????????????????????????
            @Parameter(name = "redirect_uri", required = true, description = "????????? URI", example = "https://www.iocoder.cn"),
            @Parameter(name = "auto_approve", required = true, description = "??????????????????", example = "true"),
            @Parameter(name = "state", example = "1")
    })
    @OperateLog(enable = false) // ?????? Post ???????????????????????????
    public CommonResult<String> approveOrDeny(@RequestParam("response_type") String responseType,
                                              @RequestParam("client_id") String clientId,
                                              @RequestParam(value = "scope", required = false) String scope,
                                              @RequestParam("redirect_uri") String redirectUri,
                                              @RequestParam(value = "auto_approve") Boolean autoApprove,
                                              @RequestParam(value = "state", required = false) String state) {
        @SuppressWarnings("unchecked")
        Map<String, Boolean> scopes = JsonUtils.parseObject(scope, Map.class);
        scopes = ObjectUtil.defaultIfNull(scopes, Collections.emptyMap());
        // 0. ????????????????????????????????? Spring Security ??????

        // 1.1 ?????? responseType ???????????? code ?????? token ???
        OAuth2GrantTypeEnum grantTypeEnum = getGrantTypeEnum(responseType);
        // 1.2 ?????? redirectUri ??????????????????????????? + ?????? scope ????????? Client ???????????????
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(clientId, null,
                grantTypeEnum.getGrantType(), scopes.keySet(), redirectUri);

        // 2.1 ?????? approved ??? null?????????????????????
        if (Boolean.TRUE.equals(autoApprove)) {
            // ????????????????????????????????????????????? url????????????????????????
            if (!oauth2ApproveService.checkForPreApproval(getLoginUserId(), getUserType(), clientId, scopes.keySet())) {
                return success(null);
            }
        } else { // 2.2 ?????? approved ??? null?????????????????????
            // ??????????????????????????????????????????????????????
            if (!oauth2ApproveService.updateAfterApproval(getLoginUserId(), getUserType(), clientId, scopes)) {
                return success(OAuth2Utils.buildUnsuccessfulRedirect(redirectUri, responseType, state,
                        "access_denied", "User denied access"));
            }
        }

        // 3.1 ????????? code ??????????????????????????? code ????????????????????????
        List<String> approveScopes = convertList(scopes.entrySet(), Map.Entry::getKey, Map.Entry::getValue);
        if (grantTypeEnum == OAuth2GrantTypeEnum.AUTHORIZATION_CODE) {
            return success(getAuthorizationCodeRedirect(getLoginUserId(), client, approveScopes, redirectUri, state));
        }
        // 3.2 ????????? token ?????? implicit ???????????????????????? accessToken ???????????????????????????
        return success(getImplicitGrantRedirect(getLoginUserId(), client, approveScopes, redirectUri, state));
    }

    private static OAuth2GrantTypeEnum getGrantTypeEnum(String responseType) {
        if (StrUtil.equals(responseType, "code")) {
            return OAuth2GrantTypeEnum.AUTHORIZATION_CODE;
        }
        if (StrUtil.equalsAny(responseType, "token")) {
            return OAuth2GrantTypeEnum.IMPLICIT;
        }
        throw exception0(BAD_REQUEST.getCode(), "response_type ?????????????????? code ??? token");
    }

    private String getImplicitGrantRedirect(Long userId, OAuth2ClientDO client,
                                            List<String> scopes, String redirectUri, String state) {
        // 1. ?????? access token ????????????
        OAuth2AccessTokenDO accessTokenDO = oauth2GrantService.grantImplicit(userId, getUserType(), client.getClientId(), scopes);
        Assert.notNull(accessTokenDO, "????????????????????????"); // ???????????????
        // 2. ?????????????????? URL
        // noinspection unchecked
        return OAuth2Utils.buildImplicitRedirectUri(redirectUri, accessTokenDO.getAccessToken(), state, accessTokenDO.getExpiresTime(),
                scopes, JsonUtils.parseObject(client.getAdditionalInformation(), Map.class));
    }

    private String getAuthorizationCodeRedirect(Long userId, OAuth2ClientDO client,
                                                List<String> scopes, String redirectUri, String state) {
        // 1. ?????? code ?????????
        String authorizationCode = oauth2GrantService.grantAuthorizationCodeForCode(userId, getUserType(), client.getClientId(), scopes,
                redirectUri, state);
        // 2. ?????????????????? URL
        return OAuth2Utils.buildAuthorizationCodeRedirectUri(redirectUri, authorizationCode, state);
    }

    private Integer getUserType() {
        return UserTypeEnum.ADMIN.getValue();
    }

    private String[] obtainBasicAuthorization(HttpServletRequest request) {
        String[] clientIdAndSecret = HttpUtils.obtainBasicAuthorization(request);
        if (ArrayUtil.isEmpty(clientIdAndSecret) || clientIdAndSecret.length != 2) {
            throw exception0(BAD_REQUEST.getCode(), "client_id ??? client_secret ???????????????");
        }
        return clientIdAndSecret;
    }

}
