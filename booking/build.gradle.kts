plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.27.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Booking service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val openApiOutputDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set("$projectDir/src/main/resources/openapi.yml")
	outputDir.set(openApiOutputDir.get().asFile.absolutePath)
	apiPackage.set("com.example.booking.api")
	modelPackage.set("com.example.booking.model")
	invokerPackage.set("com.example.booking.invoker")
	modelNameSuffix.set("Dto")
	configOptions.set(
		mapOf(
			"library" to "spring-boot",
			"useSpringBoot4" to "true",
			"useJackson3" to "true",
			"useJspecify" to "true",
			"useJakartaEe" to "true",
			"dateLibrary" to "java8",
			"openApiNullable" to "false",
			"useBeanValidation" to "true",
			"annotationLibrary" to "none",
			"documentationProvider" to "none",
			"generateJsonIncludeAnnotations" to "false",
			"generateJsonSetterNullsAnnotations" to "false",
		),
	)
	globalProperties.set(
		mapOf(
			"apis" to "false",
			"models" to "",
			"supportingFiles" to "false",
			"apiTests" to "false",
			"apiDocs" to "false",
			"modelTests" to "false",
			"modelDocs" to "false",
		),
	)
}

sourceSets {
	main {
		java {
			srcDir(openApiOutputDir.map { it.dir("src/main/java") })
		}
	}
}

dependencies {
	implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.1.4"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-h2console")
	runtimeOnly("com.h2database:h2")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named("compileJava") {
	dependsOn("openApiGenerate")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
