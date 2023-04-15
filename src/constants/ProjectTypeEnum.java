package constants;

/**
 * 项目类型，分为三类
 */
public enum ProjectTypeEnum {
    NOT_MAVEN("非maven项目"),
    SINGLE_MODULE_MAVEN("单模块maven项目"),
    MULTI_MODULE_MAVEN("多模块maven项目");

    private String type;

    ProjectTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
