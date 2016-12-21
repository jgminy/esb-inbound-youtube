/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.inbound.youtoubeep;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.inbound.endpoint.persistence.ServiceReferenceHolder;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;

/**
 * Read/Write operations with registry
 */
public class YoutubeRegistryHandler {
	private static final Log log = LogFactory.getLog(YoutubeRegistryHandler.class);

	private Registry registry;

	public YoutubeRegistryHandler() {
		try {
			registry = ServiceReferenceHolder.getInstance().getRegistry();
		} catch (RegistryException e) {
			log.error("Error getting the registry", e);
		}
	}

	public String readPropertiesFromRegistry(String feedName, String propertyName) {
		try {
			if (registry.resourceExists(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName)) {
				Resource resource = registry.get(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName);
				return resource.getProperty(propertyName);
			}
		} catch (RegistryException e) {
			log.error("Error while handling with registry", e);
		}
		return null;
	}

	public void writePropertiesToRegistry(String feedName, String propertyName, String propertyValue) {
		try {
			if (registry.resourceExists(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName)) {
				Resource resource = registry.get(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName);
				resource.setProperty(propertyName, propertyValue);
				registry.put(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName, resource);
			} else {
				Resource resource = registry.newResource();
				resource.setProperty(propertyName, propertyValue);
				registry.put(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName, resource);
			}
		} catch (RegistryException e) {
			log.error("Error while handling with registry", e);
		}
	}

	public void deleteResourceFromRegistry(String feedName) {
		try {
			if (registry.resourceExists(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName)) {
				registry.delete(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName);
				log.debug(YoutubeConstant.REGISTRY_YOUTUBEEP_PATH_PREFIX + feedName + " resource deleted");
			}
		} catch (RegistryException e) {
			log.error("Error while handling with registry", e);
		}
	}

}
