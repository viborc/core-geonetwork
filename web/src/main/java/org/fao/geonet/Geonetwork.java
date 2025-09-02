//=============================================================================
//===	Copyright (C) 2001-2023 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet;

import jeeves.config.springutil.ServerBeanPropertyUpdater;
import jeeves.constants.Jeeves;
import jeeves.interfaces.ApplicationHandler;
import jeeves.server.JeevesProxyInfo;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.server.sources.http.ServletPathFinder;
import jeeves.xlink.Processor;
import org.apache.commons.lang.StringUtils;
import org.fao.geonet.api.records.formatters.FormatType;
import org.fao.geonet.api.records.formatters.FormatterApi;
import org.fao.geonet.api.records.formatters.FormatterWidth;
import org.fao.geonet.api.site.LogUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.*;
import org.fao.geonet.entitylistener.AbstractEntityListenerManager;
import org.fao.geonet.events.server.ServerStartup;
import org.fao.geonet.exceptions.OperationAbortedEx;
import org.fao.geonet.inspireatom.InspireAtomType;
import org.fao.geonet.inspireatom.harvester.InspireAtomHarvesterScheduler;
import org.fao.geonet.kernel.*;
import org.fao.geonet.kernel.csw.CswHarvesterResponseExecutionService;
import org.fao.geonet.kernel.harvest.HarvestManager;
import org.fao.geonet.kernel.oaipmh.OaiPmhDispatcher;
import org.fao.geonet.kernel.search.EsSearchManager;
import org.fao.geonet.kernel.setting.SettingInfo;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.kernel.thumbnail.ThumbnailMaker;
import org.fao.geonet.languages.IsoLanguagesMapper;
import org.fao.geonet.lib.DatabaseType;
import org.fao.geonet.lib.DbLib;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.SettingRepository;
import org.fao.geonet.repository.SourceRepository;
import org.fao.geonet.resources.Resources;
import org.fao.geonet.util.ThreadUtils;
import org.fao.geonet.utils.IO;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.ProxyInfo;
import org.fao.geonet.utils.XmlResolver;
import org.fao.geonet.wro4j.GeonetWro4jFilter;
import org.jdom.Element;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @brief Main GeoNetwork application class that handles HTTP connections and initializes the system.
 * 
 * This class is responsible for the complete initialization of the GeoNetwork system.
 * It handles the setup of all core components including:
 * - Application context and configuration
 * - Database connections and data initialization
 * - Search functionality
 * - Schema management
 * - Thesaurus management
 * - OAI-PMH services
 * - Harvesting services
 * - System monitoring
 * 
 * It implements the ApplicationHandler interface to integrate with the Jeeves framework.
 */
public class Geonetwork implements ApplicationHandler {
    /** @var logger The system logger instance */
    private Logger logger;
    
    /** @var appPath The application's base file path */
    private Path appPath;
    
    /** @var searchMan The search manager for handling search operations */
    private EsSearchManager searchMan;
    
    /** @var _applicationContext Spring application context */
    private ConfigurableApplicationContext _applicationContext;
    
    /** @var oaipmhDis The OAI-PMH dispatcher for handling OAI-PMH protocol requests */
    private OaiPmhDispatcher oaipmhDis;

    /**
     * @brief Returns the application context name.
     * 
     * This method is part of the ApplicationHandler interface implementation.
     * It provides the name of the context under which the application runs.
     * 
     * @return The context name as defined in Geonet constants
     */
    public String getContextName() {
        return Geonet.CONTEXT_NAME;
    }


    /**
     * @brief Initializes the GeoNetwork engine and loads all required data.
     * 
     * This is the main initialization method that sets up the entire GeoNetwork system.
     * It performs the following operations:
     * - Sets up the application context
     * - Configures logging
     * - Initializes the data directory
     * - Sets up the database and imports initial data if needed
     * - Configures caching
     * - Initializes settings management
     * - Sets up search functionality
     * - Initializes schema management
     * - Configures the OAI-PMH server
     * - Sets up harvesting
     * - Initializes various other subsystems
     * 
     * @param config The XML configuration element containing setup parameters
     * @param context The service context providing access to the servlet environment
     * @return A GeonetContext object containing references to all initialized components
     * @throws Exception If any error occurs during initialization
     */
    public Object start(Element config, ServiceContext context) throws Exception {
        context.setAsThreadLocal();
        this._applicationContext = context.getApplicationContext();
        ApplicationContextHolder.set(this._applicationContext);

        logger = context.getLogger();
        // If an error occur during logger configuration
        // Continue starting the application with
        // a logger initialized with the default log4j.xml.
        try {
            LogUtils.refreshLogConfiguration();
        } catch (OperationAbortedEx e) {
            logger.error("Error while setting log configuration. " +
                "Check the setting in the database for logger configuration file.");
            logger.error(e.getMessage());
        }
        ConfigurableListableBeanFactory beanFactory = context.getApplicationContext().getBeanFactory();

        ServletPathFinder finder = new ServletPathFinder(this._applicationContext.getBean(ServletContext.class));
        appPath = finder.getAppPath();
        String baseURL = context.getBaseUrl();
        String webappName = "";
        if (StringUtils.isNotEmpty(baseURL)) {
            webappName = baseURL.substring(1);
        }

        final SystemInfo systemInfo = _applicationContext.getBean(SystemInfo.class);
        String version = systemInfo.getVersion();
        String subVersion = systemInfo.getSubVersion();

        logger.info("Initializing GeoNetwork " + version + "." + subVersion + " ...");

        // Get main service config handler
        @SuppressWarnings("unchecked")
        List<Element> serviceConfigElems = config.getChildren();
        ServiceConfig handlerConfig = new ServiceConfig(serviceConfigElems);

        // Init configuration directory
        final GeonetworkDataDirectory dataDirectory = _applicationContext.getBean(GeonetworkDataDirectory.class);
        dataDirectory.init(webappName, appPath, handlerConfig, context.getServlet());



        // Get config handler properties
        String systemDataDir = handlerConfig.getMandatoryValue(Geonet.Config.SYSTEM_DATA_DIR);
        String thesauriDir = handlerConfig.getMandatoryValue(Geonet.Config.CODELIST_DIR);

        logger.info("Data directory: " + systemDataDir);

        setProps(appPath, handlerConfig);

        // Initialize password encryptor
        logger.info("Initializing database password encryptor");
        final EncryptorInitializer encryptorInitializer = _applicationContext.getBean(EncryptorInitializer.class);
        encryptorInitializer.init(dataDirectory);

        importDatabaseData(context);

        JeevesJCS.setConfigFilename(appPath.resolve("WEB-INF/classes/cache.ccf"));

        // force caches to be config'd so shutdown hook works correctly
        JeevesJCS.getInstance(Processor.XLINK_JCS);
        JeevesJCS.getInstance(XmlResolver.XMLRESOLVER_JCS);

        //------------------------------------------------------------------------
        //--- initialize settings subsystem

        logger.info("  - Setting manager...");

        SettingManager settingMan = this._applicationContext.getBean(SettingManager.class);

        //--- initialize ThreadUtils with setting manager and rm props
        final DataSource dataSource = context.getBean(DataSource.class);
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            ThreadUtils.init(conn.getMetaData().getURL(), settingMan);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }

        //------------------------------------------------------------------------
        //--- initialize SRU

        logger.info("  - SRU...");

        try {
				  String[] configs = { Geonet.File.JZKITAPPLICATIONCONTEXT };
          ApplicationContext app_context = new  ClassPathXmlApplicationContext( configs, _applicationContext );

          // to have access to the GN context in spring-managed objects
          ContextContainer cc = (ContextContainer)_applicationContext.getBean("ContextGateway");
          cc.setSrvctx(context);


        } catch (Exception e) {
          logger.error("     SRU initialization failed - cannot pass context to SRU subsystem, SRU searches will not work! Error is:" + Util.getStackTrace(e));
        }

        //------------------------------------------------------------------------
        //--- initialize SchemaManager

        logger.info("  - Schema manager...");

        Path schemaPluginsDir = dataDirectory.getSchemaPluginsDir();
        Path schemaCatalogueFile = dataDirectory.getConfigDir().resolve(Geonet.File.SCHEMA_PLUGINS_CATALOG);
        Path schemaPublicationDir = dataDirectory.getSchemaPublicationDir();
        boolean createOrUpdateSchemaCatalog = handlerConfig.getMandatoryValue(Geonet.Config.SCHEMA_PLUGINS_CATALOG_UPDATE).equals("true");
        logger.info("			- Schema plugins directory: " + schemaPluginsDir);
        logger.info("			- Schema Catalog File     : " + schemaCatalogueFile);
        SchemaManager schemaMan = _applicationContext.getBean(SchemaManager.class);
        schemaMan.configure(_applicationContext, appPath, dataDirectory.getResourcesDir(), schemaPublicationDir, schemaCatalogueFile,
            schemaPluginsDir, context.getLanguage(), handlerConfig.getMandatoryValue(Geonet.Config.PREFERRED_SCHEMA),
            createOrUpdateSchemaCatalog);

        //------------------------------------------------------------------------
        //--- initialize search and editing
        logger.info("  - Search...");

        SettingInfo settingInfo = context.getBean(SettingInfo.class);
        searchMan = _applicationContext.getBean(EsSearchManager.class);


        // if the validator exists the proxyCallbackURL needs to have the external host and
        // servlet name added so that the cas knows where to send the validation notice
        ServerBeanPropertyUpdater.updateURL(settingInfo.getSiteUrl() + baseURL, _applicationContext);

        //------------------------------------------------------------------------
        //--- extract intranet ip/mask and initialize AccessManager

        logger.info("  - Access manager...");

        //------------------------------------------------------------------------
        //--- get edit params and initialize DataManager

        logger.info("  - Xml serializer and Data manager...");

        SvnManager svnManager = _applicationContext.getBean(SvnManager.class);
        XmlSerializer xmlSerializer = _applicationContext.getBean(XmlSerializer.class);

        if (xmlSerializer instanceof XmlSerializerSvn && svnManager != null) {
            svnManager.setContext(context);
            Path subversionPath = dataDirectory.getMetadataRevisionDir().toAbsolutePath().normalize();
            svnManager.setSubversionPath(subversionPath.toString());
            svnManager.init();
        }

        logger.info("  - Thesaurus...");

        _applicationContext.getBean(ThesaurusManager.class).init(false, context, thesauriDir);

        logger.info("  - Open Archive Initiative (OAI-PMH) server...");

        oaipmhDis = new OaiPmhDispatcher(settingMan, schemaMan);


        GeonetContext gnContext = new GeonetContext(_applicationContext, false);

        //------------------------------------------------------------------------
        //--- return application context

        beanFactory.registerSingleton("serviceHandlerConfig", handlerConfig);
        beanFactory.registerSingleton("oaipmhDisatcher", oaipmhDis);


        _applicationContext.getBean(DataManager.class).init(context, false);
        _applicationContext.getBean(HarvestManager.class).init(context, gnContext.isReadOnly());

        _applicationContext.getBean(ThumbnailMaker.class).init(context);

        logger.info("Site ID is : " + settingMan.getSiteId());

        // Add local site to the source table
        SourceRepository sourceRepository = _applicationContext.getBean(SourceRepository.class);
        if (sourceRepository.findOneByUuid(settingMan.getSiteId()) == null) {
            final Source source = sourceRepository.save(
                new Source(settingMan.getSiteId(),
                    settingMan.getSiteName(),
                    null,
                    SourceType.portal));
        }

        // Creates a default site logo, only if the logo image doesn't exists
        // This can happen if the application has been updated with a new version preserving the database and
        // images/logos folder is not copied from old application
        createSiteLogo(settingMan.getSiteId(), context, context.getAppPath());

        //-- Initialize the proxy configuration if required
        Lib.net.setupProxy(settingMan);

        //--- load proxy information from settings into Jeeves for observers such
        //--- as jeeves.utils.XmlResolver to use
        ProxyInfo pi = JeevesProxyInfo.getInstance();
        boolean useProxy = Lib.net.getProxyConfiguration().isEnabled();
        if (useProxy) {
            String proxyHost = Lib.net.getProxyConfiguration().getHost();
            String proxyPort = Lib.net.getProxyConfiguration().getPort();
            String username = Lib.net.getProxyConfiguration().getUsername();
            String password = Lib.net.getProxyConfiguration().getPassword();
            pi.setProxyInfo(proxyHost, Integer.valueOf(proxyPort), username, password);
        }


        boolean inspireEnable = settingMan.getValueAsBool(Settings.SYSTEM_INSPIRE_ENABLE, false);

        if (inspireEnable) {

            String atomType = settingMan.getValue(Settings.SYSTEM_INSPIRE_ATOM);
            String atomSchedule = settingMan.getValue(Settings.SYSTEM_INSPIRE_ATOM_SCHEDULE);


            if (StringUtils.isNotEmpty(atomType) && StringUtils.isNotEmpty(atomSchedule)
                && atomType.equalsIgnoreCase(InspireAtomType.ATOM_REMOTE)) {
                logger.info("  - INSPIRE ATOM feed harvester ...");

                InspireAtomHarvesterScheduler.schedule(atomSchedule, context, gnContext);
            }
        }


        //
        // db heartbeat configuration -- for failover to readonly database
        //
        boolean dbHeartBeatEnabled = Boolean.parseBoolean(handlerConfig.getValue(Geonet.Config.DB_HEARTBEAT_ENABLED, "false"));
        if (dbHeartBeatEnabled) {
            Integer dbHeartBeatInitialDelay = Integer.parseInt(handlerConfig.getValue(Geonet.Config.DB_HEARTBEAT_INITIALDELAYSECONDS,
                "5"));
            Integer dbHeartBeatFixedDelay = Integer.parseInt(handlerConfig.getValue(Geonet.Config.DB_HEARTBEAT_FIXEDDELAYSECONDS, "60"));
            createDBHeartBeat(gnContext, dbHeartBeatInitialDelay, dbHeartBeatFixedDelay);
        }

        fillCaches(context);

        AbstractEntityListenerManager.setSystemRunning(true);

        this._applicationContext.publishEvent(new ServerStartup(this._applicationContext));

        return gnContext;
    }

    /**
     * @brief Initializes and populates various application caches.
     * 
     * This method starts a background thread that pre-loads various caches:
     * - WRO4J resource caches for web resources optimization
     * - Formatter caches for metadata display
     * 
     * Pre-loading these caches improves performance for the first users of the system
     * by avoiding the initial cache population delay during normal operation.
     * 
     * @param context The service context providing access to the application environment
     */
    private void fillCaches(final ServiceContext context) {
        final FormatterApi formatService = context.getBean(FormatterApi.class); // this will initialize the formatter

        Thread fillCaches = new Thread(new Runnable() {
            @Override
            public void run() {
                final ServletContext servletContext = context.getServlet().getServletContext();
                context.setAsThreadLocal();
                ApplicationContextHolder.set(_applicationContext);
                GeonetWro4jFilter filter = (GeonetWro4jFilter) servletContext.getAttribute(GeonetWro4jFilter.GEONET_WRO4J_FILTER_KEY);

                @SuppressWarnings("unchecked")
                List<String> wro4jUrls = _applicationContext.getBean("wro4jUrlsToInitialize", List.class);

                for (String wro4jUrl : wro4jUrls) {
                    Log.info(Geonet.GEONETWORK, "Initializing the WRO4J group: " + wro4jUrl + " cache");
                    final MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext, "GET", "/static/" + wro4jUrl);
                    final MockHttpServletResponse response = new MockHttpServletResponse();
                    try {
                        filter.doFilter(servletRequest, response, new MockFilterChain());
                    } catch (Throwable t) {
                        Log.info(Geonet.GEONETWORK, "Error while initializing the WRO4J group: " + wro4jUrl + " cache", t);
                    }
                }


                final Page<Metadata> metadatas = _applicationContext.getBean(MetadataRepository.class).findAll(PageRequest.of(0, 1));
                if (metadatas.getNumberOfElements() > 0) {
                    Integer mdId = metadatas.getContent().get(0).getId();
                    context.getUserSession().loginAs(new User().setName("admin").setProfile(Profile.Administrator).setUsername("admin"));
                    @SuppressWarnings("unchecked")
                    List<String> formattersToInitialize = _applicationContext.getBean("formattersToInitialize", List.class);

                    for (String formatterName : formattersToInitialize) {
                        Log.info(Geonet.GEONETWORK, "Initializing the Formatter with id: " + formatterName);
                        final MockHttpSession servletSession = new MockHttpSession(servletContext);
                        servletSession.setAttribute(Jeeves.Elem.SESSION,  context.getUserSession());
                        final MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext);
                        servletRequest.setSession(servletSession);
                        final MockHttpServletResponse response = new MockHttpServletResponse();
                        try {
                            formatService.exec("eng", FormatType.html.toString(), mdId.toString(), null, formatterName,
                                Boolean.TRUE.toString(), FormatterWidth._100, new ServletWebRequest(servletRequest, response));
                        } catch (Throwable t) {
                            Log.info(Geonet.GEONETWORK, "Error while initializing the Formatter with id: " + formatterName, t);
                        }
                    }
                }
            }
        });
        fillCaches.setDaemon(true);
        fillCaches.setName("Fill Caches Thread");
        fillCaches.setPriority(Thread.MIN_PRIORITY);
        fillCaches.start();
    }

    /**
     * @brief Imports initial data into the database for a new installation.
     * 
     * This method checks if the database is empty (new installation) and if so:
     * - Sets up the password encryptor for a first-time setup
     * - Imports SQL data from initialization files
     * - Creates and sets a unique site UUID
     * - Initializes the ISO languages mapper
     * 
     * The method is only executed for new installations and is skipped for
     * existing databases to prevent data corruption.
     * 
     * @param context The service context providing access to the application environment
     */
    private void importDatabaseData(final ServiceContext context) {
        // check if database has any data
        final SettingRepository settingRepository = context.getBean(SettingRepository.class);
        final long count = settingRepository.count();
        if (count == 0) {
            try {
                // Set setFirstInitialSetupFlag to true for the encryptorInitializer as this is a new installation.
                EncryptorInitializer encryptorInitializer = context.getBean(EncryptorInitializer.class);
                encryptorInitializer.setFirstInitialSetupFlag(true);

                // import data from init files
                List<Pair<String, String>> importData = context.getBean("initial-data", List.class);
                Set<String> applicationLanguages = (Set<String>) ApplicationContextHolder.get().getBean("languages");

                final DbLib dbLib = new DbLib();
                for (Pair<String, String> pair : importData) {
                    String folderPath = pair.one();
                    String sqlFilePath = pair.two();
                    Path appPath = context.getAppPath();
                    ServletContext servletContext = context.getServlet().getServletContext();

                    List<String> filePaths = new ArrayList<>();
                    if (sqlFilePath.contains("{lang}")) {
                        for (String lang : applicationLanguages) {
                            filePaths.add(sqlFilePath.replace("{lang}", lang));
                        }
                    } else {
                        filePaths.add(sqlFilePath);
                    }

                    for (String filePathStr : filePaths) {
                        Log.debug(Geonet.DB, "Executing SQL from: " + folderPath + " " + filePathStr);
                        try {
                            dbLib.insertData(servletContext, context, appPath, Path.of(folderPath), filePathStr);
                        } catch (IOException ioe) {
                            Log.warning(Geonet.DB, "File not found during initialization. For language add required files if needed. " + ioe.getMessage());
                        }
                    }
                }
                String siteUuid = UUID.randomUUID().toString();
                context.getBean(SettingManager.class).setSiteUuid(siteUuid);

            } catch (Throwable t) {
                Log.error(Geonet.DB, "Error occurred while trying to execute SQL", t);
                throw new RuntimeException(t);
            }

            context.getBean(IsoLanguagesMapper.class).init();
        }
    }

    /**
     * @brief Sets up a periodic database connectivity check for failover support.
     * 
     * This method establishes a scheduled task that periodically checks whether 
     * GeoNetwork can successfully write to the database. If the database becomes
     * unavailable for writing, GeoNetwork will automatically switch to read-only mode.
     * When database write access is restored, the system will switch back to read-write mode.
     * 
     * The heartbeat mechanism provides automatic failover capability for high-availability
     * deployments with read-only database replicas.
     * 
     * @param gc The GeonetContext containing application components
     * @param initialDelay The delay in seconds before the first check is performed
     * @param fixedDelay The fixed delay in seconds between consecutive checks
     * @throws SchedulerException If there is an error setting up the scheduled task
     */
    private void createDBHeartBeat(final GeonetContext gc, Integer initialDelay, Integer fixedDelay) throws SchedulerException {
        logger.info("creating DB heartbeat with initial delay of " + initialDelay + " s and fixed delay of " + fixedDelay + " s");
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable DBHeartBeat = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean readOnly = gc.isReadOnly();
                    logger.debug("DBHeartBeat: GN is read-only ? " + readOnly);
                    boolean canWrite = checkDBWrite();
                    HarvestManager hm = gc.getBean(HarvestManager.class);
                    if (readOnly && canWrite) {
                        logger.warning("GeoNetwork can write to the database, switching to read-write mode");
                        readOnly = false;
                        gc.setReadOnly(readOnly);
                        hm.setReadOnly(readOnly);
                    } else if (!readOnly && !canWrite) {
                        logger.warning("GeoNetwork can not write to the database, switching to read-only mode");
                        readOnly = true;
                        gc.setReadOnly(readOnly);
                        hm.setReadOnly(readOnly);
                    } else {
                        if (readOnly) {
                            logger.info("GeoNetwork remains in read-only mode");
                        } else {
                            logger.debug("GeoNetwork remains in read-write mode");
                        }
                    }
                } catch (Throwable x) {
                    // any uncaught exception would cause the scheduled execution to silently stop
                    logger.error("DBHeartBeat error: " + x.getMessage() + " This error is ignored. Error details: " + Util.getStackTrace(x));
                }
            }

            /**
             * @brief Tests database write capability by attempting to create and delete a setting.
             * 
             * This method performs a simple write test to determine if the database
             * is available for write operations. It creates a temporary setting,
             * flushes it to the database, and then deletes it.
             * 
             * @return true if the database write operation succeeded, false otherwise
             */
            private boolean checkDBWrite() {
                SettingRepository settingsRepo = gc.getBean(SettingRepository.class);
                try {
                    Setting newSetting = settingsRepo.save(new Setting().setName("DBHeartBeat").setValue("value"));
                    settingsRepo.flush();
                    settingsRepo.delete(newSetting);
                    return true;
                } catch (Exception x) {
                    logger.info("DBHeartBeat Exception: " + x.getMessage());
                    return false;
                }
            }
        };
        scheduledExecutorService.scheduleWithFixedDelay(DBHeartBeat, initialDelay, fixedDelay, TimeUnit.SECONDS);
    }

    /**
     * @brief Creates a default site logo if one doesn't already exist.
     * 
     * This method checks if a logo image exists for the current site (identified by nodeUuid).
     * If no logo is found, it copies a default GeoNetwork logo to use as the site logo.
     * This ensures that every site has a logo, even for new installations or when
     * upgrading from previous versions where the logos directory might not have been copied.
     * 
     * @param nodeUuid The UUID of the node/site
     * @param context The service context providing access to the application environment
     * @param appPath The application's base file path
     */
    private void createSiteLogo(String nodeUuid, ServiceContext context, Path appPath) {
        try {
            final Resources resources = context.getBean(Resources.class);
            Path logosDir = resources.locateLogosDir(context);
            Path logo = logosDir.resolve(nodeUuid + ".png");
            if (!Files.exists(logo)) {
                resources.copyLogo(context, "images" + File.separator + "harvesting" + File.separator + "GN3.png", nodeUuid);
            }
        } catch (Throwable e) {
            logger.error("      Error when setting the logo: " + e.getMessage());
        }
    }

    /**
     * @brief Sets required system properties for the application.
     * 
     * This method configures essential system properties needed by GeoNetwork:
     * - Registers XML catalog files for schema plugins
     * - Sets up MIME type mappings for proper content type handling
     * 
     * These properties ensure proper functioning of various subsystems that
     * rely on system properties for configuration.
     *
     * @param webappDir The web application directory path
     * @param handlerConfig The service configuration containing setup parameters
     */
    private void setProps(Path webappDir, ServiceConfig handlerConfig) {

        final Path configDir = IO.toPath(handlerConfig.getValue(Geonet.Config.CONFIG_DIR));
        final Path schemapluginUriCatalog = configDir.resolve("schemaplugin-uri-catalog.xml");
        Path webInf = SchemaManager.registerXmlCatalogFiles(webappDir, schemapluginUriCatalog);

        //--- Set mime-mappings
        String mimeProp = System.getProperty("mime-mappings");
        if (mimeProp == null) mimeProp = "";
        if (!mimeProp.equals("")) {
            logger.info("Overriding mime-mappings property (was set to " + mimeProp + ")");
        }
        mimeProp = webInf.resolve("mime-types.properties").toString();
        System.setProperty("mime-mappings", mimeProp);
        logger.info("mime-mappings property set to " + mimeProp);

    }


    /**
     * @brief Gracefully shuts down the GeoNetwork application.
     * 
     * This method performs an orderly shutdown of all GeoNetwork components:
     * - Stops the entity listener manager to prevent further database updates
     * - Shuts down the CSW harvester response execution service
     * - Stops the INSPIRE Atom harvester scheduler
     * - Shuts down the harvest manager to stop all active harvesters
     * - Stops the OAI-PMH dispatcher
     * 
     * A proper shutdown ensures that all resources are released and ongoing
     * operations are completed or properly terminated.
     */
    public void stop() {
        logger.info("Stopping geonetwork...");
        AbstractEntityListenerManager.setSystemRunning(false);

        logger.info("shutting down CSW HarvestResponse executionService");
        CswHarvesterResponseExecutionService.getExecutionService().shutdownNow();

        InspireAtomHarvesterScheduler.shutdown();

        logger.info("  - Harvest Manager...");
        _applicationContext.getBean(HarvestManager.class).shutdown();

        // Beans registered using SingletonBeanRegistry#registerSingleton don't have their
        // @PreDestroy called. So do it manually.
        oaipmhDis.shutdown();
    }
}
