package service;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import constants.ProjectTypeEnum;
import util.ClassFileLocator;
import util.PluginUtil;

import java.io.File;
import java.util.List;

@Service
public final class LocateClassFileService {

    public void locateClassFile(AnActionEvent event, boolean isOpenInProjectView, boolean isOpenInExplorer) {
        try {
            VirtualFile virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
            PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            if (virtualFile == null) return;

            // 当前编辑器中被选中的文件路径
            String currentFilePath = virtualFile.getPresentableUrl();

            // 获取项目类型
            Project project = event.getProject();
            ProjectTypeEnum projectType = PluginUtil.getProjectType(project);
            String projectPath = project.getPresentableUrl();
            String projectName = project.getName();

            if (!currentFilePath.endsWith(".java")) {
                throw new Exception("The current file is not .java file.");
            }
            if (projectType == null) {
                throw new Exception("The file type could not be solved.");
            }

            PsiJavaFile javaFile = (PsiJavaFile) psiFile;

            // 获取所有可能的class文件路径
            List<String> paths = ClassFileLocator.getClassFilePaths(project, javaFile);
            List<String> existingPaths = ClassFileLocator.getExistingClassFilePaths(project, javaFile);
            String mostLikelyPath = ClassFileLocator.getMostLikelyClassFilePath(project, javaFile);

            // 显示结果
            StringBuilder message = new StringBuilder();
            message.append("Java File: ").append(javaFile.getName()).append("\n\n");
            message.append("All Possible Paths:\n");
            paths.forEach(path -> message.append("• ").append(path).append("\n"));
            message.append("\nExisting Paths:\n");
            if (existingPaths.isEmpty()) {
                message.append("• No corresponding .class files found (may need to compile first)\n");
            } else {
                existingPaths.forEach(path -> message.append("• ").append(path).append("\n"));
            }
            message.append("\nMost Likely Path:\n");
            message.append("• ").append(mostLikelyPath != null ? mostLikelyPath : "Unknown");

            // 确定要使用的class文件路径
            String classFilePathToUse = determineClassFilePathEnhanced(currentFilePath, projectType, projectPath, projectName, mostLikelyPath, existingPaths);

            if (classFilePathToUse == null) {
                // 显示错误信息
                Messages.showErrorDialog("No existing .class file found. Please compile the project first.", "File Not Found");
                // 显示完整的消息
                Messages.showInfoMessage(message.toString(), "Class File Path Information");
                return;
            }

            // 统一使用正斜杠或反斜杠（根据系统）
            String normalizedPath = classFilePathToUse.replace("/", "\\");

            // 验证最终选择的路径是否存在
            if (!new File(normalizedPath).exists()) {
                Messages.showErrorDialog("The class file does not exist: " + normalizedPath, "File Not Found");
                return;
            }

            // 执行打开操作
            if (isOpenInProjectView) {
                PluginUtil.openInProjectView(project, normalizedPath);
            }
            if (isOpenInExplorer) {
                PluginUtil.openInExplorer(normalizedPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog("Error: " + e.getMessage(), "Error");
        }
    }

    /**
     * 确定要使用的class文件路径
     * 优先使用最可能路径，如果不存在则遍历其他存在的路径
     */
    private String determineClassFilePathToUse(String mostLikelyPath, List<String> existingPaths) {
        // 首先检查最可能路径是否存在
        if (mostLikelyPath != null && new File(mostLikelyPath).exists()) {
            return mostLikelyPath;
        }

        // 如果最可能路径不存在，遍历所有存在的路径
        if (existingPaths != null && !existingPaths.isEmpty()) {
            for (String path : existingPaths) {
                if (new File(path).exists()) {
                    return path; // 返回第一个存在的路径
                }
            }
        }

        // 如果都没有找到，返回null
        return null;
    }

    /**
     * 备用方法：使用项目类型推断路径（如果上面的方法找不到）
     */
    private String inferClassFilePathByProjectType(String currentFilePath, ProjectTypeEnum projectType, String projectPath, String projectName) {
        String classFilePath = null;

        switch (projectType) {
            case NOT_MAVEN:
                classFilePath = currentFilePath.replace("/", "\\").replace(projectPath.replace("/", "\\") + "\\src\\", projectPath.replace("/", "\\") + "\\out\\production\\" + projectName + "\\").replace(".java", ".class");
                break;
            case SINGLE_MODULE_MAVEN:
                classFilePath = currentFilePath.replace("/", "\\").replace(projectPath.replace("/", "\\") + "\\src\\main\\java\\", projectPath.replace("/", "\\") + "\\target\\classes\\").replace(".java", ".class");
                break;
            case MULTI_MODULE_MAVEN:
                classFilePath = currentFilePath.replace("/", "\\").replace("\\src\\main\\java\\", "\\target\\classes\\").replace(".java", ".class");
                break;
        }

        return classFilePath;
    }

    /**
     * 增强版路径确定方法，包含项目类型推断
     */
    private String determineClassFilePathEnhanced(String currentFilePath, ProjectTypeEnum projectType, String projectPath, String projectName, String mostLikelyPath, List<String> existingPaths) {
        // 1. 优先使用最可能路径
        if (mostLikelyPath != null && new File(mostLikelyPath).exists()) {
            return mostLikelyPath;
        }

        // 2. 遍历所有存在的路径
        if (existingPaths != null) {
            for (String path : existingPaths) {
                if (new File(path).exists()) {
                    return path;
                }
            }
        }

        // 3. 使用项目类型推断路径
        String inferredPath = inferClassFilePathByProjectType(currentFilePath, projectType, projectPath, projectName);
        if (inferredPath != null && new File(inferredPath).exists()) {
            return inferredPath;
        }

        // 4. 尝试其他常见的输出目录
        String[] commonOutputPatterns = {currentFilePath.replace(".java", ".class").replace("src\\main\\java\\", "target\\classes\\"), currentFilePath.replace(".java", ".class").replace("src\\", "out\\production\\" + projectName + "\\"), currentFilePath.replace(".java", ".class").replace("src\\main\\java\\", "build\\classes\\java\\main\\"), currentFilePath.replace(".java", ".class").replace("src\\", "bin\\")};

        for (String pattern : commonOutputPatterns) {
            if (new File(pattern).exists()) {
                return pattern;
            }
        }

        return null;
    }
}