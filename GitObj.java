package entity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: xb.yang
 * @Date: 2024/06/13/14:18
 * @Description:
 */
public class GitObj {
    //反馈信息
    private static String msg = "OK";
    private static List<String> diffString = new ArrayList<>();

    //获取项目的git路径
    public static String getProjectPath() {
        Project currentProject = ProjectManager.getInstance().getOpenProjects()[0]; // 获取当前打开的第一个项目
        if (currentProject != null) {
            VirtualFile projectBaseDir = currentProject.getBaseDir();
            String projectPath = projectBaseDir.getPath();
            System.out.println("project path: " + projectPath);
            return projectPath;
        } else {
            msg = "no open project!";
            return null;
        }
    }

    //打开git
    public static void doProcess() {
        String dir = getProjectPath();
        if (dir == null) {
            return;
        }
        try {
            // 1. 打开.git文件
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(Paths.get(dir, ".git").toFile())
                    .build();

            // 2. 获取当前分支名称
            String currentBranch = repository.getBranch();

            ObjectId remoteMasterObjectId = repository.resolve("refs/remotes/origin/master");

            // 3. 执行git diff命令
            List<DiffEntry> diffs = getDiff(repository, currentBranch, remoteMasterObjectId);





            // 4. 将不同的地方写入文件
            writeDiffsToFile(diffs);

            repository.close();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }

    }

    private static List<DiffEntry> getDiff(Repository repository, String branch1, ObjectId remoteMasterObjectId) throws GitAPIException, IOException {
        try (Git git = new Git(repository)) {
            ObjectId objectId1 = repository.resolve("refs/heads/" + branch1);
            ObjectId objectId2 = remoteMasterObjectId;
            if (objectId1 == null || objectId2 == null) {
                msg = "One or both branch names are invalid.";
                return null; // or handle the error appropriately
            }
            return git.diff()
                    .setOldTree(prepareTreeParser(repository, objectId2))
                    .setNewTree(prepareTreeParser(repository, objectId1))
                    .call();
        }
    }

    private static void writeDiffsToFile(List<DiffEntry> diffs) {
        if (diffs == null) {
            msg = "不存在不同，无需比较！";
        }
        for (DiffEntry entry : diffs) {
            diffString.add(entry.getNewPath());
        }
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevTree tree = walk.parseTree(objectId);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree);
            }
            walk.dispose();
            return treeParser;
        }
    }

    public static String getMsg() {
        return msg;
    }

    public static List<String> getDiffString() {
        return diffString;
    }
}
