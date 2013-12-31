package com.sciolizer.intellihack;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// First created by Joshua Ball on 12/30/13 at 10:02 PM
public class HackConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "com.sciolizer.intellihack";
    private ExecutorService executor = Executors.newCachedThreadPool();
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
        public RunProfileState getState(@NotNull Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
            System.out.println("getState was called");
            return new RunProfileState() {

                private TextConsoleBuilder myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(env.getProject());

                @Nullable
                @Override
                public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
                    System.out.println("execute was called");
                    // todo: try using ConsoleViewImpl instead
                    final ConsoleView console = myConsoleBuilder.getConsole();
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
                            super.startNotify();
                            HackConfigurationType.this.executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    boolean errorFree = false;
                                    try {
                                        HackClassLoader hackClassLoader = new HackClassLoader(getProject());
                                        Class thing;
                                        try {
                                            thing = hackClassLoader.findClass(runnableClass);
                                        } catch (ClassNotFoundException e) {
                                            throw new RuntimeException(e);
                                        }
                                        Object o = null;
                                        outer: for (Constructor constructor : thing.getConstructors()) {
                                            List<Object> objects = new ArrayList<Object>(constructor.getParameterTypes().length);
                                            for (Class parameterType : constructor.getParameterTypes()) {
                                                if (parameterType.equals(ConsoleView.class)) {
                                                    objects.add(console);
                                                } else if (parameterType.equals(Project.class)) {
                                                    objects.add(getProject());
                                                } else {
                                                    continue outer;
                                                }
                                            }
                                            Object[] initArgs = objects.toArray();
                                            try {
                                                o = constructor.newInstance(initArgs);
                                            } catch (InstantiationException e) {
                                                throw new RuntimeException(e);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            } catch (InvocationTargetException e) {
                                                throw new RuntimeException(e);
                                            }
                                            break;
                                        }
                                        if (o == null) {
                                            console.print("Unable to instantiate\n", ConsoleViewContentType.ERROR_OUTPUT);
                                            return;
                                        }
                                        if (!(o instanceof Runnable)) {
                                            console.print("Not instance of Runnable\n", ConsoleViewContentType.ERROR_OUTPUT);
                                            return;
                                        }
                                        ((Runnable) o).run();
                                        errorFree = true;
                                    } catch (Throwable e) {
                                        StringWriter sw = new StringWriter();
                                        e.printStackTrace(new PrintWriter(sw));
                                        console.print(sw.toString(), ConsoleViewContentType.ERROR_OUTPUT);
                                    } finally {
                                        notifyProcessTerminated(errorFree ? 0 : 1);
                                    }
                                }
                            });
                        }
                    }
                    HackProcessHandler processHandler = new HackProcessHandler();
                    console.attachToProcess(processHandler);
                    return new DefaultExecutionResult(console, processHandler);
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
