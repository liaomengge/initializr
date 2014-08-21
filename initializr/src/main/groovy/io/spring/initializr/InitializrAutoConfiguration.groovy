package io.spring.initializr

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import io.spring.initializr.web.MainController

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to configure Spring initializr. In a web environment,
 * configures the necessary controller to serve the applications from the
 * root context.
 *
 * <p>Project generation can be customized by defining a custom
 * {@link ProjectGenerator}.
 *
 * @author Stephane Nicoll
 * @since 1.0
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(InitializrMetadata)
class InitializrAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MainController)
	MainController initializrMainController() {
		new MainController()
	}

	@Bean
	@ConditionalOnMissingBean(ProjectGenerator)
	ProjectGenerator projectGenerator() {
		new ProjectGenerator()
	}

	@Bean
	@ConditionalOnMissingBean(InitializrMetadataProvider)
	InitializrMetadataProvider initializrMetadataProvider(InitializrMetadata metadata) {
		return new DefaultInitializrMetadataProvider(metadata)
	}

	@Bean
	@ConditionalOnMissingBean(CacheManager)
	CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager()
		cacheManager.setCaches(Arrays.asList(
				createConcurrentMapCache(600, 'initializr')
		))
		cacheManager
	}

	private static ConcurrentMapCache createConcurrentMapCache(Long timeToLive, String name) {
		CacheBuilder<Object, Object> cacheBuilder =
				CacheBuilder.newBuilder().expireAfterWrite(timeToLive, TimeUnit.SECONDS);

		ConcurrentMap<Object, Object> map = cacheBuilder.build().asMap();
		new ConcurrentMapCache(name, map, false);
	}

}
