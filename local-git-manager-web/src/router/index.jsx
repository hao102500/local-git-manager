import { Navigate, Route, Routes } from "react-router-dom";

import BasicLayout from "../layouts/BasicLayout";
import Dashboard from "../pages/Dashboard";
import Repositories from "../pages/Repositories";
import Settings from "../pages/Settings";

function AppRoutes() {
  return (
    <Routes>
      <Route element={<BasicLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/repositories" element={<Repositories />} />
        <Route path="/settings" element={<Settings />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default AppRoutes;
