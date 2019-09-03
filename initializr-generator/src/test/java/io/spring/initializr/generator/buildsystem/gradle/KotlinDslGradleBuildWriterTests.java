/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.initializr.generator.buildsystem.gradle;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import io.spring.initializr.generator.buildsystem.Dependency;
import io.spring.initializr.generator.buildsystem.Dependency.Exclusion;
import io.spring.initializr.generator.buildsystem.DependencyScope;
import io.spring.initializr.generator.io.IndentingWriter;
import io.spring.initializr.generator.version.VersionProperty;
import io.spring.initializr.generator.version.VersionReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link KotlinDslGradleBuildWriter}
 *
 * @author Jean-Baptiste Nizet
 */
class KotlinDslGradleBuildWriterTests {

	@Test
	void gradleBuildWithCoordinates() throws IOException {
		GradleBuild build = new GradleBuild();
		build.settings().group("com.example").version("1.0.1-SNAPSHOT");
		List<String> lines = generateBuild(build);
		assertThat(lines).contains("group = \"com.example\"", "version = \"1.0.1-SNAPSHOT\"");
	}

	@Test
	void gradleBuildWithSourceCompatibility11() throws IOException {
		GradleBuild build = new GradleBuild();
		build.settings().sourceCompatibility("11");
		List<String> lines = generateBuild(build);
		assertThat(lines).contains("java.sourceCompatibility = JavaVersion.VERSION_11");
	}

	@Test
	void gradleBuildWithSourceCompatibility1Dot8() throws IOException {
		GradleBuild build = new GradleBuild();
		build.settings().sourceCompatibility("1.8");
		List<String> lines = generateBuild(build);
		assertThat(lines).contains("java.sourceCompatibility = JavaVersion.VERSION_1_8");
	}

	@Test
	void gradleBuildWithBuildscriptDependency() {
		GradleBuild build = new GradleBuild();
		build.buildscript((buildscript) -> buildscript
				.dependency("org.springframework.boot:spring-boot-gradle-plugin:2.1.0.RELEASE"));
		assertThatIllegalStateException().isThrownBy(() -> generateBuild(build));
	}

	@Test
	void gradleBuildWithBuildscriptExtProperty() {
		GradleBuild build = new GradleBuild();
		build.buildscript((buildscript) -> buildscript.ext("kotlinVersion", "\1.2.51\""));
		assertThatIllegalStateException().isThrownBy(() -> generateBuild(build));
	}

	@Test
	void gradleBuildWithBuiltinPlugin() throws IOException {
		GradleBuild build = new GradleBuild();
		build.plugins().add("java");
		build.plugins().add("war");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("plugins {", "    java", "    war", "}");
	}

	@Test
	void gradleBuildWithKotlinPluginAndVersion() throws IOException {
		GradleBuild build = new GradleBuild();
		build.plugins().add("org.jetbrains.kotlin.jvm", (plugin) -> plugin.setVersion("1.3.21"));
		build.plugins().add("org.jetbrains.kotlin.plugin.spring", (plugin) -> plugin.setVersion("1.3.21"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("plugins {", "    kotlin(\"jvm\") version \"1.3.21\"",
				"    kotlin(\"plugin.spring\") version \"1.3.21\"", "}");
	}

	@Test
	void gradleBuildWithPluginAndVersion() throws IOException {
		GradleBuild build = new GradleBuild();
		build.plugins().add("org.springframework.boot", (plugin) -> plugin.setVersion("2.1.0.RELEASE"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("plugins {",
				"    id(\"org.springframework.boot\") version \"2.1.0.RELEASE\"", "}");
	}

	@Test
	void gradleBuildWithApplyPlugin() {
		GradleBuild build = new GradleBuild();
		build.plugins().apply("io.spring.dependency-management");
		assertThatIllegalStateException().isThrownBy(() -> generateBuild(build));
	}

	@Test
	void gradleBuildWithMavenCentralRepository() throws IOException {
		GradleBuild build = new GradleBuild();
		build.repositories().add("maven-central");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("repositories {", "    mavenCentral()", "}");
	}

	@Test
	void gradleBuildWithRepository() throws IOException {
		GradleBuild build = new GradleBuild();
		build.repositories().add("spring-milestones", "Spring Milestones", "https://repo.spring.io/milestone");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("repositories {",
				"    maven { url = uri(\"https://repo.spring.io/milestone\") }", "}");
	}

	@Test
	void gradleBuildWithSnapshotRepository() throws IOException {
		GradleBuild build = new GradleBuild();
		build.repositories().add("spring-snapshots", "Spring Snapshots", "https://repo.spring.io/snapshot", true);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("repositories {",
				"    maven { url = uri(\"https://repo.spring.io/snapshot\") }", "}");
	}

	@Test
	void gradleBuildWithPluginRepository() throws IOException {
		GradleBuild build = new GradleBuild();
		build.pluginRepositories().add("spring-milestones", "Spring Milestones", "https://repo.spring.io/milestone");
		List<String> lines = generateBuild(build);
		assertThat(lines).doesNotContain("repositories {");
	}

	@Test
	void gradleBuildWithTaskWithTypesCustomizedWithNestedAssignments() throws IOException {
		GradleBuild build = new GradleBuild();
		build.customizeTasksWithType("org.jetbrains.kotlin.gradle.tasks.KotlinCompile",
				(task) -> task.nested("kotlinOptions",
						(kotlinOptions) -> kotlinOptions.set("freeCompilerArgs", "listOf(\"-Xjsr305=strict\")")));
		build.customizeTasksWithType("org.jetbrains.kotlin.gradle.tasks.KotlinCompile",
				(task) -> task.nested("kotlinOptions", (kotlinOptions) -> kotlinOptions.set("jvmTarget", "\"1.8\"")));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsOnlyOnce("import org.jetbrains.kotlin.gradle.tasks.KotlinCompile").containsSequence(
				"tasks.withType<KotlinCompile> {", "    kotlinOptions {",
				"        freeCompilerArgs = listOf(\"-Xjsr305=strict\")", "        jvmTarget = \"1.8\"", "    }", "}");
	}

	@Test
	void gradleBuildWithTaskWithTypesAndShortTypes() throws IOException {
		GradleBuild build = new GradleBuild();
		build.customizeTasksWithType("JavaCompile", (javaCompile) -> javaCompile.set("options.fork", "true"));
		assertThat(generateBuild(build)).doesNotContain("import JavaCompile")
				.containsSequence("tasks.withType<JavaCompile> {", "    options.fork = true", "}");
	}

	@Test
	void gradleBuildWithTaskCustomizedWithInvocations() throws IOException {
		GradleBuild build = new GradleBuild();
		build.customizeTask("asciidoctor", (task) -> {
			task.invoke("inputs.dir", "snippetsDir");
			task.invoke("dependsOn", "test");
		});
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("tasks.asciidoctor {", "    inputs.dir(snippetsDir)", "    dependsOn(test)",
				"}");
	}

	@Test
	void gradleBuildWithTaskCustomizedWithAssignments() throws IOException {
		GradleBuild build = new GradleBuild();
		build.customizeTask("compileKotlin", (task) -> {
			task.set("kotlinOptions.freeCompilerArgs", "listOf(\"-Xjsr305=strict\")");
			task.set("kotlinOptions.jvmTarget", "\"1.8\"");
		});
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("tasks.compileKotlin {",
				"    kotlinOptions.freeCompilerArgs = listOf(\"-Xjsr305=strict\")",
				"    kotlinOptions.jvmTarget = \"1.8\"", "}");
	}

	@Test
	void gradleBuildWithTaskCustomizedWithNestedCustomization() throws IOException {
		GradleBuild build = new GradleBuild();
		build.customizeTask("compileKotlin",
				(compileKotlin) -> compileKotlin.nested("kotlinOptions", (kotlinOptions) -> {
					kotlinOptions.set("freeCompilerArgs", "listOf(\"-Xjsr305=strict\")");
					kotlinOptions.set("jvmTarget", "\"1.8\"");
				}));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("tasks.compileKotlin {", "    kotlinOptions {",
				"        freeCompilerArgs = listOf(\"-Xjsr305=strict\")", "        jvmTarget = \"1.8\"", "    }", "}");
	}

	@Test
	void gradleBuildWithExt() throws Exception {
		GradleBuild build = new GradleBuild();
		build.properties().property("java.version", "\"1.8\"").property("alpha", "file(\"build/example\")");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("extra[\"alpha\"] = file(\"build/example\")",
				"extra[\"java.version\"] = \"1.8\"");
	}

	@Test
	void gradleBuildWithVersionProperties() throws IOException {
		GradleBuild build = new GradleBuild();
		build.properties().version(VersionProperty.of("version.property", false), "1.2.3")
				.version(VersionProperty.of("internal.property", true), "4.5.6").version("external.property", "7.8.9");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("extra[\"external.property\"] = \"7.8.9\"",
				"extra[\"internalProperty\"] = \"4.5.6\"", "extra[\"version.property\"] = \"1.2.3\"");
	}

	@Test
	void gradleBuildWithVersionedDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("kotlin-stdlib",
				Dependency.withCoordinates("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
						.version(VersionReference.ofProperty("kotlin.version")).scope(DependencyScope.COMPILE));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    implementation(\"org.jetbrains.kotlin:kotlin-stdlib-jdk8:${property(\"kotlinVersion\")}\")", "}");
	}

	@Test
	void gradleBuildWithExternalVersionedDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("acme",
				Dependency.withCoordinates("com.example", "acme")
						.version(VersionReference.ofProperty(VersionProperty.of("acme.version", false)))
						.scope(DependencyScope.COMPILE));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    implementation(\"com.example:acme:${property(\"acme.version\")}\")", "}");
	}

	@Test
	void gradleBuildWithExtAndVersionProperties() throws Exception {
		GradleBuild build = new GradleBuild();
		build.properties().version(VersionProperty.of("test-version", true), "1.0").version("alpha-version", "0.1")
				.property("myProperty", "42");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("extra[\"myProperty\"] = 42", "extra[\"alpha-version\"] = \"0.1\"",
				"extra[\"testVersion\"] = \"1.0\"");
	}

	@Test
	void gradleBuildWithConfiguration() throws Exception {
		GradleBuild build = new GradleBuild();
		build.addConfiguration("developmentOnly");
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("val developmentOnly by configurations.creating");
	}

	@Test
	void gradleBuildWithConfigurationCustomization() throws Exception {
		GradleBuild build = new GradleBuild();
		build.addConfiguration("custom");
		build.customizeConfiguration("runtimeClasspath", (configuration) -> configuration.extendsFrom("custom"));
		build.customizeConfiguration("runtimeClasspath", (configuration) -> configuration.extendsFrom("builtIn"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("val custom by configurations.creating", "configurations {",
				"    runtimeClasspath {", "        extendsFrom(custom, configurations.builtIn.get())", "    }", "}");
	}

	@Test
	void gradleBuildWithConfigurationCustomizations() throws Exception {
		GradleBuild build = new GradleBuild();
		build.addConfiguration("custom");
		build.customizeConfiguration("runtimeClasspath", (configuration) -> configuration.extendsFrom("custom"));
		build.customizeConfiguration("testRuntimeClasspath", (configuration) -> configuration.extendsFrom("builtIn"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("val custom by configurations.creating", "configurations {",
				"    runtimeClasspath {", "        extendsFrom(custom)", "    }", "    testRuntimeClasspath {",
				"        extendsFrom(configurations.builtIn.get())", "    }", "}");
	}

	@Test
	void gradleBuildWithAnnotationProcessorDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("annotation-processor", "org.springframework.boot",
				"spring-boot-configuration-processor", DependencyScope.ANNOTATION_PROCESSOR);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    annotationProcessor(\"org.springframework.boot:spring-boot-configuration-processor\")", "}");
	}

	@Test
	void gradleBuildWithCompileDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("root", "org.springframework.boot", "spring-boot-starter", DependencyScope.COMPILE);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    implementation(\"org.springframework.boot:spring-boot-starter\")", "}");
	}

	@Test
	void gradleBuildWithNoScopeDependencyDefaultsToCompile() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("root", Dependency.withCoordinates("org.springframework.boot", "spring-boot-starter"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    implementation(\"org.springframework.boot:spring-boot-starter\")", "}");
	}

	@Test
	void gradleBuildWithRuntimeDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("driver", Dependency.withCoordinates("com.example", "jdbc-driver")
				.version(VersionReference.ofValue("1.0.0")).scope(DependencyScope.RUNTIME));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {", "    runtimeOnly(\"com.example:jdbc-driver:1.0.0\")", "}");
	}

	@Test
	void gradleBuildWithProvidedRuntimeDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("tomcat", "org.springframework.boot", "spring-boot-starter-tomcat",
				DependencyScope.PROVIDED_RUNTIME);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    providedRuntime(\"org.springframework.boot:spring-boot-starter-tomcat\")", "}");
	}

	@Test
	void gradleBuildWithTestCompileDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("test", "org.springframework.boot", "spring-boot-starter-test",
				DependencyScope.TEST_COMPILE);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    testImplementation(\"org.springframework.boot:spring-boot-starter-test\")", "}");
	}

	@Test
	void gradleBuildWithCompileOnlyDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("test", "org.springframework.boot", "spring-boot-starter-foobar",
				DependencyScope.COMPILE_ONLY);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    compileOnly(\"org.springframework.boot:spring-boot-starter-foobar\")", "}");
	}

	@Test
	void gradleBuildWithTestRuntimeDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("embed-mongo", "de.flapdoodle.embed", "de.flapdoodle.embed.mongo",
				DependencyScope.TEST_RUNTIME);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    testRuntimeOnly(\"de.flapdoodle.embed:de.flapdoodle.embed.mongo\")", "}");
	}

	@Test
	void gradleBuildWithExclusions() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("test",
				Dependency.withCoordinates("com.example", "test").scope(DependencyScope.COMPILE).exclusions(
						new Exclusion("com.example.legacy", "legacy-one"),
						new Exclusion("com.example.another", "legacy-two")));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {", "    implementation(\"com.example:test\") {",
				"        exclude(group = \"com.example.legacy\", module = \"legacy-one\")",
				"        exclude(group = \"com.example.another\", module = \"legacy-two\")", "    }", "}");
	}

	@Test
	void gradleBuildWithCustomDependencyConfiguration() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("test",
				GradleDependency.withCoordinates("org.springframework.boot", "spring-boot-starter-foobar")
						.scope(DependencyScope.RUNTIME).configuration("myRuntime"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    myRuntime(\"org.springframework.boot:spring-boot-starter-foobar\")", "}");
	}

	@Test
	void gradleBuildWithNonNullArtifactTypeDependency() throws IOException {
		GradleBuild build = new GradleBuild();
		build.dependencies().add("root", Dependency.withCoordinates("org.springframework.boot", "spring-boot-starter")
				.scope(DependencyScope.COMPILE).type("tar.gz"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencies {",
				"    implementation(\"org.springframework.boot:spring-boot-starter@tar.gz\")", "}");
	}

	@Test
	void gradleBuildWithBom() throws IOException {
		GradleBuild build = new GradleBuild();
		build.boms().add("test", "com.example", "my-project-dependencies", VersionReference.ofValue("1.0.0.RELEASE"));
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencyManagement {", "    imports {",
				"        mavenBom(\"com.example:my-project-dependencies:1.0.0.RELEASE\")", "    }", "}");
	}

	@Test
	void gradleBuildWithOrderedBoms() throws IOException {
		GradleBuild build = new GradleBuild();
		build.boms().add("bom1", "com.example", "my-project-dependencies", VersionReference.ofValue("1.0.0.RELEASE"),
				5);
		build.boms().add("bom2", "com.example", "root-dependencies", VersionReference.ofProperty("root.version"), 2);
		List<String> lines = generateBuild(build);
		assertThat(lines).containsSequence("dependencyManagement {", "    imports {",
				"        mavenBom(\"com.example:my-project-dependencies:1.0.0.RELEASE\")",
				"        mavenBom(\"com.example:root-dependencies:${property(\"rootVersion\")}\")", "    }", "}");
	}

	@Test
	void gradleBuildWithCustomVersion() throws IOException {
		GradleBuild build = new GradleBuild();
		build.settings().version("1.2.4.RELEASE");
		List<String> lines = generateBuild(build);
		assertThat(lines).contains("version = \"1.2.4.RELEASE\"");
	}

	private List<String> generateBuild(GradleBuild build) throws IOException {
		GradleBuildWriter writer = new KotlinDslGradleBuildWriter();
		StringWriter out = new StringWriter();
		writer.writeTo(new IndentingWriter(out), build);
		String[] lines = out.toString().split("\\r?\\n");
		return Arrays.asList(lines);
	}

}
