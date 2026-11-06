import React from "react";
import ReactDOM from "react-dom/client";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";

import App from "./App";
import "./styles/global.css";

const theme = {
  token: {
    colorPrimary: "#2563eb",
    colorSuccess: "#16a34a",
    colorWarning: "#d97706",
    colorError: "#dc2626",
    colorText: "#1e293b",
    colorTextSecondary: "#64748b",
    colorBorder: "#e2e8f0",
    colorBgLayout: "#f8fafc",
    colorBgContainer: "#ffffff",
    borderRadius: 8,
    fontSize: 13,
    controlHeight: 34,
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  },
  components: {
    Button: {
      borderRadius: 8,
      controlHeight: 34,
      primaryShadow: "none",
    },
    Input: {
      borderRadius: 8,
    },
    Card: {
      borderRadiusLG: 16,
    },
    Menu: {
      itemBorderRadius: 8,
      itemHeight: 42,
    },
  },
};

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={theme}>
      <App />
    </ConfigProvider>
  </React.StrictMode>,
);
