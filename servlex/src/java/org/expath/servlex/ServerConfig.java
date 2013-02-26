/****************************************************************************/
/*  File:       ServerConfig.java                                           */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2009-12-12                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.servlex;

import org.expath.servlex.model.Application;
import java.io.File;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import net.sf.saxon.s9api.Processor;
import org.apache.log4j.Logger;
import org.expath.pkg.repo.ClasspathStorage;
import org.expath.pkg.repo.FileSystemStorage;
import org.expath.pkg.repo.Package;
import org.expath.pkg.repo.PackageException;
import org.expath.pkg.repo.Storage;
import org.expath.pkg.repo.UserInteractionStrategy;
import org.expath.pkg.saxon.SaxonRepository;
import org.expath.servlex.parser.EXPathWebParser;
import org.expath.servlex.parser.ParseException;
import org.expath.servlex.processors.CalabashProcessor;
import org.expath.servlex.tools.SaxonHelper;


/**
 * Singleton class with the config of the server.
 *
 * TODO: Probably should NOT be a singleton.  What if a (Java) webapp wants to
 * have different instances of Servlex, using different server configuration,
 * with several repositories?
 *
 * @author Florent Georges
 * @date   2009-12-12
 */
public class ServerConfig
{
    /** The system property name for the repo directory. */
    public static final String REPO_DIR_PROPERTY = "org.expath.servlex.repo.dir";
    /** The system property name for the repo classpath prefix. */
    public static final String REPO_CP_PROPERTY  = "org.expath.servlex.repo.classpath";
    /** The system property name for the log directory. */
    public static final String LOG_DIR_PROPERTY  = "org.expath.servlex.log.dir";

    /**
     * Initialize the webapp list from the repository got from system properties.
     */
    protected ServerConfig()
            throws ParseException
                 , PackageException
    {
        this(System.getProperty(REPO_DIR_PROPERTY), System.getProperty(REPO_CP_PROPERTY));
    }

    /**
     * Initialize the webapp list from the repository got from either parameter.
     */
    protected ServerConfig(String repo_dir, String repo_classpath)
            throws ParseException
                 , PackageException
    {
        this(getStorage(repo_dir, repo_classpath));
    }

    /**
     * Initialize the webapp list from the repository got from the parameter.
     */
    protected ServerConfig(Storage repo_storage)
            throws ParseException
                 , PackageException
    {
        // TODO: FIXME: MUST NOT BE HERE!
        java.util.logging.Logger.global.setLevel(Level.ALL);
        java.util.logging.Logger.global.addHandler(new ConsoleHandler());
        LOG.info("ServerConfig with storage: " + repo_storage);
        myStorage = repo_storage;
        // the repository object
        try {
            myRepo = new SaxonRepository(myStorage);
        }
        catch ( PackageException ex ) {
            throw new PackageException("Error inityializing the static repository", ex);
        }
        // the parser
        EXPathWebParser parser = new EXPathWebParser(myRepo);
        // the application map
        myApps = new HashMap<String, Application>();
        for ( Application app : parser.parseDescriptors() ) {
            myApps.put(app.getName(), app);
            LOG.info("Add the application to the store: " + app.getName());
        }
        // the Saxon processor
        mySaxon = SaxonHelper.makeSaxon(myRepo);
        // the Calabash processor
        myCalabash = new CalabashProcessor(myRepo);
    }

    private static Storage getStorage(String repo_dir, String repo_classpath)
            throws PackageException
    {
        if ( repo_dir == null && repo_classpath == null ) {
            // TODO: DEBUG: Must be set within web.xml...
            // repo_classpath = "appengine.repo";
            throw new PackageException("Neither " + REPO_DIR_PROPERTY + " nor " + REPO_CP_PROPERTY + " is set");
        }
        if ( repo_dir != null && repo_classpath != null ) {
            throw new PackageException("Both " + REPO_DIR_PROPERTY + " and " + REPO_CP_PROPERTY + " are set");
        }
        // the storage object
        Storage store;
        if ( repo_dir != null ) {
            File f = new File(repo_dir);
            if ( ! f.exists() ) {
                String msg = "The EXPath repository does not exist (" + REPO_DIR_PROPERTY + "=" + repo_dir + "): " + f;
                throw new PackageException(msg);
            }
            store = new FileSystemStorage(f);
        }
        else {
            store = new ClasspathStorage(repo_classpath);
        }
        return store;
    }

    public boolean canInstall()
    {
        return ! myStorage.isReadOnly();
    }

    /**
     * Reload the configuration.
     *
     * TODO: Maybe we should instead really reparse the map in the same instance,
     * so we do not invalidate all the reference to the existing instance, so
     * this is really a singleton (and other classes can keep a reference to
     * the singleton instance if they want).
     */
    public static synchronized ServerConfig reload(ServletConfig config)
            throws ParseException
                 , PackageException
    {
        // Just get rid of the previous one and instantiate a new one.
        INSTANCE = null;
        return getInstance(config);
    }

    /**
     * Return the singleton instance.
     *
     * Return the instance if it exists, without taking the params into account.
     * That's correct, but can be confusing.  TODO: Document it, or maybe find
     * a more flexible mechanism.
     */
    public static synchronized ServerConfig getInstance(ServletConfig config)
            throws ParseException
                 , PackageException
    {
        ServletContext ctxt = config.getServletContext();
        if ( LOG.isInfoEnabled() ) {
            Enumeration<String> names = ctxt.getInitParameterNames();
            while ( names.hasMoreElements() ) {
                String n = names.nextElement();
                LOG.info("Init Servlex - param " + n + ": " + ctxt.getInitParameter(n));
            }
        }
        String dir = ctxt.getInitParameter(REPO_DIR_PROPERTY);
        String cp  = ctxt.getInitParameter(REPO_CP_PROPERTY);
        LOG.info("Init Servlex - dir=" + dir + ", cp=" + cp);
        if ( INSTANCE == null ) {
            if ( dir == null && cp == null ) {
                INSTANCE = new ServerConfig();
            }
            else {
                INSTANCE = new ServerConfig(dir, cp);
            }
        }
        return INSTANCE;
    }

    /**
     * Return the singleton instance.
     *
     * TODO: Return the instance if it exists, without taking the params into
     * account.  That's correct, but can be confusing.  Document it, or maybe
     * find a more flexible mechanism.
     */
    public static synchronized ServerConfig getInstance(String repo_dir, String repo_classpath)
            throws ParseException
                 , PackageException
    {
        if ( INSTANCE == null ) {
            INSTANCE = new ServerConfig(repo_dir, repo_classpath);
        }
        return INSTANCE;
    }

    /**
     * Return the singleton instance.
     *
     * TODO: Return the instance if it exists, without taking the param into
     * account.  That's correct, but can be confusing.  Document it, or maybe
     * find a more flexible mechanism.
     */
    public static synchronized ServerConfig getInstance(Storage repo_storage)
            throws ParseException
                 , PackageException
    {
        if ( INSTANCE == null ) {
            INSTANCE = new ServerConfig(repo_storage);
        }
        return INSTANCE;
    }

    /**
     * Return an application given its name.
     * 
     * Throw an exception if there is no application with that name.
     */
    public Application getApplication(String appname)
            throws ServlexException
    {
        Application app = myApps.get(appname);
        if ( app == null ) {
            LOG.error("404: Application not found: " + appname);
            throw new ServlexException(404, "Page not found");
        }
        return app;
    }

    /**
     * Return the list of application names.
     */
    public Set<String> getApplicationNames()
    {
        return myApps.keySet();
    }

    /**
     * Return the shared Saxon instance.
     */
    public Processor getSaxon()
    {
        return mySaxon;
    }

    /**
     * Return the shared Calabash configuration.
     */
    public CalabashProcessor getCalabash()
    {
        return myCalabash;
    }

    /**
     * Return the repository.
     */
    public SaxonRepository getRepository()
    {
        return myRepo;
    }

    /**
     * Install a webapp (or a library) in the repository.
     *
     * Return the name of the newly installed webapp, or null if the package
     * is not a webapp.
     * 
     * TODO: The param 'force' is set to 'true', make it configurable.
     */
    public synchronized String install(File archive)
            throws PackageException
                 , ParseException
    {
        Package pkg = myRepo.installPackage(archive, true, new LoggingUserInteraction());
        return doInstall(pkg);
    }

    /**
     * Install a webapp (or a library) in the repository.
     *
     * Return the name of the newly installed webapp, or null if the package
     * is not a webapp.
     * 
     * TODO: The param 'force' is set to 'true', make it configurable.
     */
    public synchronized String install(URI uri)
            throws PackageException
                 , ParseException
    {
        Package pkg = myRepo.installPackage(uri, true, new LoggingUserInteraction());
        return doInstall(pkg);
    }

    private String doInstall(Package pkg)
            throws ParseException
    {
        EXPathWebParser parser = new EXPathWebParser(myRepo);
        Application app = parser.loadPackage(pkg);
        if ( app == null ) {
            // not a webapp
            return null;
        }
        else {
            // package is a webapp
            myApps.put(app.getName(), app);
            return app.getName();
        }
    }

    /**
     * Remove a webapp (or a library) in the repository.
     */
    public synchronized void remove(String appname)
            throws PackageException
                 , ParseException
    {
        Application app = myApps.get(appname);
        if ( app == null ) {
            throw new PackageException("The application is not installed: " + appname);
        }
        Package pkg = app.getPackage();
        myRepo.removePackage(pkg.getName(), true, new LoggingUserInteraction());
        myApps.remove(appname);
    }

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(ServerConfig.class);

    /** The singleton instance. */
    private static ServerConfig INSTANCE;

    /** The repository for webapps. */
    private SaxonRepository myRepo;
    /** The storage used by the repository. */
    private Storage myStorage;
    /** The Saxon instance. */
    private Processor mySaxon;
    /** The Calabash instance. */
    private CalabashProcessor myCalabash;
    /** The application map. */
    private Map<String, Application> myApps;

    /**
     * Interaction always return default, and log messages.
     */
    private class LoggingUserInteraction
            implements UserInteractionStrategy
    {
        @Override
        public void messageInfo(String msg)
                throws PackageException
        {
            LOG.info(msg);
        }

        @Override
        public void messageError(String msg)
                throws PackageException
        {
            LOG.error(msg);
        }

        @Override
        public void logInfo(String msg)
                throws PackageException
        {
            LOG.info(msg);
        }

        @Override
        public boolean ask(String prompt, boolean dflt)
                throws PackageException
        {
            return dflt;
        }

        @Override
        public String ask(String prompt, String dflt)
                throws PackageException
        {
            return dflt;
        }
    }
}


/* ------------------------------------------------------------------------ */
/*  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS COMMENT.               */
/*                                                                          */
/*  The contents of this file are subject to the Mozilla Public License     */
/*  Version 1.0 (the "License"); you may not use this file except in        */
/*  compliance with the License. You may obtain a copy of the License at    */
/*  http://www.mozilla.org/MPL/.                                            */
/*                                                                          */
/*  Software distributed under the License is distributed on an "AS IS"     */
/*  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See    */
/*  the License for the specific language governing rights and limitations  */
/*  under the License.                                                      */
/*                                                                          */
/*  The Original Code is: all this file.                                    */
/*                                                                          */
/*  The Initial Developer of the Original Code is Florent Georges.          */
/*                                                                          */
/*  Contributor(s): none.                                                   */
/* ------------------------------------------------------------------------ */
