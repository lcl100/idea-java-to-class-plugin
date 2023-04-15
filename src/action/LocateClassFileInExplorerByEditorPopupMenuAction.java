package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import service.LocateClassFileService;

/**
 * 从编辑器定位到当前 .java 文件所对应的 .class 在 explorer 打开
 */
public class LocateClassFileInExplorerByEditorPopupMenuAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        LocateClassFileService service = ApplicationManager.getApplication().getService(LocateClassFileService.class);
        service.locateClassFile(event, false, true);
    }
}
