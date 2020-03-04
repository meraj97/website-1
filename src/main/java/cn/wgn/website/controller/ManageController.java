package cn.wgn.website.controller;

import cn.wgn.website.dto.ApiRes;
import cn.wgn.website.dto.CommonData;
import cn.wgn.website.dto.manage.IpDto;
import cn.wgn.website.dto.manage.Novel;
import cn.wgn.website.dto.manage.NovelDto;
import cn.wgn.website.dto.manage.NovelQueryDto;
import cn.wgn.website.entity.NovelEntity;
import cn.wgn.website.enums.NovelTypeEnum;
import cn.wgn.website.handler.Authorize;
import cn.wgn.website.service.IManageService;
import cn.wgn.website.utils.CosClientUtil;
import cn.wgn.website.utils.ExcelUtil;
import cn.wgn.website.utils.IpUtil;
import cn.wgn.website.utils.WebSiteUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.List;

/**
 * 需要管理员权限的控制器
 *
 * @author WuGuangNuo
 * @date Created in 2020/2/18 17:30
 */

@RestController
@Api(tags = "管理")
@RequestMapping("manage")
public class ManageController extends BaseController {
    @Autowired
    private IpUtil ipUtil;

    private final IManageService manageService;

    public ManageController(IManageService manageServiceImpl) {
        this.manageService = manageServiceImpl;
    }

    @Autowired
    private CosClientUtil cosClientUtil;

    @Authorize() // 需要某个权限
    @PostMapping("getIp")
    @ApiOperation("getIp")
    public ApiRes<IpDto> index(HttpServletRequest request) {
        String ip = ipUtil.getIpAddr(request);
        IpDto result = manageService.getIp(ip);

        if (result == null) {
            return ApiRes.fail();
        } else {
            return ApiRes.suc(result);
        }
    }

    @Authorize("author")
    @PostMapping("addEdit")
    @ApiOperation("新增小说")
    public ApiRes addEdit(@RequestBody NovelDto novelDto) {
        Object result = manageService.addNovel(novelDto, NovelTypeEnum.Html);

        if (result instanceof Number) {
            return ApiRes.suc((Number) result);
        } else {
            return ApiRes.err((String) result);
        }
    }

    @Authorize("author")
    @PostMapping("addMarkdown")
    @ApiOperation("新增Markdown小说")
    public ApiRes addMarkdown(@RequestBody NovelDto novelDto) {
        Object result = manageService.addNovel(novelDto, NovelTypeEnum.Markdown);

        if (result instanceof Number) {
            return ApiRes.suc((Number) result);
        } else {
            return ApiRes.err((String) result);
        }
    }

    @Authorize("author")
    @PostMapping("novelList")
    @ApiOperation("查看小说列表")
    public ApiRes<IPage<Novel>> novelList(HttpServletResponse response, @RequestBody NovelQueryDto dto) {
        // 校验
        String checkOrderBy = WebSiteUtil.checkOrderBy(dto.getOrderBy(), NovelEntity.class);
        if (!"1".equals(checkOrderBy)) {
            return ApiRes.fail(checkOrderBy);
        }

        // 调整
        WebSiteUtil.sortDto(dto);

        // 判断
        if ("Excel".equalsIgnoreCase(dto.getExport())) {
            List<Novel> data = manageService.novelListExcel(dto);

            String fileName = "小说列表";
            String excelName = "Sheet1";
            String excelTitles = "ID,标题,作者,类型,内容,创建时间,更新时间";
            try {
                ExcelUtil.exportExcel(response, fileName, excelName, excelTitles, data);
            } catch (Exception e) {
                LOG.error(e.getMessage() + e);
            }

            return null;
        } else {
            IPage<Novel> tbody = manageService.novelList(dto);

            if (tbody != null) {
                return ApiRes.suc(tbody);
            } else {
                return ApiRes.fail();
            }
        }
    }

    @Authorize("author")
    @PostMapping("novelDetail")
    @ApiOperation("查看小说")
    public ApiRes<NovelEntity> novelDetail(@RequestBody CommonData data) {
        Integer novelId = data.getId();
        if (novelId == null || novelId <= 0) {
            return ApiRes.fail("novel id 错误");
        }
        NovelEntity result = manageService.novelDetail(novelId);

        if (result == null) {
            return ApiRes.fail();
        } else {
            return ApiRes.suc(result);
        }
    }

    @Authorize("author")
    @PostMapping("novelDelete")
    @ApiOperation("删除小说")
    public ApiRes<NovelEntity> novelDelete(@RequestBody CommonData data) {
        Integer novelId = data.getId();
        if (novelId == null || novelId <= 0) {
            return ApiRes.fail("novel id 错误");
        }
        String result = manageService.novelDelete(novelId);

        if (!"1".equals(result)) {
            return ApiRes.fail(result);
        } else {
            return ApiRes.suc("删除成功");
        }
    }

    @PostMapping(value = "uploadImg")
    @ApiOperation(value = "上传图片到COS")
    public ApiRes<String> uploadFile(@ApiParam(value = "上传文件", required = true) MultipartFile file) {
        if (file == null) {
            return ApiRes.fail("文件不能为空");
        }
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);
        int month = rightNow.get(Calendar.MONTH);
        String tm = year + (month < 9 ? "0" : "") + (month + 1);
        String result = cosClientUtil.uploadFile2Cos(file, "novelimg/" + tm);
        return ApiRes.suc("上传成功", result);
    }
}
