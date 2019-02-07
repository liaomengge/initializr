/*
 * Copyright 2012-2018 the original author or authors.
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

package io.spring.initializr.generator.language;

/**
 * A parameter, typically of a method or function.
 *
 * @author Andy Wilkinson
 */
public class Parameter {

	private final String type;

	private final String name;

	public Parameter(String type, String name) {
		this.type = type;
		this.name = name;
	}

	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

}
