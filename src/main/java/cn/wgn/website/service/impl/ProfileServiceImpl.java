package cn.wgn.website.service.impl;

import cn.wgn.website.dto.ApiRes;
import cn.wgn.website.dto.Menu;
import cn.wgn.website.dto.SelectListItem;
import cn.wgn.website.dto.common.AccountLogin;
import cn.wgn.website.dto.common.LoginData;
import cn.wgn.website.dto.profile.MenuTree;
import cn.wgn.website.entity.UserEntity;
import cn.wgn.website.enums.RedisPrefixKeyEnum;
import cn.wgn.website.mapper.UserMapper;
import cn.wgn.website.service.IBaseService;
import cn.wgn.website.service.IProfileService;
import cn.wgn.website.utils.EncryptUtil;
import cn.wgn.website.utils.RedisUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author WuGuangNuo
 * @date Created in 2020/2/22 18:54
 */
@Slf4j
@Service
public class ProfileServiceImpl implements IProfileService, IBaseService {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EncryptUtil encryptUtil;

    // 账号过期时间,单位分
    private static final int expireTime = 9000000;

    /**
     * 账号密码登录
     *
     * @return
     */
    @Override
    public ApiRes loginByPd(AccountLogin accountLogin) {
        log.info("登录用户信息：" + accountLogin.toString());

        // 验证登录密码
        UserEntity userEntity = userMapper.selectOne(
                new QueryWrapper<UserEntity>().lambda().eq(UserEntity::getUsername, accountLogin.getAccount())
        );
        if (userEntity == null) {
            return ApiRes.fail("账号不存在");
        }
        if (!userEntity.getPassword().equals(encryptUtil.getMD5Str(accountLogin.getPassword()))) {
            return ApiRes.fail("密码错误");
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // Redis : DB.Sys -> Id:No:RoleId
        redisUtil.set(token, RedisPrefixKeyEnum.Sys.toString(), userEntity.getId() + ":" + userEntity.getUsername() + ":" + userEntity.getRoleid(), expireTime);

        LoginData loginData = new LoginData();
        BeanUtils.copyProperties(userEntity, loginData);
        loginData.setToken(token);

        userEntity.setLoginAt(LocalDateTime.now());
        userMapper.updateById(userEntity);

        return ApiRes.suc("登录成功", loginData);
    }

    /**
     * 获取树形菜单
     *
     * @return
     */
    @Override
    public List<MenuTree> getMenuTree() {
        List<Menu> menus = getMenuList();

        List<MenuTree> result = new LinkedList<>();
        MenuTree menuTree = null;
        SelectListItem item;

        for (Menu m : menus) {
//            if (m.getCode().equals("index")) {
//                continue;
//            }
            if (!m.getUrl().contains("/")) {
                if (menuTree != null) {
                    result.add(menuTree);
                }
                menuTree = new MenuTree();
                menuTree.setIcon(m.getIcon())
                        .setIndex(m.getCode())
                        .setTitle(m.getName())
                        .setSubs(new LinkedList<>());
            } else {
                item = new SelectListItem();
                item.setText(m.getName())
                        .setValue(m.getCode());
                assert menuTree != null;
                menuTree.getSubs().add(item);
            }
        }
        result.add(menuTree);
        return result;
    }

    /**
     * 获取菜单列表
     *
     * @return
     */
    private List<Menu> getMenuList() {
        // 获取用户的权限 匹配的code

        String jsonStr = "[{\"code\":\"index\",\"icon\":\"el-icon-lx-home\",\"url\":\"index\",\"name\":\"系统首页\"},{\"code\":\"write\",\"icon\":\"el-icon-lx-home\",\"url\":\"write\",\"name\":\"写作\"},{\"code\":\"editor\",\"icon\":\"el-icon-lx-home\",\"url\":\"write/editor\",\"name\":\"富文本编辑器\"},{\"code\":\"markdown\",\"icon\":\"el-icon-lx-home\",\"url\":\"write/markdown\",\"name\":\"markdown编辑器\"}]";

        List<Menu> menus = JSONArray.parseArray(jsonStr, Menu.class);
        return menus;
    }
}
