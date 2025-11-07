# WebSocket分片上传使用说明

## 功能概述

系统支持通过WebSocket实时推送文件上传进度，实现以下功能：
- ✅ 分片上传（支持大文件）
- ✅ 秒传（MD5检查）
- ✅ 断点续传
- ✅ 实时进度推送
- ✅ 批量上传并发控制（最多5个文件同时上传）
- ✅ 多云存储平台适配（本地存储、阿里云OSS、MinIO等）

## 后端接口

### 1. WebSocket连接
```
ws://your-domain/ws/upload?userId={userId}
```

### 2. HTTP接口

#### 2.1 初始化上传
```http
POST /apis/transfer/init
Content-Type: application/json

{
  "fileName": "test.mp4",
  "fileSize": 104857600,
  "fileMd5": "abc123...",
  "parentId": "parent-dir-id",  // 可选，父目录ID
  "totalChunks": 100,
  "chunkSize": 1048576,  // 1MB
  "mimeType": "video/mp4"
}
```

**响应示例（秒传）：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "instant": true,
    "message": "秒传成功",
    "fileInfo": {
      "id": "file-id-xxx",
      "fileName": "test.mp4",
      ...
    }
  }
}
```

**响应示例（需要上传）：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "instant": false,
    "message": "请开始上传",
    "task": {
      "taskId": "upload-id-xxx",
      "fileName": "test.mp4",
      "totalChunks": 100,
      "uploadedChunks": 0,
      "status": "uploading"
    }
  }
}
```

#### 2.2 上传分片
```http
POST /apis/transfer/chunk
Content-Type: multipart/form-data

file: <binary-data>
taskId: upload-id-xxx
chunkIndex: 0
chunkMd5: chunk-md5-xxx
```

#### 2.3 查询已上传分片（用于断点续传）
```http
GET /apis/transfer/chunks/{taskId}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": [0, 1, 2, 5, 8]  // 已上传的分片索引
}
```

#### 2.4 合并分片
```http
POST /apis/transfer/merge/{taskId}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": "file-id-xxx",
    "fileName": "test.mp4",
    "size": 104857600,
    ...
  }
}
```

## WebSocket消息协议

### 客户端 -> 服务端

#### 订阅任务进度
```json
{
  "action": "subscribe",
  "taskId": "upload-id-xxx"
}
```

#### 取消订阅
```json
{
  "action": "unsubscribe",
  "taskId": "upload-id-xxx"
}
```

#### 心跳
```json
{
  "action": "ping"
}
```

### 服务端 -> 客户端

#### 进度推送
```json
{
  "type": "progress",
  "taskId": "upload-id-xxx",
  "data": {
    "uploadedChunks": 50,
    "totalChunks": 100,
    "uploadedSize": 52428800,
    "totalSize": 104857600,
    "progress": 50.0,
    "speed": 1048576,      // 字节/秒
    "remainTime": 50       // 剩余秒数
  }
}
```

#### 完成通知
```json
{
  "type": "complete",
  "taskId": "upload-id-xxx",
  "data": "file-id-xxx",
  "message": "上传成功"
}
```

#### 错误通知
```json
{
  "type": "error",
  "taskId": "upload-id-xxx",
  "message": "分片上传失败: ..."
}
```

#### 心跳响应
```json
{
  "type": "pong"
}
```

## 前端使用示例

### JavaScript/TypeScript

```javascript
class FileUploader {
  constructor(userId) {
    this.userId = userId;
    this.ws = null;
    this.tasks = new Map(); // taskId -> task info
  }

  // 连接WebSocket
  connect() {
    this.ws = new WebSocket(`ws://your-domain/ws/upload?userId=${this.userId}`);
    
    this.ws.onopen = () => {
      console.log('WebSocket连接成功');
      // 启动心跳
      this.startHeartbeat();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error);
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket连接关闭');
      this.stopHeartbeat();
    };
  }

  // 处理WebSocket消息
  handleMessage(message) {
    switch (message.type) {
      case 'progress':
        this.onProgress(message.taskId, message.data);
        break;
      case 'complete':
        this.onComplete(message.taskId, message.data);
        break;
      case 'error':
        this.onError(message.taskId, message.message);
        break;
      case 'pong':
        console.log('收到心跳响应');
        break;
    }
  }

  // 心跳
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ action: 'ping' }));
      }
    }, 30000); // 30秒一次
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
    }
  }

  // 订阅任务进度
  subscribe(taskId) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        action: 'subscribe',
        taskId: taskId
      }));
    }
  }

  // 上传文件
  async uploadFile(file, parentId = null) {
    // 1. 计算文件MD5
    const fileMd5 = await this.calculateMD5(file);
    
    // 2. 初始化上传
    const chunkSize = 1024 * 1024; // 1MB
    const totalChunks = Math.ceil(file.size / chunkSize);
    
    const initResponse = await fetch('/apis/transfer/init', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fileName: file.name,
        fileSize: file.size,
        fileMd5: fileMd5,
        parentId: parentId,
        totalChunks: totalChunks,
        chunkSize: chunkSize,
        mimeType: file.type
      })
    });
    
    const result = await initResponse.json();
    
    // 3. 检查是否秒传
    if (result.data.instant) {
      console.log('秒传成功！');
      return result.data.fileInfo;
    }
    
    // 4. 订阅进度
    const taskId = result.data.task.taskId;
    this.subscribe(taskId);
    
    // 5. 查询已上传分片（断点续传）
    const uploadedChunks = await this.getUploadedChunks(taskId);
    
    // 6. 上传分片
    for (let i = 0; i < totalChunks; i++) {
      // 跳过已上传的分片
      if (uploadedChunks.includes(i)) {
        console.log(`分片 ${i} 已上传，跳过`);
        continue;
      }
      
      const start = i * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      const chunk = file.slice(start, end);
      
      // 计算分片MD5
      const chunkMd5 = await this.calculateMD5(chunk);
      
      // 上传分片
      await this.uploadChunk(taskId, i, chunk, chunkMd5);
    }
    
    // 7. 合并分片
    const fileInfo = await this.mergeChunks(taskId);
    return fileInfo;
  }

  // 查询已上传分片
  async getUploadedChunks(taskId) {
    const response = await fetch(`/apis/transfer/chunks/${taskId}`);
    const result = await response.json();
    return result.data || [];
  }

  // 上传分片
  async uploadChunk(taskId, chunkIndex, chunk, chunkMd5) {
    const formData = new FormData();
    formData.append('file', chunk);
    formData.append('taskId', taskId);
    formData.append('chunkIndex', chunkIndex);
    formData.append('chunkMd5', chunkMd5);
    
    const response = await fetch('/apis/transfer/chunk', {
      method: 'POST',
      body: formData
    });
    
    return await response.json();
  }

  // 合并分片
  async mergeChunks(taskId) {
    const response = await fetch(`/apis/transfer/merge/${taskId}`, {
      method: 'POST'
    });
    
    const result = await response.json();
    return result.data;
  }

  // 计算MD5
  async calculateMD5(blob) {
    // 使用 spark-md5 或其他MD5库
    // 这里仅为示例
    return 'mock-md5-' + Date.now();
  }

  // 进度回调
  onProgress(taskId, progress) {
    console.log(`任务 ${taskId} 进度: ${progress.progress.toFixed(2)}%`);
    console.log(`速度: ${(progress.speed / 1024 / 1024).toFixed(2)} MB/s`);
    console.log(`剩余时间: ${progress.remainTime} 秒`);
  }

  // 完成回调
  onComplete(taskId, fileId) {
    console.log(`任务 ${taskId} 上传完成！文件ID: ${fileId}`);
    // 如果需要文件详细信息，可以通过fileId调用接口获取
  }

  // 错误回调
  onError(taskId, message) {
    console.error(`任务 ${taskId} 失败: ${message}`);
  }
}

// 使用示例
const uploader = new FileUploader('user-123');
uploader.connect();

// 监听文件选择
document.getElementById('fileInput').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (file) {
    try {
      const fileInfo = await uploader.uploadFile(file);
      console.log('文件上传成功:', fileInfo);
    } catch (error) {
      console.error('文件上传失败:', error);
    }
  }
});
```

## 并发控制

系统使用 `UploadTaskManager` 限制最多同时上传5个文件。如果超过限制，需要等待其他任务完成。

前端可以实现任务队列：

```javascript
class UploadQueue {
  constructor(uploader, maxConcurrent = 5) {
    this.uploader = uploader;
    this.maxConcurrent = maxConcurrent;
    this.queue = [];
    this.running = 0;
  }

  async add(file, parentId = null) {
    return new Promise((resolve, reject) => {
      this.queue.push({ file, parentId, resolve, reject });
      this.process();
    });
  }

  async process() {
    while (this.running < this.maxConcurrent && this.queue.length > 0) {
      const task = this.queue.shift();
      this.running++;
      
      try {
        const result = await this.uploader.uploadFile(task.file, task.parentId);
        task.resolve(result);
      } catch (error) {
        task.reject(error);
      } finally {
        this.running--;
        this.process(); // 继续处理队列
      }
    }
  }
}

// 使用
const queue = new UploadQueue(uploader);
files.forEach(file => {
  queue.add(file).then(result => {
    console.log('上传成功:', result);
  });
});
```

## 注意事项

1. **MD5计算**：前端需要使用 `spark-md5` 等库计算文件和分片的MD5
2. **分片大小**：建议1-5MB，太小会增加请求次数，太大会影响断点续传效果
3. **超时重试**：建议实现分片上传失败的重试机制
4. **WebSocket重连**：断线后需要自动重连
5. **并发控制**：遵守服务端的并发限制（最多5个）
6. **清理任务**：上传完成后可以选择清理 `file_upload_task` 和 `file_upload_chunk` 表的临时数据

## 数据库表设计

### file_upload_task 表
```sql
CREATE TABLE file_upload_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL COMMENT '任务ID（等于uploadId）',
  user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
  parent_id VARCHAR(64) COMMENT '父目录ID',
  object_key VARCHAR(512) NOT NULL COMMENT '对象键',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_size BIGINT NOT NULL COMMENT '文件大小',
  file_md5 VARCHAR(64) NOT NULL COMMENT '文件MD5',
  suffix VARCHAR(32) COMMENT '文件后缀',
  mime_type VARCHAR(128) COMMENT 'MIME类型',
  total_chunks INT NOT NULL COMMENT '总分片数',
  uploaded_chunks INT DEFAULT 0 COMMENT '已上传分片数',
  chunk_size BIGINT NOT NULL COMMENT '分片大小',
  storage_platform_setting_id VARCHAR(64) NOT NULL COMMENT '存储平台配置ID',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  error_msg TEXT COMMENT '错误信息',
  start_time DATETIME COMMENT '开始时间',
  complete_time DATETIME COMMENT '完成时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_task_id (task_id),
  KEY idx_user_id (user_id)
);
```

### file_upload_chunk 表
```sql
CREATE TABLE file_upload_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
  chunk_index INT NOT NULL COMMENT '分片索引',
  chunk_md5 VARCHAR(64) NOT NULL COMMENT '分片MD5',
  chunk_size BIGINT NOT NULL COMMENT '分片大小',
  chunk_path VARCHAR(512) COMMENT '分片路径（本地存储使用）',
  etag VARCHAR(128) COMMENT '分片ETag（云存储返回）',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  upload_time DATETIME COMMENT '上传完成时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_chunk (task_id, chunk_index),
  KEY idx_task_id (task_id)
);
```

