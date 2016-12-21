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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.inbound.endpoint.protocol.generic.GenericPollingConsumer;
import org.wso2.carbon.inbound.youtoubeep.retriever.YoutubeRetriever;

public class YoutubeInbound extends GenericPollingConsumer {
	private static final Log log = LogFactory.getLog(YoutubeInbound.class);

	private String apiKey;
	private String apiPlaylistId;

	private YoutubeRetriever youtubeRetriever = null;
	private final YoutubeRegistryHandler registryHandler = new YoutubeRegistryHandler();

	public YoutubeInbound(Properties properties, String name, SynapseEnvironment synapseEnvironment, long scanInterval,
			String injectingSeq, String onErrorSeq, boolean coordination, boolean sequential) {
		super(properties, name, synapseEnvironment, scanInterval, injectingSeq, onErrorSeq, coordination, sequential);
		log.info("Initialize Youtube polling consumer: " + this.name);

		this.apiKey = getInboundProperties().getProperty(YoutubeConstant.API_KEY);
		this.apiPlaylistId = getInboundProperties().getProperty(YoutubeConstant.API_PLAYLIST_ID);

		log.info("apiKey        : " + this.apiKey);
		log.info("apiPlaylistId : " + this.apiPlaylistId);

		this.youtubeRetriever = new YoutubeRetriever(scanInterval, this.apiKey, this.apiPlaylistId,
				this.registryHandler, this.name);

		log.info("Youtube polling consumer Initialized.");
	}

	public void destroy() {
		this.registryHandler.deleteResourceFromRegistry(this.name);
		log.info("Destroy invoked.");
	}

	public Object poll() {
		String out = this.youtubeRetriever.execute();
		if (out != null) {
			this.injectMessage(out, YoutubeConstant.CONTENT_TYPE_APPLICATION_JSON);
		}
		return null;
	}
}