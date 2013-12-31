package com.sciolizer.intellihack;

import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;

// First created by Joshua Ball on 12/30/13 at 10:25 PM
public class HackRunnerSettings implements RunnerSettings {

    private String className;

    @Override
    public void readExternal(Element element) throws InvalidDataException {
//        className = element.getChildText("className");
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
//        element.
    }
}
