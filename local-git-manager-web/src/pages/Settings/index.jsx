import { Card } from "antd";

function Settings() {
  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">系统设置</h1>
          <div className="page-description">
            管理应用的数据存储位置和运行配置。
          </div>
        </div>
      </div>

      <Card title="基础设置">
        <p>后续将在这里配置：</p>
        <ul>
          <li>应用数据存储目录</li>
          <li>Git 仓库默认扫描目录</li>
          <li>本地数据库位置</li>
          <li>应用运行日志</li>
        </ul>
      </Card>
    </div>
  );
}

export default Settings;
