package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区域内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<Map<String, Object>> turnoverData = orderMapper.sumGroupByDate(beginTime, endTime, Orders.COMPLETED);

        Map<LocalDate, Double> turnoverMap = new HashMap<>();
        for (Map<String, Object> row : turnoverData) {
            LocalDate date = null;
            if (row.get("date") instanceof java.sql.Date) {
                date = ((java.sql.Date) row.get("date")).toLocalDate();
            } else if (row.get("date") instanceof LocalDate) {
                date = (LocalDate) row.get("date");
            }
            Double amount = ((Number) row.get("amount")).doubleValue();
            turnoverMap.put(date, amount);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            turnoverList.add(turnoverMap.getOrDefault(date, 0.0));
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区域内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<Map<String, Object>> totalUserData = userMapper.countTotalGroupByDate(endTime);
        Map<LocalDate, Integer> totalUserDailyMap = new HashMap<>();
        for (Map<String, Object> row : totalUserData) {
            LocalDate date = null;
            if (row.get("date") instanceof java.sql.Date) {
                date = ((java.sql.Date) row.get("date")).toLocalDate();
            } else if (row.get("date") instanceof LocalDate) {
                date = (LocalDate) row.get("date");
            }
            Integer count = ((Number) row.get("count")).intValue();
            totalUserDailyMap.put(date, count);
        }

        List<Map<String, Object>> newUserData = userMapper.countNewGroupByDate(beginTime, endTime);
        Map<LocalDate, Integer> newUserMap = new HashMap<>();
        for (Map<String, Object> row : newUserData) {
            LocalDate date = null;
            if (row.get("date") instanceof java.sql.Date) {
                date = ((java.sql.Date) row.get("date")).toLocalDate();
            } else if (row.get("date") instanceof LocalDate) {
                date = (LocalDate) row.get("date");
            }
            Integer count = ((Number) row.get("count")).intValue();
            newUserMap.put(date, count);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        int runningTotal = 0;
        for (LocalDate date : dateList) {
            int newUsers = newUserMap.getOrDefault(date, 0);
            runningTotal += totalUserDailyMap.getOrDefault(date, 0);
            newUserList.add(newUsers);
            totalUserList.add(runningTotal);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }


    /**
     * 统计指定时间区域内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<Map<String, Object>> orderCountData = orderMapper.countGroupByDate(beginTime, endTime);
        Map<LocalDate, Integer> orderCountMap = new HashMap<>();
        for (Map<String, Object> row : orderCountData) {
            LocalDate date = null;
            if (row.get("date") instanceof java.sql.Date) {
                date = ((java.sql.Date) row.get("date")).toLocalDate();
            } else if (row.get("date") instanceof LocalDate) {
                date = (LocalDate) row.get("date");
            }
            Integer count = ((Number) row.get("count")).intValue();
            orderCountMap.put(date, count);
        }

        List<Map<String, Object>> validOrderCountData = orderMapper.countByStatusGroupByDate(beginTime, endTime, Orders.COMPLETED);
        Map<LocalDate, Integer> validOrderCountMap = new HashMap<>();
        for (Map<String, Object> row : validOrderCountData) {
            LocalDate date = null;
            if (row.get("date") instanceof java.sql.Date) {
                date = ((java.sql.Date) row.get("date")).toLocalDate();
            } else if (row.get("date") instanceof LocalDate) {
                date = (LocalDate) row.get("date");
            }
            Integer count = ((Number) row.get("count")).intValue();
            validOrderCountMap.put(date, count);
        }

        List<Integer> validOrderCountList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            orderCountList.add(orderCountMap.getOrDefault(date, 0));
            validOrderCountList.add(validOrderCountMap.getOrDefault(date, 0));
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    /**
     * 获统计指定时间区域内销量top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }


    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
             XSSFWorkbook excel = new XSSFWorkbook(in)) {

            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue("时间: " + dateBegin + "至" + dateEnd);

            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("导出运营数据报表失败：{}", e.getMessage());
        }
    }



    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 获取从begin到end的日期列表（包含begin和end）
     * @param begin 开始日期
     * @param end 结束日期
     * @return 日期列表
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
