
package com.localgit.repository;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitRepositoryService {

    public Map<String, Object> verify(String path) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            File workTree = new File(path);

            if (!workTree.exists() || !workTree.isDirectory()) {
                result.put("valid", false);
                result.put("message", "目录不存在");
                return result;
            }

            try (Repository repository = openRepository(workTree)) {
                result.put("valid", true);
                result.put("path", workTree.getCanonicalPath());
                result.put("branch", repository.getBranch());
                result.put("gitDirectory", repository.getDirectory().getCanonicalPath());
                result.put("message", "Git 仓库验证成功");
            }

        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getStatus(String path) {
        Map<String, Object> result = new LinkedHashMap<>();

        try (Repository repository = openRepository(new File(path));
             Git git = new Git(repository)) {

            Status status = git.status().call();

            result.put("clean", status.isClean());
            result.put("stagedAdded", new ArrayList<>(status.getAdded()));
            result.put("stagedModified", new ArrayList<>(status.getChanged()));
            result.put("stagedDeleted", new ArrayList<>(status.getRemoved()));
            result.put("modified", new ArrayList<>(status.getModified()));
            result.put("deleted", new ArrayList<>(status.getMissing()));
            result.put("untracked", new ArrayList<>(status.getUntracked()));
            result.put("conflicting", new ArrayList<>(status.getConflicting()));
            result.put("message", status.isClean() ? "工作区干净" : "存在文件变更");

            return result;

        } catch (Exception e) {
            throw new RuntimeException("获取仓库状态失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询指定文件的代码差异。
     *
     * WORKTREE：Index -> Working Tree
     * STAGED：HEAD -> Index
     */
    public Map<String, Object> getDiff(
            String path,
            String filePath,
            String diffType) {

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        String type = diffType == null ? "WORKTREE" : diffType.trim().toUpperCase();

        if (!"WORKTREE".equals(type) && !"STAGED".equals(type)) {
            throw new IllegalArgumentException("差异类型不正确，只支持 WORKTREE 或 STAGED");
        }

        try (Repository repository = openRepository(new File(path))) {

            String relativePath = filePath.trim().replace("\\", "/");

            if (relativePath.startsWith("/")) {
                throw new IllegalArgumentException("请使用仓库内的相对文件路径");
            }

            Path workTreePath = repository.getWorkTree()
                    .getCanonicalFile()
                    .toPath();

            Path targetPath = workTreePath
                    .resolve(relativePath)
                    .normalize()
                    .toFile()
                    .getCanonicalFile()
                    .toPath();

            if (!targetPath.startsWith(workTreePath)
                    || targetPath.equals(workTreePath)) {
                throw new IllegalArgumentException("文件路径超出仓库范围");
            }

            boolean worktreeExists = Files.exists(targetPath);

            if (worktreeExists && !Files.isRegularFile(targetPath)) {
                throw new IllegalArgumentException("目标路径不是普通文件");
            }

            DirCache dirCache = repository.readDirCache();

            ObjectId indexObjectId = null;
            boolean hasIndexEntry = false;
            boolean hasConflict = false;

            int index = dirCache.findEntry(relativePath);

            if (index >= 0) {
                for (int i = index; i < dirCache.getEntryCount(); i++) {
                    DirCacheEntry entry = dirCache.getEntry(i);

                    if (!relativePath.equals(entry.getPathString())) {
                        break;
                    }

                    if (entry.getStage() != 0) {
                        hasConflict = true;
                    } else {
                        hasIndexEntry = true;
                        indexObjectId = entry.getObjectId();
                    }
                }
            }

            if (hasConflict) {
                throw new IllegalArgumentException("冲突文件暂不支持差异查询");
            }

            byte[] oldBytes;
            byte[] newBytes;

            boolean oldExists;
            boolean newExists;

            try (ObjectReader reader = repository.newObjectReader()) {

                if ("WORKTREE".equals(type)) {

                    // Index -> Working Tree
                    oldExists = hasIndexEntry;
                    newExists = worktreeExists;

                    oldBytes = hasIndexEntry
                            ? reader.open(indexObjectId, Constants.OBJ_BLOB).getBytes()
                            : new byte[0];

                    newBytes = worktreeExists
                            ? Files.readAllBytes(targetPath)
                            : new byte[0];

                } else {

                    // HEAD -> Index
                    ObjectId headObjectId = getHeadFileObjectId(
                            repository,
                            relativePath
                    );

                    oldExists = headObjectId != null;
                    newExists = hasIndexEntry;

                    oldBytes = oldExists
                            ? reader.open(headObjectId, Constants.OBJ_BLOB).getBytes()
                            : new byte[0];

                    newBytes = hasIndexEntry
                            ? reader.open(indexObjectId, Constants.OBJ_BLOB).getBytes()
                            : new byte[0];
                }
            }

            // 两侧都不存在，说明当前差异范围内没有文件变化
            if (!oldExists && !newExists) {
//                throw new IllegalArgumentException("指定文件在当前差异范围内不存在");
                Map<String, Object> result = new LinkedHashMap<>();

                result.put("filePath", relativePath);
                result.put("diffType", type);
                result.put("diffCount", 0);
                result.put("diffs", new ArrayList<>());

                return result;
            }

            // 二进制文件单独处理，不生成文本 Diff
            if (isBinary(oldBytes) || isBinary(newBytes)) {

                // 两侧文件内容完全一致，不存在差异
                if (oldExists == newExists && Arrays.equals(oldBytes, newBytes)) {
                    Map<String, Object> result = new LinkedHashMap<>();

                    result.put("filePath", relativePath);
                    result.put("diffType", type);
                    result.put("diffCount", 0);
                    result.put("diffs", new ArrayList<>());

                    return result;
                }

                Map<String, Object> item = new LinkedHashMap<>();

                String changeType;

                if (!oldExists) {
                    changeType = "ADD";
                } else if (!newExists) {
                    changeType = "DELETE";
                } else {
                    changeType = "MODIFY";
                }

                item.put("oldPath", oldExists ? relativePath : null);
                item.put("newPath", newExists ? relativePath : null);
                item.put("changeType", changeType);

                item.put("isBinary", true);
                item.put("addedLines", 0);
                item.put("deletedLines", 0);
                item.put("hunks", new ArrayList<>());
                item.put("lines", new ArrayList<>());
                item.put("patch", "Binary files differ");

                List<Map<String, Object>> diffs = new ArrayList<>();
                diffs.add(item);

                Map<String, Object> result = new LinkedHashMap<>();

                result.put("filePath", relativePath);
                result.put("diffType", type);
                result.put("diffCount", diffs.size());
                result.put("diffs", diffs);

                return result;
            }

            RawText oldText = new RawText(oldBytes);
            RawText newText = new RawText(newBytes);

            DiffAlgorithm algorithm = new HistogramDiff();

            EditList edits = algorithm.diff(
                    RawTextComparator.DEFAULT,
                    oldText,
                    newText
            );

            List<Map<String, Object>> hunks = buildHunks(
                    oldText,
                    newText,
                    edits,
                    3
            );

            int addedLines = 0;
            int deletedLines = 0;

            List<String> lines = new ArrayList<>();

            for (Map<String, Object> hunk : hunks) {

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hunkLines =
                        (List<Map<String, Object>>) hunk.get("lines");

                for (Map<String, Object> line : hunkLines) {

                    String lineType = (String) line.get("type");
                    String content = (String) line.get("content");

                    if ("ADD".equals(lineType)) {
                        addedLines++;
                        lines.add("+" + content);
                    } else if ("DELETE".equals(lineType)) {
                        deletedLines++;
                        lines.add("-" + content);
                    } else {
                        lines.add(" " + content);
                    }
                }
            }

            List<Map<String, Object>> diffs = new ArrayList<>();

            if (!edits.isEmpty()) {

                Map<String, Object> item = new LinkedHashMap<>();

                String changeType;

                if (!oldExists) {
                    changeType = "ADD";
                } else if (!newExists) {
                    changeType = "DELETE";
                } else {
                    changeType = "MODIFY";
                }

                item.put("oldPath", oldExists ? relativePath : null);
                item.put("newPath", newExists ? relativePath : null);
                item.put("changeType", changeType);
                item.put("addedLines", addedLines);
                item.put("deletedLines", deletedLines);
                item.put("hunks", hunks);
                item.put("lines", lines);
                item.put("patch", buildPatch(
                        relativePath,
                        hunks,
                        oldExists,
                        newExists
                ));
                item.put("isBinary", false);

                diffs.add(item);
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("filePath", relativePath);
            result.put("diffType", type);
            result.put("diffCount", diffs.size());
            result.put("diffs", diffs);

            return result;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读取文件差异失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从 HEAD 提交树中读取指定文件的 Blob ID。
     * 如果仓库尚无提交，或者 HEAD 中没有该文件，返回 null。
     */
    private ObjectId getHeadFileObjectId(
            Repository repository,
            String relativePath) throws Exception {

        ObjectId headCommitId = repository.resolve(Constants.HEAD);

        if (headCommitId == null) {
            return null;
        }

        try (RevWalk revWalk = new RevWalk(repository)) {

            RevTree headTree = revWalk.parseCommit(headCommitId).getTree();

            try (TreeWalk treeWalk = TreeWalk.forPath(
                    repository,
                    relativePath,
                    headTree)) {

                if (treeWalk == null) {
                    return null;
                }

                return treeWalk.getObjectId(0).copy();
            }
        }
    }


    /**
     * 判断文件内容是否为二进制。
     */
    private boolean isBinary(byte[] content) {

        if (content == null || content.length == 0) {
            return false;
        }

        int length = Math.min(content.length, 8192);

        // 检查 NUL 字节
        for (int i = 0; i < length; i++) {
            if (content[i] == 0) {
                return true;
            }
        }

        // 检查 UTF-8 编码是否合法
        try {
            StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content, 0, length));
        } catch (CharacterCodingException e) {
            return true;
        }

        // 检查异常控制字符
        int controlCount = 0;

        for (int i = 0; i < length; i++) {

            int value = content[i] & 0xFF;

            if (value < 0x20
                    && value != '\t'
                    && value != '\n'
                    && value != '\r'
                    && value != '\f') {
                controlCount++;
            }
        }

        return controlCount * 100 > length;
    }

    private List<Map<String, Object>> buildHunks(
            RawText oldText,
            RawText newText,
            EditList edits,
            int context) {

        List<Map<String, Object>> result = new ArrayList<>();

        if (edits.isEmpty()) {
            return result;
        }

        List<HunkRange> ranges = new ArrayList<>();

        for (Edit edit : edits) {

            if (!ranges.isEmpty()) {

                HunkRange last = ranges.get(ranges.size() - 1);

                int gapA = edit.getBeginA() - last.lastEndA;
                int gapB = edit.getBeginB() - last.lastEndB;

                if (gapA <= context * 2 && gapB <= context * 2) {
                    last.lastEndA = edit.getEndA();
                    last.lastEndB = edit.getEndB();
                    continue;
                }
            }

            ranges.add(new HunkRange(
                    edit.getBeginA(),
                    edit.getEndA(),
                    edit.getBeginB(),
                    edit.getEndB()
            ));
        }

        for (HunkRange range : ranges) {

            int beginA = Math.max(0, range.firstBeginA - context);
            int endA = Math.min(oldText.size(), range.lastEndA + context);

            int beginB = Math.max(0, range.firstBeginB - context);
            int endB = Math.min(newText.size(), range.lastEndB + context);

            List<Map<String, Object>> hunkLines = new ArrayList<>();

            int oldPosition = beginA;
            int newPosition = beginB;

            for (Edit edit : edits) {

                if (edit.getBeginA() < range.firstBeginA
                        || edit.getBeginA() > range.lastEndA) {
                    continue;
                }

                while (oldPosition < edit.getBeginA()
                        && newPosition < edit.getBeginB()) {

                    addDiffLine(
                            hunkLines,
                            "CONTEXT",
                            oldText.getString(oldPosition),
                            oldPosition + 1,
                            newPosition + 1
                    );

                    oldPosition++;
                    newPosition++;
                }

                for (int i = edit.getBeginA(); i < edit.getEndA(); i++) {

                    addDiffLine(
                            hunkLines,
                            "DELETE",
                            oldText.getString(i),
                            i + 1,
                            null
                    );

                    oldPosition++;
                }

                for (int i = edit.getBeginB(); i < edit.getEndB(); i++) {

                    addDiffLine(
                            hunkLines,
                            "ADD",
                            newText.getString(i),
                            null,
                            i + 1
                    );

                    newPosition++;
                }
            }

            while (oldPosition < endA && newPosition < endB) {

                addDiffLine(
                        hunkLines,
                        "CONTEXT",
                        oldText.getString(oldPosition),
                        oldPosition + 1,
                        newPosition + 1
                );

                oldPosition++;
                newPosition++;
            }

            int oldCount = endA - beginA;
            int newCount = endB - beginB;

            int oldStart = getHunkStart(beginA, oldCount);
            int newStart = getHunkStart(beginB, newCount);

            Map<String, Object> hunk = new LinkedHashMap<>();

            hunk.put("oldStart", oldStart);
            hunk.put("oldCount", oldCount);
            hunk.put("newStart", newStart);
            hunk.put("newCount", newCount);

            hunk.put(
                    "header",
                    "@@ -" + oldStart + "," + oldCount
                            + " +" + newStart + "," + newCount + " @@"
            );

            hunk.put("lines", hunkLines);

            result.add(hunk);
        }

        return result;
    }

    private void addDiffLine(
            List<Map<String, Object>> lines,
            String type,
            String content,
            Integer oldLineNumber,
            Integer newLineNumber) {

        Map<String, Object> line = new LinkedHashMap<>();

        line.put("type", type);
        line.put("content", content);
        line.put("oldLineNumber", oldLineNumber);
        line.put("newLineNumber", newLineNumber);

        lines.add(line);
    }

    private int getHunkStart(int begin, int count) {
        return count == 0 ? begin : begin + 1;
    }

    private String buildPatch(
            String filePath,
            List<Map<String, Object>> hunks,
            boolean oldExists,
            boolean newExists) {

        StringBuilder patch = new StringBuilder();

        patch.append(oldExists ? "--- a/" + filePath : "--- /dev/null")
                .append("\n");

        patch.append(newExists ? "+++ b/" + filePath : "+++ /dev/null")
                .append("\n");

        for (Map<String, Object> hunk : hunks) {

            patch.append(hunk.get("header")).append("\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lines =
                    (List<Map<String, Object>>) hunk.get("lines");

            for (Map<String, Object> line : lines) {

                String type = (String) line.get("type");
                String content = (String) line.get("content");

                if ("ADD".equals(type)) {
                    patch.append("+");
                } else if ("DELETE".equals(type)) {
                    patch.append("-");
                } else {
                    patch.append(" ");
                }

                patch.append(content).append("\n");
            }
        }

        return patch.toString();
    }

    private static class HunkRange {

        private final int firstBeginA;
        private int lastEndA;

        private final int firstBeginB;
        private int lastEndB;

        private HunkRange(
                int beginA,
                int endA,
                int beginB,
                int endB) {

            this.firstBeginA = beginA;
            this.lastEndA = endA;
            this.firstBeginB = beginB;
            this.lastEndB = endB;
        }
    }

    private Repository openRepository(File workTree) throws Exception {

        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        builder.findGitDir(workTree);

        if (builder.getGitDir() == null) {
            throw new IllegalArgumentException("不是有效的 Git 仓库");
        }

        return builder
                .setWorkTree(workTree.getCanonicalFile())
                .build();
    }
}
