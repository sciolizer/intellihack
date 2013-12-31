package com.sciolizer.intellihack;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;

// First created by Joshua Ball on 12/31/13 at 12:16 PM
public class HackRunConfigurationProducer extends RunConfigurationProducer<HackConfigurationType.HackRunProfile> {

    public HackRunConfigurationProducer() {
        super(new HackConfigurationType());
    }

    @Override
    protected boolean setupConfigurationFromContext(HackConfigurationType.HackRunProfile configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        String fqn = getFullyQualifiedClassName(context);
        if (fqn == null) return false;
        HackConfigurationType.HackRunProfile profile = (HackConfigurationType.HackRunProfile) getConfigurationFactory().createConfiguration(fqn, configuration);
        profile.setRunnableClass(fqn);
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(HackConfigurationType.HackRunProfile configuration, ConfigurationContext context) {
        String fqn = getFullyQualifiedClassName(context);
        return fqn != null && fqn.equals(configuration.getRunnableClass());
    }

    private String getFullyQualifiedClassName(ConfigurationContext context) {
        PsiElement psiLocation = context.getPsiLocation(); // todo: this isn't working yet. getFullyQualifiedName expects a psiClass, but getPsiLocation only returns leaf nodes (e.g. Keyword, Identifier)
        return new FullyQualifiedNameGetter(context.getProject()).getFullyQualifiedName(psiLocation);
    }
}
