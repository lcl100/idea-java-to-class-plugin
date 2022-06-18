package action;

import com.intellij.ide.FileEditorProvider;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaToClassAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);

        try {
            // 获取被选中的文件
            VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());

            // 获取被选中文件的扩展名，如果是目录则没有扩展名会返回 null
            String extension = file.getExtension();

            // 判断该文件是否以 java 结尾
            if (extension != null && extension.contains("java")) {
                // 获取该文件的文件名，不带扩展名
                String javaFileName = file.getNameWithoutExtension();
                // 拼接文件名和 .class，成为新的文件名，如 Test.class
                String classFileName = javaFileName + ".class";

                // 查找名为 classFileName 的文件
                // 获取项目的根目录
                String projectFilePath = project.getBasePath();
                List<String> paths = new ArrayList<>();
                // 判断 out 目录是否存在，通常普通项目生成的 class 文件就放在该目录下
                String outDirPath = projectFilePath + "/out/";
                File outDirFile = new File(outDirPath);
                if (outDirFile.exists()) {
                    // 获取 out 目录下的所有文件路径
                    getDirPaths(outDirPath, paths);
                }
                // 判断 target 目录是否存在，通常 maven 项目生成的 class 文件就放在该目录下
                String targetDirPath = projectFilePath + "/target";
                File targetDirFile = new File(targetDirPath);
                if (targetDirFile.exists()) {
                    // 获取 target 目录下的所有文件路径
                    getDirPaths(targetDirPath, paths);
                }

                // 循环判断所有路径，如果找到该文件则打开该文件
                for (String path : paths) {
                    // 只查找 .class 结尾的文件
                    if (path.endsWith(".class")) {
                        // 获取该文件的文件名
                        File f = new File(path);
                        String name = f.getName();
                        // 比较判断是否找到该文件
                        if (name.equals(classFileName)) {
                            // 找到该文件则打开该文件，在编辑器中打开
                            VirtualFile classFile = LocalFileSystem.getInstance().findFileByPath(f.getAbsolutePath());
                            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, classFile);
                            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
                            // 同时在左侧的导航树视图也展开
                            SelectInContext context = new SelectInContext() {
                                @Override
                                public @NotNull
                                Project getProject() {
                                    return project;
                                }

                                @Override
                                public @NotNull
                                VirtualFile getVirtualFile() {
                                    return classFile;
                                }

                                @Override
                                public @Nullable
                                Object getSelectorInFile() {
                                    return null;
                                }

                                @Override
                                public @Nullable
                                FileEditorProvider getFileEditorProvider() {
                                    return null;
                                }
                            };
                            for (SelectInTarget target : SelectInManager.getInstance(project).getTargets()) {
                                if (target.canSelect(context)) {
                                    target.selectIn(context, true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定目录 dir 下的所有文件路径，存放在 paths 集合中
     *
     * @param dir 指定目录
     */
    private void getDirPaths(String dir, List<String> paths) {
        File file = new File(dir);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    getDirPaths(f.getAbsolutePath(), paths);
                } else {
                    paths.add(f.getAbsolutePath());
                }
            }
        }
    }
}