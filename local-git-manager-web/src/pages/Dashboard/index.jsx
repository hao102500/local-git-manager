import { Card, Button, Empty } from "antd";
import {
  FolderGit2,
  Plus,
  GitBranch,
  Clock3,
  History,
  ArrowRight,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import "./index.css";

const statistics = [
  {
    label: "本地仓库",
    value: "0",
    description: "已纳入管理的仓库",
    icon: <FolderGit2 size={19} />,
    color: "#2563eb",
    background: "#eff6ff",
  },
  {
    label: "本地分支",
    value: "0",
    description: "所有仓库的分支总数",
    icon: <GitBranch size={19} />,
    color: "#7c3aed",
    background: "#f5f3ff",
  },
  {
    label: "近期提交",
    value: "0",
    description: "最近的代码提交记录",
    icon: <History size={19} />,
    color: "#0891b2",
    background: "#ecfeff",
  },
  {
    label: "最近操作",
    value: "--",
    description: "暂无操作记录",
    icon: <Clock3 size={19} />,
    color: "#d97706",
    background: "#fffbeb",
  },
];

function Dashboard() {
  const navigate = useNavigate();

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">工作台</h1>
          <div className="page-description">
            管理本地 Git 仓库，跟踪代码变更与版本历史。
          </div>
        </div>

        <Button
          type="primary"
          icon={<Plus size={16} />}
          onClick={() => navigate("/repositories")}
        >
          添加仓库
        </Button>
      </div>

      <div className="statistics-grid">
        {statistics.map((item) => (
          <Card key={item.label} className="stat-card">
            <div className="stat-top">
              <span className="stat-label">{item.label}</span>
              <span
                className="stat-icon"
                style={{
                  color: item.color,
                  background: item.background,
                }}
              >
                {item.icon}
              </span>
            </div>

            <div className="stat-value">{item.value}</div>
            <div className="stat-description">{item.description}</div>
          </Card>
        ))}
      </div>

      <div className="dashboard-section">
        <div className="section-heading">
          <div>
            <h2>最近管理的仓库</h2>
            <p>快速访问你最近使用的本地代码仓库</p>
          </div>

          <Button
            type="link"
            icon={<ArrowRight size={15} />}
            iconPlacement="end"
            onClick={() => navigate("/repositories")}
          >
            查看全部
          </Button>
        </div>

        <Card className="repository-empty-card">
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <div>
                <div className="empty-title">还没有添加任何仓库</div>
                <div className="empty-description">
                  添加已有 Git 仓库，或选择一个本地目录初始化仓库。
                </div>
              </div>
            }
          >
            <Button
              type="primary"
              icon={<Plus size={15} />}
              onClick={() => navigate("/repositories")}
            >
              添加第一个仓库
            </Button>
          </Empty>
        </Card>
      </div>
    </div>
  );
}

export default Dashboard;
