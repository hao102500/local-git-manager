
package com.localgit.controller;

import com.localgit.repository.RepositoryManagementService;
import com.localgit.repository.GitRepositoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final GitRepositoryService gitRepositoryService;
    private final RepositoryManagementService repositoryManagementService;

    public RepositoryController(
            GitRepositoryService gitRepositoryService,
            RepositoryManagementService repositoryManagementService) {

        this.gitRepositoryService = gitRepositoryService;
        this.repositoryManagementService = repositoryManagementService;
    }

    /**
     * 验证本地 Git 仓库
     */
    @PostMapping("/verify")
    public Map<String, Object> verify(
            @RequestBody Map<String, String> request) {

        String path = request.get("path");

        Map<String, Object> data = gitRepositoryService.verify(path);

        Map<String, Object> result = new LinkedHashMap<>();

        if (Boolean.TRUE.equals(data.get("valid"))) {
            result.put("code", 200);
            result.put("message", "Git 仓库验证成功");
        } else {
            result.put("code", 400);
            result.put("message", data.get("message"));
        }

        result.put("data", data);

        return result;
    }

    /**
     * 添加本地 Git 仓库
     */
    @PostMapping("/add")
    public Map<String, Object> add(
            @RequestBody Map<String, String> request) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String path = request.get("path");
            String description = request.get("description");

            Map<String, Object> data =
                    repositoryManagementService.add(path, description);

            result.put("code", 200);
            result.put("message", "仓库添加成功");
            result.put("data", data);

        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            result.put("data", null);

        } catch (Exception e) {
            e.printStackTrace();

            result.put("code", 500);
            result.put("message", "服务器内部错误");
            result.put("data", null);
        }

        return result;
    }

    /**
     * 查询仓库列表
     */
    @GetMapping
    public Map<String, Object> list() {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> data =
                    repositoryManagementService.list();

            result.put("code", 200);
            result.put("message", "仓库列表查询成功");
            result.put("data", data);

        } catch (Exception e) {
            e.printStackTrace();

            result.put("code", 500);
            result.put("message", "仓库列表查询失败");
            result.put("data", null);
        }

        return result;
    }

    /**
     * 根据 ID 查询仓库详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Map<String, Object> data =
                    repositoryManagementService.detail(id);

            if (data == null) {
                result.put("code", 404);
                result.put("message", "仓库记录不存在");
                result.put("data", null);
                return result;
            }

            result.put("code", 200);
            result.put("message", "仓库详情查询成功");
            result.put("data", data);

        } catch (Exception e) {
            e.printStackTrace();

            result.put("code", 500);
            result.put("message", "仓库详情查询失败");
            result.put("data", null);
        }

        return result;
    }

    /**
     * 查询仓库 Git 工作区状态
     */
    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable Long id) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Map<String, Object> data =
                    repositoryManagementService.status(id);

            result.put("code", 200);
            result.put("message", "Git 状态查询成功");
            result.put("data", data);

        } catch (IllegalArgumentException e) {
            result.put("code", 404);
            result.put("message", e.getMessage());
            result.put("data", null);

        } catch (Exception e) {
            e.printStackTrace();

            result.put("code", 500);
            result.put("message", "Git 状态查询失败");
            result.put("data", null);
        }

        return result;
    }

    /**
     * 查询指定文件的代码差异
     *
     * diffType:
     * WORKTREE：未暂存差异
     * STAGED：已暂存差异
     *
     * 默认 WORKTREE，兼容原有接口
     */
    @GetMapping("/{id}/diff")
    public Map<String, Object> diff(
            @PathVariable Long id,
            @RequestParam String filePath,
            @RequestParam(defaultValue = "WORKTREE") String diffType) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            if (filePath == null || filePath.isBlank()) {
                result.put("code", 400);
                result.put("message", "文件路径不能为空");
                result.put("data", null);
                return result;
            }

            String normalizedType = diffType.trim().toUpperCase();

            if (!"WORKTREE".equals(normalizedType)
                    && !"STAGED".equals(normalizedType)) {

                result.put("code", 400);
                result.put("message", "差异类型不正确，只支持 WORKTREE 或 STAGED");
                result.put("data", null);
                return result;
            }

            Map<String, Object> data =
                    repositoryManagementService.diff(
                            id,
                            filePath.trim(),
                            normalizedType
                    );

            result.put("code", 200);
            result.put("message", "代码差异查询成功");
            result.put("data", data);

        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            result.put("data", null);

        } catch (Exception e) {
            e.printStackTrace();

            result.put("code", 500);
            result.put("message", "代码差异查询失败：" + e.getMessage());
            result.put("data", null);
        }

        return result;
    }
}
