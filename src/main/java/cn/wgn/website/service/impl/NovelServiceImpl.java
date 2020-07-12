package cn.wgn.website.service.impl;

import cn.wgn.framework.utils.WordUtil;
import cn.wgn.framework.web.service.impl.BaseServiceImpl;
import cn.wgn.website.dto.NovelDto;
import cn.wgn.website.dto.NovelQueryDto;
import cn.wgn.website.entity.NovelEntity;
import cn.wgn.website.enums.NovelTypeEnum;
import cn.wgn.website.enums.StateEnum;
import cn.wgn.website.mapper.NovelMapper;
import cn.wgn.website.service.INovelService;
import cn.wgn.website.utils.CosClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Strings;
import org.markdownj.MarkdownProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 小说表 服务实现类
 * </p>
 *
 * @author WuGuangNuo
 * @since 2020-06-27
 */
@Service
public class NovelServiceImpl extends BaseServiceImpl<NovelMapper, NovelEntity> implements INovelService {
    @Autowired
    private NovelMapper novelMapper;
    @Autowired
    private CosClientUtil cosClientUtil;

    /**
     * 新增小说
     *
     * @param novelDto
     * @param novelTypeEnum
     * @return
     */
    @Override
    public Object addNovel(NovelDto novelDto, NovelTypeEnum novelTypeEnum) {
        if (novelDto.getId() != null && novelDto.getId() > 0) {
            NovelEntity entity = novelMapper.selectById(novelDto.getId());
            if (entity == null
                    || !novelTypeEnum.toString().equals(entity.getNovelType())
                    || !getUserData().getAccount().equals(entity.getNovelAuthor())) {
                return "您不是作者，不可修改文章！";
            }
            entity.setNovelAuthor(getUserData().getAccount())
                    .setNovelTitle(novelDto.getNovelTitle())
                    .setNovelContent(novelDto.getNovelContent())
                    .setNovelType(novelTypeEnum.toString())
                    .setStatus(novelDto.getNovelState());
            novelMapper.updateById(entity);
            return entity.getId();
        } else {
            NovelEntity entity = new NovelEntity();
            BeanUtils.copyProperties(novelDto, entity);
            entity.setNovelAuthor(getUserData().getAccount())
                    .setNovelType(novelTypeEnum.toString())
                    .setStatus(novelDto.getNovelState());
            novelMapper.insert(entity);
            return entity.getId();
        }
    }

    /**
     * 查看小说列表
     *
     * @param dto
     * @return
     */
    @Override
//    @Cacheable(value = "novelList", key = "#dto.toString()+#dto.pageIndex+'_'+#dto.pageSize")
    public List<NovelEntity> novelList(NovelQueryDto dto) {
        List<NovelEntity> novelList = this.list(novelListQw(dto));
        for (NovelEntity e : novelList) {
            e.setStatus(StateEnum.valueOf(e.getStatus()).getDesc());
            e.setNovelType(NovelTypeEnum.valueOf(e.getNovelType()).getDesc());
        }
        return novelList;
    }

    /**
     * 查看小说列表（QueryWrapper）
     *
     * @param dto
     * @return
     */
    private QueryWrapper<NovelEntity> novelListQw(NovelQueryDto dto) {
        QueryWrapper<NovelEntity> qw = new QueryWrapper<NovelEntity>();
        qw.select("id", "novel_title", "novel_author", "novel_type", "substr(novel_content,1,300)", "status",
                "create_by_id", "create_by_name", "create_time", "modified_by_id", "modified_by_name", "modified_time");
        // 作者或公共
        qw.lambda().and(
                q -> q.eq(NovelEntity::getNovelAuthor, getUserData().getAccount())
                        .or()
                        .eq(NovelEntity::getStatus, StateEnum.PUBLIC.getValue())
        );
        if (!Strings.isNullOrEmpty(dto.getNovelTitle())) {
            qw.lambda().like(NovelEntity::getNovelTitle, dto.getNovelTitle());
        }
        if (!Strings.isNullOrEmpty(dto.getNovelType())) {
            qw.lambda().eq(NovelEntity::getNovelType, dto.getNovelType());
        }
        if (dto.getCreateTm1() != null) {
            qw.lambda().gt(NovelEntity::getCreateTime, dto.getCreateTm1());
        }
        if (dto.getCreateTm2() != null) {
            qw.lambda().lt(NovelEntity::getCreateTime, dto.getCreateTm2());
        }
        if (dto.getUpdateTm1() != null) {
            qw.lambda().gt(NovelEntity::getModifiedTime, dto.getUpdateTm1());
        }
        if (dto.getUpdateTm2() != null) {
            qw.lambda().lt(NovelEntity::getModifiedTime, dto.getUpdateTm2());
        }
//        // 不展示不存在状态
//        qw.lambda().ne(NovelEntity::getStatus, StateEnum.NOTHING.getValue());
//        if (!Strings.isNullOrEmpty(dto.getOrderBy())) {
//            String[] s = dto.getOrderBy().split(" ");
//            qw.orderBy(true, "ASC".equalsIgnoreCase(s[1]), s[0]);
//        } else {
//            qw.lambda().orderByDesc(NovelEntity::getId);
//        }
        return qw;
    }

    /**
     * 查看小说
     *
     * @param novelId
     * @return
     */
    @Override
    public NovelDto novelDetail(Integer novelId) {
        NovelEntity entity = novelMapper.selectById(novelId);
        if (entity != null
                && (StateEnum.PUBLIC.getValue().equals(entity.getStatus())
                || getUserData().getAccount().equals(entity.getNovelAuthor()))) {
            NovelDto dto = new NovelDto();
            BeanUtils.copyProperties(entity, dto);
            dto.setNovelState(entity.getStatus());
            return dto;
        } else {
            return null;
        }
    }

    /**
     * 删除小说(逻辑删除)
     *
     * @param novelId
     * @return
     */
    @Override
    public String novelDelete(Integer novelId) {
        NovelEntity entity = novelMapper.selectById(novelId);
        String data;
        if (entity == null) {
            data = "Novel ID = [" + novelId + "]不存在!";
        } else if (!getUserData().getAccount().equals(entity.getNovelAuthor())) {
            data = "不是作者无权删除!";
        } else {
            // 状态改为已删除，已删除的改为不存在
//            if (entity.getStatus().equals(StateEnum.DELETE.getValue())) {
//                entity.setStatus(StateEnum.NOTHING.getValue());
//            } else {
//                entity.setStatus(StateEnum.DELETE.getValue());
//            }
//            int res = novelMapper.updateById(entity);
            boolean res = this.removeById(entity);
            if (res) {
                data = "1";
            } else {
                data = "操作失败";
            }
        }
        return data;
    }

    /**
     * 下载文档
     *
     * @param id
     * @return
     */
    @Override
    public String downloadDoc(Integer id) {
        NovelEntity novelEntity = novelMapper.selectById(id);
        if (novelEntity == null) {
            return null;
        }
        String htmlBody = novelEntity.getNovelContent();
        if (NovelTypeEnum.Markdown.toString().equals(novelEntity.getNovelType())) {
            MarkdownProcessor markdownProcessor = new MarkdownProcessor();
            htmlBody = markdownProcessor.markdown(htmlBody);
        }

        String filePath = WordUtil.html2Word(htmlBody);
        String url = cosClientUtil.uploadFile2Cos(new File(filePath), "noveldoc");

        LOG.info("用户[" + getUserData().getAccount() + "]下载 Novel，ID=[" + id + "]，URL=[" + url + "]");
        return url;
    }
}
