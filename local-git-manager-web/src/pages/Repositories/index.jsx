import React, { useMemo, useState } from "react";
import {
  Button,
  Divider,
  Segmented,
  Table,
  Tag,
  Space,
  Tooltip,
  message,
} from "antd";

import {
  FolderGit2,
  Plus,
  GitBranch,
  History,
  ExternalLink,
} from "lucide-react";

import "./index.css";

// 模拟仓库数据，后续替换为 Java 后端接口
const mockRepositories = [
  {
    id: "1",
    name: "salary-manager",
    path: "/Users/hao/code/salary-manager",
    branch: "main",
    status: "modified",
    changes: 3,
    commitMessage: "feat: 新增考勤统计功能",
    commitTime: "2026-11-05 10:32",
  },
  {
    id: "2",
    name: "release-manager",
    path: "/Users/hao/code/release-manager",
    branch: "develop",
    status: "clean",
    changes: 0,
    commitMessage: "fix: 优化发布记录查询",
    commitTime: "2026-11-04 16:20",
  },
];

function Repositories() {
  const [messageApi, contextHolder] = message.useMessage();

  const [filter, setFilter] = useState("all");

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });

  // 根据筛选条件过滤仓库
  const filteredData = useMemo(() => {
    if (filter === "modified") {
      return mockRepositories.filter((item) => item.status === "modified");
    }

    if (filter === "clean") {
      return mockRepositories.filter((item) => item.status === "clean");
    }

    return mockRepositories;
  }, [filter]);

  // 切换筛选时重置页码
  const handleFilterChange = (value) => {
    setFilter(value);
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  };

  // 添加仓库
  const handleAddRepository = () => {
    messageApi.info("下一步将接入本地仓库导入功能");
  };

  // 查看仓库
  const handleView = (record) => {
    messageApi.info(`查看仓库：${record.name}`);
  };

  // 查看提交历史
  const handleHistory = (record) => {
    messageApi.info(`查看提交历史：${record.name}`);
  };

  const columns = [
    {
      title: "仓库名称与路径",
      dataIndex: "name",
      key: "name",
      width: "30%",
      render: (_, record) => (
        <div className="repository-name-cell">
          <div className="repository-name-icon">
            <FolderGit2 size={17} />
          </div>

          <div className="repository-name-info">
            <div className="repository-name">{record.name}</div>
            <Tooltip title={record.path}>
              <div className="repository-path">{record.path}</div>
            </Tooltip>
          </div>
        </div>
      ),
    },
    {
      title: "当前分支",
      dataIndex: "branch",
      key: "branch",
      width: "14%",
      render: (value) => (
        <span className="repository-branch">
          <GitBranch size={14} />
          {value}
        </span>
      ),
    },
    {
      title: "工作区状态",
      dataIndex: "status",
      key: "status",
      width: "14%",
      render: (_, record) =>
        record.status === "modified" ? (
          <Tag color="warning">有 {record.changes} 处修改</Tag>
        ) : (
          <Tag color="success">工作区干净</Tag>
        ),
    },
    {
      title: "最近提交",
      dataIndex: "commitMessage",
      key: "commitMessage",
      width: "23%",
      render: (_, record) => (
        <div className="repository-commit">
          <div className="repository-commit-message">
            {record.commitMessage}
          </div>
          <div className="repository-commit-time">{record.commitTime}</div>
        </div>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      align: "right",
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => handleView(record)}>
            查看
          </Button>

          <Button
            type="link"
            size="small"
            onClick={() => handleHistory(record)}
          >
            历史
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="page-container">
      {contextHolder}

      {/* 页面标题 */}
      <div className="page-header">
        <div>
          <h1 className="page-title">本地仓库</h1>

          <div className="page-description">
            管理本机上的 Git 仓库，查看状态、分支和提交历史。
          </div>
        </div>

        <Button
          type="primary"
          icon={<Plus size={16} />}
          onClick={handleAddRepository}
        >
          添加仓库
        </Button>
      </div>

      {/* <Divider /> */}

      {/* 仓库列表 */}
      <div className="repository-table-card">
        {/* 仓库筛选 */}
        <div className="repository-toolbar">
          {/* <div className="repository-filter"> */}
          <Segmented
            value={filter}
            onChange={handleFilterChange}
            options={[
              { label: "全部仓库", value: "all" },
              { label: "有修改", value: "modified" },
              { label: "工作区干净", value: "clean" },
            ]}
          />
          {/* </div> */}
        </div>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredData}
          size="middle"
          scroll={{ x: 1200, y: 400 }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: filteredData.length,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个仓库`,
            pageSizeOptions: [10, 20, 50],
          }}
          onChange={(nextPagination) => {
            setPagination({
              current: nextPagination.current,
              pageSize: nextPagination.pageSize,
            });
          }}
          locale={{
            emptyText: "暂无本地仓库",
          }}
        />
      </div>
    </div>
  );
}

export default Repositories;
