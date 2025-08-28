package util;

import com.intellij.ide.FileEditorProvider;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import constants.ProjectTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * 插件工具类
 */
public class PluginUtil {


    /**
     * 在 explorer（资源管理器） 打开
     * @param filePath 指定待打开的文件路径
     */
    public static void openInExplorer(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                // 显示一个简单的“信息”对话框
                Messages.showErrorDialog("The file does not exist: " + filePath, "Error");
                throw new NullPointerException("classFile 为空对象，可能文件不存在：" + filePath);
            }
            Runtime.getRuntime().exec("explorer /select, " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在右侧的导航栏中打开指定路径的文件
     * @param project 项目环境
     * @param filePath 指定待打开的文件路径
     */
    public static void openInProjectView(Project project, String filePath) {
        try {
            // 找到该文件则打开该文件，在编辑器中打开
            File f = new File(filePath);
            VirtualFile classFile = LocalFileSystem.getInstance().findFileByPath(f.getAbsolutePath());
            if (classFile == null) {
                // 显示一个简单的“信息”对话框
                Messages.showErrorDialog("The file does not exist: " + f.getAbsolutePath(), "Error");
                throw new NullPointerException("classFile 为空对象，可能文件不存在：" + f.getAbsolutePath());
            }
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, classFile);
            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            // 同时在左侧的导航树视图也展开
            SelectInContext context = new SelectInContext() {
                @Override
                public @NotNull Project getProject() {
                    return project;
                }

                @Override
                public @NotNull VirtualFile getVirtualFile() {
                    return classFile;
                }

                @Override
                public @Nullable Object getSelectorInFile() {
                    return null;
                }

                @Override
                public @Nullable FileEditorProvider getFileEditorProvider() {
                    return null;
                }
            };
            for (SelectInTarget target : SelectInManager.getInstance(project).getTargetList()) {
                if (target.canSelect(context)) {
                    target.selectIn(context, true);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断当前项目的类型
     * @param project 项目
     * @return 表示项目类型的枚举类ProjectTypeEnum
     */
    public static ProjectTypeEnum getProjectType(Project project) {
        if (isNotMavenProject(project)) {
            return ProjectTypeEnum.NOT_MAVEN;
        } else if (isSingleModuleMavenProject(project)) {
            return ProjectTypeEnum.SINGLE_MODULE_MAVEN;
        } else if (isMultiModuleMavenProject(project)) {
            return ProjectTypeEnum.MULTI_MODULE_MAVEN;
        } else {
            return null;
        }
    }

    /**
     * 判断当前项目是否不是maven项目
     * @param project 项目
     * @return 如果不是maven项目则返回true，否则返回false
     */
    private static boolean isNotMavenProject(Project project) {
        // 得到项目的根目录路径
        String projectPath = project.getPresentableUrl();

        // 判断是否是非maven项目
        String outDirPath = projectPath + "/out/production";
        File outFile = new File(outDirPath);
        if (outFile.exists() && outFile.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前项目是否是单模块maven项目
     * @param project 项目
     * @return 如果是单模块maven项目则返回true，否则返回false
     */
    private static boolean isSingleModuleMavenProject(Project project) {
        // 得到项目的根目录路径
        String projectPath = project.getPresentableUrl();

        // 判断是否是单模块maven项目
        String targetDirPath = projectPath + "/target/classes";
        File targetFile = new File(targetDirPath);
        if (targetFile.exists() && targetFile.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前项目是否是多模块maven项目
     * @param project 项目
     * @return 如果是多模块maven项目则返回true，否则返回false
     */
    private static boolean isMultiModuleMavenProject(Project project) {
        // 判断是否是多模块maven项目
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length >= 1 && !isSingleModuleMavenProject(project)) {
            return true;
        }
        return false;
    }
}