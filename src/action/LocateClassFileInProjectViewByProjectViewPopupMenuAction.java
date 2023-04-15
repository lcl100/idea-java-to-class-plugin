package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import service.LocateClassFileService;

public class LocateClassFileInProjectViewByProjectViewPopupMenuAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        LocateClassFileService service = ApplicationManager.getApplication().getService(LocateClassFileService.class);
        service.locateClassFile(event, true, false);
    }
}
