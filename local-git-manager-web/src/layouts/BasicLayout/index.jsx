import { Layout, Menu, Avatar, Dropdown, Tooltip } from "antd";

import {
  LayoutDashboard,
  FolderGit2,
  Settings,
  GitBranch,
  ChevronDown,
  PanelLeftClose,
  PanelLeftOpen,
  GitCommitHorizontal,
} from "lucide-react";

import { useLocation, useNavigate, Outlet } from "react-router-dom";
import { useState } from "react";

const { Sider, Header, Content, Footer } = Layout;

const menuItems = [
  {
    key: "/dashboard",
    icon: <LayoutDashboard size={17} />,
    label: "工作台",
  },
  {
    key: "/repositories",
    icon: <FolderGit2 size={17} />,
    label: "本地仓库",
  },
  {
    key: "/settings",
    icon: <Settings size={17} />,
    label: "系统设置",
  },
];

function BasicLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const currentMenu = menuItems.find((item) =>
    location.pathname.startsWith(item.key),
  );

  const dropdownItems = [
    {
      key: "settings",
      label: "系统设置",
      icon: <Settings size={15} />,
      onClick: () => navigate("/settings"),
    },
  ];

  return (
    <Layout className="app-layout">
      <Sider
        width={232}
        collapsedWidth={72}
        collapsed={collapsed}
        trigger={null}
        className="app-sider"
      >
        <div className={`${collapsed ? "brand-center" : "brand"}`}>
          <div className="brand-left">
            {!collapsed && (
              <div className="brand-logo">
                <GitBranch size={14} />
              </div>
            )}

            {!collapsed && (
              <div className="brand-info">
                <div className="brand-name">Git Manager</div>
              </div>
            )}
          </div>
          <div className="brand-right">
            <button
              className="collapse-button"
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? (
                <PanelLeftOpen size={17} />
              ) : (
                <PanelLeftClose size={17} />
              )}
            </button>
          </div>
        </div>

        <div className="sidebar-section">
          {!collapsed && <div className="sidebar-title">工作空间</div>}

          <Menu
            mode="inline"
            selectedKeys={[currentMenu?.key || "/dashboard"]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            className="app-menu"
          />
        </div>

        <div className="sidebar-bottom">
          {!collapsed && (
            <div className="sidebar-tip">
              <div className="sidebar-tip-icon">
                <GitCommitHorizontal size={17} />
              </div>
              <div>
                <div className="sidebar-tip-title">本地优先</div>
                <div className="sidebar-tip-desc">你的代码始终保存在本地</div>
              </div>
            </div>
          )}
        </div>
      </Sider>

      <Layout className="main-layout">
        <Header className="app-header">
          <div className="header-left">
            <div className="breadcrumb">
              <span>Local Git Manager</span>
              <span className="breadcrumb-divider">/</span>
              <strong>{currentMenu?.label || "工作台"}</strong>
            </div>
          </div>

          <div className="header-right">
            <div className="local-status">
              <span className="status-dot" />
              本地模式
            </div>

            <Dropdown menu={{ items: dropdownItems }} placement="bottomRight">
              <div className="user-profile">
                <Avatar
                  size={30}
                  style={{ background: "#dbeafe", color: "#2563eb" }}
                >
                  H
                </Avatar>
                {/* <span>本地用户</span> */}
                {/* <ChevronDown size={15} /> */}
              </div>
            </Dropdown>
          </div>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>

        {/* <Footer className="app-footer">Footer</Footer> */}
      </Layout>
    </Layout>
  );
}

export default BasicLayout;
