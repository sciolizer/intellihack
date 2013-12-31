package com.sciolizer.intellihack;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// First created by Joshua Ball on 12/30/13 at 10:21 PM
public class HackProgramRunner extends GenericProgramRunner<HackRunnerSettings> {
    @Nullable
    @Override
    protected RunContentDescriptor doExecute(Project project, RunProfileState state, RunContentDescriptor contentToReuse, ExecutionEnvironment executionEnvironment) throws ExecutionException {
        ExecutionResult executionResult = state.execute(executionEnvironment.getExecutor(), this);
        RunContentBuilder runContentBuilder = new RunContentBuilder(this, executionResult, executionEnvironment);
        return runContentBuilder.showRunContent(contentToReuse);
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return HackConfiguraitonType.ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(DefaultRunExecutor.EXECUTOR_ID) && profile instanceof HackConfiguraitonType.HackRunProfile;
    }
}
