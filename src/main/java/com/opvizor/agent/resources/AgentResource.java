package com.opvizor.agent.resources;

import com.opvizor.agent.services.AgentService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by joao on 29.07.15.
 */
@RestController
public class AgentResource {

    Logger logger = org.slf4j.LoggerFactory.getLogger(AgentResource.class);
    public AgentService agentService;



    @GET
    @RequestMapping("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void greeting(@RequestParam(value = "email") String email, @RequestParam(value = "pass") String pass, HttpServletResponse response) {
        Path pth = null;
        try {
            String dir = null;

            try {
                pth = agentService.generateTempDir();
                dir = pth.toString();
                logger.info("temp dir: " + dir);
            } catch (IOException e) {
                logger.error("Error when generating Temp directory", e);
            }
            logger.info("Generating download to user: " + email);
            File prop = null;
            try {
                prop = agentService.generateuserProperties(email, pass, dir);
            } catch (IOException e) {
                logger.debug("User properties was not generated", e);
            }
            logger.debug("User properties generated");
            File exe = agentService.getFileFromResource("opvizor-dist*", dir);
            if (exe != null) {
                logger.debug("Exe obtained from resources: " + exe.getName());
            } else {
                logger.debug("Exe file was not found");
            }

            List<File> toZipTogether = new ArrayList<File>();
            toZipTogether.add(prop);
            toZipTogether.add(exe);

            File zip = agentService.generateZip(toZipTogether, dir);
            logger.debug("zipe generated");

            File sevenz = agentService.getFileFromResource("7zS.sfx", dir);
            logger.debug("7zip plugin obtained from resource");

            File setup = null;
            try {
                setup = agentService.generateSetupfile(exe.getName(), dir);
            } catch (IOException e) {
                logger.error("Error whole generating setup file", e);
            }
            logger.debug("setup file generated");

            ArrayList<File> toConcate = new ArrayList<File>();
            toConcate.add(zip);
            toConcate.add(sevenz);
            toConcate.add(setup);
            String concated = null;
            try {
                concated = agentService.concate(setup, sevenz, zip, dir);
            } catch (IOException e) {
                logger.error("Error while concatenating files");
            }
            logger.debug("files concatenated");

            logger.info("Generation completed, download will continue now");
            final File file = new File(concated);

            if (file == null || !file.exists()) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=setup.exe");

            InputStream is = null;
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                logger.error("Setup file to upload to client not found", e);
            }
            try {
                IOUtils.copy(is, response.getOutputStream());
            } catch (IOException e) {
                logger.error("Error when copying file to upload to buffer");
            }
            try {

                response.flushBuffer();
            } catch (IOException e) {
                logger.error("Flushing error", e);
            }
        }
        finally {
            try {
                agentService.delete(pth.toFile());
            } catch (IOException e) {
                logger.error("Error deleting files");
            }
        }
    }

}
