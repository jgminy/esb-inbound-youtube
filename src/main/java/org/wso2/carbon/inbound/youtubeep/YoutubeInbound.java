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

package org.wso2.carbon.inbound.youtubeep;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.inbound.endpoint.protocol.generic.GenericPollingConsumer;
import org.wso2.carbon.inbound.youtubeep.retriever.YoutubeRetriever;

public class YoutubeInbound extends GenericPollingConsumer {
	private static final Log log = LogFactory.getLog(YoutubeInbound.class);

	private YoutubeRetriever youtubeRetriever = null;
	private final YoutubeRegistryHandler registryHandler = new YoutubeRegistryHandler();

	public YoutubeInbound(Properties properties, String name, SynapseEnvironment synapseEnvironment, long scanInterval,
			String injectingSeq, String onErrorSeq, boolean coordination, boolean sequential) {
		super(properties, name, synapseEnvironment, scanInterval, injectingSeq, onErrorSeq, coordination, sequential);
		log.info("Initialize Youtube polling consumer: " + this.name);

		String apiKey = getInboundProperties().getProperty(YoutubeConstant.API_KEY);
		String apiPlaylistId = getInboundProperties().getProperty(YoutubeConstant.API_PLAYLIST_ID);
		Properties youtubeProperties = new Properties();
		Set<Object> set = getInboundProperties().keySet();
		for (Object okey : set) {
			String key = (String) okey;
			if (key.startsWith(YoutubeConstant.API_YOUTUBE_PREFIX)) {
				youtubeProperties.setProperty(key.replaceAll(YoutubeConstant.API_YOUTUBE_PREFIX, ""),
						getInboundProperties().getProperty(key));
			}
		}

		log.info("apiKey            : " + apiKey);
		log.info("apiPlaylistId     : " + apiPlaylistId);
		log.info("youtubeProperties : " + youtubeProperties);

		this.youtubeRetriever = new YoutubeRetriever(this.name, scanInterval, apiKey, apiPlaylistId, youtubeProperties,
				this.registryHandler, this);

		log.info("Youtube polling consumer Initialized.");
	}

	public void destroy() {
		// this.registryHandler.deleteResourceFromRegistry(this.name);
		log.info("Destroy invoked.");
	}

	public Object poll() {
		this.youtubeRetriever.execute();
		return null;
	}

	@Override
	public boolean injectMessage(String strMessage, String contentType) {
		return super.injectMessage(strMessage, contentType);
	}

}