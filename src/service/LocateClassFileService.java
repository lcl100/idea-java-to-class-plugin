package service;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import constants.ProjectTypeEnum;
import util.PluginUtil;

@Service
public final class LocateClassFileService {

    public void locateClassFile(AnActionEvent event, boolean isOpenInProjectView, boolean isOpenInExplorer) {
        try {
            VirtualFile virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
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

            String classFilePath = null;
            switch (projectType) {
                case NOT_MAVEN:
                    classFilePath = currentFilePath.replaceAll("/", "\\\\").replace(projectPath.replaceAll("/", "\\\\") + "\\src\\", projectPath.replaceAll("/", "\\\\") + "\\out\\production\\" + projectName + "\\").replaceAll("\\.java", "\\.class");
                    break;
                case SINGLE_MODULE_MAVEN:
                    classFilePath = currentFilePath.replaceAll("/", "\\\\").replace(projectPath.replaceAll("/", "\\\\") + "\\src\\main\\java\\", projectPath.replaceAll("/", "\\\\") + "\\target\\classes\\").replaceAll("\\.java", "\\.class");
                    break;
                case MULTI_MODULE_MAVEN:
                    classFilePath = currentFilePath.replaceAll("/", "\\\\").replace("\\src\\main\\java\\", "\\target\\classes\\").replaceAll("\\.java", "\\.class");
                    break;
            }

            if (isOpenInProjectView) {
                PluginUtil.openInProjectView(project, classFilePath);
            }
            if (isOpenInExplorer) {
                PluginUtil.openInExplorer(classFilePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
