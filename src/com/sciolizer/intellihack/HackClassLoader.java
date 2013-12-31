package com.sciolizer.intellihack;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

// First created by Joshua Ball on 12/30/13 at 11:07 PM
public class HackClassLoader extends ClassLoader {
    private Logger logger = Logger.getInstance(getClass().getName());
    private Project project;

    public HackClassLoader(Project project) {
        super(HackClassLoader.class.getClassLoader());
        this.project = project;

    }

    public String getUrlString(VirtualFile file) {
        if (file.getPath().indexOf(".jar!/") != -1) {
            return "jar:file:" + file.getPath();
        }
        return file.getUrl();
    }

    protected URL findResource(String path) {
        VirtualFile file = getVirtualFile(path);
        if (file != null) {
            try {
                String urlString = getUrlString(file);
                return new URL(urlString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    protected Class findClass(String cla) throws ClassNotFoundException {
        String path = cla.replaceAll("\\.", "/") + ".class";
        VirtualFile classFile = getVirtualFile(path);
        if (classFile == null) {
            throw new ClassNotFoundException("Can't find class file: " + cla);
        }
        try {
            byte[] classBytes = classFile.contentsToByteArray();
            return defineClass(cla, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ClassNotFoundException("Error loading class: " + cla);
        }
    }

    private VirtualFile getVirtualFile(String path) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (int mod = 0; mod < modules.length; mod++) {
            Module module = modules[mod];
            ModuleRootManager instance = ModuleRootManager.getInstance(module);
            VirtualFile compilerOutputPath = instance.getModuleExtension(CompilerModuleExtension.class).getCompilerOutputPath();
            if (compilerOutputPath != null) {
                VirtualFile compilerClassFile = compilerOutputPath.findFileByRelativePath(path);
                if (compilerClassFile != null) {
                    return compilerClassFile;
                }
            }
            OrderEntry[] orderEntries = instance.getOrderEntries();
            for (int i = 0; i < orderEntries.length; i++) {
                OrderEntry orderEntry = orderEntries[i];
                if (orderEntry instanceof LibraryOrderEntry) {
                    VirtualFile[] files = orderEntry.getFiles(OrderRootType.CLASSES);
                    for (int j = 0; j < files.length; j++) {
                        VirtualFile libraryClassFile = files[j].findFileByRelativePath(path);
                        if (libraryClassFile != null) {
                            return libraryClassFile;
                        }
                    }
                }
            }
        }
        return null;
    }
}
