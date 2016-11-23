package grails.plugin.databasesession

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

/**
 * @author Burt Beckwith
 */
class SessionFilters {

    def gormPersisterService

    def filters = {

        flash(controller: '*', action: '*') {
            afterView = { Exception e ->
                if (e || request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null) {
                    return
                }

                def enabled = databaseSessionEnabled(grailsApplication.config)
                if (!enabled) {
                    return
                }

                def alreadyCommitted = response.isCommitted()
                if (alreadyCommitted) {
                    return
                }

                try {
                    // set the value to the key as a flag to retrieve it from the request
                    gormPersisterService.setAttribute(request.session.id, GrailsApplicationAttributes.FLASH_SCOPE, GrailsApplicationAttributes.FLASH_SCOPE)
                }
                catch (InvalidatedSessionException ignored) {
                    // ignored
                }
            }
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private boolean databaseSessionEnabled(config) {
        def pluginEnabled = config.grails.plugin.databasesession.enabled
        pluginEnabled = pluginEnabled instanceof Boolean ? pluginEnabled : false

        def flashStorageEnabled = config.grails.plugin.redisdatabasesession.storeFlashScopeWithRedis
        flashStorageEnabled = flashStorageEnabled instanceof Boolean ? flashStorageEnabled : false

        return pluginEnabled && flashStorageEnabled
    }
}
