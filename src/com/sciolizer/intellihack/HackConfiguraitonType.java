package com.sciolizer.intellihack;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.OutputStream;
import java.util.Collection;

// First created by Joshua Ball on 12/30/13 at 10:02 PM
public class HackConfiguraitonType extends ConfigurationTypeBase {
    public static final String ID = "com.sciolizer.intellihack";
    public HackConfiguraitonType() {
        super(ID, "IntelliHack", "Runs arbitrary code in the same jvm as IntelliJ", AllIcons.Debugger.StackFrame);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(final Project project) {
                return new HackRunProfile(project, this);
            }
        });
    }

    public class HackRunProfile extends ModuleBasedConfiguration<HackRunConfigurationModule> {

        private String runnableClass = "";
        private final SettingsEditor<HackRunProfile> hackSettingsEditor = new SettingsEditor<HackRunProfile>() {
            private final JPanel jPanel = new JPanel();
            private final JTextArea jTextArea = new JTextArea();

            {
                JLabel jLabel = new JLabel("Runnable class:");
                jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
                jPanel.add(jLabel);
                jPanel.add(jTextArea);
            }

            @Override
            protected void resetEditorFrom(HackRunProfile s) {
                jTextArea.setText(s.runnableClass);
            }

            @Override
            protected void applyEditorTo(HackRunProfile s) throws ConfigurationException {
                String text = jTextArea.getText();
                s.runnableClass = text;
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
