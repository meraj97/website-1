package cn.wgn.website.controller;

import cn.wgn.website.dto.ApiRes;
import cn.wgn.website.dto.profile.MenuTree;
import cn.wgn.website.service.IProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设计人员的接口
 *
 * @author WuGuangNuo
 * @date Created in 2020/2/21 21:08
 */
@RestController
@Api(tags = "用户")
@RequestMapping("profile")
public class ProfileController extends BaseController {
    @Autowired
    private IProfileService profileService;

    @GetMapping("menuTree")
    @ApiOperation("获取菜单树形列表")
    public ApiRes<List<MenuTree>> menuTree() {
        List<MenuTree> data = profileService.getMenuTree();

        if (data.size() != 0) {
            return ApiRes.suc(data);
        } else {
            return ApiRes.fail();
        }
    }
}
