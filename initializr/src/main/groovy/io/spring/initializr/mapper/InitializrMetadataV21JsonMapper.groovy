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

package io.spring.initializr.mapper

/**
 * A {@link InitializrMetadataJsonMapper} handling the meta-data format for v2.1
 * <p>
 * Version 2.1 brings 'since' and 'until' attributes for a dependency to restrict
 * the Spring Boot versions that can be used against it.
 *
 * @author Stephane Nicoll
 * @since 1.0
 */
class InitializrMetadataV21JsonMapper extends InitializrMetadataV2JsonMapper {

	@Override
	protected mapDependency(dependency) {
		def content = super.mapDependency(dependency)
		if (dependency.since) {
			content['since'] = dependency.since
		}
		if (dependency.until) {
			content['until'] = dependency.until
		}
		content
	}
}
