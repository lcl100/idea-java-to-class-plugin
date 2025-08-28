package util;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lcl100
 * @create 2025-08-28 08:29
 */
public class ClassFileLocator {

    /**
     * 获取Java文件对应的所有可能class文件路径（兼容多模块、Maven、Gradle等）
     */
    @NotNull
    public static List<String> getClassFilePaths(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();

        // 方法1: 通过模块编译输出路径
        paths.addAll(getPathsFromModuleOutput(project, javaFile));

        // 方法2: 通过项目编译输出路径
        paths.addAll(getPathsFromProjectOutput(project, javaFile));

        // 方法3: 通过Maven/Gradle输出路径（如果适用）
        paths.addAll(getPathsFromBuildTools(project, javaFile));

        // 方法4: 通过源码相对路径计算
        paths.addAll(getPathsFromSourceRelative(project, javaFile));

        // 去重并过滤空值
        return paths.stream()
                .filter(Objects::nonNull)
                .filter(path -> !path.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 通过模块编译输出路径获取class文件路径
     */
    @NotNull
    private static List<String> getPathsFromModuleOutput(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();
        VirtualFile virtualFile = javaFile.getVirtualFile();
        if (virtualFile == null) return paths;

        Module module = ModuleManager.getInstance(project).findModuleByName(virtualFile.getName());
        if (module == null) {
            // 如果没有找到特定模块，尝试所有模块
            Module[] allModules = ModuleManager.getInstance(project).getModules();
            for (Module mod : allModules) {
                paths.addAll(getModuleClassPaths(mod, javaFile));
            }
        } else {
            paths.addAll(getModuleClassPaths(module, javaFile));
        }

        return paths;
    }

    /**
     * 获取单个模块的class文件路径
     */
    @NotNull
    private static List<String> getModuleClassPaths(@NotNull Module module, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();

        // 获取编译输出目录
        CompilerModuleExtension compilerExtension = CompilerModuleExtension.getInstance(module);
        if (compilerExtension != null) {
            // 生产代码输出目录
            VirtualFile outputDir = compilerExtension.getCompilerOutputPath();
            if (outputDir != null) {
                String path = buildClassFilePath(outputDir.getPath(), javaFile);
                if (path != null) paths.add(path);
            }

            // 测试代码输出目录
            VirtualFile testOutputDir = compilerExtension.getCompilerOutputPathForTests();
            if (testOutputDir != null) {
                String path = buildClassFilePath(testOutputDir.getPath(), javaFile);
                if (path != null) paths.add(path);
            }
        }

        // 检查模块的依赖输出
        paths.addAll(getDependencyOutputPaths(module, javaFile));

        return paths;
    }

    /**
     * 通过项目级别的输出路径获取
     */
    @NotNull
    private static List<String> getPathsFromProjectOutput(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();

        // 检查项目设置中的输出路径
        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        VirtualFile[] contentRoots = rootManager.getContentRoots();

        for (VirtualFile root : contentRoots) {
            // 常见的输出目录名称
            String[] outputDirs = {"out", "build", "target", "bin", "classes"};
            for (String dir : outputDirs) {
                VirtualFile outputDir = root.findChild(dir);
                if (outputDir != null && outputDir.exists()) {
                    String path = buildClassFilePath(outputDir.getPath(), javaFile);
                    if (path != null) paths.add(path);
                }
            }
        }

        return paths;
    }

    /**
     * 通过构建工具（Maven/Gradle）的输出路径获取
     */
    @NotNull
    private static List<String> getPathsFromBuildTools(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();
        VirtualFile virtualFile = javaFile.getVirtualFile();
        if (virtualFile == null) return paths;

        // 检查Maven标准结构
        if (isMavenProject(project)) {
            paths.addAll(getMavenClassPaths(virtualFile, javaFile));
        }

        // 检查Gradle标准结构
        if (isGradleProject(project)) {
            paths.addAll(getGradleClassPaths(virtualFile, javaFile));
        }

        return paths;
    }

    /**
     * 获取Maven项目的class文件路径
     */
    @NotNull
    private static List<String> getMavenClassPaths(@NotNull VirtualFile sourceFile, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();

        VirtualFile parent = sourceFile.getParent();
        while (parent != null) {
            if (isMavenSourceDirectory(parent)) {
                // 找到Maven源码目录，构建对应的输出路径
                String sourcePath = parent.getPath();
                String outputPath = sourcePath.replace("src/main/java", "target/classes")
                        .replace("src/test/java", "target/test-classes");

                String classPath = buildClassFilePath(outputPath, javaFile);
                if (classPath != null) paths.add(classPath);
                break;
            }
            parent = parent.getParent();
        }

        return paths;
    }

    /**
     * 获取Gradle项目的class文件路径
     */
    @NotNull
    private static List<String> getGradleClassPaths(@NotNull VirtualFile sourceFile, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();

        VirtualFile parent = sourceFile.getParent();
        while (parent != null) {
            if (isGradleSourceDirectory(parent)) {
                // 找到Gradle源码目录，构建对应的输出路径
                String sourcePath = parent.getPath();
                String outputPath = sourcePath.replace("src/main/java", "build/classes/java/main")
                        .replace("src/test/java", "build/classes/java/test");

                String classPath = buildClassFilePath(outputPath, javaFile);
                if (classPath != null) paths.add(classPath);
                break;
            }
            parent = parent.getParent();
        }

        return paths;
    }

    /**
     * 通过源码相对路径计算class文件路径
     */
    @NotNull
    private static List<String> getPathsFromSourceRelative(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();
        VirtualFile virtualFile = javaFile.getVirtualFile();
        if (virtualFile == null) return paths;

        Module module = ModuleManager.getInstance(project).findModuleByName(virtualFile.getName());
        if (module != null) {
            // 获取所有源码根目录
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
            for (VirtualFile sourceRoot : sourceRoots) {
                if (virtualFile.getPath().startsWith(sourceRoot.getPath())) {
                    // 计算相对路径
                    String relativePath = virtualFile.getPath().substring(sourceRoot.getPath().length());
                    if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);

                    // 转换为class文件路径（在各种可能的输出目录中查找）
                    String classRelativePath = relativePath.replace(".java", ".class");

                    // 尝试常见的输出目录模式
                    String[] outputPatterns = {
                            "out/production/classes",
                            "out/test/classes",
                            "build/classes",
                            "target/classes",
                            "bin",
                            "classes"
                    };

                    VirtualFile moduleRoot = getModuleRoot(module);
                    if (moduleRoot != null) {
                        for (String pattern : outputPatterns) {
                            String potentialPath = moduleRoot.getPath() + "/" + pattern + "/" + classRelativePath;
                            paths.add(potentialPath);
                        }
                    }
                    break;
                }
            }
        }

        return paths;
    }

    /**
     * 构建完整的class文件路径
     */
    @Nullable
    private static String buildClassFilePath(@NotNull String outputDir, @NotNull PsiJavaFile javaFile) {
        String packageName = javaFile.getPackageName();
        String className = javaFile.getName().replace(".java", ".class");

        if (packageName.isEmpty()) {
            return outputDir + File.separator + className;
        } else {
            String packagePath = packageName.replace('.', File.separatorChar);
            return outputDir + File.separator + packagePath + File.separator + className;
        }
    }

    /**
     * 获取模块依赖的输出路径
     */
    @NotNull
    private static List<String> getDependencyOutputPaths(@NotNull Module module, @NotNull PsiJavaFile javaFile) {
        List<String> paths = new ArrayList<>();
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

        for (OrderEntry orderEntry : rootManager.getOrderEntries()) {
            if (orderEntry instanceof ModuleOrderEntry) {
                // 处理模块依赖
                Module dependencyModule = ((ModuleOrderEntry) orderEntry).getModule();
                if (dependencyModule != null) {
                    paths.addAll(getModuleClassPaths(dependencyModule, javaFile));
                }
            } else if (orderEntry instanceof LibraryOrderEntry) {
                // 处理库依赖（可能包含编译输出的jar）
                LibraryOrderEntry libraryEntry = (LibraryOrderEntry) orderEntry;
                for (VirtualFile file : libraryEntry.getFiles(OrderRootType.CLASSES)) {
                    if (file.getPath().endsWith(".jar")) {
                        // 如果是jar包，检查是否可能包含这个类
                        String jarPath = file.getPath();
                        String packageName = javaFile.getPackageName();
                        String className = javaFile.getName().replace(".java", ".class");

                        if (!packageName.isEmpty()) {
                            String internalPath = packageName.replace('.', '/') + "/" + className;
                            paths.add("jar:" + jarPath + "!/" + internalPath);
                        }
                    }
                }
            }
        }

        return paths;
    }

    /**
     * 检查是否是Maven项目
     */
    private static boolean isMavenProject(@NotNull Project project) {
        // 替换过时的 getBaseDir() 方法
        String basePath = project.getBasePath();
        if (basePath != null) {
            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (baseDir != null) {
                return baseDir.findChild("pom.xml") != null;
            }

            // 或者直接检查文件系统
            File pomFile = new File(basePath, "pom.xml");
            return pomFile.exists();
        }
        return false;
    }

    /**
     * 检查是否是Gradle项目
     */
    private static boolean isGradleProject(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (baseDir != null) {
                return baseDir.findChild("build.gradle") != null ||
                        baseDir.findChild("build.gradle.kts") != null ||
                        baseDir.findChild("settings.gradle") != null ||
                        baseDir.findChild("settings.gradle.kts") != null;
            }

            // 直接检查文件系统
            File buildGradle = new File(basePath, "build.gradle");
            File buildGradleKts = new File(basePath, "build.gradle.kts");
            File settingsGradle = new File(basePath, "settings.gradle");
            File settingsGradleKts = new File(basePath, "settings.gradle.kts");

            return buildGradle.exists() || buildGradleKts.exists() ||
                    settingsGradle.exists() || settingsGradleKts.exists();
        }
        return false;
    }

    /**
     * 检查是否是Maven源码目录
     */
    private static boolean isMavenSourceDirectory(@NotNull VirtualFile dir) {
        String path = dir.getPath();
        return path.contains("src/main/java") || path.contains("src/test/java");
    }

    /**
     * 检查是否是Gradle源码目录
     */
    private static boolean isGradleSourceDirectory(@NotNull VirtualFile dir) {
        String path = dir.getPath();
        return path.contains("src/main/java") || path.contains("src/test/java");
    }

    /**
     * 获取模块根目录
     */
    @Nullable
    private static VirtualFile getModuleRoot(@NotNull Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        return contentRoots.length > 0 ? contentRoots[0] : null;
    }

    /**
     * 获取实际存在的class文件路径
     */
    @NotNull
    public static List<String> getExistingClassFilePaths(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        return getClassFilePaths(project, javaFile).stream()
                .filter(path -> {
                    if (path.startsWith("jar:")) {
                        return true;
                    }
                    File file = new File(path);
                    return file.exists();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取最可能的class文件路径（第一个存在的路径）
     */
    @Nullable
    public static String getMostLikelyClassFilePath(@NotNull Project project, @NotNull PsiJavaFile javaFile) {
        List<String> existingPaths = getExistingClassFilePaths(project, javaFile);
        return existingPaths.isEmpty() ? null : existingPaths.get(0);
    }
}