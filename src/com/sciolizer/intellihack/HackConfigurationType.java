package com.sciolizer.intellihack;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.util.Collection;

// First created by Joshua Ball on 12/30/13 at 10:02 PM
public class HackConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "com.sciolizer.intellihack";
    public HackConfigurationType() {
        super(ID, "IntelliHack", "Runs arbitrary code in the same jvm as IntelliJ", AllIcons.Debugger.StackFrame);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(final Project project) {
                return new HackRunProfile(project, this);
            }
        });
    }

    public class HackRunProfile extends ModuleBasedConfiguration<HackRunConfigurationModule> implements RefactoringListenerProvider {

        private String runnableClass = "";
        private final FullyQualifiedNameGetter fullyQualifiedNameGetter = new FullyQualifiedNameGetter(getProject());
        private final SettingsEditor<HackRunProfile> hackSettingsEditor = new SettingsEditor<HackRunProfile>() {
            private final JPanel jPanel;
            private final JTextArea runnableClassTextArea;
            private final ConfigurationModuleSelector configurationModuleSelector;

            {
                JLabel runnableClassLabel = new JLabel("Runnable class:");

                runnableClassTextArea = new JTextArea(runnableClass);

                JPanel runnableClassPanel = new JPanel();
                runnableClassPanel.setLayout(new BoxLayout(runnableClassPanel, BoxLayout.X_AXIS));
                runnableClassPanel.add(runnableClassLabel);
                runnableClassPanel.add(runnableClassTextArea);

                JLabel moduleLabel = new JLabel("Module:");

                ComboBox moduleComboBox = new ComboBox();
                configurationModuleSelector = new ConfigurationModuleSelector(getProject(), moduleComboBox);

                JPanel modulePanel = new JPanel();
                modulePanel.setLayout(new BoxLayout(modulePanel, BoxLayout.X_AXIS));
                modulePanel.add(moduleLabel);
                modulePanel.add(moduleComboBox);

                jPanel = new JPanel();
                jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
                jPanel.add(runnableClassPanel);
                jPanel.add(modulePanel);

                moduleComboBox.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("actionPerformed: " + configurationModuleSelector.getModule());
                        setModule(configurationModuleSelector.getModule());
                    }
                });
                setModule(configurationModuleSelector.getModule());
            }

            @Override
            protected void resetEditorFrom(HackRunProfile s) {
                runnableClassTextArea.setText(s.runnableClass);
                configurationModuleSelector.reset(HackRunProfile.this);
            }

            @Override
            protected void applyEditorTo(HackRunProfile s) throws ConfigurationException {
                String text = runnableClassTextArea.getText();
                s.runnableClass = text;
                configurationModuleSelector.applyTo(HackRunProfile.this);
            }

            @NotNull
            @Override
            protected JComponent createEditor() {
                return jPanel;
            }
        };

        public HackRunProfile(Project project, ConfigurationFactory configurationFactory) {
            super(new HackRunConfigurationModule(project), configurationFactory);
        }

        @Override
        public void readExternal(Element element) throws InvalidDataException {
            runnableClass = element.getChild("runnableClass").getAttribute("class").getValue();
            getConfigurationModule().readExternal(element);
        }

        @Override
        public void writeExternal(Element element) throws WriteExternalException {
            Element child = new Element("runnableClass");
            child.setAttribute("class", runnableClass);
            element.addContent(child);
            getConfigurationModule().writeExternal(element);
        }

        public String getRunnableClass() {
            return runnableClass;
        }

        public void setRunnableClass(String runnableClass) {
            this.runnableClass = runnableClass;
        }

        @Override
        public void checkConfiguration() throws RuntimeConfigurationException {
            if (runnableClass == null || runnableClass.isEmpty()) {
                throw new RuntimeConfigurationError("Class not specified.");
            }
            Module module = getConfigurationModule().getModule();
            if (module == null) {
                throw new RuntimeConfigurationError("Module not specified.");
            }
            PsiClass aClass = JavaPsiFacade.getInstance(getProject()).findClass(runnableClass, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true));
            if (aClass == null) {
                throw new RuntimeConfigurationError("Could not find class '" + runnableClass + "'");
            }
            // todo: if module unspecified but class can be found in project (and is unambiguous), create quick fix for setting the module.
        }

        @Override
        public Collection<Module> getValidModules() {
            return JavaRunConfigurationModule.getModulesForClass(getProject(), runnableClass);
        }

        @NotNull
        @Override
        public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
            return hackSettingsEditor;
        }

        @Override
        public HackRunProfile clone() {
            HackRunProfile clone = (HackRunProfile) super.clone();
            clone.runnableClass = runnableClass;
            return clone;
        }

        @Nullable
        @Override
        public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
            String fqn = fullyQualifiedNameGetter.getFullyQualifiedName(element);
            if (fqn == null) return null;
            return new RefactoringElementListener() {
                @Override
                public void elementMoved(@NotNull PsiElement newElement) {
                    runnableClass = fullyQualifiedNameGetter.getFullyQualifiedName(newElement);
                }

                @Override
                public void elementRenamed(@NotNull PsiElement newElement) {
                    runnableClass = fullyQualifiedNameGetter.getFullyQualifiedName(newElement);
                }
            };
        }

        @Nullable
        @Override
        public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
            System.out.println("getState was called");
            return new RunProfileState() {
                @Nullable
                @Override
                public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
                    System.out.println("execute was called");
                    // todo: try using ConsoleViewImpl instead
                    ExecutionConsole executionConsole = new ExecutionConsole() {

                        JPanel panel = new JPanel();

                        @Override
                        public JComponent getComponent() {
                            return panel;
                        }

                        @Override
                        public JComponent getPreferredFocusableComponent() {
                            return panel;
                        }

                        @Override
                        public void dispose() { }
                    };
                    class HackProcessHandler extends ProcessHandler {
                        @Override
                        protected void destroyProcessImpl() {
                            System.out.println("destroyProcessImpl was called");
                        }

                        @Override
                        protected void detachProcessImpl() {
                            System.out.println("detachProcessImpl was called");
                        }

                        @Override
                        public boolean detachIsDefault() {
                            System.out.println("detachIsDefault was called");
                            return true;
                        }

                        @Nullable
                        @Override
                        public OutputStream getProcessInput() {
                            System.out.println("getProcessInput was called");
                            return System.out;
                        }

                        @Override
                        public void startNotify() {
                            System.out.println("startNotify was called");
                            HackClassLoader hackClassLoader = new HackClassLoader(getProject());
                            Class thing;
                            try {
                                thing = hackClassLoader.findClass(runnableClass);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                            Object o;
                            try {
                                o = thing.newInstance();
                            } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                            ((Runnable) o).run();
                        }
                    }
                    return new DefaultExecutionResult(executionConsole, new HackProcessHandler());
                }
            };
        }          //  */

    }

    // todo: is there any point to this class?
    private static class HackRunConfigurationModule extends RunConfigurationModule {
        private HackRunConfigurationModule(Project project) {
            super(project);
        }


    }

}
