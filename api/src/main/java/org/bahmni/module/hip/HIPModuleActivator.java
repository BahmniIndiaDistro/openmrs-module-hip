package org.bahmni.module.hip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HIPModuleActivator implements org.openmrs.module.ModuleActivator {

    private Log log = LogFactory.getLog(getClass());

    /**
     * @see org.openmrs.module.ModuleActivator#willRefreshContext()
     */
    public void willRefreshContext() {
        log.info("Refreshing hip-module");
    }

    /**
     * @see org.openmrs.module.ModuleActivator#contextRefreshed()
     */
    public void contextRefreshed() {
        log.info("hip-module refreshed");
    }

    /**
     * @see org.openmrs.module.ModuleActivator#willStart()
     */
    public void willStart() {
        log.info("Starting hip-module ");
    }

    /**
     * @see org.openmrs.module.ModuleActivator#started()
     */
    public void started() {
        log.info("hip-module started");
    }

    /**
     * @see org.openmrs.module.ModuleActivator#willStop()
     */
    public void willStop() {
        log.info("Stopping hip-module ");
    }

    /**
     * @see org.openmrs.module.ModuleActivator#stopped()
     */
    public void stopped() {
        log.info("hip-module stopped");
    }
}