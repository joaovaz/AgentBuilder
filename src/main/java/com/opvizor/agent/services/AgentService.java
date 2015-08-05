package com.opvizor.agent.services;


import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by joao on 29.07.15.
 */
public class AgentService {

    public static Logger logger = LoggerFactory.getLogger(AgentService.class);

    public static Path generateTempDir() throws IOException {

        Path workDir = Files.createTempDirectory("agentTemp");
        return workDir;
    }

public static File generateuserProperties(String email, String pass, String workDir) throws IOException {

    //create new file user.properties and add email and pass accordingly

    String data = email +"\n" + pass;
    File userP = new File(workDir,"user.properties");
    FileOutputStream fop = new FileOutputStream(userP);
    fop.write(data.getBytes("UTF-8"));
    fop.flush();
    fop.close();

 return userP;
}




    public static File generateZip(List<File> files, String workDir) {


        SevenZOutputFile sevenZOutput = null;
        File arch = null;
        try {
            arch = new File(workDir,"setup.7z");
            sevenZOutput = new SevenZOutputFile(arch);
            sevenZOutput.setContentCompression(SevenZMethod.COPY);
        } catch (IOException e) {
            logger.error("error compressing", e);
        }

        for(File toEntry: files){

            try {
                SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(toEntry, toEntry.getName());
                sevenZOutput.putArchiveEntry(entry);
                copy(toEntry, sevenZOutput);
                sevenZOutput.closeArchiveEntry();

            } catch (IOException e) {
                logger.error("Error compressing", e);
            }
        }

        try {

            sevenZOutput.close();

        } catch (IOException e) {
            logger.error("Error compressing");
        }
        return arch;
    }

    private static void copy(final File src, final SevenZOutputFile dst) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            final byte[] buffer = new byte[8*1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) >= 0) {
                dst.write(buffer, 0, bytesRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }


    public static File getFileFromResource(String filenamePatern, String tempDir) {

        ResourceLoader fileLoader=new DefaultResourceLoader();
        InputStream is = null;
        File res =null;

        try {
            Resource[] v = ResourcePatternUtils.getResourcePatternResolver(fileLoader).getResources("classpath:" + filenamePatern + "*");
            String filename = v[0].getFilename();
            is = v[0].getInputStream();
            res = new File(tempDir+"/"+filename);
            OutputStream outputStream = new FileOutputStream(res);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = is.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        } catch (IOException e) {
           logger.error("Error while obtaining resource",e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                   logger.error("Error closing stream",e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("Error closing stream",e);
                }

            }
        }

        return res;
    }


    public static File generateSetupfile(String name, String workDir) throws IOException {

        String conf = ";!@Install@!UTF-8!\n" +
                "Title=\"Opvizor\"\n" +
                "RunProgram=\""+name+"\"\n" +
                ";!@InstallEnd@!";

        File file = new File(workDir,"config.txt");
        FileOutputStream fop = new FileOutputStream(file);
        fop.write(conf.getBytes("UTF-8"));
        fop.flush();
        fop.close();

        return file;
    }

    public static String concate(File config, File plugin, File exec, String workDir) throws IOException {

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(workDir+"/setup.exe",true)));
        out.write(Files.readAllBytes(plugin.toPath()));
        out.flush();
        out.write(Files.readAllBytes(config.toPath()));
        out.flush();
        out.write(Files.readAllBytes(exec.toPath()));
        out.flush();
        out.close();

        return workDir+"/setup.exe";
    }

    public static void delete(File file)
            throws IOException{

        if(file.isDirectory()){

            //directory is empty, then delete it
            if(file.list().length==0){

                file.delete();
                logger.debug("Directory is deleted : "
                        + file.getAbsolutePath());

            }else{

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if(file.list().length==0){
                    file.delete();
                    logger.debug("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        }else{
            //if file, then delete it
            file.delete();
            logger.debug("File is deleted : " + file.getAbsolutePath());
        }
    }




}
