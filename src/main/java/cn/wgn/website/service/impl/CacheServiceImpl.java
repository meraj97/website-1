package cn.wgn.website.service.impl;

import cn.wgn.website.entity.CallEntity;
import cn.wgn.website.entity.Dictionary;
import cn.wgn.website.entity.VistorEntity;
import cn.wgn.website.mapper.CallMapper;
import cn.wgn.website.mapper.DictionaryMapper;
import cn.wgn.website.mapper.VistorMapper;
import cn.wgn.website.service.ICacheService;
import cn.wgn.website.utils.DateUtil;
import cn.wgn.website.utils.FileUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.abel533.echarts.*;
import com.github.abel533.echarts.axis.CategoryAxis;
import com.github.abel533.echarts.axis.ValueAxis;
import com.github.abel533.echarts.code.*;
import com.github.abel533.echarts.feature.MagicType;
import com.github.abel533.echarts.series.Bar;
import com.github.abel533.echarts.series.Line;
import com.github.abel533.echarts.style.ItemStyle;
import com.github.abel533.echarts.style.itemstyle.Normal;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Redis 缓存服务
 *
 * @author WuGuangNuo
 * @date Created in 2020/3/8 13:11
 */
@Service("cacheService")
public class CacheServiceImpl implements ICacheService {
    @Autowired
    private CallMapper callMapper;
    @Autowired
    private VistorMapper vistorMapper;
    @Autowired
    private DictionaryMapper dictionaryMapper;
    @Autowired
    private FileUtil fileUtil;

    /**
     * 获取菜单列表
     * {
     * "code":"所需权限",
     * "icon":"图标",
     * "url":"相对链接",
     * "name":"菜单名称"
     * }
     *
     * @return
     */
    @Override
    @Cacheable(value = "MenuJson", key = "")
    public String getMenuJson() {
        return fileUtil.getMenuJson();
    }

    /**
     * 获取首页接口访问图表
     *
     * @return
     */
    @Override
    @Cacheable(value = "HomeChart", key = "")
    public String getHomeChart() {
        List<CallEntity> callEntityList = callMapper.selectList(
                new QueryWrapper<CallEntity>().lambda()
                        .select(CallEntity::getLk, CallEntity::getTm)
                        .between(CallEntity::getTm, LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(6), LocalDateTime.now())
        );

        // 日期坐标轴
        String[] yAxis = new String[7];
        for (int i = 0; i < yAxis.length; i++) {
            yAxis[i] = LocalDate.now().minusDays(yAxis.length - 1 - i).toString();
        }

        // LinkedHashMap<接口名称，LinkedHashMap<日期，数量>>
        LinkedHashMap<String, LinkedHashMap<String, Integer>> data = new LinkedHashMap<>();
        for (CallEntity e : callEntityList) {
            String date = e.getTm().toLocalDate().toString();
            if (data.containsKey(e.getLk())) {
                int num = data.get(e.getLk()).getOrDefault(date, 0) + 1;
                data.get(e.getLk()).put(date, num);
            } else {
                LinkedHashMap<String, Integer> temp = new LinkedHashMap<>();
                for (String yAxi : yAxis) {
                    temp.put(yAxi, 0);
                }
                temp.put(date, 1);
                data.put(e.getLk(), temp);
            }
        }

        Option option = new Option();
        option.title().text("最近七天接口调用统计").subtext("api.wuguangnuo.cn 数据更新时间:" + DateUtil.dateFormat(null, DateUtil.MINUTE_PATTERN)).left("3%");
        option.tooltip().trigger(Trigger.axis).axisPointer(new AxisPointer().type(PointerType.shadow));
        option.legend(new Legend().top("8%").data(data.keySet().toArray()));
        option.toolbox().show(true).right("3%").feature(Tool.mark, Tool.dataView,
                new MagicType(Magic.line, Magic.bar).show(true), Tool.restore, Tool.saveAsImage);
        option.calculable(true);
        option.xAxis(new CategoryAxis().data(yAxis));
        option.yAxis(new ValueAxis());
        option.grid(new Grid().top("20%").left("3%").right("3%").bottom("3%").containLabel(true));

        for (String key : data.keySet()) {
            option.series(new Bar(key)
                    .data(data.get(key).values().toArray())
                    .stack("总量")
                    .label(new ItemStyle().normal(new Normal().show(true).position(Position.inside))));
        }
        return JSON.toJSONString(option);
    }

    /**
     * 访客类型图表
     * 默认最近三月
     * wuguangnuo.cn
     *
     * @param dateTime1
     * @param dateTime2
     * @return
     */
    @Override
    @Cacheable(value = "VistorChart", key = "#dateTime1.toLocalDate()+'_'+#dateTime2.toLocalDate()")
    public String getVistorChart(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        List<VistorEntity> vistorEntityList = vistorMapper.selectList(
                new QueryWrapper<VistorEntity>().lambda()
                        .select(VistorEntity::getAg, VistorEntity::getTm)
                        .between(VistorEntity::getTm, dateTime1, dateTime2)
        );

        // 日期坐标轴
        String[] yAxis = new String[(int) Duration.between(dateTime1, dateTime2).toDays() + 1];
        for (int i = 0; i < yAxis.length; i++) {
            yAxis[i] = LocalDate.now().minusDays(yAxis.length - 1 - i).toString();
        }

        // 所有折线类型
        List<Dictionary> dictionaryList = dictionaryMapper.selectList(
                new QueryWrapper<Dictionary>().lambda()
                        .eq(Dictionary::getGroupKey, "vistor_show")
        );
//        List<String> vistorShow = dictionaryList.stream().map(
//                Dictionary::getCodeNote
//        ).collect(Collectors.toList());
        // all -> 全部
        Map<String, String> vistorShow = new HashMap<>();
        for (Dictionary d : dictionaryList) {
//            改表后删除这三个条件
            if (d.getCodeValue().equals("all") || d.getCodeValue().equals("vistor") || d.getCodeValue().equals("others")) {
                continue;
            }
            vistorShow.put(d.getCodeValue(), d.getCodeNote());
        }

        // LinkedHashMap<接口名称，LinkedHashMap<日期，数量>>
        LinkedHashMap<String, LinkedHashMap<String, Integer>> data = new LinkedHashMap<>();

        LinkedHashMap<String, Integer> zeroLine = new LinkedHashMap<>();
        for (String d : yAxis) {
            zeroLine.put(d, 0);
        }

        // 初始化接口名称
        data.put("总访问量", (LinkedHashMap<String, Integer>) zeroLine.clone());
        data.put("访客", (LinkedHashMap<String, Integer>) zeroLine.clone());
        for (String key : vistorShow.keySet()) {
            data.put(vistorShow.get(key), (LinkedHashMap<String, Integer>) zeroLine.clone());
        }
        data.put("其他流量", (LinkedHashMap<String, Integer>) zeroLine.clone());

        for (VistorEntity e : vistorEntityList) {
            String date = e.getTm().toLocalDate().toString();
            String ag = e.getAg();
            data.get("总访问量").put(date, data.get("总访问量").get(date) + 1);
            if (Strings.isNullOrEmpty(ag)) {
                continue;
            }

            // 正则检测蜘蛛
            if (ag.contains("spider") || ag.contains("bot")) {
                // 匹配字典中的蜘蛛类型
                boolean isNormalSpider = false;
                for (String key : vistorShow.keySet()) {
                    if (ag.contains(key)) {
                        int num = data.get(vistorShow.get(key)).get(date) + 1;
                        data.get(vistorShow.get(key)).put(date, num);
                        isNormalSpider = true;
                        break;
                    }
                }
                if (!isNormalSpider) {
                    data.get("其他流量").put(date, data.get("其他流量").get(date) + 1);
                }
            } else {
                data.get("访客").put(date, data.get("访客").get(date) + 1);
            }
        }

        Option option = new Option();
        option.title().text("访客类型统计图").subtext("api.wuguangnuo.cn 数据更新时间:" + DateUtil.dateFormat(null, DateUtil.MINUTE_PATTERN)).left("3%");
        option.tooltip().trigger(Trigger.axis);
        option.legend(new Legend().data(data.keySet().toArray()));
        // yaxisindex none
        option.toolbox().show(true).right("3%").feature(new DataZoom().yAxisIndex("none"), Tool.restore, Tool.dataView, Tool.saveAsImage);

        List<DataZoom> dataZoomList = new ArrayList<>();
        dataZoomList.add(new DataZoom().startValue(LocalDate.now().minusMonths(1)));
        dataZoomList.add(new DataZoom().type(DataZoomType.inside));
        option.setDataZoom(dataZoomList);
        option.xAxis(new CategoryAxis().data(yAxis).boundaryGap(false).type(AxisType.category));
        option.yAxis(new ValueAxis().type(AxisType.value));
        for (String key : data.keySet()) {
            option.series(new Line(key)
                    .data(data.get(key).values().toArray())
                    .name(key));
        }

        return JSON.toJSONString(option);
    }

    /**
     * 受访页面图表
     * 默认最近七天
     * wuguangnuo.cn
     *
     * @param dateTime1
     * @param dateTime2
     * @return
     */
    @Override
    @Cacheable(value = "LinkChart", key = "")
    public String getLinkChart(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return null;
    }

    /**
     * 操作系统图表
     * 默认最近七天
     * wuguangnuo.cn
     *
     * @param dateTime1
     * @param dateTime2
     * @return
     */
    @Override
    @Cacheable(value = "SystemChart", key = "")
    public String getSystemChart(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return null;
    }

    /**
     * 客户端图表
     * 默认最近七天
     * wuguangnuo.cn
     *
     * @param dateTime1
     * @param dateTime2
     * @return
     */
    @Override
    @Cacheable(value = "BrowserChart", key = "")
    public String getBrowserChart(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return null;
    }

}
