package org.archer.common.mybatis.generator;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.archer.common.utils.VelocityUtil;
import org.mybatis.generator.api.*;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.MergeConstants;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.NullProgressCallback;
import org.mybatis.generator.internal.ObjectFactory;
import org.mybatis.generator.internal.XmlFileMergerJaxp;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static oracle.net.aso.C09.c;
import static org.apache.batik.svggen.font.table.Table.name;
import static org.mybatis.generator.internal.util.ClassloaderUtility.getCustomClassloader;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

/**
 * Created by yukunpeng on 2017/8/3.
 */
public class ArcherMyBatisGenerator  {
    private Configuration configuration;

    private ShellCallback shellCallback;

    private List<GeneratedJavaFile> generatedJavaFiles;

    private List<GeneratedXmlFile> generatedXmlFiles;

    private List<String> warnings;

    private Set<String> projects;

    /**
     * Constructs a MyBatisGenerator object.
     *
     * @param configuration
     *            The configuration for this invocation
     * @param shellCallback
     *            an instance of a ShellCallback interface. You may specify
     *            <code>null</code> in which case the DefaultShellCallback will
     *            be used.
     * @param warnings
     *            Any warnings generated during execution will be added to this
     *            list. Warnings do not affect the running of the tool, but they
     *            may affect the results. A typical warning is an unsupported
     *            data type. In that case, the column will be ignored and
     *            generation will continue. You may specify <code>null</code> if
     *            you do not want warnings returned.
     * @throws InvalidConfigurationException
     *             if the specified configuration is invalid
     */
    public ArcherMyBatisGenerator(Configuration configuration, ShellCallback shellCallback,
                            List<String> warnings) throws InvalidConfigurationException {
        super();
        if (configuration == null) {
            throw new IllegalArgumentException(getString("RuntimeError.2")); //$NON-NLS-1$
        } else {
            this.configuration = configuration;
        }

        if (shellCallback == null) {
            this.shellCallback = new DefaultShellCallback(false);
        } else {
            this.shellCallback = shellCallback;
        }

        if (warnings == null) {
            this.warnings = new ArrayList<String>();
        } else {
            this.warnings = warnings;
        }
        generatedJavaFiles = new ArrayList<GeneratedJavaFile>();
        generatedXmlFiles = new ArrayList<GeneratedXmlFile>();
        projects = new HashSet<String>();

        this.configuration.validate();
    }

    /**
     * This is the main method for generating code. This method is long running,
     * but progress can be provided and the method can be canceled through the
     * ProgressCallback interface. This version of the method runs all
     * configured contexts.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or
     *            <code>null</code> if you do not require progress information
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback) throws SQLException,
            IOException, InterruptedException {
        generate(callback, null, null);
    }

    /**
     * This is the main method for generating code. This method is long running,
     * but progress can be provided and the method can be canceled through the
     * ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or
     *            <code>null</code> if you do not require progress information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the
     *            contexts with an id specified in this list will be run. If the
     *            list is null or empty, than all contexts are run.
     * @throws InvalidConfigurationException
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds)
            throws SQLException, IOException, InterruptedException {
        generate(callback, contextIds, null);
    }

    /**
     * This is the main method for generating code. This method is long running,
     * but progress can be provided and the method can be cancelled through the
     * ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or
     *            <code>null</code> if you do not require progress information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the
     *            contexts with an id specified in this list will be run. If the
     *            list is null or empty, than all contexts are run.
     * @param fullyQualifiedTableNames
     *            a set of table names to generate. The elements of the set must
     *            be Strings that exactly match what's specified in the
     *            configuration. For example, if table name = "foo" and schema =
     *            "bar", then the fully qualified table name is "foo.bar". If
     *            the Set is null or empty, then all tables in the configuration
     *            will be used for code generation.
     * @throws InvalidConfigurationException
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds,
                         Set<String> fullyQualifiedTableNames) throws SQLException,
            IOException, InterruptedException {

        if (callback == null) {
            callback = new NullProgressCallback();
        }

        generatedJavaFiles.clear();
        generatedXmlFiles.clear();

        // calculate the contexts to run
        List<Context> contextsToRun;
        if (contextIds == null || contextIds.size() == 0) {
            contextsToRun = configuration.getContexts();
        } else {
            contextsToRun = new ArrayList<Context>();
            for (Context context : configuration.getContexts()) {
                if (contextIds.contains(context.getId())) {
                    contextsToRun.add(context);
                }
            }
        }

        // setup custom classloader if required
        if (configuration.getClassPathEntries().size() > 0) {
            ClassLoader classLoader = getCustomClassloader(configuration.getClassPathEntries());
            ObjectFactory.addExternalClassLoader(classLoader);
        }

        // now run the introspections...
        int totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getIntrospectionSteps();
        }
        callback.introspectionStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.introspectTables(callback, warnings, fullyQualifiedTableNames);
        }

        // now run the generates
        totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getGenerationSteps();
        }
        callback.generationStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.generateFiles(callback, generatedJavaFiles, generatedXmlFiles, warnings);
        }

        // now save the files
        callback.saveStarted(generatedXmlFiles.size() + generatedJavaFiles.size());

        for (GeneratedXmlFile gxf : generatedXmlFiles) {
            projects.add(gxf.getTargetProject());

            File targetFile;
            String source;
            try {
                File directory = shellCallback.getDirectory(gxf.getTargetProject(), gxf.getTargetPackage());
                targetFile = new File(directory, gxf.getFileName());
                if (targetFile.exists()) {
                    if (gxf.isMergeable()) {
                        source = XmlFileMergerJaxp.getMergedSource(gxf, targetFile);
                    } else if (shellCallback.isOverwriteEnabled()) {
                        source = gxf.getFormattedContent();
                        warnings.add(getString("Warning.11", //$NON-NLS-1$
                                targetFile.getAbsolutePath()));
                    } else {
                        source = gxf.getFormattedContent();
                        targetFile = getUniqueFileName(directory, gxf
                                .getFileName());
                        warnings.add(getString(
                                "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                    }
                } else {
                    source = gxf.getFormattedContent();
                }
            } catch (ShellException e) {
                warnings.add(e.getMessage());
                continue;
            }

            callback.checkCancel();
            callback.startTask(getString(
                    "Progress.15", targetFile.getName())); //$NON-NLS-1$
            writeFile(targetFile, source, "UTF-8"); //$NON-NLS-1$
        }

        for (GeneratedJavaFile gjf : generatedJavaFiles) {
            projects.add(gjf.getTargetProject());

            File targetFile;
            String source;
            try {
                File directory = shellCallback.getDirectory(gjf.getTargetProject(), gjf.getTargetPackage());
                targetFile = new File(directory, gjf.getFileName());
                if (targetFile.exists()) {
                    if (shellCallback.isMergeSupported()) {
                        source = shellCallback.mergeJavaFile(gjf
                                        .getFormattedContent(), targetFile
                                        .getAbsolutePath(),
                                MergeConstants.OLD_ELEMENT_TAGS,
                                gjf.getFileEncoding());
                    } else if (shellCallback.isOverwriteEnabled()) {
                        source = gjf.getFormattedContent();
                        warnings.add(getString("Warning.11", //$NON-NLS-1$
                                targetFile.getAbsolutePath()));
                    } else {
                        source = gjf.getFormattedContent();
                        targetFile = getUniqueFileName(directory, gjf
                                .getFileName());
                        warnings.add(getString(
                                "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                    }
                } else {
                    source = gjf.getFormattedContent();
                }

                callback.checkCancel();
                callback.startTask(getString("Progress.15", targetFile.getName())); //$NON-NLS-1$
                writeFile(targetFile, source, gjf.getFileEncoding());
            } catch (ShellException e) {
                warnings.add(e.getMessage());
            }
        }
        for (Context context : contextsToRun) {
            String servicePackage = context.getProperty("service_package");
            String controllerPackage = context.getProperty("controller_package");
            String jspPath = context.getProperty("jsp_path");
            String serviceImplPackage=servicePackage+"."+"Impl";
            String module = context.getProperty("module_name");
            String author = context.getProperty("author_name");
            String targetProject = context.getJavaClientGeneratorConfiguration().getTargetProject();
            String mapperPackage = context.getJavaClientGeneratorConfiguration().getTargetPackage();
            String entityPackage = context.getJavaModelGeneratorConfiguration().getTargetPackage();
            char[] cs=module.toCharArray();
            cs[0]+=32;

            File directory;
            File targetFile;
            try {
                //生成service
                directory = shellCallback.getDirectory(targetProject, servicePackage);
                targetFile = new File(directory, module+"Service.java");
                VelocityContext velocityContext = new VelocityContext();
                velocityContext.put("package_name", servicePackage);
                velocityContext.put("entityPackage", entityPackage);
                velocityContext.put("module", module);
                velocityContext.put("author", author);
                velocityContext.put("model", String.valueOf(cs));
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateString = formatter.format(new Date());
                velocityContext.put("createtime",dateString);
                Resource service_vm= new ClassPathResource("template"+File.separator+"Service.vm");
                VelocityUtil.generate(service_vm.getFile().getPath(), targetFile.getPath(), velocityContext);

                //生成serviceImpl
                directory = shellCallback.getDirectory(targetProject, serviceImplPackage);
                targetFile = new File(directory, module+"ServiceImpl.java");
                velocityContext.put("package_name", serviceImplPackage);
                velocityContext.put("mapperPackage",mapperPackage);
                velocityContext.put("servicePackage",servicePackage);
                Resource serviceImpl_vm= new ClassPathResource("template"+File.separator+"ServiceImpl.vm");
                VelocityUtil.generate(serviceImpl_vm.getFile().getPath(), targetFile.getPath(), velocityContext);

                //生成Controller
                directory = shellCallback.getDirectory(targetProject, controllerPackage);
                targetFile = new File(directory, module+"Controller.java");
                velocityContext.put("package_name", controllerPackage);
                velocityContext.put("servicePackage",servicePackage);
                Resource controller_vm= new ClassPathResource("template"+File.separator+"Controller.vm");
                VelocityUtil.generate(controller_vm.getFile().getPath(), targetFile.getPath(), velocityContext);

                //生成JSP
                //directory = shellCallback.getDirectory(targetProject, jspPath);
//                File f =new File(jspPath);
//                if (!f.exists()){
//                    f.mkdirs();
//                }
//                targetFile = new File(jspPath, module+".jsp");
//                velocityContext.put("package_name", controllerPackage);
//                velocityContext.put("servicePackage",servicePackage);
//                Resource jsp_vm= new ClassPathResource("template"+File.separator+"jsp.vm");
//                VelocityUtil.generate(jsp_vm.getFile().getPath(), targetFile.getPath(), velocityContext);



            } catch (Exception e) {
                e.printStackTrace();
            }

            //生成service
//            File serviceFile = new File(service);
//            if (!serviceFile.exists()) {
//                VelocityContext context = new VelocityContext();
//                context.put("package_name", package_name);
//                context.put("model", model);
//                context.put("ctime", ctime);
//                VelocityUtil.generate(service_vm, service, context);
//                System.out.println(service);
//            }
        }




        for (String project : projects) {
            shellCallback.refreshProject(project);
        }



        callback.done();
    }

    /**
     * Writes, or overwrites, the contents of the specified file
     *
     * @param file
     * @param content
     */
    private void writeFile(File file, String content, String fileEncoding) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw;
        if (fileEncoding == null) {
            osw = new OutputStreamWriter(fos);
        } else {
            osw = new OutputStreamWriter(fos, fileEncoding);
        }

        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.close();
    }

    private File getUniqueFileName(File directory, String fileName) {
        File answer = null;

        // try up to 1000 times to generate a unique file name
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 1000; i++) {
            sb.setLength(0);
            sb.append(fileName);
            sb.append('.');
            sb.append(i);

            File testFile = new File(directory, sb.toString());
            if (!testFile.exists()) {
                answer = testFile;
                break;
            }
        }

        if (answer == null) {
            throw new RuntimeException(getString(
                    "RuntimeError.3", directory.getAbsolutePath())); //$NON-NLS-1$
        }

        return answer;
    }
}
