package com.sciolizer.intellihack;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.OutputStream;

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

    public class HackRunProfile extends LocatableConfigurationBase {
        public HackRunProfile(Project project, ConfigurationFactory configurationFactory) {
            super(project, configurationFactory, "hack");
        }

        @NotNull
        @Override
        public SettingsEditor<HackRunProfile> getConfigurationEditor() {
            return new SettingsEditor<HackRunProfile>() {
                @Override
                protected void resetEditorFrom(HackRunProfile s) {

                }

                @Override
                protected void applyEditorTo(HackRunProfile s) throws ConfigurationException {

                }

                @NotNull
                @Override
                protected JComponent createEditor() {
                    return new JPanel();
                }
            };
        }

        @Nullable
        @Override
        public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
//                        return new JavaCommandLineState(env) {
//                            @Override
//                            protected JavaParameters createJavaParameters() throws ExecutionException {
//                                JavaParameters javaParameters = new JavaParameters();
//                                javaParameters.configureByProject(project, JavaParameters.JDK_AND_CLASSES, env.getProject().);
//                                return javaParameters;
//                            }
//                        };
            return new RunProfileState() {
                @Nullable
                @Override
                public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
                    return new ExecutionResult() {
                        @Override
                        public ExecutionConsole getExecutionConsole() {
                            return new ExecutionConsole() {

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
                        }

                        @Override
                        public AnAction[] getActions() {
                            return new AnAction[0];
                        }

                        @Override
                        public ProcessHandler getProcessHandler() {
                            class HackProcessHandler extends ProcessHandler {
                                @Override
                                protected void destroyProcessImpl() {

                                }

                                @Override
                                protected void detachProcessImpl() {

                                }

                                @Override
                                public boolean detachIsDefault() {
                                    return true;
                                }

                                @Nullable
                                @Override
                                public OutputStream getProcessInput() {
                                    return System.out;
                                }

                                @Override
                                public void startNotify() {
                                    HackClassLoader hackClassLoader = new HackClassLoader(getProject());
                                    Class thing;
                                    try {
                                        thing = hackClassLoader.findClass("Thing");
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
                            return new HackProcessHandler();
                        }
                    };
                }
            };
        }
    }
}
