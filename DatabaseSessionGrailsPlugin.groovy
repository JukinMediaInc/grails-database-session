import grails.plugin.databasesession.SessionProxyFilter
import grails.util.Environment
import grails.util.Metadata

import org.springframework.web.filter.DelegatingFilterProxy

class DatabaseSessionGrailsPlugin {
	String version = '1.2.2-jk3'
	String grailsVersion = '1.3.3 > *'
	String title = 'Database Session Plugin'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String description = 'Stores HTTP sessions in a database'
	String documentation = 'http://grails.org/plugin/database-session'

	String license = 'APACHE'
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPDATABASESESSION']
	def scm = [url: 'https://github.com/burtbeckwith/grails-database-session']

	def getWebXmlFilterOrder() {
		// make sure the filter is first but not before charEncodingFilter
		def filterMap = [:]
		try {
			def classLoader = new GroovyClassLoader(getClass().getClassLoader())
			def FilterManager = classLoader.loadClass('grails.plugin.webxml.FilterManager')
			filterMap = [sessionProxyFilter: FilterManager.GRAILS_WEB_REQUEST_POSITION - 1]
		} catch (ClassNotFoundException e) {
			log.error "Could not determine desired sessionProxyFilter position.", e
		}
		return filterMap
	}

	def doWithWebDescriptor = { xml ->
		if (!isEnabled(application.config)) {
			return
		}

		// add the filter after the last context-param
		def contextParam = xml.'context-param'

		contextParam[contextParam.size() - 1] + {
			'filter' {
				'filter-name'('sessionProxyFilter')
				'filter-class'(DelegatingFilterProxy.name)
			}
		}

		def filter = xml.'filter'
		filter[filter.size() - 1] + {
			'filter-mapping' {
				'filter-name'('sessionProxyFilter')
				'url-pattern'('/*')
				'dispatcher'('ERROR')
				'dispatcher'('FORWARD')
				'dispatcher'('REQUEST')
			}
		}
	}

	def doWithSpring = {
		if (!isEnabled(application.config)) {
			return
		}

		// do the check here instead of doWithWebDescriptor to get more obvious error display
		if (Metadata.current.getApplicationName() && !manager.hasGrailsPlugin('webxml')) {
			throw new IllegalStateException(
				'The database-session plugin requires that the webxml plugin be installed')
		}

		sessionProxyFilter(SessionProxyFilter) {
			persister = ref('gormPersisterService')
		}
	}

	private boolean isEnabled(config) {
		def enabled = config.grails.plugin.databasesession.enabled
		if (enabled instanceof Boolean) {
			return enabled
		}
		return !Environment.isDevelopmentMode()
	}
}
