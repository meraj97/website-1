package cn.wgn.website.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 爬虫知乎热搜
 * </p>
 *
 * @author WuGuangNuo
 * @since 2020-06-21
 */
@Data
@Accessors(chain = true)
@TableName("bot_zhihuhot")
@ApiModel(value="ZhihuhotEntity对象", description="爬虫知乎热搜")
public class ZhihuhotEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "排名")
    private String ranking;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "分数")
    private String score;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
