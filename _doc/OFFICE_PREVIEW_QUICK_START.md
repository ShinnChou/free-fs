# Office 文档预览 - 快速开始

## 当前状态

✅ **已完成**：
- Word/Excel/PPT 文件类型识别
- 预览策略配置（`PdfPreviewStrategy`）
- 预览模板（复用 `pdf.html`）

⚠️ **待实现**：
- Office 转 PDF 的实际转换逻辑

## 快速实现方案

### 方案 1：LibreOffice 转换（推荐）

**优点**：免费、开源、稳定

#### 步骤 1：安装 LibreOffice

```bash
# Ubuntu/Debian
sudo apt install libreoffice

# CentOS/RHEL
sudo yum install libreoffice

# Windows
# 下载安装：https://www.libreoffice.org/download/
```

#### 步骤 2：配置 application.yml

```yaml
fs:
  preview:
    # 启用 Office 转换
    enable-office-convert: true
    
    # LibreOffice 路径
    libre-office-path: /usr/bin/soffice  # Linux
    # libre-office-path: C:/Program Files/LibreOffice/program/soffice.exe  # Windows
    
    # 转换超时（秒）
    libre-office-timeout: 60
```

#### 步骤 3：使用转换服务

我已经为你创建了 `OfficeConverterService`，使用示例：

```java
@Autowired
private OfficeConverterService converterService;

// 转换 Office 文档为 PDF
byte[] pdfBytes = converterService.convertToPdf(inputStream, "document.docx");
```

---

### 方案 2：微软 Office Online Viewer（最简单）

**优点**：无需安装、无需转换、实现简单

**缺点**：需要文件可公网访问

#### 实现方式

修改 `pdf.html` 模板，对于 Office 文档使用 iframe：

```html
<!-- 判断是否为 Office 文档 -->
<div th:if="${needConvert}">
    <iframe th:src="'https://view.officeapps.live.com/op/view.aspx?src=' + ${streamUrl}" 
            width="100%" 
            height="100%"
            frameborder="0">
    </iframe>
</div>

<!-- PDF 直接预览 -->
<div th:unless="${needConvert}">
    <iframe id="pdf-iframe"></iframe>
    <!-- 原有的 PDF 预览逻辑 -->
</div>
```

---

### 方案 3：前端 JS 库预览（无需后端转换）

#### 3.1 使用 Mammoth.js（Word）

```html
<!-- 引入 Mammoth.js -->
<script src="https://cdn.jsdelivr.net/npm/mammoth@1.6.0/mammoth.browser.min.js"></script>

<script>
// 预览 Word 文档
fetch(streamUrl)
    .then(response => response.arrayBuffer())
    .then(arrayBuffer => mammoth.convertToHtml({arrayBuffer: arrayBuffer}))
    .then(result => {
        document.getElementById('content').innerHTML = result.value;
    });
</script>
```

#### 3.2 使用 SheetJS（Excel）

```html
<!-- 引入 SheetJS -->
<script src="https://cdn.sheetjs.com/xlsx-latest/package/dist/xlsx.full.min.js"></script>

<script>
// 预览 Excel 文档
fetch(streamUrl)
    .then(response => response.arrayBuffer())
    .then(data => {
        const workbook = XLSX.read(data, {type: 'array'});
        const html = XLSX.utils.sheet_to_html(workbook.Sheets[workbook.SheetNames[0]]);
        document.getElementById('content').innerHTML = html;
    });
</script>
```

---

## 推荐实现顺序

### 阶段 1：临时方案（1 小时）

使用**微软 Office Online Viewer**，快速实现预览功能：

1. 修改 `pdf.html` 添加 Office Online iframe
2. 确保文件 URL 可公网访问
3. 测试预览效果

### 阶段 2：正式方案（1 天）

实现 **LibreOffice 转换**：

1. 安装 LibreOffice
2. 集成 `OfficeConverterService`
3. 添加缓存机制
4. 性能测试和优化

### 阶段 3：优化方案（可选）

1. 异步转换队列
2. 转换进度显示
3. 分布式缓存
4. 监控和告警

---

## 完整示例代码

### 使用 LibreOffice 转换

```java
@GetMapping("/preview/{fileId}")
public ResponseEntity<?> preview(@PathVariable String fileId) {
    FileInfo file = fileInfoService.getById(fileId);
    FileTypeEnum fileType = FileTypeEnum.fromFileName(file.getDisplayName());
    
    // 判断是否需要转换
    if (fileType.isNeedConvert()) {
        try {
            // 获取原始文件流
            InputStream inputStream = storageService.getFileStream(file.getObjectKey());
            
            // 转换为 PDF
            byte[] pdfBytes = converterService.convertToPdf(inputStream, file.getDisplayName());
            
            // 返回 PDF
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
                
        } catch (Exception e) {
            log.error("Office 转换失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("文档转换失败");
        }
    }
    
    // 其他文件类型正常处理
    return streamFullFile(file);
}
```

---

## 性能对比

| 方案 | 首次加载 | 二次加载 | 服务器负载 | 格式保真 |
|------|---------|---------|-----------|---------|
| LibreOffice 转换 | 5-10秒 | 即时（缓存） | 高 | ⭐⭐⭐⭐ |
| Office Online | 即时 | 即时 | 无 | ⭐⭐⭐⭐⭐ |
| 前端 JS 库 | 即时 | 即时 | 无 | ⭐⭐⭐ |

---

## 常见问题

### Q1: 为什么选择转 PDF 而不是直接渲染？

**A**: PDF 格式统一、兼容性好、可以复用现有的 PDF.js 预览组件。

### Q2: LibreOffice 转换慢怎么办？

**A**: 
1. 启用缓存，避免重复转换
2. 使用异步转换，显示进度条
3. 限制文件大小
4. 考虑使用 Aspose（商业方案，速度更快）

### Q3: 能否支持在线编辑？

**A**: 预览功能不支持编辑。如需编辑，建议集成：
- ONLYOFFICE Document Server
- Collabora Online
- Microsoft Office 365

---

## 下一步

1. 选择一个方案实现
2. 测试各种 Office 文档格式
3. 性能测试和优化
4. 添加监控和日志

详细文档请参考：`OFFICE_PREVIEW.md`

