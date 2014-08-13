package io.spring.initializr

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import static io.spring.initializr.support.GroovyTemplate.template

/**
 * Generate a project based on the configured metadata.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Component
class ProjectGenerator {

	@Autowired
	ProjectMetadata projectMetadata

	@Value('${TMPDIR:.}')
	String tmpdir

	private transient Map<String, List<File>> temporaryFiles = new HashMap<>()

	/**
	 * Generate a Maven pom for the specified {@link ProjectRequest}.
	 */
	byte[] generateMavenPom(ProjectRequest request) {
		Map model = initializeModel(request)
		doGenerateMavenPom(model)
	}

	/**
	 * Generate a Gradle build file for the specified {@link ProjectRequest}.
	 */
	byte[] generateGradleBuild(ProjectRequest request) {
		Map model = initializeModel(request)
		doGenerateGradleBuild(model)
	}

	/**
	 * Generate a project structure for the specified {@link ProjectRequest}. Returns
	 * a directory containing the project.
	 */
	File getProjectStructure(ProjectRequest request) {
		def model = initializeModel(request)

		File dir = File.createTempFile('tmp', '', new File(tmpdir))
		addTempFile(dir.name, dir)
		dir.delete()
		dir.mkdirs()

		if (request.type.contains('gradle')) {
			String gradle = new String(doGenerateGradleBuild(model))
			new File(dir, 'build.gradle').write(gradle)
		} else {
			String pom = new String(doGenerateMavenPom(model))
			new File(dir, 'pom.xml').write(pom)
		}

		String language = request.language

		File src = new File(new File(dir, 'src/main/' + language), request.packageName.replace('.', '/'))
		src.mkdirs()
		write(src, 'Application.' + language, model)

		if (request.packaging == 'war') {
			write(src, 'ServletInitializer.' + language, model)
		}

		File test = new File(new File(dir, 'src/test/' + language), request.packageName.replace('.', '/'))
		test.mkdirs()
		if (request.isWebStyle()) {
			model.testAnnotations = '@WebAppConfiguration\n'
			model.testImports = 'import org.springframework.test.context.web.WebAppConfiguration;\n'
		} else {
			model.testAnnotations = ''
			model.testImports = ''
		}
		write(test, 'ApplicationTests.' + language, model)

		File resources = new File(dir, 'src/main/resources')
		resources.mkdirs()
		new File(resources, 'application.properties').write('')

		if (request.isWebStyle()) {
			new File(dir, 'src/main/resources/templates').mkdirs()
			new File(dir, 'src/main/resources/static').mkdirs()
		}

		dir

	}

	/**
	 * Create a distribution file for the specified project structure
	 * directory and extension
	 */
	File createDistributionFile(File dir, String extension) {
		File download = new File(tmpdir, dir.name + extension)
		addTempFile(dir.name, download)
		download
	}

	/**
	 * Clean all the temporary files that are related to this root
	 * directory.
	 * @see #createDistributionFile
	 */
	void cleanTempFiles(File dir) {
		def tempFiles = temporaryFiles.remove(dir.name)
		if (tempFiles != null) {
			tempFiles.each { File file ->
				if (file.directory) {
					file.deleteDir()
				} else {
					file.delete()
				}
			}
		}
	}

	private Map initializeModel(ProjectRequest request) {
		Assert.notNull request.bootVersion, 'boot version must not be null'
		def model = [:]
		if (request.packaging == 'war' && !request.isWebStyle()) {
			request.style << 'web'
		}
		request.properties.each { model[it.key] = it.value }
		model.dependencies = request.resolveDependencies(projectMetadata)
		model
	}


	private byte[] doGenerateMavenPom(Map model) {
		template 'starter-pom.xml', model
	}


	private byte[] doGenerateGradleBuild(Map model) {
		template 'starter-build.gradle', model
	}

	def write(File src, String name, def model) {
		String tmpl = name.endsWith('.groovy') ? name + '.tmpl' : name
		def body = template tmpl, model
		new File(src, name).write(body)
	}

	private void addTempFile(String group, File file) {
		def content = temporaryFiles.get(group)
		if (content == null) {
			content = new ArrayList<File>()
			temporaryFiles.put(group, content)
		}
		content.add(file)
	}

}
