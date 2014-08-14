package io.spring.initializr.web

import io.spring.initializr.InitializrMetadata
import io.spring.initializr.ProjectGenerator
import io.spring.initializr.ProjectRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

import static io.spring.initializr.support.GroovyTemplate.template

/**
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Controller
class MainController {

	private static final Logger logger = LoggerFactory.getLogger(MainController.class)

	@Autowired
	private InitializrMetadata metadata

	@Autowired
	private ProjectGenerator projectGenerator

	@ModelAttribute
	ProjectRequest projectRequest() {
		ProjectRequest request = new ProjectRequest();
		metadata.initializeProjectRequest(request)
		request
	}

	@RequestMapping(value = "/")
	@ResponseBody
	InitializrMetadata metadata() {
		metadata
	}

	@RequestMapping(value = '/', produces = 'text/html')
	@ResponseBody
	String home() {
		def model = [:]
		metadata.properties.each { model[it.key] = it.value }
		template 'home.html', model
	}

	@RequestMapping('/pom')
	@ResponseBody
	ResponseEntity<byte[]> pom(ProjectRequest request) {
		def mavenPom = projectGenerator.generateMavenPom(request)
		new ResponseEntity<byte[]>(mavenPom, ['Content-Type': 'application/octet-stream'] as HttpHeaders, HttpStatus.OK)

	}

	@RequestMapping('/build')
	@ResponseBody
	ResponseEntity<byte[]> gradle(ProjectRequest request) {
		def gradleBuild = projectGenerator.generateGradleBuild(request)
		new ResponseEntity<byte[]>(gradleBuild, ['Content-Type': 'application/octet-stream'] as HttpHeaders, HttpStatus.OK)
	}

	@RequestMapping('/starter.zip')
	@ResponseBody
	ResponseEntity<byte[]> springZip(ProjectRequest request) {
		def dir = projectGenerator.generateProjectStructure(request)

		File download = projectGenerator.createDistributionFile(dir, '.zip')

		new AntBuilder().zip(destfile: download) {
			zipfileset(dir: dir, includes: '**')
		}
		logger.info("Uploading: ${download} (${download.bytes.length} bytes)")
		def result = new ResponseEntity<byte[]>(download.bytes,
				['Content-Type': 'application/zip'] as HttpHeaders, HttpStatus.OK)

		projectGenerator.cleanTempFiles(dir)
		result
	}

	@RequestMapping(value='/starter.tgz', produces='application/x-compress')
	@ResponseBody
	ResponseEntity<byte[]> springTgz(ProjectRequest request) {
		def dir = projectGenerator.generateProjectStructure(request)

		File download = projectGenerator.createDistributionFile(dir, '.tgz')

		new AntBuilder().tar(destfile: download, compression: 'gzip') {
			zipfileset(dir:dir, includes:'**')
		}
		logger.info("Uploading: ${download} (${download.bytes.length} bytes)")
		def result = new ResponseEntity<byte[]>(download.bytes,
				['Content-Type':'application/x-compress'] as HttpHeaders, HttpStatus.OK)

		projectGenerator.cleanTempFiles(dir)
		result
	}

}
