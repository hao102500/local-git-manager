
package com.localgit.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RepositoryManagementService {

    private final JdbcTemplate jdbcTemplate;
    private final GitRepositoryService gitRepositoryService;

    public RepositoryManagementService(
            JdbcTemplate jdbcTemplate,
            GitRepositoryService gitRepositoryService) {

        this.jdbcTemplate = jdbcTemplate;
        this.gitRepositoryService = gitRepositoryService;
    }

    /**
     * 添加本地 Git 仓库
     */
    public Map<String, Object> add(String path, String description) {

        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("仓库路径不能为空");
        }

        try {
            String canonicalPath = new File(path.trim()).getCanonicalPath();

            Map<String, Object> verifyResult =
                    gitRepositoryService.verify(canonicalPath);

            if (!Boolean.TRUE.equals(verifyResult.get("valid"))) {
                throw new IllegalArgumentException(
                        String.valueOf(verifyResult.get("message"))
                );
            }

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM repositories WHERE path = ?",
                    Integer.class,
                    canonicalPath
            );

            if (count != null && count > 0) {
                throw new IllegalArgumentException("该 Git 仓库已经添加过了");
            }

            String name = String.valueOf(verifyResult.get("name"));
            String now = LocalDateTime.now().toString();

            String sql = """
                    INSERT INTO repositories
                    (name, path, description, default_branch, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            jdbcTemplate.update(
                    sql,
                    name,
                    canonicalPath,
                    description,
                    null,
                    now,
                    now
            );

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("id", jdbcTemplate.queryForObject(
                    "SELECT last_insert_rowid()",
                    Long.class
            ));
            result.put("name", name);
            result.put("path", canonicalPath);
            result.put("description", description);
            result.put("currentBranch", verifyResult.get("branch"));
            result.put("createdAt", now);

            return result;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("添加仓库失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询所有已登记的 Git 仓库
     */
    public List<Map<String, Object>> list() {

        String sql = """
                SELECT
                    id,
                    name,
                    path,
                    description,
                    default_branch AS defaultBranch,
                    created_at AS createdAt,
                    updated_at AS updatedAt
                FROM repositories
                ORDER BY id DESC
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 根据 ID 查询仓库详情
     */
    public Map<String, Object> detail(Long id) {

        String sql = """
                SELECT
                    id,
                    name,
                    path,
                    description,
                    default_branch AS defaultBranch,
                    created_at AS createdAt,
                    updated_at AS updatedAt
                FROM repositories
                WHERE id = ?
                """;

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(sql, id);

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> result =
                new LinkedHashMap<>(rows.get(0));

        String path = String.valueOf(result.get("path"));

        Map<String, Object> gitInfo =
                gitRepositoryService.verify(path);

        boolean gitAvailable =
                Boolean.TRUE.equals(gitInfo.get("valid"));

        result.put("gitAvailable", gitAvailable);

        if (gitAvailable) {
            result.put("currentBranch", gitInfo.get("branch"));
            result.put("gitDirectory", gitInfo.get("gitDirectory"));
        } else {
            result.put("currentBranch", null);
            result.put("gitDirectory", null);
            result.put("gitError", gitInfo.get("message"));
        }

        return result;
    }

    /**
     * 根据仓库 ID 查询 Git 工作区状态
     */
    public Map<String, Object> status(Long id) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT path FROM repositories WHERE id = ?",
                id
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("仓库记录不存在");
        }

        String path = String.valueOf(rows.get(0).get("path"));

        Map<String, Object> result =
                gitRepositoryService.getStatus(path);

        result.put("repositoryId", id);
        result.put("path", path);

        return result;
    }

    /**
     * 查询指定仓库文件的代码差异
     *
     * WORKTREE：Index -> Working Tree
     * STAGED：HEAD -> Index
     */
    public Map<String, Object> diff(
            Long id,
            String filePath,
            String diffType) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT path FROM repositories WHERE id = ?",
                id
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("仓库记录不存在");
        }

        String path = String.valueOf(rows.get(0).get("path"));

        Map<String, Object> result =
                gitRepositoryService.getDiff(path, filePath, diffType);

        result.put("repositoryId", id);

        return result;
    }
}
