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

import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.buildsystem.BuildSystemFactory;

/**
 * {@link BuildSystemFactory Factory} for {@link GradleKtsBuildSystem}.
 *
 * @author Jean-Baptiste Nizet
 */
class GradleKtsBuildSystemFactory implements BuildSystemFactory {

	@Override
	public BuildSystem createBuildSystem(String id) {
		if (GradleKtsBuildSystem.ID.equals(id)) {
			return new GradleKtsBuildSystem();
		}
		return null;
	}

}
