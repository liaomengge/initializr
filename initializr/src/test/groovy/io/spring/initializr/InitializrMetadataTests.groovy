/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.initializr

import io.spring.initializr.test.InitializrMetadataBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.*

/**
 * @author Stephane Nicoll
 */
class InitializrMetadataTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none()

	private final InitializrMetadata metadata = new InitializrMetadata()

	@Test
	void createRootSpringBootStarter() {
		InitializrMetadata.Dependency d = new InitializrMetadata.Dependency();
		d.asSpringBootStarter("")
		assertEquals 'org.springframework.boot', d.groupId
		assertEquals 'spring-boot-starter', d.artifactId
	}

	@Test
	void setCoordinatesFromId() {
		def dependency = createDependency('org.foo:bar:1.2.3')
		metadata.validateDependency(dependency)
		assertEquals 'org.foo', dependency.groupId
		assertEquals 'bar', dependency.artifactId
		assertEquals '1.2.3', dependency.version
		assertEquals 'org.foo:bar:1.2.3', dependency.id
	}

	@Test
	void setCoordinatesFromIdNoVersion() {
		def dependency = createDependency('org.foo:bar')
		metadata.validateDependency(dependency)
		assertEquals 'org.foo', dependency.groupId
		assertEquals 'bar', dependency.artifactId
		assertNull dependency.version
		assertEquals 'org.foo:bar', dependency.id
	}

	@Test
	void setIdFromCoordinates() {
		def dependency = new InitializrMetadata.Dependency()
		dependency.groupId = 'org.foo'
		dependency.artifactId = 'bar'
		dependency.version = '1.0'
		metadata.validateDependency(dependency)
		assertEquals 'org.foo:bar', dependency.id
	}

	@Test
	void setIdFromCoordinatesNoVersion() {
		def dependency = new InitializrMetadata.Dependency()
		dependency.groupId = 'org.foo'
		dependency.artifactId = 'bar'
		metadata.validateDependency(dependency)
		assertEquals 'org.foo:bar', dependency.id
	}

	@Test
	void setIdFromSimpleName() {
		def dependency = createDependency('web')

		metadata.validateDependency(dependency)
		assertEquals 'org.springframework.boot', dependency.groupId
		assertEquals 'spring-boot-starter-web', dependency.artifactId
		assertNull dependency.version
		assertEquals 'web', dependency.id
	}

	@Test
	void invalidDependency() {
		thrown.expect(InvalidInitializrMetadataException)
		metadata.validateDependency(new InitializrMetadata.Dependency())
	}

	@Test
	void invalidSinceVersion() {
		def dependency = createDependency('web')
		dependency.since = 'A.B.C'

		thrown.expect(InvalidInitializrMetadataException)
		thrown.expectMessage('A.B.C')
		metadata.validateDependency(dependency)
	}

	@Test
	void invalidUntilVersion() {
		def dependency = createDependency('security')
		dependency.until = 'A.B.C'

		thrown.expect(InvalidInitializrMetadataException)
		thrown.expectMessage('A.B.C')
		metadata.validateDependency(dependency)
	}

	@Test
	void invalidIdFormatTooManyColons() {
		def dependency = createDependency('org.foo:bar:1.0:test:external')

		thrown.expect(InvalidInitializrMetadataException)
		metadata.validateDependency(dependency)
	}

	@Test
	void generateIdWithNoGroupId() {
		def dependency = new InitializrMetadata.Dependency()
		dependency.artifactId = 'bar'
		thrown.expect(IllegalArgumentException)
		dependency.generateId()
	}

	@Test
	void generateIdWithNoArtifactId() {
		def dependency = new InitializrMetadata.Dependency()
		dependency.groupId = 'foo'
		thrown.expect(IllegalArgumentException)
		dependency.generateId()
	}

	@Test
	void indexedDependencies() {
		def dependency = createDependency('first')
		def dependency2 = createDependency('second')

		def metadata = InitializrMetadataBuilder.withDefaults()
				.addDependencyGroup('foo', dependency, dependency2).validateAndGet()

		assertSame dependency, metadata.getDependency('first')
		assertSame dependency2, metadata.getDependency('second')
		assertNull metadata.getDependency('anotherId')
	}

	@Test
	void addTwoDependenciesWithSameId() {
		def dependency = createDependency('conflict')
		def dependency2 = createDependency('conflict')

		def builder = InitializrMetadataBuilder.withDefaults()
				.addDependencyGroup('foo', dependency, dependency2)

		thrown.expect(IllegalArgumentException)
		thrown.expectMessage('conflict')
		builder.validateAndGet()
	}

	@Test
	void addDependencyWithAliases() {
		def dependency = createDependency('first')
		dependency.aliases.add('alias1')
		dependency.aliases.add('alias2')

		def metadata = InitializrMetadataBuilder.withDefaults()
				.addDependencyGroup('foo', dependency).validateAndGet()

		assertSame dependency, metadata.getDependency('first')
		assertSame dependency, metadata.getDependency('alias1')
		assertSame dependency, metadata.getDependency('alias2')
	}

	@Test
	void aliasClashWithAnotherDependency() {
		def dependency = createDependency('first')
		dependency.aliases.add('alias1')
		dependency.aliases.add('alias2')

		def dependency2 = createDependency('alias2')

		def builder = InitializrMetadataBuilder.withDefaults()
				.addDependencyGroup('foo', dependency)
				.addDependencyGroup('bar', dependency2)

		thrown.expect(IllegalArgumentException)
		thrown.expectMessage('alias2')
		builder.validateAndGet()
	}

	@Test
	void createProjectRequest() {
		def metadata = InitializrMetadataBuilder.withDefaults().validateAndGet()
		def request = doCreateProjectRequest(metadata)
		assertEquals metadata.defaults.groupId, request.groupId
	}

	@Test
	void validateAction() {
		def metadata = new InitializrMetadataBuilder()
				.addType('foo', false, 'my-action.zip', 'none', 'none').validateAndGet()
		assertEquals '/my-action.zip', metadata.getType('foo').action
	}

	@Test
	void validateArtifactRepository() {
		def metadata = InitializrMetadataBuilder.withDefaults().instance()
		metadata.env.artifactRepository = 'http://foo/bar'
		metadata.validate()
		assertEquals 'http://foo/bar/', metadata.env.artifactRepository
	}

	@Test
	void getDefaultNoDefault() {
		List elements = []
		elements << createJavaVersion('one', false) << createJavaVersion('two', false)
		assertEquals 'one', InitializrMetadata.getDefault(elements)
	}

	@Test
	void getDefaultEmpty() {
		List elements = []
		assertNull InitializrMetadata.getDefault(elements)
	}

	@Test
	void getDefault() {
		List elements = []
		elements << createJavaVersion('one', false) << createJavaVersion('two', true)
		assertEquals 'two', InitializrMetadata.getDefault(elements)
	}

	private static ProjectRequest doCreateProjectRequest(InitializrMetadata metadata) {
		def request = new ProjectRequest()
		metadata.initializeProjectRequest(request)
		request
	}

	private static InitializrMetadata.Dependency createDependency(String id) {
		def dependency = new InitializrMetadata.Dependency()
		dependency.id = id
		dependency
	}

	private static InitializrMetadata.JavaVersion createJavaVersion(String version, boolean selected) {
		def javaVersion = new InitializrMetadata.JavaVersion()
		javaVersion.id = version
		javaVersion.default = selected
		javaVersion
	}

}
