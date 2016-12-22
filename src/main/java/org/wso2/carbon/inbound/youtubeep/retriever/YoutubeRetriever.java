package org.wso2.carbon.inbound.youtubeep.retriever;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.inbound.youtubeep.YoutubeConstant;
import org.wso2.carbon.inbound.youtubeep.YoutubeRegistryHandler;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItemListResponse;

public class YoutubeRetriever {
	private static final Log log = LogFactory.getLog(YoutubeRetriever.class);

	private static final long NUMBER_OF_VIDEOS_RETURNED = 50;

	private String name;
	private long scanInterval;
	private long lastRanTime;
	private String apiKey;
	private String apiPlaylistId;
	private Properties youtubeProperties;
	private static YouTube youtube;

	private YoutubeRegistryHandler registryHandler;

	/**
	 * Define a global instance of the HTTP transport.
	 */
	public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/**
	 * Define a global instance of the JSON factory.
	 */
	public static final JsonFactory JSON_FACTORY = new JacksonFactory();

	public YoutubeRetriever(String name, long scanInterval, String apiKey, String apiPlaylistId,
			Properties youtubeProperties, YoutubeRegistryHandler registryHandler) {
		this.name = name;

		this.apiKey = apiKey;
		this.apiPlaylistId = apiPlaylistId;
		this.youtubeProperties = youtubeProperties;

		this.scanInterval = scanInterval;

		this.registryHandler = registryHandler;

		youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName(this.name).build();

	}

	public String execute() {
		log.debug("Execute: " + this.name);
		String out = null;
		long currentTime = (new Date()).getTime();

		if (((this.lastRanTime + this.scanInterval) <= currentTime)) {
			this.lastRanTime = currentTime;
			log.debug("LastRanTime: " + this.lastRanTime);
			out = consume();
		} else {
			log.debug("Skip cycle since concurrent rate is higher than the scan interval: " + this.name);
		}
		log.debug("End: " + this.name);
		return out;
	}

	private String consume() {

		try {
			YouTube.PlaylistItems.List list = youtube.playlistItems().list("id").setPlaylistId(this.apiPlaylistId)
					.setKey(this.apiKey).setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
			
			for (Object okey : this.youtubeProperties.keySet()) {
				String key = (String)okey;
				list.set(key, this.youtubeProperties.getProperty(key));
			}

			PlaylistItemListResponse listResponse = list.execute();

			String lastRegistryETag = registryHandler.readPropertiesFromRegistry(name,
					YoutubeConstant.REGISTRY_YOUTUBEEP_UPDATE_DATE_PROP);

			log.debug("lastRegistryETag: " + lastRegistryETag);
			log.debug("listResponseETag: " + listResponse.getEtag());

			if (lastRegistryETag != null) {
				if (!lastRegistryETag.equals(listResponse.getEtag())) {
					log.debug("listResponseETag != lastRegistryETag");
					registryHandler.writePropertiesToRegistry(name, YoutubeConstant.REGISTRY_YOUTUBEEP_UPDATE_DATE_PROP,
							listResponse.getEtag());
					log.info("Playlist updated to be injected: " + name);
					return listResponse.toPrettyString();
				}

			} else {
				log.debug("lastRegistryETag is null");
				registryHandler.writePropertiesToRegistry(name, YoutubeConstant.REGISTRY_YOUTUBEEP_UPDATE_DATE_PROP,
						listResponse.getEtag());
				log.info("Playlist updated to be injected: " + name);
				return listResponse.toPrettyString();
			}

		} catch (IOException e) {
			log.error("Error while geting videos", e);
		}

		return null;
	}

}
