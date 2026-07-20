package com.example.mall.order;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class OrderExcelExporter {

    private static final String[] HEADERS = {
            "订单ID", "订单号", "商品名称", "商品ID列表", "总金额(分)", "创建时间", "状态", "用户ID"
    };

    public byte[] exportToByteArray(List<OrderWithProductsDTO> orders) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单数据");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            // 填充数据行
            int rowNum = 1;
            for (OrderWithProductsDTO order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getId() != null ? order.getId().doubleValue() : 0);
                row.createCell(1).setCellValue(order.getOrderSn() != null ? order.getOrderSn() : "");
                row.createCell(2).setCellValue(getProductNamesString(order));
                row.createCell(3).setCellValue(getProductIdsString(order));
                row.createCell(4).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");
                row.createCell(7).setCellValue(order.getUserId() != null ? order.getUserId().doubleValue() : 0);
            }

            // 自动调整列宽
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String getProductNamesString(OrderWithProductsDTO order) {
        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            return "";
        }
        return order.getProducts().stream()
                .map(ProductInfo::getName)
                .collect(Collectors.joining(", "));
    }

    private String getProductIdsString(OrderWithProductsDTO order) {
        if (order.getProductIds() == null || order.getProductIds().isEmpty()) {
            return "";
        }
        return order.getProductIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}