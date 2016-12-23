package org.wso2.carbon.inbound.youtubeep.retriever;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.inbound.youtubeep.YoutubeConstant;
import org.wso2.carbon.inbound.youtubeep.YoutubeInbound;
import org.wso2.carbon.inbound.youtubeep.YoutubeRegistryHandler;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

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
	private YoutubeInbound youtubeInbound;

	/**
	 * Define a global instance of the HTTP transport.
	 */
	public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/**
	 * Define a global instance of the JSON factory.
	 */
	public static final JsonFactory JSON_FACTORY = new JacksonFactory();

	public YoutubeRetriever(String name, long scanInterval, String apiKey, String apiPlaylistId,
			Properties youtubeProperties, YoutubeRegistryHandler registryHandler, YoutubeInbound youtubeInbound) {
		this.name = name;

		this.apiKey = apiKey;
		this.apiPlaylistId = apiPlaylistId;
		this.youtubeProperties = youtubeProperties;

		this.scanInterval = scanInterval;

		this.registryHandler = registryHandler;
		this.youtubeInbound = youtubeInbound;

		youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName(this.name).build();

	}

	public void execute() {
		log.debug("Execute: " + this.name);
		long currentTime = (new Date()).getTime();

		if (((this.lastRanTime + this.scanInterval) <= currentTime)) {
			this.lastRanTime = currentTime;
			log.debug("LastRanTime: " + this.lastRanTime);
			consume();
		} else {
			log.debug("Skip cycle since concurrent rate is higher than the scan interval: " + this.name);
		}
		log.debug("End: " + this.name);
	}

	private void consume() {

		try {

			/* Check if Playlist is modified */
			YouTube.Playlists.List playList = youtube.playlists().list("id").setId(this.apiPlaylistId)
					.setKey(this.apiKey);

			PlaylistListResponse playlistListResponse = playList.execute();

			String lastRegistryETag = registryHandler.readPropertiesFromRegistry(name,
					YoutubeConstant.REGISTRY_YOUTUBEEP_UPDATE_DATE_PROP);

			String listResponseETag = playlistListResponse.getItems().get(0).getEtag();

			log.debug("lastRegistryETag: " + lastRegistryETag);
			log.debug("listResponseETag: " + listResponseETag);

			boolean retrievePlaylistItems = false;

			if (lastRegistryETag != null) {
				if (!lastRegistryETag.equals(listResponseETag)) {
					log.debug("listResponseETag != lastRegistryETag");
					retrievePlaylistItems = true;
				}

			} else {
				log.debug("lastRegistryETag is null");
				retrievePlaylistItems = true;
			}

			if (retrievePlaylistItems) {
				registryHandler.writePropertiesToRegistry(name, YoutubeConstant.REGISTRY_YOUTUBEEP_UPDATE_DATE_PROP,
						listResponseETag);

				/* Retrieve Playlist videos list */
				YouTube.PlaylistItems.List list = youtube.playlistItems().list("contentDetails")
						.setPlaylistId(this.apiPlaylistId).setKey(this.apiKey).setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

				PlaylistItemListResponse listResponse = null;
				do {
					listResponse = list.execute();

					for (PlaylistItem playlistItem : listResponse.getItems()) {
						/* Retrieve Video Informations */
						YouTube.Videos.List videosList = youtube.videos().list("snippet, contentDetails")
								.setKey(this.apiKey).setId(playlistItem.getContentDetails().getVideoId());

						for (Object okey : this.youtubeProperties.keySet()) {
							String key = (String) okey;
							videosList.set(key, this.youtubeProperties.getProperty(key));
						}

						VideoListResponse videoListResponse = videosList.execute();

						log.info("Playlist Video updated to be injected: " + videoListResponse.getItems().get(0).getId());

						youtubeInbound.injectMessage(videoListResponse.toString(),
								YoutubeConstant.CONTENT_TYPE_APPLICATION_JSON);
					}

					/* Retrieve Playlist videos list next page*/
					list.setPageToken(listResponse.getNextPageToken());
				} while (!StringUtils.isEmpty(listResponse.getNextPageToken()));
			}

		} catch (IOException e) {
			log.error("Error while geting videos", e);
		}
	}

}
