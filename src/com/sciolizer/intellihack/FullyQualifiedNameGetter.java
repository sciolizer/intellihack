package com.sciolizer.intellihack;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

// First created by Joshua Ball on 12/31/13 at 12:32 PM
public class FullyQualifiedNameGetter {
    private final Project project;

    public FullyQualifiedNameGetter(Project project) {
        this.project = project;
    }

    public String getFullyQualifiedName(PsiElement element) {
        if (!(element instanceof PsiClass)) return null;
        PsiClass psiClass = (PsiClass) element;
        PsiIdentifier classIdentifier = psiClass.getNameIdentifier();
        if (classIdentifier == null) return null;
        String text = classIdentifier.getText();
        PsiJavaFile javaFile = (PsiJavaFile) psiClass.getContainingFile();
        PsiPackage pkg = JavaPsiFacade.getInstance(project).findPackage(javaFile.getPackageName());
        if (pkg == null) return null;
        String packageName = pkg.getQualifiedName();
        return packageName + (packageName.isEmpty() ? "" : '.') + text;
    }
}
